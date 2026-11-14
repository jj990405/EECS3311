# Verification record — 13 November 2026

Baseline source: `f7c2d6e`; current audit fixes are uncommitted. Both local HEAD and GitHub `origin/main` were confirmed at `f7c2d6ec816f5dbc60105bb52291887d9bcd14fa` with read-only `git ls-remote`. This document is supporting evidence for the later report, not a final report or video demonstration.

## Check layers and evidence

| Layer | Result | What this establishes / limit |
| --- | --- | --- |
| Maven build and deterministic checks | **25 tests, 0 failures, 0 errors**; packaged shaded JAR | Persistence, priority/filtering, readers, source/response validation, approval, calendar import, daily TBD markers, compact date/color setup, quiz/essay/history, narrow controls, daily timer totals and date-only edit regressions. A test method may exercise several assertions; do not present 25 as the number of all scenarios. |
| Actual API core acceptance | **16 feature checks passed**, 0 failures; successful run recorded 15 API calls | Real 9-page EECS3311 syllabus; explicitly synthetic 2-page SOLID lecture and week schedule. Exercises F01–F15 plus classification. Sources below. This is one observed set of responses, not a guarantee for every PDF/model output. |
| Actual CLI acceptance | **12 command groups passed**, no `Error:` or unknown commands | Invoked the real `CommandLineInterface.run` with scripted input and actual API. Setup, syllabus/week proposals and approval, schedule/calendar/progress/notices, references/Q&A, notes/MCQ/essay, weaknesses, brief/history. Transcript retained locally. |
| Native launchers | GUI EXE launched directly through computer use; CLI EXE default launch emitted help and exited normally; explicit `--cli --data` smoke passed | No PowerShell is needed to launch the prepared GUI. A CLI is expected to display a text console. Passing custom jpackage arguments replaces launcher defaults, so custom CLI invocations explicitly include `--cli`. |
| Native GUI through computer use | Original PDF preview/paging, saved notes, MCQ 2/2 review, timer controls/records, calendar filter/day add, source chat and split divider, bulk review/Add all/TBD all checked | Real Swing app image with isolated copied data. The UI checks are a selected running acceptance tour, not a recording of every possible menu combination. Additional automated/CLI/controller evidence covers the full feature inventory. |
| User data preservation | **82 original files before and after; all SHA-256 hashes identical** | Root `data/` stayed unchanged. Testing used new directories below ignored `codex/`; original `.env` retained. API keys and copied course files are excluded from Git. |
| Diagrams | 7 architecture/class + 17 sequence + 3 use-case sheets; XML parse and SVG→PNG render checks | Two parallel source audits, rendered contact sheets and dense views reviewed. Root inspected architecture, command and bulk import sheets. Draw.io sources are editable; no claim of interactive diagrams.net editing validation. |

Reproducible deterministic build from source: `mvnw.cmd package` with JDK17+. Audit used available JDK25 and offline cached dependencies. The environment needed `-Daether.syncContext.named.factory=noop` for local Maven locking; this is an audit environment workaround, not an application feature.

Local-only artifacts are under `codex/`:

- `feature-audit-20261113/run-26f006f6-750b-4c57-9265-35cb8b3ffdef/results.tsv` — 16 passed feature checks, successful actual API run; generated answer/summary/brief/advice and isolated data alongside it.
- `cli-acceptance-20261113/run-0bf0ee7d-f52d-4b20-a329-f79cf630309e/cli-transcript.txt` — actual CLI commands, inputs and outputs for 12 groups.
- `FeatureAudit.java`, `CliAudit.java` — Java audit harnesses. They use the submitted classes and real configured API, not hard-coded answers. Synthetic source material is explicitly labeled.
- `build/target/surefire-reports/` — Maven XML/text results.
- `build/acceptance-gui-20261113/` — isolated app image used for native UI checks, with copied test data. Its temporary `.env` is removed after testing; root `.env` is preserved.
- `original-data-audit-20261113.json` — original path/hash baseline.

Do not publish these raw local artifacts wholesale: they can contain course excerpts, generated content, and personal paths. The checked-in matrix summarizes outcomes without exposing secrets.

## Actual API checks

1. Upload real syllabus and synthetic PDF; confirm extracted pages and persistence.
2. Syllabus extracts 10 assessments; verify Lab Test2 / November18 against source, approve one and reject others.
3. Syllabus context suggests lecture week3; store changes only after approval.
4. Schedule sorting, same-date difficulty, search, completion and month labels.
5. Timer pause/resume/early stop saves a session.
6. Reference recommendation returns the uploaded SOLID lecture.
7. Due notices are deduplicated; undated notice returns on next date after being marked shown.
8. Natural-language schedule edit changes Nov20 to Nov21 only after approval.
9. Open/Closed answer cites uploaded `SOLID-lecture.pdf`, page1.
10. Two-page notes and consolidated guide generated, saved and reopenable.
11. Two real-generated MCQs grade 2/2 with model answer keys; wrong answers are also recorded for topic statistics.
12. Essay question and AI feedback generated; proposed grade corrected to85 and persisted after approval.
13. Wrong answers plus confidence produce weak topics and actual source-based advice.
14. Actual weekly brief contains upcoming schedule context.
15. Misfiled synthetic lecture produces a category suggestion; reviewed move preserves original file.
16. Restart restores data/artifacts/history; selected history deletion persists.

Initial harness attempts included a path-normalization mistake and overly specific request-log parsing. Those were harness defects, corrected before the successful run; they are not counted as application failures or hidden as passing tests.

## Defects corrected in this audit

| Observed problem | Correction | Verification |
| --- | --- | --- |
| AI date-only edit could omit title/type and fail, although the original deadline already had them | `SchedulePlanner` supplies existing type/scope in context and preserves omitted fields on edits | `ScheduleEditTest`; actual API edit and CLI edit/approval passed |
| Duplicate detection recognized typo-equivalent titles, but Merge still required exact title equality | Use `DeadlineTitleMatcher.same` consistently for merge and candidate duplicate warnings | `CalendarImportTest` now merges the typo candidate, approves it and asserts only one stored deadline |
| Weekly briefing used course-linked study seconds, while normal GUI timer is unlinked | Supply all persisted weekly study seconds, explicitly labeled not course-linked | `StudyRecordsTest` checks 900 seconds from two unlinked sessions reaches the model context |
| Same-day sessions appeared as separate rows; seconds were hidden as zero minutes | Aggregate daily totals in GUI/CLI, display minutes+seconds and weekly total | Regression with two sessions; native GUI displayed one day totaling12 seconds, then another saved8 seconds |
| Narrow course page and timer lost action buttons offscreen | Responsive action grids, wrapped course rows and viewport-width content | `NarrowPanelTest` checks all5 course and7 timer buttons remain inside a310×600 panel; native narrow pane inspected |
| Course page could scroll below its first deadline when rebuilt | Disable text caret auto-update and reset course viewport to top after rebuild | Native reinspection after final build |
| Paused timer looked like a running timer; inactive actions enabled | Explicit Paused state and enabled/disabled duration/pause/resume/stop controls | Native15:00 start →14:52 Paused →Stop saves8 seconds; timer expiry logic accelerated in test |
| Store refresh could reselect a file and reopen/switch content after a study operation | Guard selection callbacks during workspace rebuild | Native timer save left timer active tab; notes/chat kept their selected surface during bulk import |
| Generated guide displayed raw `**` and cramped paragraphs | Styled bold spans, actual paragraph attributes, larger text and section spacing | Automated emphasis/spacing assertion and native saved guide visual check |
| Calendar had no explicit all-course/one-course control | Independent calendar filter and filter-aware day addition | Native filter to EECS2021, added a quiz on emptyNov19 and confirmed correct course/date |

Native bulk analysis was also rerun on the real syllabus plus synthetic schedule: 2 files analyzed,10 suggestions; first row and evidence immediately visible. Add all immediately prompted for unknown FinalExam date. Keeping TBD and confirming quiz cutoff warnings added9 entries and skipped1 existing duplicate. Calendar showed all-week named results; source chat remained visible. That validates the requested review flow without silently inventing dates.

## Boundaries still relevant to a demo

- Full15-minute natural expiry and audible alarm were not waited through in this audit. Expiry logic was tested with1 second; native real timer start/pause/resume/stop/records were checked. Record a real expiry segment for the strongest video evidence.
- Reset persistence is tested on isolated data; native destructive confirmation is inspected and canceled. Owner data is not cleared as part of verification.
- The app has no OCR, background notification service, general multi-action planner or automatic verification of factual AI correctness. Pending proposals do not survive restart; approved data does.
- Not every GUI path was manually replayed in every configuration. The full inventory has combined automated, native UI, actual controller/API and actual CLI coverage, as stated above.
- API aggregate and native Pro subscription/credit metrics were inspected on Nov14 and recorded in the collaboration log. Historical development raw tokens remain unavailable in the inspected UI. Video links are still unrecorded; no final report has been produced. The owner approved committing and pushing the audit changes on Nov14; this record describes the pre-commit verification snapshot.
