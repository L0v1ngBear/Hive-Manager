# 质量与售后管理

## 源码 / 路由 / 批次

- 页面：`management-ui/src/views/function/badProduct/badProduct.vue`
- API：`management-ui/src/views/function/badProduct/api/badProduct.js`
- 共享依赖：`BusinessTimeCorrectionPanel.vue`、`DragAttachmentUpload.vue`、`TableColumnSettings.vue`。
- 路由：`/function/bad-product`，路由名 `BadProduct`，功能开关 `module.badProduct`。
- 迁移批次：Batch 2；状态为 `Audit baseline`。

## 用户功能

- 按关键字、类型、状态和日期等条件分页查询记录。
- 在质量问题与售后问题两个业务范围间切换，并使用各自类型选项。
- 查看编号、关联订单、异常数量、损失金额档位、责任/处理信息和状态。
- 登记或编辑记录，维护业务时间、类型、描述和单个附件凭证。
- 直接用列表行数据打开详情并下载附件，没有独立详情请求。
- 填写负责人员、处理方式、处理措施、改进方案和备注后提交审核。
- 调整本地表格列顺序并导出当前质量列表。

## API

| 函数                           | 方法 | 路径                               | 用途                       |
| ------------------------------ | ---- | ---------------------------------- | -------------------------- |
| `getBadProductPage`            | GET  | `/bad-product/list`                | 质量/售后记录分页筛选      |
| `saveBadProduct`               | POST | `/bad-product/save`                | 新增或编辑质量问题         |
| `processBadProduct`            | POST | `/bad-product/process`             | 提交处理结果并更新状态     |
| `uploadBadProductAttachment`   | POST | `/bad-product/attachment/upload`   | 上传登记附件               |
| `downloadBadProductAttachment` | GET  | `/bad-product/attachment/download` | 按 URL 与名称下载附件 Blob |

## 权限

- 路由入口接受 `badproduct:list`、`badproduct:save`、`badproduct:process` 任一权限，并受 `module.badProduct` 控制。
- 登记/编辑命令使用 `badproduct:save`；处理命令使用 `badproduct:process`；列表/详情读取使用 `badproduct:list`。
- 权限硬化测试要求保存和处理动作继续使用统一 `v-permission` 禁用语义。
- 风险：仅保存或处理权限可进入路由，但初始化列表读取仍可能因缺少 `badproduct:list` 失败。

## 状态流

- 初始化分页列表；筛选、重置、分页重新请求，行操作打开详情。
- 登记：填写业务时间、类型、问题描述、异常数量、损失档位和附件 → 保存 → 关闭表单并刷新。
- 编辑：加载/回填记录 → 修改允许字段与附件 → 保存刷新。
- 处理：填写责任与处理字段 → 提交处理接口 → 进入审核中并刷新列表；页面没有处理附件字段。
- 状态展示：`pending` 待处理、`pending_audit` 审核中、`processed` 已处理。
- 质量类型包括原材料、工艺标准、工艺流程、其他；页面另有售后对象类型选项，值必须原样提交。

## 加载空错态

- 列表有加载标志和空列表提示；详情/表单/处理层分别维护可见状态。
- 附件上传组件有上传中、文件名/地址/大小和移除状态；单文件前端上限 10MB，接口失败由消息提示。
- 页面没有持久错误面板；列表或详情失败时需避免继续显示上一筛选或上一记录数据。
- 保存和处理属于高影响提交，迁移后需要独立按钮 loading 与失败后表单保留。

## UI 控件现状

- 原生输入、选择、文本域、表格、分页和手写弹层/抽屉；状态使用自定义徽标。
- 已使用 `ElMessage`；业务时间和拖拽附件为项目自定义组件。
- 表格动态列与导出依赖现有 DOM/列配置契约。

## Element Plus 替换与保留项

- 替换：筛选/表单→`ElForm`、`ElInput`、`ElSelect`、`ElInputNumber`；命令→`ElButton`。
- 替换：列表→`ElTable`，分页→`ElPagination`，详情/处理→`ElDrawer`/`ElDialog`，状态→`ElTag`。
- 替换：标准上传外围可用 `ElUpload`，但只有在完全兼容单附件返回值、移除、Blob 下载和预览行为时进行。
- 保留：`BusinessTimeCorrectionPanel`、`DragAttachmentUpload`、附件元数据结构、动态列/导出、业务状态与权限指令。

## Element Plus Migration

- The list now uses explicitly imported `ElTable`, `ElPagination`, `ElTag`, `ElEmpty`, and `v-loading`.
- Filters, record editing, and process submission use `ElForm`, `ElInput`, and `ElSelect`; details and commands use `ElDrawer`.
- Type and scope values, loss-bracket payloads, attachment ordering, `BusinessTimeCorrectionPanel`, `DragAttachmentUpload`, and permission directives are retained.

## 风险

- 质量附件不是装饰字段：当前每条记录只有 `attachmentName/attachmentUrl/attachmentSize` 一组元数据；迁移不得误改为多附件或虚构处理附件。
- 编辑已有记录时，移除本地展示项是否代表删除服务端附件必须严格遵循当前提交契约；不能擅自增加删除语义。
- 上传成功而质量记录保存失败会产生已上传但未关联的文件；页面必须保留附件引用和失败表单，避免用户重复上传。
- 处理提交与审批中心的 `badproduct:process` 权限相连；状态流不能在前端直接标记完成来绕过后端处理。
- 损失金额 UI 是档位选择，实际提交代表值 `25/100/350/1250/3500/5001`；不能把档位标签误当精确损失金额，也不能替换为任意金额输入而改变合同。
- 业务时间是本地日期时间字符串；替换日期控件时不得引入 UTC 转换或改变补秒格式。
- 导出依赖当前表格 DOM；切换 `ElTable` 后需重新验证动态列与附件/长文本的导出表现。

## 验证清单

- [ ] 三类权限分别验证路由、列表、登记/编辑和处理按钮状态。
- [ ] 筛选、分页、动态列、详情和导出正确。
- [ ] 各质量/售后类型和值与后端枚举一致。
- [ ] 单个登记附件、已有附件下载/移除及保存失败恢复正确，处理表单不新增附件语义。
- [ ] 六个损失档位的代表值、展示区间和必填校验保持一致。
- [ ] 业务时间显示与提交不发生时区漂移。
- [ ] 处理成功后状态、处理信息、责任信息和审批中心可见结果一致。
- [ ] 加载、真实空数据、接口错误、旧详情清理和重复提交状态可辨识。
