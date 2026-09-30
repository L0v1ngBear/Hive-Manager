# 售后多次处理交接

分支：`codex/feat-aftersales-treatment-rounds`；基线：`9f8fb14`。隔离工作目录：`D:/HiveManager-aftersales-rounds`。不包含订单收件人分支、其他拆分需求或桌面部署包的额外热修复。

## 使用与边界

售后详情 → 新增处理记录 → 补发配件 / 换电机 / 两者同时。配件先独立出库，再完成处理、填写本次回访。未解决则继续新增；历史不可覆盖。我的待办及小铃铛定位具体处理记录，待办按主工单当前负责人隔离；未新增微信订阅推送。

原工单须已研判、已指派、处于处理中或已结案，且所需审批已通过。追加会恢复主工单为处理中，不重置质保。新增后原资料只读，后续结案说明追加留档。全部新增处理完成回访且最近操作确认解决，才允许主工单结案。此版本不提供追加处理删除/取消或重新审批。

## API 契约

统一前缀 `/api/after-sales`，沿用现有 `Result` 包装与业务异常格式。

| 方法及路径 | 输入 / 返回 |
| --- | --- |
| GET `/tickets/{ticketId}/treatments` | 按追加序号返回处理列表，最多100条 |
| POST `/tickets/{ticketId}/treatments` | `AfterSalesTreatmentSaveRequest`；返回新增记录 |
| PUT `/tickets/{ticketId}/treatments/{id}` | 同上，必须带当前 `version`；返回更新记录 |
| POST `/tickets/{ticketId}/treatments/{id}/{action}` | action 为 `outbound`、`complete`、`follow-up`；`AfterSalesTreatmentActionRequest` |
| GET `/treatment-todos` | `pageNum`、`pageSize`（最多100）；当前负责人的分页待办 |

创建须传稳定 `requestKey`，同键同内容重试返回原记录，不同内容拒绝。保存需 `treatmentType`、`description`；补发需非空 `parts`，换电机需型号与正数数量。完成传 `version`、`resolution`；回访传 `version`、`followUp`（满意度、是否解决、内容、可选图片）。租户/负责人/质保快照由服务端确定。过期版本、越权、非法状态均拒绝，不静默覆盖。

## 数据与部署

新迁移 `V20260919_002_after_sales_treatments.sql` 已注册清单及校验和。新增处理表、库存流水关联字段及主单结案历史字段，不重写历史迁移。后端须在迁移执行后启动；已有发布流程不变。

本次未合并主分支、未生成交付目录、未远程部署。打包前先对齐订单收件人分支、桌面现有热修复及并行拆分需求；不得用本分支基线覆盖现有生产功能。回退策略见 [ADR](../decisions/001-after-sales-treatment-history.md)。

## 验证记录（2026-09-19）

- 后端 `mvnw.cmd -q package`：488测试通过，0失败、0跳过，JAR构建成功。
- 前端 `npm test`：469项，467通过、0失败、2项既有浏览器测试跳过；生产构建与本次变更文件 ESLint 通过。
- `node scripts/check-aftersales-treatments-browser.mjs`：隔离Chrome中验证追加、表单校验、320/768/1024/1440宽度、完成、回访、历史不变，无浏览器错误。使用模拟API，不等于真实服务端端到端联调。
- H2 MySQL模式执行实际迁移与真实Mapper分页查询，验证旧流水保留、租户和负责人隔离；事务测试验证失败回滚、并发出库只扣一次。尚未在真实MySQL执行迁移。
- 本地依赖复用主工作区 node_modules；基线锁文件 `npm ci` 存在缺项，未擅改依赖。全量测试中两个原文件按LF本地规范化以适配既有文本断言，无内容差异且不提交无关文件。
- 发布前仍须验证干净依赖安装、真实MySQL迁移、完整登录页面联调，并按固定部署目录约定校验产物。当前结论为开发验证通过，不是已上线。
