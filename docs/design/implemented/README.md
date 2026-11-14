# Implementation diagrams — 14 November 2026

These describe the current Java implementation. The original Stage 1 report and diagrams remain the baseline in the parent directory. This is supporting material for the later implementation report, not the final report itself.

| Set | Editable source | Rendered views and traceability |
| --- | --- | --- |
| Architecture and classes: 7 sheets | [Combined draw.io](architecture/implemented-architecture-and-classes.drawio) | [Architecture index](architecture/README.md) — SVG and PNG for each sheet |
| Behavior: 17 sequence sheets and 3 use-case sheets | [Combined draw.io](behavior/Stage2_Behavior_Diagrams.drawio) | [Behavior index](behavior/README.md) — SVG and PNG for each sheet |

The sets were made in parallel from the source code, checked against real class/method names, parsed as XML and rendered for visual inspection. Selected relationships are split across sheets for legibility. Use the vector exports in the later report; do not shrink the entire class model onto a single unreadable page.

Six distinct GoF patterns are supported by the implementation. Simple Factory is documented separately. In particular, the Observer is the store-to-GUI callback collaboration, not a nonexistent timer Observer hierarchy. The timer uses Swing polling. The index files explicitly record other differences from Stage 1.

Runtime evidence and feature coverage are tracked in [the development audit](../../development/README.md). A diagram documents a design; it does not establish that an AI response will always be correct.

The Nov14 follow-up refreshes SD01a, SD10a, SD10b and SD13 for optional pre-upload AI review, the quiz workspace and adaptive full-document classification. Architecture/class sheets show selected relationships from Nov13; the added helper/UI components are identified in their index.
