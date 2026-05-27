# 通知闭环模块说明

`notification_record` 用于承接经营建议、异常预测、自动提醒和后续短信/订阅消息推送。

- `dedupe_key`：业务去重键，多后端实例同时运行时避免重复生成同一条提醒。
- `receiver_user_id`：为空时表示租户内广播；有值时表示指定员工接收。
- `channel`：当前默认 `IN_APP`，后续可扩展 `SMS`、`WECHAT_SUBSCRIBE`。
- `send_status`：预留外部推送状态，短信或微信订阅消息接入后更新。
- `route`：前端点击通知后的业务入口，用于形成处理闭环。
