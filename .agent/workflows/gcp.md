---
description: Git commit and push with generated message.
---

Find the latest version number from git tags (e.g., `git tag -l 'v*' --sort=-v:refname | head -1`) or parse from the most recent commit message if no tags exist. Increment the version number by 0.0.0.1 if its a patch release, by 0.0.1 if its a minor release, by 0.1.0 if its a major release. Show the current and incremented version numbers to the user and ask the user 1. to confirm the version number or 2. keep an existing version number or 3. enter a new version number.

Commit all changes and push to the remote repository with a generated commit message. Start the commit message with the version number (e.g., "v0.7.2.7: ..."). After pushing, create a git tag with the version number and push the tag.

Please analyze the current changes and generate an appropriate commit message according to the CLAUDE.md file, then commit and push to the repository. Do not merge to anything.

This command is only for committing changes to the current branch.
This command is your permission to commit changes to the current branch.

NEVER mention Claude, AI, or any assistant in commit messages.
NEVER switch to a different branch.
NEVER merge to another branch.
