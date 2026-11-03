# Semester Study and Deadline Management Agent

Open `Study Agent.exe` here for the graphical app, or `Study Agent CLI.exe` for the command-line app. Keep `app/` and `runtime/` beside the EXEs. If those folders are absent after cloning from GitHub, extract `Study-Agent-Windows-x64.zip` into this folder. The ZIP includes its own Java runtime.

- `project1/`: Stage 1 design report and diagrams.
- `project2/`: Stage 2 Java source, tests, build instructions, and assignment PDF.
- `codex/`: local build output and test work, ignored by Git.
- `data/` and `.env`: local user data and API credentials, ignored by Git.

For OpenAI features, copy `project2/.env.example` to `.env` in this folder and set `LLM_API_KEY`. Never commit `.env`. Source build and feature details are in [project2/README.md](project2/README.md).
