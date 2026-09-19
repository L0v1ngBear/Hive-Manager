# 布局与导航维护档案

## Global Teal Theme Migration Note (2026-07-15)

**Migration status:** The application shell, sidebar, and navbar consume the
shared teal contract. Selected navigation, primary shell commands, badges, and
loading affordances use the canonical primary tokens while protected status
colors retain their existing meaning.

The migration does not change routes, route metadata, access checks, permissions,
menu structure, responsive layout behavior, or notification APIs. Deep-primary
shell descendants retain white text and icons through the shared cascade rule;
disabled primary controls use the neutral disabled palette.

Controller browser QA passed the recorded contrast and overflow checks for public
entry screens: Login at 1440x900, 1024x768, and 390x844, plus JoinOrganization
at 1024x768. Protected business pages remain unapproved: `/function/order`
redirected to login because no local authenticated session was available and the
local API surfaced HTTP 502.

> 状态：Task 8 Element Plus migrated；迁移批次：Batch 2；范围：应用壳、路由守卫、侧栏、顶栏、搜索与通知入口。

## 功能

- `Layout` 组合桌面/移动侧栏、顶栏、滚动主区和路由页面容器。
- 侧栏提供品牌/租户信息、主菜单、更多菜单、折叠状态和审批待办数量。
- 顶栏提供当前页标题、跨模块搜索、通知待办、租户/用户菜单和退出登录。
- 路由守卫处理公开页、登录重定向、强制改密、平台租户、套餐功能和权限拒绝。

## 源码索引

| 文件                                                   | 当前职责                                 |
| ------------------------------------------------------ | ---------------------------------------- |
| `management-ui/src/router/index.js`                    | 路由表、meta 与全局前置守卫              |
| `management-ui/src/layout/index.vue`                   | 应用壳、移动抽屉、滚动复位、父级视口模式 |
| `management-ui/src/layout/components/Sidebar.vue`      | 品牌、菜单、折叠和审批角标               |
| `management-ui/src/layout/components/Navbar.vue`       | 标题、搜索、通知、用户菜单和退出         |
| `management-ui/src/components/ResponsivePageFrame.vue` | 子路由响应式外壳                         |
| `management-ui/src/utils/access.js`                    | 菜单与路由访问状态                       |
| `management-ui/src/stores/user.js`                     | 会话、权限、功能和租户身份               |

## 路由与元数据

- `/dashboard`、`/manual`、`/no-permission` 作为 `Layout` 子路由。
- 业务页位于 `/function/*`，路由 meta 可包含 `title`、`permissions`、`features`、`developerOnly`。
- `permissions` 和 `features` 都按“数组中任一项满足”放行。
- `/function/tenant` 使用 `developerOnly`，实际判定是 `userStore.isPlatformTenant`。
- `/no-permission` 通过 `allowDenied` 避免再次进入拒绝循环。

## Router / Sidebar / Navbar 元数据漂移

- 路由表是页面访问守卫的数据源，但 Sidebar 和 Navbar 没有从路由 meta 派生菜单。
- Sidebar 重复维护菜单名称、路径、图标、权限数组和 `menuFeatureMap`。
- Navbar 再次维护搜索名称、描述、路径、图标、权限数组和另一份 `menuFeatureMap`。
- Navbar 的 `menuFeatureMap` 缺少 `/function/installation-task` 与 `/function/equipment`。
- Navbar 的可搜索菜单包含安装任务，但完全没有设备巡检入口。
- Sidebar 同时包含安装任务与设备巡检，并为两者映射 `module.order`、`module.equipment`。
- 路由只在 Dashboard meta 中写了 icon，其余导航图标来自侧栏/顶栏私有数据。
- 公告发布在路由与 Navbar 搜索中存在，但不在 Sidebar 菜单中。
- 因上述重复维护，新增或修改路由时可能出现“能路由、不能搜索”或 feature 禁用原因不一致。

## API

Navbar 通过 `management-ui/src/api/notification.js` 调用：

| 方法 | 接口                        | 用途                     |
| ---- | --------------------------- | ------------------------ |
| GET  | `/notifications/unread`     | 打开通知面板时读取未读项 |
| POST | `/notifications/{id}/read`  | 打开通知前标记已读       |
| POST | `/notifications/{id}/close` | 将待办标记完成或跳过     |
| POST | `/notifications/sync`       | 有发布权限时手动同步待办 |

- Sidebar 通过审批 API读取摘要并展示总待办数。
- 通知加载失败显示 warning；标记已读失败不会阻止业务跳转。

## 权限与访问流

1. 公开路由直接放行；已登录访问 `/login` 时转强制改密或目标页。
2. 无 token 的受保护路由跳转 `/login?redirect=...`。
3. `mustChangePassword` 为真时只允许强制改密页。
4. 平台租户除 `/function/tenant` 外统一重定向到企业授权页。
5. 之后按 feature、permission、developerOnly 计算拒绝原因。
6. 被拒绝时显示 `ElMessage.warning`，跳到 `/no-permission?from=...`。

- 菜单不隐藏无权限普通项，而是装饰为 disabled 并显示原因。
- 左侧导航不展示“企业授权”；平台租户仍由路由守卫进入 `/function/tenant`，企业授权页面、顶部应用入口及接口能力保持不变。
- 退出使用 `ElMessageBox.confirm`，确认后清会话并替换到 `/login`。

## UI 状态流

- `Layout` 根据窗口宽度生成 compact/comfortable/wide，并在路由变化时关闭移动菜单、复位主滚动区。
- 桌面侧栏默认折叠；移动侧栏作为遮罩层中的第二个 Sidebar 实例。
- “更多功能”在当前路由属于次级菜单时自动展开。
- 顶栏搜索支持模块入口，并对订单号、条码、客户、员工、价格和考勤生成带 query 的直达结果。
- 顶栏不再使用固定桌面高度；页面标题、搜索、企业标识、通知和账号区按可用宽度收缩，长企业名称只在自身区域省略，不能遮挡通知、账号或主内容。
- 搜索、通知和用户菜单互斥打开；点击组件外部统一关闭。

## 空态、错态

- 无次级菜单时“更多功能”不显示。
- 无可用搜索结果时没有专门空面板；回车只尝试第一个未禁用结果。
- 结果全部禁用时回车显示“当前账号暂无权限打开这些入口”。
- 通知请求失败仅提示并保留当前数组；平台租户直接清空通知。
- 审批摘要请求失败时 Sidebar 将角标归零，不区分真实 0 与请求失败。

## Element Plus 控件和保留项

- Navbar 通知面板使用显式导入的 `ElPopover`、`ElBadge`、`ElButton`；打开时仍刷新未读通知，完成/跳过/同步 API 不变。
- Navbar 用户菜单使用 `ElDropdown`、`ElDropdownMenu`、`ElDropdownItem`；退出仍经过 `ElMessageBox.confirm`。
- Sidebar 菜单、更多功能和折叠命令使用 `ElButton`，审批待办数使用 `ElBadge`。
- 菜单仍由 RouterLink、`decorateAccessItems`、feature/permission 检查和原导航方法驱动，禁用警告文案不变。
- 移动侧栏和搜索结果面板保持原响应式结构；本任务没有修改路由元数据或三份导航描述数据。
- 图标继续使用本地 Material Symbols 字体，Element Plus 组件均为显式导入。

## 风险

- Navbar 缺失 feature 映射会使搜索入口与路由守卫显示不同访问状态。
- 设备巡检无法从顶部搜索发现，但路由和侧栏可进入。
- 审批角标权限集合与审批路由权限集合并非同一份数据。
- 手写浮层缺少 Element Plus 内建焦点管理与 Escape 行为。
- 菜单 active 使用 `startsWith`，路径前缀相近时可能同时判定活跃。

## 验证清单

- [ ] 逐项比对路由、侧栏和搜索的 path、title、permissions、features、developerOnly。
- [ ] 套餐未启用、账号无权限和平台租户三类禁用原因分别正确。
- [ ] 登录、强制改密、拒绝页和 redirect 参数无循环跳转。
- [ ] 桌面折叠、移动遮罩、路由后自动关闭和滚动复位正常。
- [ ] 搜索直达 query 与目标页面现有解析契约一致。
- [ ] 通知已读、完成、跳过、同步及失败提示均可复现。

## 2026-09-19 顶栏职位与员工列表同源（热修）

- 现象：顶栏职位是固定文案（`const roleLabel = computed(() => '运营管理')`），与员工管理列表中的员工档案 `user.position` 不一致。
- 修复（前后端必须同版本发布）：后端 `AuthMapper` 11 处查询补 `u.position AS positionName`，`LoginVO`/`LoginUserRow` 暴露该字段，`AuthenticationService` 把空白归一为 `null`；前端 `Navbar.vue` 改为 `userStore.userInfo?.positionName || '未设置职位'`，`stores/user.js` 在登录与会话刷新两处透传 `positionName`。
- 约定：员工未维护职位时显示"未设置职位"，不得用固定文案冒充；顶栏与员工列表必须读取同一事实。
- 验证：`tests/navbar-user-position.test.js`（3 例静态契约）与 `AuthenticationServiceTest` 两条服务用例通过；顶栏真实显示待发布后人工点验。
- 关联交接：`docs/operations/2026-09-19-hotfix-role-permission-and-topbar-position.md`。
