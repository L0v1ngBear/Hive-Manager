# 客户管理维护档案

> 当前状态：Element Plus 已迁移（Task 3）；改造批次：Batch 1。

## 源码 / 路由 / 改造批次

- 列表页：management-ui/src/views/function/customer/customer.vue。
- 新建/编辑抽屉：management-ui/src/views/function/customer/customerCreate.vue。
- 前端 API：management-ui/src/views/function/customer/api/customer.js。
- 动态字段：useTenantFieldConfig、tenantFieldConfig 工具及共享租户字段配置 API。
- 直接依赖：TableColumnSettings、useLocalTableColumns、Element Plus 表格/分页/弹窗/抽屉组件。
- 路由：/function/customer，名称 Customer，标题“客户管理”。
- 路由门槛：permissions = [customer:page]，features = [module.customer]。
- 后端入口：CustomerController，类级 feature 为 CODE_CUSTOMER。
- 改造批次：Batch 1（标准管理页）。

## 用户可见功能

- 按关键字、客户类型和创建日期范围筛选客户。
- 分页查看客户列表，并从路由 keyword 或 q 同步搜索词。
- 列表列名、可见字段和顺序受租户字段配置与本地列顺序共同影响。
- 查看客户详情。
- 打开新建抽屉，录入客户名称、类型、联系人和项目。
- 打开编辑抽屉并加载详情回填。
- 动态新增或删除联系人。
- 动态新增或删除项目，并维护项目名称、施工面积和项目负责人。
- 按租户字段配置控制字段显示、标签和必填规则。
- 调整表格列顺序、恢复默认并导出当前表格。

## 前端 API

| 封装函数           | HTTP | 路径                  | 用途                    |
| ------------------ | ---- | --------------------- | ----------------------- |
| getCustomerPage    | GET  | /customer/page        | 分页筛选客户            |
| createCustomer     | POST | /customer/add         | 新建客户                |
| updateCustomer     | POST | /customer/update      | 更新客户                |
| getCustomerDetail  | GET  | /customer/detail/{id} | 详情展示与编辑回填      |
| getCustomerOptions | GET  | /customer/options     | 订单页客户/项目联想选项 |

- customer.vue 还调用共享 getCurrentTenantFieldConfig；该调用属于共享租户字段配置 API，不在 customer/api/customer.js 内重复定义。

## 权限与 feature

- 页面和侧栏入口检查 module.customer 与 customer:page。
- 分页与客户选项后端要求 customer:page。
- 详情后端要求 customer:detail。
- 新建后端要求 customer:add。
- 更新后端要求 customer:update。
- 列表页和 CustomerCreateDrawer 使用 useUserStore 检查上述细粒度权限。
- 新建、详情、编辑和当前页导出命令保持可见；权限不足时置灰、禁用鼠标并通过 title 说明原因。
- TableColumnSettings 的导出按钮检查 table:export；后端权限仍是最终边界。

## 关键状态 / 数据流

- 列表状态由 filters、pageNum、pageSize、total、totalPages 和 customerList 组成。
- 挂载时先处理路由关键字，并加载租户字段配置和客户列表。
- 路由 keyword/q 变化会重新应用关键字并刷新列表。
- customerFieldConfig 决定字段标签、显示与必填规则。
- 列表只为 customerName、customerType、contactName、contactPhone、projectName、projectOwner、projectCount、constructionArea 提供渲染器。
- useLocalTableColumns 将动态默认列与浏览器保存顺序合并。
- 打开详情后通过 getCustomerDetail 写入 detailData。
- 新建成功关闭/复位抽屉并刷新列表；编辑成功同样刷新。
- 编辑抽屉 watch visible/customerId，按模式重置表单或加载详情。
- 提交 payload 保持 contacts 与 projects 数组结构；更新模式携带客户 id。

## 加载 / 空态 / 错误态

- loading 控制 ElTable 的 v-loading、分页禁用和空态判定。
- 非加载且 customerList 为空时显示表格空态。
- detailLoading、loadingDetail 与 submitting 分别覆盖详情、编辑回填和提交过程。
- 列表请求开始即清空旧数据；401/403、网络错误和 5xx 分别形成持久错误面板，并提供重试。
- 权限/请求失败、加载、真实空列表和筛选无结果按互斥分支展示。
- 详情、字段配置和列表分别维护自己的异步流程，没有合并成会互相覆盖的单一 loading。

## 当前 Element Plus / 自定义控件

- Element Plus：ElInput、ElSelect/ElOption、ElDatePicker、ElButton、ElTable/ElTableColumn、ElPagination、ElEmpty、ElDialog 和 ElMessage。
- CustomerCreateDrawer 内使用 ElDrawer、ElForm/ElFormItem、ElInput、ElSelect/ElOption 和 ElButton。
- 自定义：CustomerCreateDrawer 内的联系人/项目重复表单。
- 共享自定义：TableColumnSettings；日期筛选直接使用 ElDatePicker。

## Element Plus 实现与明确保留项

- 搜索与文本字段使用 ElInput；客户类型使用 ElSelect/ElOption。
- 日期范围使用 ElDatePicker，并通过 value-format 保持 createStart/createEnd 的 YYYY-MM-DD 字符串格式。
- 列表、分页、空态和加载使用 ElTable、ElPagination、ElEmpty 与 v-loading。
- 详情使用 ElDialog；新建/编辑使用 ElDrawer。
- 命令使用 ElButton；提交 loading 和 disabled 规则保持。
- 明确保留：租户字段配置驱动的 visible、label、required。
- 明确保留：动态列本地顺序、TableColumnSettings，以及按当前动态列映射 customerList 的结构化当前页导出。
- 明确保留：contacts/projects 数组、编辑回填和成功刷新时序。
- 明确保留：路由 keyword/q 驱动的筛选行为。

## 已发现风险

- 租户字段配置若增加不在 customerColumnRenderers 集合中的字段，该字段不会出现在列表列中。
- 动态字段配置、列表与编辑抽屉分别加载配置，改造时存在标签/必填规则短暂不一致的风险。

## 验证清单

- [ ] customer:page/detail/add/update 各种组合的入口、按钮和后端结果一致。
- [ ] module.customer 关闭时路由和侧栏均不可进入。
- [ ] 关键字、类型、日期、路由 q/keyword、重置和分页通过。
- [ ] 租户字段的显示、标签、必填和列表列同步。
- [ ] 动态列顺序刷新后保留，恢复默认和导出可用。
- [ ] 新建/编辑联系人与项目数组完整提交并正确回显。
- [ ] 详情、编辑回填、提交失败不显示上一客户的错误数据。
- [ ] 加载、空态、403 和网络错误可区分。
- [ ] 桌面与窄屏下表格、详情和抽屉无溢出或遮挡。
