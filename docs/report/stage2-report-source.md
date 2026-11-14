# Stage 2 Implementation Report

Semester Study and Deadline Management Agent

Junhyeok Park | Student number 216576324 | EECS3311 Fall 2026

Prepared 14 November 2026 | Repository [jj990405/EECS3311](https://github.com/jj990405/EECS3311)

**Implementation update:** The report reflects the latest verified source, including complete source indexing, lecture-based preparation and chat-only answers. 21 feature video entries still need actual links/start times. Section 7 contains planned segment IDs, not fabricated timestamps. Complete those entries before submission.

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

### Working status and limits

Approved data survives restart. AI deadline, lecture-week, schedule and essay-grade proposals require explicit review before changing the relevant student data. Notes and quizzes are saved when explicitly generated. Human approval does not prove an AI answer is correct: citations, dates, categories and answer keys remain reviewable outputs.

Important limits are model-assisted image reading that can misread diagrams/code, no background OS notification service, one deadline action per natural-language schedule request, and transient pending proposals. The GUI timer is not linked to a course. Essay grades are persisted separately and do not feed multiple-choice topic accuracy. Multi-file MCQ metadata retains the first material ID. These boundaries are disclosed rather than hidden as complete implementations of nonexistent subsystems.

## 2 Feature to implementation mapping

All Java class/file names in this section refer to src/main/java/eecs3311/studyagent/ unless another path is stated. GUI/CLI clients call AgentController; DataStore owns approved entity persistence, StudyArtifacts owns generated study items/results, and InteractionHistory owns the JSONL conversation/event record. Each item below states its original reference, entry point, collaboration, patterns and verification boundary. Its demonstration reference points to Section 7, whose recording links are still pending.

### F01 Workspace/materials  -  implemented, modified upload flow

**Reference and status:** Stage 1 F01. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC01, UC17; SD01a, SD01b. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** New semester/course, Add files; workspace right-click. CLI `setup`, `semester add`, `course add`, `material add`, `tree`, `ai week`

**Classes and collaboration:** `AgentController.createSemester/createCourse/uploadMaterial` → `DataStore` validates/copies/persists; `DocumentReaderFactory.getReader` → PDF/PPTX/DOCX/TXT reader extracts page text. `WorkspaceTree.build` produces recursive nodes. Week suggestion uses syllabus context → parser → `AssignWeekCommand` → approval.

**Mode and design patterns:** Hybrid; Facade, Composite, Simple Factory, Adapter, Command

**Evidence and limit:** Real 9-page syllabus and synthetic 2-page lecture uploaded; real API week 3 suggestion approved. Readers covered by automated checks. PDF has original-page preview; other originals open externally. Upload prepares every physical page through StudySources and saves TXT plus a SourceIndex word/page index. Sparse/image-heavy PDF pages and embedded PPTX pictures receive model-assisted reading; originals remain unchanged. Actual 25-file / 1,055-page preparation preserved every original page; second preparation added zero image calls.

**Demonstration:** V1.01, V1.02, V1.03, V3.01, V3.02. Pending recording.

### F02 Syllabus deadlines  -  implemented

**Reference and status:** Stage 1 F02. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC02, UC17; SD02. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** AI → Extract syllabus deadlines; review proposal edit/merge/approve/reject. CLI `ai syllabus`, `proposals`, `approve`, `reject`

**Classes and collaboration:** `suggestSyllabusDeadlines` → `PromptBuilder`/`LlmClient`/`ResponseParser.deadlines` → `DeadlineValidator` → deferred add/edit commands. Revised drafts and merge preserve approval boundary.

**Mode and design patterns:** Hybrid; Adapter, Command, Facade

**Evidence and limit:** Real syllabus yielded 10 assessment proposals; Lab Test 2 / Nov 18 checked and approved. Typo-equivalent merge regression covered. AI dates/weights still require source review; invalid semester dates cannot save.

**Demonstration:** V1.04, V3.02. Pending recording.

### F03 Calendar/timeline  -  implemented, expanded

**Reference and status:** Stage 1 F03. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC03; SD03. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Timeline; persistent top Calendar button; all/one-course filter; click day or deadline. CLI `timeline`, `calendar`, deadline add/edit/delete

**Classes and collaboration:** `schedule` filters period/course/query and delegates ordering; `CalendarPanel` renders all weeks with `[course] title`; `showCalendarDay` opens add/edit even on empty dates; store saves changes.

**Mode and design patterns:** Deterministic; Facade, Strategy, Observer

**Evidence and limit:** Runtime date/search/order checks; GUI all/course filters and blank-day addition; automated all-week labels and empty-cell callback. No drag-and-drop rescheduling.

**Demonstration:** V1.05, V3.03. Pending recording.

### F04 Priority  -  implemented

**Reference and status:** Stage 1 F04. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC04; SD03, SD08. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Timeline/current-week ordering; weekly brief. CLI `timeline`

**Classes and collaboration:** `PriorityRecommender.order` implements `PriorityStrategy`: earlier dates first, then harder course on equal date. Student chooses workload.

**Mode and design patterns:** Deterministic; Strategy

**Evidence and limit:** Runtime high/low same-date assertion. One current priority implementation; no automatic workload allocation.

**Demonstration:** V1.05, V3.03. Pending recording.

### F05 Study timer/records  -  implemented

**Reference and status:** Stage 1 F05. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC05, UC06; SD04. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Study timer: 15/30/60, Pause/Resume/Stop/Records. CLI `study week` reads saved records

**Classes and collaboration:** `StudyTimer` counts active time; `MainWindow` polls and sounds expiry alarm; `stopStudy` → `DataStore.recordStudySession`; `TimerRecordPanel.refresh` and CLI aggregate by day and week. Timer is deliberately not course-linked.

**Mode and design patterns:** Deterministic; Facade; store Observer updates views

**Evidence and limit:** Native countdown, pause/resume/early stop and bottom-status navigation checked. A full real-time 15-minute run ended with Saved 900 seconds; persisted session and daily/weekly totals checked. Alarm code executed; audible speaker output was not independently measured. CLI timer execution was not in Stage 1; records are.

**Demonstration:** V2.01, V3.06. Pending recording.

### F06 Reference files  -  implemented

**Reference and status:** Stage 1 F06. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC07; SD05. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** AI → Suggest reference files; course Reference files. CLI `ai references`

**Classes and collaboration:** `suggestReferences` builds deadline scope and readable candidates → `ReferenceSelector.select` → AI JSON → validates chosen names against uploaded files.

**Mode and design patterns:** Hybrid; Facade, Adapter

**Evidence and limit:** Actual API selected the uploaded SOLID lecture. No readable references is handled; selection usefulness remains model-dependent.

**Demonstration:** V2.02, V3.04. Pending recording.

### F07 Deadline notices  -  implemented

**Reference and status:** Stage 1 F07. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC08; SD06. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Schedule notification configuration; GUI startup/minute checks; reminders. CLI `deadline notify`, `notifications`, `reminders`

**Classes and collaboration:** `ReminderService` evaluates due/lead times; `unseenNotifications` filters persisted shown markers; `markNotificationShown` records them.

**Mode and design patterns:** Deterministic; Facade

**Evidence and limit:** Due notice deduplication tested with explicit times; CLI notification configuration exercised. GUI must be open or notices appear at next launch; no OS background service.

**Demonstration:** V1.07, V3.03. Pending recording.

### F08 Natural-language edits  -  implemented

**Reference and status:** Stage 1 F08. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC09, UC17; SD07. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Chat → Change schedule; AI → Request schedule change. CLI `ai schedule`

**Classes and collaboration:** `SchedulePlanner.plan` gives existing deadlines to LLM → parse add/edit/delete/clarify → validate ownership/date → deferred command in `CommandInvoker`; approval executes. Date-only edits retain omitted title/type/scope.

**Mode and design patterns:** Hybrid; Adapter, Command

**Evidence and limit:** Real API move Nov20→21 before/after approval; actual CLI edit and approval. Date-only regression added. One action per request; multi-action workload planning unsupported.

**Demonstration:** V2.07, V3.06. Pending recording.

### F09 Progress  -  implemented

**Reference and status:** Stage 1 F09. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC10; SD03. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Deadline completion action; course progress. CLI `complete`, `progress`

**Classes and collaboration:** `DataStore.setCompleted` persists; `AgentController.progress` computes total/completed/overdue; listener refreshes UI.

**Mode and design patterns:** Deterministic; Observer, Facade

**Evidence and limit:** Runtime and CLI completion/progress checked. Tracks deadlines, not chapter completion.

**Demonstration:** V1.05, V3.03. Pending recording.

### F10 Weekly brief  -  implemented

**Reference and status:** Stage 1 F10. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC11; SD08. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** AI → Weekly brief; first GUI start of week. CLI `weekly brief`

**Classes and collaboration:** `weeklyBriefing` collects ordered deadlines, total weekly timer seconds and weakness records → LLM; deterministic fallback if unavailable. `weeklyBriefDue/markWeeklyBriefShown` persists weekly marker.

**Mode and design patterns:** Hybrid; Strategy, Adapter, Facade

**Evidence and limit:** Real API brief includes upcoming item; regression ensures unlinked timer seconds included. Automatic first-week marker and fallback covered by checks.

**Demonstration:** V2.08, V3.06. Pending recording.

### F11 Grounded Q&A  -  implemented

**Reference and status:** Stage 1 F11. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC12; SD09a. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Chat → Ask sources; AI actions → Ask sources; file context menu. CLI `ask`

**Classes and collaboration:** Course materials is the default scope; Selected file only or the file context action explicitly pins `answerFromMaterial`. `MaterialSearcher.search` queries saved SourceIndex pages across the course; assessment queries add complete syllabus and saved scope. Preparation requests use AssessmentStudySources to select and read all relevant teaching pages, then require lecture citations and practical exercises. Only prior same-course student questions enter memory. Invalid citations regenerate against the exact allowed physical pages, up to three attempts. `AiFeedback` routes exact tool commands before Q&A and handles greetings/failures; source answers render once in Chat; generic text results use side tabs.

**Mode and design patterns:** Hybrid; Adapter, Strategy, Facade

**Evidence and limit:** Actual lecture Q&A returned cited Factory/Adapter answers in native GUI. The complete-source audit answered the exact Lab Test 2 and final questions with syllabus-aware context and validated citations. Greetings, hi -> quiz dispatch and answer-window maximize/restore checked. Earlier CLI source acceptance remains applicable. Citation provenance is not proof of factual correctness.

**Demonstration:** V2.02, V3.04. Pending recording.

### F12 Summaries/study notes  -  implemented, batched reading UI

**Reference and status:** Stage 1 F12. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC13; SD09b. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** AI actions → Create/Saved study notes; file/PDF context menu; Chat notes. CLI `summary create/list`

**Classes and collaboration:** `generateSummary` uses `StudyPages.batches` to read every nonblank source character in page-labelled batches of at most 12,000 characters, then combines notes and saves through `StudyArtifacts`; `StudyNotesPanel` renders the saved guide. GUI worker shows progress/cancel.

**Mode and design patterns:** Hybrid; Adapter, Facade

**Evidence and limit:** Actual 66-page Creational lecture: 3 input batches plus synthesis, about 22 seconds in controller audit. Latest native Chat notes completed an 82-page Structural Patterns lecture in 4 source batches plus synthesis. Regression preserves all source characters/page numbers. StudySources supplies all prepared pages, including labelled model-assisted image readings. Model-written facts still require source review.

**Demonstration:** V2.03, V3.04. Pending recording.

### F13 Quiz/essay  -  implemented, slide scope added

**Reference and status:** Stage 1 F13. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC14, UC17; SD10a, SD10b. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** AI actions → Practice quiz; file/PDF right-click quiz; PDF AI menu. CLI `quiz create/take` with file/pages/week/all; essay commands retained

**Classes and collaboration:** `generateQuiz` uses only lecture materials and all their prepared pages for file/week/all; syllabus files are excluded, and syllabus-only page quizzes are rejected before any API call; long corpora receive full-source fact batches instead of prefix truncation. An unassigned week offers file selection in the GUI or all-lecture fallback in the CLI; `generateSlideQuiz` uses `StudyPages.parse/selected` to validate and send only chosen physical pages (MCQ only). `StudyArtifacts.grade` compares saved answer indices. Essay AI grade remains a correctable, explicitly approved command.

**Mode and design patterns:** Hybrid; Adapter, Command, Facade

**Evidence and limit:** Actual complete-source DBC and Architecture quizzes plus a combined 22-lecture quiz generated three questions each. A newly uploaded 58-page DBC file completed native quiz generation, answering and 3/3 review. Native selected-slide quiz generated, answered and graded 1/1; final prompt rejects content-only outlines and generated a concept question from actual lecture p.21. Regressions verify outside slides never enter the prompt, invalid ranges fail, CLI pages scope saves a real quiz, and prior essay count/difficulty checks pass. Earlier 3-question navigation and approved 85 essay evidence retained. AI answer keys need review; MCQ/essay analytics remain separate and multi-file metadata retains the first material ID.

**Demonstration:** V2.04, V2.05, V3.05. Pending recording.

### F14 Weak topics  -  implemented

**Reference and status:** Stage 1 F14. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC15; SD11. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Study → Weak topics/Self assess/Review advice. CLI `weak topics`, `self assess`, `weak advice`

**Classes and collaboration:** `WeakTopicAnalyzer.statistics/analyze` combines MCQ topic accuracy with confidence; `weakTopicAdvice` sends ranked context to LLM and checks suggested file names.

**Mode and design patterns:** Hybrid; Facade, Adapter

**Evidence and limit:** Wrong quiz results + confidence produce ranked topics; actual guidance and CLI checked. Essay marks do not feed MCQ topic accuracy.

**Demonstration:** V2.06, V3.05. Pending recording.

### F15 History/memory  -  implemented

**Reference and status:** Stage 1 F15. Implemented, with relevant modifications disclosed in Section 4.

**Affected design trace:** UC16; SD12. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** History menu view/delete. CLI `history`, `history delete`

**Classes and collaboration:** `InteractionHistory.add/list/recent/delete` persists JSONL events; recent same-course student questions contextualize Q&A separately from source evidence; previous generated answers and their citations are not reused as evidence.

**Mode and design patterns:** Hybrid; Facade

**Evidence and limit:** Restart restored artifacts/history; selected deletion checked. Not every manual GUI action is a conversational event. Pending unapproved commands are in memory only.

**Demonstration:** V2.08, V3.06. Pending recording.

### N01 Reviewed file classification and moves

**Reference and status:** New feature. Implemented as an extension to the Stage 1 core.

**Affected design trace:** New UC18; SD13; extends F01. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Add files review table; AI classification; file right-click manual move. Fresh-workspace native 25-PDF upload and actual API run both yielded 1 syllabus, 22 lecture files and 2 assignment specifications, all correct against a manual purpose-based check. An ambiguous renamed 20-page document received full review. Missing/duplicate/foreign AI IDs fail without partial application.

**Classes and collaboration:** `UploadReviewDialog` inspects names/document titles without API; `DocumentClassification` sends complete page text for small batches and reads every extracted page in segments for long sources; no 1,500-character front sample remains; shared controller methods classify before upload or before moving stored files

**Mode and design patterns:** Hybrid; Facade, Adapter

**Evidence and limit:** AI can be wrong; reviewed Apply action is the approval. CLI can upload into chosen category but has no batch sorting UI.

**Demonstration:** V1.02. Pending recording.

### N02 On-demand source workspace

**Reference and status:** New feature. Implemented as an extension to the Stage 1 core.

**Affected design trace:** UC01, UC03, UC13; SD01a, SD03, SD09b; extends F01/F03/F12. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Click PDF; next/previous/fit controls; × closes; all closed restores main width. Native original two-page preview and paging checked.

**Classes and collaboration:** `MainWindow.showViewerTab/closeMaterialTab`, `PdfPreviewPanel`; split pane expand/restore and closable PDF/files/calendar/notes tabs

**Mode and design patterns:** Deterministic; Facade

**Evidence and limit:** Text extraction is for AI; preview uses actual rendered PDF pages. Other original formats use external viewers.

**Demonstration:** V1.03, V2.03. Pending recording.

### N03 Bulk calendar import and undated reminders

**Reference and status:** New feature. Implemented as an extension to the Stage 1 core.

**Affected design trace:** New UC19; UC08, UC17; SD14; extends F02/F07. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Analyze files for calendar after upload; inspect evidence; approve bulk items. Automated import/duplicate/next-day reminder checks plus actual API extraction.

**Classes and collaboration:** `suggestCalendarFromFiles`, review table/Add all/Add checked, per-TBD date/unknown/skip, duplicate checks; `unseenUndatedReminders` daily markers

**Mode and design patterns:** Hybrid; Facade, Adapter, Command

**Evidence and limit:** GUI-only bulk interaction; CLI individual proposals cover core extraction. Daily reminder requires app open or next launch.

**Demonstration:** V1.04, V1.07. Pending recording.

### N04 Easier setup and date/color selection

**Reference and status:** New feature. Implemented as an extension to the Stage 1 core.

**Affected design trace:** UC01, UC03; SD01a, SD03; extends F01/F03. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** New semester/course dialogs, date calendar button, color swatches and course context menu; CLI setup. Numeric `20261122` accepted in live CLI; date validation/color choice tests; native calendar date addition checked.

**Classes and collaboration:** GUI date input → `DatePickerDialog`/`FlexibleDate`; course color choice → `CourseColors`; CLI `setup` → shared controller semester/course creation

**Mode and design patterns:** Deterministic; Facade

**Evidence and limit:** Supports validated Gregorian local dates; invalid dates and dates outside the semester are rejected. No natural-language date parser is claimed.

**Demonstration:** V1.01, V3.01. Pending recording.

### N05 Confirmed full reset

**Reference and status:** New feature. Implemented as an extension to the Stage 1 core.

**Affected design trace:** New UC20 in UC-A; MainWindow.clearAllData -> AgentController.clearAllData -> DataStore.clearAllData. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Isolated automated reset checks; confirmation cancellation checked separately in native GUI.

**Classes and collaboration:** Red button → `MainWindow.clearAllData` → `AgentController.clearAllData` clears store/artifacts/history/proposals

**Mode and design patterns:** Deterministic; Facade, Observer

**Evidence and limit:** Owner requested a clean workspace on Nov14: original 82 files moved to ignored backup, with every SHA-256 verified; fresh data used for the native bulk run. Local `.env` and external originals preserved. GUI reset confirmation/persistence checks remain covered separately.

**Demonstration:** V3.07. Pending recording.

### N06 Timeline selection and context actions

**Reference and status:** New feature. Implemented as an extension to the Stage 1 core.

**Affected design trace:** New UC21; SD15; extends F03/F09. Diagram sources and code anchors are in docs/design/implemented/.

**Entry and behavior:** Drag or Ctrl/Shift-select rows; right-click or Shift+F10 for single edit/details and bulk completion/copy/delete. Native two-row completion and incompletion restored; delete confirmation canceled; sorted snapshot regression passed.

**Classes and collaboration:** `MainWindow.timelineMenu` captures `TimelineSelection.ids` after view-to-model conversion; `setDeadlinesCompleted/deleteDeadlines` call shared `AgentController`/`DataStore` operations; `copyDeadlines` writes TSV to the Java clipboard

**Mode and design patterns:** Deterministic; Facade, Observer

**Evidence and limit:** GUI-only convenience over shared deadline operations; dragging selects rows rather than rescheduling dates. Deletion defaults to Cancel and needs explicit confirmation.

**Demonstration:** V1.06. Pending recording.

## 3 Implemented design patterns

The implementation contains six distinct GoF pattern categories below. Repeated Strategy uses count as one distinct category. Merely naming a class after a pattern is not the evidence; the roles and calls below establish the collaboration. Appendix A includes the current architecture and UML class sheets. Editable sources and all affected use-case/sequence sheets are in docs/design/implemented/.

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

Stage 2 permits documented replacements while preserving minimum functionality. The application does not claim literal class-for-class identity with the Stage 1 drawings. The following consolidated record identifies the original design, implemented change, reason and affected responsibilities. Feature IDs are retained so the reviewer can trace the changes rather than treating new UI work as an excuse for omitted original functions.

### `LLMClient`/`DeepSeekClient`  ->  `LlmClient`/configurable `HttpLlmClient`

**Reason:** Owner chose OpenAI; preserve provider boundary

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

**Reason:** Owner later expressly requested full reading/indexing at initial upload and authorized full text/image transmission to OpenAI

**Impact:** F01 upload now prepares complete text/image sources and local indexes; N01 classification and N03 calendar changes still require review. CLI upload prepares sources and retains its reviewed week proposal.

### Nonselected calendar weeks show dots  ->  all weeks show course/title labels + course filter + day editor

**Reason:** Make deadlines usable without selecting every week

**Impact:** F03 keeps month navigation and adds empty-day create/edit. Affected `CalendarPanel/MainWindow`, SD03.

### Paragraph summary popups  ->  saved study guide tab with headings/bullets/bold and source button

**Reason:** Owner needs usable study material beside original PDF

**Impact:** F12 retained; `StudyNotesPanel`, SD09b; generated facts require review.

### Wide quiz option popups  ->  `QuizDialog` hosting `QuizSessionPanel` with question navigation, wrapping choices, progress and review

**Reason:** Long choices previously exceeded screen width

**Impact:** F13 retained with file/week/all scope, count, difficulty and type; MCQ and essay stored separately.

### Raw RGB/date input  ->  swatches/automatic color and typed compact date/calendar picker

**Reason:** Remove unnecessary setup knowledge and strict punctuation

**Impact:** N04 extends F01/F03. Color identifies course in tree/calendar; timeline uses a thin muted marker with no red/green or course-tinted background.

### Fixed viewer  ->  optional split workspace with closable PDF/calendar/notes tabs

**Reason:** Owner requested source alongside main work and full width after closing tabs

**Impact:** N02, `MainWindow/PdfPreviewPanel`; originals stay unchanged.

### Individual extraction only  ->  explicit bulk review/Add all/TBD decisions and daily unknown-date reminder

**Reason:** Reduce repetitive approval while keeping date uncertainty visible

**Impact:** N03 extends F02/F07; duplicate prevention and date validation still run.

### Short-prefix classification  ->  complete source text, split into full-coverage segments when long

**Reason:** Owner found mixed bulk uploads inaccurate, then requested removing the 1,500-character limit entirely

**Impact:** N01; shared `DocumentClassification`, pre-upload `UploadReviewDialog`, strict complete-response validation; no sorting applies before review.

### No reset/classification flow  ->  reviewed category moves and confirmed local reset

**Reason:** Recover from wrong folders and restart a workspace

**Impact:** N01/N05 additions; stored copies move, original external files and `.env` stay.

### Scattered AI actions and display-only file rows  ->  grouped AI menus and selectable contextual file rows

**Reason:** Owner found course buttons/file selection unclear and wanted file/slide-specific actions

**Impact:** F01/F11-F13, N02; `MainWindow`, `CoursePage`, `MaterialList`, `ChatPanel`, `PdfPreviewPanel`. Selected-file capture keeps notes/quizzes/questions on the intended file. No new GoF pattern is claimed.

### Short chat commands interpreted as questions and unbounded answer popups  ->  explicit tool routing, then chat-only answers and closable result tabs

**Reason:** Typing quiz produced an ungraded text quiz; long answers exceeded the screen

**Impact:** F11-F13/UC12-UC14; `AiFeedback.action` dispatches exact commands before Q&A. The earlier `ResultWindow` bounded long answers; the current revision displays ordinary answers once in `ChatPanel` and generic text in closable right tabs. `QuizDialog` retains its separate workspace. Normal conceptual questions retain the grounded-answer flow.

### One summary request per page section  ->  bounded page-labelled batches with visible progress/cancel

**Reason:** A 66-slide deck should not require 66 individual calls or appear idle

**Impact:** F12/UC13/SD09b; `StudyPages.batches`, `AgentController.generateSummary`, Swing worker feedback. All nonblank source characters are retained; grouping reduces calls rather than discarding later pages.

### File/week/all MCQ only  ->  current slide and selected-page MCQ as an additional scope

**Reason:** Owner requested quizzes directly from lecture files/slides

**Impact:** F13/UC14/SD10a; `StudyPages.parse/selected`, `generateSlideQuiz`, PDF/file menus and CLI pages scope. Page indices are validated, outside-page text is excluded, and insufficient concept content is reported. Essays retain file/week/all scope.

### Small retrieved assessment context  ->  full selected-lecture preparation with validated source notes

**Reason:** Earlier answers repeated syllabus logistics instead of teaching concepts

**Impact:** F11/SD09a; `AssessmentStudySources` selects exact lecture identifiers, reads every selected page or validates all source-note batches, and requires teaching-file citations. Syllabus supplies scope, not the study guide itself. F13/SD10a filters quiz scopes to lecture material. No new GoF category.

### Timeline interaction refinement

The first course-row presentation used filled backgrounds, then neutral gray shades. Owner feedback requested a quieter design, so the final local version uses a normal dark row surface and a 3-pixel muted blue/lavender/slate/sand marker beside the course name, with no red/green row marker. TimelineSelection resolves view rows to model UUIDs. N06 adds drag/range selection, context editing, completion, copy and confirmed deletion. These are deterministic GUI actions over existing store operations, not an additional AI or Command-pattern claim. Dragging does not reschedule dates.

### Lecture preparation and source presentation

Original assessment Q&A retrieved a small course-page selection and sometimes overemphasized syllabus logistics. The implementation now uses AssessmentStudySources for scope selection and complete relevant lecture reading, with bounded source notes for large contexts and teaching-file citation requirements. F11 continues to use the shared facade, HTTP Adapter and explanation Strategy; no extra pattern category is claimed. F13 rejects syllabus-only quiz scopes. Chat-only answers and closable text tabs replace the duplicate answer frame, while PDF raster density is independent of logical page size. Affected collaborations are SD09a/SD10a and the existing F11/F13 use cases.

## 5 AI tools and collaboration log

### Development tool and application model

OpenAI Codex desktop was used for implementation, debugging, tests, diagram preparation and report support. The observed native analytics identified this chat as GPT-6.1 Sol, High effort, Standard speed. Earlier exact model revisions and historical raw development tokens are not fully exported. The development tool was accessed through a ChatGPT Pro subscription costing USD 200 per month. That is a shared subscription fee, not a metered invoice attributable entirely to this repository.

The running Java application calls the separately configured OpenAI API through HttpLlmClient, with default model gpt-6-luna. It uses AI for syllabus/week interpretation, file-purpose classification, references and source Q&A, notes, quizzes/essay feedback, schedule planning, weak-topic advice and briefing. Unit tests substitute LlmClient; actual API checks and native GUI/CLI runs also used the configured live endpoint.

| Purpose | Money cost | Token evidence |
| --- | --- | --- |
| Development through OpenAI Codex | USD 200/month subscription | Project-specific historical raw total unavailable |
| Application OpenAI API | USD 0.02 displayed at the observed snapshot | 138179 total: 116494 input and 21685 output; 115 requests |

The API snapshot was observed shortly after midnight Toronto time on 14 November 2026, for Personal organization / Default project, Oct 20-Nov 14, all keys/users/models. The dashboard uses UTC and the selected URL ended at until=2026-11-15. The displayed spend is rounded. Additional verification calls occurred after this snapshot; it is not represented as the final current total. Account/project aggregation does not exclude unrelated use of that same API project. A final billing refresh would improve precision, but no missing raw token figure or per-project subscription allocation has been invented. Sources: [API overview](https://platform.openai.com/analytics/overview?from=2026-10-20&until=2026-11-15), [usage](https://platform.openai.com/analytics/usage?from=2026-10-20&until=2026-11-15), [spend](https://platform.openai.com/analytics/spend?from=2026-10-20&until=2026-11-15).

### Representative collaboration and review

- Stage 1 implementation: the owner requested a Java GUI/CLI application based on the design report and diagrams. Codex proposed and edited domain, persistence, reader, API, approval and study workflows. Later owner review identified missing behavior and unsuitable presentation; subsequent source changes addressed those issues rather than treating the initial generated code as final.

- Original PDFs and controlled API use: The owner rejected plain extracted-text previews and initially requested separate paid AI actions. The later explicit request enables complete text/image reading and indexing at upload while calendar changes still need review. Codex added local original-page PDF tabs, upload review and explicit AI sort/calendar actions. Java still extracts text for retrieval and AI without rewriting the stored original.

- Quiz and setup iteration: screenshots exposed clipped options, raw RGB input, strict date punctuation, missing answer navigation and essay count/difficulty gaps. Revisions added compact dates/calendar choice, swatches, a question workspace, answer retention, submission checks and separated explanation depth. Tests and actual native flows were used to review integration.

- Full-document classification: the owner found bulk misclassification and authorized reading beyond the first page. The latest helper uses complete page text and full-coverage segments for long sources; the earlier 1,500-character prefix limit is removed. A real 25-PDF mixed batch and an ambiguous renamed 20-page file were checked against manually identified document purposes.

- Source-study follow-up: the owner reported silent/failed study actions and an overflowing answer after typing hi then quiz. Shared file context menus, explicit chat tool commands, progress/cancellation and resizable landscape result frames were added in the earlier build. The latest owner-requested revision displays ordinary answers only in Chat and other text results in closable side tabs; the interactive quiz remains separate. Notes preserve complete source text in batches; selected-slide MCQs send only requested physical pages. Live native/controller/CLI checks covered actual lectures, not fixed demonstration responses. Runtime replacement now requires the app closed; a stale-code mismatch was suspected but no original linkage exception was captured.

- Diagrams and verification: two diagram agents were used on 13 November at the owner's explicit request, one for architecture/classes/patterns and one for behavior/use cases. The root agent checked rendered outputs against actual classes. Further local edits added SD15 for timeline context actions. Temporary generators, raw test data and API configuration stay below ignored local paths.

The owner described an earlier lost AI-agent session, but its exact tool, model, token total and cost cannot be reconstructed from available records. This report does not assign invented contributions or amounts to that session. The student remains responsible for understanding and reviewing the submitted code; generated artifacts and passing sample tests do not transfer that responsibility to the development tool.

## 6 Development and verification summary

| Verification layer | Result and scope |
| --- | --- |
| Maven package | 67 tests; 0 failures/errors; shaded JAR built |
| Actual API/controller acceptance | 16 checks passed across F01-F15 plus classification; successful run made 17 actual API calls |
| Actual CLI acceptance | Historical 23 groups covered 41 then-recognized commands; current material index, DBC quiz and lecture-based preparation also passed live CLI checks. Current dispatcher: 42 commands |
| Mixed actual-PDF upload | 25 of 25 expected categories; 1 syllabus, 22 lecture/reference PDFs, 2 assignment instructions |
| Native GUI | Original PDF, calendar review/Add all/TBD, quiz 3/3 review, full 15-minute expiry saving 900 seconds, status navigation, timeline drag/context/completion/cancel inspected |
| Data handling | Original 82 files backed up with matching SHA-256 hashes before the owner-requested fresh bulk test; .env and external originals preserved |

These are complementary checks, not a claim that every GUI combination or arbitrary LLM response is defect-free. The full timer run naturally ended after 15 minutes and persisted 900 seconds; speaker output was not independently recorded as audio evidence. Timeline completion was temporarily exercised in the native app and restored; deletion confirmation was canceled, preserving the deadlines. Sorting/selection targeting is covered by a dedicated regression. Raw transcripts and temporary fixtures are local under codex/ and are not published wholesale.

Later live follow-up: actual 66-page lecture notes completed in 3 source batches plus synthesis in 22 seconds; final native notes also completed an 82-page Structural Patterns PDF in 4 batches plus synthesis. Native hi -> quiz opened a real lecture MCQ and completed a 1/1 review. A cited Adapter answer used the new landscape frame; native maximize/restore/close and AI cancellation were checked. Final concept-page-21 quiz and an actual CLI selected-page generation/answer/history run passed. These bounded checks supplement the earlier acceptance inventory; they are not a fresh replay of every earlier command. Original user data was not the write target for these API/native tests.

Important corrected defects included date-only schedule edits losing omitted fields, inconsistent typo-equivalent merge behavior, missing unlinked study time in briefing, split same-day records, narrow/clipped controls, raw Markdown summaries, wrong-purpose bulk classification, incomplete classifier responses, quiz navigation/submission and essay count/difficulty/cancel handling. For Stage 2 this is basic working-implementation verification; the later full deterministic and KUMA agent-validation requirements remain Stage 3 work under the project instructions.

Complete-source follow-up: the owner explicitly authorized full uploaded text/page images to OpenAI after an initial approval rejection. Java prepared all 25 actual files, 1,055 pages and 602,801 characters, preserving all raw pages and caching TXT plus JSON word/page indexes. A second preparation made zero image calls. Actual DBC/Architecture whole-file quizzes, a 22-lecture combined quiz and both reported Lab Test 2/final questions passed bounded checks. This run made 29 text and 129 image requests in 466 seconds, after the earlier billing snapshot. A newly uploaded native DBC deck also completed a three-question quiz and 3/3 review. These are samples, not guarantees of arbitrary model accuracy. Original 102 current data files were preserved separately from new derived caches.

Source navigation and error follow-up: underlined local citations in Chat open the exact physical PDF page through MainWindow and PdfPreviewPanel. The earlier ResultWindow navigation check is historical; ordinary answers no longer open that duplicate frame. Native syllabus page-7 and page-5 links were observed. Eight added deterministic methods cover authentication/rate-limit/server/status errors, malformed/empty/truncated/refused provider envelopes, timeout/reset/model parsing failures without saved answers or schedule mutations, missing source questions, incomplete/blank/duplicate image pages with safe retry, cancellation and citation callbacks retaining course context. These are injected fault checks, not evidence that real provider outages were induced. The current CLI adds material index; its actual indexing, complete DBC quiz generation and assessment Q&A passed after the historical 41-command audit.

### Latest lecture-study and presentation verification

The final local package passes 67 deterministic tests with zero failures or errors. AssessmentStudySources maps preparation questions to relevant uploaded teaching files using syllabus/schedule context and the indexed inventory. Every selected lecture page is supplied directly or processed in validated source-note batches when the scope is large. The final answer must cite at least two selected teaching files when available. Syllabus-only responses and nonexistent lecture selections are rejected or regenerated; no successful answer history is saved on exhausted failure. File and slide quizzes accept lecture material rather than syllabus logistics.

For the exact question how to prepare for labtest 2?, the actual API selected the 58-page DBC, 27-page DbC-with-JML and 22-page More JML decks. Two calls produced a 4,955-character guide from a 93,157-character final input. It cited all three teaching files and covered Account variants, equality boundaries, preconditions/invariants/postconditions, JML contracts and loop annotations. The native GUI separately exercised the large-scope reading path, displayed only one Chat answer, followed a citation to physical JML page 11, and completed a real slide quiz with a 1/1 review. The CLI repeated complete indexing, lecture quiz generation and assessment preparation. These are bounded actual checks, not promises about every future model response.

Ordinary source answers now stay in Chat, with the view positioned at the beginning of the new answer; generic text results use closable right tabs. The quiz retains a separate workspace. PDF pages render at a higher screen-aware raster density, keep logical dimensions, and discard stale page/zoom renders. This improves source legibility without rewriting the stored PDF. All 152 files in the current user-data hash baseline remained unchanged during isolated follow-up checks.

### Weekly source commit summary

The official reporting periods are Toronto-local Oct 20-26, Oct 27-Nov 2 and Nov 3-9, with at least five meaningful source/test commits pushed in each. The LMS submission extension to 15 November 2026 at 11:59 PM does not itself change those periods. Commit dates below are Git metadata; they are not evidence of when GitHub received a push.

### Period 1 October 20 to 26

| Displayed date | Commit | Source or test progress |
| --- | --- | --- |
| Oct23 | [f16332a](https://github.com/jj990405/EECS3311/commit/f16332a) | Workspace/domain persistence, GUI/CLI semester/course/material/deadline management, priority and validation tests |
| Oct23 | [3a948e4](https://github.com/jj990405/EECS3311/commit/3a948e4) | PDF/PPTX/DOCX/TXT reader interface, format factory and extracted-text persistence |
| Oct23 | [166a137](https://github.com/jj990405/EECS3311/commit/166a137) | Configurable LLM transport, structured prompts/JSON parsing and AI workflows |
| Oct23 | [32996dd](https://github.com/jj990405/EECS3311/commit/32996dd) | Deferred add/week commands and explicit approval/rejection before persistence |
| Oct23 | [0e6e16c](https://github.com/jj990405/EECS3311/commit/0e6e16c) | Month calendar and upcoming reminder behavior |

Five changes are substantive source work, but the first five commits have retrospective Oct 23 dates. Earlier session records place their first publication on Nov 2. Historical PushEvents were not independently reconstructed in this report, so period-1 push compliance is not certified. The brief specifies a single total 5-point deduction if any reporting period is deficient; a later date edit or later push cannot recreate a historical push.

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

Eight implementation/test candidates are listed. README-only commit 83ab8d8 is excluded. Earlier approved-push records place this work in period 2; Git metadata alone is not a push-timing certificate.

### Period 3 November 3 to 9

| Displayed date | Commit | Source or test progress |
| --- | --- | --- |
| Nov3 | [4e9bddf](https://github.com/jj990405/EECS3311/commit/4e9bddf) | Guided setup, compact dates and color selection |
| Nov3 | [fb6e17c](https://github.com/jj990405/EECS3311/commit/fb6e17c) | Date picker and syllabus date validation |
| Nov3 | [6c7f27b](https://github.com/jj990405/EECS3311/commit/6c7f27b) | Working quiz/PDF workspace and reviewed bulk AI calendar imports |
| Nov3 | [13fe83c](https://github.com/jj990405/EECS3311/commit/13fe83c) | Reviewed AI folder classification, move/reset functionality and tests |
| Nov3 | [a6c36e7](https://github.com/jj990405/EECS3311/commit/a6c36e7) | Corrected file sorting and per-file destination review before upload |
| Nov4 | [f7c2d6e](https://github.com/jj990405/EECS3311/commit/f7c2d6e) | All-week calendar labels, day editing, chat layout, daily TBD and study notes/tests |

Six implementation/test candidates are listed. Repository-layout move 7cdfd95 is excluded from the conservative count. Meaningfulness is based on behavior/test changes, not file counts or raw insertion totals.

Subsequent approved work includes [fdcfcd7](https://github.com/jj990405/EECS3311/commit/fdcfcd7), [1ab1b5a](https://github.com/jj990405/EECS3311/commit/1ab1b5a), [7d55e8e](https://github.com/jj990405/EECS3311/commit/7d55e8e) [a001883](https://github.com/jj990405/EECS3311/commit/a001883) and [8ab4a77](https://github.com/jj990405/EECS3311/commit/8ab4a77) on Nov 14. The cost-only clarification is not counted as meaningful implementation progress. The owner authorized committing and pushing the latest source and updated report/filming documents on 14 November. This update follows baseline 8ab4a77; its publication is checked after the push. It is outside all three weekly reporting windows. Work after a reporting window cannot compensate for a missed historical push window.

### Submission readiness audit

The five-page Stage 2 brief was checked against the source and all seven report sections. The latest deterministic suite passed 67 tests. The companion docs/development/stage2-requirements-check.md records evidence and unresolved items: actual videos and all 21 URL/start-time pairs, final video-linked report revision, publication verification of this approved update and LMS submission. Historical period-1 push timing is not certified, and project-specific historical development tokens remain unavailable. Implementation sections are updated; the demonstration index remains incomplete until actual recordings and final submission steps are completed.

## 7 Demonstration index

Recording URLs have not yet been supplied for all features. The following planned segments cover all 15 original and 6 added features. They are not real timestamps. Before final submission, replace Pending recording with an accessible video URL and the actual start time for each feature. A combined recording is allowed when each feature has a labeled segment and direct timestamp link. The instructor/TA must not need an access request.

The complete click/input/narration script is docs/demo/filming-script.ko.md, also rendered as docs/demo/filming-guide.pdf. The editable index is docs/demo/demonstration-index.json. Video 1 covers workspace/deadlines; Video 2 covers study/agent workflows; Video 3 covers CLI and the final demonstration-only reset. Spoken English or readable English captions can deliver the required explanation; the brief does not mandate a fixed duration, voice format or face camera.

| Feature | Planned segment references | Actual URL and start time |
| --- | --- | --- |
| F01 Workspace/materials  | V1.01, V1.02, V1.03, V3.01, V3.02 | Pending recording |
| F02 Syllabus deadlines  | V1.04, V3.02 | Pending recording |
| F03 Calendar/timeline  | V1.05, V3.03 | Pending recording |
| F04 Priority  | V1.05, V3.03 | Pending recording |
| F05 Study timer/records  | V2.01, V3.06 | Pending recording |
| F06 Reference files  | V2.02, V3.04 | Pending recording |
| F07 Deadline notices  | V1.07, V3.03 | Pending recording |
| F08 Natural-language edits  | V2.07, V3.06 | Pending recording |
| F09 Progress  | V1.05, V3.03 | Pending recording |
| F10 Weekly brief  | V2.08, V3.06 | Pending recording |
| F11 Grounded Q&A  | V2.02, V3.04 | Pending recording |
| F12 Summaries/study notes  | V2.03, V3.04 | Pending recording |
| F13 Quiz/essay  | V2.04, V2.05, V3.05 | Pending recording |
| F14 Weak topics  | V2.06, V3.05 | Pending recording |
| F15 History/memory  | V2.08, V3.06 | Pending recording |
| N01 Reviewed file classification and moves | V1.02 | Pending recording |
| N02 On-demand source workspace | V1.03, V2.03 | Pending recording |
| N03 Bulk calendar import and undated reminders | V1.04, V1.07 | Pending recording |
| N04 Easier setup and date/color selection | V1.01, V3.01 | Pending recording |
| N05 Confirmed full reset | V3.07 | Pending recording |
| N06 Timeline selection and context actions | V1.06 | Pending recording |

After recording, enter accessible feature links and actual start times, regenerate this report, publish the approved revision and submit the repository URL through LMS. Videos and LMS submission are still required.

## Appendix A Updated architecture and UML diagrams

The following large-format sheets preserve readable class names and relationships instead of shrinking the whole class model onto a small page. A1-A7 cover architecture and selected UML class collaborations. A8 is the updated workspace/use-case sheet; A9 shows adaptive AI classification, A10 shows timeline context actions, A11 shows lecture-based source Q&A and A12 shows complete lecture quiz generation. The complete 28-sheet editable/rendered set and the original Stage 1 baseline remain in docs/design/. UI helpers omitted from detailed class sheets are named in the architecture index; TimelineSelection is one such presentation helper, not a new persistence or AI subsystem.
