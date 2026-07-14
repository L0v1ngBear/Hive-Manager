# Order List Express Number Column Design

## Goal

Replace the order list's `备注` column with the logistics tracking number stored by the order form, while preserving the order remark field everywhere else.

## Scope

- Keep the existing `信息渠道` column and show only `informationChannel` in it.
- Replace the list column key `remark` with `expressNo` and label it `物流单号`.
- Render `row.expressNo`, with `未填写物流单号` as the empty state.
- Export `expressNo` under the `物流单号` header.
- Bump the local table-column storage version so existing browser column preferences do not leave the new column in the wrong position.
- Rename the column-specific CSS selector from `order-column-remark` to `order-column-expressNo` and keep the same stable width and wrapping behavior.

## Non-Goals

- Do not remove or rename the order form's `remark` field.
- Do not change database columns, backend DTOs, or order save behavior.
- Do not display the logistics company in the replacement column; the requested value is the logistics tracking number.

## Data Flow

The management order page already receives `expressNo` from `SalesOrderPageVO`. The table and Excel export will read that existing field directly, so no API or migration change is required.

## Verification

- A source-level regression test must fail while the list still uses `remark` and pass after the replacement.
- Existing order information-channel, permission, and build checks must remain green.
