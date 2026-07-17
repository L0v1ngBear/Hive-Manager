import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import test from "node:test";

const read = (path) => readFileSync(new URL(path, import.meta.url), "utf8");

test("announcement pages use Element Plus controls", () => {
  const list = read("../src/views/function/announcement/announcement.vue");
  const publish = read("../src/views/function/announcement/publish.vue");
  for (const tag of ["el-button", "el-empty"])
    assert.match(list, new RegExp(`<${tag}\\b`));
  for (const tag of ["el-form", "el-input", "el-select", "el-button"]) {
    assert.match(publish, new RegExp(`<${tag}\\b`));
  }
  assert.doesNotMatch(publish, /<(input|select|textarea|button)\b/);
});

test("role list and creation form use Element Plus controls", () => {
  const list = read("../src/views/function/role/role.vue");
  const create = read("../src/views/function/role/createRoleDrawer.vue");
  for (const tag of ["el-table", "el-table-column", "el-button", "el-empty"]) {
    assert.match(list, new RegExp(`<${tag}\\b`));
  }
  for (const tag of ["el-drawer", "el-form", "el-input", "el-tree-select"]) {
    assert.match(create, new RegExp(`<${tag}\\b`));
  }
});

test("role creation form prevents native submit with the existing handler", () => {
  const create = read("../src/views/function/role/createRoleDrawer.vue");
  assert.match(create, /<el-form\b(?=[^>]*@submit\.prevent="submit")[^>]*>/);
});

test("announcement list keeps loading, failure, empty and data states separate", () => {
  const list = read("../src/views/function/announcement/announcement.vue");
  assert.match(list, /const announcementLoadError = ref\(''\)/);
  assert.match(
    list,
    /<el-skeleton v-if="loading"[\s\S]*v-else-if="announcementLoadError"[\s\S]*<el-empty v-else-if="!announcements\.length"[\s\S]*<div v-else/,
  );
  assert.match(
    list,
    /catch \(error\) \{[\s\S]*announcements\.value = \[\][\s\S]*announcementLoadError\.value =/,
  );
  assert.match(list, /@click="loadAnnouncements"[\s\S]*重新加载/);
});

test("role list keeps request failure separate from its true empty state", () => {
  const list = read("../src/views/function/role/role.vue");
  assert.match(list, /const roleLoadError = ref\(''\)/);
  assert.match(
    list,
    /catch \(error\) \{[\s\S]*roles\.value = \[\][\s\S]*roleLoadError\.value =/,
  );
  assert.match(
    list,
    /<template #empty>[\s\S]*v-else-if="roleLoadError"[\s\S]*@click="fetchData"[\s\S]*<el-empty v-else/,
  );
});

test("permission drawer distinguishes authorization failures from request failures", () => {
  const permission = read("../src/views/function/role/permissionDrawer.vue");
  assert.match(permission, /createRolePermissionLoader/);
  assert.match(permission, /permissionLoader\.state\.loadState === 'forbidden'/);
  assert.match(permission, /permissionLoader\.state\.loadState === 'failed'/);
  assert.match(permission, /permissionLoader\.state\.loadState === 'empty'/);
  assert.match(permission, /无权查看角色权限/);
  assert.match(permission, /权限数据加载失败/);
  assert.doesNotMatch(permission, /permissionLoadError/);
});

test("permission drawer keeps full group actions separate from filtered checkbox layout", () => {
  const permission = read("../src/views/function/role/permissionDrawer.vue");
  const checkboxRule = permission.match(
    /\.permission-checkbox-list :deep\(\.el-checkbox\) \{([^}]*)\}/,
  )?.[1] ?? "";

  assert.match(
    permission,
    /groupActionIds\(permissionLoader\.state\.treeData,\s*group\.id\)/,
  );
  assert.match(checkboxRule, /display:\s*flex/);
  assert.match(checkboxRule, /align-items:\s*flex-start/);
  assert.doesNotMatch(checkboxRule, /grid-template-columns/);
});

test("latest request guard rejects stale data, error and loading writes", async () => {
  const helperUrl = new URL(
    "../src/views/function/announcement/latestRequestGuard.js",
    import.meta.url,
  );
  assert.equal(existsSync(helperUrl), true, "latest request guard must exist");

  const { createLatestRequestGuard } = await import(helperUrl);
  const guard = createLatestRequestGuard();
  const state = { data: [], error: "", loading: false };
  const firstRequestId = guard.begin();
  guard.commit(firstRequestId, () => {
    state.loading = true;
  });

  const latestRequestId = guard.begin();
  guard.commit(latestRequestId, () => {
    state.data = ["latest"];
    state.loading = true;
  });

  assert.equal(
    guard.commit(firstRequestId, () => {
      state.data = ["stale"];
      state.error = "stale failure";
    }),
    false,
  );
  assert.equal(
    guard.commit(firstRequestId, () => {
      state.loading = false;
    }),
    false,
  );
  assert.deepEqual(state, { data: ["latest"], error: "", loading: true });

  assert.equal(
    guard.commit(latestRequestId, () => {
      state.loading = false;
    }),
    true,
  );
  assert.equal(state.loading, false);
});

test("announcement list commits every async state through the latest request guard", () => {
  const list = read("../src/views/function/announcement/announcement.vue");
  assert.match(list, /createLatestRequestGuard/);
  assert.match(list, /const requestId = announcementRequestGuard\.begin\(\)/);
  assert.equal(
    list.match(/announcementRequestGuard\.commit\(requestId/g)?.length,
    4,
  );
  assert.match(
    list,
    /finally \{[\s\S]*announcementRequestGuard\.commit\(requestId,[\s\S]*loading\.value = false/,
  );
});

test("role table empty slot suppresses error and empty states while loading", () => {
  const list = read("../src/views/function/role/role.vue");
  assert.match(
    list,
    /<template #empty>[\s\S]*v-if="loading"[\s\S]*v-else-if="roleLoadError"[\s\S]*<el-empty v-else/,
  );
});

test("permission loader keeps only the latest role response when requests finish out of order", async () => {
  const { createRolePermissionLoader } = await import(
    "../src/views/function/role/permissionLoaders.js"
  );
  const pending = new Map();
  const deferred = (key) => new Promise((resolve, reject) => pending.set(key, { resolve, reject }));
  const loader = createRolePermissionLoader({
    getAllPermissions: () => deferred(`tree-${pending.size}`),
    getRolePermissionIds: (roleId) => deferred(`owned-${roleId}`),
  });

  const first = loader.load(11);
  const second = loader.load(22);
  pending.get("tree-2").resolve([{ id: 220, permName: "新角色权限" }]);
  pending.get("owned-22").resolve([220]);
  await second;
  assert.deepEqual(loader.state.treeData, [{ id: 220, permName: "新角色权限" }]);
  assert.deepEqual(loader.state.checkedPermissionIds, [220]);
  assert.equal(loader.state.loadState, "ready");
  assert.equal(loader.state.loading, false);

  pending.get("tree-0").resolve([{ id: 110, permName: "旧角色权限" }]);
  pending.get("owned-11").resolve([110]);
  await first;
  assert.deepEqual(loader.state.treeData, [{ id: 220, permName: "新角色权限" }]);
  assert.deepEqual(loader.state.checkedPermissionIds, [220]);
  assert.equal(loader.state.loadState, "ready");
  assert.equal(loader.state.loading, false);
});

test("permission caller does not roll back user edits when an older role request finishes", async () => {
  const { createRolePermissionLoader, syncCommittedPermissionIds } = await import(
    "../src/views/function/role/permissionLoaders.js"
  );
  const pending = new Map();
  const deferred = (key) => new Promise((resolve) => pending.set(key, resolve));
  const loader = createRolePermissionLoader({
    getAllPermissions: () => deferred(`tree-${pending.size}`),
    getRolePermissionIds: (roleId) => deferred(`owned-${roleId}`),
  });
  let selectedIds = [];

  const first = loader.load(11);
  const second = loader.load(22);
  pending.get("tree-2")([{ id: 220, permName: "新角色权限" }]);
  pending.get("owned-22")([220]);
  selectedIds = syncCommittedPermissionIds(selectedIds, await second);
  selectedIds = [220, 221];

  pending.get("tree-0")([{ id: 110, permName: "旧角色权限" }]);
  pending.get("owned-11")([110]);
  selectedIds = syncCommittedPermissionIds(selectedIds, await first);
  assert.deepEqual(selectedIds, [220, 221]);
});

test("permission caller ignores an older retry for the same role", async () => {
  const { createRolePermissionLoader } = await import(
    "../src/views/function/role/permissionLoaders.js"
  );
  const trees = [];
  const owned = [];
  const loader = createRolePermissionLoader({
    getAllPermissions: () => new Promise((resolve) => trees.push(resolve)),
    getRolePermissionIds: () => new Promise((resolve) => owned.push(resolve)),
  });
  const first = loader.load(33);
  const second = loader.load(33);
  trees[1]([{ id: 330, permName: "最新权限" }]);
  owned[1]([330]);
  const latestResult = await second;
  assert.equal(latestResult.committed, true);

  trees[0]([{ id: 331, permName: "旧权限" }]);
  owned[0]([331]);
  const staleResult = await first;
  assert.equal(staleResult.committed, false);
  assert.deepEqual(latestResult.checkedPermissionIds, [330]);
});

test("create-role permission loader separates forbidden, failed and empty outcomes", async () => {
  const { createPermissionTreeLoader, permissionTreeCanSubmit } = await import(
    "../src/views/function/role/permissionLoaders.js"
  );
  const outcomes = [
    [{ code: 401 }, "forbidden"],
    [{ code: 403 }, "forbidden"],
    [{ response: { status: 401 } }, "forbidden"],
    [{ response: { status: 403 } }, "forbidden"],
    [{ code: 500 }, "failed"],
    [new Error("network down"), "failed"],
  ];

  for (const [error, expected] of outcomes) {
    const loader = createPermissionTreeLoader({ getAllPermissions: async () => { throw error; } });
    await loader.load();
    assert.equal(loader.state.loadState, expected);
    assert.equal(loader.state.loading, false);
    assert.deepEqual(loader.state.treeData, []);
  }

  const emptyLoader = createPermissionTreeLoader({ getAllPermissions: async () => [] });
  await emptyLoader.load();
  assert.equal(emptyLoader.state.loadState, "empty");
  assert.equal(emptyLoader.state.loading, false);
  assert.deepEqual(emptyLoader.state.treeData, []);
  assert.equal(permissionTreeCanSubmit("empty"), true);
  assert.equal(permissionTreeCanSubmit("ready"), true);
  assert.equal(permissionTreeCanSubmit("loading"), false);
  assert.equal(permissionTreeCanSubmit("forbidden"), false);
  assert.equal(permissionTreeCanSubmit("failed"), false);
});
