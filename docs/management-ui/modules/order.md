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
- 查看分页订单表；点击行或“查看”图标打开订单详情。
- 查看订单明细、附件、物流、开票、流转码和状态流转日志。
- 新建订单；客户与项目可从客户选项联动，也允许录入新值。
- 编辑订单主体、明细、状态、物流、开票、业务时间和附件。
- 订单备注使用独立多记录：可新增、可修改，不允许删除；每条显示最后修改人和最后修改时间。
- 按当前订单阶段权限执行推进、回退审批和流转码补打。
- 配置分订单小项的未更新预警天数，并支持全量或单条重新计算。
- 调整表格列顺序，恢复默认，并导出当前页或当前筛选的全部数据。
- 业务时间修正模式下可修正订单创建时间及状态日志时间。

## 前端 API

| 封装函数                   | HTTP | 路径                             | 用途                         |
| -------------------------- | ---- | -------------------------------- | ---------------------------- |
| getOrderPage               | GET  | /orders                           | 分页查询与全量导出取数       |
| getOrderStatusSummary      | GET  | /orders/status-summary            | 状态、小项和开票统计         |
| getOrderDetail             | GET  | /orders/{orderId}                 | 详情、编辑回填、备注与日志刷新 |
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
- 默认普通流程：pending_confirm → pending_pay → pending_material → producing → pending_ship → shipped → completed。
- 图纸预算流程：budgeting → budget_completed。
- 创建或保存 `pending_pay` 不创建审批。只有用户执行 `pending_pay -> pending_material` 推进时才创建备料审批候选；审批通过后进入备料中，拒绝后保持待收款。
- 回退动作提交审批；特殊订单创建固定从 pending_confirm 进入审核语义。
- 推进到 shipped 前要求物流公司和物流单号；编辑保存也执行同样前端校验。
- 动态列默认 8 列：编号、客户/项目、订单信息、信息渠道、物流单号、状态、进度、时间。
- 列顺序以 hive.table.columns.order.list.commercial.v3 存在 localStorage；当前实现只排序，不隐藏列。
- 当前页导出读取 DOM；全部导出按当前筛选重新请求，最多 2000 条，并严格使用当前动态列顺序。

## 加载 / 空态 / 错误态

- orderState.loading 通过 v-loading 控制表格加载并禁用分页；非加载且请求成功、rows 为空时才显示“暂无订单数据”。
- 列表将真实空态与 401/403、网络异常、5xx/其他失败互斥展示，错误态提供“重新加载”；request-id 防止旧筛选响应覆盖新结果。
- detailLoading、submitting、附件上传、预警刷新和日志保存均有独立状态。
- 详情打开前清空旧数据；加载、成功内容和错误态互斥显示，错误态留在抽屉内并可重试，request-id 防止跨订单旧响应。
- 编辑抽屉同样在打开前重置表单，通过独立 loading、错误态、重试和 request-id 隔离跨订单响应；加载失败或请求进行中禁止保存，关闭抽屉会使在途请求失效。
- 统计与预警统计失败只记录 console.warn，页面没有持久错误块或重试块。

## Element Plus 改造结果与保护面

- 已迁移：筛选与编辑表单使用 ElInput、ElSelect/ElOption、ElDatePicker、ElInputNumber；分页、抽屉、预警表单、状态、进度、空态和加载分别使用 ElPagination、ElDrawer、ElDialog/ElForm、ElTag、ElProgress、ElEmpty 和 v-loading。
- 预警设置由页面拥有的 ElDialog 表单承载，不再拼接 HTML 提示内容。
- 显式保留原生动态响应式订单表格：它承载动态列顺序、移动端 data-label、整行点击与操作按钮 stop，并继续为当前页导出提供当前列 DOM；不得改造成破坏这些契约的固定列结构。
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
- TableColumnSettings 当前页和全部导出按 `table:export` 保持可见禁用并给出原因，全部导出 handler 仍执行二次守卫。
- order API 中 update、warning/refresh、health 三个封装当前未被页面使用，改造时不得误当死接口删除。

## 验证清单

- [ ] order:list、order:detail、order:create、order:warning:setting 与各 order:status:\* 组合逐一验证。
- [ ] `order:note:view/create/update` 分别验证可见、新增、修改和无删除入口；并发版本冲突不得覆盖他人内容。
- [ ] 新建待收款无审批，推进备料才生成 `order:audit:material` 审批候选。
- [ ] 普通流程、图纸预算、特殊订单、推进审批和回退审批不变。
- [ ] shipped 的物流校验、附件上传下载与日志时间修正通过。
- [ ] 筛选、卡片、路由关键字、分页和统计刷新一致。
- [ ] 动态列顺序刷新后保留，恢复默认可用。
- [ ] 当前页/全部导出列顺序一致，0 条与超过 2000 条提示正确。
- [ ] 加载、空态、403、详情失败和列表失败可区分。
- [ ] 桌面与窄屏下行点击、操作按钮和抽屉不重叠。
