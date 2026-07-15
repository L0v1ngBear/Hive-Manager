# MySQL 存储异常处理

## 当前典型现象

- `mysqladmin ping` 显示 `mysqld is alive`。
- `CREATE TABLE` 报 `ERROR 1030 (HY000): Got error 168 - 'Unknown (generic) error from engine'`。
- `CREATE DATABASE hive_shadow` 报 `ERROR 3680 (HY000): Failed to create schema directory ... errno: 2`。

## 判断

这不是业务 SQL 或迁移清单问题，而是 MySQL 数据目录、InnoDB 存储引擎或容器挂载目录已经不健康。此时继续跑迁移只会失败。

如果诊断输出里出现：

```text
ls: cannot access './mysql/data': No such file or directory
touch: cannot touch '/var/lib/mysql/__write_probe': No such file or directory
```

基本可以确认是宿主机 `./mysql/data` 目录缺失或旧容器仍挂着异常数据目录。需要停掉并移除旧 MySQL 容器，重新创建宿主机数据目录后再启动。

## 处理顺序

1. 非破坏诊断：

```bash
cd /root/hive
bash db-migrations/scripts/diagnose-mysql-storage.sh
```

2. 如果只是权限问题，优先修复数据目录权限：

```bash
cd /root/hive
docker compose stop mysql
mkdir -p mysql/data
chown -R 999:999 mysql/data
chmod 750 mysql/data
docker compose up -d mysql
bash db-migrations/scripts/preflight-online.sh
```

3. 如果 DDL 仍失败，说明需要按 baseline 重建 MySQL。该操作会保留旧数据目录，但会重建新的线上 `hive`：

```bash
cd /root/hive
CONFIRM_REBUILD_MYSQL=YES bash db-migrations/scripts/rebuild-mysql-from-baseline.sh
```

## 注意

- 执行重建前必须确认本地 baseline 是标准库。
- 重建脚本会先尝试逻辑备份，再物理打包并重命名旧 `mysql/data`。
- 旧数据目录不会删除，方便后续手工抽取数据。
