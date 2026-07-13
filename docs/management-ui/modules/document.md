# 文档管理维护档案

> 状态：Element Plus 已迁移（Task 3）；迁移批次：Batch 1；路由：`/function/document`；feature：`module.document`。

## 功能

- 浏览根目录或指定父目录下的文件夹和文件。
- 通过面包屑、上级、根目录和双击文件夹完成目录导航。
- 在当前目录新建文件夹、拖拽/选择上传文件、刷新列表。
- 按名称/扩展名关键词和文件/文件夹类型在当前结果中本地筛选。
- 双击文件时仅允许以 `http:` 或 `https:` 在新窗口打开链接。
- 支持本地列顺序和当前页 Excel 导出。

## 源码索引

| 层级      | 文件                                                                                  |
| --------- | ------------------------------------------------------------------------------------- |
| 页面      | `management-ui/src/views/function/document/document.vue`                              |
| 前端 API  | `management-ui/src/views/function/document/api/document.js`                           |
| 上传组件  | `management-ui/src/components/DragAttachmentUpload.vue`                               |
| 列设置    | `management-ui/src/components/TableColumnSettings.vue`                                |
| 控制器    | `management/src/main/java/my/management/controller/DocumentController.java`           |
| 服务      | `management/src/main/java/my/management/module/document/service/DocumentService.java` |
| 类型/状态 | `DocumentTypeEnum.java`、`DocumentUploadStatusEnum.java`                              |

## API

| 方法 | 接口                                | 当前页面用途                      | 后端权限                 |
| ---- | ----------------------------------- | --------------------------------- | ------------------------ |
| GET  | `/document/list/{parentId}`         | 当前目录列表                      | `document:list`          |
| GET  | `/document/breadcrumbs?documentId=` | 当前目录面包屑                    | `document:breadcrumbs`   |
| POST | `/document/folder/create`           | 新建文件夹                        | `document:folder:create` |
| POST | `/document/file/upload`             | multipart 上传 `file`、`parentId` | `document:file:upload`   |
| PUT  | `/document/rename`                  | 后端已提供，页面未接入            | `document:rename`        |
| PUT  | `/document/move`                    | 后端已提供，页面未接入            | `document:move`          |

- 控制器整体受 `CODE_DOCUMENT` 租户功能开关保护。
- 文件夹类型为 `0`，文件类型为 `1`；当前上传状态枚举只有 `UPLOADED`。
- 服务按 `tenantCode` 查询，过滤 `isDeleted=0`，文件夹优先、创建时间升序。
- 同目录名称必须唯一，名称最长 180 字符，不允许路径穿越字符。
- 服务端按租户 `maxStorageMb` 校验存储额度，并使用本地文件存储服务。

## 权限现状

- 路由入口要求 `document:list` 和 `module.document`。
- “新建文件夹”和上传区域分别检查 `document:folder:create` 与 `document:file:upload`。
- 面包屑需要独立 `document:breadcrumbs`，但路由只检查 list；上级、面包屑和双击文件夹入口单独检查该权限。
- 当前页导出检查 `table:export`；目录导航检查 `document:breadcrumbs`。命令保持可见，权限不足时置灰、禁用鼠标并通过 title 说明原因。
- 后端权限仍是最终边界，前端禁用状态用于在发请求前提供一致反馈。

## 状态流

1. setup 末尾直接调用 `fetchDocuments(0)` 加载根目录。
2. `fetchDocuments(parentId)` 开始时清空旧列表与面包屑；列表和新面包屑均成功后才一起提交结果。
3. 本地 `filters` 只过滤已加载的当前目录数组，不发搜索请求。
4. 双击文件夹复用 `fetchDocuments(id)`；双击文件校验协议后 `window.open`。
5. 创建文件夹成功后刷新当前目录。
6. 上传前端先限制 20MB，成功后刷新当前目录。

- 列顺序由 `useLocalTableColumns('document.list', ...)` 保存在当前浏览器。
- 文档列表没有分页，页面一次渲染接口返回的全部直接子项。

## 空态、错态与加载态

- 列表加载时由 ElTable 的 v-loading 显示加载态。
- 真实空目录显示“当前目录为空”；已加载目录的本地筛选无匹配显示“没有符合筛选条件的文档”。
- 401/403、网络错误和 5xx 分别形成持久错误面板，并提供当前目录重试。
- 请求序号阻止较早目录请求覆盖较新的目录状态，不会出现新目录 ID 配旧列表。
- 面包屑失败时不提交已暂存的新列表，错误、权限、空态和筛选无匹配互斥。
- 新建文件夹使用受控 ElDialog；请求失败由统一请求层反馈，弹窗保持可重试状态。

## 控件和样式现状

- 页面为固定左栏加主内容区，移动端在 768px 以下改为纵向。
- 工具栏使用 ElButton、ElInput、ElSelect/ElOption 和 Material Symbols。
- 列表使用 ElTable/ElTableColumn 与 ElEmpty；文件名列继续根据扩展名显示色块。
- 新建文件夹使用受控 ElDialog、ElForm 和 ElInput，通知使用 ElMessage。
- 上传使用自定义 `DragAttachmentUpload`，列设置使用自定义 `TableColumnSettings`。
- DragAttachmentUpload 保留原生隐藏 file input，并使用 ElButton 提供附件命令；页面未改用 ElUpload，以保持现有 select/download/remove 事件。

## Element Plus 实现与保留项

- 关键词使用 ElInput、类型使用 ElSelect，保留当前纯前端过滤语义。
- 工具栏命令使用 ElButton，并按 folder:create、file:upload、table:export 显示禁用原因。
- 列表使用 ElTable，并保留双击、动态列顺序和移动端布局。
- 当前页导出显式使用 filteredDocumentList 与动态列映射，不依赖 ElTable 分离的 header/body DOM。
- 上传继续使用 DragAttachmentUpload，保留 20MB 限制、parentId、multipart API 和成功刷新。
- 面包屑仍使用轻量按钮结构，目录 id 和导航顺序不变。

## 风险

- 缺少 breadcrumbs 权限时，上级、面包屑与文件夹导航保持可见但置灰并显示原因，事件边界不会调用目录 API。
- 页面 accept 已包含 `.ppt/.pptx`，选择和拖放统一按 accept 校验；文件内容安全仍依赖服务端存储链路。
- 前端检查 accept 扩展名、20MB 大小和打开链接协议，不校验文件内容或下载域名。
- 后端 rename/move 已存在但页面无入口，不能在视觉迁移中误标为现有功能。
- 结构化导出映射必须与动态列渲染保持同步，否则页面显示值和导出值可能不一致。

## 验证清单

- [ ] 根目录、三级以上目录、上级、面包屑和根目录导航一致。
- [ ] list、breadcrumbs、folder:create、file:upload 权限分别验证。
- [ ] 同名、非法名称、空文件、超 20MB、租户额度不足均显示后端有效错误。
- [ ] 文件/文件夹筛选和无匹配文案不误导。
- [ ] http/https 文件打开正常，其他协议被拒绝且 opener 为空。
- [ ] 列顺序刷新后保留，导出列顺序和页面一致。
- [ ] 320px 与桌面宽度下工具栏、左栏、表格无不可用溢出。
