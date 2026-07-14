# Hive 单一后端合并设计

## 1. 目标与边界

Hive 的管理网页和微信小程序最终由同一个 Spring Boot 进程提供服务，只构建、发布和运行一个可执行 JAR。所有业务接口统一使用 `/api/**`，不保留 `/web/**`、旧 Token、旧缓存、旧权限码或历史接口兼容逻辑。正式上线前会清空业务数据，但历史版本化 SQL 仍保持不可修改，只能追加新迁移文件。

主仓库为 `D:\HiveManager`，目标分支为 `codex/unify-hive-backend`。`D:\HiveBackend\server` 和 `D:\HiveCommon\hive-backend-common` 仅作为合并来源；最终业务代码、公共基础能力、数据库迁移、部署源文件和文档全部归入主仓库。部署产物同步到 `C:\Users\HUAWEI\Desktop\hive部署_全新配置`。

## 2. 当前状态

### 2.1 代码库

| 范围 | 规模与状态 | 当前职责 | 主要问题 |
|---|---|---|---|
| 管理端后端 | 387 个 Java 文件、22 个 Controller、37 个 Service、55 个 Mapper | 管理端、员工、权限、订单、审批、通知、维护 | 使用 `/web`，与小程序大量重复 |
| 小程序后端 | 254 个 Java 文件、14 个 Controller、32 个 Service、40 个 Mapper | 小程序、微信、订单流转、考勤、统计 | 使用 `/api`，独立 Token、配置和任务 |
| 通用模块 | 51 个 Java 文件 | Token、租户、权限 AOP、操作日志、打印等 | 独立版本发布，且两端仍复制部分公共实现 |
| 部署目录 | 两个业务 JAR、两个 Dockerfile、两个业务容器 | 线上部署、迁移和运维脚本 | 配置、日志、健康检查和发布校验重复 |

管理端当前使用端口 `8081` 和 context path `/web`；小程序使用端口 `8080` 和 context path `/api`。管理端前端的默认 `baseURL` 是 `/web`。

主仓库在审计时存在 Permission Catalog V3、Controller、Service、前端测试和操作文档等未提交改动。这些改动属于现有工作成果，合并过程中必须保留，不得重置、覆盖或混入不相关提交。

### 2.2 部署与迁移

Docker Compose 当前运行两个业务服务：

- `backend-1` / `hive-mini-backend-1`
- `management-backend-1` / `hive-management-backend-1`

两者分别拥有 Dockerfile、JAR、日志目录、XXL-JOB executor 配置、应用名、端口和环境变量。启动、重启、健康检查、低成本运行检查、冒烟测试、发布完整性检查和构建信息均假设双后端存在。

正式版本化迁移体系目前仅存在于部署目录，入口为 `scripts/migrate-db.sh`，下层使用 `db-migrations/migration_manifest.txt`、`migrations/V*.sql`、preflight、备份、校验及漂移诊断脚本。两个应用的 `src/main/resources/sql` 中还存在散落和重复 SQL，不能继续作为独立迁移入口。

## 3. 重复模块矩阵

| 领域 | 管理端能力 | 小程序能力 | 目标唯一实现 |
|---|---|---|---|
| 认证 | 管理员登录、密码重置、加入组织、扫码登录 | 账号登录、微信登录、当前用户 | 独立入口，共用 Token、会话、租户、权限和用户状态解析 |
| 审批 | 审批中心、审核人、订单和质量审批 | 请假、财务、离职、订单和质量审批 | 一个审批领域服务和统一审批事务模型 |
| 订单 | 订单维护、设置、状态管理 | 销售/生产订单、状态流转和流程打印 | 一个订单聚合服务和状态机 |
| 库存 | 查询、出入库、预警、趋势 | 出入库、识别、预警和打印 | 一个库存服务和一套 Mapper |
| 质量 | 不良品处理与审核 | 不良品处理与质量审批 | 一个质量服务 |
| 安装任务 | 管理端主实现 | 小程序同步能力 | 一个安装任务服务 |
| 员工、角色、权限 | 完整员工和角色管理、Permission Catalog V3 | 用户与部分角色实体 | 管理端 Permission Catalog V3 为唯一权限目录 |
| 客户 | CRUD、详情、选项 | CRUD、详情 | 一个客户服务 |
| 文档 | 文件夹、上传、移动、重命名 | 基本相同 | 一个文档服务 |
| 设备 | 维护、禁用、巡检 | 查询、扫码、巡检 | 一个设备服务 |
| 标签与打印 | 标签模板、回执打印 | 标签模板、流程打印 | 一个模板与打印任务领域 |
| 通知 | 通知闭环、短信、公告 | 公告、微信订阅消息 | 一个通知领域，短信和微信为渠道适配器 |
| 考勤 | 规则、查询、统计 | 打卡、个人记录、统计 | 一个考勤领域服务 |
| 租户 | 平台租户管理 | 当前租户能力 | 共用租户上下文和仓储 |
| 任务与消息 | 维护、通知闭环 | 考勤和库存统计 | 一个 XXL-JOB executor，每个 handler 和消费者只注册一次 |

已识别的默认 Bean 名冲突包括 Controller、Service、Mapper 和配置类，例如 `approvalController`、`authController`、`orderController`、`inventoryController`、`authService`、`orderService`、`inventoryService`、`mybatisPlusConfig`、`webMvcConfig`、`xxlJobConfig` 和 `ossStorageService`。因此禁止通过扩大组件扫描范围直接并装两个现有包树，也禁止靠重命名 Bean 长期保留两套业务实现。

## 4. 方案选择

### 4.1 采用：按领域逐个收敛到主仓库

以管理端代码为骨架，逐领域比较两个实现。每个领域先固化接口和行为测试，再选择一个主实现，吸收另一端独有能力，最后删除重复 Controller、Service、Mapper、DTO、实体、配置和任务。

该方案能真正满足“一份业务逻辑”，并允许订单、审批、库存等高风险领域分模块验证。

### 4.2 不采用：整体引入小程序包后逐步清理

该方案需要排除扫描、Bean 改名或临时适配层才能启动，会阶段性保留两套 Service 和权限逻辑，容易把临时结构变成长期负担。

### 4.3 不采用：两个内部模块加一个聚合启动器

该方案只能实现一个 JAR，不能消除两个业务版本，仍不满足相同业务调用同一 Service 的要求。

## 5. 目标架构

### 5.1 包结构

```text
my.hive
├── HiveApplication
├── api
│   ├── auth
│   ├── approval
│   ├── attendance
│   ├── customer
│   ├── document
│   ├── employee
│   ├── equipment
│   ├── installation
│   ├── inventory
│   ├── notification
│   ├── order
│   ├── permission
│   ├── print
│   ├── quality
│   ├── tenant
│   └── wechat
├── domain
│   └── <domain>/{service,model,repository}
├── infrastructure
│   ├── persistence
│   ├── messaging
│   ├── scheduler
│   ├── storage
│   ├── wechat
│   └── printing
└── shared
    ├── auth
    ├── permission
    ├── tenant
    ├── web
    ├── exception
    └── config
```

Controller 只处理 HTTP 契约、输入校验和输出转换。管理网页与小程序对相同业务的调用必须进入同一个领域 Service。客户端差异只允许存在于认证入口、请求/响应适配和渠道适配器中。

### 5.2 HTTP 路径

应用唯一 context path 为 `/api`。不提供 `/web` 兼容映射。

认证路径固定为：

- 管理端登录：`/api/auth/admin/login`
- 管理端扫码登录：`/api/auth/admin/scan-login/**`
- 小程序账号登录：`/api/auth/mini/login`
- 小程序微信登录：`/api/auth/mini/wechat-login`
- 共用当前用户：`/api/auth/me`
- 共用退出：`/api/auth/logout`

其他领域不按客户端分叉，统一使用 `/api/orders/**`、`/api/approval/**`、`/api/inventory/**`、`/api/employee/**` 等领域路径。现有 `/order` 与 `/orders`、`/notification` 与 `/notifications` 等命名差异在接口清单中明确收敛，调用端同步修改，不保留旧路径。

### 5.3 认证、租户和权限

所有登录入口最终签发同一种 Token。Token 只包含统一会话标识及必要声明；Token 校验、会话版本、用户状态、租户解析、权限加载和上下文清理只保留一个实现。

Permission Catalog V3 是唯一权限目录：

- 权限判断只接受 V3 精确权限码。
- 不允许通配符、旧码映射、别名、前缀回退或历史库兼容逻辑。
- 角色权限、员工覆盖权限、Controller 注解和前端权限常量必须由同一目录校验。
- 管理员也通过明确的 V3 权限集合授权；除设计明确的系统平台账号外，不使用隐式全权限绕过。

请求进入后统一执行 Token 校验、用户状态校验、租户上下文建立和权限解析，请求结束时统一清理上下文。公开认证路径只在一个 Web 配置中声明。

## 6. 数据与迁移设计

主仓库保存唯一 `db-migrations` 目录和唯一 `scripts/migrate-db.sh` 入口。部署目录的迁移内容由主仓库发布流程同步，不再反向手工维护。

规则如下：

1. 已进入 manifest 或执行历史的 `V*.sql` 永不修改、重命名或删除。
2. 合并所需的 schema、索引、权限目录和调度配置只通过新版本文件实现。
3. 新版本文件同时追加到 `migration_manifest.txt`，并更新校验脚本与 `RELEASE_BUILD_INFO.txt` 的 manifest 哈希。
4. 上线允许清空业务数据，但 schema baseline 与版本化历史仍需完整验证。
5. 应用 `resources/sql` 不作为迁移入口；确认内容已纳入版本化体系后删除重复文件。

## 7. 运行与部署设计

目标 Compose 只保留一个 Hive 业务服务：

```text
nginx /api/** -> hive-backend:8080
                    ├── management APIs
                    ├── mini-program APIs
                    ├── one auth/tenant/permission stack
                    ├── one scheduler executor
                    └── one message-consumer set
```

业务容器统一命名为 `hive-backend`，只挂载一个日志目录和一个上传目录。删除 `backend-1`、`management-backend-1`、mini-backend 日志目录、两个 executor 端口以及脚本中的双容器假设。

统一环境变量包含数据库、Redis、RabbitMQ、Token、租户、微信、短信、OSS、XXL-JOB、维护任务和日志配置。相同含义的变量只保留一个名称；客户端专属变量只保留在对应适配器配置下。

nginx 只转发 `/api/**` 到唯一容器。健康检查、启动、重启、低成本检查、备份、冒烟、完整性校验和发布快照脚本全部以一个业务容器和一个 JAR 为准。

## 8. 合并顺序

1. 固化当前工作区和分支基线，建立不可覆盖清单。
2. 建立统一启动类、单一 `/api` context path、配置和测试门禁。
3. 将通用模块源码归入主仓库，统一 Token、租户、权限和异常处理。
4. 收敛 Permission Catalog V3、员工、角色和权限数据访问。
5. 合并管理端与小程序认证入口。
6. 合并订单和审批领域。
7. 合并库存、质量和安装任务领域。
8. 合并客户、文档、设备、标签和打印领域。
9. 合并通知、公告、短信和微信订阅渠道。
10. 合并考勤、统计、维护任务、XXL-JOB 和消息消费。
11. 删除重复 DTO、Mapper、实体、Bean、SQL、配置和旧包树。
12. 迁入并验证唯一版本化数据库迁移入口。
13. 修改管理端前端及相关调用端到新的 `/api` 契约。
14. 改造 nginx、Compose、环境变量、健康检查、重启和冒烟脚本。
15. 构建唯一 JAR，更新部署目录和 `RELEASE_BUILD_INFO.txt`，完成发布、回滚和验证演练。

每完成一个领域模块，都同步更新架构文档、接口清单、迁移说明和部署文档，不在最后集中补写。

## 9. 测试与验收

每个领域先建立管理端与小程序共用 Service 的契约测试，再删除重复实现。整体门禁包括：

- Spring context 启动测试，检测 Bean 名、HTTP mapping、Mapper 和配置冲突。
- Permission Catalog V3 静态与运行时测试，拒绝旧码、通配符和别名。
- 认证集成测试，覆盖四类入口及共用 Token/租户/状态解析。
- 订单、审批、库存、质量和安装任务的事务与状态流转测试。
- XXL-JOB handler、RabbitMQ listener 和打印接口唯一性测试。
- 版本化迁移 manifest、历史文件校验和及 shadow schema 验证。
- 唯一可执行 JAR 的构建和启动测试。
- 同一进程响应管理端、小程序和健康检查接口的冒烟测试。
- Compose 配置检查，确保只有一个 Hive 业务容器。
- nginx、启动、重启、回滚、备份和发布完整性脚本验证。

验收时不得存在两个 Spring Boot 启动类、两个后端 JAR、两个业务容器、两个同领域 Service、重复权限常量或旧 `/web` 路由。

## 10. 发布与回滚原则

发布前生成数据库备份、旧双后端部署快照、唯一 JAR 哈希、迁移 manifest 哈希和前端构建哈希。停止双后端后执行新增迁移，启动单后端，再按管理登录、小程序登录、订单、审批、库存、通知、打印和任务顺序验证。

回滚以部署快照和数据库备份为边界。若新增迁移不可逆或已写入新格式数据，回滚必须恢复发布前数据库备份，不执行手写降级 SQL。由于本次不兼容旧 Token 和旧缓存，发布与回滚后均清理相关 Redis namespace，并要求用户重新登录。

## 11. 风险与控制

| 风险 | 控制措施 |
|---|---|
| 当前未提交 Permission V3 改动被覆盖 | 建立独立目标分支，只提交明确文件，禁止 reset 和 checkout 覆盖 |
| `/api` 下 HTTP mapping 冲突 | 在引入 Controller 前生成并校验完整 mapping 清单 |
| 同名实体字段相同但语义不同 | 按数据库表、业务约束和调用行为比较，不按文件名机械覆盖 |
| 订单、审批、库存事务差异 | 先写状态机和事务集成测试，再收敛实现 |
| Token 默认密钥和有效期不同 | 使用唯一强制环境变量，旧 Token 全部失效 |
| Permission V3 仍在演进 | 以主仓库目录、迁移和契约测试为唯一来源 |
| XXL-JOB 重复 handler 或 executor | 只保留一个 executor appName、端口和 handler 注册表 |
| RabbitMQ 重复消费者 | 增加 listener 唯一性测试并只加载一套配置 |
| `PrintTaskController` 再次冲突 | 打印入口集中在 `api.print`，启动测试校验唯一 mapping |
| 部署目录再次漂移 | 主仓库作为部署源，发布脚本单向同步并校验哈希 |
| 历史迁移被误改 | 保留并扩展历史 SHA-256 校验门禁 |

