# Logistics Popover Design QA

- Source visual truth: `docs/quality/logistics-popover-reference.png`
- Implementation capture: `docs/quality/logistics-popover-implementation.png`
- Side-by-side comparison: `docs/quality/logistics-popover-comparison.png`
- Comparison viewport: 1400 × 800
- Implementation capture viewport: 1024 × 768
- Browser scale: 100%
- Component state: populated logistics result with carrier, waybill number, status, route summary, and three tracking events

## Evidence

The full popover is the focused region for this task, so the side-by-side artifact contains both the full-view and focused-region evidence. The 668 × 493 reference was normalized to 390 px wide beside the 390 × 307 implementation card.

## Iteration history

1. Initial implementation was too tall, visually split the header from the waybill, and used several theme variables that were not defined in the current design system.
2. Replaced undefined variables with existing Hive tokens, joined the header and waybill into one visual surface, strengthened the timeline line and dots, and reduced vertical padding.
3. Compressed the final card to 390 × 307 and repeated the side-by-side comparison.

## Findings

- P0: none
- P1: none
- P2: none
- P3: the implementation is 19 px taller after width normalization. This preserves clearer current-product typography and is within the accepted density target.
- Intentional difference: the implementation uses the product's existing teal primary color instead of the reference image's blue.
- Intentional difference: route origin and destination are only rendered when the API returns at least two real locations; the UI does not invent missing route data.

## Interaction and runtime checks

- The existing hover trigger still opens the popover.
- The copy-waybill control is keyboard focusable and has hover/focus styling.
- Copy handling is wired to the Clipboard API with success and failure feedback.
- No browser console errors were observed in the visual fixture.

final result: passed
