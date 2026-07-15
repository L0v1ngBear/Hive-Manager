# 出库单打印维护档案

> 当前状态：Element Plus migrated with protected custom surface；Batch 3。打印 DOM、毫米尺寸与打印 CSS 是受保护输出面。

## 源码 / 路由 / 改造批次

- 页面：management-ui/src/views/function/receipt.vue。
- 前端 API：management-ui/src/views/function/receipt/api/receipt.js。
- 打印适配：management-ui/src/utils/printProfile.js。
- 直接依赖：BusinessTimeCorrectionPanel、useTimeCorrectionMode。
- 路由：/function/receipt，名称 Receipt，标题“出库单打印”。
- 路由门槛：permissions = [`print:receipt:list`]，features = [`module.receipt`]。
- 后端入口：ReceiptPrintController，类级 feature 为 CODE_RECEIPT。
- 改造批次：Batch 3，protected print DOM。
- 迁移只覆盖管理壳层标准控件；打印 DOM/CSS 不改。

## 用户可见功能

- 查看并刷新待打印出库单队列，选择单据后加载打印详情。
- 在打印预览中按模板分页，显示客户、单号、项目、日期、制单人、明细、合计、物流和签字区。
- 打印前修正单据字段和明细行，新增/移除行，并保存可追溯修订。
- 调整纸宽、纸高、页边距、X/Y 偏移和缩放，持久化浏览器打印配置。
- 从模板同步纸张尺寸、恢复默认配置、打印校准页。
- 打开浏览器打印窗口，确认已打印，或作废/跳过当前单据。
- 新建、选择、编辑、保存并设为默认出库单模板。
- 配置模板标题、每页行数、仓库、提示语、物流/签字显示开关。
- 配置打印列的显示、列名、毫米列宽和顺序，并实时预览。
- 业务时间修正模式下单独修正录单日期。

## 前端 API

| 封装函数                     | HTTP | 路径                           | 用途                                   |
| ---------------------------- | ---- | ------------------------------ | -------------------------------------- |
| getPendingPrintOrders        | GET  | /receipt/print/pending         | 查询待打印队列                         |
| getPrintDetail               | GET  | /receipt/print/detail          | 按 orderNo 读取可打印详情              |
| getRawPrintCommand           | GET  | /receipt/print/raw-command     | 获取原始打印命令；当前浏览器页面未调用 |
| updatePrintDetail            | POST | /receipt/print/update          | 保存打印修订或仅修正日期               |
| markPrinted                  | POST | /receipt/print/mark-printed    | 确认打印完成                           |
| cancelPrint                  | POST | /receipt/print/cancel          | 作废/跳过待打印单据                    |
| listReceiptTemplateVariables | GET  | /receipt/template/variables    | 查询 receipt 模板变量                  |
| listReceiptTemplates         | GET  | /receipt/template/list         | 查询 receipt 模板                      |
| saveReceiptTemplate          | POST | /receipt/template/save         | 保存模板                               |
| setDefaultReceiptTemplate    | POST | /receipt/template/{id}/default | 设置默认模板                           |

## 权限与 feature

- 页面和侧栏入口检查 `module.receipt` 与 `print:receipt:list`。
- 队列、模板列表和模板变量后端要求 `print:receipt:list`。
- 详情要求 `print:receipt:detail`；生成打印指令和确认已打印要求 `print:receipt:execute`。
- 修订打印内容、保存模板和设置默认模板要求 `print:receipt:update`。
- 作废/跳过要求 `print:receipt:cancel`。
- 页面使用 `useUserStore` 对细粒度命令授权，读、执行、修订、取消四类动作不互相替代。
- 无权命令保持可见、禁用并通过 tooltip 说明原因；无详情权限时不请求或展示单据内容。后端权限与取消、确认语义保持不变。

## 关键状态 / 数据流

- 挂载时并行加载待打印队列、模板和模板变量。
- 刷新队列先清空 selectedOrder、tableData、printDraft，并关闭时间修正模式。
- 选择队列项后按 orderNo 拉详情，生成可编辑行和 printDraft。
- 后端只允许 order_status=1 且 print_status=0 的待打印记录读取、修订和状态变更。
- 确认打印将记录更新为 order_status=2、print_status=1；作废更新为 order_status=3、print_status=0。
- 打印前 openBrowserPrint 会先静默保存修订，再复制 #print-paper-area 的 innerHTML。
- 新窗口由 buildPrintHtml 生成，注入 printCss，300ms 后调用 window.print。
- printPages 按 rowsPerPage（限制 4 到 10）分组，并用空行补满；无明细也生成一页骨架。
- 模板列支持 visible、label、widthMm 和顺序；全部隐藏时回退默认可见列。
- 保存模板先写 saveReceiptTemplate，再调用 setDefaultReceiptTemplate。
- 打印 profile 存浏览器本地；模板 designJson/content 存后端。

## 加载 / 空态 / 错误态

- isFetchingList 控制队列刷新；队列为空且非加载时显示明确空态。
- isLoadingDetail 显示预览区 loading-mask；未选择单据时显示预览空态。
- isPrinting、isSubmitting、isTemplateSaving 分别禁用打印、状态提交和模板保存。
- 队列和详情分别维护 loading、成功空态与持久失败面板；401、403、网络和 5xx 文案可区分并可重试。
- 队列和详情均使用 request-id 实现 last-request-wins；选择新单据前清空旧详情、明细和草稿。刷新队列或切离打印页会同步使详情请求失效、释放 loading 并清空受保护详情状态，旧响应与旧 finally 不覆盖新状态。
- 打印 DOM 缺失或弹窗被拦截时有页面级错误提示。

## 当前原生 / 自定义控件

- 管理外围不再保留原生 input；打印修正区的文本与明细行字段使用 `ElInput`，模板列宽使用 `ElInputNumber` 并保持数值类型、10–80 mm 范围、1 mm 步长与原 change 归一化事件。
- 自定义：BusinessTimeCorrectionPanel、打印 profile 控件、模板列编辑器、实时纸张预览。
- 明确保留的原生输出面：#print-paper-area、.paper-stack、.receipt-page、.receipt-print-table；这些打印 DOM/CSS 未迁移。

## Element Plus 对照与明确保留项

- 管理壳 tabs、按钮、输入、选择、数字和复选框已迁移为显式导入的 `ElTabs`、`ElButton`、`ElInput`、`ElSelect`、`ElInputNumber`、`ElCheckbox`。
- 队列和详情加载/空态/失败态使用 `v-loading`、`ElEmpty`、`ElResult`；确认动作继续使用 `ElMessageBox`。
- 模板管理表单使用标准控件，并维持原数值范围、步长和 Boolean 类型。
- 明确保留且不得改：#print-paper-area 及其子节点层级和 class。
- 明确保留且不得改：buildPrintHtml、printCss、@page、mm 尺寸、page-break/break-after。
- 明确保留且不得改：纸张 transform、字体/边框/行高、空行补页和分页计算。
- 明确保留且不得改：visibleColumns 的顺序、显示和 widthMm 对 colgroup 的绑定。
- 打印 DOM/CSS 的任何修改必须脱离常规 Element Plus 迁移单独设计并做实物/预览回归。

## 已发现风险

- 路由入口只要求 `print:receipt:list`，因此仅列表权限用户仍可进入页面；详情、修订、打印执行、模板保存和作废命令会在页面内按各自精确权限禁用并说明原因。
- getRawPrintCommand 存在但浏览器打印流未使用，不能在样式迁移中擅自切换打印通道。
- 页面 scoped 预览 CSS 与 printCss 各维护一套关键尺寸，后续改动存在预览/实打漂移风险。
- 浏览器弹窗策略、打印驱动、纸张 DPI、缩放和物理边距仍可能造成预览与实打差异，必须通过目标打印机校准页和实体纸张验证。

## 验证清单

- [ ] list/detail/mark/cancel 四类权限组合的按钮状态和后端结果一致。
- [ ] 仅待打印状态可详情、修订、确认或作废；重复提交被拒绝且提示清楚。
- [ ] 修订保存、仅日期修正、金额自动计算和至少一条明细校验通过。
- [ ] 模板新增、更新、默认设置、列显隐/顺序/宽度和变量加载通过。
- [ ] 队列加载、空队列、详情加载、详情失败和弹窗拦截可区分。
- [ ] #print-paper-area 的 DOM 快照在控件迁移前后保持一致。
- [ ] printCss、@page、毫米尺寸、分页数和每页空行保持一致。
- [ ] 241-1/目标连续纸、浏览器预览和实际打印做前后对比。
