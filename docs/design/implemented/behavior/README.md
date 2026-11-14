# Implemented behavior diagrams

These diagrams describe the Java implementation inspected on 13 November 2026, with SD01a/SD10a/SD10b/SD13 refreshed on 14 November for the upload, quiz and classification follow-up, and SD15/UC21 added for timeline context actions. They support the Stage 2 report; they do not establish that every runtime scenario has passed.

Open [Stage2_Behavior_Diagrams.drawio](Stage2_Behavior_Diagrams.drawio) in diagrams.net to edit the complete 21-page set. Each page also has a vector export in `svg/` and a full-size raster export in `png/`. Keep the original Stage 1 files in `../../diagrams/` as the historical baseline.

## Reading conventions

- Solid open-arrow lines show requests or visible actions; dashed open-arrow lines show returned results. Self messages describe work performed inside the participant.
- `MainWindow / CLI` identifies alternative interface entry points into the same facade. It does not mean that the two interfaces invoke one another.
- `LLM + JSON helpers` groups the distinct `PromptBuilder`, `LlmClient`/`HttpLlmClient`, `ResponseParser`, and `Json` helpers on one lifeline to keep the diagrams readable. Labels identify the task and parse operation. `CommandInvoker / AgentCommand` similarly collapses the invoker-to-command forwarding in two longer sequences.
- Some domain getters and ordinary return messages are omitted. All important data-changing paths show persistence and the relevant approval boundary. The application never gives the LLM direct access to the filesystem or `DataStore`.
- The sequence sheets describe successful paths plus stated conditions and alternatives. Their footer notes are implementation limits, not additional implemented functionality.
- UC01-UC17 retain the Stage 1 IDs. UC03-1 (GUI calendar) and UC03-2 (CLI timeline) are consolidated into the UC03 ellipse. UC18-UC21 are new identifiers used only by this implemented-diagram set.

## Sequence index and code anchors

| Sheet | Original features | Main implementation anchors |
|---|---|---|
| [SD01a](svg/SD01a.svg) Upload and original files | F01 / UC01 | `MainWindow.uploadFiles`, `AgentController.uploadMaterial`, `DataStore.uploadMaterial`, `DocumentReaderFactory.getReader`, `DocumentReader.extractText`, `WorkspaceTree.build`, `PdfPreviewPanel` |
| [SD01b](svg/SD01b.svg) Lecture-week proposal | F01 / UC01, UC17 | `AgentController.suggestLectureWeek`, `ResponseParser.week`, `CommandInvoker.propose/approve`, `AssignWeekCommand.execute`, `DataStore.assignLectureWeek` |
| [SD02](svg/SD02.svg) Syllabus deadline extraction | F02 / UC02, UC17 | `suggestSyllabusDeadlines`, `suggestDocumentDeadlines`, `DeadlineValidator.resolveRelativeDate/warnings`, `reviseSyllabusCandidate`, `mergeSyllabusCandidate`, `AddDeadlineCommand.execute` |
| [SD03](svg/SD03.svg) Schedule, priority and progress | F03, F04, F09 / UC03, UC04, UC10 | `AgentController.schedule/progress`, `PriorityRecommender.order`, `CalendarPanel`, `MainWindow.showCalendarDay/showDeadlineDetails`, `DataStore.setCompleted` |
| [SD04](svg/SD04.svg) Timer and records | F05 / UC05, UC06 | `MainWindow.startStudy/stopStudy`, `AgentController.startStudy/remainingStudySeconds/stopStudy`, `StudyTimer`, `DataStore.recordStudySession`, `TimerRecordPanel.refresh`, CLI `studyWeek` |
| [SD05](svg/SD05.svg) Reference files | F06 / UC07 | `AgentController.suggestReferences/candidateSnippets`, `ReferenceSelector.select`, exact uploaded-name checking |
| [SD06](svg/SD06.svg) Deadline notices | F07 / UC08 | `AgentController.unseenNotifications/markNotificationShown/setNotification`, GUI `showDueNotifications`, CLI `printDueNotifications` |
| [SD07](svg/SD07.svg) Natural-language schedule edits | F08 / UC09, UC17 | `AgentController.planScheduleChange`, `SchedulePlanner.plan`, `DeadlineValidator.validate`, add/edit/delete commands, `MainWindow.reviewProposal` |
| [SD08](svg/SD08.svg) Weekly brief | F10 / UC11 | `AgentController.weeklyBriefing/weeklyBriefDue/markWeeklyBriefShown`, `MainWindow.showWeeklyBriefIfNeeded`, `WeakTopicAnalyzer` |
| [SD09a](svg/SD09a.svg) Source-based Q&A | F11 / UC12 | `AgentController.answerQuestion`, `ReferenceSelector`, `MaterialSearcher.search`, `ExplanationStrategy.forDifficulty`, `InteractionHistory.recent`, `GroundedAnswer` |
| [SD09b](svg/SD09b.svg) Saved study notes | F12 / UC13 | `AgentController.generateSummary/summaries`, `StudyArtifacts.saveSummary`, `StudyNotesPanel`, GUI `showStudyNotes` |
| [SD10a](svg/SD10a.svg) Multiple-choice quiz | F13 / UC14 | `generateQuiz/quizContext/gradeQuiz`, `StudyArtifacts.Question/Quiz/Attempt`, `QuizDialog`/`QuizSessionPanel`, CLI `createQuiz/takeQuiz` |
| [SD10b](svg/SD10b.svg) Essay and score approval | F13 / UC14, UC17 | `generateEssayPrompt`, `suggestEssayGrade`, `proposeEssayGrade`, `StudyArtifacts.saveEssayGrade`, GUI `answerEssayPrompt` |
| [SD11](svg/SD11.svg) Weak-topic review | F14 / UC15 | `WeakTopicAnalyzer.statistics/analyze`, `AgentController.saveSelfAssessment/weakTopicAdvice`, `StudyArtifacts` attempts and confidence records |
| [SD12](svg/SD12.svg) History and recent memory | F15 / UC16 | `InteractionHistory.add/list/recent/delete`, `AgentController.history/deleteHistory/answerQuestion` |
| [SD13](svg/SD13.svg) AI file classification | New / UC18 | `AgentController.suggestUploadCategories/suggestFileCategories/moveMaterial`, `DocumentClassification`, `UploadReviewDialog`, `DataStore.moveMaterial` |
| [SD15](svg/SD15.svg) Timeline context actions | New N06 / UC21 | `MainWindow.timelineMenu`, `TimelineSelection`, `AgentController.setCompleted/deleteDeadline` |
| [SD14](svg/SD14.svg) Bulk calendar and daily TBD notices | New / UC19; extends F02/F07 | `suggestCalendarFromFiles`, `MainWindow.reviewCalendarSuggestions/addCalendarSuggestions/showUndatedReminders`, `hasSyllabusDuplicate`, `unseenUndatedReminders/markUndatedRemindersShown` |

The original SD01, SD09 and SD10 have been split into sub-sheets because their distinct upload/week, Q&A/summary and MCQ/essay paths now contain enough steps to warrant separate views. This changes presentation, not their feature IDs.

## Use-case sheets

| Sheet | Included behavior |
|---|---|
| [UC-A](svg/UC-A.svg) Workspace and schedule | Original UC01-UC04/UC10; file classification, bulk calendar analysis and confirmed full local-data reset |
| [UC-B](svg/UC-B.svg) Study and collaboration | Original UC07/UC09/UC12-UC15/UC17; both deterministic grading and reviewed AI scores |
| [UC-C](svg/UC-C.svg) Time and memory | Original UC05/UC06/UC08/UC11/UC16 |

The LLM actor represents the configured API. The implementation uses `HttpLlmClient` with an OpenAI-compatible HTTPS endpoint; it is not hard-wired to the Stage 1 DeepSeek provider. No secret values appear in these artifacts.

## Stage 1 to implementation changes

| Original design | Implemented behavior | Reason and impact |
|---|---|---|
| `Planner.interpret/plan`, generic `ToolManager.execute`, `Tool`, `ToolResult` | `SchedulePlanner.plan` and explicit helper calls coordinated by `AgentController` | Planning remains an LLM-to-command workflow. Deterministic retrieval/validation is called directly; a generic dynamic tool registry was not implemented. This structural difference must be disclosed in the final report. |
| `MemoryManager` and `InteractionRecord` | `InteractionHistory` with local JSONL `Entry` values and `recent(5)` in later Q&A | Persistent agent-event memory survives restarts, supports viewing/deletion, and is explicitly separated from cited source evidence. |
| `DeepSeekClient` behind `LLMClient` | `HttpLlmClient` behind `LlmClient`; configured endpoint/model | Provider choice changed while the Adapter interface isolates the rest of the application from the transport. |
| Separate `ApprovalDialog`, `StatusBar`, `WorkspaceTreePanel`, and `QuizPanel` classes | Approval/status/tree are managed by `MainWindow`; `QuizDialog` and `StudyNotesPanel` implement dedicated study surfaces | The user-visible responsibilities remain, but class boundaries changed. Sequence participants use actual classes rather than planned names. |
| Proposed automatic matching/extraction following upload | Storage finishes first; AI week matching, classification and calendar analysis run on explicit request | This supports multi-file upload before paid API use and keeps data-changing AI work reviewable. Upload itself is deterministic. |
| Only selected week lists titles; other weeks show colored dots | All weeks show course/title labels; selected-week emphasis remains; an empty dated cell can open add/edit actions | This is a requested usability change. The calendar includes an independent all-course/one-course filter. |
| `FileNameValidator` tool checks AI-selected files | `ReferenceSelector.select` validates names against its supplied `MaterialFile` candidates | Source Q&A rejects unavailable files; reference recommendations can ignore them. No separate `FileNameValidator` class exists. |
| Long material summaries | Page/section notes, optional synthesis into a shorter study guide, persisted summaries in a closeable tab | Makes generated content easier to study while keeping original PDFs accessible. Summary source lists are deterministic; generated facts and inline references still require review. |
| A unified conceptual quiz/result model | MCQ quizzes/attempts and essay prompts/grades are persisted separately in `StudyArtifacts` | MCQ grading is local and deterministic. Essay scores are proposed by AI, optionally corrected in GUI, and saved after approval. |
| Abstract `Subject`/`Observer` model | `DataStore.addChangeListener(Runnable)` and `changed()` callbacks; GUI refresh dispatched on Swing EDT | The Observer collaboration remains but uses Java callbacks rather than named custom observer interfaces. |
| F01-F15 only | Reviewed category moves, on-demand PDF tabs, bulk import with date decisions, daily TBD reminders, and confirmed full local-data reset | These additions need their own feature mapping and demonstration segments in the final report; they do not replace original features. |

## Boundaries to show honestly in demonstrations

- Timer execution and GUI notifications require the application to be open; no OS background task is installed. The CLI can read persisted timer records and display due notifications on startup.
- Scanned images without selectable text have no OCR path. Originals remain stored and viewable, while AI text actions reject unreadable materials.
- Pending proposals live in `CommandInvoker` memory and are not restored after restarting the app. Approved data and recorded decisions are persisted.
- The application verifies supplied source identifiers and Q&A citation provenance; it cannot independently prove that an LLM answer, summary, question, or answer key is factually correct.
- MCQ weak-topic accuracy excludes separately stored essay scores. MCQ metadata currently stores only the first material identifier for a generated multi-file scope, although generation receives the selected source context.
- A natural-language schedule request proposes one action. It does not allocate workload or execute an unconstrained multi-action plan.
- Category classification uses a reviewed table of `CategorySuggestion` values; it does not enqueue `CommandInvoker` proposals. User approval is the Apply action.

## Artifact verification

The combined `.drawio` XML and all 21 SVG exports were parsed successfully. The prior 20 raster exports were checked together on a contact sheet, with larger individual views used for dense sequence and use-case pages. SD15 and the updated UC-A were rendered and reviewed with the report appendix. Temporary generation/QA files are in the ignored `codex/` folder. Runtime verification is tracked by the parent implementation audit separately; generating a diagram does not establish that its scenario passes.
