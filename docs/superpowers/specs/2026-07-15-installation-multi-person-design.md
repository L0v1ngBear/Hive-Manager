# 安装任务支持多个安装人员设计

## 目标与范围

本改造仅处理统一后端中的安装任务，不涉及双后端合并。一个安装任务可保存 0 至 20 名自由文本安装人员，每名人员由姓名和联系电话组成。所有公开接口继续位于现有 `/api/**` 上下文中，不增加 `/web/**` 路由或旧字段兼容。

旧的 `constructionPersonnel`、`constructionPhone` 业务契约和数据库列直接删除，不迁移、不兼容旧数据。`constructionRemark` 等其余安装任务字段保持不变。

## 数据模型与迁移

新增 `installation_task_installer` 表：

- `id`：自增主键。
- `tenant_code`：租户编码，所有读写的强制隔离条件。
- `installation_task_id`：所属安装任务 ID。
- `installer_name`：安装人员姓名，最长 50 个字符。
- `installer_phone`：联系电话，最长 40 个字符，不限定手机号格式，允许座机和分机。
- `sort_order`：请求中的零基顺序，用于稳定回显。
- `create_time`、`update_time`：创建和更新时间。

添加 `(tenant_code, installation_task_id)` 查询索引，以及 `(tenant_code, installation_task_id, sort_order)` 唯一约束。业务层额外阻止同一任务内 trim 后完全相同的“姓名 + 电话”组合；不同姓名可以共用同一电话。

只新增更高版本的迁移文件。该迁移创建明细表，并从 `installation_task` 删除 `construction_personnel` 和 `construction_phone`。不得修改任何历史迁移。新版本必须追加到 `db-migrations/migration_manifest.txt` 和完整 SHA-256 快照，并同步到部署包。

## 后端契约与模型

`InstallationTaskStatusUpdateRequest` 使用：

```json
{
  "installers": [
    { "name": "张三", "phone": "010-12345678 转 801" }
  ]
}
```

`installers` 缺失或为 `null` 按空列表处理。只要存在人员行，姓名和电话都必须在 trim 后非空。姓名最长 50 字符、电话最长 40 字符，最多 20 行。

`InstallationTaskVO` 返回：

```json
{
  "installers": [
    { "id": 1, "name": "张三", "phone": "010-12345678 转 801", "sortOrder": 0 }
  ]
}
```

新增聚焦于请求项和响应项的模型，以及 `InstallationTaskInstaller` Entity、`InstallationTaskInstallerMapper`。继续由唯一的 `InstallationTaskService` 编排，不增加重复领域 Service。旧单人字段从 DTO、VO、Entity、Service、前端和文档中删除。

## 保存流程与事务

`InstallationTaskService.updateStatus()` 保持 `@Transactional(rollbackFor = Exception.class)`，并按以下顺序执行：

1. 使用当前 `tenant_code` 和任务 ID 查询任务，确认归属当前租户。
2. 将人员姓名和电话 trim，校验数量、完整性、长度和重复组合。
3. 校验状态；`completed_accepted` 至少要求一名完整人员，其他状态允许空列表。
4. 更新任务状态和其余可编辑字段。
5. 使用 `tenant_code + installation_task_id` 删除原有人员明细。
6. 按请求顺序批量插入全部新明细，生成连续 `sort_order`。
7. 使用刚保存的任务和人员构造响应。

任务更新、删除旧明细或插入新明细任一步失败均回滚。所有人员删除、查询和写入都显式包含 `tenant_code`。

## 分页查询

任务分页继续使用现有 `InstallationTaskMapper.selectPage()`。获得当前页任务后，服务通过一条 `tenant_code = ? AND installation_task_id IN (...)` 查询批量加载当前页所有人员，按 `installation_task_id, sort_order` 排序，然后在内存中分组装配 VO。空页不发起人员查询。

该方式固定为每次分页最多一条任务分页查询和一条人员明细查询，避免逐任务 N+1，也避免 JOIN 一对多破坏分页边界。

## 前端交互

`installationTask.vue` 将旧的单人姓名和电话输入替换为动态列表：

- 每行包含姓名、联系电话和删除按钮。
- 提供“添加安装人员”按钮，达到 20 行后禁用或拒绝新增。
- 保存前校验每行两项必填、长度、完全重复组合和完成状态至少一人。
- 编辑时从 `row.installers` 深拷贝回填，保存请求只包含 `installers[]`，不发送旧字段。
- 列表默认展示前三人姓名和电话；超过三人显示“另有 N 人”，并提供查看全部人员的交互。

人员列表规范化、校验、回填和请求组装逻辑放入同目录的纯 JavaScript 模块，以便 Node 测试覆盖新增、删除、回填和提交契约；Vue 组件负责渲染与 Element Plus 提示。

## 错误处理

非法人员数据由后端统一抛出 `BusinessException`，前端做同等即时校验但不作为安全边界。任务不存在或属于其他租户时返回统一的任务不存在业务错误，避免泄漏跨租户数据。数据库约束异常由事务回滚，不能留下任务已更新但人员列表未完整替换的状态。

## 测试与交付验证

严格按失败测试先行覆盖：

- 0、1、多人保存和回显。
- 姓名缺失、电话缺失、长度超限、完全重复、超过 20 人。
- `completed_accepted` 无人员失败，未完成状态空列表成功。
- 人员查询、删除和写入的租户隔离。
- 明细替换失败导致整个事务回滚。
- 分页只批量查询一次人员明细，空页不查询。
- 前端新增、删除、上限、回填、校验和 `installers[]` 请求。
- 业务代码不存在 `constructionPersonnel`、`constructionPhone` 契约。
- 历史迁移哈希不变，新迁移与 manifest、checksum 一致。

最终运行后端 Maven 全量测试和唯一 JAR 构建、`management-ui` 全量测试和构建、迁移完整性检查，并同步部署目录的新迁移、校验文件、文档、JAR 和构建信息。提交并推送 `codex/installation-multi-person`，报告最终 commit ID、迁移版本和合入 `dev` 的命令。
