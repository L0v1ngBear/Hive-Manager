import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
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
