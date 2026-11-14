# Implementation diagrams — 14 November 2026

These describe the current Java implementation. The original Stage 1 report and diagrams remain the baseline in the parent directory. This is supporting material for the later implementation report, not the final report itself.

| Set | Editable source | Rendered views and traceability |
| --- | --- | --- |
| Architecture and classes: 7 sheets | [Combined draw.io](architecture/implemented-architecture-and-classes.drawio) | [Architecture index](architecture/README.md) — SVG and PNG for each sheet |
| Behavior: 18 sequence sheets and 3 use-case sheets | [Combined draw.io](behavior/Stage2_Behavior_Diagrams.drawio) | [Behavior index](behavior/README.md) — SVG and PNG for each sheet |

The sets were made in parallel from the source code, checked against real class/method names, parsed as XML and rendered for visual inspection. Selected relationships are split across sheets for legibility. Use the vector exports in the later report; do not shrink the entire class model onto a single unreadable page.

Six distinct GoF patterns are supported by the implementation. Simple Factory is documented separately. In particular, the Observer is the store-to-GUI callback collaboration, not a nonexistent timer Observer hierarchy. The timer uses Swing polling. The index files explicitly record other differences from Stage 1.

Runtime evidence and feature coverage are tracked in [the development audit](../../development/README.md). A diagram documents a design; it does not establish that an AI response will always be correct.

The latest Nov14 follow-up refreshes architecture sheets 01/03 and SD01a/SD01b/SD09a/SD09b/SD10a/SD13 for complete upload preparation, image input, persistent page/word indexes, full-source quizzes, course-aware retrieval, exact-page citation links and complete classification. Selected helpers/UI components are identified in their index rather than presenting every Swing control as a domain class.

SD15 and UC21 now cover deterministic timeline drag selection and contextual single/bulk actions; total diagram sheets: 28.
