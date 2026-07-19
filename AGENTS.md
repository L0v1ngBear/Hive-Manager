# Hive 本地部署交付约定

## 固定交付目录

- 所有供用户上传服务器的部署文件，统一写入 `C:\Users\HUAWEI\Desktop\hive全新部署`。
- 用户提到“放到桌面”“全新配置”“部署包”或“发布文件”时，均指上述固定目录。
- 默认提供未压缩的完整目录结构，不要只提供单独 JAR，不要新建日期目录，也不要生成压缩包；用户明确要求时除外。

## 目录内容

- 后端产物必须放在 `backend/hive-backend.jar`。
- 必须同时包含 `backend/`、`db-migrations/`、`management-ui/`、`nginx/`、`scripts/`、`publish.sh`、`docker-compose.yml`、`.env.example` 和 `RELEASE_BUILD_INFO.txt`。
- 不得放入服务器运行时文件：`.env`、证书、数据库数据、Redis 数据、上传文件和备份。
- 同步新版本时直接覆盖固定交付目录中的发布文件，保留其中与发布无冲突的说明文档。

## 交付前验证

- 完整运行后端测试并成功打包。
- 校验 `backend/hive-backend.jar` 的 SHA-256 与 `RELEASE_BUILD_INFO.txt` 中的 `BackendJarSha256` 一致。
- 校验完整目录包含 `publish.sh` 及发布所需文件，不包含运行时密钥或持久化数据。
- 对登录问题相关版本，确认编译后的后端类不包含旧英文客户提示，并包含对应中文提示。

## 用户发布方式

用户会把固定交付目录中的内容覆盖到服务器 `/root/hive`，然后执行：

```bash
cd /root/hive
bash publish.sh
```

不要擅自改变这套交付方式。除非用户明确授权，不要代替用户远程发布。
