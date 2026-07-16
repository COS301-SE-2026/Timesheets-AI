# Contributing to Momently

This document highlights the contribution rules for the Momently X Cybernauts project. All team members are expected to follow these guidelines without exception (unless the exception was agreed upon during one of our meetings and documented here such it applies to all of us.)

## Golden Rule: NO Vibe Coding

AI tools are here to assist us with learning, brainstorming and debugging only. Submitting code you cannot explain during a Demo or code review is a violation. If AI was used, please log it in the shared AI log sheet such we remain transparent with tutors.

## Git Workflow

### Branching
Never commit directly to `main` or `dev`. Do your work on a dedicated branch using the following naming format:
- `feat/your-task-name`
- `fix/bug-name`
- `docs/document-name`

### Commit frequency
Every team member muust average atleast 10 meaningful(hyperperform doesn't count documentation commits) commits per week. Commits can and will be disregarded by graders where deemed so.

### Conventional Commits
Commit messages must follow the Conventional Commits format:
- feat: ...
- fix: ...
- docs: ...

### Pull Requests
A template has been created for every pull request one makes. Just edit it accordingly.

## Code Organisation
We follow a folder-by-feature structure based on **LIFT principle** (read more about it in our Conding Standards Handbook).

## Testing Standards
A feature is not complete until its tested and all test cases pass.
- **AAA Pattern:** All unit test must follow this pattern.
- **Test Naming:** Use format `should_[behaivour]_when_[condition]`.
- **Coverage:** SonarCloud badges must remain green.

## Documentation and Safety

- **File Headers:** Every new file must include the standard authorship
  header linking to its Functional Requirement ID from the Traceability Matrix
  (e.g. `FR-01`).
- **Database Migrations:** Never modify the database schema manually.
  All schema changes must go through a migration file.
- **No Secrets:** Never commit passwords, API keys, or JWT secrets.
  Use the `.env` file and ensure it is listed in `.gitignore`.


## Definition of Done
A task is only considered Done when all of the following are true:

1. It complies with the Coding Standards Handbook.
2. All automated tests pass and linting is clean.
3. At least one teammate has reviewed and approved the PR.
4. The feature works on the live AWS deployment URL.
   A feature that only works locally is not done.


*Maintained by the QA Lead. Last updated: 07 July 2026.*

