# Semester Study and Deadline Management Agent

This is an incremental implementation of the EECS3311 Stage 1 design. It uses Java 17+ and the bundled Apache PDFBox 3.0.8 application JAR for real PDF text extraction. No API key is needed for local features.

## Build and run

From this directory in PowerShell:

```powershell
.\run.ps1 -Mode gui
.\run.ps1 -Mode cli
.\run.ps1 -Mode test
```

On macOS or Linux, use `./run.sh gui`, `./run.sh cli`, or `./run.sh test` (or `bash run.sh ...`). Both scripts compile the Java sources with the standard JDK compiler, add PDFBox to the classpath, and run `Main`. `run.ps1` uses `JAVA_HOME`, then `javac` on `PATH`, then an Eclipse JustJ JDK if installed. If PowerShell blocks local scripts, use `powershell -ExecutionPolicy Bypass -File .\run.ps1 -Mode cli`. User data is saved under `data/`; pass `-DataDirectory <path>` on Windows or a second argument on macOS/Linux to use another directory. `data/` is excluded from Git.

## Working workflow

1. Create a semester with start and end dates.
2. Create courses with a color and difficulty (high, medium, or low).
3. Upload PDF, PPTX, DOCX, or TXT into the four fixed categories. The original file bytes and extracted text are stored locally; double-click a file in the GUI tree to open it. Scanned PDFs without selectable text show a warning and are excluded from AI features.
4. Enter deadlines, including an optional TBD date. Dates outside the semester are rejected.
5. View deadlines in the GUI table or CLI timeline. The timeline supports period, course, and title/type filters. Earlier dates come first; deadlines on the same date put the more difficult course first.
6. Edit or delete deadlines, mark them complete, and inspect course progress and overdue counts. Close and reopen the application to verify persistence.

The GUI and CLI share `AgentController` and `DataStore`, so changes made in one interface appear in the other after restart. The local data format is one UTF-8 `.properties` file per entity plus copied material files.

## Design traceability and current limits

| Stage 1 feature | Current status | Main classes |
| --- | --- | --- |
| F01 Semester, Course, and Material Management | Partial: workspace tree, categories, file storage/opening, PDF/PPTX/DOCX/TXT extraction, course difficulty/color, and persistence work. AI lecture-week matching remains. | `WorkspaceTree`, `DocumentReaderFactory`, `DataStore`, `AgentController`, both viewers |
| F03 Schedule View | Partial: GUI schedule table and CLI timeline, search/filter, edit/delete, and TBD dates work. The planned monthly calendar is not built yet. | `AgentController`, `MainWindow`, `CommandLineInterface` |
| F04 Priority Recommendation | Implemented: date first, then course difficulty for same-day deadlines. | `PriorityRecommender` |
| F09 Progress Tracking | Implemented: completion state, completed/total count, and overdue count. | `AgentController`, `DataStore`, both viewers |

The other Stage 1 features are **not implemented yet**. In particular, syllabus deadline extraction, an LLM connection, AI approval proposals, study tools, timer, reminders, weekly briefing, and history still need implementation before the Stage 2 submission. This slice alone does **not** satisfy the full Stage 2 requirements.

Patterns already visible in code: **Facade** (`AgentController`), **Composite** (`WorkspaceNode`, `WorkspaceBranch`, `WorkspaceLeaf`), and **Observer** (`DataStore` change listeners used by `MainWindow`). The final project needs at least five distinct implemented patterns.

## Source and secrets

Commit the source, this README, `lib/pdfbox-app-3.0.8.jar`, and `.gitignore` to the same Stage 1 GitHub repository. The JAR is the Apache PDFBox 3.0.8 release; its SHA-512 is in `lib/pdfbox-app-3.0.8.jar.sha512`. Do not commit `data/`, `build/`, or a real API key. When an LLM client is added, use a local secret configuration and commit only a placeholder example.
