# Hive P0 发布收敛设计

## 目标

将当前统一后端项目恢复为一个可由 Git 提交完整重建、可验证、可发布的单一版本，消除以下 P0：

- 部署数据库已经采用多人安装模型，但当前源码仍使用旧单人字段；
- 管理端部署产物仍引用 Permission V3 已废弃权限码；
- 发布元数据引用无法从 Git 获取的整合提交，JAR、UI、迁移和源码无法相互证明；
- 测试依赖桌面部署目录，导致源码缺迁移时仍可能显示通过。

本次不回退任何已执行迁移，不恢复双后端，不兼容旧单人安装字段，不处理已明确延期的取消原因功能。

## 唯一源码基线

在当前 `D:/HiveManager` 工作区直接使用分支 `codex/fix-p0-release-convergence`，保留现有未提交改动，不创建 worktree。

多人安装功能以远端提交 `a9390ead6223a50798af4276d0781e1f37ab7684` 为参考，只移植该提交自身的 27 个功能文件差异。不得整体合并它所基于的旧分支，避免回退后续订单备注、开票预警、权限和主题修改。

最终必须产生一个已推送的 Git 提交。发布元数据中的 `SourceGitCommit`、`ReleasePackageGitCommit` 和 `ManagementUiSourceGitCommit` 必须指向可从远端获取的提交，不允许记录临时工作区或已经丢失的提交。

## 数据库与安装任务

部署包已有的两条迁移必须回归主仓库并成为唯一事实来源：

- `V20260715_001_order_notes_and_material_approval.sql`
- `V20260715_002_installation_task_installer.sql`

迁移必须进入仓库 `db-migrations/migration_manifest.txt` 和 `migration_checksums.sha256`。部署目录只能从仓库迁移树同步，不允许反向成为测试事实来源。

安装任务继续采用 `installation_task_installer` 明细表和 `installers[]` 契约。旧 `construction_personnel`、`construction_phone` 列及前后端字段彻底删除。状态为 `completed_accepted` 时至少填写一名安装人员；其他状态允许空列表。所有查询、删除和插入必须带 `tenant_code`，批量查询避免 N+1，任务更新和人员替换处于同一事务。

## 权限收敛

后端 `PermissionCatalogV3` 是权限码唯一事实来源。管理端源码、构建产物和小程序现有页面不得引用已退休权限，包括 `customer:page`、`table:export`、`approval:order:audit` 等。

管理端必须使用当前精确权限：

- 客户入口和列表使用 `customer:list`，导出使用 `customer:export`；
- 订单导出使用订单模块的有效权限，不依赖 `table:export`；
- 审批中心入口使用 `approval:list`，订单审核动作分别使用 `order:audit:material`、`order:audit:shipment`、`order:audit:cancel`；
- 库存详情使用 `inventory:detail`，列表、预警、记录、趋势、入库、出库和导入导出保持独立权限。

权限测试必须扫描所有形如 `root:action` 的前端权限字面量，不再通过根节点白名单跳过未知权限根。构建后的 `dist` 还要执行相同的退休权限扫描。

## 发布门禁与秘密隔离

`verify-release-integrity.sh` 必须校验：

- 唯一后端 JAR 的 SHA-256；
- 管理端 `dist` 的目录哈希和文件数；
- 迁移 manifest 哈希、迁移数量和 checksums 文件；
- 发布元数据中的提交在当前 Git 仓库可解析；
- 构建产物不存在退休权限码和旧安装字段。

版本控制部署模板不得包含真实 `.env`、TLS 私钥、线上证书或运行时数据。服务器运行目录可以保留这些文件，但发布同步必须显式排除它们。健康检查验证服务器证书存在和有效，发布包清洁检查验证待上传文件集合不携带证书或秘密，两类检查不能混为一体。

## 测试策略

先新增会失败的门禁测试，证明当前问题可被捕获：

1. 仓库迁移必须包含 `_001`、`_002`，且测试不得读取绝对桌面路径；
2. 当前安装任务源码不得包含旧单人字段；
3. 前端源码与构建产物不得出现退休权限码；
4. 发布完整性脚本必须验证 UI、迁移和 Git 提交，而不只验证 JAR；
5. 发布清洁测试必须拒绝 `.env`、`.key`、线上证书和运行时数据进入待上传集合。

然后移植多人安装实现、迁移和当前权限修复，使定向测试转绿，再运行：

- 后端 Maven 全量测试与唯一 JAR 打包；
- 管理端 Node 全量测试与 Vite 生产构建；
- 小程序现有契约和 JavaScript 语法检查；
- 迁移 manifest/checksum 一致性验证；
- 发布包静态健康、完整性和退休契约扫描。

## 发布产物

部署目录 `C:/Users/HUAWEI/Desktop/hive部署_全新配置` 在所有源码测试通过后，从修复分支重新装配：

- `backend/hive-backend.jar`；
- `management-ui/dist`；
- 完整 `db-migrations`；
- 部署脚本、Nginx 配置模板和文档；
- 新的 `RELEASE_BUILD_INFO.txt`。

真实 `.env` 和服务器证书不复制、不删除、不覆盖。最终报告 Git 分支、提交、JAR/UI/manifest 哈希、测试数量和服务器执行命令。

## 验收标准

- 当前 Git 仓库可从零构建与部署包相同的 JAR、UI 和迁移集合；
- `installation_task` 不再读写已删除的单人字段，统一使用 `installers[]`；
- 源码和 `dist` 均不存在退休权限码；
- 所有发布元数据提交都可从远端 Git 获取；
- 完整性门禁篡改 JAR、UI 或迁移任一项都会失败；
- 真实秘密和 TLS 私钥不进入待上传发布集合；
- 全量测试、生产构建和发布静态门禁全部通过后才允许覆盖部署目录。
