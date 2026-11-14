# Implemented architecture and class diagrams

These diagrams describe the Java implementation inspected on November 13, 2026. They replace the architecture/class views for implementation documentation; the original Stage 1 diagrams remain unchanged in `docs/design/diagrams/`.

Open `implemented-architecture-and-classes.drawio` in diagrams.net to edit all seven sheets. Each sheet is also available as a separate editable `.drawio`, scalable `.svg`, and rendered `.png`. Use SVG for the report to preserve readable text. Selected members are shown rather than every overload, UI control, or Java-library type.

| Figure | Scope | Preview |
|---|---|---|
| I01 | Shared GUI/CLI facade, deterministic services, external LLM adapter and local persistence | [Architecture](01-architecture.svg) |
| I02 | Controller, store and persisted domain records; UUID relationships are explicit | [Core and domain](02-core-and-domain.svg) |
| I03 | LLM interface/adapter, prompts, response parsing, retrieval, explanations and history | [LLM and retrieval](03-llm-and-retrieval.svg) |
| I04 | Deferred commands, transient proposals, planner and approval | [Command approval](04-command-approval.svg) |
| I05 | Extension-based reader selection, reader implementations and secure Office XML helper | [Document readers](05-document-readers.svg) |
| I06 | Composite workspace projection and recursive branch/leaf structure | [Workspace Composite](06-workspace-composite.svg) |
| I07 | Actual Observer collaboration, study timer, artifacts, weak topics and reminders | [Study and Observer](07-study-and-observer.svg) |

Notation: solid arrows show retained references or calls as labeled; dashed arrows show dependencies. Dashed lines with hollow triangles mean interface realization. The filled diamond on `WorkspaceBranch.children` represents contained tree nodes. `+` marks public methods, `-` private members, and `~` package access. A `<<record>>` is a Java record. UUID references between records are dashed and labeled; they do not imply fields holding complete Java objects.

## Patterns visible in the actual code

Six distinct GoF patterns are present. The extension-based Simple Factory is useful as a seventh pattern category in the course's broader Factory terminology, but it is not claimed as the GoF Factory Method or Abstract Factory pattern.

| Pattern | Actual roles and evidence | Problem addressed | Implementation boundary |
|---|---|---|---|
| Facade | `AgentController`; clients `MainWindow` and `CommandLineInterface`. Methods include `schedule`, `uploadMaterial`, `answerQuestion`, `planScheduleChange` and approval methods. | Both interfaces need the same validation, storage and AI workflows. | Several UI presentation choices remain in `MainWindow`; the facade does not render controls. |
| Composite | `WorkspaceNode` component; `WorkspaceBranch` composite; `WorkspaceLeaf` leaf. Both expose `label()` and `children()`. `WorkspaceTree.build(DataStore)` constructs the recursive projection. | The viewer and CLI traverse semester/course/category branches and file leaves uniformly. | Domain records are persisted separately. The Composite is a view tree, not a persisted entity inheritance tree. |
| Adapter | Application interface `LlmClient.complete`; concrete adapter `HttpLlmClient.complete`; adapted boundary is the HTTPS chat-completions service. | Prompt workflows should not construct endpoint-specific HTTP requests. | An object adapter over an external protocol; there is no separate vendor SDK class in the model. The configured model may differ by local `.env`. |
| Observer | Subject `DataStore`; callback interface `Runnable`; `MainWindow` registers a lambda via `AgentController.addChangeListener`. `DataStore.changed()` calls the callbacks, which schedule GUI refresh on the Swing EDT. | Mutations must refresh the workspace, schedule and course views. | Listener registration exists; there is no detach method. `StudyTimer` itself is not an Observer subject. |
| Command | `AgentCommand.execute`; concrete `AddDeadlineCommand`, `EditDeadlineCommand`, `DeleteDeadlineCommand`, `AssignWeekCommand`; invoker `CommandInvoker`; receiver `DataStore`. Essay-score commands are lambdas. | AI-proposed changes must be stored for review and executed only after approval. | `Proposal` contains ID/description while a separate map contains the commands. Pending proposals are not persisted across application restarts. |
| Strategy | `PriorityStrategy.order` with `PriorityRecommender`; `ExplanationStrategy.instruction` with difficulty-selected lambdas from `forDifficulty`. | Separate ordering policy and explanation depth from controller workflows. | Priority has one current concrete implementation, fixed in the controller. Explanation variants are runtime-selected lambdas, not three named classes. Grading is not implemented as a Strategy family. |
| Simple Factory | `DocumentReaderFactory.getReader` returns `Optional<DocumentReader>` backed by `PdfReader`, `PptxReader`, `DocxReader`, `TxtReader`. | Centralize format selection and hide concrete reader construction from upload callers. | Factory uses an extension switch/conditional. Adding a format requires changing this factory; do not claim a GoF Factory Method hierarchy. |

## Source-to-diagram evidence

All names refer to `src/main/java/eecs3311/studyagent/`.

| Source | Members / relationship represented | Figures |
|---|---|---|
| `Main.java` | Creates `HttpLlmClient` and one `AgentController`, then selects `MainWindow` or `CommandLineInterface` using `--cli` | I01 |
| `AgentController.java` | Retains store, LLM, prompt builder, parser, invoker, priority strategy, timer, artifacts and history; coordinates task-specific services | I01-I04, I06-I07 |
| `DataStore.java` | Entity maps; `uploadMaterial`, `materialPages`, `createDeadline`, `editDeadline`, `recordStudySession`, `addChangeListener`, private `changed` | I01-I02, I05, I07 |
| `Semester.java`, `Course.java`, `Deadline.java`, `MaterialFile.java`, `StudySession.java`, `Difficulty.java`, `Category.java` | Java record fields, enum values and logical UUID references | I02 |
| `LlmClient.java`, `HttpLlmClient.java` | Interface realization and HTTPS protocol adaptation; local configuration | I03 |
| `PromptBuilder.java`, `ResponseParser.java` | `system`, `user`, `parse`, `deadlines`, `week`; task-specific structured model response processing | I03 |
| `MaterialSearcher.java`, `ReferenceSelector.java`, `InteractionHistory.java`, `ExplanationStrategy.java` | `search`, `select`, `add/list/recent/delete`, `instruction/forDifficulty` | I03 |
| `CommandInvoker.java`, `AgentCommand.java`, four concrete command record files | Deferred execution, replacement/rejection and `execute` operations on the receiver | I04 |
| `SchedulePlanner.java` | `plan` interprets add/edit/delete/clarify, checks course ownership and creates reviewed commands | I04 |
| `DocumentReader.java`, `DocumentReaderFactory.java`, four reader files, `OfficeXmlReader.java` | Interface realization, factory construction, visible page/slide text extraction and secure OOXML helper | I05 |
| `WorkspaceNode.java`, `WorkspaceBranch.java`, `WorkspaceLeaf.java`, `WorkspaceTree.java` | Sealed interface, contained child list, empty leaf list and recursive projection builder | I06 |
| `MainWindow.java`, `StudyTimer.java`, `StudyArtifacts.java`, `WeakTopicAnalyzer.java`, `ReminderService.java` | Swing listener registration / timer polling; study-session persistence; artifact grading; topic statistics and deadline notifications | I07 |

Additional UI classes are concrete Swing components used by `MainWindow`: `CalendarPanel`, `CoursePage`, `TimerPanel`, `TimerRecordPanel`, `ChatPanel`, `PdfPreviewPanel`, `StudyNotesPanel`, `QuizDialog`, `QuizSessionPanel`, `UploadReviewDialog`, and `DatePickerDialog`. Utilities include `AppTheme`, `FlexibleDate`, `CourseColors`, `FileCategorySuggester`, `DocumentClassification`, `CourseRowRenderer`, `DeadlineTitleMatcher`, `CalendarView`, and `Json`. They are omitted from detailed class sheets where they do not change the relationship being explained.

## Differences from the Stage 1 class plan

These are real structural differences to explain in the later report. They are not additional missing-class requirements by themselves: the feature audit must check whether the responsibility and expected behavior remain implemented.

| Original design | Actual implementation | Architectural effect |
|---|---|---|
| `LLMClient` + `DeepSeekClient` | `LlmClient` + configurable `HttpLlmClient` | Vendor-specific name is replaced by an OpenAI-compatible protocol adapter. The app's default provider/model and local configuration must be described accurately. |
| `Planner` | `SchedulePlanner.plan` | Schedule planning is a focused service invoked by the facade. |
| `ToolManager`, `Tool`, `ToolResult` registry | Direct service invocation from `AgentController`, `SchedulePlanner` and related helpers | There is no dynamic tool registry. Deterministic search/validation still runs as explicit Java calls. |
| `FileNameValidator` | Checks in `ReferenceSelector.select`, grounded-answer validation in `AgentController.answerQuestion`, and uploaded-file checks in weak-topic advice | Validation is kept near the relevant output boundary instead of a shared named tool. |
| `MemoryManager`, `InteractionRecord` | `InteractionHistory`, nested `InteractionHistory.Entry`, and `StudyArtifacts` | Interaction memory is JSONL history; artifacts use JSON. Recent history informs grounded Q&A. |
| `Subject`/`Observer` implemented by `StudyTimer`/`TimerPanel` | `DataStore` notifies `Runnable` callbacks; `MainWindow` polls `StudyTimer` with `javax.swing.Timer` | Observer still exists, but its participants and use case differ. Do not draw a nonexistent timer observer hierarchy. |
| Three named explanation-style classes | Difficulty-specific `ExplanationStrategy` lambdas | Strategy variants exist without additional source files. |
| `GradingStrategy`, `MultipleChoiceGrading`, `EssayGrading` | `StudyArtifacts.grade` performs local MCQ scoring; controller uses the LLM to draft essay scores | There is no grading-strategy interface. Strategy claims must use the actual priority/explanation boundaries. |
| `UpdateGradeCommand` | `AgentCommand` lambda created by `proposeEssayGrade` | Explicit score approval remains deferred command execution without a separate named class. |
| `UpdateDeadlineCommand` | `EditDeadlineCommand` | Naming changes; same receiver-oriented command role. |
| `CategoryFolder` with multiple entity Composite types | `WorkspaceBranch.Kind` for semester/course/category and `WorkspaceLeaf` for files | A view projection provides the Composite while persisted entities remain records. |
| `ProgressTracker`, `NotificationService`, `QuizStatistics` | Controller `progress`, `ReminderService` plus controller markers, and `WeakTopicAnalyzer` | Responsibilities are consolidated or renamed. They should not appear as invented concrete classes in updated diagrams. |
| `ApprovalDialog`, `WorkspaceTreePanel`, `QuizPanel`, `StatusBar` | Dialogs/tree/status are mostly controls in `MainWindow`; quiz UI is `QuizDialog` | UI responsibilities remain but the planned class decomposition is changed. |
| One `DataStore` holding all study models | `DataStore` for semester/workspace/deadlines/sessions, `StudyArtifacts` for generated study data, `InteractionHistory` for records | Persistence is split by data family. |

The updated calendar can select all courses or one course through the current implementation's calendar filter. The month view also shows named deadline entries throughout the month instead of Stage 1's colored dots outside the expanded week. This is a presentation change; the new sequence/use-case sheets document the interaction detail.

## Verification and maintenance

The `.drawio` and `.svg` files were parsed as XML after generation. Every SVG was rendered with librsvg (through Sharp) to produce its delivered PNG. Every PNG sheet was reviewed in a contact sheet, and the dense LLM / Observer sheets were inspected separately at full size. Labels, footer spacing and long class names were adjusted after rendering. The draw.io source retains editable nodes and connectors; it was XML-validated, not interactively edited in diagrams.net during this audit.

Temporary generation code and contact sheets are kept under ignored `codex/`, not submitted as application source. A later code change that adds/removes a class, alters a retained dependency, or changes a workflow's approval boundary requires a matching diagram update.

Nov14 follow-up: the classification helper reads all extracted pages for uncertain/conflicting files, with an explicit reviewed pre-upload UI. Course tints and the quiz navigator are presentation components; the shared facade, persistence and six GoF collaborations above remain unchanged. SD13 and SD10a were refreshed accordingly.
