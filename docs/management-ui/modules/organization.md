# 组织部门

## 源码/路由/批次

- 页面：`management-ui/src/views/function/organization/organization.vue`。
- 前端 API：`management-ui/src/views/function/organization/api/organization.js`。
- 后端：`management/src/main/java/my/management/controller/OrganizationController.java`。
- 服务：`management/src/main/java/my/management/module/organization/service/OrganizationService.java`。
- 路由：`/function/organization`；入口权限 `employee:list`；feature 为 `module.employee`。
- 迁移批次：Batch 1；组织层级展示属于保留的自定义可视面。
- 当前状态：Element Plus migrated with protected custom surface。

### 迁移结果（2026-07-13）

- 保留递归部门树；抽屉、表单、命令、加载/空错态使用显式导入的 Element Plus 组件。
- overview 请求前清空旧树和统计，互斥呈现 loading、真实空态、401/403、网络/5xx 失败；失败可重试。
- overview 请求开始即清空当前部门与成员并使在途成员请求失效，因此 403 或失败时不会暴露旧内容。
- overview 使用独立 request gate；旧成功、旧失败与旧 finally 均不能覆盖新请求，也不能反向触发部门自动选择而使新成员请求失效。
- 成员请求切换前清旧数据并使用 request-id，忽略过期响应与过期 finally。
- 新增、下级新增、编辑、保存按 `employee:update` 保持可见但禁用并说明原因；删除按 `employee:delete` 同样处理。

## 功能

- 加载部门树以及部门总数、启用数、员工数、空部门数。
- 选择部门后查看负责人、岗位数和直属部门成员。
- 新增一级部门、创建下级部门、编辑名称/编码/上级/负责人/排序/状态。
- 删除没有下级部门且没有员工的部门。
- 部门改名时，后端事务内同步员工记录中的部门名称。
- 后端校验父子循环、同租户部门存在性和部门名称重复。

## API 表

| 包装器                    | 方法与路径                                    | 后端权限          | 用途           |
| ------------------------- | --------------------------------------------- | ----------------- | -------------- |
| `getOrganizationOverview` | GET `/organization/overview`                  | `employee:list`   | 部门树与统计   |
| `getDepartmentEmployees`  | GET `/organization/department/{id}/employees` | `employee:list`   | 部门成员       |
| `saveDepartment`          | POST `/organization/department/save`          | `employee:update` | 新增或更新部门 |
| `deleteDepartment`        | DELETE `/organization/department/{id}`        | `employee:delete` | 删除空部门     |

## 权限/feature

- `OrganizationController` 整体受 `module.employee` 约束，与路由一致。
- 查看部门树和成员均使用 `employee:list`，与页面入口一致。
- “新增部门”“新增下级”“编辑”“保存”按后端 `employee:update` 权限保持可见；无权时按钮禁用，tooltip 说明原因，处理函数也会拒绝调用 API。
- “删除”按后端 `employee:delete` 权限采用相同的可见禁用与二次守卫策略。
- 组织模块复用员工权限码；当前源码没有独立 organization 权限，本迁移不新增权限定义。

## 状态流

1. 挂载后请求 overview，写入部门树和四项统计。
2. 无当前选择时自动选择第一个部门，并请求该部门成员。
3. 刷新后按旧部门 ID 在新树中重新定位；不存在则清空选择和成员。
4. 新增/编辑共用 `ElDrawer` 和 `ElForm`，提交前校验部门名称并规范化 parentId、sortNo、status；`sortNo = 0` 保持有效。
5. 保存成功后关闭抽屉、刷新 overview，并重新加载当前部门成员。
6. 删除先由 `ElMessageBox` 确认，后端再次检查下级部门和员工，再逻辑删除。

## 空错态

- overview 在请求前清空旧树和统计；加载、真实空态、401、403、网络异常与 5xx 失败互斥呈现，失败面板可重试。
- 成员区有独立 `memberLoading`；选择部门时先清空旧成员，并用 request-id 忽略过期响应与过期 `finally`。
- 成员的无选择、真实空态、401、403、网络异常与 5xx 失败互斥呈现，失败面板可重试。
- 抽屉没有保存中状态、重复提交禁用或未保存关闭保护。

## 控件现状

- 部门树仍由递归 `DepartmentNode` 绘制，以保留层级缩进、当前选中态和子部门命令；节点命令与状态使用 `ElButton`、`ElTag`。
- 新增/编辑使用 `ElDrawer`、`ElForm`、`ElInput`、`ElSelect`、`ElInputNumber` 与 `ElSwitch`。
- 页面命令使用 `ElButton`，成员状态使用 `ElTag`，加载与空态使用 `v-loading`、`ElEmpty`。
- 删除确认和反馈继续使用 `ElMessageBox`、`ElMessage`。

## Element Plus 对照/保留项

- 抽屉已迁移为 `ElDrawer`，表单已迁移为 `ElForm`、`ElInput`、`ElSelect`、`ElInputNumber`、`ElSwitch`。
- 保存、删除、刷新和节点命令使用 `ElButton`；部门和成员状态使用 `ElTag`。
- 加载/空态使用 `v-loading`、`ElEmpty`；overview 和成员请求使用本地 `ElResult` 错误面板及重试命令。
- 删除继续使用 `ElMessageBox`，取消不报错。
- 保留递归部门层级的缩进、节点状态、子部门命令和当前节点视觉，不为追求组件数量强制替换组织可视面。
- 保留后端 parentId 数字/null、status 数字和部门改名同步契约。
- 变更命令保持可见，并按既有后端权限码禁用、说明原因；未新增或改写权限码。

## 风险

- 服务层名称唯一性是“先查询再写入”，数据库现有证据只明确部门编码唯一；并发同名创建需专项验证。
- 保存没有版本号或更新时间前置条件，并发编辑同一部门为最后写入者覆盖。
- 保存命令没有独立 loading，快速重复点击仍可能产生并发提交；抽屉也没有未保存关闭保护。
- 部门归属以名称同步和统计，改名是跨员工数据更新，迁移不能拆散事务或改变调用顺序。
- 删除确认文案不能替代后端的子部门/员工校验。

## 验证

- [x] 源码契约测试验证 `employee:update`、`employee:delete` 的命令可见禁用、原因 tooltip 与处理函数守卫。
- [x] 源码契约测试验证成员切换前清旧数据、request-id 及过期 `finally` 防护。
- [x] 源码契约测试验证成员失败后不保留旧数据，并提供独立错误态和重试。
- [ ] 验证首个部门自动选择、刷新后重选、删除当前部门后的清空与回落。
- [ ] 验证自身/后代不能设为上级、同名校验、排序和启停状态。
- [ ] 验证有下级或有员工的部门不可删除，空部门可删除。
- [ ] 验证部门改名后员工列表、考勤筛选及组织统计同步。
- [ ] 验证抽屉窄屏、键盘焦点、Escape/遮罩关闭和重复提交保护。
- [ ] 运行目标测试、lint、生产构建，并确认四个 API 合约不变。
