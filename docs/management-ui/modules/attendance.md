# 考勤管理

## 源码/路由/批次

- 页面：`management-ui/src/views/function/attendance/attendanceManagement.vue`。
- 前端 API：`management-ui/src/views/function/attendance/api/attendance.js`。
- 后端：`management/src/main/java/my/management/controller/AttendanceManageController.java`。
- 服务：`management/src/main/java/my/management/module/attendance/service/AttendanceManageService.java`。
- 路由：`/function/attendance`。
- 路由权限：`attendance:record:list` 或 `attendance:*`；feature 为 `module.attendance`。
- 迁移批次：Batch 2；当前状态为 Element Plus migrated（已完成 Element Plus 迁移）。

## 功能

- 按日期展示应到、实到、迟到、早退、缺勤/缺卡等汇总。
- 按员工关键字、部门、状态和日期筛选考勤记录。
- 服务端分页展示员工、工号、部门、签到、签退、状态和更新时间。
- 加载部门选项并支持从全局路由查询关键字进入页面。
- 打开规则抽屉，维护考勤时间规则和一个或多个打卡地点。
- 导出当前筛选条件对应的 Excel。
- 支持本地调整考勤表格列顺序。

## API 表

| 包装器                     | 方法与路径                     | 后端权限                 | 用途         |
| -------------------------- | ------------------------------ | ------------------------ | ------------ |
| `getAttendanceSummary`     | GET `/attendance/summary`      | `attendance:record:list` | 指定日期统计 |
| `getAttendancePage`        | GET `/attendance/page`         | `attendance:record:list` | 筛选分页     |
| `getAttendanceDepartments` | GET `/attendance/departments`  | `attendance:record:list` | 部门选项     |
| `getAttendanceRule`        | GET `/attendance/rule`         | `attendance:record:list` | 规则回显     |
| `saveAttendanceRule`       | POST `/attendance/rule/save`   | `attendance:*`           | 保存规则     |
| `exportAttendanceExcel`    | GET `/attendance/export-excel` | `attendance:record:list` | blob 导出    |

## 权限/feature

- `AttendanceManageController` 整体受 `module.attendance` 约束，与路由一致。
- 统计、记录、部门、规则读取和导出统一要求 `attendance:record:list`。
- “考勤规则”按钮使用 `v-permission="'attendance:*'"`，保存接口也要求 `attendance:*`。
- “导出”按钮和接口均要求 `attendance:record:list`。
- `attendance:*` 按现有通配规则覆盖 `attendance:record:list`，因此规则管理员可以读取回显。
- 当前页面不使用 `attendance:punch`；该权限属于打卡端，不应为管理页迁移而改写。

## 状态流

1. 页面初始化应用路由关键字，并加载汇总、列表和部门。
2. 查询或重置会把页码恢复为 1，再请求列表和汇总。
3. 翻页只在 `1..totalPages` 范围内更新并重新请求。
4. 打开规则抽屉时请求现有规则，将后端地点结构规范化为可编辑数组。
5. 用户可增加/删除地点；保存前把地点字段转为后端约定的数字/字符串载荷。
6. 保存规则成功后关闭抽屉并提示；记录本身不在本页面手工改写。
7. 导出复用当前关键字、部门、状态和日期，响应以 blob 下载。

## 空错态

- 列表有覆盖式 loading 和“暂无考勤记录”行。
- 汇总与列表分别互斥展示加载、真实空态、401/403 和网络/5xx 失败状态。
- `refreshAll` 使用同一查询快照和请求代次并行加载汇总与列表，旧响应不会覆盖新条件。
- 列表请求开始即清空旧 rows，失败状态提供重试。
- 规则抽屉打开即展示加载状态，并区分空态和失败重试；保存有 loading 与防重复提交。
- 导出失败没有页面内状态，依赖全局错误提示。

## 控件现状

- 顶部命令、筛选、统计、表格和分页使用显式导入的 Element Plus 组件。
- 日期筛选使用共享 `DateFilterInput`，列设置使用 `TableColumnSettings`。
- 规则使用 `ElDrawer`，时间、数字、文本、布尔开关和地点列表分别使用 `ElTimePicker`、`ElInputNumber`、`ElInput`、`ElCheckbox` 和 `ElButton`。
- 状态显示为自定义 class pill。
- 文件下载由脚本创建链接完成；消息反馈使用 Element Plus 服务。

## Element Plus 迁移

- 考勤筛选、结果表格、加载/空错态和分页均显式导入 Element Plus 组件。
- 规则抽屉保持原查询与保存契约，日期、时间、数值和工作日控件使用 `ElDatePicker`、`ElTimePicker`、`ElInputNumber` 和 `ElCheckboxGroup`。
- `attendance:record:list` 与 `attendance:*` 权限边界、Blob 导出、跨夜规则值及查询参数格式保持不变。

## Element Plus 对照/保留项

- 筛选迁移为 `ElInput`、`ElSelect` 和与 API 格式一致的 `ElDatePicker`。
- 列表迁移为 `ElTable`，分页用 `ElPagination`，状态用 `ElTag`。
- 加载/空态使用 `v-loading`、`ElEmpty`；增加独立 `ElAlert`/重试错误态。
- 规则侧滑层使用 `ElDrawer` 和 `ElForm`。
- 时间、宽限分钟、半径等字段使用 `ElTimePicker`、`ElInputNumber`，明确 min/max/step。
- 地点增删使用 `ElButton`，保留地点数组顺序、坐标/半径类型和至少一项等后端约束。
- 保留 `DateFilterInput` 的查询序列化以及 `TableColumnSettings` 的本地列顺序。
- blob 导出逻辑保留，不将二进制响应交给普通 JSON 处理。

## 风险

- 规则保存已使用 loading 与提交守卫防止同一页面重复提交；跨管理员并发仍为最后保存覆盖。
- 规则更新没有版本字段；两个管理员同时编辑时为最后保存覆盖。
- 日期格式必须与 Spring `LocalDate` 解析一致，组件默认 Date 对象或时区转换会改变查询日期。
- 数值控件保持显式边界、步长和经纬度六位精度，提交时继续按原载荷转换数值。
- 列表失败会清空旧 rows；汇总和列表共享查询快照与请求代次。
- `attendance:*` 是保存规则的现有权限，不能替换为新造的 `attendance:rule:update`。
- 导出权限与列表权限相同；迁移不得额外放宽到仅有页面入口即可导出。

## 验证

- [ ] 用 `attendance:record:list`、`attendance:*`、`attendance:punch` 账号验证入口和命令。
- [ ] 验证日期、关键字、部门、状态、重置和分页请求参数。
- [ ] 快速切换日期和筛选，确认汇总与列表属于同一条件且无陈旧响应覆盖。
- [ ] 验证空记录、403、500、慢请求和重试状态。
- [ ] 验证规则读取、时间字段、地点增删、数值边界和重复提交保护。
- [ ] 验证 Excel blob、文件名、当前筛选条件和无权限导出。
- [ ] 验证列顺序、窄屏 data-label、长员工名和长部门名。
- [ ] 运行目标测试、lint、生产构建，确认六个 API 与日期格式不变。
