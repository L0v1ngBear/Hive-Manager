# 组织部门

## 源码/路由/批次

- 页面：`management-ui/src/views/function/organization/organization.vue`。
- 前端 API：`management-ui/src/views/function/organization/api/organization.js`。
- 后端：`management/src/main/java/my/management/controller/OrganizationController.java`。
- 服务：`management/src/main/java/my/management/module/organization/service/OrganizationService.java`。
- 路由：`/function/organization`；入口权限 `employee:list`；feature 为 `module.employee`。
- 迁移批次：Batch 1；组织层级展示属于保留的自定义可视面。
- 当前状态：Audit baseline。

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
- 页面“新增部门”“新增下级”“编辑”“保存”没有 `v-permission`，后端保存要求 `employee:update`。
- 页面“删除”也没有 `v-permission`，后端删除要求 `employee:delete`。
- 因此前端会向只有 `employee:list` 的用户展示变更命令，点击后由后端返回 403。
- 组织模块复用员工权限码；当前源码没有独立 organization 权限，本迁移不新增权限定义。

## 状态流

1. 挂载后请求 overview，写入部门树和四项统计。
2. 无当前选择时自动选择第一个部门，并请求该部门成员。
3. 刷新后按旧部门 ID 在新树中重新定位；不存在则清空选择和成员。
4. 新增/编辑共用手写抽屉，提交前校验部门名称并规范化 parentId、sortNo、status。
5. 保存成功后关闭抽屉、刷新 overview，并重新加载当前部门成员。
6. 删除先由 `ElMessageBox` 确认，后端再次检查下级部门和员工，再逻辑删除。

## 空错态

- overview 有加载遮罩；无部门时显示创建引导空态。
- 成员区有独立 `memberLoading`，无选择与无成员使用不同文案。
- overview 和成员请求没有本地 `catch`，错误提示依赖全局 request。
- 选择新部门前不会先清空旧 `members`；成员请求失败时，旧成员可能出现在新部门标题下。
- 抽屉没有保存中状态、重复提交禁用或未保存关闭保护。

## 控件现状

- 部门树由递归 `DepartmentNode` 使用渲染函数和原生按钮绘制。
- 统计卡、成员卡和空态为自定义 HTML/CSS。
- 新增/编辑为手写侧滑层，字段使用原生 `input` 和 `select`。
- 删除确认和反馈使用 `ElMessageBox`、`ElMessage`。
- 当前没有 `ElTree`、`ElDrawer`、`ElForm` 或独立错误面板。

## Element Plus 对照/保留项

- 抽屉迁移为 `ElDrawer`，表单迁移为 `ElForm`、`ElInput`、`ElSelect`、`ElInputNumber`。
- 保存、删除、刷新命令使用 `ElButton`；状态可使用 `ElTag`。
- 加载/空态使用 `v-loading`、`ElEmpty`，但请求错误必须是独立状态。
- 删除继续使用 `ElMessageBox`，取消不报错；保存按钮增加真实 loading。
- 保留递归部门层级的缩进、节点状态、子部门命令和当前节点视觉，不为追求组件数量强制替换组织可视面。
- 保留后端 parentId 数字/null、status 数字和部门改名同步契约。
- 命令迁移后必须补回现有后端权限对应的 `v-permission`，但不创建新权限码。

## 风险

- 已确认的成员请求竞态：快速点击 A、B 部门时没有序号、AbortController 或 ID 校验；A 的慢响应可覆盖 B 的成员。
- 同一竞态中，较早请求的 `finally` 还可能提前关闭较晚请求的 `memberLoading`。
- 请求失败不会清空旧成员，违反“失败请求不得把陈旧数据显示在新实体下”的迁移不变量。
- 变更按钮无前端权限门，是明确的前后端权限不一致和可用性缺陷。
- 服务层名称唯一性是“先查询再写入”，数据库现有证据只明确部门编码唯一；并发同名创建需专项验证。
- 保存没有版本号或更新时间前置条件，并发编辑同一部门为最后写入者覆盖。
- 部门归属以名称同步和统计，改名是跨员工数据更新，迁移不能拆散事务或改变调用顺序。
- 删除确认文案不能替代后端的子部门/员工校验。

## 验证

- [ ] 用 `employee:list`、`employee:update`、`employee:delete` 的独立组合验证命令可见性和 403。
- [ ] 人工制造 A 慢、B 快的成员响应，确认最终标题和成员始终属于 B。
- [ ] 验证成员请求失败后不保留上一部门成员，并保持错误态可重试。
- [ ] 验证首个部门自动选择、刷新后重选、删除当前部门后的清空与回落。
- [ ] 验证自身/后代不能设为上级、同名校验、排序和启停状态。
- [ ] 验证有下级或有员工的部门不可删除，空部门可删除。
- [ ] 验证部门改名后员工列表、考勤筛选及组织统计同步。
- [ ] 验证抽屉窄屏、键盘焦点、Escape/遮罩关闭和重复提交保护。
- [ ] 运行目标测试、lint、生产构建，并确认四个 API 合约不变。
