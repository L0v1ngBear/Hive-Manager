# 价格管理

## 源码 / 路由 / 批次

- 列表页：`management-ui/src/views/function/price/price.vue`
- 编辑页组件：`management-ui/src/views/function/price/priceCreate.vue`
- API：`management-ui/src/views/function/price/api/price.js`
- 路由：`/function/price`，路由名 `Price`，功能开关 `module.price`；编辑页由列表页内部切换，不是独立路由。
- 迁移批次：Batch 2；状态为 `Audit baseline`。

## 用户功能

- 查看价格统计、SKU 分页列表，并按关键字、状态等条件筛选。
- 查看型号规格、基准价、状态、更新时间、等级价、客户特价和变更记录详情。
- 新建或编辑 SKU 价格，选择型号规格并维护基准价。
- 维护客户等级固定价/折扣率、客户特价和生效日期，并通过发布接口保存价格矩阵。
- 删除价格记录、下载导入模板、批量导入、按当前筛选导出 Excel。
- 从导航全局搜索接收 `keyword` 查询参数。

## API

| 函数                          | 方法   | 路径                     | 用途                       |
| ----------------------------- | ------ | ------------------------ | -------------------------- |
| `getPriceStats`               | GET    | `/price/stats`           | 价格统计                   |
| `getPricePage`                | GET    | `/price/page`            | SKU 价格分页筛选           |
| `getPriceDetail`              | GET    | `/price/detail/{id}`     | 价格详情、等级价和客户特价 |
| `getPriceModels`              | GET    | `/price/models`          | 型号规格候选               |
| `getPriceCustomers`           | GET    | `/price/customers`       | 客户候选                   |
| `publishPrice`                | POST   | `/price/publish`         | 新增或调整并发布价格矩阵   |
| `deletePrice`                 | DELETE | `/price/{id}`            | 删除价格记录               |
| `exportPriceExcel`            | GET    | `/price/export-excel`    | 按当前筛选导出 Excel       |
| `downloadPriceImportTemplate` | GET    | `/price/import-template` | 下载价格导入模板           |
| `importPrices`                | POST   | `/price/import`          | 上传表格批量导入价格       |

## 权限

- 路由入口要求 `price:list`，并受 `module.price` 控制。
- 后端列表、统计、客户/型号选项、导出和模板下载要求 `price:list`；详情要求 `price:detail`；发布与导入要求 `price:publish`；删除要求 `price:delete`。
- 当前列表页和编辑页没有价格命令级 `v-permission`，新增、编辑、删除、导入等入口对列表用户可见，最终依赖后端拒绝。
- 风险：路由只声明 `price:list`，打开详情本身还需要 `price:detail`，不能把可进入列表等同于可查看/编辑详情。

## 状态流

- 初始化读取统计和分页列表；路由关键字变化时回填筛选并重新查询。
- 打开新建页时加载型号和客户候选；打开编辑时先加载详情并回填基准价、等级价、客户特价和生效日期。
- 发布前校验型号、规格、基准价和生效日期，通过 `/price/publish` 提交后返回列表并刷新统计。
- 删除通过确认框调用 DELETE；导入显示成功/失败统计，导出按当前标准化查询下载 Blob。
- 价格状态由后端枚举驱动；页面显示草稿/生效/停用等文本时不得自行推导新状态。

## 加载空错态

- 列表和编辑详情有加载状态；无 SKU 时显示空列表，等级价/客户特价为空时显示对应空内容。
- 请求错误主要依赖全局请求拦截和消息提示，没有持久页内错误区。
- 编辑详情请求失败时需避免保留上一条 SKU 草稿；保存/发布需防止重复提交。

## UI 控件现状

- 列表以原生筛选、按钮、表格、状态徽标和手写分页为主。
- 编辑页使用原生输入、数字/日期控件和可增删的等级价、客户特价行。
- 已使用 Element Plus 消息/确认服务；页面切换和表单校验由组件内状态管理。

## Element Plus 替换与保留项

- 替换：筛选与编辑→`ElForm`、`ElInput`、`ElSelect`、`ElInputNumber`、`ElDatePicker`；命令→`ElButton`。
- 替换：列表/变更记录→`ElTable`，分页→`ElPagination`，状态→`ElTag`，加载空态→`v-loading`/`ElEmpty`。
- 等级价和客户特价可用可编辑表格或动态表单行，但须保留行级增删、客户唯一性及验证定位。
- 保留：金额精度、后端字段名、状态值、`YYYY-MM-DD` 生效日期和列表/编辑内部切换行为。

## 风险

- JavaScript `Number` 是二进制浮点；金额输入、比较和展示不得通过浮点运算产生额外小数，提交精度必须与后端 `BigDecimal` 契约一致。
- 不得用 `||` 给金额设默认值，否则合法的 `0` 会被覆盖；组件值类型需明确是数字、字符串还是 `null`。
- 新建默认生效日期使用 `new Date().toISOString().slice(0, 10)`，它按 UTC 取日期；UTC 与本地跨日窗口可能默认成前一天或后一天。
- 日期组件迁移必须显式设置 `value-format="YYYY-MM-DD"`，不能提交 ISO `Z` 时间或毫秒时间戳替代当前日期字符串。
- 页面按钮文案“发布价格”对应唯一写接口，不存在单独 `/price/save` 草稿接口；迁移不得虚构保存/发布双状态。
- 当前前端无命令级价格权限保护，后端 403 前用户仍可填写完整表单。
- 动态价格行索引变化时，校验错误必须跟随业务行而非旧数组下标。

## 验证清单

- [ ] 路由关键字、筛选、分页、统计与详情返回一致。
- [ ] 新建、编辑、取消返回时不会串用上一 SKU 草稿。
- [ ] 金额 `0`、两位以上小数、极大值、空值和非法字符按后端规则处理。
- [ ] 等级价和客户特价增删、客户唯一性和折扣计算正确。
- [ ] 在 Asia/Shanghai 及 UTC 日期跨日窗口验证默认生效日期不漂移。
- [ ] 发布、删除、导入、导出和详情变更记录正确。
- [ ] 无写权限、加载、空数据、接口错误和重复提交状态可辨识。
