# 使用手册维护档案

> 状态：Element Plus migrated with protected custom surface；迁移批次：Batch 3；保护面：结构化内容模型、文章卡片与 Markdown 导出；路由：`/manual`；feature：`module.manual`。

## 功能

- 展示顶部说明、快捷指南、目录、业务章节、步骤明细、提示和常见问题。
- 章节可配置业务路由，并通过按钮进入对应模块。
- 编辑顶部说明、快捷卡、章节、自定义窗口、FAQ 窗口和 FAQ 条目。
- 维护企业内部补充手册，支持推荐模板、清空、保存和 Markdown 导出。
- 同一后端 content 字段兼容结构化 JSON 与历史纯文本。

## 源码索引

| 层级          | 文件                                                                                    |
| ------------- | --------------------------------------------------------------------------------------- |
| 页面/默认内容 | `management-ui/src/views/manual/UserManual.vue`                                         |
| 前端 API      | `management-ui/src/views/manual/api/manual.js`                                          |
| 路由          | `management-ui/src/router/index.js`                                                     |
| 控制器        | `management/src/main/java/my/management/controller/TenantManualController.java`         |
| 服务          | `management/src/main/java/my/management/module/manual/service/TenantManualService.java` |
| DTO/VO        | `TenantManualSaveRequest.java`、`TenantManualVO.java`                                   |

## API

| 方法 | 接口             | 请求/响应                              | 权限                |
| ---- | ---------------- | -------------------------------------- | ------------------- |
| GET  | `/manual/custom` | 返回 `content`、`savedAt`、`updaterId` | 无方法级 permission |
| POST | `/manual/custom` | `{ content }`，返回保存后 VO           | `document:rename`   |

- 控制器整体受 `CODE_MANUAL` 租户功能开关保护。
- GET 显式设置 `showGlobalLoading: false`，页面自行维护 `customManualLoading`。
- DTO 将 content 上限设为 120000 字；页面 textarea 同样设置 `maxlength=120000`。
- 服务按当前租户 upsert，并记录当前用户为 updater。

## 内容模型与状态流

- 前端固定类型为 `hive-full-manual`，当前结构版本为 `1`。
- 结构包含 hero、quickGuides、sections、faqWindow、faqs、customManual。
- 加载空内容时使用代码内默认完整手册。
- content 不是合法 JSON 时，原文本被放入 `customManual.content`，其余使用默认结构。
- content 是可识别对象时逐区 normalize；缺失值回退默认内容。
- 页面挂载调用 `loadCustomManual()`，成功后同步 config、draft、savedAt。
- 弹窗保存先在本地 `applyManualEditor`，再将完整结构序列化为 JSON 发给后端。
- 自定义正文保存也发送完整结构，而不是只发送 textarea 文本。
- 导出在浏览器生成完整 Markdown Blob，不调用后端。

## 权限现状

- 路由只要求 `module.manual`，没有 route-level permission。
- 页面按后端真实 `document:rename` 权限计算编辑能力；编辑、新增、保存、模板覆盖和清空命令保持可见但置灰，并显示中文原因，处理函数也会二次阻止请求。
- 后端 POST 复用了 `document:rename` 权限，接口错误文案为无权编辑企业使用手册。
- 只具备浏览能力的用户仍可阅读和导出，但无法打开编辑器或提交变更。
- 可编辑章节 route 字符串，但最终跳转仍经过全局路由守卫。

## 空态、错态与加载态

- GET 期间对自定义编辑区使用 `v-loading="customManualLoading"`。
- 应用入口只全局注册 `ElLoadingDirective`，本页加载遮罩可用。
- GET 失败显示 `ElMessage.warning`，页面继续保留代码内默认手册。
- 保存失败显示统一 `ElMessage.error`，不展示后端具体错误文案。
- FAQ、章节和快捷指南 normalize 时，空数组会回退默认数组，不会渲染空容器。
- 自定义正文为空时仍可保存；导出只在最终 Markdown 为空时阻止。

## 控件和样式现状

- 页面约两千行，默认内容、编辑逻辑、序列化、导出和 scoped CSS 位于同一 SFC。
- 命令使用显式导入的 `ElButton`，自定义正文和动态字段使用 `ElInput`。
- 编辑弹层使用 `ElDialog`，覆盖与清空确认使用 `ElMessageBox`；取消确认不作为错误提示。
- 空 FAQ 使用 `ElEmpty`，消息使用 `ElMessage`，加载模板使用 `v-loading`。
- FAQ 的 `details/summary`、文章 hero、卡片、目录和章节布局仍为受保护的自定义内容表面。
- 页面使用自定义 hero、卡片、左侧目录、章节、FAQ 和编辑弹层样式。
- 断点和大量硬编码颜色/圆角由本文件 scoped CSS 维护。

## Element Plus 迁移结果

- 标准命令、输入、编辑弹层、确认框和空态已迁移为 `ElButton`、`ElInput`、`ElDialog`、`ElMessageBox`、`ElEmpty`。
- 所有 Element Plus 组件均在页面显式导入；应用仍只全局注册 Loading 指令。
- FAQ 保留原生 `details/summary`，以维持浏览器展开语义和现有文章布局。
- 自定义正文继续保留 120000 字上限、纯文本内容和完整结构 JSON 保存。
- JSON 兼容、默认手册、章节导航与 Markdown 输出格式未改变。

## 风险

- 手册编辑权限复用文档重命名权限，领域语义耦合明显，但视觉迁移不得自行改权限码。
- 弹窗保存先修改本地 config；请求失败没有回滚，页面显示内容可能与服务端不一致。
- 清空正文先清 draft 再保存；保存失败时当前页面仍为空而服务端仍保留旧值。
- 两个并列格式长期共存：纯文本可读，但下一次保存会转为完整结构化 JSON。
- 可编辑 route、图标名和长文本缺少专门格式校验，错误配置可能造成坏入口或布局膨胀。

## 验证清单

- [ ] 空库、历史纯文本、合法结构 JSON、缺字段 JSON 均能稳定加载。
- [ ] 120000 字边界、超限后端校验和错误提示可复现。
- [ ] 有/无 `document:rename` 时编辑控件和保存结果分别核验。
- [ ] 加载、保存成功、保存失败、清空失败后页面与服务端状态一致性明确。
- [ ] Markdown 导出包含 hero、快捷指南、章节、明细、自定义正文和 FAQ。
- [ ] 弹层支持关闭、遮罩点击、长字段滚动和窄屏输入。
- [ ] 章节 route 点击仍经过 feature/permission 路由守卫。
