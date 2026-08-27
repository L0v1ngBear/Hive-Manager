# 本地开发启动

## 前置条件

- JDK 21：本机可使用 `D:\idea\jdk-21.0.5`。在 IDEA 的 Project Structure 中将项目 SDK 设为该 JDK。
- MySQL 8 已启动，地址 `127.0.0.1:3306`；开发库为 `hive`。
- Redis 已启动，地址 `127.0.0.1:6379`，开发配置使用数据库 `4`。
- Node.js 20.19+ 或 22.12+。当前项目依赖已安装在 `management-ui/node_modules`。

本地配置在 `management/src/main/resources/application-dev.yaml`，仅用于开发环境：数据库账号为 `root`，密码为 `123456`。不要把本地 `.env` 或任何生产密钥提交到代码库。

## 启动后端（IDEA）

1. 在 IDEA 打开 `D:\HiveManager`，导入 Maven 模块 `management/pom.xml`。
2. 设置 Project SDK 为 `D:\idea\jdk-21.0.5`，并在 Maven 设置中使用同一 JDK。
3. 直接运行 `management/src/main/java/my/hive/HiveApplication.java` 中的 `HiveApplication` 主类。项目默认以 `dev` profile 运行，监听 `http://localhost:8080/api`。

本地默认使用 MySQL、Redis 和内存操作日志队列，因此无需安装 RabbitMQ 或 XXL-JOB。若需验证 RabbitMQ 日志链路，启动 RabbitMQ 后设置环境变量 `OPERATION_LOG_QUEUE_TYPE=rabbitmq`。

## 启动前端

在 `D:\HiveManager\management-ui` 运行：

```powershell
npm run dev
```

打开 Vite 输出的地址（默认 `http://localhost:5173`）。开发服务器会将 `/api` 自动代理到后端 `http://localhost:8080`，无需另外配置跨域或 API 地址。

## 常见启动失败

- **提示 Java 版本不支持**：IDEA 或 Maven 仍在使用 JDK 8，改为 JDK 21 后重新导入 Maven。
- **数据库连接失败**：确认 MySQL80 服务已启动，并确认本地 `hive` 库存在。
- **Redis 连接失败**：确认 Windows 的 Redis 服务已启动且监听 6379。
- **8080 或 5173 已被占用**：停止占用进程后重启对应服务。
