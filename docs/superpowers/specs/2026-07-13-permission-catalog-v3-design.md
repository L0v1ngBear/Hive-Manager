# Hive 权限目录 V3 与员工个人权限设计

日期：2026-07-13

## 1. 背景

Hive 尚未正式上线，可以在不保留错误兼容行为的前提下重整权限。当前权限体系已经具备角色授权和员工个人 `GRANT` / `DENY`，但仍有以下结构性问题：

1. 页面入口、数据可见范围、订单状态和写操作混在同一个权限码中。
2. `order:list` 在部分接口中同时代表查看、编辑、推进、回退和打印，违反最小授权原则。
3. `order:status:*`、`module:*` 等通配符与个人 `DENY` 组合时可能重新放开已禁止的能力。
4. 员工权限页面使用三套分离选择器，无法直接看出角色来源、个人覆盖和最终有效结果。
5. 销售员、生产员虽然共用统一订单页，但没有可靠的本人、本部门数据范围隔离。
6. 部分模块借用其他模块权限，例如安装任务借用 `order:list`，审批中心缺少独立入口权限。
7. 员工停用、离职或权限目录变化后，旧 Token 和旧缓存不能保证立即失效。
8. 目录节点、通配符和实际动作均可被勾选，配置结果与运行时含义不一致。

本设计替代旧的“继续修补现有权限码”方案，形成统一、可解释、可测试的权限终态。

## 2. 目标与非目标

### 2.1 目标

- 角色决定岗位默认能力，员工个人权限只影响该员工，不修改角色。
- 员工权限配置只显示一棵统一树，并明确显示角色来源、个人覆盖和最终有效状态。
- 无权限入口统一可见但置灰、禁止点击且不请求业务内容；直接访问路由返回无权限页；后端始终做最终校验。
- 权限拆分为入口、动作、状态动作和数据范围，禁止用查看权限执行写操作。
- 统一订单页继续保留，销售和生产按业务责任范围隔离订单，不恢复旧销售订单页和生产订单页。
- 正式上线前清空原租户业务数据、用户角色关系和个人权限覆盖；新租户只使用 V3 内置角色矩阵。
- 员工停用、离职、角色变更和个人覆盖变更在下一次请求立即生效。
- 已存在的版本化迁移文件保持不变，只新增迁移；新迁移不承担历史业务数据或旧权限配置兼容。

### 2.2 非目标

- 不把平台管理员权限加入企业租户员工树。
- 不恢复已删除的 AI 建议权限、旧销售订单权限或旧生产订单权限。
- 本阶段不实现文档目录级密级权限；文档仍按模块动作授权。
- 不保留 `*`、`*:*`、`module:*` 作为企业租户可配置权限。

## 3. 方案比较与选择

### 方案 A：统一有效权限树（采用）

保留角色继承和个人覆盖，员工配置页以最终有效权限为主视图。勾选和取消勾选自动转化为个人 `GRANT` 或 `DENY`，并可一键恢复角色默认。订单另加状态动作和数据范围。

优点是配置结果直观、角色可持续维护、个人差异可审计，并能完整解决订单越权和数据范围问题。代价是需要同时调整数据库、后端鉴权、管理网页和小程序。

### 方案 B：保留三套权限选择器

继续分别展示角色权限、追加权限和禁止权限，只整理权限码。改动较小，但管理员仍需自行推导最终结果，父子回显和冲突解释继续复杂。

### 方案 C：把角色权限复制到员工

每个员工保存一份最终权限快照。页面简单，但角色后续修改不能自然继承，调岗和批量维护成本高，不适合商用。

## 4. 核心授权模型

### 4.1 权限节点类型

`sys_permission` 使用以下节点类型：

| 类型 | 含义 | 可直接分配 |
| --- | --- | --- |
| `GROUP` | 模块或分组目录 | 否 |
| `ENTRY` | 页面入口、列表访问 | 是 |
| `ACTION` | 新增、编辑、删除、上传、导出、审核等动作 | 是 |
| `STATE_ACTION` | 指定订单状态下的查看、推进、回退、取消 | 是 |
| `DATA_SCOPE` | 本人、本部门、已分配、全租户数据范围 | 是 |

父节点只负责组织和计算半选状态，不写入角色权限表或员工覆盖表。租户管理员只能分配启用且 `assignable=1` 的叶节点。

### 4.2 最终有效权限

```text
effective(permission) =
  permission.enabled
  AND permission.assignable
  AND (roleGrant(permission) OR personalGrant(permission))
  AND NOT personalDeny(permission)
```

固定规则：

1. 个人 `DENY` 始终优先于角色授权和个人 `GRANT`。
2. 角色只保存授权关系，不保存拒绝关系。
3. 员工个人覆盖只写 `sys_user_permission`，绝不修改 `sys_role_permission`。
4. 状态权限不推导页面入口，页面入口也不推导任何状态或动作。
5. 数据范围只缩小业务数据集合，不能授予没有的入口或动作。
6. 后端校验必须同时满足入口、动作、当前状态动作和数据范围，不信任前端隐藏结果。

### 4.3 员工权限三态语义

员工权限页面对每个可分配叶节点只提供一组三态选择：

- `继承角色`：不写个人覆盖，最终结果由角色授权决定。
- `个人允许`：写入个人 `GRANT`，角色未授予时仅为该员工开放。
- `个人禁用`：写入个人 `DENY`，即使角色已授予也仅对该员工禁用。

页面同时显示角色来源和三态选择后的最终生效结果，不再保留“角色权限、额外允许、单独禁用”三套分离选择器。父节点只用于组织、展开和搜索，不写入关系表，也不在服务端展开为隐式授权。

## 5. 权限目录

### 5.1 通用模块

| 模块 | 权限叶节点 |
| --- | --- |
| 总览 | `dashboard:view` |
| 企业公告 | `notification:announcement:list`、`notification:announcement:publish` |
| 库存 | `inventory:list/detail/warning:list/warning:setting/record:list/trend/barcode:search/model:search/cloth:in/cloth:out/import/export` |
| 出库打印 | `print:receipt:list/detail/execute/update/cancel` |
| 标签打印 | `print:label:list/detail/create/update/upload/default/disable` |
| 质量 | `quality:list/detail/create/update/process/audit/attachment:upload/attachment:download/export` |
| 客户 | `customer:list/detail/create/update/delete/import/export` |
| 价格 | `price:list/detail/create/update/publish/delete/import/export` |
| 审批中心 | `approval:list`、`approval:<leave|finance|resignation>:list/submit/detail/audit`、`approval:auditor:list/setting` |
| 安装任务 | `installation:list/detail/update/attachment:upload/attachment:download/export` |
| 考勤 | `attendance:punch/record:list/rule:list/rule:update/export` |
| 设备 | `equipment:list/detail/create/update/disable/inspection:list/inspection:submit/export` |
| 员工 | `employee:list/detail/create/update/status/delete/import/export/permission:manage` |
| 角色 | `role:list/create/update/delete/permission:list/permission:update` |
| 文档 | `document:list/folder:create/file:upload/file:download/rename/move/delete/export` |

斜杠用于文档压缩展示，数据库中每一项仍是完整权限码。例如 `inventory:list/detail` 表示 `inventory:list` 和 `inventory:detail` 两个独立叶节点。

### 5.2 统一订单权限

订单权限分为四层：

1. 入口：`order:list`、`order:detail`。
2. 通用动作：`order:create`、`order:update`、`order:print`、`order:warning:list`、`order:warning:setting`。
3. 审核动作：`order:audit:shipment`、`order:audit:cancel`。
4. 状态动作和数据范围。

每个订单状态是不可分配分组，下面只建立该状态真实支持的叶动作：

```text
order:status:<status>:view
order:status:<status>:advance
order:status:<status>:rollback
order:status:<status>:cancel
```

状态包括：

```text
budgeting
budget-completed
pending-confirm
pending-pay
pending-material
producing
pending-ship
shipped
completed
pending-cancel
cancelled
```

终态或不支持某动作的状态不创建对应动作叶节点。例如 `budget-completed`、`cancelled` 不创建推进节点。上线清库后只按 V3 叶节点重新授权，不把旧 `order:status:<status>` 转换或映射到任何新权限。

订单数据范围：

```text
order:scope:sales:self
order:scope:sales:department
order:scope:production:self
order:scope:production:department
order:scope:assigned
order:scope:installation:department
order:scope:tenant
```

列表、统计、详情、编辑、打印、推进、回退、取消和审核候选读取都必须复用同一个 `OrderAccessPolicy`，避免列表过滤后仍可通过订单 ID 越权访问。

## 6. 订单责任与数据范围

新增 `order_responsibility`：

```text
tenant_code
order_type
order_id
lane                SALES | PRODUCTION | INSTALLATION
user_id
department_id
active
create_time
update_time
```

唯一约束为租户、订单类型、订单 ID、责任线和当前有效责任记录。责任规则：

1. 新建订单时，创建人为销售责任人，记录创建人当时的部门。
2. 首次进入生产责任阶段时，显式选择的生产人员优先；未选择时，以执行该次流转的生产人员作为生产责任人。
3. 安装责任以安装任务中指定的施工/安装人员为准。
4. `self` 只匹配当前用户；`department` 匹配当前用户所在部门与责任记录部门；`assigned` 匹配任一有效责任记录；`tenant` 可访问租户内数据。
5. 同一用户拥有多个范围时取并集；个人 `DENY` 某一范围后，该范围不参与并集。
6. 平台管理员通过独立平台身份域访问，不写入订单数据范围权限。

由于系统尚未上线，正式发布前删除测试订单及其责任数据，不进行责任人回填或模糊推断。清库后产生的新订单全部按上述规则实时建立责任记录。

## 7. 内置角色矩阵

### 7.1 基础原则

- `ADMIN` 获得全部启用的租户叶权限和 `order:scope:tenant`，但不获得平台权限。
- 其他内置角色共同继承员工基础能力：总览、公告查看、本人打卡和考勤记录、三类申请提交及本人详情、文档查看。
- 专员获得本岗位日常操作；负责人获得专员能力、本部门范围、设置、审核和导出。
- 审核人必须同时拥有审核权限并命中当前业务记录的审核候选人，只有权限但未被选择不能审核。

### 7.2 订单岗位边界

| 角色 | 默认订单范围 | 可见状态 | 默认状态动作 |
| --- | --- | --- | --- |
| `SALES_STAFF` | `order:scope:sales:self` | 本人订单全部状态 | 创建、早期编辑、待确认推进、早期取消、打印 |
| `SALES_MANAGER` | `order:scope:sales:department` | 本部门订单全部状态 | 销售专员能力、预警设置、部门订单处理和导出 |
| `FINANCE_STAFF` | `order:scope:tenant` | 待收款、备料中 | 待收款处理和财务申请 |
| `FINANCE_MANAGER` | `order:scope:tenant` | 待收款、备料中 | 财务专员能力、财务审核和价格管理 |
| `PRODUCTION_STAFF` | `order:scope:production:self` | 备料中、生产中、待发货、已完成 | 生产阶段编辑、推进、允许的回退和打印 |
| `PRODUCTION_MANAGER` | `order:scope:production:department` | 备料中、生产中、待发货、已完成 | 生产专员能力、指派、质量处理和导出 |
| `WAREHOUSE_STAFF` | `order:scope:tenant` | 备料中、待发货、已发货 | 出入库、发货申请和出库打印 |
| `WAREHOUSE_MANAGER` | `order:scope:tenant` | 备料中、待发货、已发货 | 仓储专员能力、库存与打印设置和导出 |
| `INSTALLATION_STAFF` | `order:scope:assigned` | 已发货、已完成 | 只处理分配给自己的安装任务 |
| `INSTALLATION_MANAGER` | `order:scope:installation:department` | 已发货、已完成 | 本部门安装任务管理和导出 |
| `APPROVAL_MANAGER` | 审核候选记录 | 待发货、待取消 | 发货审核、取消审核；不能直接代替业务岗位推进 |

“全部状态可见”不等于“全部状态可操作”。销售人员可以跟踪本人订单全流程，但只有被明确授予的状态动作可以推进、回退或取消。生产人员只能看到分配给本人或本部门的生产责任订单。

### 7.3 其他内置角色

V3 新建：`ADMIN`、`EMPLOYEE`、`SALES_STAFF`、`SALES_MANAGER`、`WAREHOUSE_STAFF`、`WAREHOUSE_MANAGER`、`PRODUCTION_STAFF`、`PRODUCTION_MANAGER`、`QUALITY_STAFF`、`QUALITY_MANAGER`、`FINANCE_STAFF`、`FINANCE_MANAGER`、`HR_STAFF`、`HR_MANAGER`、`INSTALLATION_STAFF`、`INSTALLATION_MANAGER`、`APPROVAL_MANAGER`、`DOCUMENT_MANAGER`、`EQUIPMENT_STAFF`、`EQUIPMENT_MANAGER`。旧 `TENANT_OWNER`、旧 `AI_MANAGER`、自定义角色和旧角色授权均不迁移；清库后只保留这套新内置矩阵。

## 8. 员工个人权限页面

### 8.1 页面结构

员工详情中的“单独权限”使用一棵统一树：

- 顶部显示员工、岗位角色、权限版本和最后更新时间。
- 支持按模块、权限名称、权限码和“仅看个人调整”搜索。
- 每个叶节点显示三态选择器、角色来源标签、个人覆盖和最终生效结果。
- 角色授权显示“来自：销售专员”等来源；多个角色全部列出。
- 个人追加显示“个人增加”；个人禁止显示“个人禁用”。
- 父节点只负责组织和展开，不作为权限保存。
- 保存前显示增加、禁用、恢复角色默认的数量摘要。

页面重新打开时，已有个人配置必须直接选中对应三态；个人禁用的角色权限明确显示“角色有权限，个人已禁用”。

### 8.2 API

```http
GET /emp/employee/{userId}/permission-profile
PUT /emp/employee/{userId}/permission-overrides
```

GET 返回权限树节点、角色来源、个人覆盖、最终状态和版本：

```json
{
  "permissionVersion": 12,
  "permissions": [
    {
      "code": "order:status:pending-confirm:advance",
      "type": 4,
      "assignable": true,
      "roleSources": [{ "roleCode": "SALES_STAFF", "roleName": "销售专员" }],
      "personalEffect": null,
      "effective": true,
      "effectiveSource": "ROLE"
    }
  ]
}
```

PUT 使用稳定权限码，不使用数据库自增 ID：

```json
{
  "permissionVersion": 12,
  "grants": ["document:file:upload"],
  "denies": ["order:status:pending-confirm:cancel"]
}
```

服务端先按租户校验员工和 `permission_version`，再以条件更新抢占新版本；同一事务内整体替换该员工覆盖并清除管理端、小程序端权限缓存。版本冲突返回 `409`，前端提示权限已被其他管理员修改并重新加载，不覆盖新数据。请求和响应只使用稳定权限编码，旧权限 ID 请求格式和旧 `/permission-overrides` 读取接口直接删除。

## 9. 鉴权与会话失效

### 9.1 统一 evaluator

管理后端和小程序后端必须使用同一公共权限 evaluator，禁止各自实现不同的通配符或状态推导。evaluator 只处理精确权限码和个人拒绝，不再支持企业租户通配符。

### 9.2 权限版本

- `sys_user.permission_version`：角色或个人权限变化时递增。
- `sys_user.auth_version`：停用、离职、密码重置或强制退出时递增。
- `sys_permission_catalog.catalog_version`：权限目录或内置矩阵升级时递增。

Token 记录签发时的 `auth_version`。每次请求通过轻量缓存校验用户启用状态和版本；版本不匹配立即拒绝。权限缓存键包含用户权限版本和目录版本，因此无需等待 30 分钟自然过期，也不使用全库 `FLUSHDB`。

### 9.3 离职与停用

员工停用或离职必须在同一事务中：

1. 更新员工状态。
2. 递增 `auth_version` 和 `permission_version`。
3. 软删除有效角色绑定。
4. 保留个人覆盖用于审计，但运行时因用户状态无效而不加载。
5. 清理管理端和小程序端该用户缓存。

## 10. 数据模型

### 10.1 `sys_permission`

新增或规范字段：

```text
module_code
perm_type
assignable
status
sort
```

根节点 `parent_id` 为 `NULL`。权限码全局唯一；父子关系增加自关联约束；被角色或员工覆盖引用的节点不得物理删除。

### 10.2 关系表

- `sys_role_permission` 只关联启用叶节点。
- `sys_user_permission` 使用唯一键 `(tenant_code, user_id, permission_id)`，`effect` 只允许 `GRANT`、`DENY`。
- 新增租户一致性校验和必要索引；应用写入前再次校验角色、用户和权限属于同一租户域。
- 平台权限使用独立表或独立身份域，不写入租户角色和员工个人覆盖。

## 11. 全新上线策略

订单未开票预警迁移预留 `V20260713_002`。权限目录使用新的 `V20260713_003_permission_catalog_v3.sql`；如果合并前 `_003` 已被正式占用，则只提升本迁移版本号并同步 manifest，绝不改写已执行文件。

正式上线以清库后的全新数据为唯一目标，不提供升级库兼容路径：

1. 上线前按清理脚本删除测试租户、员工、角色关系、个人权限覆盖、业务单据、审批、库存流水和附件引用；平台基础配置按清理文档明确保留。
2. 从基线建立空业务库，再按 manifest 顺序执行新迁移。
3. 扩展权限目录和用户版本字段，创建 `sys_permission_catalog` 与 `order_responsibility`。
4. 直接写入 V3 权限目录；目录节点不可分配，只有启用叶节点可分配。
5. 不建立旧权限码映射，不迁移旧自定义角色，不迁移旧角色权限关系，不迁移个人 `GRANT` / `DENY`，不回填旧订单责任。
6. 新租户创建时由代码写入 20 个 V3 内置角色及其确定性权限矩阵。
7. 校验无通配符、无目录授权、无孤儿关系、无非法个人覆盖，并校验代码目录与数据库目录版本一致。
8. 将权限目录版本设为 `3`，管理端和小程序端只读取 V3 权限与缓存命名空间。

已存在的历史迁移文件仍保持不可变；这只是发布文件完整性约束，不代表支持历史数据升级。回滚依赖发布前备份或重建全新库。

## 12. 错误处理与审计

- 无入口或动作权限返回 `403`，不返回空列表伪装成功。
- 数据范围不匹配的详情和写请求统一返回 `403`，避免根据 `404` 判断其他租户或员工数据是否存在。
- 权限版本冲突返回 `409`。
- 停用、离职和旧 Token 返回 `401`，客户端清理登录态。
- 员工个人覆盖变更在同一事务写入 `emp_employee_change_log`，`change_type=PERMISSION_OVERRIDE`，记录租户、目标员工、操作者、变更前后 `grants/denies` 权限编码、权限版本和时间；控制器通用操作日志作为接口调用审计补充。
- 所有角色权限变更同样记录操作者、目标角色、变更前后、时间和租户。
- 审计日志只记录稳定权限码，不依赖自增 ID 解释含义。

## 13. 前端行为

1. 侧边栏、更多功能、按钮和小程序入口都读取同一份最终权限集合。
2. 无权限入口保持原位置并置灰，鼠标显示禁用，点击只提示“权限不足”，不发业务请求。
3. 有入口但无写动作时可查看数据，新增、编辑、推进、回退、取消、审核、上传和导出按钮分别置灰。
4. 直接输入无权限路由显示统一无权限页，页面业务组件不挂载。
5. 网络或后端错误必须显示加载失败，不能误显示为权限不足。
6. 小程序审核中心只加载有权限的页签；新建按钮由对应 `submit` 权限控制。

## 14. 测试策略

所有实现先写失败测试，再修改生产代码。

### 14.1 公共鉴权

- 角色授权、个人追加、个人禁用、恢复继承。
- `DENY` 优先、停用权限无效、父节点不可授权。
- 状态动作不推导入口，入口不推导动作。
- 企业租户通配符无效，平台身份域不受影响。

### 14.2 员工权限

- 已有最终权限正确打勾，角色来源和个人覆盖正确回显。
- 保存只修改目标员工，不修改任何角色权限。
- 父节点批量操作只展开有效叶节点。
- 权限版本冲突不覆盖新数据。
- 停用和离职后旧 Token 立即失效。

### 14.3 订单权限

- 销售本人、本部门与跨部门订单。
- 生产本人、本部门、未分配和跨部门订单。
- 列表、统计、详情、编辑、打印、推进、回退、取消均使用同一范围。
- 每个状态动作分别验证允许和拒绝。
- 发货审核和取消审核同时验证权限与候选审核人。
- 跨租户请求始终拒绝。

### 14.4 页面

- 无权限入口置灰、不可点击且不请求内容。
- 有查看无编辑时内容可见、写按钮禁用。
- 直接路由不挂载业务页面。
- 网络失败与 `403` 呈现不同状态。
- 员工权限树全选、半选、搜索和恢复角色默认正确。

### 14.5 迁移与部署

- 空库执行完整迁移链。
- 清理脚本执行后不残留测试租户、旧角色关系、个人覆盖或业务数据。
- 无活动旧通配符、无孤儿关系、无跨租户关系。
- 内置角色矩阵 checksum 与代码目录一致。
- 管理后端、小程序后端、管理网页和小程序源码包全部由同一提交构建。

## 15. 已沉淀实施记录

截至 2026-07-13 已完成并进入代码与部署校验的内容：

1. 公共权限 evaluator 改为精确编码判断，移除企业租户通配符和状态推导。
2. 建立 190 节点 V3 权限目录、20 个系统内置角色及确定性授权矩阵。
3. 部署迁移和静态门禁校验目录完整性、非法关系、个人覆盖 effect 和孤儿关系；不建立旧权限映射或历史授权迁移。
4. 员工个人权限替换为统一权限档案服务，使用角色来源、个人三态覆盖、最终有效结果和乐观版本。
5. 删除旧权限 ID 接口、父节点展开、通配符展开和三套分离选择器；不提供兼容入口。

## 16. 实施边界与顺序

权限改造按以下阶段实施，每一阶段通过测试后才进入下一阶段：

1. 公共权限 evaluator、目录模型和迁移。
2. 内置角色矩阵、员工个人权限 API 和统一权限树。
3. 管理后端各模块接口权限拆分。
4. 订单状态动作与数据范围。
5. 小程序后端权限和会话失效。
6. 管理网页与小程序的统一置灰、路由和按钮控制。
7. 全链迁移演练、构建、部署包更新和逻辑链文档回写。

部署包在四端代码、迁移、构建产物和权限校验全部一致前保持阻断状态，不发布部分完成的权限目录。

## 17. 验收标准

1. 角色配置和员工个人配置均只展示启用的可分配叶节点，目录不可保存。
2. 员工个人权限页只使用一棵树和一组三态控制，并能准确解释每个最终生效状态。
3. 对员工单独增加或禁用权限不会修改角色，也不会影响同角色其他员工。
4. `order:list` 只能开放订单入口，不能单独执行编辑、推进、回退、取消、打印或审核。
5. 销售员只能访问本人订单，销售负责人只能访问本部门订单；生产岗位按生产责任人和部门隔离。
6. 每个订单状态的查看、推进、回退和取消独立授权。
7. 安装、审批、质量、库存、打印等模块不再借用无关模块权限。
8. 无权限入口全量置灰、不可点击、不加载内容；直接接口调用返回 `403`。
9. 员工停用、离职或权限变化后，管理端和小程序端下一次请求立即使用新结果。
10. 清库后新租户统一获得 V3 内置角色矩阵；旧自定义角色和个人覆盖均不保留。
11. 历史迁移 SHA-256 不变，新迁移通过空库完整链路演练，不执行升级库兼容验收。
12. 四端自动化测试、管理网页构建、两个后端 Maven 测试和部署静态检查全部通过。
