# Organization Boss Root Design

Date: 2026-07-12

## Objective

Render the company boss as the single top node of the employee organization chart. The chart must no longer insert a synthetic node labeled `组织架构` when multiple employees have no configured supervisor.

## Root Selection

Only employees already identified as hierarchy roots are eligible. Rank them by these signals, in order:

1. Position or employee name contains `老板`, `董事长`, `总经理`, `首席执行官`, or `CEO`.
2. The root has the greatest number of descendants.
3. The original employee-list order provides a deterministic final tie break.

The selected employee becomes the chart root. Any other employee with no valid supervisor is displayed directly below the selected boss. Existing valid supervisor relationships are unchanged.

## Presentation

- Keep the drawer title `组织架构`.
- Remove all synthetic-root rendering branches and synthetic `account_tree` cards.
- Mark the selected boss node as the organization root so it keeps the visually prominent root treatment while still showing the employee name, department, position, and status.
- Display one top-level person whenever employee data exists.
- Count other leaderless roots as `未设置上级`; the expected boss itself is not counted as an error.
- Preserve collapse, zoom, restore, and existing responsive behavior.

## Implementation Boundary

Move hierarchy and root-selection behavior into a small pure JavaScript module beside `employee.vue`. The Vue page consumes the module and remains responsible only for loading data and rendering. No backend API or database change is required.

## Verification

Automated tests must prove:

- a single existing root remains the root;
- a boss keyword wins when several roots exist;
- keyword matching is case-insensitive for `CEO`;
- fallback selects the root with the most descendants;
- other leaderless roots are reparented under the boss;
- the output contains no synthetic `组织架构` node;
- the page template no longer branches on `isVirtualRoot`.

