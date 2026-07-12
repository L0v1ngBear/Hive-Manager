# 租户管理

## 源码/路由/批次

- 页面：`management-ui/src/views/function/tenant/tenant.vue`。
- 前端 API：`management-ui/src/api/tenantManage.js`。
- 后端：`management/src/main/java/my/management/controller/TenantManageController.java`。
- 核心服务：`TenantManageService.java`、`TenantLicenseService.java`。
- 平台边界：`management/src/main/java/my/management/common/interceptor/PlatformScopeInterceptor.java`。
- 租户可用性：`management/src/main/java/my/management/common/interceptor/AuthTokenInterceptor.java`。
- 路由：`/function/tenant`；`developerOnly: true`；无普通 permission/feature。
- 迁移批次：Batch 2；当前状态为 Audit baseline。

## 功能

- 列出所有未删除租户及品牌、联系人、负责人、套餐、容量、到期时间和已启用功能。
- 编辑企业名称、类型、联系人和联系电话。
- 上传企业 Logo，并在编辑当前租户品牌时同步用户 store。
- 编辑套餐、订阅状态、起止时间、员工/存储额度和原始 feature flags JSON。
- 启用或停用租户。
- 重新分配企业负责人账号，可复用已有员工或创建账号并重置初始密码。

## API 表

| 包装器                       | 方法与路径                                 | 服务端边界              | 用途           |
| ---------------------------- | ------------------------------------------ | ----------------------- | -------------- |
| `listTenants`                | GET `/platform/tenants`                    | 仅 `super` 平台租户路径 | 租户列表       |
| `listTenantFeatures`         | GET `/platform/tenants/features`           | 仅 `super` 平台租户路径 | feature 目录   |
| `updateTenantProfile`        | PUT `/platform/tenants/{id}/profile`       | 仅 `super` 平台租户路径 | 企业资料       |
| `uploadTenantLogo`           | POST `/platform/tenants/{id}/logo`         | 仅 `super` 平台租户路径 | multipart Logo |
| `updateTenantLicense`        | PUT `/platform/tenants/{id}/license`       | 仅 `super` 平台租户路径 | 授权与 feature |
| `updateTenantStatus`         | PUT `/platform/tenants/{id}/status`        | 仅 `super` 平台租户路径 | 启用/停用      |
| `reassignTenantOwnerAccount` | PUT `/platform/tenants/{id}/owner-account` | 仅 `super` 平台租户路径 | 负责人转移     |

## 权限/feature

- 前端 `developerOnly` 实际以当前 `tenantCode === "super"` 判断，不使用 `isDeveloper` 字段或权限码。
- `PlatformScopeInterceptor` 阻止非 `super` 账号访问 `/platform/*`。
- 同一拦截器还把 `super` 账号限制在 `/platform/*`、初始密码修改和上传路径。
- `TenantManageController` 的七个接口均没有 `@RequirePermission`，也没有 feature 注解。
- 因此平台租户内任一已认证账号均可调用所有读取和高危写接口；服务端边界是租户身份，不是细粒度操作权限。
- 本模块没有可据以新增的权限码；迁移文档只记录该边界，不虚构“租户管理员”等权限。

## 状态流

1. 页面挂载后并行加载 feature 目录和租户列表。
2. 企业资料保存后清租户运行缓存并刷新列表；Logo 上传切换到目标租户上下文存储文件。
3. 授权保存会规范化套餐/订阅状态、额度和 feature JSON，并清状态、授权、feature、考勤规则缓存。
4. 停用租户后，普通租户请求会在 `AuthTokenInterceptor.ensureTenantUsable` 被拒绝。
5. 到期、暂停或过期时间同样会使租户不可用。
6. 负责人重分配会确保内置角色存在，为目标绑定 `ADMIN`，移除其他人的 `ADMIN` 并降为 `EMPLOYEE`。
7. 目标负责人密码会被重设、`mustChangePassword=1`，账号置为在职并清理新旧负责人权限缓存。

## 空错态

- 列表有刷新 loading 和“暂无租户数据”。
- feature 目录加载失败会被静默转换为空数组，功能名回退显示 code，无法区分真实空目录与请求失败。
- 列表及各保存请求主要依赖全局 request 错误反馈。
- 企业资料、授权和负责人使用三个手写抽屉，没有未保存关闭保护。
- Logo 有上传中状态和前端类型/2MB 校验。
- 授权保存和负责人保存有 loading；状态切换没有行级 pending 锁。

## 控件现状

- 租户使用自定义卡片网格和状态/feature pill。
- 三个抽屉通过 `Teleport` 和手写遮罩实现。
- 表单为原生输入、数字输入、日期时间、选择、复选框和 textarea。
- Logo 上传为隐藏 file input 加点击/拖拽区域。
- 状态切换与负责人转移使用 `ElMessageBox`；反馈使用 `ElMessage`。
- 授权 feature flags 当前直接编辑 JSON 字符串。

## Element Plus 对照/保留项

- 卡片命令使用 `ElButton`；状态和 feature 使用 `ElTag`，保持停用弱化视觉。
- 三个侧滑层迁移为 `ElDrawer`，并用 `ElForm` 管理校验和提交状态。
- 文本、数字、时间、套餐、状态和考勤开关分别使用 `ElInput`、`ElInputNumber`、`ElDatePicker`、`ElSelect`、`ElSwitch`。
- Logo 使用 `ElUpload` 的拖拽模式，严格保留 multipart、类型、2MB 和目标租户上下文。
- feature JSON 可使用 `ElInput type="textarea"`；未设计结构化编辑器前不得改变字符串契约。
- 高危确认继续使用 `ElMessageBox`，取消属于正常结果。
- 保留平台专用信息密度，不改造成营销页，不增加普通租户入口。

## 风险

- 停用企业是即时锁户操作；确认后该租户的正常 API 会被拦截，影响所有在线用户。
- 将订阅设为 `EXPIRED/SUSPENDED` 或设置过去的到期时间同样可锁户，但授权保存当前没有二次确认。
- 负责人重分配是最高权限转移：会重置密码、授予 `ADMIN`、撤销旧负责人 `ADMIN` 并改变其角色等级。
- 平台接口只有 `super` 路径边界，没有每项操作权限；平台租户账号泄露时暴露全部租户控制面。
- feature flags 是原始 JSON；错误关闭功能会隐藏前端入口并触发后端 feature 拒绝。
- 授权、状态和负责人请求没有版本字段，两个平台操作员并发修改时为最后写入覆盖。
- 状态按钮在请求中未锁定，快速重复点击可基于同一旧状态发送相同目标值。
- 当前确认只覆盖启停和负责人转移；授权降级、额度归零、到期时间修改同样高危。

## 验证

- [ ] 非 `super` 账号访问页面和七个 `/platform/*` 接口均为 403。
- [ ] `super` 账号不能越过平台白名单访问普通业务接口。
- [ ] 验证资料、Logo、套餐、状态、额度和 feature JSON 的返回值及缓存失效。
- [ ] 在隔离租户验证停用、过期、暂停后的登录/API 拒绝以及恢复路径。
- [ ] 验证负责人新建、复用、跨租户账号冲突、密码重置、强制改密和新旧角色。
- [ ] 验证取消确认不发请求，高危操作重复点击不产生并发写。
- [ ] 验证 feature 目录失败与真实空态可区分，抽屉失败保留输入。
- [ ] 验证桌面/窄屏抽屉、长企业名、长 feature code 和 Logo 预览。
- [ ] 运行目标测试、lint、生产构建，确认没有新增权限码或普通租户入口。
