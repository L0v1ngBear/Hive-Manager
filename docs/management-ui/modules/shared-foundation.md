# 共享基础维护档案

## Global Teal Theme Contract (2026-07-15)

**Migration status:** The management UI global teal migration is complete at the
shared-foundation layer. CLI verification is recorded in the
[`management UI README`](../README.md#verification-status).

Controller browser QA passed the recorded contrast and overflow checks for public
entry screens: Login at 1440x900, 1024x768, and 390x844, plus JoinOrganization
at 1024x768. Protected business pages remain unapproved: `/function/order`
redirected to login because no local authenticated session was available and the
local API surfaced HTTP 502.

## Canonical Semantic Palette

| Token | Value |
| --- | --- |
| Primary | `#0F766E` |
| Primary dark | `#115E59` |
| Primary container | `#CCFBF1` |
| On-primary-container | `#134E4A` |
| On-primary | `#FFFFFF` |
| Main text | `#0F172A` |
| Secondary text | `#475569` |
| Disabled text | `#94A3B8` |
| Disabled background | `#E2E8F0` |

`management-ui/src/style.css` is the shared contract for Tailwind, Hive semantic,
RGB compatibility, and Element Plus theme variables.

## Cascade and Preservation Rules

- Deep-primary surfaces (`.bg-primary`, `.el-button--primary`, and
  `.function-action-primary`) set their foreground to `--ys-on-primary`; the
  descendant rule runs after global text utilities so nested text and Material
  Symbols stay white.
- Non-disabled `.bg-primary`, Element Plus, and shared function primary controls use
  `--ys-primary-hover` on hover and `--ys-primary-dark` while active, with
  `--ys-on-primary` foregrounds in both states.
- All native disabled buttons and `aria-disabled="true"` button-like controls,
  including primary, warning, and success variants, use neutral
  `--ys-disabled-text` and `--ys-disabled-bg`, keep nested text and icons neutral,
  remove colored shadows, and hold opacity at `1`. Local enabled status colors
  must not outrank this shared disabled cascade. Permission directives and named
  permission-disabled state classes use the same contract, and disabled function
  actions do not apply hover or active transforms.
- Error red, warning amber/yellow/orange, success green, order-stage colors,
  chart palettes, and black/white print-only rules are semantic exceptions and
  are not recolored by the teal migration. Warning surfaces, borders, rings, and
  gradients retain Tailwind semantics; the compatibility layer only darkens
  warning-family foreground utilities where normal text needs readable contrast.
  Semantic text and meaningful icons may not use alpha-primary foregrounds;
  decorative watermarks require an explicit source-test allowlist and rationale.
  Receipt and label print output remain outside the interactive retired-color scan.

> 状态：Foundation migrated；迁移批次：Batch 1；范围：`management-ui` 应用级依赖、根入口、全局样式、请求状态与公共组件。

## 功能与边界

- 提供 Vue 应用启动、Pinia、Router、权限指令和全局样式入口。
- 在根组件统一挂载合规页脚、全局请求遮罩和路由视图。
- 提供页面响应式容器、表格列设置、附件拖拽、日期筛选、业务时间修正等公共能力。
- 本档案只描述现状，不把业务模块专属交互提升为公共抽象。

## 源码索引

| 文件                                         | 当前职责                                                           |
| -------------------------------------------- | ------------------------------------------------------------------ |
| `management-ui/package.json`                 | 前端依赖与 Vite、lint、format、build 脚本                          |
| `management-ui/src/main.js`                  | 创建应用，安装 Pinia/Router，注册 `permission` 与 Element Plus `loading` 指令，导入样式 |
| `management-ui/src/App.vue`                  | 根 `ElConfigProvider`、路由出口、合规页脚、全局请求遮罩            |
| `management-ui/src/style.css`                | Tailwind 主题、品牌兼容层、Element Plus 变量、响应式表格和全局样式 |
| `management-ui/src/utils/request.js`         | Axios 请求、鉴权响应处理和全局请求状态接入                         |
| `management-ui/src/stores/requestStatus.js`  | 并发查询/变更计数、180ms 延迟显示和遮罩文案                        |
| `management-ui/src/utils/access.js`          | 路由及菜单的功能开关、权限和平台租户判定                           |
| `management-ui/src/utils/permission.js`      | 通配、拒绝项和历史订单权限兼容                                     |
| `management-ui/src/directives/permission.js` | 无权限控件禁用、提示和点击阻断                                     |

## Element Plus 接入现状

- Foundation 已迁移：注册 `loading` 指令（`ElLoadingDirective`）。
- 稳定语义 token：`--ys-control-height`、`--ys-control-radius`、`--ys-focus-ring`。
- Element Plus 映射：`--el-component-size`、`--el-border-radius-base`。

- `package.json` 声明 `element-plus: ^2.13.0`，当前基线按 Element Plus 2.13 维护。
- `main.js` 导入 `element-plus/dist/index.css`，并调用 `installElementPlusFoundation(app)`；没有 `app.use(ElementPlus)`。
- 组件与服务由各文件显式从 `element-plus` 导入，当前不是全量 JS 插件安装模式。
- `App.vue` 显式导入 `ElConfigProvider`，并以 `zhCn` 设置中文 locale。
- `main.js` 通过 `installElementPlusFoundation(app)` 注册 `loading`，由 `ElLoadingDirective` 提供实现。
- `UserManual.vue` 已使用 `v-loading`；此前“模板使用而应用入口未注册”的缺口已解决。

## Token 与样式现状

当前可核验的业务主题 token 有三套来源：

1. `style.css` 的 Tailwind `@theme`：`--color-*` 与 `--font-sans`。
2. `body` 上的品牌变量：`--ys-primary`、`--ys-accent`、`--ys-surface`、`--ys-line` 等。
3. `body` 上供 `rgb(var(...))` 使用的数值变量：`--primary`、`--on-primary`、`--surface-*`、`--on-surface*`。

- 同一文件另以 `--el-*` 覆盖 Element Plus 主色、文本、边框、填充和圆角，这是组件库适配层。
- 页面和公共组件仍大量使用 Tailwind 色类、十六进制色、`rgb/rgba` 和局部 scoped CSS。
- 全局样式仅为 amber/yellow/orange 前景工具类补充可读色；警告背景、边框、ring 与渐变保留 Tailwind 语义，并继续对 Element Plus 输入、表格、弹层和消息做覆盖。
- 响应式表格在 900px 以下改为块级卡片，并依赖 `data-label` 展示表头。

## 公共组件职责

| 组件                              | 当前职责                                                      | 保留边界                           |
| --------------------------------- | ------------------------------------------------------------- | ---------------------------------- |
| `ResponsivePageFrame.vue`         | 观察容器宽度、生成视口 class、为原生响应式表格补 `data-label` | 不承担业务筛选和分页               |
| `TableColumnSettings.vue`         | 本地列顺序入口、当前页/全量导出事件、点击外部关闭             | 数据列状态由调用方 composable 管理 |
| `GlobalRequestOverlay.vue`        | 根据请求状态 store 以 Teleport 展示全局非阻塞遮罩             | 不替代页面局部加载态               |
| `DragAttachmentUpload.vue`        | 点击/键盘/拖放选择单文件，展示上传中和附件动作                | 不执行上传 API，也不校验文件内容   |
| `DateFilterInput.vue`             | 包装原生 `input[type=date]`，处理空值占位                     | 值格式仍为浏览器日期字符串         |
| `ComplianceFooter.vue`            | 展示版权、ICP备案、手册、隐私、条款和联系邮箱                 | 文案来自 `siteConfig`              |
| `BusinessTimeCorrectionPanel.vue` | 条件展示业务时间修正原生输入并透传值                          | 不决定谁有修正权限                 |

## API、权限与状态流

- 业务 API 统一经 `request.js`；默认请求会进入 `requestStatus` 并区分 query/mutation。
- `GlobalRequestOverlay` 在请求持续超过 180ms 后显示；并发计数归零时关闭。
- `v-permission` 接收一个或多个权限码，按“任一满足”判断；无权限时保留控件但禁用并拦截点击。
- 路由/菜单访问由 `access.js` 同时判断 feature、permission 和 `developerOnly`。
- `TableColumnSettings` 当前页导出调用 `/export/table`，导出错误通过 `ElMessage` 展示。

## 空态、错态与加载态

- 全局遮罩只表达正在查询或处理，不表达成功、空结果或失败。
- 请求失败消息由请求层和页面自身共同承担，存在重复提示的可能。
- `ResponsivePageFrame` 对空表格不造数据，只处理现有 DOM 的响应式标签。
- 公共附件组件在未选择文件时不发事件，上传失败后的提示和恢复由调用方负责。

## Element Plus 接入/替换建议

- 保持显式导入策略，继续通过 `installElementPlusFoundation(app)` 注册实际使用的 Loading 指令；不要为此改为全量 `app.use(ElementPlus)`。
- 将三套业务 token 收敛到一个语义源，再分别映射 Tailwind、RGB 兼容变量和 `--el-*`。
- `DateFilterInput` 可在确认 API 日期格式后迁移到 `ElDatePicker`，必须显式设置 `value-format`。
- `TableColumnSettings` 的弹出层可迁移为 `ElPopover`，按钮可迁移为 `ElButton`；导出逻辑保持不变。
- `DragAttachmentUpload` 可评估 `ElUpload` 的 drag 模式，但文件选择事件、大小限制和业务上传仍由调用方控制。
- 全局请求遮罩与页面局部 `v-loading` 应分工，不能用一个全屏状态覆盖所有局部空错态。

## 风险

- 历史风险（已解决）：`v-loading` 未注册曾可能导致指令不能按预期工作并产生运行期指令解析警告；当前已由 `ElLoadingDirective` 注册覆盖。
- 三套 token、`--el-*` 和硬编码颜色并存，修改品牌色时容易出现局部漂移。
- 全局 `!important` 和旧色类重映射可能改变新组件的状态色语义。
- 权限指令是前端体验层，不能替代后端 `@RequirePermission`。
- 响应式表格依赖原生 table DOM；迁移到 `ElTable` 后不能继续假设同一 DOM 结构。

## 验证清单

- [ ] `npm run build` 无未解析指令或样式导入错误。
- [ ] 中文分页、日期、空态等 Element Plus 文案由根 ConfigProvider 生效。
- [ ] 同时发起查询和变更请求时，全局遮罩计数和文案正确收敛。
- [ ] 无权限按钮保持禁用、提示明确，后端仍拒绝直接请求。
- [ ] 320px、768px、1024px 和宽屏下公共容器及响应式表格无横向溢出。
- [ ] 导出、拖拽上传、键盘选择文件和日期清空行为保持现有契约。
