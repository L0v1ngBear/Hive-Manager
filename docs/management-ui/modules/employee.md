# 员工管理

## 源码/路由/批次

- 页面：`management-ui/src/views/function/employee/employee.vue`。
- 新增/编辑：`management-ui/src/views/function/employee/employeeCreate.vue`。
- 员工个人权限：`management-ui/src/views/function/employee/EmployeePermissionDrawer.vue`。
- 组织图转换：`management-ui/src/views/function/employee/employeeOrganization.js`。
- 前端 API：`management-ui/src/views/function/employee/api/employee.js`。
- 后端入口：`management/src/main/java/my/management/controller/EmployeeController.java`、`OrganizationController.java`。
- 核心服务：`management/src/main/java/my/management/module/employee/service/EmployeeService.java`。
- 个人权限生效证据：`management/src/main/java/my/management/module/auth/mapper/AuthMapper.java`、`management/src/main/resources/sql/user_permission_overrides.sql`。
- 路由：`/function/employee`；入口权限 `employee:list`；feature 为 `module.employee`。
- 迁移批次：Batch 2；当前状态为 Element Plus migrated（已完成 Element Plus 迁移）。

## 功能

- 展示员工统计、筛选条件、分页列表和员工详情。
- 新增、编辑员工，维护部门、岗位、负责人、角色、考勤要求与考勤地点。
- 打开员工组织架构图；现有纯函数会从无上级员工中选择老板/最大层级根节点。
- 生成 15 分钟有效的组织加入码。
- 下载导入模板、导入员工、导出当前筛选结果。
- 为单个员工查看角色继承权限，并配置“额外允许”和“单独禁用”。
- API 层还保留状态调整、批量更新和删除包装器；当前主页面未直接暴露这些命令。

## API 表

| 包装器                              | 方法与路径                                    | 后端权限               | 当前用途               |
| ----------------------------------- | --------------------------------------------- | ---------------------- | ---------------------- |
| `getEmployeePage`                   | GET `/emp/employee/page`                      | `employee:list`        | 筛选分页               |
| `getEmployeeStats`                  | GET `/emp/employee/stats`                     | `employee:list`        | 顶部统计               |
| `getEmployeeDetail`                 | GET `/emp/employee/{id}`                      | `employee:detail`      | 详情及编辑回填         |
| `createEmployee`                    | POST `/emp/employee/create`                   | `employee:create`      | 新增员工               |
| `updateEmployee`                    | POST `/emp/employee/update`                   | `employee:update`      | 编辑员工               |
| `getEmployeePermissionOverrides`    | GET `/emp/employee/{id}/permission-overrides` | `employee:update`      | 个人权限回显           |
| `updateEmployeePermissionOverrides` | POST `/emp/employee/permission-overrides`     | `employee:update`      | 覆盖个人权限           |
| `changeEmployeeStatus`              | POST `/emp/employee/change-status`            | `employee:status`      | API 已有，主页面未接线 |
| `batchUpdateEmployees`              | POST `/emp/employee/batch-update`             | `employee:update`      | API 已有，主页面未接线 |
| `deleteEmployee`                    | DELETE `/emp/employee/{id}`                   | `employee:delete`      | API 已有，主页面未接线 |
| `searchEmployeeLeaders`             | GET `/emp/employee/leader/search`             | `employee:list`        | 负责人搜索             |
| `getEmployeeFormOptions`            | GET `/emp/employee/init-form-options`         | `employee:list`        | 部门、岗位、角色等选项 |
| `exportEmployees`                   | POST `/emp/employee/export`                   | `employee:export`      | JSON 导出包装器        |
| `exportEmployeesExcel`              | GET `/emp/employee/export-excel`              | `employee:export`      | 页面 Excel 导出        |
| `downloadEmployeeImportTemplate`    | GET `/emp/employee/import-template`           | `employee:export`      | 导入模板               |
| `importEmployees`                   | POST `/emp/employee/import`                   | `employee:create`      | multipart 导入         |
| `createOrganizationJoinCode`        | POST `/organization/join-code`                | `employee:create`      | 组织加入码             |
| `getAllPermissions`                 | GET `/sys/role/role/all`                      | `role:permission:list` | 个人权限树             |

## 权限/feature

- `EmployeeController` 整体受 `module.employee` 约束；路由也检查同一 feature。
- 路由要求 `employee:list`；查看命令按 `employee:detail` 保持可见置灰并说明原因。
- 编辑命令同时要求 `employee:update` 与回填所需的 `employee:detail`，handler 会在无权时停止请求。
- “单独权限”命令同时要求 `employee:update` 与 `role:permission:list`，无权时保持可见置灰。
- 下载导入模板在前端和后端均使用 `employee:export`；导入使用 `employee:create`。
- 个人覆盖保存只要求 `employee:update`，不存在独立的新权限码，本文件不建议凭空新增权限。
- 角色权限与个人覆盖在登录查询中合并：`GRANT` 返回原权限码，`DENY` 返回带 `!` 前缀的权限码；前端权限匹配先处理拒绝项。

## 状态流

1. 进入页面后加载列表与统计；查询、重置和翻页重新请求列表。
2. 新增时加载表单选项，提交后创建账号、角色关联和考勤地点，再刷新列表。
3. 编辑时先取详情，提交后同步员工主体、扩展资料、角色和考勤地点。
4. 离职状态在服务层会撤销角色；审批通过的离职也复用员工离职处理。
5. 个人权限抽屉并行加载权限树与覆盖数据，禁止同一 ID 同时出现在允许和禁用集合。
6. 保存个人权限时后端事务内删除该员工旧覆盖、写入新集合并清理权限缓存。

## 空错态

- 列表有加载遮罩和“暂无员工数据”；分页边界会禁用上一页/下一页。
- 个人权限抽屉互斥展示加载、就绪、真实空态、401/403 与网络/5xx 失败；失败可重试，错误时不渲染权限内容。
- 员工列表和组织架构使用请求代次，切换条件时清除旧数据，旧响应不能覆盖新请求。
- 导入结果通过消息反馈；文件格式、模板下载和 blob 导出错误仍依赖全局处理。
- 组织图无员工时显示空节点；该视图的专用空态应继续与请求错误区分。

## 控件现状

- 主列表、筛选、分页、详情命令和编辑抽屉均已迁移为显式导入的 Element Plus 控件。
- 员工表单使用 `ElDrawer`、`ElForm`、`ElInput`、`ElSelect`、`ElDatePicker`、`ElRadioGroup` 与 `ElButton`；关闭、取消、提交、考勤地点、角色和负责人结果均为 Element Plus 命令控件，备注使用 `ElInput type="textarea"`。
- 员工导入继续保留主列表中的隐藏原生 file input，以维持现有 `.xlsx` 选择和 multipart 上传契约。
- 个人权限使用 `ElDrawer`、`ElButton`、`ElEmpty`、`ElTree`、`ElTreeSelect` 和 `ElMessage`。
- 表格列顺序由 `TableColumnSettings` 和 `useLocalTableColumns` 持久化。
- 组织图使用 `vue3-tree-org` 与专用层级转换，不是普通数据表。

## Element Plus 迁移

- 员工列表为筛选、日期、表格状态、列、命令和分页显式导入 Element Plus 组件。
- 员工编辑与个人权限界面使用 `ElDrawer`，表单控件使用 `ElForm`、`ElInput`、`ElSelect`、`ElDatePicker`、`ElRadioGroup`、`ElTree` 和 `ElTreeSelect`。
- 组织架构图继续使用 `buildEmployeeOrganizationChart` 和自定义图形界面；导入导出、负责人搜索、权限 ID、事件和 API 载荷类型保持不变。

## Element Plus 对照/保留项

- 筛选输入、选择、日期、复选框：`ElInput`、`ElSelect`、`ElDatePicker`、`ElCheckbox`。
- 列表、分页、状态：`ElTable`、`ElPagination`、`ElTag`，必须保留列设置与响应式 data-label 行为。
- 新增/编辑/详情：`ElDrawer` 或 `ElDialog`，保留关闭、回填、负责人搜索和提交状态。
- 文件导入、按钮与反馈：`ElUpload`、`ElButton`、`ElMessage`；保持 multipart、blob 和权限指令。
- 加载/空态：`v-loading`、`ElEmpty`；网络失败不得降级成真实空数据。
- 保留 `vue3-tree-org`、`employeeOrganization.js` 的老板根节点算法、缩放/展开行为和组织图视觉面。
- 保留个人权限的三段语义及 deny 优先规则，不把权限 ID 转成字符串。

## 风险

- 明确的前后端权限不一致：详情、编辑回填和个人权限树可能在按钮可见后收到 403。
- `employee:update` 可直接调用个人覆盖保存；服务只校验目标员工属于当前租户、ID 有效且可分配，未见“不得给自己授权”或“不得超过操作者权限”的上限检查。
- 因此个人 `GRANT/DENY` 是高敏感能力；迁移不得弱化后端校验，也不得把它误写成普通角色编辑。
- 保存覆盖采用“先删后写”，事务可保证单次原子性，但并发编辑仍是后提交覆盖先提交。
- 列表 API 与状态/批量/删除包装器的接线范围不同，迁移时不得因“API 已存在”而擅自新增按钮。
- 员工详情含手机号等敏感字段；后端已有掩码逻辑，前端不得绕过或缓存明文。

## 验证

- [ ] 分别用 `employee:list`、`employee:detail`、`employee:update`、`role:permission:list` 的组合账号验证入口和按钮。
- [ ] 验证新增、编辑、负责人搜索、角色与考勤地点回填及员工容量限制。
- [ ] 验证导入模板、合法/非法导入、Excel blob 下载和筛选参数不变。
- [ ] 验证个人允许、个人禁用、冲突拦截、缓存失效及重新登录后的 deny 优先。
- [ ] 验证无数据、403、500、慢请求和快速切换筛选时不展示陈旧数据。
- [ ] 验证组织图老板根节点、无上级员工归并、缩放和窄屏布局。
- [ ] 运行前端测试、lint、生产构建，并确认本迁移未改变 API 路径和权限码。
