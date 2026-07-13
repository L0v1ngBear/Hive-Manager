# 总览大盘维护档案

> 状态：Task 8 Element Plus migrated；迁移批次：Batch 2；路由：`/dashboard`；feature：`module.dashboard`。

## 功能

- 按当前时段问候登录用户。
- 展示本月新增订单、预警订单、待我审批三个摘要卡片。
- 展示后端按权限生成的业务提醒、今日考勤摘要与异常人员。
- 展示后端返回的快捷动作并跳转到对应业务页。
- 分两组读取普通/紧急公告与重要公告，并提供公告中心和发布入口。

## 源码索引

| 层级     | 文件                                                                                        |
| -------- | ------------------------------------------------------------------------------------------- |
| 页面     | `management-ui/src/views/dashboard/index.vue`                                               |
| 前端 API | `management-ui/src/views/dashboard/api/dashboard.js`                                        |
| 公告 API | `management-ui/src/api/notification.js`                                                     |
| 路由     | `management-ui/src/router/index.js`                                                         |
| 控制器   | `management/src/main/java/my/management/controller/DashboardController.java`                |
| 服务     | `management/src/main/java/my/management/module/dashboard/service/DashboardService.java`     |
| 数据访问 | `management/src/main/java/my/management/module/dashboard/mapper/DashboardMapper.java`       |
| 出参     | `management/src/main/java/my/management/module/dashboard/model/vo/DashboardOverviewVO.java` |

## API

| 方法 | 接口                           | 请求                           | 当前用途                                   |
| ---- | ------------------------------ | ------------------------------ | ------------------------------------------ |
| GET  | `/dashboard/overview`          | 无显式参数                     | 摘要、visibility、业务提醒、考勤、快捷动作 |
| GET  | `/notifications/announcements` | `limit=4&levels=normal,urgent` | 普通与紧急公告                             |
| GET  | `/notifications/announcements` | `limit=8&levels=important`     | 重要公告                                   |

- `DashboardController` 受 `CODE_DASHBOARD` 租户功能开关保护。
- overview 方法没有单独的 `@RequirePermission`；服务按当前权限构建 visibility 和数据。
- 公告列表接口要求 `notification:announcement:list`。
- 发布按钮仅跳转发布页，发布动作不在本页面完成。

## 权限模型

- 路由只要求 `module.dashboard`，没有 route-level permission 数组。
- 订单、库存、审批、出库单、考勤 visibility 由后端权限上下文逐项计算。
- 订单统计还按当前用户可见的订单状态集合过滤。
- 快捷动作由后端按发布公告、审批、打印、员工、客户、价格权限动态生成。
- 发布公告按钮使用 `v-permission="'notification:announcement:publish'"`。
- 前端不依据 visibility 隐藏三个顶部摘要卡，而是显示后端默认的 0。

## 数据与状态流

1. 组件挂载调用 `fetchOverview()`，先设置全页 `loading=true`。
2. overview 成功后分别替换 summary、visibility、业务提醒、考勤和快捷动作。
3. 随后调用 `fetchAnnouncements()`；该调用未被 `await`，独立维护公告 loading。
4. 两个公告请求以 `Promise.allSettled` 并行执行，普通/紧急公告与重要公告分别落地成功数据或持久错误。
5. 快捷动作直接使用后端返回的 `action.route` 调用 `router.push`。

- 服务端 overview 结果按租户、用户和权限签名构造 Redis key，TTL 为 90 秒。
- summary 出参包含六项；当前页面只直接展示 `monthOrderCount`、`orderWarningCount`、`pendingApprovalCount`。
- `totalInventoryMeters`、`pendingPrintCount`、`inventoryWarningCount` 当前未直接渲染为摘要卡。

## 空态、错态与加载态

- overview 加载时显示手写 fixed 全屏遮罩和旋转 Material 图标。
- overview 请求前清空汇总与图表数据；失败时呈现可重试的独立错误面板，不保留旧数据，成功返回的零值仍按真实业务数据展示。
- 公告加载使用独立 `v-loading`，不会阻塞 overview 主数据。
- 公告真实为空只在对应请求成功且返回空数组时使用 `ElEmpty` 展示。
- 401、403、网络错误和 5xx 分别记录为认证、权限、网络和服务错误；区域错误持续显示并提供重试。
- 两组公告独立处理结果，其中一组失败不会清空或伪装另一组的成功结果。
- 业务提醒为空显示“暂无提醒”。
- 无考勤权限显示“暂无查看权限”；有权限但无数据使用后端 statusText。

## Element Plus 控件和样式现状

- overview 请求前清空 summary、visibility、提醒、考勤与快捷动作；独立错误面板区分 401/403、网络/5xx 并可重试，真实零值仍作为成功数据展示。
- overview 使用 request-id 维持 last-request-wins，旧成功、旧失败和旧 finally 不覆盖新状态。

- 快捷动作、发布公告和查看全部使用显式导入的 `ElButton`，后端下发 route 与点击方法保持不变。
- 公告加载和空态使用 `v-loading`、`ElEmpty`；区域错误使用持久状态与 `ElButton` 重试，全局请求消息保持不变。
- 摘要、公告、提醒和考勤保留原有非嵌套卡片布局，没有为迁移额外套入 `ElCard`。
- 业务图标继续使用 Material Symbols。
- 卡片在 1/2/3 列断点间响应，主体最大宽度为 `max-w-7xl`。
- 公告和提醒列表使用隐藏滚动条的局部滚动容器。

## 保留项

- overview 数据映射、快捷动作 route、公告 API 参数和发布权限未改变。
- 摘要卡保持当前信息密度和三列响应式布局。
- 公告 normal/urgent/important/critical/warning 文本和颜色映射保持原实现。

## 风险

- 只有 dashboard feature、没有公告列表权限的用户仍会发起公告请求，但 403 会显示独立权限状态而不再伪装为空公告。
- 公告重试会同时刷新两组数据；当前没有单组重试入口。
- 快捷动作的后端 route 未在前端再次校验；最终安全性依赖路由守卫和后端接口。
- overview 的缓存包含权限签名，但页面不会主动刷新，权限变化需重新进入或刷新。
- 页面定义的 summary 字段多于实际渲染字段，维护时容易误以为六项都已展示。

## 验证清单

- [ ] 有/无各业务权限时 visibility、统计、提醒和快捷动作同步变化。
- [ ] 无公告列表权限、公告为空和公告接口失败三种状态分别核验。
- [ ] overview 慢请求、失败、缓存命中和 90 秒后刷新数据正确。
- [ ] 发布按钮无权限时禁用，直接访问发布路由仍受路由/后端保护。
- [ ] 考勤 empty、waiting、warning、normal、no_permission 文案和颜色正确。
- [ ] 桌面、平板、手机下卡片和滚动列表无截断或重叠。
