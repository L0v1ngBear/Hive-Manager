import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const main = readFileSync(new URL("../src/main.js", import.meta.url), "utf8");
const plugin = readFileSync(
  new URL("../src/plugins/elementPlus.js", import.meta.url),
  "utf8",
);
const style = readFileSync(
  new URL("../src/style.css", import.meta.url),
  "utf8",
);

test("registers only the required Element Plus loading directive", () => {
  assert.match(main, /installElementPlusFoundation\(app\)/);
  assert.match(plugin, /ElLoadingDirective/);
  assert.match(
    plugin,
    /app\.directive\(['"]loading['"],\s*ElLoadingDirective\)/,
  );
  assert.doesNotMatch(main, /app\.use\(ElementPlus\)/);
});

test("maps Hive semantic control tokens into Element Plus variables", () => {
  for (const token of [
    "--ys-control-height",
    "--ys-control-radius",
    "--ys-focus-ring",
  ]) {
    assert.match(style, new RegExp(token));
  }
  assert.match(style, /--el-component-size:/);
  assert.match(style, /--el-border-radius-base:/);
});
