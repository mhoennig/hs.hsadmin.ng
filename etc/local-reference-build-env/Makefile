# Build and run the local reference build environment (see the Dockerfile in
# this directory). The repository root is derived from this Makefile's own
# location, so the targets work regardless of the directory make is invoked from.

IMAGE         := hsadmin-ng-local-reference-build-env
GRADLE_VOLUME := hsadmin-ng-gradle-home

ROOT       := $(abspath $(dir $(lastword $(MAKEFILE_LIST)))/../..)
DOCKERFILE := $(ROOT)/etc/local-reference-build-env/Dockerfile

# make cannot depend on a Docker image, so a stamp file stands in for it. Its
# name embeds a short checksum of the Dockerfile + this Makefile, so changing
# either yields a new stamp name that does not exist yet -- which makes
# `build`/`shell`/`test` rebuild the image. Content-addressed, not mtime-based.
STAMP_HASH := $(shell cat $(DOCKERFILE) $(lastword $(MAKEFILE_LIST)) 2>/dev/null | sha256sum | cut -c1-12)
STAMP      := $(ROOT)/build/.$(IMAGE).$(STAMP_HASH).stamp

# Reconcile the stamp with the real daemon state for the targets that launch a
# container: if the image is gone (removed outside `make clean`, pruned, or never
# built on this host) drop the stamp so it gets rebuilt. Pure make + docker +
# coreutils, nothing to install. Scoped to build/shell so e.g. `make help` and
# `make clean` need no daemon.
ifneq (,$(filter build shell test,$(MAKECMDGOALS)))
_ := $(shell docker image inspect $(IMAGE) >/dev/null 2>&1 || rm -f $(STAMP))
endif

DOCKER_BUILD = docker build -t $(IMAGE) -f $(DOCKERFILE) $(ROOT)

# Common docker run options for every run target (everything before the image).
RUN_OPTS = --rm -it \
		-u "$$(id -u):$$(id -g)" -e HOME=/gradle \
		-v "$(ROOT)":"$(ROOT)" -w "$(ROOT)" \
		-v $(GRADLE_VOLUME):/gradle

# Docker-out-of-Docker: expose the host daemon so the full build's
# Testcontainers integration tests can run. Mount the daemon socket and share
# the host network so Testcontainers' published ports are reachable at localhost.
# Mount the socket at the path the daemon sees (/var/run/docker.sock); on
# VM-backed engines like Rancher Desktop this is the path inside the VM, NOT the
# resolved host path -- so override with the literal in-daemon path if needed.
DOCKER_SOCK ?= /var/run/docker.sock

# gid of the socket as seen INSIDE the container, so --group-add lets the --user
# process use it. This differs from the host gid on VM-backed engines (e.g.
# Rancher Desktop: the in-VM socket is root:<vm-gid>), hence we read it through a
# throwaway container rather than `stat`-ing the host path. Empty if no daemon.
SOCK_GID = $(shell docker run --rm -v $(DOCKER_SOCK):/var/run/docker.sock $(IMAGE) stat -c '%g' /var/run/docker.sock 2>/dev/null)

# TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE tells Testcontainers which socket path to
# mount into the helper containers it spawns (Ryuk, etc.); it must be valid for
# the daemon, i.e. /var/run/docker.sock, not our --user's host path.
DOCKER_DAEMON = --network host \
		-v $(DOCKER_SOCK):/var/run/docker.sock \
		$(if $(SOCK_GID),--group-add $(SOCK_GID)) \
		-e TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock

.PHONY: image build shell test clean help
.DEFAULT_GOAL := help

help: ## Show this help and what this build environment is for
	@echo 'Local reference build environment for hsadmin-ng mostly to verify tooling.'
	@echo
	@echo 'A container with ONLY the mandatory prerequisites documented in the'
	@echo 'main README.md.'
	@echo 'Nothing else is installed on purpose, so a normal build that'
	@echo 'relies on an undocumented tool fails here instead of silently succeeding'
	@echo 'on a developer machine.'
	@echo
	@echo 'The repo is mounted at its host path (so file:// links in build reports'
	@echo 'stay clickable); the Gradle home (dependency cache + auto-provisioned'
	@echo 'JDK) persists in the docker volume $(GRADLE_VOLUME).'
	@echo
	@echo '`make test` runs the full build incl. the Testcontainers integration'
	@echo 'tests, using the host Docker daemon via the mounted socket.'
	@echo
	@echo 'Every shell in the container auto-loads .aliases and .tc-environment.'
	@echo
	@echo 'Targets:'
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z0-9_-]+:.*## / {printf "  %-7s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# Force a (re)build regardless of the stamp.
image: ## Force a (re)build of the image
	$(DOCKER_BUILD)
	@mkdir -p $(dir $(STAMP)) && rm -f $(ROOT)/build/.$(IMAGE).*.stamp && touch $(STAMP)

# The stamp name is content-addressed (see STAMP), so its mere absence -- after a
# Dockerfile/Makefile change or on a fresh checkout -- is what triggers a rebuild;
# old-hash stamps are pruned so build/ keeps only the current one.
$(STAMP):
	$(DOCKER_BUILD)
	@mkdir -p $(dir $@) && rm -f $(ROOT)/build/.$(IMAGE).*.stamp && touch $@

build: $(STAMP) ## Run a normal, test-less build (./gradlew assemble; no daemon needed)
	docker run $(RUN_OPTS) $(IMAGE)

# Full build incl. the Testcontainers integration tests; needs the host Docker
# daemon (DOCKER_DAEMON). .tc-environment/.aliases are auto-loaded by the image
# (BASH_ENV), so the command just calls gradle.
test: $(STAMP) ## Run the full build incl. integration tests (needs Docker)
	docker run $(RUN_OPTS) $(DOCKER_DAEMON) $(IMAGE) \
		bash -c './gradlew --no-daemon test'

shell: $(STAMP) ## Open an interactive bash shell (with Docker access)
	docker run $(RUN_OPTS) $(DOCKER_DAEMON) $(IMAGE) bash

# Remove all containers created from this image, then the image and the stamps.
# Best-effort and quiet: "nothing to remove" / a missing docker is not an error.
# The gradle cache volume ($(GRADLE_VOLUME)) is intentionally kept; remove it
# explicitly with: docker volume rm $(GRADLE_VOLUME)
clean: ## Remove containers, the image and stamps (keeps the gradle cache volume)
	@docker ps -aq --filter ancestor=$(IMAGE) 2>/dev/null | xargs -r docker rm -f >/dev/null 2>&1 || true
	@docker image rm $(IMAGE) >/dev/null 2>&1 || true
	@rm -f $(ROOT)/build/.$(IMAGE).*.stamp
	@echo "clean: removed image, containers and stamps for $(IMAGE) (where present)"
