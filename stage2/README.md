# Semester Study and Deadline Management Agent

Java 17 Swing and CLI implementation of the Stage 1 design for EECS3311. The application lives under `stage2/` in the same EECS3311 repository as the Stage 1 design.

## Run

On Windows, download `dist/Study-Agent-Windows-x64.zip`, extract it, then open `Study Agent.exe` for the GUI or `Study Agent CLI.exe` for the terminal interface. Keep the entire `Study Agent` folder together: it includes the Java runtime, so the computer running it does not need a separate Java installation.

The standard Maven commands from `stage2/` are:

```text
Windows:  .\mvnw.cmd test
Windows:  .\mvnw.cmd package
Windows:  java -jar target\study-agent.jar --cli
macOS/Linux: ./mvnw test
macOS/Linux: ./mvnw package
macOS/Linux: java -jar target/study-agent.jar --cli
```

Run the JAR without `--cli` to open the GUI. Building from source requires JDK 17+ and an internet connection on the first Maven run. The Maven Wrapper downloads Maven if needed, and Maven fetches Apache PDFBox and JUnit. To rebuild the Windows EXE, use PowerShell with a JDK that includes `jpackage`:

```powershell
.\mvnw.cmd clean package
$packageOutput = 'build\app-image-' + [guid]::NewGuid().ToString('N')
New-Item -ItemType Directory -Force build\package-input, $packageOutput, dist | Out-Null
Copy-Item target\study-agent.jar build\package-input\study-agent.jar
jpackage --type app-image --name 'Study Agent' --input build\package-input --main-jar study-agent.jar --main-class eecs3311.studyagent.Main --dest $packageOutput --java-options '"-Dproject2.root=$ROOTDIR"' --add-launcher 'Study Agent CLI=packaging\cli-launcher.properties'
Copy-Item .env.example (Join-Path $packageOutput 'Study Agent\.env.example')
Compress-Archive -LiteralPath (Join-Path $packageOutput 'Study Agent') -DestinationPath dist\Study-Agent-Windows-x64.zip -Force
```

The packaged EXE stores data in its own `data/` directory. Git ignores generated build and data directories.

To use AI features in the JAR, copy `.env.example` to `.env` next to `pom.xml`. For the packaged EXE, copy `.env.example` into `dist/Study Agent/` and rename that copy to `.env`. Supply your own `LLM_API_KEY` locally. The example key is intentionally empty. The default is an OpenAI-compatible DeepSeek chat-completions endpoint; `LLM_BASE_URL` and `LLM_MODEL` are configurable. No live LLM call was tested without a key. All non-AI features and injected fake-client AI integration tests run without a key. Do not commit `.env`.

## Workflow

1. Create a semester, course, and upload a syllabus or study file (PDF, PPTX, DOCX, or TXT). Scanned PDFs without selectable text are stored but cannot feed AI features.
   Right-click a course or category in the workspace to upload several files at once. Selecting a file opens the Files tab with its locally extracted text; this preview does not need an AI key.
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
| F03 / SD03 | Filtered timeline, graphical month grid with course colours, expandable weeks, clickable deadline details and edits, and CLI calendar. | Dragging deadlines between dates is not supported. |
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
