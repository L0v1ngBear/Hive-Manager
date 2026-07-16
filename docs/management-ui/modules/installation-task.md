# 安装任务维护档案

> 当前状态：Element Plus migrated；归 Batch 2。

## 源码 / 路由 / 改造批次

- 页面：management-ui/src/views/function/installationTask/installationTask.vue。
- 前端 API：management-ui/src/views/function/installationTask/api/installationTask.js。
- 附件组件：management-ui/src/components/DragAttachmentUpload.vue。
- 路由：/function/installation-task，名称 InstallationTask，标题“安装任务”。
- 路由门槛：permissions = [order:list]，features = [module.order]。
- 后端入口：InstallationTaskController。
- 后端类级 feature：CODE_ORDER。
- 改造批次：Batch 2（运营管理模块）。
- 改造边界：不改状态枚举、物流校验和附件契约；安装人员改为多人明细契约。

## 用户可见功能

- 查看安装任务总量、各状态和本页附件数量摘要。
- 按状态卡片筛选；按关键字、客户和项目查询。
- 分页查看订单、客户项目、交付物流、安装状态、安装人员和附件；默认显示前三人，超过时显示“另有 N 人”并可展开全部。
- 刷新列表，查看当前更新时间和状态色。
- 打开“处理”弹窗，动态新增或删除安装人员；每人单独维护姓名和联系电话，最多 20 人。
- 维护“特殊及异常情况说明”。
- 上传、下载或移除验收附件；单文件前端限制 10MB。
- 保存任务后关闭弹窗并刷新当前列表。

## 前端 API

| 封装函数                           | HTTP | 路径                                   | 用途                           |
| ---------------------------------- | ---- | -------------------------------------- | ------------------------------ |
| getInstallationTaskPage            | GET  | /installation-tasks/page                | 分页筛选任务并返回 installers[] |
| updateInstallationTaskStatus       | POST | /installation-tasks/status              | 保存状态、物流、installers[] 和附件字段 |
| uploadInstallationTaskAttachment   | POST | /installation-tasks/attachment/upload   | 上传附件，30 秒超时            |
| downloadInstallationTaskAttachment | GET  | /installation-tasks/attachment/download | 下载附件 Blob，30 秒超时       |

## 权限与 feature

- 页面入口使用 module.order 与 order:list，而不是安装模块的细粒度权限。
- 后端列表要求 installation:list。
- 后端保存要求 installation:update。
- 后端上传要求 installation:attachment:upload。
- 后端下载要求 installation:attachment:download。
- 权限枚举还定义 installation:\*。
- 页面按真实接口权限分别检查 `installation:update`、`installation:attachment:upload`、`installation:attachment:download`；命令保持可见，无权时置灰并显示原因，处理函数同步阻止请求。附件组件用独立 `disabled` 状态表达无上传权限，只有真实上传请求才显示上传动画；`select/download/remove` 事件契约保持不变。
- 内置安装岗位同时包含 order:list 与 installation:\* 子权限，但自定义角色仍可能触发入口/接口错配。

## 关键状态 / 数据流

- filters 包含 current、size、status、keyword、customerName、projectName。
- 挂载后调用 loadTasks；状态卡、重置和翻页都会更新 filters 后重新请求。
- 服务端返回 data、total、pages，分别写入 rows 与 pagination。
- 状态固定为 production_completed、shipped_pending_install、completed_accepted。
- 编辑器从当前行的 `installers` 深拷贝回填，不另发详情请求。
- shipped_pending_install 保存前必须同时填写 expressCompany 与 expressNo。
- 每个人员行的姓名和联系电话都必填；姓名最多 50 字，电话最多 40 字，允许手机号、座机和分机，完全相同的姓名和电话组合不能重复。
- completed_accepted 保存前至少需要一名完整安装人员；其他状态允许 `installers` 为空。
- 保存 payload 只提交 `installers: [{ name, phone }]`，不提交废弃的单人字段；同时保留状态、物流、异常说明和附件元数据。
- 上传成功只回填编辑器；最终关联任务依赖后续 status 保存。
- 下载通过 Blob 和临时 object URL 触发浏览器保存。
- 各状态摘要计数由当前 rows 计算；总数取服务端 pagination.total。

## 加载 / 空态 / 错误态

- 列表互斥显示 loading、真实空态、401/403 和网络/5xx 错误态，错误可重试。
- saving 与 attachmentUploading 控制保存、关闭和上传交互。
- 请求开始先清空旧 rows，并使用最新请求代次阻止旧成功、旧失败和旧 loading 状态覆盖新筛选。
- 上传或保存失败由 finally 恢复状态，弹窗保持当前编辑内容。

## 当前原生 / 自定义控件

- 标准命令、输入、选择、文本域、表格、分页和弹窗均使用显式导入的 Element Plus 组件。
- 自定义：摘要卡、状态标签、响应式表格、遮罩弹窗、DragAttachmentUpload。
- 已用 Element Plus：ElMessage，仅用于成功与校验提示。
- 图标使用 Material Symbols。

## Element Plus 对照与明确保留项

- button → ElButton；input → ElInput；select → ElSelect/ElOption；textarea → ElInput type=textarea。
- 手写弹窗 → ElDialog；table/pagination → ElTable、ElPagination。
- 加载/空态 → v-loading、ElEmpty；状态可映射 ElTag。
- 明确保留：三个状态值及其提交字符串，不做名称重写。
- 明确保留：发货待安装的物流必填、完成验收的施工人员必填。
- 明确保留：specialExceptionNote、附件元数据和更新 payload 字段。
- 明确保留：Blob 下载、10MB 前端限制和 30 秒附件请求超时。

## Element Plus 迁移结果

- 列表使用显式导入的 `ElTable`、`ElPagination`、`ElTag`、`ElEmpty` 和 `v-loading`。
- 筛选与状态编辑使用 `ElForm`、`ElInput`、`ElSelect`，编辑层使用 `ElDialog`。
- 安装状态校验、物流与施工字段、特殊说明及 `DragAttachmentUpload` 契约保持不变。

## 已发现风险

- 路由要求 order:list，接口要求 installation:list；两者不等价，可能出现“能进不能查”或“有安装列表权却不能进”。
- 三类变更与附件权限已独立守卫；自定义角色仍需正确组合读取、更新、上传和下载权限。
- 各状态摘要是当前页计数，却与服务端总数卡并列，不能代表全量状态分布。
- 点击“查询”不会显式把 current 重置为 1；在后页修改条件可能请求同一页码并得到空页。
- 列表使用请求代次隔离快速筛选；后续修改必须保持所有异步写入受最新请求控制。
- 关闭按钮只受 saving 控制，模板中的取消按钮未同时禁用 attachmentUploading；closeEditor 会拒绝关闭但没有解释。

## 验证清单

- [ ] order:list 与 installation:list 不同组合的路由和列表行为已验证。
- [ ] update、attachment upload、attachment download 的按钮权限状态与后端一致。
- [ ] 三种状态筛选、分页、关键字/客户/项目查询通过。
- [ ] 发货待安装缺物流时前后端均拒绝。
- [ ] 完成验收缺施工人员时前后端均拒绝。
- [ ] 特殊异常说明保存并回显。
- [ ] 10MB 限制、上传、移除、下载和失败恢复通过。
- [ ] 加载、空态、错误态及窄屏表格/弹窗布局通过。
