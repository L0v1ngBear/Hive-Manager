# 考勤管理

## 源码/路由/批次

- 页面：`management-ui/src/views/function/attendance/attendanceManagement.vue`。
- 前端 API：`management-ui/src/views/function/attendance/api/attendance.js`。
- 后端：`management/src/main/java/my/management/controller/AttendanceManageController.java`。
- 服务：`management/src/main/java/my/management/module/attendance/service/AttendanceManageService.java`。
- 路由：`/function/attendance`。
- 路由权限：`attendance:record:list` 或 `attendance:*`；feature 为 `module.attendance`。
- 迁移批次：Batch 2；当前状态为 Audit baseline。

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
- 汇总、列表、部门初始加载没有独立错误区域，主要依赖全局 request。
- `refreshAll` 并行请求汇总和列表；任一失败时需要避免另一项成功结果被误判为整体成功。
- 列表失败不会主动清空旧 rows，筛选变化后存在陈旧数据显示风险。
- 规则抽屉没有独立加载骨架、加载失败重试面或保存中禁用。
- 导出失败没有页面内状态，依赖全局错误提示。

## 控件现状

- 顶部命令、筛选、统计、表格和分页主要为原生元素与 Tailwind。
- 日期筛选使用共享 `DateFilterInput`，列设置使用 `TableColumnSettings`。
- 规则为手写侧滑层，使用原生时间、数字、文本和地点列表控件。
- 状态显示为自定义 class pill。
- 文件下载由脚本创建链接完成；消息反馈使用 Element Plus 服务。

## Element Plus Migration

- Attendance filters, result table, loading/empty states, and pagination use explicit Element Plus component imports.
- The rule drawer retains its existing query and save contracts while using `ElDatePicker`, `ElTimePicker`, `ElInputNumber`, and `ElCheckboxGroup` for date, time, numeric, and work-day controls.
- The existing `attendance:record:list` and `attendance:*` permission boundaries, Blob export flow, overnight rule values, and query parameter formats are unchanged.

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

- 规则保存按钮没有 loading/防双击，重复点击可能发送并发覆盖请求。
- 规则更新没有版本字段；两个管理员同时编辑时为最后保存覆盖。
- 日期格式必须与 Spring `LocalDate` 解析一致，组件默认 Date 对象或时区转换会改变查询日期。
- 原生数字输入迁移时若把空值强制为 0，可能改变后端默认规则。
- 筛选失败保留旧 rows 会把上一条件的记录显示在新条件下。
- 汇总和列表是两个请求，切换日期时需要同一请求代次，否则可能组合出不同日期的数据。
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
