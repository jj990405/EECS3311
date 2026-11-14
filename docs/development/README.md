# Development audit — 14 November 2026

Supporting records for the Stage 2 implementation report. The verified implementation fixes, tests and diagrams were committed and pushed in [fdcfcd7](https://github.com/jj990405/EECS3311/commit/fdcfcd7) on Nov14 after owner approval. A subsequent documentation clarification separates the development subscription fee from the application's API usage. The Nov14 follow-up adds full-document classification, neutral gray row shades, a new quiz workspace, timer-status navigation and expanded CLI quiz settings; the owner approved committing and pushing this follow-up on Nov14.

Later approved timeline/report work is in [a001883](https://github.com/jj990405/EECS3311/commit/a001883). The current local follow-up groups AI actions, makes file lists selectable/contextual, adds selected-slide quizzes and chat tool commands, provides landscape result windows with native maximize controls, and batches complete source documents for notes. Its 45-test build, actual API/CLI/native checks and updated full Korean-action/English-narration script are recorded below. These latest changes await separate commit approval.

- [Feature inventory and verification](feature-matrix.md): every Stage 1 feature F01–F15, additions, entry points, collaboration, and practical limits.
- [Patterns and design changes](patterns-and-changes.md): six distinct GoF patterns, Simple Factory, and explicit Stage 1 differences.
- [Verification record](verification.md): automated checks, actual API tests, native GUI checks and corrected defects.
- [AI collaboration and costs](ai-collaboration.md): evidence-based development history and information still needed from the owner.
- [Weekly source commits](weekly-commits.md): conservative implementation/test commit count and date limitations.
- [Stage 2 requirement check](stage2-requirements-check.md): line-by-line implementation/submission audit, including pending recordings and unavailable historical evidence.
- [Detailed filming script](../demo/filming-script.ko.md) and [printable guide](../demo/filming-guide.pdf): exact GUI/CLI actions and English narration. Actual recording links/times remain to be supplied.
- [Stage 2 report](../../stage2-report.pdf): English draft with all required sections and readable diagram appendix; the demonstration index is not final.
- [Updated diagrams](../design/implemented/README.md): 28 rendered, editable sheets.

The assignment PDF is in `docs/assignment/`. The LMS screenshot supplied by the owner extends submission to **15 November 2026, 11:59 PM**. That screenshot does not announce a change to the three weekly reporting periods in the PDF. The original PDF still prints the old due date.

The application is Java. PDF extraction/rendering uses Apache PDFBox; Swing supplies the GUI; Java HTTP supplies API calls. Development/rendering tools used to prepare documentation are not application runtime dependencies.

Remaining submission work: record the running GUI and CLI with labeled feature segments; supply accessible links/timestamps; retain the measured API aggregate and declare missing historical development token totals; fill the actual recording links/times in the prepared report and finalize it; push approved final source/document changes; submit the repository URL through LMS. API totals and native subscription/usage metrics were inspected through Computer Use on Nov14 and are recorded in the collaboration log. This audit is not a claim of a guaranteed grade or exhaustive defect freedom.
