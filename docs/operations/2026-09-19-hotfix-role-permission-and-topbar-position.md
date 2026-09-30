# 2026-09-19 热修交接：角色权限保存 + 顶栏职位一致性 + 发布链路 CRLF

## 0. 交接摘要

| 项目 | 内容 |
| --- | --- |
| 分支 | `hotfix/role-permission-stale-binding` |
| 基线 | `53fcc2146313ae2075e3a97fc4d4255c8db70bcc`（与线上 2026-09-16 交付包同源） |
| 提交 | `b71ff45` 角色权限 · `a2bc7ab` 顶栏职位（前端） · `9d13c76` 职位（后端链路） · `614dd0a` 发布行尾 |
| 交付目录 | `C:\Users\HUAWEI\Desktop\hive全新部署`（已同步，`verify-release-integrity.sh` 与 `verify-upload-package.sh` 均通过） |
| 当前状态 | 代码、包、门禁完成；**线上发布待执行**（服务器若收到含 CRLF 的 `publish.sh`，先见 §5.3 一行命令） |
| 窗口归属 | 修复一来自"角色权限"窗口；修复二来自"顶栏职位/员工列表不一致"窗口；本窗口负责合并、隔离打包、发布链路修复 |

本次把两个窗口的改动统一到**同一个修复版本**，未掺入订单/售后/安装等在优化中的改动。

## 1. 变更清单（相对基线 `53fcc21`）

| 文件 | 增/删 | 归属 |
| --- | --- | --- |
| `management/src/main/java/my/hive/domain/permission/service/RoleService.java` | +8 −1 | 修复一 |
| `management/src/test/java/my/hive/domain/permission/service/RoleServicePermissionV3Test.java` | +51 | 修复一（回归用例） |
| `management-ui/src/views/function/role/permissionDrawer.vue` | +5 −1 | 修复一（前端提示） |
| `management/src/main/java/my/hive/domain/auth/mapper/AuthMapper.java` | +11 −11 | 修复二（后端） |
| `management/src/main/java/my/hive/domain/auth/model/vo/LoginVO.java` | +3 | 修复二（后端） |
| `management/src/main/java/my/hive/domain/auth/model/vo/LoginUserRow.java` | +2 | 修复二（后端） |
| `management/src/main/java/my/hive/domain/auth/service/AuthenticationService.java` | +8 | 修复二（后端） |
| `management/src/test/java/my/hive/domain/auth/service/AuthenticationServiceTest.java` | +29 | 修复二（服务用例） |
| `management-ui/src/layout/components/Navbar.vue` | +2 −1 | 修复二（前端） |
| `management-ui/src/stores/user.js` | +2 | 修复二（前端） |
| `management-ui/tests/navbar-user-position.test.js` | +30（新增） | 修复二（契约用例） |
| `.gitattributes` | +5 | 发布链路 |

## 2. 修复一：角色分配权限报"员工不存在"且保存失败

### 2.1 现象

系统配置 → 角色权限 → 给某角色勾选权限 → 点"确认分配"，先弹后端消息「员工不存在」，再弹「保存失败，请检查网络环境」，权限未保存。反馈中受影响角色为"销售负责人"（其他角色正常，说明是该角色下的个别脏数据触发）。

### 2.2 根因（已确认）

保存链路：`permissionDrawer.vue#save`（L184）→ `POST /sys/role/role/update` → `RoleService.updateRole`（L186）。写完 `sys_role_permission` 后调用 `evictRoleUsersPermissionCache`（L251）：查出该角色所有**有效**绑定的 `user_id`，逐个执行

```sql
UPDATE `user` SET permission_version = COALESCE(permission_version,1)+1 WHERE tenant_code = ? AND id = ?
```

原实现在影响行数 ≠ 1 时抛 `BusinessException(404,"员工不存在")`，而该方法处于 `@Transactional(rollbackFor = Exception.class)` 中 → **整次授权保存被回滚**；前端 catch 又用网络文案兜底，掩盖了真实原因。

触发条件：该角色名下存在**失效绑定**——`sys_user_role` 里状态有效的行，其 `(tenant_code, user_id)` 在 `user` 表中已无对应行（多为 `user.tenant_code` 与绑定的 `tenant_code` 不一致）。

### 2.3 为什么会有失效绑定（排查结论）

已证实：

- `sys_user_role` 无外键（baseline 全库 `FOREIGN KEY` 数量为 0），插入时不校验用户是否存在；
- 当前代码、全量 git 历史、全部迁移与部署脚本中**没有任何物理删除 `user` 行**的路径（员工删除只做：状态置离职 + 软删 `emp_employee_ext` + 软删本租户角色绑定）；
- 代码中唯一会在建号后改写 `user.tenant_code` 的路径是"组织邀请码自助加入"复用未加入账号的分支（候选 SQL 含 `tenant_code IS NULL OR tenant_code = ''`，命中后改租户并新增绑定）；
- 撤销绑定是租户维度的（`SysUserRoleMapper#markActiveRolesDeleted`，L26，`tenant_code + user_id`），一旦租户编码分叉，旧租户的绑定**再无任何代码路径可清理**，成为永久孤儿。

结论：失效绑定来自"租户编码单边变更或人工/迁移 SQL 造成的绑定与用户不一致"，不是正常业务流程必然产生的（本机开发库 18 条绑定全部匹配，0 条孤儿）。

### 2.4 改动

`RoleService.evictRoleUsersPermissionCache`：影响行数 ≠ 1 时**跳过并打 WARN**（`log.warn("角色权限变更时跳过失效的员工绑定: tenantCode={}, roleId={}, userId={}"...)`，L258），只对真实存在的员工轮换权限版本与清缓存，不再回滚整次授权。

`permissionDrawer.vue`：catch 只在**真正的网络异常**（无 `response` 且无业务 `code`）时提示"请检查网络环境"，业务/HTTP 错误交由 request 拦截器给出准确提示。

### 2.5 验证

- 新增回归用例 `keepsSavingPermissionsWhenRoleHasAStaleEmployeeBinding`：角色同时存在失效绑定（7L）与正常绑定（8L），断言保存不抛异常、只对 8L 清缓存、正常写入 `sys_role_permission`。
- 后端全量测试 **477 项 0 失败**（含该用例）。
- 单点反例（未修复时行为）：线上 JAR 的 `RoleService.class` 含 `员工不存在` 字面量，修复后不再含、改为含跳过告警文案（解压 class 校验）。

### 2.6 遗留

`EmployeeService#rotatePermissionVersion`（L924）是**同型写法**（`!= 1` 即抛 404），当前未触发但建议同口径收口，否则"编辑员工改角色"可能以同样方式失败。

## 3. 修复二：顶栏职位与员工管理列表不一致

### 3.1 现象与根因

顶栏职位显示固定文案（`Navbar.vue` 原为 `const roleLabel = computed(() => '运营管理')`），与员工管理列表中的员工档案 `user.position` 不一致。

**关键点：这不是纯前端问题。** 基线后端并未把职位返回给登录态，因此本修复必须成对上线：

| 层 | 改动 |
| --- | --- |
| 后端 | `AuthMapper` 11 处查询补 `u.position AS positionName`；`LoginVO`/`LoginUserRow` 暴露 `positionName`；`AuthenticationService#loginVO.setPositionName(...)`（L1146）+ `normalizePositionName`（L1163，空白归一为 null） |
| 前端 | `Navbar.vue` L375 改为 `userStore.userInfo?.positionName || '未设置职位'`；`stores/user.js` L61 / L105 在登录与会话刷新两处透传 `positionName` |
| 用例 | `AuthenticationServiceTest` 新增 2 例（有职位 / 未维护职位）；`navbar-user-position.test.js` 3 例静态契约 |

> 教训：本次曾误判"后端无需改动"并出过一次缺后端链路的包——若单独上线该前端改动，顶栏会对所有人显示"未设置职位"。**该修复的前后端必须同一版本发布。**

### 3.2 前端产物处理方式

线上 UI dist 是用"带未提交改动的工作树"构建的（订单看板、售后工单表单、安装状态等未提交前端内容自 2026-09-03 起就在线上，且对应后端在基线已支持、功能是通的）。为避免本次发布把这些内容下架，前端**没有重建 dist**，而是在线上 dist 上做**最小补丁**：

- 仅 `assets/index-2H6I1UnV.js` 一个文件：`roleLabel` 改读 `userInfo.positionName`（1 处）+ `setLoginInfo` / `refreshCurrentSession` 两处注入 `positionName`；
- 与线上 dist 逐文件比对：**仅该 1 个文件内容不同，无增删文件**；
- 补丁后 bundle 通过 ESM 语法解析；全 dist 已无 `运营管理` 残留。

代价：该前端产物**不能由源码复现**，已在 `RELEASE_BUILD_INFO.txt` 标注 `ManagementUiBuild=LIVE_DIST_MINIMAL_PATCH`。等订单/售后那批优化正式合入并重建 dist 后应重新收口。

## 4. 修复三：发布链路 `publish.sh` 行尾（本次线上发布阻断点）

### 4.1 现象

服务器 `bash publish.sh` 报：

```
: invalid option nameet: pipefail
```

### 4.2 根因

`deploy/publish.sh` 在 `.gitattributes` 中**缺少 `eol=lf` 声明**（只声明了 `deploy/scripts/*.sh` 与 `deploy/docker-compose.yml`），本机 `core.autocrlf=true`，Windows 检出时被转成 CRLF；交付包直接复制了该 CRLF 版本，服务器上 `set -euo pipefail\r` 被 bash 判为非法选项。交付目录扫描确认仅 3 个文件为 CRLF：`publish.sh`(40)、`.env.example`(204)、`nginx/conf.d/hive.conf`(77)。

### 4.3 修复

- 仓库：`.gitattributes` 追加 `deploy/publish.sh`、`deploy/.env.example`、`deploy/nginx/**` 的 `eol=lf`（提交 `614dd0a`，`git check-attr` 已验证生效），并把工作树副本归一化为 LF；
- 交付包：上述 3 个文件转为 LF（JAR 与前端 dist 未动，摘要不变）；`bash -n publish.sh` / `bash -n scripts/restart.sh` 通过；
- 服务器应急（已提供给现场）：`cd /root/hive && sed -i 's/\r$//' publish.sh nginx/conf.d/hive.conf .env.example && bash publish.sh`。

### 4.4 遗留建议

发布门禁目前不检查行尾，本次 CRLF 骗过了全部自动化校验。建议在 `scripts/verify-release-integrity.sh` 增加一条：任何 `.sh` / `.conf` / `.yml` / `.env.example` 含 CRLF 直接 FAIL。

## 5. 统一交付版本

### 5.1 产物摘要（发布前核对）

```
BackendJarSha256=eaedbfba99a7d3d35a2c7bfd76d331edb21806f4a10c04807a3c4ae70793740e
BackendJarBytes=103579694
ManagementUiSha256=02ac8f03ff24bdd119764f273d12dc5e912827479acd11086128953d77582a1e
ManagementUiIndexSha256=8609c70bed6c5a7aecfd15f8345b7c6c451064939719629fa8dff7fa66bfd3a0
ManagementUiFileCount=101
MigrationManifestSha256=2bdeecddddd7491ead94ad7be6bd3611aa0186f56d9b70a9a92e2ab9d37edbb3
MigrationChecksumsSha256=bd2fee7537a6accac1d8bf10f50fed306a87a7a697f05b21d37672ab86dc2bab
MigrationCount=108
```

### 5.2 产物级差异（对线上 2026-09-16 包）

- **后端 JAR**：条目数 838 vs 838，**无增删**；内容差异 = 5 个类（`RoleService`、`AuthenticationService`、`LoginVO`、`LoginUserRow`、`AuthMapper`）+ 3 个 `application*.yaml`（仅 CRLF/LF 行尾，文本一致）+ `META-INF/build-info.properties`（构建时间戳）。
- **前端 dist**：仅 `assets/index-2H6I1UnV.js` 不同（见 §3.2）。
- **迁移**：`db-migrations` 未改动（摘要与线上一致）。

### 5.3 门禁结果

| 检查 | 结果 |
| --- | --- |
| 后端全量测试 + 打包（`mvn clean package`） | PASS（477 项，0 失败） |
| 职位契约测试（`node --test tests/navbar-user-position.test.js`） | PASS（3/3） |
| `scripts/verify-release-integrity.sh` | PASS |
| `scripts/verify-upload-package.sh` | PASS |
| `bash -n publish.sh` / `scripts/restart.sh` | PASS |
| 真实浏览器端到端 / 生产现场验收 | **未执行**（需发布后在页面点验，见 §6.2） |

> 构建坑：从其它工作树复制源码后，若文件时间戳早于已有 class，Maven 增量编译会**跳过**这些文件（本次曾导致 JAR 缺 auth 类、新用例未运行且总数不变）。跨工作树复制源码后必须 `mvn clean package`，并以"产物类差异"复核，不能只看测试总数。

## 6. 上线与验收

### 6.1 上线

```bash
# 上传交付目录内容覆盖 /root/hive 后
cd /root/hive
sed -i 's/\r$//' publish.sh nginx/conf.d/hive.conf .env.example   # 若本次包已修正可跳过
bash publish.sh
```

### 6.2 发布后必须人工验证

1. **角色权限**：给"销售负责人"分配"下载文件"→ 保存成功；日志出现 `角色权限变更时跳过失效的员工绑定: tenantCode=…, roleId=…, userId=…` 表示失效绑定被正确跳过（本次**不清理**脏数据，先观察日志）。
2. **顶栏职位**：顶栏职位与该员工在"员工管理"中的职位一致；未维护职位时显示"未设置职位"。
3. 记录被跳过的 `userId`，用下面 SQL 确认属于哪一类失效（只读）：

```sql
SELECT ur.user_id, ur.tenant_code AS binding_tenant, r.role_name, ur.create_time AS binding_created,
       u.id AS user_exists, u.tenant_code AS user_tenant, u.name, u.login_name,
       u.create_time AS user_created, u.update_time AS user_updated
FROM sys_user_role ur
LEFT JOIN `user` u ON u.id = ur.user_id
LEFT JOIN sys_role r ON r.id = ur.role_id
WHERE IFNULL(ur.is_deleted,0) = 0
  AND (u.id IS NULL OR u.tenant_code IS NULL OR u.tenant_code = '' OR u.tenant_code <> ur.tenant_code);
```

判读：`user_exists IS NULL` → 用户行确已被清理（仅可能来自人工/历史操作）；`user_tenant` 非空且 ≠ `binding_tenant` → 账号被迁移过、旧绑定残留（最符合当前代码能力）；`user_tenant` 为空 → 未加入组织的账号持有绑定。

### 6.3 回滚

| 对象 | 位置 | 摘要 |
| --- | --- | --- |
| 线上原版后端 | `D:\Hive-rollback-pre-role-permission-fix\backend\hive-backend.jar` | `ad95ba6b36753479387cabed2530003b92dce5f965720a1bf52f4da05157b8a9` |
| 线上原版前端 | `D:\Hive-rollback-pre-role-permission-fix\management-ui\` | dist 树 `352d9cd882a0db5c3347a40d8730520e095d2095683a819b6c270765a07d9b90` |
| 统一包副本 | `D:\Hive-package-merged-20260919` | 与交付目录一致，可快速回同步 |

前端如需单独回滚，恢复原版 `management-ui/dist` + `dist-manifest.sha256` 即可（后端修复独立，不需要回滚）。

## 7. 未纳入本次发布的内容与风险

1. **线上前端含未提交内容**：订单看板卡片、售后工单表单字段、安装状态、故障图片上传等未提交前端内容已在线上（09-03 起）。本次不动它们；若将来用干净基线重建 dist，会把这些内容一并下架。
2. **脏绑定未清理**：按现场决定先观察日志，不执行数据清理。
3. **交付目录存在并发写入风险**：本次发现有另一个窗口在 11:21 向同一交付目录打过包（该包 JAR 含 1161 条目，即把未提交后端一起打进去了，且元数据声称"仅 RoleService 一个类差异"与实际不符）。**发布前务必核对 §5.1 摘要**，并确保同一时间只有一个窗口在写该目录。
4. **员工侧同型隐患**：见 §2.6。
5. **前端补丁产物不可源码复现**：见 §3.2。

## 8. 待办建议

- [ ] 在发布门禁加入 CRLF 检查（§4.4）。
- [ ] 角色-员工绑定一致性巡检（把 §6.2 的 SQL 做成定时告警，非零即告警），并决定孤儿绑定的清理策略。
- [ ] `EmployeeService#rotatePermissionVersion` 与角色侧同口径收口（跳过 + 告警）。
- [ ] 优化分支合入后重建一次前端 dist，取消 `LIVE_DIST_MINIMAL_PATCH`。
- [ ] 发布后在页面完成 §6.2 的人工验收并回填本文件。

## 9. 关键代码锚点

| 关注点 | 位置 |
| --- | --- |
| 角色权限更新事务 | `RoleService.java` L186（`updateRole`） |
| 失效绑定跳过（本次修复） | `RoleService.java` L251–L263（WARN 在 L258） |
| 有效绑定查询 | `SysUserRoleMapper.java` L26（`selectUserIdsByRoleId`） |
| 权限版本轮换 SQL | `EmployeeMapper.java` L51–L59（`incrementPermissionVersion`） |
| 同型隐患（未改） | `EmployeeService.java` L924（`rotatePermissionVersion`） |
| 绑定撤销（租户维度） | `SysUserRoleMapper.java` L26（`markActiveRolesDeleted`） |
| 前端保存与提示 | `permissionDrawer.vue` L184（`save`） |
| 顶栏职位 | `Navbar.vue` L375；`stores/user.js` L61 / L105 |
| 职位后端链路 | `AuthenticationService.java` L1146 / L1163；`AuthMapper.java` 11 处 `AS positionName` |
| 行尾声明 | `.gitattributes` L7–L9 |
| 表结构（无外键） | `db-migrations/baseline/hive_schema_baseline_v2.sql`（`sys_user_role` 仅主键与 `idx_user_tenant`；全库 `FOREIGN KEY` 0 条） |

## 10. 涉及模块文档

- `docs/management-ui/modules/role.md`：已补"2026-09-19 权限保存失效绑定"小节。
- `docs/management-ui/modules/layout-navigation.md`：已补"2026-09-19 顶栏职位同源"小节。
- 建议同步：`docs/management-ui/modules/authentication.md`（登录返回 `positionName` 的对外契约）。
