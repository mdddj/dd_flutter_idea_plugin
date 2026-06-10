---
name: flutterx-release
description: Release workflow for the dd_flutter_idea_plugin / FlutterX IntelliJ plugin. Use when the user asks to publish, release, bump, tag, or prepare a new plugin version, including creating the version branch, updating CHANGELOG.md and gradle.properties, pushing code, triggering GitHub Actions with a v* tag, and merging the released version branch back to master.
---

# FlutterX Release

## Overview

Follow this workflow to publish a new FlutterX plugin version from this repository. The GitHub release workflow is tag-triggered and checks out a branch named after the tag without the `v` prefix, so the remote version branch must exist before pushing the tag. After a successful release workflow, merge the released version branch back into `master` and push `master`.

## Preconditions

- Work from the repository root.
- Fetch remotes and tags first: `git fetch --tags origin`.
- Inspect the current branch, working tree, latest tags, and version:
  - `git status --short --branch`
  - `git log --oneline --decorate --max-count=12`
  - `git tag --sort=-version:refname --list 'v*' | head`
  - `grep '^pluginVersion=' gradle.properties`
- Infer the next patch version from `pluginVersion` unless the user gives a specific version.
- Confirm the remote branch and tag do not already exist:
  - `git branch -a --list '*<version>*'`
  - `git tag --list 'v<version>'`
- Do not discard or revert local changes. If user says to publish all code, include all current changes after a quick review for obvious secrets or generated junk.

## Release Workflow

1. Commit current work on the current development branch.
   - Review `git diff --stat` and relevant file diffs.
   - Run the project verification that matches the changes. At minimum use `./gradlew compileKotlin verifyPluginConfiguration`.
   - Stage all intended files with `git add -A`.
   - Commit with a concise feature/fix message.
   - Push the current branch to origin.

2. Create the version branch from the pushed HEAD.
   - Use the plain version string as branch name, for example `7.0.5`.
   - Run `git switch -c <version>`.

3. Update release metadata on the version branch.
   - In `CHANGELOG.md`, add `## <version> - <YYYY-MM-DD>` immediately after `## Unreleased`.
   - Summarize changes from `git log v<previous-version>..HEAD` and local diffs if needed.
   - Use concise sections such as `New Features`, `Improvements`, `Fixes`, `Localization`, or `Automation`.
   - In `gradle.properties`, set `pluginVersion=<version>`.

4. Verify the version branch.
   - Run `./gradlew compileKotlin verifyPluginConfiguration`.
   - If verification fails, fix the issue before continuing.

5. Commit and push the version branch.
   - Stage only release metadata unless additional fixes were required.
   - Commit as `release: <version>`.
   - Push with `git push -u origin <version>`.

6. Push the release tag after the remote branch exists.
   - Create `v<version>` on the release commit: `git tag v<version>`.
   - Push it: `git push origin v<version>`.
   - This triggers `.github/workflows/release.yml`, which checks out branch `<version>`.

7. Check the release workflow.
   - Run `gh run list --workflow release.yml --limit 5` when GitHub CLI is available.
   - Report the workflow status, run id, and any immediate failure.

8. Merge the released version branch back to `master`.
   - Start only after the release workflow succeeds. If the workflow is still running, report `in_progress` and do not merge to `master` yet.
   - Preserve any local uncommitted changes before switching branches. Prefer committing intended release follow-up changes to `<version>`; otherwise stash them with a clear message.
   - Update master first: `git switch master` then `git pull --ff-only origin master`.
   - Merge the released version branch: `git merge --no-ff <version> -m "merge: <version> into master"`.
   - Resolve conflicts deliberately. Prefer the released `<version>` side for release metadata and released feature implementation; preserve independent `master` changes when they do not conflict with the release.
   - Run at least `./gradlew compileKotlin verifyPluginConfiguration` after resolving conflicts.
   - Push master: `git push origin master`.

9. Leave the repository on `master`.
   - After the master merge succeeds, ensure the local checkout is on `master` and tracks `origin/master`.
   - Report any stashes that were created to preserve local-only files or post-release edits.
   - Treat `master` as the synchronized branch containing the released version.

## Changelog Guidance

Base the changelog on commits since the previous release tag:

```bash
git log --oneline v<previous-version>..HEAD
```

Write user-facing entries, not raw commit messages. Mention setting changes, UI changes, compatibility changes, localization, CI/release automation, and bug fixes when present.

## Guardrails

- Never push `v<version>` before pushing branch `<version>`.
- Never merge `<version>` to `master` before the release workflow has completed successfully.
- Never reuse an existing version branch or tag without explicit user instruction.
- Do not return to the version branch after a successful master merge unless the user explicitly asks.
- Never run destructive git commands such as `git reset --hard` or `git checkout --` unless the user explicitly asks.
- Do not leave required long-running commands active at the end of the turn.
- Include validation results and the GitHub Actions state in the final response.
- Include the master merge commit and push result in the final response.
