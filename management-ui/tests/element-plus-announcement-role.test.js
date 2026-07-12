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
  assert.match(permission, /const permissionLoadState = ref\('idle'\)/);
  assert.match(permission, /error\?\.response\?\.status/);
  assert.match(permission, /error\?\.code/);
  assert.match(permission, /\[401, 403\]\.includes\(statusCode\)/);
  assert.match(permission, /permissionLoadState === 'forbidden'/);
  assert.match(permission, /permissionLoadState === 'failed'/);
  assert.match(permission, /无权查看角色权限/);
  assert.match(permission, /权限数据加载失败/);
  assert.doesNotMatch(permission, /permissionLoadError/);
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
