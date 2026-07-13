# Organization Boss Root Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the company boss the single root of the employee organization chart and remove the synthetic `组织架构` chart node.

**Architecture:** Extract hierarchy construction and root selection into a pure JavaScript module beside the employee page. The Vue page renders the returned employee node and keeps presentation concerns local.

**Tech Stack:** Vue 3, `vue3-tree-org`, JavaScript ES modules, Node.js test runner.

## Global Constraints

- Do not change backend APIs or persisted supervisor relationships.
- Boss keywords are `老板`, `董事长`, `总经理`, `首席执行官`, and case-insensitive `CEO`.
- Other leaderless roots become direct display children of the selected boss.
- Never render a synthetic node labeled `组织架构` inside the chart.

---

### Task 1: Define Root Selection Behavior

**Files:**
- Create: `D:/HiveManager/management-ui/tests/employee-organization-root.test.js`
- Create later: `D:/HiveManager/management-ui/src/views/function/employee/employeeOrganization.js`

**Interfaces:**
- Consumes: employee rows with `id`, `name`, `positionName`, `leaderId`, and optional `leaderName`.
- Produces: `buildEmployeeHierarchy(source)` and `buildOrganizationChart(roots)`.

- [ ] Write tests for one root, keyword priority, case-insensitive CEO matching, descendant-count fallback, orphan reparenting, and absence of a synthetic label.
- [ ] Run `node --test tests/employee-organization-root.test.js` and verify it fails because the helper module does not exist.

### Task 2: Implement And Integrate The Pure Helper

**Files:**
- Create: `D:/HiveManager/management-ui/src/views/function/employee/employeeOrganization.js`
- Modify: `D:/HiveManager/management-ui/src/views/function/employee/employee.vue`

**Interfaces:**
- `buildEmployeeHierarchy(source: Array<Employee>): Array<EmployeeNode>` returns nested employee roots without mutating input.
- `buildOrganizationChart(roots: Array<EmployeeNode>): { data, topLevelCount, unassignedCount }` returns one employee root.

- [ ] Implement keyword scoring, descendant scoring, deterministic tie breaking, recursive `pid` normalization, and `isOrganizationRoot` marking.
- [ ] Import the helper in `employee.vue`, remove local hierarchy conversion functions and all `isVirtualRoot` rendering branches.
- [ ] Render the boss root with normal employee metadata and status while retaining prominent root styling.
- [ ] Drive `顶层人员` and `未设置上级` from the chart result.
- [ ] Run the focused test and verify it passes.

### Task 3: Verify Frontend Output

**Files:**
- Verify: `D:/HiveManager/management-ui/src/views/function/employee/employee.vue`

- [ ] Run all Node tests with `node --test tests/*.test.js`.
- [ ] Run `npm run build` and require a successful production build.
- [ ] Copy the generated `dist` into the canonical local deployment package and verify exact file hashes.
