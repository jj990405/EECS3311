# Semester Study and Deadline Management Agent

One Java 17 project with a Swing desktop app and a CLI. The source, tests, Maven wrapper, and packaging recipe are at the repository root. The original design report and diagrams are in `docs/design/`; the assignment brief is in `docs/assignment/`.

## Run

On the prepared Windows desktop package, double-click **Study Agent.exe** for the GUI or **Study Agent CLI.exe** for the console. Keep both launchers alongside `app/` and `runtime/`; the EXE alone is not the complete application. The bundled runtime means the prepared package does not need a separately installed Java. Generated launchers remain local; a fresh source clone must be built as below.

Build from a clone with JDK 17 or newer. Maven downloads dependencies on the first run. From this directory:

```powershell
.\mvnw.cmd clean package
java -jar .\codex\build\target\study-agent.jar
java -jar .\codex\build\target\study-agent.jar --cli
```

On macOS/Linux, use `sh ./mvnw clean package` and `java -jar ./codex/build/target/study-agent.jar` (add `--cli` for the CLI). Invoking the wrapper through `sh` also works when a checkout has not preserved its executable bit. The app reads `.env` and stores local data in `data/`. Build output goes to `codex/build/`.

For a local Windows desktop package, set `JAVA_HOME` to a JDK with `jpackage` and run `powershell -File packaging/build-windows.ps1`. It creates `Study Agent.exe`, `Study Agent CLI.exe`, and their `app/` and `runtime/` folders for one-click use. Generated binaries and the ZIP stay local and are excluded from Git; GitHub contains the buildable source.

Application updates publish a new hash-named JAR and atomically switch the launcher configuration. Existing JAR versions remain available to running processes; reopen the app to use the new version. Never overwrite a JAR that a running app has loaded. To update only the application code in an existing image, run `powershell -File packaging/update-app.ps1 -SourceJar codex/build/target/study-agent.jar -AppDirectory app`. Uncaught application errors are recorded locally in `codex/runtime-diagnostics/last-error.log` with the loaded image and stack trace.

The portable Java artifact is the JAR. The EXEs are Windows launchers around the same Java application. A JAR needs a compatible Java runtime; the Windows app image supplies its own. When passing custom arguments to the CLI EXE, include `--cli` explicitly, for example `& '.\Study Agent CLI.exe' --cli --data codex/demo-data`, because explicit launcher arguments replace its default arguments.

To use AI features, copy `.env.example` to `.env` and supply `LLM_API_KEY` locally. OpenAI GPT-6 Luna is the default; `LLM_BASE_URL` and `LLM_MODEL` are configurable. Do not commit `.env`.

## Workflow

AI features are grouped under **AI actions** in the top toolbar and course page. Files in the tree, course page and Files list support selection and right-click study actions. Double-click a list file to open it. A PDF's **AI** menu and page context menu offer a quiz for the current slide or a page range, study notes and a whole-file quiz. Practice quizzes use lecture materials rather than syllabus logistics. Slide quizzes use only the selected teaching pages and currently support multiple-choice questions. In the CLI, choose `pages` under `quiz create`, then enter a range such as `2-5,8`.

In Chat, `quiz`, `slide quiz` and `notes` open their actual study tools; other study questions use source Q&A. Source answers appear once in Chat, with no duplicate answer popup. Other text results open in closable right-hand tabs; the interactive quiz retains its own window. Click an underlined source filename/page in Chat to open that exact physical page in the right-hand PDF tab.

While an AI request runs, the GUI shows elapsed time, reading progress where available and **Cancel AI**. Failures are shown in a dialog and the chat. `hi` receives local guidance. Chat defaults to **Course materials**, even when a PDF is open; choose **Selected file only** or its file context action for a narrow question. Assessment preparation questions first identify the topic from syllabus/schedule evidence, then read all pages of the relevant uploaded teaching files and produce study steps, source-based exercises, pitfalls and self-check questions. Large scopes use full-coverage reading batches. A study guide that only cites the syllabus is regenerated when lecture evidence is available. Teaching recommendations are distinguished from confirmed assessment coverage. Invalid source citations trigger a fresh, validated response rather than being accepted.

Uploads now read and index every page. Plain extraction uses Java PDFBox/Office XML. Sparse or image-heavy PDF slides and embedded PPTX pictures use the configured OpenAI image API; their readings are labelled separately from the original text. This can make the first upload slower and uses API credit. The originals remain unchanged. Full source text and a persistent word-to-page index are saved in `data/artifacts/source-pages/<material-id>.txt` and `.json`; the JSON includes the original filename and physical page numbers. Reopening the app or generating another quiz reuses these files. There is no 1,500-character source prefix limit.

For previously uploaded files, choose **AI actions > Read and index all uploaded files**, or type `material index` in the CLI. New GUI/CLI uploads start preparation automatically. Unsupported or unreadable content is reported; image readings can be imperfect and must be checked against the original. Linked external PPTX images are not fetched. Large quiz scopes process all source pages in batches instead of discarding the end of each lecture.

1. Create a semester and course, then upload syllabus and study files (PDF, PPTX, DOCX, or TXT). Right-click a course or category to upload several files at once. Each destination is editable before saving. **AI sort files** reads all available source-page text; long documents are reviewed in complete segments. After upload, automatic preparation reads and indexes all pages and uses image reading when required. Review folder suggestions before applying moves. Right-click a file to move it manually. Selecting a PDF opens its original pages in a closable right-side tab; local preview needs no API key. PDF pages use a high-resolution raster matched to display scaling while retaining their normal on-screen size. Image-assisted source preparation uses the configured API separately.
2. Add/edit/delete deadlines, view the timeline or month calendar, configure due time and notification lead time, and inspect due-soon reminders. The top **Calendar** button is always available. Filter the calendar to all courses or one course; every week shows `[course] title` entries. Click a dated cell, including an empty one, to add or edit work for that day. Course difficulty breaks same-date priority ties. In the timeline, drag or Ctrl/Shift-select rows and right-click for details, editing, completion, copying or confirmed deletion; Shift+F10 also opens the menu.
3. After uploading files, choose **AI actions > Analyze files for calendar** to use the AI API on readable syllabi and assignment specifications. It shows proposed dates, source files, evidence, and warnings in a review table. Select the entries to add; no proposed deadline enters the calendar before approval. A date released with a 24-hour submission window is proposed for the following day, with a reminder to confirm the exact cutoff time. Dates without enough evidence remain TBD. The separate **Extract syllabus deadlines** and **Suggest lecture week** actions remain available for individual review. In the CLI, use `ai syllabus`, `ai week`, `proposals`, `approve`, or `reject`.
4. Use the GUI's 15-, 30-, or 60-minute study timer. Click its remaining-time text at the bottom to return to the timer tab. Pause is visibly labeled; Resume continues active time. It sounds an alarm and saves the session when time ends, or saves elapsed time on Stop. Records combine sessions into daily and weekly totals, including seconds. Use `study week` in the CLI to inspect the same persisted records. The timer is not linked to a course.
5. Use `ai references`, `ask`, `summary create`, `quiz create`, `quiz take`, `weak topics`, and `weekly brief` for material-based study assistance. Quiz settings include file/week/all lecture scope, count, difficulty and multiple-choice/essay/both; selected pages are an additional MCQ scope. The GUI has a question navigator, wrapping answer rows, answered progress and a separate explanation review. Answers are retained when moving between questions; every MCQ needs an answer before submission.
6. Use `ai schedule` for a natural-language schedule edit; its before-and-after preview is not saved. `proposals` lists pending suggestions with numbers; type `approve` or `reject` and select the corresponding number. The calendar changes only after approval. `essay create` generates a question; `essay take` proposes a grade and saves it only after approval. In the GUI, the proposed score can be corrected before approval. `self assess` and `weak advice` add confidence-based review guidance. `history` displays saved AI interactions; `history delete` removes a selected record.

On a fresh CLI workspace, type `setup` for guided semester, course, and optional syllabus setup. Course colors are assigned automatically in the CLI. Dates accept `20261231` or `2026-12-31`; invalid dates can be corrected without restarting a command. Type `help` for commands. In the GUI, dates can be typed or chosen from the calendar button. Choose a course color from seven swatches when adding a course, or right-click the course to change its color later. That color marks the course in the workspace tree and calendar. Timeline rows use a thin, muted blue/lavender/sand marker beside the course name, with a uniform dark background. Selecting a course opens its page with deadlines, materials, progress, and study actions. The GUI also offers the calendar, source Q&A, summaries, configurable quiz scope and types, editable essay grading, notifications, weekly briefing, and history.

The red **Delete all local data...** button in the workspace requires confirmation. It clears courses, uploaded copies, deadlines, study records, generated artifacts, and history for a fresh start. It does not delete `.env` or the original files you uploaded from elsewhere on your computer.

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
| F14 / SD11 | Per-topic quiz accuracy and self-assessments produce ranked weak topics; AI guidance names only uploaded lecture files. | Essay scores are kept separately from multiple-choice accuracy. |
| F15 / SD12 | Local interaction history records questions and answers, quiz results, proposal descriptions and decisions, and generated items; recent entries inform later Q&A and individual records can be deleted. | Manual UI actions are not all stored as conversation records. |

Implemented distinct GoF patterns: Facade (`AgentController`), Composite (`WorkspaceNode` hierarchy), Adapter (`HttpLlmClient` behind `LlmClient`), Observer (`DataStore` listeners), Command (`AgentCommand` and `CommandInvoker`), and Strategy (`PriorityStrategy`, `ExplanationStrategy`). `DocumentReaderFactory` additionally uses Simple Factory; it is not a GoF Factory Method hierarchy.

The [current diagram index](docs/design/implemented/README.md) contains editable architecture, class, use-case and sequence diagrams. The [development audit](docs/development/README.md) maps every original and added feature to code, checks and known limits. The [recording guide](docs/demo/recording-guide.ko.md) provides a GUI/CLI demonstration plan for the later report.

The [Stage 2 report](stage2-report.pdf) includes 21 feature mappings, six GoF patterns, updated UML, 75-test verification and direct timestamped demonstration links. Watch the [GUI demonstration](https://youtu.be/VxEf256ZtDc) or [CLI demonstration](https://youtu.be/qe8We5Za7Jc).

The [submission requirement check](docs/development/stage2-requirements-check.md) records what is verified and what still needs recording, finalization or historical evidence.
