# 企业公告

## 源码/路由/批次

- 列表：`management-ui/src/views/function/announcement/announcement.vue`。
- 发布：`management-ui/src/views/function/announcement/publish.vue`。
- 共享 API：`management-ui/src/api/notification.js`。
- 后端：`management/src/main/java/my/management/controller/NotificationController.java`。
- 服务：`management/src/main/java/my/management/module/notification/service/EnterpriseAnnouncementService.java`。
- 路由：`/function/announcement`、`/function/announcement/publish`。
- 前端 feature：两条路由均为 `module.dashboard`。
- 迁移批次：Batch 1；当前状态为 Element Plus migrated。

## 功能

- 查看当前组织最近公告，默认上限 30 条。
- 按全部、普通、紧急、重要四个等级切换。
- 展示标题、正文、等级、更新时间、已读/未读/总接收人数。
- 展开式展示接收人姓名、部门、岗位和个人已读状态。
- 有发布权限时进入发布页。
- 发布普通、紧急或重要公告，校验标题和正文后返回列表。
- 当前页面没有公告编辑、撤回、删除或手工标记已读功能。

## API 表

| 包装器                | 方法与路径                          | 后端权限                            | 用途                     |
| --------------------- | ----------------------------------- | ----------------------------------- | ------------------------ |
| `getAnnouncements`    | GET `/notifications/announcements`  | `notification:announcement:list`    | 列表、等级筛选、阅读统计 |
| `publishAnnouncement` | POST `/notifications/announcements` | `notification:announcement:publish` | 发布公告                 |

`notification.js` 还包含未读通知、标记已读、关闭任务和同步通知接口；它们属于共享通知面，不是本页面当前交互。

## 权限/feature

- 列表路由允许 `notification:announcement:list`、`notification:announcement:publish` 或 `dashboard:*` 任一权限。
- 发布路由允许 `notification:announcement:publish` 或 `dashboard:*`。
- 后端列表只接受 `notification:announcement:list`，发布只接受 `notification:announcement:publish`。
- 因此“仅发布”账号可进入列表路由但列表请求 403；“仅 dashboard:\*”账号可进入两条路由但公告接口仍会 403。
- 列表页的发布按钮只按 `notification:announcement:publish` 计算，不把 `dashboard:*` 当成发布权限。
- 后端 `NotificationController` 的公告接口没有 `@RequireTenantFeature(module.dashboard)`，feature 只在前端路由层存在。
- 以上为明确的前后端权限/feature 差异；迁移不得把 `dashboard:*` 静默改写成公告权限。

## 状态流

1. 列表挂载后以 `limit=30` 请求公告。
2. 选择等级先更新 `activeLevel`，再以 `levels` 参数重新加载。
3. 后端返回接收人及 readFlag，前端汇总展示，不在本页修改阅读状态。
4. 发布表单初始等级为 `normal`，标题上限 80，正文上限 1000。
5. 标题或正文为空时在前端拦截。
6. 发布成功后清空表单并跳回公告列表。
7. 等级展示兼容 `urgent/critical` 和 `important/warning`，普通等级为默认分支。

## 空错态

- 列表明确区分“正在加载公告”和“暂无公告”。
- 加载失败使用 `ElMessage.error`，但页面没有持久错误块或重试说明。
- 切换等级前不会清空旧公告；请求失败后，新标签可能继续显示上一等级的数据。
- 发布按钮有 `publishing` 禁用和文案变化。
- 发布失败保留表单内容并提示；没有离开页面时的未保存确认。
- 接收人为空时显示“当前暂无可统计人员”。

## 控件现状

- 页面布局、公告卡和接收人列表保留领域布局；命令与等级切换使用 `ElButton`。
- 发布页使用 `ElForm`、`ElSelect`、`ElOption`、`ElInput` 和 `ElButton`。
- 列表使用 `ElSkeleton`、`ElEmpty` 和 `ElTag`；反馈继续使用 `ElMessage`。
- 公告列表是双列卡片，不是传统表格。
- 已读人员区域使用限定高度的自定义滚动列表。

## Element Plus 对照/保留项

- 等级筛选使用 `ElButton`，值仍为 `all/normal/urgent/important`。
- 等级和阅读状态使用 `ElTag`，保持现有紧急/重要/普通语义颜色。
- 发布表单使用 `ElForm`、`ElSelect`、`ElInput` 和 textarea 模式。
- 命令使用 `ElButton`，发布按钮保留真实 loading 和权限禁用原因。
- 加载、空态、错误分别使用 `ElSkeleton`、`ElEmpty`、`ElMessage`，不得互相替代。
- 保留公告卡与接收人阅读明细的领域布局，不强行改造成 `ElTable`。
- 保留正文换行、长文本滚动、接收人字段和 `limit/levels` 查询契约。

## 风险

- 路由允许集合比后端公告权限宽，是已确认的权限不一致。
- 前端 feature 与后端 feature 保护不一致；直接 API 调用不依赖 `module.dashboard`。
- 等级切换失败会把上一筛选结果显示在新选中等级下。
- 公告阅读明细包含员工姓名、部门、岗位和阅读状态，`list` 权限具有组织行为可见性。
- 发布页虽有长度限制，迁移到 `ElInput` 时仍须保留 trim、maxlength 和换行载荷。
- 发布成功立即跳转；重复请求保护仅依赖单页 `publishing`，刷新/多标签页没有幂等键。
- 列表 key 在缺少 id 时退化为标题和更新时间，重复公告可能产生渲染复用。

## 验证

- [ ] 用仅 list、仅 publish、仅 `dashboard:*` 和组合账号验证两条路由与两个接口。
- [ ] 验证 feature 关闭时前端入口，以及直接 API 的现有后端行为。
- [ ] 验证四个等级参数、兼容等级文案和 30 条上限。
- [ ] 验证加载、真实空、403、500、慢请求和等级切换失败时无陈旧卡片。
- [ ] 验证接收人 0 人、长名单、长姓名、长部门和已读/未读计数。
- [ ] 验证标题/正文 trim、80/1000 上限、发布中禁用、失败保留和成功跳转。
- [ ] 验证桌面双列、窄屏单列、正文换行和键盘焦点。
- [ ] 运行目标测试、lint、生产构建，确认两条公告 API 未改变。
