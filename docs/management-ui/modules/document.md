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
- 页面上的“新建文件夹”和上传区域没有 `v-permission` 或权限计算。
- 面包屑需要独立 `document:breadcrumbs`，但路由只检查 list。
- 列设置默认显示当前页导出按钮；页面没有基于 `table:export` 控制可见/禁用状态。
- 后端权限仍是最终边界，前端控件可见不代表请求可执行。

## 状态流

1. setup 末尾直接调用 `fetchDocuments(0)` 加载根目录。
2. `fetchDocuments(parentId)` 先更新 `currentParentId`，再取列表，非根目录随后取面包屑。
3. 本地 `filters` 只过滤已加载的当前目录数组，不发搜索请求。
4. 双击文件夹复用 `fetchDocuments(id)`；双击文件校验协议后 `window.open`。
5. 创建文件夹成功后刷新当前目录。
6. 上传前端先限制 20MB，成功后刷新当前目录。

- 列顺序由 `useLocalTableColumns('document.list', ...)` 保存在当前浏览器。
- 文档列表没有分页，页面一次渲染接口返回的全部直接子项。

## 空态、错态与加载态

- 列表加载时在表格容器内显示手写半透明遮罩。
- 非 loading 且本地筛选结果为空时统一显示“当前目录为空”。
- 因此“目录真实为空”和“筛选无匹配”使用同一文案。
- `fetchDocuments` 没有局部 catch，失败反馈依赖统一请求层。
- 列表失败时 `currentParentId` 已先改变；旧列表可能仍保留。
- 面包屑失败发生在列表成功之后，新列表与旧面包屑可能短暂不一致。
- 创建弹窗 catch 同时接收用户取消和请求失败，局部代码不区分二者。

## 控件和样式现状

- 页面为固定左栏加主内容区，移动端在 768px 以下改为纵向。
- 工具栏使用原生按钮、文本输入、select 和 Material Symbols。
- 列表使用原生响应式 table；文件名列根据扩展名显示手写色块。
- 新建文件夹使用 `ElMessageBox.prompt`，通知使用 `ElMessage`。
- 上传使用自定义 `DragAttachmentUpload`，列设置使用自定义 `TableColumnSettings`。
- 页面没有使用 `ElInput`、`ElSelect`、`ElTable`、`ElEmpty` 或 `ElUpload`。

## Element Plus 接入/替换建议

- 关键词改为 `ElInput`、类型改为 `ElSelect`，保留当前纯前端过滤语义。
- 工具栏命令改为 `ElButton`，按对应权限显示 disabled 原因。
- 表格可迁移 `ElTable`，但先覆盖双击、列顺序、当前页导出和移动端展示。
- 空结果区分目录为空、筛选无结果和请求失败后再使用 `ElEmpty`/错误 Result。
- 上传可评估 `ElUpload drag`，继续保留 20MB 前端限制、parentId 和成功刷新。
- 面包屑可迁移 `ElBreadcrumb`，目录 id 和导航顺序不变。

## 风险

- list-only 用户能看到创建和上传控件，点击后才收到后端拒绝。
- 缺少 breadcrumbs 权限时进入文件夹可能出现列表已切换、面包屑未切换。
- 上传提示称支持 PPT，但共享上传组件默认 accept 不包含 `.ppt/.pptx`；拖放路径又不校验扩展名。
- 前端只检查 20MB 和协议，不校验 MIME、扩展名或下载域名；安全校验依赖服务端存储链路。
- 后端 rename/move 已存在但页面无入口，不能在视觉迁移中误标为现有功能。
- 当前页导出依赖原生 table DOM，直接换 `ElTable` 会破坏采集逻辑。

## 验证清单

- [ ] 根目录、三级以上目录、上级、面包屑和根目录导航一致。
- [ ] list、breadcrumbs、folder:create、file:upload 权限分别验证。
- [ ] 同名、非法名称、空文件、超 20MB、租户额度不足均显示后端有效错误。
- [ ] 文件/文件夹筛选和无匹配文案不误导。
- [ ] http/https 文件打开正常，其他协议被拒绝且 opener 为空。
- [ ] 列顺序刷新后保留，导出列顺序和页面一致。
- [ ] 320px 与桌面宽度下工具栏、左栏、表格无不可用溢出。
