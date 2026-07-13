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

- Full management UI verification passes: 154 Node tests, ESLint, and the Vite production build.
- Responsive browser inspection at 1440×900, 1024×768, and 390×844 confirms the login shell has no page-level horizontal overflow; the mobile layout stacks correctly.
- Authenticated route visual QA remains environment-dependent: the local API currently returns HTTP 502 and the router correctly redirects order, inventory, receipt, label, and manual routes to login. Do not treat an unauthenticated redirect as visual approval of those protected pages.
