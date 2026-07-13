# Hive Management UI Module Index

This directory is the maintenance index for the Hive management UI. Each module file
records current functions, interfaces, permissions, state flow, UI implementation,
Element Plus migration decisions, risks, and verification requirements.

The migration design is documented in
[`2026-07-12-management-ui-element-plus-migration-design.md`](../superpowers/specs/2026-07-12-management-ui-element-plus-migration-design.md).

## Status Legend

- `Audit baseline`: current behavior is documented; migration is not complete.
- `Batch 1`: foundation and standard administration modules.
- `Batch 2`: operational administration modules.
- `Batch 3`: complex workflow, printing, and editing modules.
- `Protected custom surface`: standard controls may migrate, but the named domain
  surface remains custom until separately designed and verified.

## Module Registry

| Module                   | Route / scope                           | Migration                               | Documentation                                         |
| ------------------------ | --------------------------------------- | --------------------------------------- | ----------------------------------------------------- |
| Shared foundation        | Application-wide                        | Batch 1                                 | [Shared foundation](modules/shared-foundation.md)     |
| Layout and navigation    | Application shell                       | Batch 2                                 | [Layout and navigation](modules/layout-navigation.md) |
| Authentication and entry | `/login`, join, password, legal, denied | Batch 2                                 | [Authentication](modules/authentication.md)           |
| Dashboard                | `/dashboard`                            | Batch 2                                 | [Dashboard](modules/dashboard.md)                     |
| Announcements            | `/function/announcement`                | Batch 1                                 | [Announcements](modules/announcement.md)              |
| Customer management      | `/function/customer`                    | Batch 1                                 | [Customer](modules/customer.md)                       |
| Organization departments | `/function/organization`                | Batch 1                                 | [Organization](modules/organization.md)               |
| Role management          | `/function/role`                        | Batch 1                                 | [Role](modules/role.md)                               |
| Document management      | `/function/document`                    | Batch 1                                 | [Document](modules/document.md)                       |
| Equipment management     | `/function/equipment`                   | Batch 1                                 | [Equipment](modules/equipment.md)                     |
| Employee management      | `/function/employee`                    | Batch 2                                 | [Employee](modules/employee.md)                       |
| Tenant management        | `/function/tenant`                      | Batch 2                                 | [Tenant](modules/tenant.md)                           |
| Price management         | `/function/price`                       | Batch 2                                 | [Price](modules/price.md)                             |
| Installation tasks       | `/function/installation-task`           | Batch 2                                 | [Installation task](modules/installation-task.md)     |
| Quality and after-sales  | `/function/bad-product`                 | Batch 2                                 | [Quality](modules/quality.md)                         |
| Approval center          | `/function/approval`                    | Batch 2                                 | [Approval](modules/approval.md)                       |
| Attendance               | `/function/attendance`                  | Batch 2                                 | [Attendance](modules/attendance.md)                   |
| Order management         | `/function/order`                       | Batch 3                                 | [Order](modules/order.md)                             |
| Inventory management     | `/function/inventory`                   | Batch 3                                 | [Inventory](modules/inventory.md)                     |
| Inventory model detail   | `/function/inventory/model-detail`      | Batch 3                                 | [Inventory detail](modules/inventory-detail.md)       |
| Receipt printing         | `/function/receipt`                     | Batch 3, protected print DOM            | [Receipt printing](modules/receipt-print.md)          |
| Label printing           | `/function/label`                       | Batch 3, protected print/canvas surface | [Label](modules/label.md)                             |
| User manual              | `/manual`                               | Batch 3, protected content editor       | [Manual](modules/manual.md)                           |

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
