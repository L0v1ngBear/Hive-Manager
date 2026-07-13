# 库存单匹布详情

## 源码、路由与迁移状态

- 页面：`management-ui/src/views/function/inventory/InventoryModelDetail.vue`。
- API：`management-ui/src/views/function/inventory/api/inventory.js`。
- 路由：`/function/inventory/model-detail`，路由名 `InventoryModelDetail`，功能开关 `module.inventory`。
- 查询参数：`modelCode`、`spec`、可选 `status`、`timeOrder`；原字符串与数字转换规则保持不变。
- 迁移批次：Batch 3；状态为“Element Plus 已迁移，动态响应式表格为受保护自定义界面”。

## 功能与接口

- 按型号、规格、状态和 FIFO/LIFO 查询单匹布列表，展示布匹数与剩余米数汇总。
- `getInventoryModelDetail` 继续使用 `modelCode/spec/status/timeOrder`；`status` 仅在路由参数存在且非空时转数字。
- `getInventoryClothDetail` 继续按 `id` 与 `barcode` 读取单匹布及流水。
- `outCloth` 继续提交 `{ barcode, meters: Number(meters) }`；租户动态字段继续由 `getCurrentTenantFieldConfig` 提供。

## 权限

- 列表和单匹详情读取依赖 `inventory:warning:list`，出库依赖 `inventory:cloth:out`。
- 无读取权限时内容不可见；无出库权限时命令保持可见但禁用并显示原因 tooltip，打开与提交处理函数均再次校验权限。
- 仅有出库权限的账号可能通过路由但不能读取列表，这是现有权限模型风险。

## 状态与条码一致性

- 列表请求前清空旧行，loading、本地无权限、401/403/网络/5xx 持久错误、成功真空或数据互斥展示，并支持重试与 latest-request 提交。
- 每次打开单匹详情先清空旧详情和旧错误，再显示 loading；401、403、网络与服务失败使用持久错误面板，成功空响应显示真实空态。
- 单匹详情使用递增 request-id；跨布匹的旧响应和旧 `finally` 都不能覆盖当前详情。
- 从行打开出库抽屉时记录预览对应条码。用户修改条码会立即清除旧预览；只有预览条码与实际提交条码完全一致时才允许提交，后端仍负责剩余量和状态最终校验。

## Element Plus 与受保护界面

- 排序选择、命令、抽屉、表单、文字/数字输入、状态标签、tooltip、loading 和 empty 使用显式导入的 Element Plus 组件。
- 单匹布列表保留原生 `table`，保护租户动态列、`TableColumnSettings` 顺序持久化、移动端 `data-label`、行点击和按钮事件隔离。
- 流水时间线保留定制 DOM，不强制改造成普通表格。

## 风险与验证

- 单个型号布匹数很大时没有分页，动态字段渲染成本随行数线性增长。
- 预览不是授权或库存锁；并发出库、超量出库与状态变化仍以服务端校验为准。
- 验证缺少型号、所有路由参数组合、FIFO/LIFO、动态列、窄屏、详情快速切换、失败重试、条码改写、权限禁用以及部分/全部出库。
