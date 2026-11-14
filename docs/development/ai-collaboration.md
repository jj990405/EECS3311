# AI collaboration record

Prepared 13 November 2026 from repository history, this conversation and the current audit. This is a factual working log for the later report. It is not a complete provider billing ledger.

| Tool/platform | Model information supported by evidence | Development use / representative interaction | Review and integration |
| --- | --- | --- | --- |
| OpenAI Codex desktop coding agent | Native Usage analytics identifies this chat as **GPT-6.1 Sol**, High effort, Standard speed (observed Nov14). Earlier exact model revisions are not fully recorded. | User requested implementation from Stage 1 report/diagrams, GUI and CLI, Java-only PDF handling, approval before AI calendar changes, original-page PDF viewer and usable quizzes/notes. Agent generated/edited Java classes and tests. | Owner supplied screenshots/defects, rejected unusable layouts and approved individual commits. Automated tests and actual GUI/API checks were used before integration. |
| Codex parallel agents, 13 Nov | Inherited model of the current Codex session; exact billable model ID/token count not exported here | User explicitly requested parallel diagram preparation. One agent inspected architecture/classes/pattern roles; one inspected sequences/use cases. Produced 7 architecture/class sheets and 20 behavior sheets. | Root inspected rendered architecture/command/SD14 samples and reconciled actual participants and limitations. XML/render checks performed. No app code was accepted merely because diagrams rendered. |
| Configured external OpenAI API, application runtime | `HttpLlmClient`; default model ID `gpt-6-luna`. Local configuration may override it. Never publish the local key. | Syllabus extraction, week matching, reference selection, source Q&A, notes, MCQ/essay generation, essay feedback, schedule edits, weak-topic advice, weekly brief and file classification. | Source identifiers and dates are validated in Java; calendar/grade changes require approval. MCQ score comparison is deterministic. Actual calls exercised in audit and CLI runs. |
| Earlier AI agent mentioned by owner | Tool/model unknown; session was lost when subscription ended | Owner described earlier partial implementation/planning. Exact contributions and token/money totals cannot be reconstructed from the present evidence. | Do not invent a vendor/model, cost, or recoverability claim. Owner should identify this if it materially contributed to submitted implementation. |

## Representative iterations

| Evidence / period | Request → AI work → human/verification feedback |
| --- | --- |
| Initial source commits | Implement Stage 1 workspace/readers/API/approval/calendar, then timer/retrieval/notes/quizzes/planning/essay/history → source changes in ten substantive commits → later reviews exposed UI and feature gaps, addressed by follow-up implementation. |
| Nov2–4 source history | Owner requested EXE, simpler setup, compact dates/colors, actual syllabus/API checks, usable quiz and original PDF side tabs, file sorting, bulk calendar import, daily TBD prompts and study notes → repeated code/test revisions → owner approved each resulting commit. |
| 13 Nov audit | Owner requested complete feature verification and parallel current diagrams before the report → source/Stage 1 audit, real API acceptance harness, actual CLI transcripts, native EXE computer use → date-only schedule edit, typo merge consistency, unlinked timer briefing totals, daily totals and narrow controls corrected. |
| 13 Nov rendering feedback | Native GUI showed narrow course/timer controls, raw Markdown and dense notes → action grids, wrapping, real styled emphasis and paragraph spacing → rebuilt, automated tests and native reinspection. |

## Measured API usage and remaining cost fields

At the owner's request, the API analytics pages were inspected directly through native browser Computer Use on **14 November 2026, shortly after midnight Toronto time**. Selected scope: **Personal organization → Default project**, date selector **Oct20–Nov14, 2026**, all API sources/keys/users/models. The chart labels use UTC and the page URL ends at `until=2026-11-15`.

| Dashboard field | Observed value |
| --- | --- |
| Total tokens | **138,179** |
| Requests | **115** |
| Model breakdown | **gpt-6-luna**, all115 displayed requests |
| Input tokens | **116,494** |
| Output tokens | **21,685** |
| Total spend | **USD $0.02**, rounded as displayed by Overview and Spend |
| Observed source pages | [Overview](https://platform.openai.com/analytics/overview?from=2026-10-20&until=2026-11-15), [Usage](https://platform.openai.com/analytics/usage?from=2026-10-20&until=2026-11-15), [Spend](https://platform.openai.com/analytics/spend?from=2026-10-20&until=2026-11-15) |

These are the selected API project's aggregate totals at observation time, not an invoice or a per-repository meter. The observed model matches the application. No API-key filter was selected, so unrelated uses of the same Default project cannot be excluded from those totals. Future calls/late reporting can change the figures; refresh before final submission. The dashboard display does not establish additional decimal precision beyond $0.02.

| Cost category | Token total | Money total | Evidence to obtain |
| --- | --- | --- | --- |
| Codex development, including diagram agents | Project-specific historical raw token total unavailable | Project-specific money cost **not separable from available records** | Owner explicitly confirmed Nov14 that this week's development usage includes other work. Account subscription price and account-wide allowance consumption must not be attributed to this project. |
| Application API development/testing | Dashboard aggregate138,179 (scope above) | Dashboard rounded USD$0.02 (scope above) | Actual project analytics inspected. Per-repository attribution and invoice precision are not available from this aggregate. |
| Earlier lost AI-agent session | Unknown | Unknown | Owner's invoice or usage record, if available. |

The application client currently discards the provider's usage metadata and retains response content. The successful core audit logged **15 calls**, but this is only that run, not all development usage, retries or CLI/GUI calls. Do not report15 as the project-wide total. Use the actual dashboard aggregate with its scope and precision above; do not substitute current model price estimates for measured spend.

This documentation includes no API keys, local `.env` contents, private billing details or raw uploaded course documents.

The Codex native **Settings → Usage → Overview** displayed ChatGPT Pro200 at $200/month. This is account plan context only, **not this project's cost**. **Analytics →30d** selected Oct16–Nov14,2026; expanding this chat, “Explain Project2 requirements,” showed GPT-6.1Sol / High / Standard. Its row used allowance/credit metrics rather than raw project tokens or an invoice. The owner explicitly clarified that this week's development usage belongs to other work as well. Therefore neither the weekly account graph nor the monthly subscription fee is entered as an EECS3311 development expense, and no percentage-to-dollar allocation is made. No subscription, payment, plan limit or account setting was changed during this read-only inspection.
