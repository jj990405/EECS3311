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

To use AI features in the JAR, copy `.env.example` to `.env` next to `pom.xml`. For the packaged EXE, copy `.env.example` into `dist/Study Agent/` and rename that copy to `.env`. Supply your own `LLM_API_KEY` locally. The example key is intentionally empty. The default is OpenAI GPT-6 Luna; `LLM_BASE_URL` and `LLM_MODEL` are configurable. No live LLM call was tested without a key. All non-AI features and injected fake-client AI integration tests run without a key. Do not commit `.env`.

## Workflow

1. Create a semester, course, and upload a syllabus or study file (PDF, PPTX, DOCX, or TXT). Scanned PDFs without selectable text are stored but cannot feed AI features.
   Right-click a course or category in the workspace to upload several files at once. Selecting a file opens the Files tab with its locally extracted text; this preview does not need an AI key.
2. Add/edit/delete deadlines, view the timeline or month calendar, configure due time and notification lead time, and inspect due-soon reminders. Course difficulty breaks same-date priority ties.
3. Uploading a readable syllabus proposes deadlines; uploading readable lecture files proposes weeks when a syllabus is available. Review and edit each proposal before approval. In the CLI, use `ai syllabus`, `ai week`, `proposals`, `approve`, or `reject`.
4. Use the GUI's 15-, 30-, or 60-minute study timer. It sounds an alarm and saves the session when time ends. Use `study week` in the CLI to inspect daily and weekly records.
5. Use `ai references`, `ask`, `summary create`, `quiz create`, `quiz take`, `weak topics`, and `weekly brief` for material-based study assistance.
6. Use `ai schedule` for a natural-language schedule edit; review the before-and-after change before approval. `essay create` generates a question; `essay take` proposes a grade and saves it only after approval. In the GUI, the proposed score can be corrected before approval. `self assess` and `weak advice` add confidence-based review guidance. `history` displays saved AI interactions; `history delete` removes a selected record.

Type `help` in the CLI for all commands. Selecting a course opens its page with deadlines, materials, progress, and study actions. The GUI also offers the calendar, source Q&A, summaries, configurable quiz scope and types, editable essay grading, notifications, weekly briefing, and history.

## Design traceability

| Stage 1 ID / diagram | Implementation and diagram flow | Limits |
| --- | --- | --- |
| F01 / SD01 | `AgentController` → `DataStore` → `DocumentReaderFactory`/reader → `PromptBuilder`/`LlmClient`/`ResponseParser` → `AssignWeekCommand` → approval. Composite workspace tree, multiple-file upload, duplicate replacement, and syllabus-based week matching. | Scanned PDFs without selectable text need manual week assignment. |
| F02 / SD02 | Syllabus extraction → deadline drafts with weight and source evidence → `DeadlineValidator` warnings → edit, merge, reject, or approve → persistence. | Out-of-semester dates must be corrected before saving. |
| F03 / SD03 | Filtered timeline, graphical month grid with course colours, expandable weeks, clickable deadline details and edits, and CLI calendar. | Dragging deadlines between dates is not supported. |
| F04 / SD03, SD08 | `PriorityStrategy`/`PriorityRecommender` orders due date, then course difficulty. | No other strategy is exposed to users. |
| F05 / SD04 | GUI 15/30/60-minute countdown, alarm, unlinked persisted sessions, and selectable weekly records. | The timer runs while the GUI is open. |
| F06 / SD05 | `ReferenceSelector` uses deadline scope and uploaded-file snippets, then checks model-selected names against readable files. | Selection is AI dependent. |
| F07 / SD06 | Due time and lead-minute notices appear in the GUI and CLI. Missed notices appear on the next launch and are recorded as shown. | No OS background service is installed. |
| F08 / SD07 | `SchedulePlanner` parses add/edit/delete requests into reviewable commands. | One deadline action per request. |
| F09 / SD03 | Completion flags, counts, and overdue progress. | Deadline progress, not chapter-level progress. |
| F10 / SD08 | Weekly briefing uses schedule, study seconds, weak topics, and LLM. It appears on the first GUI launch of each week and falls back to collected facts if the LLM is unavailable. | — |
| F11 / SD09 | `MaterialSearcher` retrieves relevant pages; difficulty-based `ExplanationStrategy` shapes the answer; Q&A only accepts citations to pages supplied as evidence. | Citation validation cannot prove the answer's factual correctness. |
| F12 / SD09 | Cited summaries are saved, reopened, and generated in sections for long materials. | — |
| F13 / SD10 | Quiz settings include one file, a lecture week, or all lectures; question count, difficulty, and multiple-choice/essay/both. MCQs are graded locally; AI essay scores can be corrected before approval. | MCQ and essay artifacts are persisted separately. |
| F14 / SD11 | Missed quiz answers and self-assessments produce ranked weak topics; AI gives review guidance. | Ranking is a simple score based on misses and confidence. |
| F15 / SD12 | Local interaction history records AI proposals, approvals, generated items, and responses; recent entries inform later Q&A and individual records can be deleted. | Manual UI actions are not all stored as conversation records. |

Implemented patterns: Facade (`AgentController`), Composite (`WorkspaceNode` hierarchy), Factory (`DocumentReaderFactory`), Adapter (`HttpLlmClient` behind `LlmClient`), Observer (`DataStore` listeners), Command (`AgentCommand` and `CommandInvoker`), and Strategy (`PriorityStrategy`, `ExplanationStrategy`).
