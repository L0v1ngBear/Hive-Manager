# Hive 统一后端前端契约

## 适用范围

本契约同时约束管理网页和微信小程序。两端共用 `management` 中的统一 Spring Boot 后端，不再维护管理端后端、小程序后端两套实现。

## 服务入口

- 生产域名：`https://hellohive.top`
- 统一 API 前缀：`/api`
- Compose 业务服务：`backend`
- Compose 容器：`hive-backend`
- 禁止新增 `/web`、独立小程序 API 域名或第二个业务后端容器。

## 响应结构

普通响应只接受：

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

分页响应位于 `Result.data`，字段固定为 `current`、`size`、`total`、`pages`、`data`。前端不得兼容 `records`、`totalCount`、`code = 0` 或双层业务 `data`。

## 登录会话

小程序登录接口为 `/auth/mini/login` 和 `/auth/mini/wechat-login`，管理网页登录接口为 `/auth/admin/login`。`/auth/me` 与登录接口统一返回扁平 `LoginVO`：

- `token`
- `userId`
- `userName`
- `tenantCode`
- `tenantName`
- `tenantLogoUrl`
- `responseKey`
- `permissions`
- `features`

前端不得读取旧的 `user`、`tenant` 嵌套对象。`features` 使用 `module.*` 代码，小程序首页必须将其映射到对应现有入口；权限只读取 `permissions`。

## 权限规则

- 权限目录唯一来源：`PermissionCatalogV3`。
- 前端必须使用 Controller 上声明的精确权限码。
- 不使用通配权限、旧别名或 `table:export`。
- 无权限命令保持可见时必须禁用并阻止请求；受保护内容不得提前加载或展示。
- 订单数据范围继续由 `order:scope:*` 控制，合并订单页面不等于销售、生产人员可以越权查看彼此订单。

## 主要路由

- 订单：`/orders`、`/orders/status-summary`、`/orders/{id}`、`/orders/{id}/status-log`、`/orders/{id}/process`、`/orders/flow/{flowCode}/advance`
- 审批：`/approval/*`
- 库存：`/inventory/*`
- 质量：`/quality/*`
- 考勤个人记录：`/attendance/records/me`
- 微信订阅：`/wechat/subscriptions/*`
- 加入组织：`/auth/admin/join-organization/code`、`/auth/admin/join-organization`

完整接口以 `docs/api/unified-api-catalog.md` 和统一后端 Controller 为准。

## 订单物流合同

- 管理端订单发货正式使用 `shipments` 列表；每个发货批次分别提交 `shipments[].logisticsCompany` 和 `shipments[].trackingNo`，订单不再定义顶层物流字段。
- 已保存的 shipment 记录不可删除；后续操作只能更新或新增 shipment，以保留订单发货审计记录。
- 单条物流轨迹按 shipment 查询：`GET /orders/{orderId}/shipments/{shipmentId}/logistics-tracking`。
- 小程序前端当前不在本次订单多物流改造范围内。本合同不宣称小程序已经实现 shipment 列表物流；其后续适配必须以本节订单物流合同为准。

## 小程序适配约束

- 加入组织接口是公开接口，首次加入组织时必须使用 `needAuth: false` 和 `needTenant: false`。
- 首页入口同时受租户功能开关和精确权限控制，不能只根据 `features` 展示。
- 库存页面只请求当前用户有权读取的数据；只有出入库权限的员工也能进入页面并执行对应操作。
- 出库允许直接输入订单号；拥有 `order:list` 时才提供订单搜索候选。
- 统一订单的生产工序与异常上报使用 `POST /orders/{orderId}/process`，不再把生产字段提交给普通状态接口。
- 普通订单和图纸预算订单只允许推进到紧邻的下一状态，统一调用 `POST /orders/{orderId}/advance`；`/status` 不承担需要审批的正常推进。
- 发票状态固定为 `0=未开票`、`1=已开票`、`2=其他`，筛选、列表显示和发货表单必须覆盖三种值。
- 客户施工区域属于 `projects[].constructionArea`，不得作为客户顶层保存字段提交。
- 库存出库请求只提交 `barcode`、`meters`、`orderNo`。
- 本阶段小程序仅对齐已经存在的页面；安装任务仍是管理网页的独立入口，小程序不注册一个空壳页面。

## 安装任务部分更新语义

- `POST /installation-tasks/status` 省略 `installers` 表示保留现有施工人员。
- 显式提交 `installers: []` 表示清空施工人员；`completed_accepted` 状态不允许最终人员列表为空。
- 提交非空 `installers` 表示按请求顺序全量替换。

## 发布门禁

每次涉及接口、登录、权限或部署的改动至少执行：

1. 管理端 `npm test`。
2. 管理端 `npm run build`。
3. 后端 `mvnw.cmd test`（Linux 使用 `./mvnw test`）。
4. 小程序统一后端契约测试及全部 JS 语法检查。
5. 扫描旧后端地址、旧路由、旧响应字段和退役权限码。
6. `git diff --check`。
