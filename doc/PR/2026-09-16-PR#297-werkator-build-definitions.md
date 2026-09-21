## Related Links (Hostsharing-internal)

- [PR#297](https://dev.hostsharing.net/hostsharing/hs.hsadmin.ng/pulls/297)

## The Problem

Currently, mostly the CI host configuration on vm4006 (`.git/werkator/.werkator.yml`) defines how this repository is built:
build commands, branch triggers, the nightly schedule, artifact directories, the Docker build image and Docker socket access.
This should go into the repository, where changes are versioned and reviewed.
The host configuration took it over from the legacy migration as a temporary copy.

But the config in the repo does not yet provide these settings
because it still uses the legacy top-level `branches` section, which Werkator ignores.
Werkator's next release also rejects that section.
On `master` this is going to stop the CI instance from starting, because the CI host's checkout is on `master`
and this file is part of the instance configuration.
On other branches it only fails the builds of that branch.

## Non-Goals

- Application changes
- CI host configuration
- Adding `werkator.version.since`: a value below the release that changes the configuration format is rejected at start, so it can only be set once that release number is known.
- Migrating other branches; they need to be rebased onto master or merge it afterwards.

## The Solution

Replace the legacy section with the build definitions `default` (all branches except master)
and `release` (master, on push and nightly), carrying the same commands, artifacts
and Docker settings the host configuration defines today.

The names are the same as in the host configuration on purpose:
Werkator merges definitions with the same name across configuration layers, while different names would be separate jobs and build every branch twice.

Rollout order:

1. Merge this PR.
2. Remove the build definitions from the host configuration, keeping only secrets, server settings and the pinned Docker settings.
   In the reverse order master would not be built at all.
3. Deploy the Werkator release that rejects the legacy section.

Verification: PR CI green with test and JaCoCo reports; after the merge, a green nightly `release` build and no legacy-section warning in the CI host journal.

## Additional Changes

- Rename `.gittally.yml` to `.werkator.yml` to match the new tool name, including the references in `../../README.md`, `../ci-build-server-setup.md` and `../../tools/pr-quick-check`.
- Remove the deprecated `builds.maxConcurrent`, it's now in host config as `executor.maxConcurrent`.
- Share Claude and Codex hook configuration in `.agents/hooks.json` through relative symlinks.
- Support both Claude edit payloads and Codex patches in the shared Java convention hook, checking only newly added text.
- Add `Writing Style` section to `AGENTS.md`.
