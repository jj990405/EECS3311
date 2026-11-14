# AI tools and collaboration

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

[API usage screenshot](../report/evidence/api-usage-2026-11-14.png) | [Implementation report](../../stage2-report.pdf)
