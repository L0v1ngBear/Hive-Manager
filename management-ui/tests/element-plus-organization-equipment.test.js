import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import { buildEquipmentExport } from "../src/views/function/equipment/equipmentExport.js";
import { createLatestRequestGate, planEquipmentDrawerOpen, resolveEquipmentAccess, resolveInspectionEquipmentId } from "../src/views/function/equipment/equipmentAccess.js";
import { createOverviewRequestGate } from "../src/views/function/organization/overviewRequestGate.js";

const read = (path) => readFileSync(new URL(path, import.meta.url), "utf8");

const between = (source, start, end) => {
  const startIndex = source.indexOf(start);
  const endIndex = source.indexOf(end, startIndex);
  assert.ok(startIndex >= 0, `missing start marker: ${start}`);
  assert.ok(endIndex > startIndex, `missing end marker: ${end}`);
  return source.slice(startIndex, endIndex);
};

test("organization keeps its domain tree and migrates standard editor controls", () => {
  const source = read("../src/views/function/organization/organization.vue");
  for (const tag of [
    "el-drawer",
    "el-form",
    "el-input",
    "el-select",
    "el-input-number",
  ]) {
    assert.match(source, new RegExp(`<${tag}\\b`));
  }
  assert.match(source, /const DepartmentNode\s*=\s*defineComponent/);
  assert.match(source, /<el-tabs\b/);
  assert.match(source, /<el-table\b/);
  assert.match(source, /openPositionCreate/);
  assert.match(source, /submitPosition/);
});

test("organization clears an Element Plus parent selector back to the root value", () => {
  const source = read("../src/views/function/organization/organization.vue");
  assert.match(source, /:value-on-clear="''"/);
});

test("equipment uses Element Plus table pagination and drawers", () => {
  const source = read("../src/views/function/equipment/equipment.vue");
  for (const tag of [
    "el-table",
    "el-pagination",
    "el-drawer",
    "el-form",
    "el-input-number",
  ]) {
    assert.match(source, new RegExp(`<${tag}\\b`));
  }
});

test("equipment renders one empty state through the ElTable empty slot", () => {
  const source = read("../src/views/function/equipment/equipment.vue");
  const table = source.match(/<el-table\b[\s\S]*?<\/el-table>/)?.[0];

  assert.ok(table, "equipment table should exist");
  assert.match(table, /<template\s+#empty>[\s\S]*?<el-empty\b[\s\S]*?<\/template>/);
  assert.equal((table.match(/<el-empty\b/g) || []).length, 1);

  const afterTable = source.slice(source.indexOf(table) + table.length);
  const beforePagination = afterTable.slice(0, afterTable.indexOf('<div class="table-footer">'));
  assert.doesNotMatch(beforePagination, /<el-empty\b/);
});

test("equipment clears stale rows and persists distinct request failures with retry", () => {
  const source = read("../src/views/function/equipment/equipment.vue");
  const fetchDevices = between(source, "async function fetchDevices()", "function handleSearch()");
  const clearIndex = fetchDevices.indexOf("devices.value = []");
  const requestIndex = fetchDevices.indexOf("getEquipmentPage(queryParams.value)");

  assert.ok(clearIndex >= 0 && clearIndex < requestIndex, "stale equipment rows must clear before requesting");
  assert.match(fetchDevices, /listFailure\.value = null/);
  assert.match(fetchDevices, /catch \(error\)[\s\S]*listFailure\.value = resolveRequestFailure\(error/);
  assert.match(source, /const listFailure = ref\(null\)/);
  assert.match(source, /v-if="listFailure"/);
  assert.match(source, /@click="fetchDevices"[^>]*>重试<\/el-button>/);
  assert.match(source, /function getRequestStatusCode\(error\)[\s\S]*error\?\.response\?\.status[\s\S]*error\?\.response\?\.data\?\.code[\s\S]*error\?\.statusCode[\s\S]*error\?\.code/);
  assert.match(source, /statusCode === 401/);
  assert.match(source, /statusCode === 403/);
  assert.match(source, /登录状态已失效/);
  assert.match(source, /暂无权限查看设备列表/);
  assert.match(source, /设备列表加载失败/);
});

test("organization clears members and only applies the latest department request", () => {
  const source = read("../src/views/function/organization/organization.vue");
  const selectDepartment = between(source, "async function selectDepartment(node)", "function openCreate(parent)");
  const clearIndex = selectDepartment.indexOf("members.value = []");
  const requestIndex = selectDepartment.indexOf("getDepartmentEmployees(node.id)");

  assert.ok(clearIndex >= 0 && clearIndex < requestIndex, "old department members must clear before requesting");
  assert.match(source, /let memberRequestId = 0/);
  assert.match(selectDepartment, /const requestId = \+\+memberRequestId/);
  assert.match(selectDepartment, /if \(requestId !== memberRequestId\) return/);
  assert.match(selectDepartment, /catch \(error\)[\s\S]*memberFailure\.value = resolveMemberFailure\(error/);
  assert.match(selectDepartment, /finally[\s\S]*requestId === memberRequestId[\s\S]*memberLoading\.value = false/);
  assert.match(source, /const memberFailure = ref\(null\)/);
  assert.match(source, /v-if="memberFailure"/);
  assert.match(source, /@click="retryMembers"[^>]*>重试<\/el-button>/);
  assert.match(source, /function getRequestStatusCode\(error\)[\s\S]*error\?\.response\?\.status[\s\S]*error\?\.response\?\.data\?\.code[\s\S]*error\?\.statusCode[\s\S]*error\?\.code/);
  assert.match(source, /暂无权限查看部门成员/);
  assert.match(source, /部门成员加载失败/);
});

test("equipment explains its locked code and disables pagination while loading", () => {
  const source = read("../src/views/function/equipment/equipment.vue");
  const pagination = source.match(/<el-pagination\b[\s\S]*?\/>/)?.[0] || "";

  assert.match(source, /<el-tooltip\b[\s\S]*设备码已用于固定二维码，创建后不可修改。[\s\S]*<el-input\b[\s\S]*:disabled="!!editingId"/);
  assert.match(pagination, /:disabled="loading"/);
});

test("equipment export serializes the current page rows without table DOM clones", () => {
  const devices = [{ id: 7, equipmentName: "定型机01", equipmentCode: "EQ-001", equipmentType: "生产设备", location: "一车间", responsiblePerson: "张三", inspectionCycleDays: 0, lastInspectionTime: "2026-07-13T08:09:00", status: "enabled" }];
  const result = buildEquipmentExport(devices);
  assert.deepEqual(result.headers, ["设备", "类型/位置", "负责人", "最近巡检", "状态"]);
  assert.deepEqual(result.rows, [["定型机01 EQ-001", "生产设备 一车间", "张三", "2026-07-13 08:09", "启用中"]]);
  assert.equal(result.rows.length, devices.length, "fixed 操作列克隆不能产生重复导出行");
});

test("organization overview has mutually exclusive failure and retry state", () => {
  const source = read("../src/views/function/organization/organization.vue");
  const fetchOverview = between(source, "async function fetchOverview()", "async function selectDepartment(node)");
  assert.match(source, /const overviewFailure = ref\(null\)/);
  assert.match(fetchOverview, /departments\.value = \[\][\s\S]*overviewFailure\.value = null[\s\S]*getOrganizationOverview/);
  assert.match(fetchOverview, /catch \(error\)[\s\S]*overviewFailure\.value = resolveOverviewFailure\(error/);
  assert.match(source, /v-if="overviewFailure"[\s\S]*@click="fetchOverview"[^>]*>重试<\/el-button>/);
});

test("equipment detail and records reject stale responses and expose retryable states", () => {
  const source = read("../src/views/function/equipment/equipment.vue");
  const openDetail = between(source, "async function openDetail(device)", "async function fetchRecords(");
  const fetchRecords = between(source, "async function fetchRecords(", "function closeDrawers()");
  assert.match(source, /let detailRequestId = 0/);
  assert.match(openDetail, /detail\.value = null[\s\S]*records\.value = \[\][\s\S]*const requestId = \+\+detailRequestId/);
  assert.match(openDetail, /if \(requestId !== detailRequestId\) return/);
  assert.match(fetchRecords, /const requestId = \+\+recordsRequestId[\s\S]*if \(requestId !== recordsRequestId\) return/);
  assert.match(source, /detailFailure/);
  assert.match(source, /recordsFailure/);
  assert.match(source, /retryDetail/);
  assert.match(source, /retryRecords/);
});

test("organization and equipment mutation commands stay visible but disabled with reasons", () => {
  const organization = read("../src/views/function/organization/organization.vue");
  const equipment = read("../src/views/function/equipment/equipment.vue");
  for (const permission of ['organization:department:manage', 'organization:department:delete', 'organization:position:manage', 'organization:position:delete']) {
    assert.match(organization, new RegExp(`hasPermission\\('${permission.replaceAll(':', '\\:')}'\\)`));
    assert.match(organization, new RegExp(`暂无 ${permission.replaceAll(':', '\\:')} 权限`));
  }
  for (const permission of ['equipment:create', 'equipment:update', 'equipment:disable', 'equipment:export']) {
    assert.match(equipment, new RegExp(`hasPermission\\('${permission.replaceAll(':', '\\:')}'\\)`));
    assert.match(equipment, new RegExp(`暂无 ${permission.replaceAll(':', '\\:')} 权限`));
  }
});

test("organization and equipment module records describe the migrated behavior consistently", () => {
  const organization = read("../../docs/management-ui/modules/organization.md");
  const equipment = read("../../docs/management-ui/modules/equipment.md");

  for (const staleStatement of [
    "overview 和成员请求没有本地 `catch`",
    "选择新部门前不会先清空旧 `members`",
    "已确认的成员请求竞态",
    "变更按钮无前端权限门",
  ]) {
    assert.doesNotMatch(organization, new RegExp(staleStatement));
  }
  for (const staleStatement of [
    "从 `ElTable` 的真实 DOM 根节点读取",
    "页面缺少 V3 设备写入权限的命令级禁用状态",
    "当前导出绑定真实 DOM 表格",
    "页面没有独立错误占位或重试面板",
  ]) {
    assert.doesNotMatch(equipment, new RegExp(staleStatement));
  }
});

test("equipment detail and inspection permissions remain independent", () => {
  const access = (permissions) => resolveEquipmentAccess((code) => permissions.includes(code));
  assert.deepEqual(access([]), { canViewList: false, canViewDetail: false, canViewInspection: false });
  assert.deepEqual(access(["equipment:detail"]), { canViewList: false, canViewDetail: true, canViewInspection: false });
  assert.deepEqual(access(["equipment:inspection:list"]), { canViewList: false, canViewDetail: false, canViewInspection: true });
  assert.deepEqual(access(["equipment:list", "equipment:detail", "equipment:inspection:list"]), { canViewList: true, canViewDetail: true, canViewInspection: true });
});

test("equipment list permission blocks the page API with an authorization state", () => {
  const source = read("../src/views/function/equipment/equipment.vue");
  const fetchDevices = between(source, "async function fetchDevices()", "function handleSearch()");
  assert.match(fetchDevices, /if \(!canViewList\.value\)[\s\S]*listFailure\.value = [\s\S]*kind: 'forbidden'[\s\S]*return/);
  assert.ok(fetchDevices.indexOf("return") < fetchDevices.indexOf("getEquipmentPage(queryParams.value)"));
});

test("equipment list applies only the latest deferred response", async () => {
  const gate = createLatestRequestGate();
  const applied = [];
  let resolveSlow;
  let resolveFast;
  const slow = new Promise((resolve) => { resolveSlow = resolve; });
  const fast = new Promise((resolve) => { resolveFast = resolve; });
  const run = async (promise) => {
    const requestId = gate.begin();
    const value = await promise;
    if (gate.isLatest(requestId)) applied.push(value);
  };
  const slowRun = run(slow);
  const fastRun = run(fast);
  resolveFast("新筛选");
  await fastRun;
  resolveSlow("旧筛选");
  await slowRun;
  assert.deepEqual(applied, ["新筛选"]);
});

test("inspection-only retry uses the stable selected device without detail data", () => {
  assert.equal(resolveInspectionEquipmentId({ id: 42 }, null), 42);
  const source = read("../src/views/function/equipment/equipment.vue");
  assert.match(source, /function retryRecords\(\)[\s\S]*resolveInspectionEquipmentId\(selectedDevice, detail\.value\)[\s\S]*fetchRecords\(equipmentId\)/);
});

test("equipment guards detail requests and hides inspection content without permission", () => {
  const source = read("../src/views/function/equipment/equipment.vue");
  const openDetail = between(source, "async function openDetail(device)", "async function fetchRecords(");
  const fetchRecords = between(source, "async function fetchRecords(", "function retryDetail()");
  assert.match(source, /content="暂无 equipment:detail 权限"[\s\S]*:disabled="!canViewDetail"[\s\S]*@click="openDetail\(row\)"/);
  assert.match(openDetail, /planEquipmentDrawerOpen\('detail', equipmentAccess\.value\)[\s\S]*if \(!plan\.open\) return/);
  assert.match(source, /content="暂无 equipment:inspection:list 权限"[\s\S]*:disabled="!canViewInspection"[\s\S]*@click="fetchRecords"/);
  assert.match(source, /<div v-if="canViewInspection" v-loading="recordsLoading"/);
  assert.match(fetchRecords, /if \(!canViewInspection\.value\) return/);
});

test("organization overview invalidates members and clears protected content before requesting", () => {
  const source = read("../src/views/function/organization/organization.vue");
  const fetchOverview = between(source, "async function fetchOverview()", "async function selectDepartment(node)");
  const requestIndex = fetchOverview.indexOf("getOrganizationOverview()")
  for (const statement of ["memberRequestId += 1", "activeDepartment.value = null", "members.value = []", "memberFailure.value = null", "memberLoading.value = false"]) {
    const index = fetchOverview.indexOf(statement)
    assert.ok(index >= 0 && index < requestIndex, `${statement} must run before overview request`)
  }
});

test("equipment drawer plans API requests from command and permission combinations", () => {
  const inspectionOnly = { canViewDetail: false, canViewInspection: true };
  const detailOnly = { canViewDetail: true, canViewInspection: false };
  const denied = { canViewDetail: false, canViewInspection: false };
  assert.deepEqual(planEquipmentDrawerOpen("inspection", inspectionOnly), { open: true, requestDetail: false, requestInspection: true });
  assert.deepEqual(planEquipmentDrawerOpen("detail", detailOnly), { open: true, requestDetail: true, requestInspection: false });
  assert.deepEqual(planEquipmentDrawerOpen("detail", denied), { open: false, requestDetail: false, requestInspection: false });
  assert.deepEqual(planEquipmentDrawerOpen("inspection", denied), { open: false, requestDetail: false, requestInspection: false });
});

test("equipment exposes independent detail and inspection commands and protected drawer blocks", () => {
  const source = read("../src/views/function/equipment/equipment.vue");
  const openInspection = between(source, "function openInspection(device)", "async function openDetail(device)");
  assert.match(source, /content="暂无 equipment:inspection:list 权限"[\s\S]*:disabled="!canViewInspection"[\s\S]*@click="openInspection\(row\)"[\s\S]*巡检记录/);
  assert.match(openInspection, /planEquipmentDrawerOpen\('inspection', equipmentAccess\.value\)[\s\S]*if \(!plan\.open\) return/);
  assert.match(openInspection, /fetchRecords\(device\.id\)/);
  assert.doesNotMatch(openInspection, /getEquipmentDetail|openDetail/);
  assert.match(source, /<el-descriptions v-if="canViewDetail && detail"/);
  assert.match(source, /<section v-if="canViewInspection" class="mt-8">/);
});

test("organization overview commits success failure finally and auto-select only for latest deferred request", async () => {
  const gate = createOverviewRequestGate();
  const state = { data: null, failure: null, loading: false, selected: [] };
  let resolveOld;
  let rejectOlder;
  const oldSuccess = new Promise((resolve) => { resolveOld = resolve; });
  const olderFailure = new Promise((resolve, reject) => { rejectOlder = reject; });
  const run = async (promise) => {
    const requestId = gate.begin();
    state.loading = true;
    try {
      const data = await promise;
      if (!gate.isLatest(requestId)) return;
      state.data = data;
      state.selected.push(data.id);
    } catch (error) {
      if (!gate.isLatest(requestId)) return;
      state.failure = error.message;
    } finally {
      if (gate.isLatest(requestId)) state.loading = false;
    }
  };
  const oldRun = run(oldSuccess);
  const olderFailureRun = run(olderFailure);
  const latestRun = run(Promise.resolve({ id: "new" }));
  await latestRun;
  resolveOld({ id: "old" });
  rejectOlder(new Error("old failure"));
  await Promise.all([oldRun, olderFailureRun]);
  assert.deepEqual(state, { data: { id: "new" }, failure: null, loading: false, selected: ["new"] });
});

test("organization overview guards every state commit and auto-select with its request gate", () => {
  const source = read("../src/views/function/organization/organization.vue");
  const fetchOverview = between(source, "async function fetchOverview()", "async function selectDepartment(node)");
  assert.match(source, /const overviewRequestGate = createOverviewRequestGate\(\)/);
  assert.match(fetchOverview, /const requestId = overviewRequestGate\.begin\(\)/);
  assert.match(fetchOverview, /if \(!overviewRequestGate\.isLatest\(requestId\)\) return[\s\S]*departments\.value/);
  assert.match(fetchOverview, /catch \(error\)[\s\S]*if \(!overviewRequestGate\.isLatest\(requestId\)\) return[\s\S]*overviewFailure\.value/);
  assert.match(fetchOverview, /finally[\s\S]*overviewRequestGate\.isLatest\(requestId\)[\s\S]*loading\.value = false/);
});
