# 订单管理维护档案

> 当前状态：Element Plus migrated with protected custom surface；Batch 3。动态响应式表格、联想选项及订单状态流是受保护业务面。

## 源码 / 路由 / 改造批次

- 页面：management-ui/src/views/function/order/order.vue。
- 前端 API：management-ui/src/views/function/order/api/order.js。
- 直接依赖：TableColumnSettings、DateFilterInput、BusinessTimeCorrectionPanel、DragAttachmentUpload。
- 状态与权限依赖：useUserStore、useLocalTableColumns、useTimeCorrectionMode。
- 路由：/function/order，名称 Order，标题“订单列表”。
- 路由门槛：permissions = [order:list]，features = [module.order]。
- 后端入口：OrderController，类级 feature 为 CODE_ORDER。
- 改造批次：Batch 3（复杂流程模块）。
- 改造边界：只迁移标准控件，不改 API、状态权限、审批语义、动态列和导出绑定。

## 用户可见功能

- 查看订单总量、主要状态、开票状态、未更新预警及订单小项统计卡。
- 按状态、小项、开票、关键字、客户、品牌、创建时间、交付时间和预警筛选。
- 开票状态分为未开票、已开票、其他类型；未开票订单从创建时间起满 7 个完整自然日后标红预警，已开票和其他类型统一使用灰色已处理样式。
- 查看分页订单表；点击行或“查看”图标打开订单详情。
- 查看订单明细、附件、多条物流、开票、流转码和状态流转日志。
- 新建订单；客户与项目可从客户选项联动，也允许录入新值。
- 编辑订单主体、明细、状态、物流子表、开票、业务时间和附件。
- 订单备注使用独立多记录：可新增、可修改，不允许删除；每条显示最后修改人和最后修改时间。
- 按当前订单阶段权限执行推进、回退审批和流转码补打。
- 配置分订单小项的未更新预警天数，并支持全量或单条重新计算。
- 调整表格列顺序，恢复默认，并导出当前页或当前筛选的全部数据。
- 当前页与全部页导出都走 `formatOrderExportCell` 程序化格式化；多条物流单号固定使用 `、` 拼接，不读取表格 DOM 文本。
- 业务时间修正模式下可修正订单创建时间及状态日志时间。

## 前端 API

| 封装函数                   | HTTP | 路径                             | 用途                         |
| -------------------------- | ---- | -------------------------------- | ---------------------------- |
| getOrderPage               | GET  | /orders                           | 分页查询与全量导出取数       |
| getOrderStatusSummary      | GET  | /orders/status-summary            | 状态、小项和开票统计         |
| getOrderDetail             | GET  | /orders/{orderId}                 | 详情、编辑回填、备注与日志刷新 |
| getOrderOperationLogs      | GET  | /orders/{orderId}/operation-logs  | 分页读取订单操作日志           |
| getOrderLogisticsTracking  | GET  | /orders/{orderId}/shipments/{shipmentId}/logistics-tracking | 查询指定物流记录的轨迹 |
| createOrder                | POST | /orders                           | 新建订单并保存备注           |
| uploadOrderAttachment      | POST | /orders/attachment                | 上传订单附件，30 秒超时      |
| downloadOrderAttachment    | GET  | /orders/attachment                | 下载附件 Blob，30 秒超时     |
| saveOrder                  | PUT  | /orders/{orderId}                 | 保存订单及新增/修改备注      |
| advanceOrderNextStage      | POST | /orders/{orderId}/advance         | 推进到下一阶段或提交关键审批 |
| submitOrderRollback        | POST | /orders/{orderId}/rollback        | 提交回退审批                 |
| createOrderFlowPrintTask   | POST | /orders/flow-print-task           | 创建流转码补打任务           |
| correctOrderLogTime        | POST | /orders/status-log/{logId}/time   | 修正状态日志时间             |
| getOrderWarningSetting     | GET  | /orders/warning/setting           | 读取预警阈值                 |
| updateOrderWarningSetting  | POST | /orders/warning/setting           | 保存预警阈值                 |
| getOrderWarningSummary     | GET  | /orders/warning/summary           | 读取预警统计                 |
| refreshOrderWarningSummary | POST | /orders/warning/refresh           | 刷新预警摘要                 |
| refreshOrderWarnings       | POST | /orders/warning/refresh-all       | 重算全部订单预警             |
| refreshOrderWarning        | POST | /orders/warning/{orderId}/refresh | 重算单条订单预警             |

## 多物流子表与轨迹合同

- 订单物流使用 `shipments` 子表合同，保存项包含 `id`、`logisticsCompany`、`trackingNo` 和 `version`；列表、详情、编辑回填与导出都读取 shipment 列表，不再读写订单级物流公司或物流单号。
- 新增行在首次保存前可以放弃；服务端已经保存并取得 `id` 的行不可删除，只能修改。保存请求必须回传全部已保存行，遗漏任一行会被后端拒绝为“已保存的物流记录不允许删除”。
- 已保存行以 `version` 做乐观锁校验。版本缺失、过期或并发更新失败返回 409，前端不得覆盖他人修改，应重新加载详情后再编辑。
- 保存为 `shipped` 或从 `pending_ship` 推进到 `shipped` 前，至少要有一条物流公司和物流单号均完整的已保存记录；推进服务以数据库中的 shipment 为准。
- 编辑后推进必须先调用 `PUT /orders/{orderId}` 保存完整订单和 shipment 子表，保存成功后才调用 `POST /orders/{orderId}/advance`；保存失败时不得继续推进。
- 单条轨迹只使用 shipment-specific 路径 `GET /orders/{orderId}/shipments/{shipmentId}/logistics-tracking`，响应单号字段为 `trackingNo`，不保留订单级或旧字段兼容合同。
- 物流供应商查询仅在用户 hover 打开对应物流单号 popover 时触发；列表加载、详情加载和普通鼠标移动不得预查询供应商。
- 物流 hover 查询要求 `order:detail` 与当前订单阶段查看权限；list-only 用户仅看到置灰单号，不创建 popover，也不调用 tracking API。
- 成功轨迹缓存 30 分钟，供应商失败使用 30 秒短缓存抑制重复请求。缓存身份包含 tenant、order、shipmentId、当前物流公司和当前物流单号；更换公司、单号或 shipment 后必须形成新的缓存身份。
- 新增和更新 shipment 在事务提交后分别记录 `add_order_shipment`、`update_order_shipment` 操作动作；日志只保存 shipment 标识和脱敏后的单号指纹，不记录明文物流单号。
- 多物流上线采用 `V20260717_001` clean-launch destructive contract：部署前必须清空旧业务数据，不回填、不保留、不兼容读取 `sales_order.express_company` / `sales_order.express_no`。

## 权限与 feature

- 页面和侧栏以 module.order 与 order:list 控制入口；路由权限数组按“任一满足”判断。
- 新建按钮要求 order:\* 或 order:create。
- 编辑和保存要求 `order:update`，推进、回退与订单阶段动作还要求对应的精确状态操作权限。
- 备注内容按 `order:note:view` 控制可见；新增要求 `order:note:create`，修改要求 `order:note:update`，并同时要求 `order:update`。无查看权限时前端不渲染内容，后端也不返回备注。
- 下划线状态会转为连字符权限码，例如 pending_confirm 对应 order:status:pending-confirm。
- 预警设置按钮使用 v-permission=order:warning:setting。
- 后端详情要求 order:detail；附件上传要求 order:create；附件下载要求 order:detail。
- 后端保存、推进、回退、日志修正和流转码任务入口要求 order:list，服务层继续校验阶段权限。
- 预警阈值读写要求 order:warning:setting；预警摘要与刷新要求 order:list。
- 不得在控件迁移中弱化禁用态、提示文案、按钮 stop 传播或服务端二次校验。

## 关键状态 / 数据流

- 初次挂载并行加载列表、统计/预警和客户选项；路由 keyword 或 q 变化会重载列表。
- 列表请求参数由 filters 与 page/size 组成；查询和卡片切换会回到第 1 页。
- `isInvoice` 只接受 `0`（未开票）、`1`（已开票）、`2`（其他类型）。列表返回 `invoiceWarning`、`invoiceAgeDays`、`invoiceWarningDays`；固定 7 天开票预警仅适用于未开票且非取消审核中/已取消的订单，并与 `staleWarning` 独立计算。
- 状态统计同时返回 `invoice_unpaid`、`invoice_paid`、`invoice_other` 和 `invoice_warning`，其中 `invoice_warning` 是满足固定 7 天规则的未开票订单数量。
- 默认普通流程：pending_confirm → pending_pay → pending_material → producing → pending_ship → shipped → completed。
- 图纸预算流程：budgeting → budget_completed。
- 创建或保存 `pending_pay` 不创建审批。只有用户执行 `pending_pay -> pending_material` 推进时才创建备料审批候选；审批通过后进入备料中，拒绝后保持待收款。
- 回退动作提交审批；特殊订单创建固定从 pending_confirm 进入审核语义。
- 推进到 shipped 前要求至少一条完整的 shipment；管理端按“先保存、再推进”的顺序执行，后端对 `/advance` 请求启用嵌套 DTO 校验并以已持久化 shipment 做状态校验。
- 动态列默认 8 列：编号、客户/项目、订单信息、信息渠道、物流单号列表、状态、进度、时间。
- 列顺序以 hive.table.columns.order.list.commercial.v5 存在 localStorage；当前实现只排序，不隐藏列。
- 当前页导出通过 `exportCell(row, column)` 回调读取结构化订单数据；全部导出按当前筛选重新请求，最多 2000 条，两者都使用 `formatOrderExportCell(row, column.key)` 并严格遵循当前动态列顺序。

## 加载 / 空态 / 错误态

- orderState.loading 通过 v-loading 控制表格加载并禁用分页；非加载且请求成功、rows 为空时才显示“暂无订单数据”。
- 列表将真实空态与 401/403、网络异常、5xx/其他失败互斥展示，错误态提供“重新加载”；request-id 防止旧筛选响应覆盖新结果。
- detailLoading、submitting、附件上传、预警刷新和日志保存均有独立状态。
- 每条 shipment 的物流轨迹按订单、shipment、公司、单号和版本维护独立 loading/data/error 状态；只有 popover 打开时请求，失败短缓存期间直接复用错误，不冲击供应商。
- 详情打开前清空旧数据；加载、成功内容和错误态互斥显示，错误态留在抽屉内并可重试，request-id 防止跨订单旧响应。
- 编辑抽屉同样在打开前重置表单，通过独立 loading、错误态、重试和 request-id 隔离跨订单响应；加载失败或请求进行中禁止保存，关闭抽屉会使在途请求失效。
- 统计与预警统计失败只记录 console.warn，页面没有持久错误块或重试块。

## Element Plus 改造结果与保护面

- 已迁移：筛选与编辑表单使用 ElInput、ElSelect/ElOption、ElDatePicker、ElInputNumber；分页、抽屉、预警表单、状态、进度、空态和加载分别使用 ElPagination、ElDrawer、ElDialog/ElForm、ElTag、ElProgress、ElEmpty 和 v-loading。
- 预警设置由页面拥有的 ElDialog 表单承载，不再拼接 HTML 提示内容。
- 显式保留原生动态响应式订单表格：它承载动态列顺序、移动端 data-label、整行点击与操作按钮 stop；当前页导出使用结构化行数据和列 key，不依赖表格 DOM 文本；不得改造成破坏这些契约的固定列结构。
- 普通页面命令已迁移为 ElButton，并保留原有 `.stop`、CSS 和阶段权限语义；原生 button 只保留在统计/状态卡片和业务联想选项等定制交互面。
- 自定义：TableColumnSettings、DateFilterInput、BusinessTimeCorrectionPanel、DragAttachmentUpload。
- 工序步骤和客户/项目联想面板仍为业务自定义实现。

## Element Plus 对照与明确保留项

- button → ElButton；input → ElInput；select → ElSelect/ElOption；数字项 → ElInputNumber。
- 日期筛选可迁移 ElDatePicker，但 value-format 必须保持当前请求格式。
- 手写抽屉 → ElDrawer；表格/分页/空态/加载可评估 ElTable、ElPagination、ElEmpty、v-loading。
- 状态标签可用 ElTag，但文字、颜色和阶段含义必须保持。
- 明确保留：订单状态权限计算、两套状态流、审批分支、行点击与按钮 stop。
- 明确保留：动态列顺序、localStorage key、当前列驱动的当前页/全部导出。
- 明确保留：附件下载 Blob 流、10MB 限制、业务时间修正和客户/项目联动。

## 已发现风险

- 页面初始化仍需复核无 order:warning:setting 时是否读取预警设置；该权限组合纳入视觉 QA。
- 详情、编辑入口会按 `order:detail` 与阶段权限保持可见禁用并给出原因；handler 同时阻止无权限请求。
- TableColumnSettings 当前页和全部导出按 `order:list` 保持可见禁用并给出原因，全部导出 handler 仍执行二次守卫。
- order API 中 update、warning/refresh、health 三个封装当前未被页面使用，改造时不得误当死接口删除。

## 验证清单

- [ ] order:list、order:detail、order:create、order:warning:setting 与各 order:status:\* 组合逐一验证。
- [ ] `order:note:view/create/update` 分别验证可见、新增、修改和无删除入口；并发版本冲突不得覆盖他人内容。
- [ ] 新建待收款无审批，推进备料才生成 `order:audit:material` 审批候选。
- [ ] 普通流程、图纸预算、特殊订单、推进审批和回退审批不变。
- [ ] shipment 新增、修改、未保存行放弃、已保存行不可删除和 409 乐观锁冲突均通过。
- [ ] shipped 的多物流校验与“先保存再推进”、附件上传下载及日志时间修正通过。
- [ ] shipment-specific 轨迹只在 hover 时查询；30 分钟成功缓存、30 秒失败短缓存及 company-aware 身份均通过。
- [ ] list-only 用户的物流单号置灰且不会创建 popover 或触发 tracking API；有 `order:detail` 才按 `@show` 查询。
- [ ] `add_order_shipment` / `update_order_shipment` 操作日志在事务提交后生成且不泄露明文单号。
- [ ] 当前页和全部页导出均使用程序化 formatter，多物流单号使用 `、` 拼接。
- [ ] 筛选、卡片、路由关键字、分页和统计刷新一致。
- [ ] 动态列顺序刷新后保留，恢复默认可用。
- [ ] 当前页/全部导出列顺序一致，0 条与超过 2000 条提示正确。
- [ ] 加载、空态、403、详情失败和列表失败可区分。
- [ ] 桌面与窄屏下行点击、操作按钮和抽屉不重叠。
