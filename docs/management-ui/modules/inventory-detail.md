# 库存单匹布明细

## 源码 / 路由 / 批次

- 页面：`management-ui/src/views/function/inventory/InventoryModelDetail.vue`
- API：`management-ui/src/views/function/inventory/api/inventory.js`
- 后端入口：`management/src/main/java/my/management/controller/InventoryController.java`
- 路由：`/function/inventory/model-detail`，路由名 `InventoryModelDetail`，功能开关 `module.inventory`。
- 查询参数：`modelCode`、`spec`、可选 `status`、`timeOrder`。
- 迁移批次：Batch 3；状态为 `Audit baseline`。

## 用户功能

- 按型号、规格和状态查看单匹布列表，并切换先进先出/先进后出排序。
- 查看布匹数和当前列表剩余米数汇总。
- 查看条码、总米数、剩余米数、状态、入库/更新时间及租户自定义字段。
- 打开单匹详情，查看入库方式、出库时间和该匹布的出入库流水。
- 从选中行发起单匹布出库，默认带入条码和全部剩余米数。
- 返回库存聚合页、手动刷新、调整本地表格列顺序。

## API

| 函数                          | 方法 | 路径                      | 用途                                  |
| ----------------------------- | ---- | ------------------------- | ------------------------------------- |
| `getInventoryModelDetail`     | GET  | `/inventory/model/detail` | 按型号/规格/状态/排序取单匹列表       |
| `getInventoryClothDetail`     | GET  | `/inventory/cloth/detail` | 按 `id` 或 `barcode` 取单匹详情与流水 |
| `outCloth`                    | POST | `/inventory/cloth/out`    | 按条码和米数执行出库                  |
| `getCurrentTenantFieldConfig` | GET  | `/tenant/field-config`    | 读取库存字段配置                      |

## 权限

- 路由入口接受 `inventory:warning:list`、`inventory:record:recent`、`inventory:cloth:out` 任一权限，并受 `module.inventory` 控制。
- 列表、单匹详情后端均要求 `inventory:warning:list`；行内出库按钮要求 `inventory:cloth:out`，出库接口同权。
- 风险：只有出库权限的用户可通过路由守卫，但列表接口仍会拒绝；只有读取权限的用户可看列表，出库按钮按指令置为无权状态。

## 状态流

- 从路由解析型号、规格、状态与排序；缺少型号时提示并停止请求。
- 加载型号明细 → 计算当前返回行数与剩余米数 → 行点击加载单匹详情和流水。
- 排序切换或刷新重新读取整个明细列表，没有前端分页。
- 出库：选择一行 → 预填该行条码和剩余米数 → 用户可编辑 → 提交出库 → 关闭抽屉并刷新列表。
- 状态：`0` 在库、`2` 部分出库、`1` 已出库；剩余米数小于等于 0 时禁用行内出库。

## 加载空错态

- 列表与单匹详情分别有遮罩/居中加载态；列表为空显示“暂无单匹布明细”，流水为空显示独立空态。
- 字段配置失败回退默认字段。
- 列表和详情请求只用 `finally` 结束加载，没有页内错误说明、重试态或失败时清空旧详情的处理。
- 单匹详情打开后立即显示抽屉；若请求失败，旧 `clothDetail` 可能继续被计算属性读取。

## UI 控件现状

- 原生选择器、按钮、表格、手写抽屉和遮罩；`ElMessage` 负责提示。
- `TableColumnSettings` 与 `useLocalTableColumns` 提供动态列；响应式表格依赖现有 class/data-label 契约。
- 行点击看详情，详情/出库按钮使用 `.stop`；抽屉覆盖层点击直接关闭。

## Element Plus 替换与保留项

- 替换：排序选择→`ElSelect`，刷新/返回/操作→`ElButton`，表格→验证动态列后的 `ElTable`，抽屉→`ElDrawer`。
- 替换：米数→`ElInputNumber`，状态→`ElTag`，加载与空态→`v-loading`/`ElEmpty`。
- 保留：路由查询参数类型、FIFO/LIFO 值、动态租户字段、列顺序持久化、行点击与按钮事件隔离。
- 保留：单匹详情中的流水时间线可继续定制，不必为 Element Plus 统一而改成普通表格。

## 风险

- 单匹出库抽屉允许编辑预填条码，但编辑后不会重新查询或更新 `outPreview`；界面可能展示原布匹信息、实际提交另一条码。
- 提交只检查条码非空和米数大于 0；剩余量、状态和条码对应关系依赖后端最终校验，前端预览不是授权或一致性保证。
- 行按钮文案为“扫码出库”，但从行发起时实际是预填条码；组件迁移不得误加自动提交或绕过确认。
- 无分页意味着单个型号匹数较大时表格和自定义字段渲染成本会线性增长。
- `spec` 从路由原样发送，`status` 转数字；新控件/路由同步不得把空状态变成 `0`。

## 验证清单

- [ ] 缺少型号、有效型号、不同规格和状态查询均正确。
- [ ] FIFO/LIFO 顺序与后端结果一致，刷新不丢查询上下文。
- [ ] 动态字段增删、列排序和窄屏响应式标签正常。
- [ ] 行点击与两个操作按钮互不串事件。
- [ ] 单匹详情与流水加载、空态、错误后旧数据处理可辨识。
- [ ] 改写出库条码时必须重新核对目标，部分/全量/超量出库均验证。
- [ ] 无 `inventory:warning:list` 或无 `inventory:cloth:out` 的访问行为符合权限契约。
