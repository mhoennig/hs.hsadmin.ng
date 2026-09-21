# CI Build-Server Setup

This guide shows how to run the `hsadmin-ng` CI build on a new Hostsharing container server.
The CI system is [GitTally](https://github.com/mhoennig/gittally).
It watches the branches on origin, builds each new commit, and reports the result to Gitea.

This guide covers only the parts that are specific to `hsadmin-ng`.
For all other questions, the GitTally repository is the reference:
read `docs/deployment.md`, `docs/bootstrapping.md`, and `docs/configuration.md` there.

## What a Hostsharing Container Server Provides

A Hostsharing container server has some properties that decide the installation method:

- No Java runtime, and you cannot install one.
  Therefore install GitTally as a self-contained runtime bundle, not as a JAR.
- No root access and no web server on the host.
  Therefore let GitTally manage its own nginx and its own TLS certificate.
- Rootless Docker.
  GitTally supports this; do not change the container user.
- A systemd user session.
  GitTally runs as a user service.
- Public ports 80 and 443, which the platform routes to the ports 8080 and 8443 of the container.

The reference host `vm4006` has 4 cores and 11 GiB of RAM.
The build uses 3 parallel test forks, which already loads these 4 cores.
Only increase `HSADMINNG_TEST_MAX_PARALLEL_FORKS` if the server has more cores.

## Prerequisites

- A DNS name that points to the server, for example `vm4006.hostsharing.net`.
  The managed TLS certificate needs it.
- `git` and `docker` on the `PATH`.
- A Gitea account with write access to the repository, and an API token of that account.
  GitTally needs the token to fetch the repository and to report build statuses.

Enable lingering once, so that the service survives a logout:

```bash
loginctl enable-linger "$USER"
```

## 1. Install the GitTally Runtime Bundle

Build the bundle on a Linux x86_64 machine.
Its glibc must not be newer than the glibc of the server.

```bash
cd <gittally-repo>
./gradlew runtimeBundle
scp build/distributions/gittally-runtime-linux-x64.tar.gz <user>@<server>:/tmp/
```

Unpack it on the server:

```bash
mkdir -p ~/opt
tar -xzf /tmp/gittally-runtime-linux-x64.tar.gz -C ~/opt
~/opt/gittally/bin/gittally --version
```

All commands below use `~/opt/gittally/bin/gittally`;
the GitTally documentation writes `java -jar ~/bin/gittally.jar` for the same program.

## 2. Clone the Repository

```bash
git clone https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng.git ~/hs.hsadmin.ng
cd ~/hs.hsadmin.ng
```

GitTally builds each branch in its own git worktree below `.git/gittally/worktrees/`, never in this primary checkout.

## 3. Create the Local Configuration

```bash
~/opt/gittally/bin/gittally init
```

This creates `.git/werkator/.werkator.yml`, which git never tracks.
It holds the secrets and all settings that belong to this host.
Fill it in:

```yaml
git:
  account: <gitea-account>
  token: <gitea-api-token>

server:
  publicBaseUrl: "https://<server>.hostsharing.net/"
  port: 18080
  bindAddress: 0.0.0.0
  impressumUrl: "<url-of-your-legal-disclosure>"
  nginx:
    enabled: true
    serverName: <server>.hostsharing.net
    httpPort: 8080
    httpsPort: 8443
    letsencryptEmail: ""
```

Keep `bindAddress` at `0.0.0.0`.
The nginx container cannot reach the `127.0.0.1` of the host, so a loopback binding would make GitTally unreachable.
`server.port` must differ from `httpPort` and `httpsPort`.

GitTally obtains the certificate from Let's Encrypt through a certbot container.
Therefore the DNS name must already point to the server before the first start.

`init` would also create a `../.werkator.yml` template in the repository root,
but it never overwrites the committed file of `hsadmin-ng` — there is nothing to do here.

## 4. What the Committed Configuration Already Does

[`../.werkator.yml`](../.werkator.yml) in the repository root holds the build configuration of `hsadmin-ng`.
It is committed, so a branch can change its own build, and you do not configure any of it on the server.
It defines:

- The build command, which runs the tests, the coverage report, the documentation, the smoke test, and the PR quick check.
- The build container, which GitTally builds from [`Jenkins/jenkins-agent/Dockerfile`](../Jenkins/jenkins-agent/Dockerfile).
  The image holds the JDK, `git`, `docker`, PostgreSQL client, and the pandoc/LaTeX tools for the documentation PDFs.
- `TESTCONTAINERS_RYUK_DISABLED` and the Testcontainers socket options, which the integration tests need.
- `HSADMINNG_POSTGRES_ADMIN_USERNAME` and `HSADMINNG_POSTGRES_RESTRICTED_USERNAME`:
  the migration tests start Liquibase without Spring, so the changelog parameters resolve only through Liquibase's own environment variables.
- A nightly build of `master`, which finds problems that creep in through the environment or through dependencies.
- An extra artifact of the `master` build: the deployable jar with the rendered documentation inside it.
  It is a release artifact, and it costs a second scenario-test run plus the rendering, so branch builds do not build it.

GitTally never lets a branch change the security-relevant settings.
It strips the secrets and the container policy from the committed file, and always takes them from the host configuration.

## 5. Install and Start the Service

```bash
~/opt/gittally/bin/gittally init --systemd
```

The command prints the install commands for the generated units.
Run them, then start the service:

```bash
systemctl --user daemon-reload
systemctl --user enable --now gittally-hs.hsadmin.ng.service
systemctl --user enable --now gittally-docker-prune.timer
```

Expect many builds after the first start:
GitTally builds every branch whose last commit is younger than `watcher.newBranchMaxAge` (5 days), one after the other.

## 6. Verify the Installation

```bash
systemctl --user status gittally-hs.hsadmin.ng.service
journalctl --user -u gittally-hs.hsadmin.ng.service -f
```

Then check in a browser:

- `https://<server>.hostsharing.net/` shows the build overview with a valid certificate.
- A build reaches `success`, and its artifact page shows the test reports.
- Gitea shows the build status at the commit.

## 7. Reach the Artifacts

Each build has its own address below `/builds/<build-id>`, which always keeps the artifacts of that one build.

Each branch also has a permanent address below `/branches/<branch>`, which has no build id in it.
It always shows the newest **green** build of that branch, so a failed build never replaces the artifacts behind such a link.
Example: `https://<server>.hostsharing.net/branches/master/reports/tests/test/`.

## 8. Update GitTally

Wait until no build runs, because a restart stops a running build.
The start page shows this, and so does `/api/builds/current`.

```bash
systemctl --user stop gittally-hs.hsadmin.ng.service
cp -a ~/opt/gittally ~/opt/gittally.bak
tar -xzf /tmp/gittally-runtime-linux-x64.tar.gz -C ~/opt
~/opt/gittally/bin/gittally --version
systemctl --user start gittally-hs.hsadmin.ng.service
```

Remove the backup after some quiet days.

## Troubleshooting

**The build fails with `environment variable HSADMINNG_POSTGRES_ADMIN_USERNAME not set`.**
The host configuration overrides `branches.default.docker.env` and thus drops the values of the committed file.
Do not set build settings on the host; keep them in the committed `../.werkator.yml`.

**The build fails with `fatal: not a git repository` or `Failed to create parent directory`.**
An old GitTally version — update it.
Current versions mount the primary `.git` read-only with `.git/gittally/` hidden,
and run the rootless-Docker build container as user `0`, the unprivileged host user.

**The web UI answers with HTTP 404 after the first start.**
The certificate is missing, so nginx did not start.
Check the DNS name, and check that the platform routes the public port 80 to `httpPort`.
GitTally treats all certificate errors as warnings, so the plain HTTP server keeps running.
