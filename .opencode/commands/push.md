---
description: Commit all changes and push them to GitHub
---

Commit and push the current project changes to GitHub.

1. Check the current Git status.
2. Stage all relevant changed and untracked files with `git add .`.
3. Create a commit using the argument I provide as the commit message.
4. Push the current branch to its configured remote.
5. If there are no changes to commit, tell me that the working tree is already clean.
6. If any Git command fails, stop and explain the error instead of ignoring it.

Commit message: $ARGUMENTS