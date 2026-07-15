# Hive Management UI Module Index

This directory is the maintenance index for the Hive management UI. Each module file
records current functions, interfaces, permissions, state flow, UI implementation,
Element Plus migration decisions, risks, and verification requirements.

The migration design is documented in
[`2026-07-12-management-ui-element-plus-migration-design.md`](../superpowers/specs/2026-07-12-management-ui-element-plus-migration-design.md).

## Status Legend

- `Element Plus migrated`: standard controls have completed migration and verification.
- `Element Plus migrated with protected custom surface`: standard controls are migrated,
  while the documented domain-specific rendering or interaction surface remains protected.
- `Batch 1`: foundation and standard administration modules.
- `Batch 2`: operational administration modules.
- `Batch 3`: complex workflow, printing, and editing modules.
- `Protected custom surface`: standard controls may migrate, but the named domain
  surface remains custom until separately designed and verified.

## Module Registry

| Module                   | Route / scope                           | Migration                               | Documentation                                         |
| ------------------------ | --------------------------------------- | --------------------------------------- | ----------------------------------------------------- |
| Shared foundation        | Application-wide                        | Element Plus migrated                   | [Shared foundation](modules/shared-foundation.md)     |
| Layout and navigation    | Application shell                       | Element Plus migrated                   | [Layout and navigation](modules/layout-navigation.md) |
| Authentication and entry | `/login`, join, password, legal, denied | Element Plus migrated                   | [Authentication](modules/authentication.md)           |
| Dashboard                | `/dashboard`                            | Element Plus migrated                   | [Dashboard](modules/dashboard.md)                     |
| Announcements            | `/function/announcement`                | Element Plus migrated                   | [Announcements](modules/announcement.md)              |
| Customer management      | `/function/customer`                    | Element Plus migrated                   | [Customer](modules/customer.md)                       |
| Organization departments | `/function/organization`                | Migrated; protected organization graph  | [Organization](modules/organization.md)               |
| Role management          | `/function/role`                        | Element Plus migrated                   | [Role](modules/role.md)                               |
| Document management      | `/function/document`                    | Element Plus migrated                   | [Document](modules/document.md)                       |
| Equipment management     | `/function/equipment`                   | Element Plus migrated                   | [Equipment](modules/equipment.md)                     |
| Employee management      | `/function/employee`                    | Element Plus migrated                   | [Employee](modules/employee.md)                       |
| Tenant management        | `/function/tenant`                      | Element Plus migrated                   | [Tenant](modules/tenant.md)                           |
| Price management         | `/function/price`                       | Element Plus migrated                   | [Price](modules/price.md)                             |
| Installation tasks       | `/function/installation-task`           | Element Plus migrated                   | [Installation task](modules/installation-task.md)     |
| Quality and after-sales  | `/function/bad-product`                 | Element Plus migrated                   | [Quality](modules/quality.md)                         |
| Approval center          | `/function/approval`                    | Element Plus migrated                   | [Approval](modules/approval.md)                       |
| Attendance               | `/function/attendance`                  | Element Plus migrated                   | [Attendance](modules/attendance.md)                   |
| Order management         | `/function/order`                       | Migrated; protected dynamic table       | [Order](modules/order.md)                             |
| Inventory management     | `/function/inventory`                   | Migrated; protected dynamic table       | [Inventory](modules/inventory.md)                     |
| Inventory model detail   | `/function/inventory/model-detail`      | Migrated; protected dynamic table       | [Inventory detail](modules/inventory-detail.md)       |
| Receipt printing         | `/function/receipt`                     | Migrated; protected print DOM           | [Receipt printing](modules/receipt-print.md)          |
| Label printing           | `/function/label`                       | Migrated; protected print/canvas        | [Label](modules/label.md)                             |
| User manual              | `/manual`                               | Migrated; protected content model       | [Manual](modules/manual.md)                           |

## Cross-module Rules

1. Interface paths, payload types, permissions, and business state transitions are
   invariants during style migration.
2. Use real Element Plus controls for standard fields, commands, tables, pagination,
   dialogs, drawers, empty states, and loading states.
3. Preserve print DOM, label canvas, barcode/QR output, organization visualization,
   dynamic-column export behavior, and specialized attachment/time correction
   components unless their module document explicitly approves replacement.
4. Update a module document in the same change that alters its functions, APIs,
   permissions, state flow, or migration status.
5. Record discovered permission or data-contract defects as risks; do not silently
   redefine backend behavior inside a visual migration.

## Verification Status

- Global teal theme CLI verification (2026-07-15): `npm test` passed **207/207** Node tests with zero failures, and `npm run build` completed successfully with Vite 8.1.3 after transforming 1,858 modules.
- The retired interactive-blue scan found **0** matches for `#1f3f5f`, `#0b1f33`, `rgba(31, 63, 95, ...)`, or `rgba(30, 64, 104, ...)` in `management-ui/src`. The required placeholder scan also found **0** matches.
- Controller browser QA verified the local Vite app at `http://127.0.0.1:5173/`:
  - Login at 1440x900 (`/login?redirect=/dashboard`) had no horizontal overflow or overlap. Computed `--color-primary` and `--ys-primary` were `#0f766e`, `--ys-on-primary` was `#ffffff`, and `--ys-disabled-text` was `#94a3b8`; the primary login button used a `rgb(17, 94, 89)` to `rgb(15, 118, 110)` gradient with `rgb(255, 255, 255)` text.
  - Login at 1024x768 had no horizontal overflow, retained the teal/white primary-button contrast, and required only normal vertical scrolling. At 390x844, it had no page-level horizontal overflow; the layout stacked cleanly, heading/copy/QR panel remained readable, and the below-fold primary button retained teal/white contrast. Decorative HIVE text extends visually, but document `scrollWidth` remains within the viewport.
  - JoinOrganization at 1024x768 had no horizontal overflow and retained a readable layout and teal/white primary confirmation button.
- Protected `/function/order` redirected to `/login?redirect=/function/order` because no local authenticated session was available and the local API surfaced HTTP 502. Order, employee, approval, inventory, installation, label, and receipt protected pages are **not** visually approved.
