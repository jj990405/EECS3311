# Verification record — 14 November 2026

Source baseline: `1ab1b5a` (after the approved Nov13 audit and cost clarification). The owner approved committing and pushing this follow-up on 14 November 2026. It records bounded evidence for the later Stage 2 report; it is not the final report or a video submission.

## Results

| Layer | Observed result | Scope and practical limit |
| --- | --- | --- |
| Maven package and deterministic checks | **35 tests, 0 failures, 0 errors** | Persistence/readers, date/source validation, approvals, priority, calendar/TBD markers, layout, timer totals, full-document segmentation, complete classification responses, sorted row colors, quiz answer retention/submission gate/read-only review, CLI essay scope/count, chosen question difficulty versus course explanation depth, and malformed essay regeneration. Test methods are not a count of all scenarios. |
| Actual API core acceptance | **16 checks passed**, 0 failures; **17 calls** in the successful run | F01–F15 plus classification: real 9-page EECS3311 syllabus, explicitly synthetic 2-page SOLID lecture and schedule. Sources, approval, persistence, timer controls, reminders, references, schedule edits, Q&A, notes, MCQ, essay, weak topics, briefing and history. |
| Actual mixed PDF classification | **25 of 25 correct** against a manual document-purpose check | 1 official syllabus, 22 teaching/reference PDFs, 2 assignment/project instruction PDFs. Separate API classification and actual native pre-upload AI sorting both checked. An ambiguous renamed 20-page Admin deck received full-document review. This sample result does not imply universal accuracy. |
| Actual CLI acceptance | **23 groups passed; all 41 recognized commands exercised** | Real `CommandLineInterface.run`, scripted input and configured API. Basic 12 groups plus 11 supplemental groups. File/week/all quiz scope, MCQ/essay/both, two requested essays, duplicate upload skip, compact-date retry, multi-semester choice, approval/rejection, history deletion, schedule CRUD, filtering, reminders and all study commands. |
| Native EXE | GUI EXE launched directly; latest CLI EXE `--cli --data` help/records/exit smoke passed | Prepared launchers use the same SHA-256-matched shaded JAR. GUI needs no terminal. CLI naturally presents a text console; explicit custom launcher arguments include `--cli`. |
| Native quiz | Actual SOLID PDF → 3-question quiz → **3/3**, 100% review | Question navigation, preserved prior answer, disabled final submission with an unanswered question, full-row choices and explanation review observed. Correct/wrong labels and read-only review also checked deterministically. |
| Native timer | **15-minute real-time expiry → 900 seconds persisted** | Started 00:36:07, saved 00:51:07 Toronto. Expiry notification observed while quiz review was open. Restart preserved record. Additional pause/resume/stop saved 20 seconds; GUI daily and weekly total displayed 15 min 20 sec. Alarm code executed, but speaker output was not independently recorded/measured. |
| Native follow-up workflow | Neutral gray course rows, 25-file upload/review, original PDFs, timer status navigation and bulk calendar import checked | 3 readable syllabus/assignment files analyzed, 10 assessment suggestions. Add all immediately requested the unknown Final Exam date; Keep TBD retained uncertainty, cutoff warnings required confirmation, calendar showed named entries in every affected week. |
| Owner-requested clean workspace | Original **82 files backed up; all SHA-256 hashes match** | Original `data/` moved into ignored backup, root data started fresh for the bulk test. `.env` and external originals retained. Current workspace contains verified uploaded files and explicitly named demo deadlines/quiz/timer evidence; it is not the old database. |
| Diagrams and source mapping | 7 architecture/class + 17 sequence + 3 use-case sheets | SD01a/SD10a/SD10b/SD13 updated for this follow-up. XML parsed and affected SVG/PNG renders inspected. Diagram labels document collaborations, not proof of AI correctness. |

## Reproducible checks and local evidence

Build from source with JDK17+: `mvnw.cmd package`. The audit used cached dependencies with JDK25 and `-Daether.syncContext.named.factory=noop` for this environment's Maven locking. That flag is not an application requirement.

Raw evidence stays below ignored `codex/`; these paths are relative to the repository:

- `build/target/surefire-reports/` — 35-test XML/text results.
- `feature-audit-20261114/run-dcc9540d-fbe5-421d-a3c3-a198332e6b3d/results.tsv` — 16 passed actual API checks and generated study artifacts.
- `bulk-upload-20261114/classification-results.tsv` — 25 real PDFs, expected and returned categories. The input directory contains local copies of course PDFs, including the renamed ambiguous file.
- `cli-acceptance-20261114/run-2119c2cf-b9d3-434c-8806-5b235620eaa4/cli-transcript.txt` — 12 basic CLI groups.
- `cli-complete-20261114/run-385ef4a2-0f72-4ea4-b618-685c44faa572/transcript.txt` — 11 supplemental groups. Combined input blocks were compared against all 41 commands in the actual switch dispatcher; none was missing.
- `FeatureAudit.java`, `BulkUploadAudit.java`, `CliAudit.java`, `CliSupplementAudit.java` — Java-only audit harnesses calling the application implementation, with the actual configured API where applicable.
- `backups/data-before-bulk-20261114-002500/` and `original-data-audit-20261113.json` — the 82-file original database backup and verified hash baseline.

Do not publish raw evidence wholesale: it can contain course excerpts, generated content and personal paths. Credentials, data, runtime images, EXEs and ZIP remain excluded from Git.

## Follow-up defects and corrections

| Problem | Correction | Evidence |
| --- | --- | --- |
| Mixed uploads could inherit the selected Syllabus folder; first-prefix classification missed document purpose | Inspect names/titles locally, sample first/middle/last pages; uncertain/conflicting files receive every extracted page in 18,000-character segments and an aggregate purpose decision | All 25 actual PDFs classified correctly; renamed 20-page file fully reviewed; regression concatenates every segment and verifies no omitted text |
| Missing or duplicate AI classifications could silently become partial sorting | Require each expected UUID exactly once and reject unknown IDs/categories; apply results only after complete success and human review | Missing/duplicate response regression; native pre-upload review before any file storage |
| Timeline rows did not visually distinguish courses | Different neutral gray cell backgrounds and a gray first-column edge, resolving sorted view rows to model rows | Native gray rows plus sorted-row and monochrome-selection regression |
| Quiz layout lacked usable navigation and reliable long-choice presentation | `QuizSessionPanel` with navigator, progress, wrapping vertical choices, retained answers, explicit submit gate and explanation review | Native actual-PDF 3/3 flow; deterministic answer-jump/submit/read-only checks |
| Canceling an AI-result answer dialog displayed an empty error | Handle review cancellation separately from application failures; keep generated questions available | Native two-question generation confirmed; cancellation regression checked in final EXE |
| Bottom timer status opened undated deadlines instead of the timer | Active timer status selects Study timer; inactive status retains undated-deadline action | Latest EXE: Timeline → bottom remaining-time click → Study timer |
| GUI essay-only quiz ignored requested count; CLI quiz lacked week/all and essay/both settings | Honor essay count and selected difficulty; use course difficulty for MCQ/essay explanations; regenerate blank essay questions; expose scope/count/difficulty/type through CLI | Deterministic two-essay week test; actual CLI all/week/file and mixed types passed |

The earlier Nov13 audit also corrected date-only schedule edits, typo-equivalent deadline merges, unlinked timer briefing totals, daily record aggregation, narrow course/timer controls, view refresh selection, Markdown styling and calendar course filtering. Those changes were committed as `fdcfcd7` after approval and are retained.

## Boundaries

- Full inventory F01–F15 has combined deterministic, actual API/controller, native GUI and actual CLI evidence. Every GUI menu combination was not replayed manually. A passing sample does not guarantee arbitrary future model responses.
- Stage 1 responsibilities remain mapped, but implementation classes and some workflows differ. The [change record](patterns-and-changes.md) explicitly documents provider, helper/registry, memory, Observer and grading differences. Do not describe this as literal class-for-class identity.
- No OCR, background OS notification service or general multi-action planner. Timer/periodic reminders require the GUI open or notifications appear on next launch. Pending proposals are transient; approved data persists.
- AI source citations, answer keys, dates and categories still require review. Files still uncertain after full review stay in Other for manual choice. A scanned page cannot be fully text-reviewed without OCR.
- The measured $0.02 API dashboard amount in the cost log is a timestamped earlier snapshot. This follow-up made additional calls; refresh analytics before the final report. Development remains the separately declared USD200/month subscription.
- Demonstration videos, public links/timestamps and final Stage 2 report remain to be produced. The owner explicitly approved one new commit and push for this follow-up on 14 November 2026.
