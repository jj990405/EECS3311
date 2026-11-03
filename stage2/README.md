# Semester Study and Deadline Management Agent

Java 17 Swing and CLI implementation of the Stage 1 design for EECS3311. The application lives under `stage2/` in the same EECS3311 repository as the Stage 1 design.

## Run

From `stage2/` in PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 -Mode test
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 -Mode cli
powershell -NoProfile -ExecutionPolicy Bypass -File .\run.ps1 -Mode gui
```

Java 17+ is required. `run.ps1` finds `JAVA_HOME`, `javac` on `PATH`, or an installed Eclipse JustJ JDK. On macOS/Linux use `./run.sh test`, `./run.sh cli`, or `./run.sh gui`. Both scripts use the bundled Apache PDFBox 3.0.8 JAR in `lib/` for PDF text extraction. Its SHA-512 verification file is adjacent to the JAR. The application stores personal data under `data/`, which Git ignores.

To use AI features, copy `.env.example` to `.env` and supply your own `LLM_API_KEY`. The example key is intentionally empty. The default is an OpenAI-compatible DeepSeek chat-completions endpoint; `LLM_BASE_URL` and `LLM_MODEL` are configurable. No live LLM call was tested without a key. All non-AI features and injected fake-client AI integration tests run without a key. Do not commit `.env`.

## Workflow

1. Create a semester, course, and upload a syllabus or study file (PDF, PPTX, DOCX, or TXT). Scanned PDFs without selectable text are stored but cannot feed AI features.
2. Add/edit/delete deadlines, view the timeline or month calendar, configure due time and notification lead time, and inspect due-soon reminders. Course difficulty breaks same-date priority ties.
3. Use `ai syllabus` to propose deadlines and `ai week` to propose lecture-week assignment. Review with `proposals`, then `approve` or `reject`. AI never directly changes these records.
4. Use `study start`, `study pause`, `study resume`, `study stop`, and `study week` to record study time. The GUI countdown sounds an alarm and saves the session when the set duration ends.
5. Use `ai references`, `ask`, `summary create`, `quiz create`, `quiz take`, `weak topics`, and `weekly brief` for material-based study assistance.
6. Use `ai schedule` for a natural-language schedule edit; review the resulting command before approval. `essay create` generates a question; `essay take` proposes a grade and saves it only after approval. `self assess` and `weak advice` add confidence-based review guidance. `history` displays saved AI interactions; `history delete` clears them.

Type `help` in the CLI for all commands. The GUI exposes the workspace, timeline, calendar, manual reminders, timer, source Q&A, weekly briefing, syllabus/week proposals, approval, and history. Some study tools are CLI-only.

## Design traceability

| Stage 1 ID / diagram | Implementation and diagram flow | Limits |
| --- | --- | --- |
| F01 / SD01 | `AgentController` → `DataStore` → `DocumentReaderFactory`/reader → `PromptBuilder`/`LlmClient`/`ResponseParser` → `AssignWeekCommand` → approval. Composite workspace tree and local extraction. | One AI week candidate per file; no OCR for scanned PDFs. |
| F02 / SD02 | Syllabus extraction → deadline drafts → `DeadlineValidator` → `AddDeadlineCommand` → approval and persistence. | Model dates need selectable source text; duplicates are not merged automatically. |
| F03 / SD03 | Filtered timeline, editable deadlines, graphical month grid with current-week strip, and CLI calendar. | Calendar cells are read-only; edit through the deadline table. |
| F04 / SD03, SD08 | `PriorityStrategy`/`PriorityRecommender` orders due date, then course difficulty. | No other strategy is exposed to users. |
| F05 / SD04 | Pause/resume countdown timer, GUI alarm, persisted sessions and weekly totals. | One running timer at a time; CLI countdown is not animated. |
| F06 / SD05 | `ReferenceSelector` uses deadline scope and uploaded-file snippets, then checks model-selected names against readable files. | Selection is AI dependent. |
| F07 / SD06 | `ReminderService` checks each deadline's due time and lead minutes. CLI checks on launch; GUI checks every minute and alerts in the status bar. | Alerts require the app to be running; no OS background service. |
| F08 / SD07 | `SchedulePlanner` parses add/edit/delete requests into reviewable commands. | One deadline action per request. |
| F09 / SD03 | Completion flags, counts, and overdue progress. | Deadline progress, not chapter-level progress. |
| F10 / SD08 | Weekly briefing uses schedule, study seconds, weak topics, and LLM. | Generated on demand; not scheduled. |
| F11 / SD09 | `MaterialSearcher` retrieves relevant pages; difficulty-based `ExplanationStrategy` shapes the answer; Q&A only accepts citations to pages supplied as evidence. | Citation validation cannot prove the answer's factual correctness. |
| F12 / SD09 | Cited summaries are saved and can be listed later. | A summary covers one material at a time. |
| F13 / SD10 | AI multiple-choice and essay-question generation, local MCQ grading, saved attempts; AI essay grade proposal requires approval. | Essay grades can be approved or rejected, but the proposed numeric score cannot be edited. |
| F14 / SD11 | Missed quiz answers and self-assessments produce ranked weak topics; AI gives review guidance. | Ranking is a simple score based on misses and confidence. |
| F15 / SD12 | Local interaction history records AI proposals, approvals, generated items, and responses; recent entries inform later Q&A and history can be deleted. | It does not capture every manual UI action. |

Implemented patterns: Facade (`AgentController`), Composite (`WorkspaceNode` hierarchy), Factory (`DocumentReaderFactory`), Adapter (`HttpLlmClient` behind `LlmClient`), Observer (`DataStore` listeners), Command (`AgentCommand` and `CommandInvoker`), and Strategy (`PriorityStrategy`, `ExplanationStrategy`). These classes follow the Stage 1 class/sequence diagram roles while retaining the explicit differences above.
