# 库存管理

## 源码、路由与迁移状态

- 页面：`management-ui/src/views/function/inventory/inventory.vue`。
- API：`management-ui/src/views/function/inventory/api/inventory.js`。
- 路由：`/function/inventory`，路由名 `Inventory`，功能开关 `module.inventory`。
- 共享组件：`DateFilterInput`、`BusinessTimeCorrectionPanel`、`DragAttachmentUpload`、`TableColumnSettings`。
- 迁移批次：Batch 3；状态为“Element Plus 已迁移，动态响应式表格为受保护自定义界面”。

## 功能与接口

- 展示库存汇总、型号聚合分页、低库存预警、最近流水和七天趋势。
- 支持关键字、状态、规格、剩余米数、更新时间和 FIFO/LIFO 筛选。
- 支持预警阈值设置、手工入库、扫码出库、外部库存导入和图片识别候选人工确认入库。
- `getInventoryModelPage`、`getInventorySummary`、`getInventoryWarnings`、`getInventoryWarningSetting`、`updateInventoryWarningSetting`、`getRecentInventoryRecords`、`getInventoryTrend` 的路径、方法和响应契约保持不变。
- `searchInventoryModels`、`searchInventoryBarcode`、`inCloth`、`outCloth`、`downloadInventoryImportTemplate`、`importInventory`、`recognizeInventoryImage` 和 `getCurrentTenantFieldConfig` 保持原包装器与 payload。
- 状态筛选在控件中仍是字符串，发请求时仅非空值转为数字；规格、米数和阈值提交仍显式转为数字。
- 入库业务时间继续补秒并校验不得晚于当前时间；图片上传仍限制 PNG/JPG/JPEG/WEBP、5MB，识别结果只生成候选，不自动入库。

## 权限

- 读取聚合列表、汇总、预警及型号/条码搜索依赖 `inventory:warning:list`。
- 最近流水依赖 `inventory:record:recent`，趋势依赖 `inventory:trend`。
- 预警设置依赖 `inventory:warning:setting`；导入说明、批量导入、图片识别和手工入库依赖 `inventory:cloth:in`；出库依赖 `inventory:cloth:out`。
- 命令无权限时保持可见、禁用鼠标并通过 tooltip 说明权限码；处理函数再次校验权限，不会绕过界面直接发请求。
- 抽屉关闭、候选增删/选择等标准命令也使用 `ElButton`；入库、识别确认入库和出库提交按钮分别按 `inventory:cloth:in`、`inventory:cloth:out` 禁用并说明原因。
- 无 `inventory:warning:list` 时库存内容不可见。路由允许仅有入库或出库权限的用户进入，这是现有路由与读取权限不一致风险，不在本次迁移中发明新权限码。

## 状态与错误处理

- 型号列表请求开始时清空旧行，loading、本地无权限、持久错误、成功真空或数据互斥展示。
- 401、403、网络错误和 5xx 分别转换为可重试的持久错误；递增 request-id 只允许最后一次列表请求提交数据或结束 loading。
- 筛选与分页只刷新型号聚合列表；成功入库、出库或导入后按原流程刷新相关区域。
- 图片识别候选保留逐条草稿、增删、人工核对标记和串行调用普通入库接口的流程。中途失败仍可能形成部分成功，这是现有真实风险。
- 扫码出库预览记录发起请求时的条码并采用 request-id；旧成功/失败不能覆盖新条码，条码变化会清空预览，只有预览条码与提交条码完全一致时才允许出库。

## Element Plus 与受保护界面

- 标准命令、搜索、选择、数字输入、复选框、分页、抽屉、表单、标签、tooltip、loading 和 empty 使用显式导入的 Element Plus 组件。
- 手工入库与图片识别候选编辑器的型号、条码、自定义字段改用 `ElInput`，规格和米数改用 `ElInputNumber`，人工核对改用 `ElCheckbox`；`data-field`、动态输入类型、输入事件以及提交时的显式数值转换保持不变。
- 日期筛选、业务时间修正、附件上传和列设置继续使用项目共享组件。
- 型号聚合表保留原生 `table`：它依赖租户动态列顺序、移动端 `data-label`、行点击与操作 `.stop`，并与 `TableColumnSettings` 的导出契约绑定；强制换成 `ElTable` 会破坏这些行为。
- 条码展示和导入文件 input 保留业务所需的定制 DOM；文件 input 是唯一保留的原生输入控件。

## 风险与验证

- `refreshAll` 涉及不同读取权限，单区失败的降级边界仍需浏览器权限矩阵验证。
- 图片候选串行入库没有事务回滚；失败候选的续传与幂等仍是后续专项。
- 扫码输入节奏可能导致预览与输入变化，提交前仍由后端校验条码和剩余米数。
- 验证动态列、导出、移动端标签、筛选类型、业务时间、附件、候选草稿、入出库 payload、权限禁用及四态互斥。
