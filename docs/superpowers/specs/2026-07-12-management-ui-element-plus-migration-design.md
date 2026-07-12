# Management UI Element Plus Migration Design

## Context

The Hive management UI already depends on Element Plus 2.13 and uses its message,
message box, dialog, drawer, tree, and select components in a few modules. Most
business pages still implement buttons, fields, tables, pagination, dialogs, and
drawers with native elements plus repeated Tailwind or scoped CSS.

This migration standardizes suitable UI controls without changing business APIs,
permission semantics, order states, print output, or domain-specific editors. It
also creates a durable module record so future work can identify each page's
functions, interfaces, permissions, state flow, risks, and verification status.

## Goals

1. Use Element Plus for standard management controls where it improves consistency,
   accessibility, validation, loading behavior, and maintenance.
2. Keep the existing restrained Hive visual language by mapping the current semantic
   tokens to Element Plus variables instead of accepting an unrelated default theme.
3. Preserve every existing API contract, emitted event, query parameter, permission
   check, and business state transition during component replacement.
4. Document every management module with a repeatable structure covering functions,
   APIs, permissions, data flow, UI implementation, risks, and verification.
5. Deliver the migration in reviewable batches so complex printing and operational
   workflows retain a stable baseline.

## Non-goals

- No backend endpoint or database schema changes are part of the style migration.
- The migration does not invent new permission codes. Permission mismatches found by
  the audit are recorded as risks and require a separate behavior decision.
- Print DOM, millimeter sizing, page-break CSS, barcode/QR rendering, label canvas,
  and organization visualization are not replaced merely to increase Element Plus
  usage.
- The app will not globally install all of Element Plus solely for convenience.
- Existing page composition and information architecture remain intact unless a
  component replacement requires a small layout adjustment.

## Considered Approaches

### Chosen: phased component replacement

Replace standard controls module by module, beginning with shared foundations and
lower-risk administration pages. This produces real Element Plus behavior while
keeping diffs, QA, and rollback manageable.

### Rejected: full application replacement in one release

This would produce the fastest visual convergence but combines unrelated workflows,
permission matrices, responsive tables, and printing behavior into one high-risk
change set.

### Rejected: CSS-only imitation

Restyling native elements to resemble Element Plus would keep duplicate validation,
focus, loading, dialog, and pagination implementations. It would not achieve the
requested component standardization.

## Migration Batches

### Batch 1: foundation and standard administration pages

- Shared Element Plus theme bridge and loading directive registration.
- Announcement list and publishing.
- Customer list, detail dialog, and create/edit drawer.
- Organization department tree, member panel, and department drawer.
- Role list, creation form, and existing permission drawers.
- Document filters, list, folder dialog, and upload actions.
- Equipment filters, list, detail drawer, and editor.
- Shared filter, dialog, pagination, empty, and loading conventions used by these
  pages.

### Batch 2: operational administration pages

- Employee, tenant, price, installation task, quality, approval, attendance,
  dashboard, navigation, and authentication entry pages.

### Batch 3: complex operational and output pages

- Order, inventory, inventory detail, receipt printing, label printing, and the
  editable user manual.
- Standard controls may migrate, but print/canvas/graph/editor internals remain
  purpose-built unless separately designed and verified.

## Component Mapping

| Existing pattern          | Element Plus target                      | Notes                                                                                                          |
| ------------------------- | ---------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| Native command button     | `ElButton`                               | Preserve permission directive, disabled reason, loading, and event propagation.                                |
| Text/search input         | `ElInput`                                | Preserve trimming, keyboard submit, and query serialization.                                                   |
| Numeric input             | `ElInputNumber`                          | Set explicit min, max, step, precision, and disabled rules where contracts exist.                              |
| Native select             | `ElSelect` + `ElOption`                  | Preserve value types; numeric values must not become strings.                                                  |
| Date/time input           | `ElDatePicker` / `ElTimePicker`          | Set explicit `value-format` matching the current API.                                                          |
| Checkbox/radio/toggle     | `ElCheckbox`, `ElRadioGroup`, `ElSwitch` | Preserve Boolean versus numeric payload conventions.                                                           |
| Handwritten dialog/drawer | `ElDialog` / `ElDrawer`                  | Preserve close guards, focus target, unsaved data, and responsive width.                                       |
| Handwritten list table    | `ElTable`                                | Migrate only after dynamic columns, row-click isolation, responsive behavior, and export bindings are covered. |
| Handwritten pagination    | `ElPagination`                           | Preserve server page numbering and page-size behavior.                                                         |
| Empty/loading blocks      | `ElEmpty`, `ElSkeleton`, `v-loading`     | Network errors remain distinct from real empty results.                                                        |
| Status pill               | `ElTag`                                  | Keep existing business color semantics and text.                                                               |
| Destructive confirmation  | `ElMessageBox` / `ElPopconfirm`          | Preserve cancellation as a normal non-error outcome.                                                           |

## Element Plus Integration

- Continue importing components and services explicitly from `element-plus`.
- Register only the Element Plus Loading directive required by templates; do not use
  `app.use(ElementPlus)` and pull the complete JavaScript plugin into the bundle.
- Keep the Chinese locale through the existing `ElConfigProvider`.
- Define one semantic token source for primary colors, text, borders, radii, control
  height, shadows, and focus rings. Map those tokens into both Tailwind and `--el-*`
  variables.
- Avoid page-level `!important` overrides unless an Element Plus state cannot be
  expressed through supported variables or component props.

## Behavioral Invariants

1. API modules and endpoint paths remain unchanged during a style-only patch.
2. Request payload value types and date formats remain byte-for-byte compatible.
3. Existing `v-permission`, command-level checks, disabled explanations, and route
   guards remain attached to the equivalent command.
4. Row clicks, button `.stop` behavior, keyboard submit, drawer close behavior, and
   local column persistence remain intact.
5. Loading, empty, permission-denied, and request-error states are visually distinct.
6. A failed request must not display stale data under a newly selected entity.
7. Print and label output is verified independently from the management page shell.

## Documentation Structure

`docs/management-ui/README.md` is the module index. Every file under
`docs/management-ui/modules/` records:

- source files, route, feature flag, and migration batch;
- user-visible functions;
- API wrapper, HTTP method, endpoint, and purpose;
- permission checks and known front/back mismatch;
- key state and data flow;
- loading, empty, error, and permission states;
- current controls and proposed Element Plus mapping;
- components that must remain custom;
- risks and a verification checklist;
- current migration status.

## Testing And Visual QA

- Add a failing source-contract test before each batch to identify its intended
  Element Plus components and protected custom areas.
- Run targeted ESLint, the complete Node test suite, and a production Vite build.
- Verify each migrated page at desktop and narrow mobile widths.
- Exercise loading, empty, error, disabled, permission-denied, and long-content states.
- Validate keyboard focus, Escape behavior, drawer/dialog focus restoration, and
  reduced-motion behavior.
- For tables, verify row actions, dynamic columns, pagination, export, and responsive
  labels. For print pages, compare physical/page-preview output before and after.

## Rollback

Each module is committed independently where practical. A module can be reverted
without reverting the shared token bridge, provided it does not depend on a newly
introduced component contract. Deployment artifacts are rebuilt only after the full
selected batch passes verification.

## Acceptance Criteria

- The module index and every listed module file exist and reflect current source.
- Batch 1 standard controls use actual Element Plus components, not look-alike CSS.
- Protected custom output and visualization surfaces remain unchanged.
- No API path, payload contract, permission rule, or business transition regresses.
- Tests, lint, build, responsive screenshots, and deployment artifact comparison pass.
