# Weekly implementation commit evidence

Source review through `f7c2d6e`, prepared 13 November 2026. Times below are Toronto local commit dates. A Git author/committer date is not evidence of when GitHub received a push. The owner previously requested retrospective dates for the first five commits; those displayed dates must not be presented as proof of period-1 pushes.

The PDF requires five meaningful implementation/test commits **pushed in each** of Oct20–26, Oct27–Nov2 and Nov3–9. The later LMS submission extension does not itself establish an extension to these reporting windows. Final grading is the instructor's decision.

## Period 1 — five substantive changes, historical push timing not satisfied by dates alone

| Displayed date | Commit | Actual source change |
| --- | --- | --- |
| Oct23 | [f16332a](https://github.com/jj990405/EECS3311/commit/f16332a) | Workspace/domain persistence, GUI/CLI semester/course/material/deadline management, priority and validation tests |
| Oct23 | [3a948e4](https://github.com/jj990405/EECS3311/commit/3a948e4) | PDF/PPTX/DOCX/TXT reader interface, format factory and extracted-text persistence |
| Oct23 | [166a137](https://github.com/jj990405/EECS3311/commit/166a137) | Configurable LLM transport, structured prompts/JSON parsing and AI workflows |
| Oct23 | [32996dd](https://github.com/jj990405/EECS3311/commit/32996dd) | Deferred add/week commands and explicit approval/rejection before persistence |
| Oct23 | [0e6e16c](https://github.com/jj990405/EECS3311/commit/0e6e16c) | Month calendar and upcoming reminder behavior |

Each changes implementation, rather than adding a separate decorative file or empty commit. Earlier conversation records identify the first public push as Nov2, not Oct23. This audit has not independently re-fetched that historical PushEvent. Therefore report these as substantive code changes with retrospective commit dates; **do not certify period 1 compliance** from Git dates. The PDF specifies a single total five-point deduction if any reporting period is deficient, not five points per commit or period.

## Period 2 — eight implementation/test candidates

| Date | Commit | Actual development progress |
| --- | --- | --- |
| Nov2 | [06c8850](https://github.com/jj990405/EECS3311/commit/06c8850) | Study timer and persistent sessions |
| Nov2 | [10d11e6](https://github.com/jj990405/EECS3311/commit/10d11e6) | Uploaded reference selection and grounded answers with citation checks |
| Nov2 | [1358dbd](https://github.com/jj990405/EECS3311/commit/1358dbd) | Saved cited summaries and deterministic MCQ scoring |
| Nov2 | [642ba16](https://github.com/jj990405/EECS3311/commit/642ba16) | Reviewable schedule planning, weekly brief and weak topics |
| Nov2 | [e06f69e](https://github.com/jj990405/EECS3311/commit/e06f69e) | Notifications, reviewed essay grades and history |
| Nov2 | [3822984](https://github.com/jj990405/EECS3311/commit/3822984) | Workspace upload improvements and GUI/CLI packaging |
| Nov2 | [4a37217](https://github.com/jj990405/EECS3311/commit/4a37217) | Stage 1 study workflow alignment |
| Nov2 | [9641759](https://github.com/jj990405/EECS3311/commit/9641759) | Study workflows, persistence and project deliverable integration |

README-only `83ab8d8` is deliberately not counted. These candidates contain actual source/test changes. Prior approved pushes in the conversation place this work in period 2; commit metadata alone does not prove push timing.

## Period 3 — six implementation/test candidates, excluding layout-only reorganization

| Date | Commit | Actual development progress |
| --- | --- | --- |
| Nov3 | [4e9bddf](https://github.com/jj990405/EECS3311/commit/4e9bddf) | Guided setup, compact dates and color selection |
| Nov3 | [fb6e17c](https://github.com/jj990405/EECS3311/commit/fb6e17c) | Date picker and syllabus date validation |
| Nov3 | [6c7f27b](https://github.com/jj990405/EECS3311/commit/6c7f27b) | Working quiz/PDF workspace and reviewed bulk AI calendar imports |
| Nov3 | [13fe83c](https://github.com/jj990405/EECS3311/commit/13fe83c) | Reviewed AI folder classification, move/reset functionality and tests |
| Nov3 | [a6c36e7](https://github.com/jj990405/EECS3311/commit/a6c36e7) | Corrected file sorting and per-file destination review before upload |
| Nov4 | [f7c2d6e](https://github.com/jj990405/EECS3311/commit/f7c2d6e) | All-week calendar labels, day editing, chat layout, daily TBD and study notes/tests |

`7cdfd95` primarily moves the same application into a unified repository layout, so it is excluded from this conservative count. A path move can appear as thousands of insertions/deletions without being thousands of new implementation lines. Meaningfulness is supported by behavior changes, not file count or raw diff size.

The approved follow-up commits `fdcfcd7`, `1ab1b5a`, `7d55e8e` and `a001883` were pushed after these reporting windows. They cannot repair an earlier missing push window. The last includes timeline context actions, muted course markers, SD15 and the report/filming draft. The later AI/file/result-window corrections were approved and pushed as 8ab4a77. The owner authorized the complete-source/lecture-study follow-up and report/script update on Nov14; none of this later work is counted toward an earlier period.
