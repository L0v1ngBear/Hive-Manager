# 认证与入口维护档案

> 状态：Audit baseline；迁移批次：Batch 2；范围：登录、加入组织、强制改密、法律页、拒绝页、会话 store 与路由守卫。

## 功能

- `/login` 提供公开登录入口。
- `/join-organization` 提供公开的加入组织入口。
- `/force-password-change` 承接首次/强制改密状态，必须已有会话。
- `/privacy` 与 `/terms` 共用法律内容页面，均为公开路由。
- `/no-permission` 在应用壳内展示页面访问被拒绝，并接收来源地址。
- 全局守卫统一处理 token、redirect、强制改密、平台租户和页面权限。

## 源码索引

| 文件                                                                    | 当前职责                         |
| ----------------------------------------------------------------------- | -------------------------------- |
| `management-ui/src/views/Login.vue`                                     | 登录入口和本地交互状态           |
| `management-ui/src/views/JoinOrganization.vue`                          | 加入组织入口                     |
| `management-ui/src/views/ForcePasswordChange.vue`                       | 强制修改初始密码                 |
| `management-ui/src/views/NoPermission.vue`                              | 拒绝页与返回操作                 |
| `management-ui/src/views/legal/LegalPage.vue`                           | 隐私政策/服务条款共用页面        |
| `management-ui/src/api/auth.js`                                         | 认证相关 HTTP 封装               |
| `management-ui/src/stores/user.js`                                      | 会话、用户、权限、功能与租户状态 |
| `management-ui/src/utils/request.js`                                    | 请求鉴权、响应解包和会话异常处理 |
| `management-ui/src/utils/secure.js`                                     | 认证响应加解密辅助               |
| `management-ui/src/utils/redirect.js`                                   | 登录 redirect 构造与归一化       |
| `management-ui/src/router/index.js`                                     | 认证/授权前置守卫                |
| `management/src/main/java/my/management/controller/AuthController.java` | 后端认证入口                     |

## 前端会话 API

`useUserStore` 对页面暴露的稳定接口：

| 方法/状态                        | 当前行为                                                                 |
| -------------------------------- | ------------------------------------------------------------------------ |
| `setLoginInfo(loginData)`        | 写入 token、用户、permissions、features、responseKey、expireAt、改密标记 |
| `renewSession(...)`              | 同步更新 token、responseKey 和 expireAt                                  |
| `markPasswordChanged()`          | 清除 `mustChangePassword` 并写回存储                                     |
| `updateTenantBrand(...)`         | 更新当前组织名称/logo                                                    |
| `logout()`                       | 清空内存及两类 Web Storage 的认证键                                      |
| `hasPermission/hasAnyPermission` | 复用通配与拒绝权限匹配                                                   |
| `hasFeature/hasAnyFeature`       | 判断租户功能；旧会话无 features 时兼容基础 module                        |

- HTTP 路径和 payload 由 `src/api/auth.js` 集中封装，入口页面不应自行拼接认证 URL。
- 登录结果被归一到上述 store 字段，路由与请求层只消费 store，不各自维护第二份用户状态。

## 存储与 token 状态

- 当前认证主存储是 `window.sessionStorage`，不是持久 localStorage 登录。
- 键集合为 `token`、`userInfo`、`permissions`、`features`、`responseKey`、`expireAt`、`mustChangePassword`。
- store 模块加载时主动清理 localStorage 中同名认证键。
- 登录、续期、品牌更新和改密后也会清理 localStorage，再写 sessionStorage。
- `isPlatformTenant` 由当前 `tenantCode` 小写等于 `super` 判断。
- `isDeveloper` 来自登录数据的 `developer` 字段，但路由 `developerOnly` 实际使用平台租户判定。

## 路由状态流

1. `meta.public` 路由无需 token；已登录访问 `/login` 会跳到改密页或规范化 redirect。
2. 受保护页无 token 时跳 `/login`，并通过 `buildLoginQuery(to.fullPath)` 保留目标。
3. 已登录且 `mustChangePassword=true` 时，除强制改密页外全部重定向到该页。
4. 改密标记清除后再访问强制改密页会回到 `/dashboard`。
5. 平台租户除企业授权页外统一重定向 `/function/tenant`。
6. feature/permission/developerOnly 拒绝时提示并跳 `/no-permission?from=...`。

- `/no-permission` 与 `allowDenied` 路由在鉴权后直接放行，避免拒绝循环。
- 强制改密判断优先于普通页面授权判断。

## 权限边界

- 登录态只表示存在 store token，不代表拥有任一业务权限。
- 页面权限采用任一 permission/feature 满足；显式拒绝权限由权限工具优先处理。
- 前端守卫和禁用状态用于导航体验，后端认证拦截器与 `@RequirePermission` 才是接口边界。
- 法律页为公开内容；合规页脚也可从应用根部进入这些路由。

## 空态、错态与加载态

- Login 自行维护提交 loading，并在按钮内显示旋转图标和验证文案。
- 路由无 token 不显示错误页，直接带 redirect 返回登录。
- 页面权限失败通过 warning 加拒绝页呈现，不泄露目标页内容。
- 强制改密与加入组织各自维护表单反馈；统一请求层负责通用网络/会话错误。
- 法律页不依赖业务接口，内容类型由当前路由决定。

## 控件和样式现状

- 入口页主要使用原生 form/input/button、Tailwind 类和 scoped CSS。
- Login 包含本地 SVG/动效视觉、Material Symbols 和手写 loading 状态。
- NoPermission 使用简单页面动作，不是 Element Plus Result。
- LegalPage 使用共享站点配置和 RouterLink，隐私/条款复用同一布局。
- 认证页没有通过全量 Element Plus 插件获得组件，消息服务按需导入。

## Element Plus 接入/替换建议

- 登录、加入组织和改密表单可迁移 `ElForm`、`ElInput`、`ElButton`，保留现有 payload 与提交锁。
- 密码字段迁移时保留浏览器 autocomplete 语义、可见性切换和键盘提交。
- NoPermission 可使用 `ElResult`，但来源返回和平台租户规则不能改变。
- 表单错误优先落到字段级，再保留统一网络错误消息。
- 入口页视觉属于品牌表面，标准控件迁移不应移除现有首屏身份识别和响应式布局。

## 风险

- 仅以 Boolean token 判断登录；过期和失效必须依赖请求层响应清理，不能由路由静态判断。
- `developer` 与 `isPlatformTenant` 是两个不同概念，维护时不可把 developerOnly 误改为 `isDeveloper`。
- 旧会话无 features 时基础 module 默认可用，这是兼容策略，不是套餐授权事实。
- redirect 若在页面中自行解析，可能绕过 `normalizeLoginRedirect` 的现有约束。
- 改密标记同时存在内存和 sessionStorage，成功后必须调用 store 方法保持一致。
- localStorage 被主动清理，新增“记住登录”不能只改 Login 页面。

## 验证清单

- [ ] 未登录访问每个受保护页均回登录并保留安全 redirect。
- [ ] 已登录访问登录页、必须改密、改密完成三条分支无循环。
- [ ] 刷新后 sessionStorage 会话恢复，关闭会话后不从 localStorage 恢复。
- [ ] token 失效、续期成功、续期失败和手动退出均正确清理状态。
- [ ] 平台租户只能进入企业授权，普通租户不能进入 developerOnly 路由。
- [ ] permissions/features 为空、通配、显式拒绝和历史订单权限兼容均按工具函数执行。
- [ ] 登录、加入组织、改密、拒绝页和法律页在手机/桌面均可键盘操作。
