# 角色管理

## 源码/路由/批次

- 列表：`management-ui/src/views/function/role/role.vue`。
- 新建抽屉：`management-ui/src/views/function/role/createRoleDrawer.vue`。
- 权限抽屉：`management-ui/src/views/function/role/permissionDrawer.vue`。
- 前端 API：`management-ui/src/views/function/role/api/role.js`。
- 后端：`management/src/main/java/my/management/controller/RoleController.java`、`module/sys/service/RoleService.java`。
- 权限目录：`management/src/main/java/my/management/module/sys/model/enums/PermissionCodeEnum.java`。
- 路由：`/function/role`；入口权限 `role:list`；feature 为 `module.role`。
- 迁移批次：Batch 1；当前状态为 Element Plus migrated。

## 功能

- 一次加载最多 100 个角色，展示名称、系统/自定义类型和创建时间。
- 支持本地调整列表列顺序并恢复默认顺序。
- 新建角色时填写名称并从权限树选择权限。
- 配置现有角色权限时并行加载全量权限树和该角色已拥有的权限 ID。
- 保存后刷新角色列表；当前页面没有删除、停用或角色成员管理入口。

## API 表

| 包装器                  | 方法与路径                              | 后端权限               | 用途               |
| ----------------------- | --------------------------------------- | ---------------------- | ------------------ |
| `getRolePage`           | GET `/sys/role/page`                    | `role:list`            | 角色分页           |
| `getAllPermissions`     | GET `/sys/role/role/all`                | `role:permission:list` | 权限树             |
| `getRolePermissionIds`  | GET `/sys/role/{roleId}/permission-ids` | `role:permission:list` | 已分配权限回显     |
| `createRole`            | POST `/sys/role/create`                 | `role:create`          | 创建角色及权限集合 |
| `updateRolePermissions` | POST `/sys/role/role/update`            | `role:update`          | 覆盖角色权限       |

## 权限/feature

- `RoleController` 整体受 `module.role` 约束，和路由 feature 一致。
- 角色列表入口与列表接口均要求 `role:list`。
- “新建角色”按钮只检查 `role:create`，但抽屉加载权限树还需要 `role:permission:list`。
- “配置权限”按钮只检查 `role:update`，但打开抽屉还需要 `role:permission:list` 两次读取。
- 后端创建接口本身只标注 `role:create`，更新接口只标注 `role:update`；读取权限树是独立的 `role:permission:list`。
- 上述差异是现状证据，不在视觉迁移中合并权限或定义新权限码。

## 状态流

1. 页面挂载后请求固定页码 `page=1,size=100`，请求期间显示列表遮罩。
2. 新建抽屉打开时清空表单，再加载权限树。
3. 名称为空、权限树加载失败时阻止创建；提交时权限 ID 去重。
4. 配置抽屉打开时清空旧状态，并行加载权限树和已拥有 ID。
5. DOM 渲染完成后再写入选中 ID，避免树选择器只回显数字。
6. 保存权限时提交 `roleId + permissionIds`，成功后关闭抽屉并刷新列表。

## 空错态

- 列表使用 `v-loading`、loading 空占位、持久 `ElAlert` 和 `ElEmpty` 区分加载、请求失败与真实空态；失败时清空旧列表并提供重试。
- 新建抽屉区分权限树加载中、403 文案和一般加载失败。
- 配置抽屉按 request 的 HTTP `response.status` 或业务 `code`，将 401/403 权限不足与网络/5xx 加载失败显示为不同持久状态；请求失败状态可重试。
- 配置抽屉继续使用 `ElEmpty` 表示成功请求后的空权限树，不把错误降级为空态。
- 保存失败保持抽屉，按钮由提交状态禁用；取消应视为正常关闭。

## 控件现状

- 角色列表使用 `ElTable`、`ElTableColumn`、`ElPagination`、`ElButton`、`ElTag`、`ElAlert` 和 `ElEmpty`。
- 新建抽屉使用 `ElDrawer`、`ElForm`、`ElInput` 和 `ElButton`；权限抽屉使用 `ElDrawer` 和 `ElButton`。
- 权限选择使用 `ElTreeSelect`；配置错误态使用 `ElAlert`，空态使用 `ElEmpty`。
- 消息反馈使用 `ElMessage`。
- 列设置使用共享 `TableColumnSettings` 和 `useLocalTableColumns`。

## Element Plus 对照/保留项

- 列表已迁移为 `ElTable`、`ElPagination`，加载用 `v-loading`，错误用 `ElAlert`，空态用 `ElEmpty`。
- 角色名称使用 `ElInput`；命令使用 `ElButton`。
- 系统/自定义类型使用 `ElTag`，保持既有语义和文案。
- 继续使用 `ElDrawer`、`ElTreeSelect`、`ElMessage`，不重复封装类似组件。
- 权限树必须保持数字 ID、父子结构、过滤、折叠标签和勾选回显。
- 保留列顺序本地持久化；迁移表格时不得切断 `TableColumnSettings`。
- 保留显式导入 Element Plus 的方式，不改为全量 `app.use(ElementPlus)`。

## 风险

- 明确的权限依赖不一致：具有 `role:create` 或 `role:update` 但没有 `role:permission:list` 的用户会看到可用按钮，打开后却无法加载权限树。
- 反向情况下，只有 `role:permission:list` 不能进入页面，因为路由仍要求 `role:list`。
- 页面把后端树响应兼容性地拆多层 `data`；统一 request 返回形态后，这段兼容代码可能掩盖契约漂移。
- 列表分页控件当前只呈现固定 `page=1,size=100` 查询结果；角色数超过 100 时页面仍不完整。
- 系统角色和自定义角色都显示“配置权限”；迁移不得自行改变后端对内置角色的保护规则。
- 权限更新是整集合覆盖，两个管理员并发编辑时存在后提交覆盖先提交的风险。
- 权限 ID 类型若被 `ElSelect` 字符串化，会造成回显或保存差异。

## 验证

- [ ] 用四种权限组合验证 `role:list`、`role:create`、`role:update`、`role:permission:list`。
- [ ] 验证权限树 403、500、空树和慢请求，不把错误显示为“暂无权限”。
- [ ] 验证新建名称校验、ID 去重、成功刷新和失败保留表单。
- [ ] 验证配置抽屉的已有权限回显、取消、保存及关闭后的焦点恢复。
- [ ] 验证数字 ID、父子节点、通配权限和系统角色文案不变。
- [ ] 验证列顺序移动、重置、窄屏列表和 100 条边界。
- [ ] 运行目标测试、lint 和生产构建，确认五个 API 路径未改变。
