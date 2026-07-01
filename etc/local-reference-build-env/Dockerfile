# Minimal local reference build environment for hsadmin-ng
# mostly to verify that the build and helper scripts work with this minimal setup.
# It only contains the mandatory prerequisites documented in the repo-root README.md.
# Nothing else is installed on purpose.
#
# Build, run and read the rationale via the Makefile in this directory:
#
#   make -f etc/local-reference-build-env/Makefile help
#
# or
#
#   cd etc/local-reference-build-env
#   make help

FROM debian:trixie

ENV DEBIAN_FRONTEND=noninteractive \
    GRADLE_USER_HOME=/gradle

# The mandatory prerequisites from the README: a JRE to run Gradle (Debian trixie
# ships JDK 21, not 17; Gradle 8.5 runs on 21, and the build JDK is the
# toolchain-provisioned Adoptium 21 regardless), the bind9 utils for the DNS
# checks, and the Docker client. The client talks to the host daemon through a
# mounted socket (see the Makefile `test` target); no daemon runs in this
# container. `locales` + locale-gen provide en_US.UTF-8 so the JVM uses a UTF-8
# charset like the host (the minimal image otherwise has only C/ASCII, which
# breaks tests comparing non-ASCII expected data). python3 runs the repo's
# `howto`/`fixmes` helper tools (README suggested tools); git is needed by
# `fixmes` for its gitignore-aware file listing (git ls-files).
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
        openjdk-21-jre-headless \
        bind9-utils \
        docker.io \
        locales \
        python3 \
        git \
 && echo 'en_US.UTF-8 UTF-8' > /etc/locale.gen && locale-gen \
 && rm -rf /var/lib/apt/lists/* \
 && mkdir -p /gradle && chmod 777 /gradle

# Match the host locale so file.encoding/sun.jnu.encoding default to UTF-8.
ENV LANG=en_US.UTF-8

# Auto-load the project's shell env (.tc-environment) and aliases (.aliases) for
# every shell started in the mounted repo: interactive shells via
# /etc/bash.bashrc (e.g. `make shell`), non-interactive ones via $BASH_ENV (e.g.
# `make build`/`test`). Interactive shells always (re)load so aliases are present
# even in nested shells; non-interactive shells load once per process tree to
# avoid re-sourcing in every sub-shell Gradle spawns.
RUN cat >/etc/hsadmin-rc.sh <<'RC'
case $- in
    *i*) : ;;
    *)   [ -n "$HSADMIN_RC_LOADED" ] && return ;;
esac
export HSADMIN_RC_LOADED=1
shopt -s expand_aliases 2>/dev/null || true

# This container is ephemeral (docker run --rm): a Gradle daemon would die with
# it and leave stale registries in GRADLE_USER_HOME that break the next run, so
# default every gradle invocation (incl. a manual ./gradlew) to no-daemon.
_gp="${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties"
grep -qs '^org.gradle.daemon=' "$_gp" 2>/dev/null || echo 'org.gradle.daemon=false' >> "$_gp" 2>/dev/null || true
unset _gp

# .aliases sources a developer-local .environment at the end, which would
# override the canonical test config, so source .tc-environment LAST: it resets
# all HSADMINNG_* vars (via .unset-environment) and re-applies the reference set.
[ -r .aliases ] && . .aliases
[ -r .tc-environment ] && . .tc-environment
RC
# Wire the auto-load rc for interactive shells.
RUN echo '[ -r /etc/hsadmin-rc.sh ] && . /etc/hsadmin-rc.sh' >> /etc/bash.bashrc
ENV BASH_ENV=/etc/hsadmin-rc.sh

# No WORKDIR on purpose: the Makefile mounts the repository at its host path and
# sets the working directory to it (-w), so file:// links in build reports stay
# clickable on the host. CMD is the test-less default build (Makefile `build`);
# it runs via bash so the auto-load above (BASH_ENV) applies.
CMD ["bash", "-c", "./gradlew --no-daemon assemble"]
