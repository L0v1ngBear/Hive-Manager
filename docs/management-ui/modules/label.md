# 标签模板与打印

## 源码 / 路由 / 批次

- 页面：`management-ui/src/views/function/label.vue`
- API：`management-ui/src/views/function/label/api/label.js`、`management-ui/src/views/function/label/api/printTask.js`
- 打印配置：`management-ui/src/utils/printProfile.js`；条码/二维码由页面运行时生成。
- 路由：`/function/label`，路由名 `Label`，功能开关 `module.label`。
- 迁移批次：Batch 3；状态为 `Element Plus migrated with protected custom surface`。

## 用户功能

- 在布匹标签、订单流转码、设备巡检码三个页签间切换，并查看待打印数量。
- 查看和选择模板，编辑模板名称、标题、毫米尺寸、条码/二维码开关、默认标记和字段可见性后保存。
- 加载布匹/订单待打印任务；设备页签按关键字读取设备列表并选择打印目标。
- 调整纸宽、纸高、页边距、X/Y 偏移和缩放，保存浏览器热敏打印配置并打印校准页。
- 生成 CODE128 SVG 条码和二维码 Data URL，在 DOM 标签预览中显示并打开独立打印窗口。
- 打印窗口结束后由用户确认成功或失败，再回写打印任务结果。

## API

| 函数                         | 方法   | 路径                           | 用途                         |
| ---------------------------- | ------ | ------------------------------ | ---------------------------- |
| `listLabelTemplates`         | GET    | `/label-template/list`         | 查询当前打印类型的模板       |
| `listLabelTemplateVariables` | GET    | `/label-template/variables`    | 查询可用模板变量             |
| `saveLabelTemplate`          | POST   | `/label-template/save`         | 新增或更新模板               |
| `uploadLabelTemplate`        | POST   | `/label-template/upload`       | API 封装存在；当前页面未调用 |
| `setDefaultLabelTemplate`    | POST   | `/label-template/{id}/default` | API 封装存在；当前页面未调用 |
| `deleteLabelTemplate`        | DELETE | `/label-template/{id}`         | API 封装存在；当前页面未调用 |
| `listPendingPrintTasks`      | GET    | `/print-task/pending`          | 查询当前类型待打印任务       |
| `getPendingPrintTaskCount`   | GET    | `/print-task/pending-count`    | 查询三个页签待打印数         |
| `reportPrintTask`            | POST   | `/print-task/report`           | 回写用户确认的打印成功/失败  |
| `getEquipmentPage`           | GET    | `/equipment/page`              | 设备巡检码页签搜索设备       |

## 权限

- 路由入口要求 `label:template:list`，并受 `module.label` 控制。
- 后端模板列表/变量要求 `label:template:list`，保存要求 `label:template:save`；API 中另有上传、设默认、停用的独立权限接口，但当前页面未调用。
- 设备页签、设备内容及概览计数调用 `/equipment/page`，统一按仓库真实权限 `equipment:list` 保护；无权限时不请求设备接口、不展示设备页签和内容，也不会用待打印计数伪装设备数量。
- “编辑模板/保存模板”按 `label:template:save` 保持可见但禁用，并通过 tooltip 说明原因；保存函数也执行权限防线。
- 当前 `management` 源码中未找到 `/print-task/*` 控制器，打印任务接口的服务来源与权限无法由本仓库后端实现继续确认。

## 状态流

- 初始化加载模板、变量、页签计数、当前待打印任务与本地打印配置；设备页签改为加载设备分页。
- 模板编辑：从当前模板解析设计 JSON → 修改基础属性和字段可见性 → 保存 → 重载模板并选中新记录。
- 打印：选择业务目标与模板 → 生成二维码/条码 → 按毫米尺寸生成打印 DOM/CSS → 打开打印窗口。
- 打印窗口触发结束处理后再次询问用户；确认成功上报状态 `1`，选择未成功上报状态 `2`。
- 打印载荷兼容 `labelQrPayload`、`clothQrPayload`、`inventoryQrPayload`；设备载荷使用 `HIVE_EQUIPMENT:{equipmentCode}`。

## 加载空错态

- 模板、变量和当前业务列表统一按 tab 请求，切换前清空旧内容；loading、成功空态、401/403、网络/5xx 失败互斥显示并支持重试。
- tab 加载使用 request-id 实现 last-request-wins，旧成功、旧失败与旧 finally 不覆盖新 tab。
- 保存和打印依赖 `ElMessage`/`ElMessageBox` 反馈；接口错误主要由全局请求层提示。
- 条码或二维码生成异常会把对应内容清空，但没有面向用户的独立渲染错误态。
- 打印窗口被拦截有明确错误；浏览器结束打印后仍以用户二次确认为准，代码不能直接得知纸张是否输出。

## UI 控件现状

- 大量原生 `button/input/select` 与手写页签、模板表单、任务列表、预览区和打印区。
- 所谓打印“画布”实际是 `printAreaRef` 下的 DOM/CSS 标签预览，并非 HTML `<canvas>`；当前页面也没有拖拽、缩放元素或层级编辑交互。
- 标签成品使用固定毫米尺寸的 `.thermal-label`、运行时注入打印页样式和专用打印窗口 HTML。
- 已使用 Element Plus 消息与确认服务；二维码、条码、打印配置使用专用工具和运行时 DOM。

## Element Plus 替换与保留项

- 已替换：外围 tabs/徽标、标准表单、选择器、数字输入、开关、命令按钮、加载空态和持久错误面板；Element Plus 组件均显式导入。
- 可用 `ElInputNumber` 承载纸张尺寸、边距、X/Y 偏移和缩放，但必须保留单位、精度、上下限与序列化类型。
- 保留：DOM 标签预览、条码 SVG、二维码图片、毫米尺寸打印 HTML/CSS、运行时样式注入和打印窗口协议。
- 保留：`printProfile` 持久化、变量插值、业务载荷构造以及打印任务完成时机。
- 保护节点：`printAreaRef`、`.thermal-label` 及其内部 DOM 不套入 Element Plus 控件；`QRCode.toDataURL`、`JsBarcode`、`buildLabelPrintHtml`、`afterprint` 监听和 `reportPrintTask` 成功/失败上报保持原实现。

## 风险

- 标签预览与实际打印共享毫米尺寸、padding、transform 和运行时 CSS；任何额外 wrapper、盒模型或样式优先级变化都可能造成实体标签偏移。
- 不得把 `.thermal-label` 内部打印节点直接替换成会注入额外 DOM 的 Element Plus 控件；Element Plus 只迁移打印画布外围控制区。
- 浏览器打印受打印机驱动、页边距、DPI、缩放和方向影响；屏幕预览正确不等于热敏打印正确。
- 条码/二维码必须等待渲染资源就绪再打印；载荷字段兼容顺序不可随意删除，否则旧任务可能生成不同码值。
- 打印结果依赖用户二次确认；取消确认会以“浏览器打印状态回写失败”上报失败，文案并不等价于真实打印机故障。
- `/print-task/*` 在当前后端源码中没有映射，不能仅凭前端封装宣称端到端接口已落地。
- 模板 JSON/元素配置是持久化契约；表单组件替换不得把数值变字符串、丢失未知字段或重排坐标。

## 验证清单

- [ ] 模板列表、变量、编辑、保存及只有列表权限时的命令状态正确。
- [ ] 旧模板加载保存后未知字段、字段可见性和毫米尺寸不漂移。
- [ ] DOM 打印画布外围组件替换后结构、尺寸、条码和二维码位置不变。
- [ ] 库存条码、库存二维码、设备巡检二维码载荷逐字节一致。
- [ ] 30/40mm 等已支持标签尺寸在浏览器预览和真实打印机实测。
- [ ] 纸宽/纸高、边距、X/Y 偏移、缩放和本地配置恢复正确。
- [ ] 打印取消、资源失败、任务完成接口失败不会静默丢任务。
- [ ] 设备页签在缺少 `equipment:list` 时有明确权限状态。
- [ ] 桌面与窄屏编辑器外围可用，打印 DOM 不受响应式 CSS 污染。
