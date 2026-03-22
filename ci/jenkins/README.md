# Jenkins Migration

This repo now treats Jenkins as the authoritative release path. The legacy `scripts/build-release.sh` wrapper remains only as a local fallback and calls the same shared release primitives that the controller runs.

## Operations Tool

Use [scripts/jenkinsctl.py](/home/pranav/Desktop/ProgrammingProject/YetAnotherMusicPlayer/scripts/jenkinsctl.py) for day-to-day controller work instead of raw `curl` calls. It reads `JENKINS_USERNAME` and `JENKINS_TOKEN` from the repo `.env` file by default and supports:

- auth validation with `python3 scripts/jenkinsctl.py login`
- controller inventory with `python3 scripts/jenkinsctl.py jobs` and `python3 scripts/jenkinsctl.py queue`
- pipeline inspection with `python3 scripts/jenkinsctl.py job-info YetAnotherMusicPlayer-CI`
- build history and logs with `python3 scripts/jenkinsctl.py builds YetAnotherMusicPlayer-CI`, `build-info`, and `console`
- live execution with `python3 scripts/jenkinsctl.py trigger YetAnotherMusicPlayer-CI --param RELEASE_TYPE=ci --wait --follow`
- admin/script-console automation with `python3 scripts/jenkinsctl.py script --file /path/to/script.groovy`

## What Changed

- `Jenkinsfile` provides a declarative Android pipeline with checkout, bootstrap, quality gates, optional connected tests, signed release packaging, tagging, and GitHub Release publishing.
- `version.properties` is now the single source of truth for app versioning. Jenkins bumps that file instead of editing `app/build.gradle.kts`.
- `app/build.gradle.kts` now supports release signing from Jenkins credentials via environment variables, so the controller no longer depends on a committed `keystore.properties`.
- `scripts/ci/*.sh` contains reusable versioning, packaging, and GitHub release publishing logic shared by Jenkins and local fallback runs.
- `scripts/jenkinsctl.py` provides a repo-owned Jenkins client for authentication, queue/build control, log streaming, and script-console tasks.

## Local Findings

The existing install on this machine is a fresh Debian-package controller:

- Jenkins `2.541.3`
- Service account: `jenkins`
- No plugins are installed in `/var/lib/jenkins/plugins`
- Package service expects Java 17/21/25; shell `java` currently points to Java 22, which is unsupported for Jenkins CLI invocations
- The controller user cannot traverse `/home/pranav`, so it cannot currently read `/home/pranav/Android/Sdk`

That last point is the main environment blocker for Android builds from Jenkins.

## Controller Setup

1. Install the plugins listed in [plugins.txt](/home/pranav/Desktop/ProgrammingProject/YetAnotherMusicPlayer/ci/jenkins/plugins.txt). Jenkins docs recommend the Plugin Installation Manager Tool for offline or scripted installs.
2. Install a real compiler-capable JDK for controller-side builds, for example `openjdk-21-jdk-headless`. The current machine only has the Java 21 JRE package installed.
3. Add the systemd drop-in from [override.conf](/home/pranav/Desktop/ProgrammingProject/YetAnotherMusicPlayer/ci/jenkins/systemd/override.conf) with `systemctl edit jenkins`.
4. Copy [jenkins.yaml](/home/pranav/Desktop/ProgrammingProject/YetAnotherMusicPlayer/ci/jenkins/casc/jenkins.yaml) to `/var/lib/jenkins/casc_configs/jenkins.yaml`.
5. Grant the `jenkins` user access to the Android SDK path or relocate the SDK to a shared path like `/opt/android-sdk`.
6. Restart Jenkins and verify `http://localhost:8080/` loads.
7. Create these Jenkins credentials:
   - `github-token` as a Secret text credential with repo release/tag/push scope
   - `yamp-release-keystore` as a Secret file credential
   - `yamp-keystore-password` as Secret text
   - `yamp-key-alias` as Secret text
   - `yamp-key-password` as Secret text
8. Create a Multibranch Pipeline job pointed at `https://github.com/praj107/YetAnotherMusicPlayer.git`.

## First Release Run

Use these parameters on the Jenkins job:

- `RELEASE_TYPE=patch|minor|major|chore`
- `RUN_LINT=true`
- `RUN_CONNECTED_TESTS=false` unless an emulator/device is attached

The pipeline will:

- bump `version.properties`
- run unit tests and lint
- build signed `apk` and `aab` outputs
- generate checksums, mapping, source archive, and release notes
- commit the version bump
- tag the release
- publish the GitHub Release assets used by the in-app updater
