# APISpace 物流查询与 OSS 预留设计

## 目标

- 将订单列表悬浮物流查询的唯一上游从快递100替换为 APISpace 全国快递物流查询。
- 保留现有“鼠标悬浮才查询”、多批次发货、30 分钟服务端缓存、失败短缓存、租户与订单权限校验。
- 删除快递100源码、配置、测试和文档，不保留兼容分支。
- 保留并规范阿里云 OSS 私有文件存储服务，默认关闭且不改变当前本地附件行为。

## APISpace 接入

物流查询使用统一入口和供应商抽象：`LogisticsTrackingGateway` 是业务层唯一入口，`LogisticsTrackingProvider` 定义供应商编码和统一查询方法，`ApispaceLogisticsTrackingProvider` 实现 APISpace 协议。Gateway 根据 `LOGISTICS_PROVIDER` 选择子实现。订单领域只依赖 Gateway，不直接依赖 APISpace 类。当前唯一允许值为 `apispace`；未来增加供应商时只新增实现并注册供应商编码。

后端通过 `POST https://eolink.o.apispace.com/wlgj1/paidtobuy_api/trace_search` 查询，发送 JSON：

```json
{
  "cpCode": "YTO",
  "mailNo": "YT...",
  "tel": "0000",
  "orderType": "asc"
}
```

请求头固定使用 `Content-Type: application/json` 和 `X-APISpace-Token`。Token 只从服务器环境变量 `APISPACE_LOGISTICS_TOKEN` 读取，不写入 Git、前端、小程序或发布包。当前聊天中已经出现过的 Token 应在上线配置完成后轮换。

客户端使用项目现有 JDK `HttpClient`，不新增 OkHttp 依赖。连接和请求超时分别由 `APISPACE_LOGISTICS_CONNECT_TIMEOUT`、`APISPACE_LOGISTICS_REQUEST_TIMEOUT` 控制。`APISPACE_LOGISTICS_ENABLED=false` 时接口返回明确的未配置错误。

## 公司编码与隐私

现有中文物流公司名称映射改为 APISpace 大写 `cpCode`，例如圆通 `YTO`、中通 `ZTO`、顺丰 `SF`、申通 `STO`、韵达 `YUNDA`、EMS `EMS`、德邦 `DBKD`。数据库直接保存合法大写编码时允许直接使用。

仅在需要电话辅助识别的查询中发送客户电话数字的最后四位；不向供应商发送完整电话号码。电话号码为空时发送空字符串。

## 响应归一化

成功响应读取 `logisticsTrace`：

- `logisticsCompanyName`、`cpCode`、`mailNo` 映射到现有物流查询 VO；
- `logisticsStatus`、`logisticsStatusDesc` 映射状态和状态文案；
- `theLastMessage`、`theLastTime` 映射最新轨迹；
- `logisticsTraceDetailList` 映射时间、描述、状态和区域；
- 毫秒时间戳统一格式化为北京时间字符串，并按最新时间在前返回，保持现有悬浮框展示契约。

HTTP `401/403/413/416/504` 分别转换为鉴权、来源限制、频率限制、套餐额度和上游超时错误。HTTP 成功但 `success=false`、缺少 `logisticsTrace` 或 JSON 无效时，不返回伪造轨迹。错误响应不得记录 Token、完整运单号或客户电话。

## 缓存与调用保护

订单服务继续按租户、订单号、物流记录 ID、公司和运单号生成指纹。成功结果缓存 30 分钟，失败结果缓存 30 秒，同一指纹并发请求只调用一次上游。外部调用事件供应商名称改为 `apispace-logistics`，日志只保存运单号指纹。

前端接口和交互保持不变：`GET /orders/{orderId}/shipments/{shipmentId}/logistics-tracking` 仍只在物流悬浮框显示时调用。

## OSS 预留

文件上传使用供应商抽象：`FileStorageProvider` 定义供应商编码、上传和失败清理方法，`LocalFileStorageProvider` 与 `AliyunOssFileStorageProvider` 分别实现本地和 OSS 协议，`FileStorageProviderRouter` 根据 `FILE_STORAGE_PROVIDER` 选择实现。文件领域不得通过条件分支直接实例化供应商客户端。

`FILE_STORAGE_PROVIDER` 默认值为 `local`。配置为 `aliyun-oss` 时才允许路由到 OSS；未知值必须明确失败，禁止静默回退到本地，以免线上文件落错位置。

保留 `OssStorageProperties` 与 `OssStorageService`，继续使用以下服务器环境变量：

- `ALIYUN_OSS_ENABLED=false`
- `ALIYUN_OSS_ENDPOINT`
- `ALIYUN_OSS_BUCKET`
- `ALIYUN_OSS_ACCESS_KEY_ID`
- `ALIYUN_OSS_ACCESS_KEY_SECRET`
- `ALIYUN_OSS_PUBLIC_BASE_URL`
- `ALIYUN_OSS_PATH_PREFIX=hive`

默认值必须保持关闭，空密钥不得阻止后端启动。OSS 对象键必须包含租户、业务模块和日期，上传继续校验扩展名、MIME、大小、租户限流和 SHA-256。Bucket 按私有模式设计；后续正式切换时由后端鉴权下载或生成短期签名 URL，不把永久公开读作为默认方案。

本轮不把现有订单、质量、财务、安装、库存和文档附件自动切换到 OSS，避免只有上传没有私有下载闭环。OSS 服务作为可测试、可配置的基础设施保留，后续切换统一文件存储路由时无需重新引入 SDK 或重新设计元数据。

## 测试与发布

- APISpace 客户端测试覆盖请求方法、URL、Token 请求头、JSON 参数、成功归一化、时间排序、未配置和上游错误。
- 订单物流服务测试覆盖 APISpace 公司编码、电话后四位、成功/失败缓存和调用事件供应商名。
- 静态部署测试确认不存在 `KUAIDI100_*`、快递100类或明文 Token，并确认 `APISPACE_LOGISTICS_*` 与 OSS 配置映射完整。
- 前端悬浮查询测试保持通过，证明没有进入列表加载流程。
- 构建统一后端并刷新桌面 `hive全新部署`，线上 Token 只由 `/root/hive/.env` 管理。
