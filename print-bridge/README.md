# 本地打印桥接服务

这个程序运行在用户电脑本机，用来把管理端下发的原始打印指令写入本地 USB 打印机。

## 环境要求

- Windows
- Java 21

## 接口

- `GET http://127.0.0.1:13528/health`
- `GET http://127.0.0.1:13528/printers`
- `POST http://127.0.0.1:13528/print/raw`

`/print/raw` 请求体示例：

```json
{
  "jobName": "CK20260415001.prn",
  "driverType": "ESC_P",
  "contentType": "application/octet-stream",
  "charset": "GB18030",
  "base64Content": "....",
  "printerName": "EPSON LQ-630K"
}
```

## 构建

```powershell
cd D:\HiveManager\print-bridge
mvn package
```

构建完成后运行：

```powershell
java -jar target\print-bridge-0.0.1-SNAPSHOT.jar
```

## 快速启动

也可以直接运行：

```powershell
start-bridge.bat
```
