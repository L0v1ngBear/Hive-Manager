# 库存管理

## 源码 / 路由 / 批次

- 页面：`management-ui/src/views/function/inventory/inventory.vue`
- API：`management-ui/src/views/function/inventory/api/inventory.js`
- 共享依赖：`DateFilterInput.vue`、`BusinessTimeCorrectionPanel.vue`、`DragAttachmentUpload.vue`、`TableColumnSettings.vue`、租户字段配置。
- 路由：`/function/inventory`，路由名 `Inventory`，功能开关 `module.inventory`。
- 迁移批次：Batch 3；状态为 `Audit baseline`。

## 用户功能

- 查看库存总米数、在库布匹数、低库存数、今日入库与今日出库。
- 按关键字、状态、规格、剩余米数、更新时间及先进先出/先进后出筛选型号聚合列表。
- 查看 7 天趋势、低库存预警、最近库存流水；调整预警阈值。
- 打开型号汇总详情并跳转单匹布明细页。
- 手工入库、型号联想、扫码/输入条码出库。
- 下载导入说明、批量导入外部库存并查看成功/失败/标签任务统计。
- 上传图片识别候选布匹，逐条人工核对后按候选逐笔入库。
- 读取租户库存字段配置，支持自定义字段、动态列顺序和本地列配置。

## API

| 函数                              | 方法 | 路径                                 | 用途                      |
| --------------------------------- | ---- | ------------------------------------ | ------------------------- |
| `getInventorySummary`             | GET  | `/inventory/summary`                 | 汇总指标                  |
| `getInventoryModelPage`           | GET  | `/inventory/model/page`              | 型号聚合分页与筛选        |
| `getInventoryWarnings`            | GET  | `/inventory/warning/list`            | 低库存预警                |
| `getInventoryWarningSetting`      | GET  | `/inventory/warning/setting`         | 当前预警阈值              |
| `updateInventoryWarningSetting`   | POST | `/inventory/warning/setting`         | 保存预警阈值              |
| `getRecentInventoryRecords`       | GET  | `/inventory/record/recent`           | 最近库存流水              |
| `getInventoryTrend`               | GET  | `/inventory/trend`                   | 7 天出入库趋势            |
| `searchInventoryModels`           | GET  | `/inventory/model/search`            | 入库型号联想              |
| `searchInventoryBarcode`          | GET  | `/inventory/barCode/search`          | 出库前按条码取库存        |
| `inCloth`                         | POST | `/inventory/cloth/in`                | 手工或识别候选入库        |
| `outCloth`                        | POST | `/inventory/cloth/out`               | 按条码与米数出库          |
| `downloadInventoryImportTemplate` | GET  | `/inventory/import-template`         | 下载外部库存导入说明      |
| `importInventory`                 | POST | `/inventory/import`                  | 上传表格批量导入          |
| `recognizeInventoryImage`         | POST | `/inventory/cloth/image-recognition` | 上传图片并返回识别候选    |
| `getCurrentTenantFieldConfig`     | GET  | `/tenant/field-config`               | 读取 `inventory` 字段配置 |

## 权限

- 路由入口接受 `inventory:warning:list`、`inventory:record:recent`、`inventory:cloth:in`、`inventory:cloth:out` 任一权限，并受 `module.inventory` 控制。
- 页面按钮：预警设置 `inventory:warning:setting`；导入说明、批量导入、图片识别、手工入库均为 `inventory:cloth:in`；出库为 `inventory:cloth:out`。
- 后端读取汇总、聚合列表、预警、型号与条码搜索使用 `inventory:warning:list`；流水、趋势分别要求 `inventory:record:recent`、`inventory:trend`。
- 风险：路由允许只有入库或出库权限的用户进入，但页面初始化会并发请求读取、流水和趋势接口；缺少其中权限时不能保证整页完整加载。

## 状态流

- 初始化并发读取字段配置、聚合列表、汇总、预警、流水和趋势；筛选与分页只刷新聚合列表。
- 聚合行点击后打开汇总抽屉，再携带 `modelCode/spec/status/timeOrder` 跳转单匹明细。
- 手工入库：校验型号、规格、米数、自定义字段和可选业务时间，提交后刷新全部数据。
- 图片识别：选择图片 → 校验类型/5MB → 识别候选 → 人工逐条确认 → 串行调用入库接口 → 刷新全部数据。
- 出库：输入条码触发查询预览 → 输入米数 → 提交 → 关闭抽屉并刷新全部数据。
- 库存状态展示：`0` 在库，`2` 部分出库，`1` 已出库；流水展示入库/出库语义。

## 加载空错态

- 聚合表有遮罩式加载态；无数据时显示“暂无符合条件的库存记录”。
- 趋势、预警、流水各自有空态；字段配置失败回退默认配置。
- 图片识别有上传中状态、前端格式/大小提示和接口失败消息；识别结果必须人工确认。
- 聚合列表请求用 `finally` 结束加载，但未提供页内错误态，失败时可能保留旧行；其余初始化请求也没有独立错误区或重试按钮。
- 导入结果使用消息框展示统计；下载会检查空文件、文本错误响应和 XLSX ZIP 文件头。

## UI 控件现状

- 原生 `button/input/select/table/aside` 配合 Tailwind/scoped CSS；手写遮罩、抽屉、分页、状态徽标和空态。
- 已使用 `ElMessage`、`ElMessageBox.prompt/alert`；日期、业务时间、附件上传、列设置为项目自定义组件。
- 表格行承担跳转，操作按钮使用 `.stop` 隔离；列顺序保存在本地。

## Element Plus 替换与保留项

- 替换：命令按钮→`ElButton`，搜索→`ElInput`，数字范围→`ElInputNumber`，状态/顺序→`ElSelect`，分页→`ElPagination`。
- 替换：标准抽屉→`ElDrawer`，聚合表→验证动态列和响应式标签后的 `ElTable`，状态→`ElTag`，空态/加载→`ElEmpty` 与 `v-loading`。
- 保留：`BusinessTimeCorrectionPanel`、`DragAttachmentUpload`、`DateFilterInput`、租户动态字段、列设置与导出契约。
- 保留：图片识别候选编辑与逐条人工确认流程；迁移不得把“识别”改成自动入库。

## 风险

- 图片识别接口只生成候选；前端随后对每条候选串行调用普通入库。中途失败会形成部分成功，当前没有事务、回滚或失败候选续传摘要。
- 识别候选的 `customFields` 初始化为空，不会自动承接识别响应中的同名自定义字段；需以当前代码行为为准。
- 主页面出库预览只在条码 `change` 时更新；提交前应验证条码、预览与米数仍对应，避免扫描器输入节奏造成旧预览。
- `refreshAll` 的并发请求权限不一致，单个失败可使刷新 Promise 拒绝；页面没有分区降级状态。
- 状态筛选在控件中是字符串，发请求时转数字；组件替换不得改变类型。
- 业务时间通过本地 `Date` 与 `Date.now()` 校验并补秒，迁移日期控件时不得改变请求格式或未来时间规则。

## 验证清单

- [ ] 四类路由权限与 `module.inventory` 开关分别验证。
- [ ] 筛选、重置、分页、动态列、行点击和操作点击隔离保持一致。
- [ ] 手工入库、自定义必填字段、业务时间修正及标签任务提示正确。
- [ ] 图片格式/大小、候选增删、逐条人工确认、部分失败场景可观察。
- [ ] 扫码查询、部分出库、全量出库、超剩余米数由后端拒绝并正确提示。
- [ ] 导入说明下载与表格导入结果、失败明细、标签任务数量正确。
- [ ] 加载、真实空数据、接口错误和旧数据不混淆。
- [ ] 桌面与窄屏表格、抽屉、长型号和动态字段无溢出。
