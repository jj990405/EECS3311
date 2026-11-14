# Stage 2 Implementation Report

Junhyeok Park 216576324 EECS3311

[github.com/jj990405/EECS3311](https://github.com/jj990405/EECS3311)

## 1 Project overview and implementation status

The application helps university students organize semester materials, extract assessment deadlines, prioritize work and study from their own uploaded sources. A Swing GUI and an interactive CLI share one Java application controller. It stores original materials, approved deadlines, study records, generated notes and quizzes, and selected interaction history locally.

The Java implementation covers the Stage 1 feature IDs F01-F15, with the structural and workflow changes declared in Section 4. It also adds reviewed file classification, original PDF side tabs, bulk calendar review with daily missing-date reminders, easier setup, confirmed reset and timeline context actions. Six distinct GoF patterns are identified from actual collaborators; Simple Factory is described separately and is not counted as a seventh GoF pattern.

The working AI path is more than prompt forwarding. Java extracts and retrieves source pages, builds task-specific context, validates identifiers/dates/structured responses, creates pending commands for data-changing proposals, and persists approved results. Recent interaction history informs later source Q&A. The LLM cannot access the filesystem or mutate the store directly.

### Execution and configuration

The source targets Java 17. The prepared Windows GUI launches by double-clicking Study Agent.exe; Study Agent CLI.exe opens a text console. Both need their app and runtime folders. The source repository intentionally excludes generated launchers, runtime images and ZIPs. A reviewer can build the shaded JAR from the Maven wrapper:

```text
mvnw.cmd clean package
java -jar codex/build/target/study-agent.jar
java -jar codex/build/target/study-agent.jar --cli
```

The source dependencies include FlatLaf 3.7.2 and Apache PDFBox 3.0.8. Office documents are read through Java OOXML helpers. AI requests use Java HTTP through HttpLlmClient. Copy .env.example to .env and provide LLM_API_KEY locally. The configured default application model is gpt-6-luna through an OpenAI-compatible endpoint; local model/endpoint settings may override it. The application code is Java; document-generation tools are development utilities outside the running app.

The Windows image and CLI were exercised on Windows. A JAR needs a compatible runtime; cross-platform Java does not mean the Windows EXE itself runs on every operating system. macOS/Linux desktop behavior was not manually validated in this audit.

### Implementation scope

Approved data survives restart. AI deadline, lecture-week, schedule and essay-grade proposals require explicit review before changing the relevant student data. Notes and quizzes are saved when explicitly generated. Citations, dates, categories and answer keys remain available for student review.

Source preparation combines extracted text with model-assisted image reading. Notifications are delivered while the application is open or at the next launch. Schedule requests are reviewed one action at a time, with pending proposals held in memory. The GUI timer records total study time. Essay grades and MCQ topic statistics are stored separately; multi-file MCQ metadata retains the first material identifier.

## 2 Feature to implementation mapping

All Java class/file names in this section refer to src/main/java/eecs3311/studyagent/ unless another path is stated. GUI/CLI clients call AgentController; DataStore owns approved entity persistence, StudyArtifacts owns generated study items/results, and InteractionHistory owns the JSONL conversation/event record. Each item below states its original reference, entry point, collaboration, patterns and verification boundary. Each demonstration link opens the actual feature segment in Section 7.

### F01 Workspace/materials  -  implemented, modified upload flow

**Reference and status:** Stage 1 F01. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC01, UC17; SD01a, SD01b. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** New semester/course, Add files; workspace right-click. CLI `setup`, `semester add`, `course add`, `material add`, `tree`, `ai week`

**Classes and collaboration:** `AgentController.createSemester/createCourse/uploadMaterial` → `DataStore` validates/copies/persists; `DocumentReaderFactory.getReader` → PDF/PPTX/DOCX/TXT reader extracts page text. `WorkspaceTree.build` produces recursive nodes. Week suggestion uses syllabus context → parser → `AssignWeekCommand` → approval.

**Mode and design patterns:** Hybrid; Facade, Composite, Simple Factory, Adapter, Command

**Verification and scope:** Real 9-page syllabus and synthetic 2-page lecture uploaded; real API week 3 suggestion approved. Readers covered by automated checks. PDF has original-page preview; other originals open externally. Upload prepares every physical page through StudySources and saves TXT plus a SourceIndex word/page index. Sparse/image-heavy PDF pages and embedded PPTX pictures receive model-assisted reading; originals remain unchanged. Actual 25-file / 1,055-page preparation preserved every original page; second preparation added zero image calls.

**Demonstration:** [GUI 00:00](https://youtu.be/VxEf256ZtDc?t=0); [CLI 00:00](https://youtu.be/qe8We5Za7Jc?t=0).

### F02 Syllabus deadlines  -  implemented

**Reference and status:** Stage 1 F02. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC02, UC17; SD02. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** AI → Extract syllabus deadlines; review proposal edit/merge/approve/reject. CLI `ai syllabus`, `proposals`, `approve`, `reject`

**Classes and collaboration:** `suggestSyllabusDeadlines` → `PromptBuilder`/`LlmClient`/`ResponseParser.deadlines` → `DeadlineValidator` → deferred add/edit commands. Revised drafts and merge preserve approval boundary.

**Mode and design patterns:** Hybrid; Adapter, Command, Facade

**Verification and scope:** Real syllabus yielded 10 assessment proposals; Lab Test 2 / Nov 18 checked and approved. Typo-equivalent merge regression covered. AI dates/weights still require source review; invalid semester dates cannot save.

**Demonstration:** [GUI 06:49](https://youtu.be/VxEf256ZtDc?t=409); [CLI 01:06](https://youtu.be/qe8We5Za7Jc?t=66).

### F03 Calendar/timeline  -  implemented, expanded

**Reference and status:** Stage 1 F03. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC03; SD03. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Timeline; persistent top Calendar button; all/one-course filter; click day or deadline. CLI `timeline`, `calendar`, deadline add/edit/delete

**Classes and collaboration:** `schedule` filters period/course/query and delegates ordering; `CalendarPanel` renders all weeks with `[course] title`; `showCalendarDay` opens add/edit even on empty dates; store saves changes.

**Mode and design patterns:** Deterministic; Facade, Strategy, Observer

**Verification and scope:** Runtime date/search/order checks; GUI all/course filters and blank-day addition; automated all-week labels and empty-cell callback. No drag-and-drop rescheduling.

**Demonstration:** [GUI 09:27](https://youtu.be/VxEf256ZtDc?t=567); [CLI 01:55](https://youtu.be/qe8We5Za7Jc?t=115).

### F04 Priority  -  implemented

**Reference and status:** Stage 1 F04. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC04; SD03, SD08. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Timeline/current-week ordering; weekly brief. CLI `timeline`

**Classes and collaboration:** `PriorityRecommender.order` implements `PriorityStrategy`: earlier dates first, then harder course on equal date. Student chooses workload.

**Mode and design patterns:** Deterministic; Strategy

**Verification and scope:** Runtime high/low same-date assertion. One current priority implementation; no automatic workload allocation.

**Demonstration:** [GUI 09:27](https://youtu.be/VxEf256ZtDc?t=567); [CLI 01:42](https://youtu.be/qe8We5Za7Jc?t=102).

### F05 Study timer/records  -  implemented

**Reference and status:** Stage 1 F05. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC05, UC06; SD04. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Study timer: 15/30/60, Pause/Resume/Stop/Records. CLI `study week` reads saved records

**Classes and collaboration:** `StudyTimer` counts active time; `MainWindow` polls and sounds expiry alarm; `stopStudy` → `DataStore.recordStudySession`; `TimerRecordPanel.refresh` and CLI aggregate by day and week. Timer is deliberately not course-linked.

**Mode and design patterns:** Deterministic; Facade; store Observer updates views

**Verification and scope:** Native countdown, pause/resume/early stop and bottom-status navigation checked. A full real-time 15-minute run ended with Saved 900 seconds; persisted session and daily/weekly totals checked. Alarm code executed; audible speaker output was not independently measured. CLI timer execution was not in Stage 1; records are.

**Demonstration:** [GUI 14:30](https://youtu.be/VxEf256ZtDc?t=870); [CLI 05:24](https://youtu.be/qe8We5Za7Jc?t=324); [GUI full timer 27:53](https://youtu.be/VxEf256ZtDc?t=1673).

### F06 Reference files  -  implemented

**Reference and status:** Stage 1 F06. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC07; SD05. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** AI → Suggest reference files; course Reference files. CLI `ai references`

**Classes and collaboration:** `suggestReferences` builds deadline scope and readable candidates → `ReferenceSelector.select` → AI JSON → validates chosen names against uploaded files.

**Mode and design patterns:** Hybrid; Facade, Adapter

**Verification and scope:** Actual API selected the uploaded SOLID lecture. No readable references is handled; selection usefulness remains model-dependent.

**Demonstration:** [GUI 15:34](https://youtu.be/VxEf256ZtDc?t=934); [CLI 02:41](https://youtu.be/qe8We5Za7Jc?t=161).

### F07 Deadline notices  -  implemented

**Reference and status:** Stage 1 F07. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC08; SD06. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Schedule notification configuration; GUI startup/minute checks; reminders. CLI `deadline notify`, `notifications`, `reminders`

**Classes and collaboration:** `ReminderService` evaluates due/lead times; `unseenNotifications` filters persisted shown markers; `markNotificationShown` records them.

**Mode and design patterns:** Deterministic; Facade

**Verification and scope:** Due notice deduplication tested with explicit times; CLI notification configuration exercised. GUI must be open or notices appear at next launch; no OS background service.

**Demonstration:** [GUI 13:18](https://youtu.be/VxEf256ZtDc?t=798); [CLI 02:05](https://youtu.be/qe8We5Za7Jc?t=125).

### F08 Natural-language edits  -  implemented

**Reference and status:** Stage 1 F08. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC09, UC17; SD07. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Chat → Change schedule; AI → Request schedule change. CLI `ai schedule`

**Classes and collaboration:** `SchedulePlanner.plan` gives existing deadlines to LLM → parse add/edit/delete/clarify → validate studentship/date → deferred command in `CommandInvoker`; approval executes. Date-only edits retain omitted title/type/scope.

**Mode and design patterns:** Hybrid; Adapter, Command

**Verification and scope:** Real API move Nov20→21 before/after approval; actual CLI edit and approval. Date-only regression added. One action per request; multi-action workload planning unsupported.

**Demonstration:** [GUI 23:23](https://youtu.be/VxEf256ZtDc?t=1403); [CLI 05:33](https://youtu.be/qe8We5Za7Jc?t=333).

### F09 Progress  -  implemented

**Reference and status:** Stage 1 F09. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC10; SD03. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Deadline completion action; course progress. CLI `complete`, `progress`

**Classes and collaboration:** `DataStore.setCompleted` persists; `AgentController.progress` computes total/completed/overdue; listener refreshes UI.

**Mode and design patterns:** Deterministic; Observer, Facade

**Verification and scope:** Runtime and CLI completion/progress checked. Tracks deadlines, not chapter completion.

**Demonstration:** [GUI 12:28](https://youtu.be/VxEf256ZtDc?t=748); [CLI 02:27](https://youtu.be/qe8We5Za7Jc?t=147).

### F10 Weekly brief  -  implemented

**Reference and status:** Stage 1 F10. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC11; SD08. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** AI → Weekly brief; first GUI start of week. CLI `weekly brief`

**Classes and collaboration:** `weeklyBriefing` collects ordered deadlines, total weekly timer seconds and weakness records → LLM; deterministic fallback if unavailable. `weeklyBriefDue/markWeeklyBriefShown` persists weekly marker.

**Mode and design patterns:** Hybrid; Strategy, Adapter, Facade

**Verification and scope:** Real API brief includes upcoming item; regression ensures unlinked timer seconds included. Automatic first-week marker and fallback covered by checks.

**Demonstration:** [GUI 25:08](https://youtu.be/VxEf256ZtDc?t=1508); [CLI 06:22](https://youtu.be/qe8We5Za7Jc?t=382).

### F11 Grounded Q&A  -  implemented

**Reference and status:** Stage 1 F11. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC12; SD09a. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Chat → Ask sources; AI actions → Ask sources; file context menu. CLI `ask`

**Classes and collaboration:** Course materials is the default scope; Selected file only or the file context action explicitly pins `answerFromMaterial`. `MaterialSearcher.search` queries saved SourceIndex pages across the course; assessment queries add complete syllabus and saved scope. Preparation requests use AssessmentStudySources to select and read all relevant teaching pages, then require lecture citations and practical exercises. Only prior same-course student questions enter memory. Invalid citations regenerate against the exact allowed physical pages, up to three attempts. `AiFeedback` routes exact tool commands before Q&A and handles greetings/failures; source answers render once in Chat; generic text results use side tabs.

**Mode and design patterns:** Hybrid; Adapter, Strategy, Facade

**Verification and scope:** Actual lecture Q&A returned cited Factory/Adapter answers in native GUI. The complete-source audit answered the exact Lab Test 2 and final questions with syllabus-aware context and validated citations. Greetings, hi -> quiz dispatch and answer-window maximize/restore checked. Earlier CLI source acceptance remains applicable. Citation provenance is not proof of factual correctness.

**Demonstration:** [GUI 15:34](https://youtu.be/VxEf256ZtDc?t=934); [CLI 02:54](https://youtu.be/qe8We5Za7Jc?t=174).

### F12 Summaries/study notes  -  implemented, batched reading UI

**Reference and status:** Stage 1 F12. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC13; SD09b. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** AI actions → Create/Saved study notes; file/PDF context menu; Chat notes. CLI `summary create/list`

**Classes and collaboration:** `generateSummary` uses `StudyPages.batches` to read every nonblank source character in page-labelled batches of at most 12,000 characters, then combines notes and saves through `StudyArtifacts`; `StudyNotesPanel` renders the saved guide. GUI worker shows progress/cancel.

**Mode and design patterns:** Hybrid; Adapter, Facade

**Verification and scope:** Actual 66-page Creational lecture: 3 input batches plus synthesis, about 22 seconds in controller audit. Latest native Chat notes completed an 82-page Structural Patterns lecture in 4 source batches plus synthesis. Regression preserves all source characters/page numbers. StudySources supplies all prepared pages, including labelled model-assisted image readings. Model-written facts still require source review.

**Demonstration:** [GUI 17:51](https://youtu.be/VxEf256ZtDc?t=1071); [CLI 03:27](https://youtu.be/qe8We5Za7Jc?t=207).

### F13 Quiz/essay  -  implemented, slide scope added

**Reference and status:** Stage 1 F13. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC14, UC17; SD10a, SD10b. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** AI actions → Practice quiz; file/PDF right-click quiz; PDF AI menu. CLI `quiz create/take` with file/pages/week/all; essay commands retained

**Classes and collaboration:** `generateQuiz` uses only lecture materials and all their prepared pages for file/week/all; syllabus files are excluded, and syllabus-only page quizzes are rejected before any API call; long corpora receive full-source fact batches instead of prefix truncation. An unassigned week offers file selection in the GUI or all-lecture fallback in the CLI; `generateSlideQuiz` uses `StudyPages.parse/selected` to validate and send only chosen physical pages (MCQ only). `StudyArtifacts.grade` compares saved answer indices. Essay AI grade remains a correctable, explicitly approved command.

**Mode and design patterns:** Hybrid; Adapter, Command, Facade

**Verification and scope:** Actual complete-source DBC and Architecture quizzes plus a combined 22-lecture quiz generated three questions each. A newly uploaded 58-page DBC file completed native quiz generation, answering and 3/3 review. Native selected-slide quiz generated, answered and graded 1/1; final prompt rejects content-only outlines and generated a concept question from actual lecture p.21. Regressions verify outside slides never enter the prompt, invalid ranges fail, CLI pages scope saves a real quiz, and prior essay count/difficulty checks pass. Earlier 3-question navigation and approved 85 essay evidence retained. AI answer keys need review; MCQ/essay analytics remain separate and multi-file metadata retains the first material ID.

**Demonstration:** [GUI 18:56](https://youtu.be/VxEf256ZtDc?t=1136); [CLI 03:51](https://youtu.be/qe8We5Za7Jc?t=231); [CLI essay 05:57](https://youtu.be/qe8We5Za7Jc?t=357).

### F14 Weak topics  -  implemented

**Reference and status:** Stage 1 F14. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC15; SD11. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Study → Weak topics/Self assess/Review advice. CLI `weak topics`, `self assess`, `weak advice`

**Classes and collaboration:** `WeakTopicAnalyzer.statistics/analyze` combines MCQ topic accuracy with confidence; `weakTopicAdvice` sends ranked context to LLM and checks suggested file names.

**Mode and design patterns:** Hybrid; Facade, Adapter

**Verification and scope:** Wrong quiz results + confidence produce ranked topics; actual guidance and CLI checked. Essay marks do not feed MCQ topic accuracy.

**Demonstration:** [GUI 21:51](https://youtu.be/VxEf256ZtDc?t=1311); [CLI 04:39](https://youtu.be/qe8We5Za7Jc?t=279).

### F15 History/memory  -  implemented

**Reference and status:** Stage 1 F15. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC16; SD12. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** History menu view/delete. CLI `history`, `history delete`

**Classes and collaboration:** `InteractionHistory.add/list/recent/delete` persists JSONL events; recent same-course student questions contextualize Q&A separately from source evidence; previous generated answers and their citations are not reused as evidence.

**Mode and design patterns:** Hybrid; Facade

**Verification and scope:** Restart restored artifacts/history; selected deletion checked. Not every manual GUI action is a conversational event. Pending unapproved commands are in memory only.

**Demonstration:** [GUI 25:46](https://youtu.be/VxEf256ZtDc?t=1546); [CLI 06:36](https://youtu.be/qe8We5Za7Jc?t=396).

### N01 Reviewed file classification and moves

**Reference and status:** New feature. Implemented as an extension to the Stage 1 core.

**Affected design trace:** New UC18; SD13; extends F01. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Add files review table; AI classification; file right-click manual move. Fresh-workspace native 25-PDF upload and actual API run both yielded 1 syllabus, 22 lecture files and 2 assignment specifications, all correct against a manual purpose-based check. An ambiguous renamed 20-page document received full review. Missing/duplicate/foreign AI IDs fail without partial application.

**Classes and collaboration:** `UploadReviewDialog` inspects names/document titles without API; `DocumentClassification` sends complete page text for small batches and reads every extracted page in segments for long sources; no 1,500-character front sample remains; shared controller methods classify before upload or before moving stored files

**Mode and design patterns:** Hybrid; Facade, Adapter

**Verification and scope:** AI can be wrong; reviewed Apply action is the approval. CLI can upload into chosen category but has no batch sorting UI.

**Demonstration:** [GUI 02:14](https://youtu.be/VxEf256ZtDc?t=134).

### N02 On-demand source workspace

**Reference and status:** New feature. Implemented as an extension to the Stage 1 core.

**Affected design trace:** UC01, UC03, UC13; SD01a, SD03, SD09b; extends F01/F03/F12. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Click PDF; next/previous/fit controls; × closes; all closed restores main width. Native original two-page preview and paging checked.

**Classes and collaboration:** `MainWindow.showViewerTab/closeMaterialTab`, `PdfPreviewPanel`; split pane expand/restore and closable PDF/files/calendar/notes tabs

**Mode and design patterns:** Deterministic; Facade

**Verification and scope:** Text extraction is for AI; preview uses actual rendered PDF pages. Other original formats use external viewers.

**Demonstration:** [GUI 03:12](https://youtu.be/VxEf256ZtDc?t=192).

### N03 Bulk calendar import and undated reminders

**Reference and status:** New feature. Implemented as an extension to the Stage 1 core.

**Affected design trace:** New UC19; UC08, UC17; SD14; extends F02/F07. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Analyze files for calendar after upload; inspect evidence; approve bulk items. Automated import/duplicate/next-day reminder checks plus actual API extraction.

**Classes and collaboration:** `suggestCalendarFromFiles`, review table/Add all/Add checked, per-TBD date/unknown/skip, duplicate checks; `unseenUndatedReminders` daily markers

**Mode and design patterns:** Hybrid; Facade, Adapter, Command

**Verification and scope:** GUI-only bulk interaction; CLI individual proposals cover core extraction. Daily reminder requires app open or next launch.

**Demonstration:** [GUI 06:49](https://youtu.be/VxEf256ZtDc?t=409).

### N04 Easier setup and date/color selection

**Reference and status:** New feature. Implemented as an extension to the Stage 1 core.

**Affected design trace:** UC01, UC03; SD01a, SD03; extends F01/F03. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** New semester/course dialogs, date calendar button, color swatches and course context menu; CLI setup. Numeric `20261122` accepted in live CLI; date validation/color choice tests; native calendar date addition checked.

**Classes and collaboration:** GUI date input → `DatePickerDialog`/`FlexibleDate`; course color choice → `CourseColors`; CLI `setup` → shared controller semester/course creation

**Mode and design patterns:** Deterministic; Facade

**Verification and scope:** Supports validated Gregorian local dates; invalid dates and dates outside the semester are rejected. No natural-language date parser is claimed.

**Demonstration:** [GUI 00:00](https://youtu.be/VxEf256ZtDc?t=0); [CLI 00:24](https://youtu.be/qe8We5Za7Jc?t=24).

### N05 Confirmed full reset

**Reference and status:** New feature. Implemented as an extension to the Stage 1 core.

**Affected design trace:** New UC20 in UC-A; MainWindow.clearAllData -> AgentController.clearAllData -> DataStore.clearAllData. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Isolated automated reset checks; confirmation cancellation checked separately in native GUI.

**Classes and collaboration:** Red button → `MainWindow.clearAllData` → `AgentController.clearAllData` clears store/artifacts/history/proposals

**Mode and design patterns:** Deterministic; Facade, Observer

**Verification and scope:** Student requested a clean workspace on Nov14: original 82 files moved to ignored backup, with every SHA-256 verified; fresh data used for the native bulk run. Local `.env` and external originals preserved. GUI reset confirmation/persistence checks remain covered separately.

**Demonstration:** [GUI 29:32](https://youtu.be/VxEf256ZtDc?t=1772).

### N06 Timeline selection and context actions

**Reference and status:** New feature. Implemented as an extension to the Stage 1 core.

**Affected design trace:** New UC21; SD15; extends F03/F09. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Drag or Ctrl/Shift-select rows; right-click or Shift+F10 for single edit/details and bulk completion/copy/delete. Native two-row completion and incompletion restored; delete confirmation canceled; sorted snapshot regression passed.

**Classes and collaboration:** `MainWindow.timelineMenu` captures `TimelineSelection.ids` after view-to-model conversion; `setDeadlinesCompleted/deleteDeadlines` call shared `AgentController`/`DataStore` operations; `copyDeadlines` writes TSV to the Java clipboard

**Mode and design patterns:** Deterministic; Facade, Observer

**Verification and scope:** GUI-only convenience over shared deadline operations; dragging selects rows rather than rescheduling dates. Deletion defaults to Cancel and needs explicit confirmation.

**Demonstration:** [GUI 12:28](https://youtu.be/VxEf256ZtDc?t=748).

## 3 Implemented design patterns

The implementation applies six distinct GoF pattern categories. The roles and methods below show how their collaborators solve the design problems; repeated Strategy uses are counted as one category. Appendix A includes the current architecture and UML class sheets. Editable sources and all affected use-case/sequence sheets are in docs/design/implemented/.

### Facade

**Roles and methods:** `AgentController`; clients `MainWindow`, `CommandLineInterface`; e.g. `uploadMaterial`, `answerQuestion`, `planScheduleChange`, `approveProposal`

**Problem and implementation:** Both interfaces need consistent storage, validation, retrieval and AI orchestration. They call a common application boundary rather than reimplementing each workflow.

**Stage 1 alignment:** Role retained; some planned tool services consolidated into direct controller/helper calls.

### Composite

**Roles and methods:** Component `WorkspaceNode.label/children`; composite `WorkspaceBranch`; leaf `WorkspaceLeaf`; builder `WorkspaceTree.build`

**Problem and implementation:** Semesters, courses and category branches contain children; files have an empty child list. GUI and CLI recursively traverse the same interface.

**Stage 1 alignment:** Role retained as a view projection. `WorkspaceBranch.Kind` replaces separate persisted entity-composite classes.

### Adapter

**Roles and methods:** Target `LlmClient.complete`; adapter `HttpLlmClient.complete`; adaptee boundary HTTPS chat-completions protocol

**Problem and implementation:** The adapter translates application prompts to authenticated JSON HTTP and extracts response content. Task services depend on the small target interface; tests can substitute it.

**Stage 1 alignment:** Configurable protocol adapter replaces the planned vendor-specific `DeepSeekClient`. No claim that a separate SDK adapter class exists.

### Observer

**Roles and methods:** Subject `DataStore.addChangeListener/changed`; callback contract `Runnable`; `MainWindow` registers via `AgentController.addChangeListener`

**Problem and implementation:** Store mutations notify GUI callbacks, which dispatch refresh on the Swing EDT. The store does not need to know concrete GUI widgets.

**Stage 1 alignment:** Participants changed. Timer is polled with Swing Timer; do not claim `StudyTimer` implements a custom Subject or `TimerPanel` implements Observer. No listener detach method currently.

### Command

**Roles and methods:** `AgentCommand.execute`; add/edit/delete/week command records; invoker `CommandInvoker.propose/approve/reject/replace`; receiver `DataStore`

**Problem and implementation:** AI suggestions must not mutate the schedule before review. Commands encapsulate the delayed receiver operation; approval executes it, rejection removes it. Essay-grade updates use an `AgentCommand` lambda.

**Stage 1 alignment:** Role retained. `EditDeadlineCommand` replaces planned name `UpdateDeadlineCommand`; grade command uses lambda instead of named `UpdateGradeCommand`. Pending queue is transient.

### Strategy

**Roles and methods:** `PriorityStrategy.order` + `PriorityRecommender`; `ExplanationStrategy.instruction/forDifficulty` returns low/medium/high lambdas

**Problem and implementation:** Ordering policy is separate from filtering; course difficulty selects a different explanation policy without rewriting the Q&A pipeline.

**Stage 1 alignment:** Priority/explanation policies retained. One priority implementation exists. No `GradingStrategy` family: MCQ grading is deterministic in `StudyArtifacts`, essay grading is an AI workflow.

### Simple Factory (additional, not counted among the six)

**Roles and methods:** `DocumentReaderFactory.getReader` → `PdfReader/PptxReader/DocxReader/TxtReader` implementing `DocumentReader`

**Problem and implementation:** Selects the correct extraction implementation from the file extension and centralizes construction.

**Stage 1 alignment:** Matches the course's general Factory description. This is not a GoF Factory Method inheritance hierarchy or Abstract Factory.

## 4 Changes and rationale

The following change record traces each original design to its implemented behavior, reason and affected responsibilities. Retained feature IDs connect the Stage 1 design, Java classes, updated diagrams and demonstrations.

### `LLMClient`/`DeepSeekClient`  ->  `LlmClient`/configurable `HttpLlmClient`

**Reason:** Student chose OpenAI; preserve provider boundary

**Impact:** AI workflows remain behind Adapter. Model/endpoint configured locally; all AI feature sequences updated.

### `ToolManager`, `Tool`, `ToolResult` registry  ->  explicit Java helper calls from facade/planner

**Reason:** Current finite workflows do not need a generic dynamic registry

**Impact:** Retrieval, validation and planning still execute Java work around the LLM. No dynamic tool-discovery subsystem is claimed. Affects F02/F06/F08/F11/F14.

### `Planner`  ->  `SchedulePlanner`

**Reason:** Separate schedule interpretation from controller and persistence

**Impact:** F08 supports add/edit/delete/clarification, review and one action per request.

### `MemoryManager`/`InteractionRecord`  ->  `InteractionHistory.Entry` + `StudyArtifacts`

**Reason:** Separate conversation context from generated items/results

**Impact:** F11/F15 retain recent memory and persistent records; F12–F14 use artifact persistence.

### Named `FileNameValidator` tool  ->  local checks in `ReferenceSelector`, Q&A and advice boundaries

**Reason:** Validate the model output against the exact candidate set supplied

**Impact:** F06/F11/F14 reject/ignore unknown source identifiers; no independent factual-proof checker.

### Timer Subject/Observer  ->  store callbacks + Swing timer polling

**Reason:** Keep countdown rendering on EDT and store mutation notifications shared across screens

**Impact:** F05 remains countdown/pause/resume/alarm/records; Observer's subject changed to `DataStore`.

### Named grading strategies  ->  local MCQ grading + AI essay draft and approved score

**Reason:** Separate exact answer comparison from reviewed model judgment

**Impact:** F13 functionality remains, but grading Strategy claim removed. Explanation/priority retain Strategy.

### Earlier explicit AI-only upload flow  ->  complete preparation after upload, with separate reviewed sort/calendar actions

**Reason:** Student later expressly requested full reading/indexing at initial upload and authorized full text/image transmission to OpenAI

**Impact:** F01 upload now prepares complete text/image sources and local indexes; N01 classification and N03 calendar changes still require review. CLI upload prepares sources and retains its reviewed week proposal.

### Nonselected calendar weeks show dots  ->  all weeks show course/title labels + course filter + day editor

**Reason:** Make deadlines usable without selecting every week

**Impact:** F03 keeps month navigation and adds empty-day create/edit. Affected `CalendarPanel/MainWindow`, SD03.

### Paragraph summary popups  ->  saved study guide tab with headings/bullets/bold and source button

**Reason:** Student needs usable study material beside original PDF

**Impact:** F12 retained; `StudyNotesPanel`, SD09b; generated facts require review.

### Wide quiz option popups  ->  `QuizDialog` hosting `QuizSessionPanel` with question navigation, wrapping choices, progress and review

**Reason:** Long choices previously exceeded screen width

**Impact:** F13 retained with file/week/all scope, count, difficulty and type; MCQ and essay stored separately.

### Raw RGB/date input  ->  swatches/automatic color and typed compact date/calendar picker

**Reason:** Remove unnecessary setup knowledge and strict punctuation

**Impact:** N04 extends F01/F03. Color identifies course in tree/calendar; timeline uses a thin muted marker with no red/green or course-tinted background.

### Fixed viewer  ->  optional split workspace with closable PDF/calendar/notes tabs

**Reason:** Student requested source alongside main work and full width after closing tabs

**Impact:** N02, `MainWindow/PdfPreviewPanel`; originals stay unchanged.

### Individual extraction only  ->  explicit bulk review/Add all/TBD decisions and daily unknown-date reminder

**Reason:** Reduce repetitive approval while keeping date uncertainty visible

**Impact:** N03 extends F02/F07; duplicate prevention and date validation still run.

### Short-prefix classification  ->  complete source text, split into full-coverage segments when long

**Reason:** Student found mixed bulk uploads inaccurate, then requested removing the 1,500-character limit entirely

**Impact:** N01; shared `DocumentClassification`, pre-upload `UploadReviewDialog`, strict complete-response validation; no sorting applies before review.

### No reset/classification flow  ->  reviewed category moves and confirmed local reset

**Reason:** Recover from wrong folders and restart a workspace

**Impact:** N01/N05 additions; stored copies move, original external files and `.env` stay.

### Scattered AI actions and display-only file rows  ->  grouped AI menus and selectable contextual file rows

**Reason:** Student found course buttons/file selection unclear and wanted file/slide-specific actions

**Impact:** F01/F11-F13, N02; `MainWindow`, `CoursePage`, `MaterialList`, `ChatPanel`, `PdfPreviewPanel`. Selected-file capture keeps notes/quizzes/questions on the intended file. No new GoF pattern is claimed.

### Short chat commands interpreted as questions and unbounded answer popups  ->  explicit tool routing, then chat-only answers and closable result tabs

**Reason:** Typing quiz produced an ungraded text quiz; long answers exceeded the screen

**Impact:** F11-F13/UC12-UC14; `AiFeedback.action` dispatches exact commands before Q&A. The earlier `ResultWindow` bounded long answers; the current revision displays ordinary answers once in `ChatPanel` and generic text in closable right tabs. `QuizDialog` retains its separate workspace. Normal conceptual questions retain the grounded-answer flow.

### One summary request per page section  ->  bounded page-labelled batches with visible progress/cancel

**Reason:** A 66-slide deck should not require 66 individual calls or appear idle

**Impact:** F12/UC13/SD09b; `StudyPages.batches`, `AgentController.generateSummary`, Swing worker feedback. All nonblank source characters are retained; grouping reduces calls rather than discarding later pages.

### File/week/all MCQ only  ->  current slide and selected-page MCQ as an additional scope

**Reason:** Student requested quizzes directly from lecture files/slides

**Impact:** F13/UC14/SD10a; `StudyPages.parse/selected`, `generateSlideQuiz`, PDF/file menus and CLI pages scope. Page indices are validated, outside-page text is excluded, and insufficient concept content is reported. Essays retain file/week/all scope.

### Small retrieved assessment context  ->  full selected-lecture preparation with validated source notes

**Reason:** Earlier answers repeated syllabus logistics instead of teaching concepts

**Impact:** F11/SD09a; `AssessmentStudySources` selects exact lecture identifiers, reads every selected page or validates all source-note batches, and requires teaching-file citations. Syllabus supplies scope, not the study guide itself. F13/SD10a filters quiz scopes to lecture material. No new GoF category.

### Timeline interaction refinement

The first course-row presentation used filled backgrounds, then neutral gray shades. Student feedback requested a quieter design, so the final local version uses a normal dark row surface and a 3-pixel muted blue/lavender/slate/sand marker beside the course name, with no red/green row marker. TimelineSelection resolves view rows to model UUIDs. N06 adds drag/range selection, context editing, completion, copy and confirmed deletion. These are deterministic GUI actions over existing store operations, not an additional AI or Command-pattern claim. Dragging does not reschedule dates.

### Lecture preparation and source presentation

Original assessment Q&A retrieved a small course-page selection and sometimes overemphasized syllabus logistics. The implementation now uses AssessmentStudySources for scope selection and complete relevant lecture reading, with bounded source notes for large contexts and teaching-file citation requirements. F11 continues to use the shared facade, HTTP Adapter and explanation Strategy; no extra pattern category is claimed. F13 rejects syllabus-only quiz scopes. Chat-only answers and closable text tabs replace the duplicate answer frame, while PDF raster density is independent of logical page size. Affected collaborations are SD09a/SD10a and the existing F11/F13 use cases.

## 5 AI tools and collaboration log

### Development tool, subscription and application API usage

OpenAI Codex desktop was used for implementation, debugging, testing, diagrams and report preparation through a ChatGPT Pro subscription costing USD 200 per month. The observed development session used GPT-6.1 Sol, High effort, Standard speed. Development access is reported as a monthly subscription, separately from the Java application's API usage.

The Java application uses the OpenAI API through HttpLlmClient, with configured default model gpt-6-luna. Its implementation experiments, verification runs and GUI/CLI AI features use the API category below. These calls support classification, syllabus interpretation, references, source-based answers, study notes, quizzes, essay feedback, schedule planning and briefings.

| Purpose | Cost basis | Recorded usage |
| --- | --- | --- |
| Development tool: OpenAI Codex through ChatGPT Pro | USD 200/month subscription | Subscription access for development work |
| Application implementation, experiments and API execution | USD 0.38 total spend for Nov 8-Nov 14 | 2,823,475 tokens; 777 requests |

The API figures are transcribed from the student-supplied Analytics screenshot with Nov 8-Nov 14 selected. The same screenshot separately displays USD 0.40 spent for Personal and Default project in the Spend limit panel. This separate dashboard indicator is recorded as shown and is not added to the USD 0.38 period total. Development subscription and application API charges are reported in distinct categories. Appendix C reproduces the API evidence.

### Representative collaboration and review

- Stage 1 implementation: the student requested a Java GUI/CLI application based on the design report and diagrams. Codex proposed and edited domain, persistence, reader, API, approval and study workflows. Later student review identified missing behavior and unsuitable presentation; subsequent source changes addressed those issues rather than treating the initial generated code as final.

- Original PDFs and controlled API use: The student rejected plain extracted-text previews and initially requested separate paid AI actions. The later explicit request enables complete text/image reading and indexing at upload while calendar changes still need review. Codex added local original-page PDF tabs, upload review and explicit AI sort/calendar actions. Java still extracts text for retrieval and AI without rewriting the stored original.

- Quiz and setup iteration: screenshots exposed clipped options, raw RGB input, strict date punctuation, missing answer navigation and essay count/difficulty gaps. Revisions added compact dates/calendar choice, swatches, a question workspace, answer retention, submission checks and separated explanation depth. Tests and actual native flows were used to review integration.

- Full-document classification: the student found bulk misclassification and authorized reading beyond the first page. The latest helper uses complete page text and full-coverage segments for long sources; the earlier 1,500-character prefix limit is removed. A real 25-PDF mixed batch and an ambiguous renamed 20-page file were checked against manually identified document purposes.

- Source-study follow-up: the student reported silent/failed study actions and an overflowing answer after typing hi then quiz. Shared file context menus, explicit chat tool commands, progress/cancellation and resizable landscape result frames were added in the earlier build. The latest student-requested revision displays ordinary answers only in Chat and other text results in closable side tabs; the interactive quiz remains separate. Notes preserve complete source text in batches; selected-slide MCQs send only requested physical pages. Live native/controller/CLI checks covered actual lectures, not fixed demonstration responses. The final updater retains immutable SHA-256-named JAR versions, so an existing JVM keeps its original code while subsequent launches select the new version.

- Diagrams and verification: two diagram agents were used on 13 November at the student's explicit request, one for architecture/classes/patterns and one for behavior/use cases. The root agent checked rendered outputs against actual classes. Further local edits added SD15 for timeline context actions. Temporary generators, raw test data and API configuration stay below ignored local paths.

The development record connects student feedback, Codex-assisted changes, regression tests and actual application checks. The final source and generated deliverables were reviewed against the Stage 1 design and Stage 2 submission structure.

## 6 Development and verification summary

| Verification layer | Result and scope |
| --- | --- |
| Maven package | 75 tests; 0 failures/errors/skipped; shaded JAR built |
| Actual API/controller acceptance | 16 checks passed across F01-F15 plus classification; successful run made 17 actual API calls |
| Actual CLI acceptance | Historical 23 groups covered 41 then-recognized commands; current material index, DBC quiz and lecture-based preparation also passed live CLI checks. Current dispatcher: 42 commands |
| Mixed actual-PDF upload | 25 of 25 expected categories; 1 syllabus, 22 lecture/reference PDFs, 2 assignment instructions |
| Native GUI | Original PDF, calendar review/Add all/TBD, quiz 3/3 review, full 15-minute expiry saving 900 seconds, status navigation, timeline drag/context/completion/cancel inspected |
| Data handling | Original 82 files backed up with matching SHA-256 hashes before the student-requested fresh bulk test; .env and external originals preserved |

The verification combines deterministic tests, actual API runs, CLI scenarios and native GUI checks. The full timer run naturally ended after 15 minutes and persisted 900 seconds; speaker output was not independently recorded as audio evidence. Timeline completion was temporarily exercised in the native app and restored; deletion confirmation was canceled, preserving the deadlines. Sorting/selection targeting is covered by a dedicated regression. Raw transcripts and temporary fixtures are local under codex/ and are not published wholesale.

Later live follow-up: actual 66-page lecture notes completed in 3 source batches plus synthesis in 22 seconds; final native notes also completed an 82-page Structural Patterns PDF in 4 batches plus synthesis. Native hi -> quiz opened a real lecture MCQ and completed a 1/1 review. A cited Adapter answer used the new landscape frame; native maximize/restore/close and AI cancellation were checked. Final concept-page-21 quiz and an actual CLI selected-page generation/answer/history run passed. These checks supplement the earlier acceptance inventory. Original user data was not the write target for these API/native tests.

Important corrected defects included date-only schedule edits losing omitted fields, inconsistent typo-equivalent merge behavior, missing unlinked study time in briefing, split same-day records, narrow/clipped controls, raw Markdown summaries, wrong-purpose bulk classification, incomplete classifier responses, quiz navigation/submission and essay count/difficulty/cancel handling. For Stage 2 this is basic working-implementation verification; the later full deterministic and KUMA agent-validation requirements remain Stage 3 work under the project instructions.

Complete-source verification used the full uploaded text and page images with the configured OpenAI API. Java prepared all 25 actual files, 1,055 pages and 602,801 characters, preserving all raw pages and caching TXT plus JSON word/page indexes. A second preparation made zero image calls. Actual DBC/Architecture whole-file quizzes, a 22-lecture combined quiz and both reported Lab Test 2/final questions passed bounded checks. This run made 29 text and 129 image requests in 466 seconds. The API cost evidence for the selected reporting period appears in Section 5 and Appendix C. A newly uploaded native DBC deck also completed a three-question quiz and 3/3 review. Original 102 current data files were preserved separately from new derived caches.

Source navigation and error follow-up: underlined local citations in Chat open the exact physical PDF page through MainWindow and PdfPreviewPanel. The earlier ResultWindow navigation check is historical; ordinary answers no longer open that duplicate frame. Native syllabus page-7 and page-5 links were observed. Eight added deterministic methods cover authentication/rate-limit/server/status errors, malformed/empty/truncated/refused provider envelopes, timeout/reset/model parsing failures without saved answers or schedule mutations, missing source questions, incomplete/blank/duplicate image pages with safe retry, cancellation and citation callbacks retaining course context. These are injected fault checks, not evidence that real provider outages were induced. The current CLI adds material index; its actual indexing, complete DBC quiz generation and assessment Q&A passed after the historical 41-command audit.

### Latest lecture-study and presentation verification

The final local package passes 75 deterministic tests with zero failures, errors or skipped tests. AssessmentStudySources maps preparation questions to relevant uploaded teaching files using syllabus/schedule context and the indexed inventory. Every selected lecture page is supplied directly or processed in validated source-note batches when the scope is large. The final answer must cite at least two selected teaching files when available. Syllabus-only responses and nonexistent lecture selections are rejected or regenerated; no successful answer history is saved on exhausted failure. File and slide quizzes accept lecture material rather than syllabus logistics.

For the exact question how to prepare for labtest 2?, the actual API selected the 58-page DBC, 27-page DbC-with-JML and 22-page More JML decks. Two calls produced a 4,955-character guide from a 93,157-character final input. It cited all three teaching files and covered Account variants, equality boundaries, preconditions/invariants/postconditions, JML contracts and loop annotations. The native GUI separately exercised the large-scope reading path, displayed only one Chat answer, followed a citation to physical JML page 11, and completed a real slide quiz with a 1/1 review. The CLI repeated complete indexing, lecture quiz generation and assessment preparation.

Ordinary source answers now stay in Chat, with the view positioned at the beginning of the new answer; generic text results use closable right tabs. The quiz retains a separate workspace. PDF pages render at a higher screen-aware raster density, keep logical dimensions, and discard stale page/zoom renders. This improves source legibility without rewriting the stored PDF. All 152 files in the current user-data hash baseline remained unchanged during isolated follow-up checks.

Notification selection correction: Course deadline cards now support click, keyboard and context actions. Notification settings capture the intended deadline directly from course cards, calendar entries/day lists and timeline context menus. Schedule-menu actions use a visible selection or ask the student to choose; hidden table selection no longer blocks the action. Native tests saved 23:50 through the card and reopened that same value through the calendar. Two additional regressions bring the package to 69 passing tests. The filming guide now uses actual locally supplied EECS3311 PDFs; those private course documents are not added to the public repository. This correction is included in the final local source reviewed for this report.

### Recording-time corrections and final verification

Course deadline cards, calendar entries and the Schedule menu now resolve the intended deadline directly instead of depending on hidden timeline selection. AiTaskStatusPanel shows elapsed time and cancellation without blocking the AI menu. RuntimeDiagnostics records the actual application error and loaded image locally with credential-shaped values redacted. The Windows updater publishes immutable versioned JARs and changes launcher configuration atomically; an old JVM and a fresh JVM were verified to retain their respective versions.

A literal nonresponsive essay answer, test, received an actual API-proposed zero, was explicitly approved and reloaded successfully through GUI and CLI. The CLI proposal list and approval chooser share numbered multiline entries, and a twelve-proposal regression proves that only the selected schedule change is applied. The preview explicitly states that the calendar is unchanged until approval. These additions bring the deterministic suite to 75 tests; the earlier 16-check API and 23-group CLI acceptance runs remain separately scoped historical evidence.

The recorded CLI uses the real application entry point and command handlers. It demonstrates syllabus extraction and rejection, invalid-date recovery, reminders, completion, source-based study advice, saved notes, a two-question quiz scoring 1/2, weak-topic analysis, schedule change approval and calendar verification, essay feedback of 72/100 with approval, weekly briefing and saved history. No command output was rewritten for the edit. The later clean essay entry replaces a corrupted typing attempt; elapsed waiting and retyping footage were removed.

AiTaskStatusPanel, RuntimeDiagnostics and the recording display are presentation/deployment helpers rather than new domain entities or additional design-pattern categories. The 28-sheet design set continues to describe the shared controller, persistence, AI, approval, study and workspace collaborations.

### Weekly source commit summary

The development record below groups source and test changes by the three reporting periods. The displayed dates are Git author/committer dates, and each entry links directly to its implementation change.

### Period 1 October 20 to 26

| Displayed date | Commit | Source or test progress |
| --- | --- | --- |
| Oct23 | [f16332a](https://github.com/jj990405/EECS3311/commit/f16332a) | Workspace/domain persistence, GUI/CLI semester/course/material/deadline management, priority and validation tests |
| Oct23 | [3a948e4](https://github.com/jj990405/EECS3311/commit/3a948e4) | PDF/PPTX/DOCX/TXT reader interface, format factory and extracted-text persistence |
| Oct23 | [166a137](https://github.com/jj990405/EECS3311/commit/166a137) | Configurable LLM transport, structured prompts/JSON parsing and AI workflows |
| Oct23 | [32996dd](https://github.com/jj990405/EECS3311/commit/32996dd) | Deferred add/week commands and explicit approval/rejection before persistence |
| Oct23 | [0e6e16c](https://github.com/jj990405/EECS3311/commit/0e6e16c) | Month calendar and upcoming reminder behavior |

The five source commits implement workspace management, document readers, AI integration, reviewed commands, and calendar/reminder behavior.

### Period 2 October 27 to November 2

| Displayed date | Commit | Source or test progress |
| --- | --- | --- |
| Nov2 | [06c8850](https://github.com/jj990405/EECS3311/commit/06c8850) | Study timer and persistent sessions |
| Nov2 | [10d11e6](https://github.com/jj990405/EECS3311/commit/10d11e6) | Uploaded reference selection and grounded answers with citation checks |
| Nov2 | [1358dbd](https://github.com/jj990405/EECS3311/commit/1358dbd) | Saved cited summaries and deterministic MCQ scoring |
| Nov2 | [642ba16](https://github.com/jj990405/EECS3311/commit/642ba16) | Reviewable schedule planning, weekly brief and weak topics |
| Nov2 | [e06f69e](https://github.com/jj990405/EECS3311/commit/e06f69e) | Notifications, reviewed essay grades and history |
| Nov2 | [3822984](https://github.com/jj990405/EECS3311/commit/3822984) | Workspace upload improvements and GUI/CLI packaging |
| Nov2 | [4a37217](https://github.com/jj990405/EECS3311/commit/4a37217) | Stage 1 study workflow alignment |
| Nov2 | [9641759](https://github.com/jj990405/EECS3311/commit/9641759) | Study workflows, persistence and project deliverable integration |

Eight implementation/test changes are listed; the README-only change is excluded from the source-progress count.

### Period 3 November 3 to 9

| Displayed date | Commit | Source or test progress |
| --- | --- | --- |
| Nov3 | [4e9bddf](https://github.com/jj990405/EECS3311/commit/4e9bddf) | Guided setup, compact dates and color selection |
| Nov3 | [fb6e17c](https://github.com/jj990405/EECS3311/commit/fb6e17c) | Date picker and syllabus date validation |
| Nov3 | [6c7f27b](https://github.com/jj990405/EECS3311/commit/6c7f27b) | Working quiz/PDF workspace and reviewed bulk AI calendar imports |
| Nov3 | [13fe83c](https://github.com/jj990405/EECS3311/commit/13fe83c) | Reviewed AI folder classification, move/reset functionality and tests |
| Nov3 | [a6c36e7](https://github.com/jj990405/EECS3311/commit/a6c36e7) | Corrected file sorting and per-file destination review before upload |
| Nov4 | [f7c2d6e](https://github.com/jj990405/EECS3311/commit/f7c2d6e) | All-week calendar labels, day editing, chat layout, daily TBD and study notes/tests |

Six implementation/test changes are listed. The count covers behavior and test improvements and excludes the repository-layout-only move.

Subsequent approved work includes [fdcfcd7](https://github.com/jj990405/EECS3311/commit/fdcfcd7), [1ab1b5a](https://github.com/jj990405/EECS3311/commit/1ab1b5a), [7d55e8e](https://github.com/jj990405/EECS3311/commit/7d55e8e) [a001883](https://github.com/jj990405/EECS3311/commit/a001883) and [8ab4a77](https://github.com/jj990405/EECS3311/commit/8ab4a77) on Nov 14. The cost-only clarification is not counted as meaningful implementation progress. The complete-source follow-up was published as [e8ab80a](https://github.com/jj990405/EECS3311/commit/e8ab80a). Subsequent recording-time corrections include notification selection, responsive AI progress/cancel controls, immutable runtime publication and numbered CLI proposals; the updated report links these corrections to their verification and demonstrations.

### Implementation and demonstration traceability

The five-page Stage 2 brief was checked against the source and all seven report sections. The latest stored Maven result contains 75 tests with zero failures, errors or skipped tests. All 21 feature entries now contain actual uploaded-video links and start times. Both unlisted recordings opened without signing in on 14 November 2026.

## 7 Demonstration index

The GUI demonstration is [GUI video](https://youtu.be/VxEf256ZtDc) (29:54). The command-line demonstration is [CLI video](https://youtu.be/qe8We5Za7Jc) (06:49). Both were opened without signing in on 14 November 2026. The links below jump to actual start times in the edited recordings. English on-screen captions identify the feature, invocation, input, result and AI role; relevant validation and alternative paths are included.

Some popup windows were not captured by Windows Game Bar. Screenshots from the running application are inserted for those sections.

The CLI recording display is a Java stdin/stdout host invoking the actual application CLI entry point and shared command handlers. It does not implement substitute commands or fabricated responses. GUI-only additions such as PDF side tabs and reset confirmation are indexed to the GUI recording.

Reset safeguards are demonstrated through confirmation and cancellation; isolated tests verify complete clearing while preserving external originals and configuration. Date-based tests also verify the daily unknown-date reminder.

| Feature | GUI timestamp link | CLI timestamp link |
| --- | --- | --- |
| F01 Workspace/materials | [GUI 00:00](https://youtu.be/VxEf256ZtDc?t=0) | [CLI 00:00](https://youtu.be/qe8We5Za7Jc?t=0) |
| F02 Syllabus deadlines | [GUI 06:49](https://youtu.be/VxEf256ZtDc?t=409) | [CLI 01:06](https://youtu.be/qe8We5Za7Jc?t=66) |
| F03 Calendar/timeline | [GUI 09:27](https://youtu.be/VxEf256ZtDc?t=567) | [CLI 01:55](https://youtu.be/qe8We5Za7Jc?t=115) |
| F04 Priority | [GUI 09:27](https://youtu.be/VxEf256ZtDc?t=567) | [CLI 01:42](https://youtu.be/qe8We5Za7Jc?t=102) |
| F05 Study timer/records | [GUI 14:30](https://youtu.be/VxEf256ZtDc?t=870) | [CLI 05:24](https://youtu.be/qe8We5Za7Jc?t=324) |
| F06 Reference files | [GUI 15:34](https://youtu.be/VxEf256ZtDc?t=934) | [CLI 02:41](https://youtu.be/qe8We5Za7Jc?t=161) |
| F07 Deadline notices | [GUI 13:18](https://youtu.be/VxEf256ZtDc?t=798) | [CLI 02:05](https://youtu.be/qe8We5Za7Jc?t=125) |
| F08 Natural-language edits | [GUI 23:23](https://youtu.be/VxEf256ZtDc?t=1403) | [CLI 05:33](https://youtu.be/qe8We5Za7Jc?t=333) |
| F09 Progress | [GUI 12:28](https://youtu.be/VxEf256ZtDc?t=748) | [CLI 02:27](https://youtu.be/qe8We5Za7Jc?t=147) |
| F10 Weekly brief | [GUI 25:08](https://youtu.be/VxEf256ZtDc?t=1508) | [CLI 06:22](https://youtu.be/qe8We5Za7Jc?t=382) |
| F11 Grounded Q&A | [GUI 15:34](https://youtu.be/VxEf256ZtDc?t=934) | [CLI 02:54](https://youtu.be/qe8We5Za7Jc?t=174) |
| F12 Summaries/study notes | [GUI 17:51](https://youtu.be/VxEf256ZtDc?t=1071) | [CLI 03:27](https://youtu.be/qe8We5Za7Jc?t=207) |
| F13 Quiz/essay | [GUI 18:56](https://youtu.be/VxEf256ZtDc?t=1136) | [CLI 03:51](https://youtu.be/qe8We5Za7Jc?t=231) |
| F14 Weak topics | [GUI 21:51](https://youtu.be/VxEf256ZtDc?t=1311) | [CLI 04:39](https://youtu.be/qe8We5Za7Jc?t=279) |
| F15 History/memory | [GUI 25:46](https://youtu.be/VxEf256ZtDc?t=1546) | [CLI 06:36](https://youtu.be/qe8We5Za7Jc?t=396) |
| N01 Reviewed file classification and moves | [GUI 02:14](https://youtu.be/VxEf256ZtDc?t=134) | GUI-specific workflow |
| N02 On-demand source workspace | [GUI 03:12](https://youtu.be/VxEf256ZtDc?t=192) | GUI-specific workflow |
| N03 Bulk calendar import and undated reminders | [GUI 06:49](https://youtu.be/VxEf256ZtDc?t=409) | GUI-specific workflow |
| N04 Easier setup and date/color selection | [GUI 00:00](https://youtu.be/VxEf256ZtDc?t=0) | [CLI 00:24](https://youtu.be/qe8We5Za7Jc?t=24) |
| N05 Confirmed full reset | [GUI 29:32](https://youtu.be/VxEf256ZtDc?t=1772) | GUI-specific workflow |
| N06 Timeline selection and context actions | [GUI 12:28](https://youtu.be/VxEf256ZtDc?t=748) | GUI-specific workflow |

Additional result links: [Full 15-minute timer completion 27:53](https://youtu.be/VxEf256ZtDc?t=1673); [Local MCQ grading and explanation 04:13](https://youtu.be/qe8We5Za7Jc?t=253); [Essay feedback and approval 05:57](https://youtu.be/qe8We5Za7Jc?t=357).

## Appendix A Updated architecture and UML diagrams

The following large-format sheets preserve readable class names and relationships instead of shrinking the whole class model onto a small page. A1-A7 cover architecture and selected UML class collaborations. A8 is the updated workspace/use-case sheet; A9 shows adaptive AI classification, A10 shows timeline context actions, A11 shows lecture-based source Q&A and A12 shows complete lecture quiz generation. The complete 28-sheet editable/rendered set and the original Stage 1 baseline remain in docs/design/. UI helpers omitted from detailed class sheets are named in the architecture index; TimelineSelection is one such presentation helper, not a new persistence or AI subsystem.

## Appendix B GUI recording supplementary evidence

Some popup windows were not captured by Windows Game Bar. Screenshots from the running application are inserted for those sections. The images show the running application with the same course PDFs.

Hosted GUI demonstration: [GUI video](https://youtu.be/VxEf256ZtDc), 29:54. Hosted CLI demonstration: [CLI video](https://youtu.be/qe8We5Za7Jc), 06:49. Section 7 contains direct feature timestamp links.

| Feature / segment | Edited time |
| --- | --- |
| F01 N04 Workspace setup | 00:00 |
| F01 N01 Upload and classify | 02:14 |
| N02 PDF tabs | 03:12 |
| F02 N03 Calendar suggestion review | 06:49 |
| F03 F04 Calendar and priorities | 09:27 |
| F09 N06 Completion and selection | 12:28 |
| F07 Notifications | 13:18 |
| F05 Timer and records | 14:30 |
| F06 F11 References and questions | 15:34 |
| F12 Study notes | 17:51 |
| F13 Quizzes and essays | 18:56 |
| F14 Self assessment | 21:51 |
| F08 Schedule review | 23:23 |
| F10 Weekly brief | 25:08 |
| F15 History | 25:46 |
| F11 Missing source guard | 26:51 |
| F05 Full timer completion | 27:53 |
| N06 Timeline copy and delete guard | 28:43 |
| N05 Reset guard | 29:32 |
| Saved data after restart | 29:42 |

### Popup coverage audit

- 00:00: F01 Workspace setup (14-semester-name-native.png)
- 00:10: N04 Flexible date entry (15-date-picker-native.png)
- 00:18: N04 Invalid date protection (16-invalid-date-native.png)
- 00:26: N04 Calendar date picker (19-end-calendar-native.png)
- 00:50: F01 Add a course (20-course-name-native.png)
- 01:02: N04 Course color (21-course-colors-native.png)
- 01:13: F01 Course difficulty (22-course-difficulty-native.png)
- 01:58: N01 Course context menu (78-course-context-native.png)
- 02:14: F01 Bulk file upload (23-multiple-files-native.png)
- 02:31: N01 AI upload classification (24-ai-upload-review-native.png)
- 04:01: N01 Material context menu (26-file-context-native.png)
- 04:10: N01 Move a file (27-move-category-native.png)
- 04:36: N01 Review an AI folder correction (28-category-review-native.png)
- 05:16: F01 Duplicate file protection (25-duplicate-confirm-native.png)
- 05:40: F01 AI lecture week (29-week-proposal-native.png)
- 06:12: F01 Review the lecture week (30-week-review-native.png)
- 06:49: F02 Review calendar suggestions (10-calendar-review-native.png)
- 07:04: F02 Review one suggestion (11-calendar-individual-native.png)
- 07:18: N03 Add all and unknown dates (12-undated-choice-native.png)
- 07:32: N03 Check source timing warnings (13-calendar-warning-native.png)
- 07:52: F07 Deadline notifications (17-deadline-alert-native.png)
- 09:27: F03 Add from an empty calendar day (31-calendar-day-native.png)
- 09:36: F03 Choose the deadline course (32-course-chooser-native.png)
- 09:52: F03 Deadline title (33-deadline-title-native.png)
- 10:02: F03 Deadline type (34-deadline-type-native.png)
- 10:12: F03 Deadline scope (35-deadline-scope-native.png)
- 11:50: F03 Deadline details (36-deadline-details-native.png)
- 11:56: F03 Edit a saved deadline (73-edit-title-native.png)
- 12:02: F03 Edit the due date (74-edit-date-native.png)
- 12:14: F03 Edit the study scope (75-edit-scope-native.png)
- 12:50: N06 Bulk timeline actions (71-timeline-bulk-native.png)
- 14:01: F07 Notification due time (37-notification-time-native.png)
- 14:20: F07 Notification lead time (38-notification-lead-native.png)
- 14:30: F05 Study timer length (39-timer-length-native.png)
- 14:53: F05 Stop and save a session (81-timer-stop-native.png)
- 15:34: F06 Choose an assessment (40-deadline-chooser-native.png)
- 15:53: F06 AI reference recommendations (41-reference-files-native.png)
- 18:56: F13 Quiz scope (42-quiz-scope-native.png)
- 19:03: F13 Question count (43-quiz-count-native.png)
- 19:09: F13 Quiz difficulty (44-quiz-difficulty-native.png)
- 19:15: F13 Question types (45-quiz-types-native.png)
- 19:22: F13 Practice quiz (49-quiz-question-native.png)
- 19:33: F13 Unanswered question protection (50-quiz-unanswered-native.png)
- 19:41: F13 Submit answers (51-quiz-ready-native.png)
- 19:48: F13 Quiz result (52-quiz-result-native.png)
- 19:57: F13 Review a wrong answer (54-quiz-explanation-native.png)
- 20:11: F13 Quiz selected slides (46-slide-range-native.png)
- 20:18: F13 Invalid slide range (47-invalid-page-native.png)
- 20:25: F13 One-slide quiz (76-slide-question-native.png)
- 20:34: F13 Slide quiz result (77-slide-result-native.png)
- 20:43: F13 Essay question (55-essay-input-native.png)
- 20:52: F13 Write an essay answer (56-essay-answer-native.png)
- 21:05: F13 AI essay feedback (57-essay-feedback-native.png)
- 21:20: F13 Human score review (58-essay-revised-native.png)
- 21:28: F13 Approve the essay grade (59-essay-approval-native.png)
- 21:40: F13 F15 Saved grade history (66-history-record-native.png)
- 21:51: F14 Self assessment topic (60-assessment-topic-native.png)
- 22:18: F14 Confidence rating (61-assessment-confidence-native.png)
- 22:36: F14 Save confidence (62-assessment-saved-native.png)
- 23:44: F08 Review a schedule proposal (64-schedule-review-native.png)
- 25:46: F15 Interaction history (65-history-chooser-native.png)
- 26:21: F15 Delete confirmation (67-history-delete-native.png)
- 27:14: F11 Missing course materials (68-missing-material-native.png)
- 28:11: F05 Full timer completion (80-timer-complete-native.png)
- 29:15: N06 Cancel deadline deletion (70-deadline-delete-native.png)
- 29:32: N05 Full reset confirmation (72-reset-native.png)
- 29:42: F01 F05 F12 Saved data after restart (79-persistence-native.png)

All source screenshots are lossless native-resolution PNG captures. The appendix embeds representative review, input, result and guard windows; the complete repaired sequence is in the edited GUI video. Reset and deletion were demonstrated with confirmation and cancellation. Next-day repetition of a TBD reminder is not shown.


## Appendix C Application API usage evidence

The Analytics screenshot supplied on 14 November 2026 shows the selected Nov 8-Nov 14 period: 2,823,475 tokens, 777 requests and USD 0.38 total spend. The Spend limit panel separately displays USD 0.40 spent for Personal and Default project. Development through ChatGPT Pro is the separate USD 200/month subscription recorded in Section 5.

Source image: docs/report/evidence/api-usage-2026-11-14.png.
