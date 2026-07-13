# 设备巡检

## 源码 / 路由 / 批次

- 页面：`management-ui/src/views/function/equipment/equipment.vue`
- API：`management-ui/src/views/function/equipment/api/equipment.js`
- 路由：`/function/equipment`，路由名 `Equipment`，功能开关 `module.equipment`。
- 迁移批次：Batch 1；状态为 `Element Plus migrated`。

### 迁移结果（2026-07-13）

- 当前页 Excel 使用显式 headers/rows，不读取 `ElTable` DOM；按用户确认的新契约导出 5 列（设备、类型/位置、负责人、最近巡检、状态），保持名称+编码、类型+位置组合文本并移除巡检周期，fixed 操作列克隆不会重复导出行。
- 打开详情前清空旧详情和巡检；详情、巡检各自互斥呈现 loading、empty、401/403、网络/5xx 失败并可重试。
- 详情和巡检分别使用 request-id，跨设备慢响应与旧 finally 不覆盖新设备状态。
- 新建、编辑、停用、保存按 `equipment:save` 保持可见但禁用，并用 tooltip 说明原因。
- 设备名和详情命令按 `equipment:detail` 保持可见但禁用并说明原因，handler 同步拦截；巡检记录内容仅对 `equipment:inspection:list` 可见，刷新命令无权限时置灰并不发请求。

## 用户功能

- 按关键字和状态分页查询设备，查看设备编码、类型、位置、负责人、巡检周期、最近巡检和状态。
- 新建设备、编辑设备；编辑时设备编码不可修改，不填编码时由服务端生成。
- 查看设备详情及巡检记录，按设备筛选记录分页。
- 停用设备，提交前通过确认框说明停用后现场扫码巡检不可用。
- 导出当前设备表格为 Excel。

## API

| 函数                            | 方法 | 路径                            | 用途             |
| ------------------------------- | ---- | ------------------------------- | ---------------- |
| `getEquipmentPage`              | GET  | `/equipment/page`               | 设备分页筛选     |
| `getEquipmentDetail`            | GET  | `/equipment/detail/{id}`        | 设备详情         |
| `saveEquipment`                 | POST | `/equipment/save`               | 新增或编辑设备   |
| `disableEquipment`              | POST | `/equipment/disable/{id}`       | 停用设备         |
| `getEquipmentInspectionRecords` | GET  | `/equipment/inspection/records` | 设备巡检记录分页 |

## 权限

- 路由入口接受 `equipment:list`、`equipment:detail`、`equipment:inspection:list` 任一权限，并受 `module.equipment` 控制。
- 列表、详情、巡检记录分别对应上述三项权限。
- 后端保存与停用都要求 `equipment:save`；没有独立的 `equipment:disable` 权限。
- 新建、编辑、停用和保存命令按 `equipment:save` 保持可见；无权时按钮禁用，tooltip 说明原因，处理函数也会拒绝调用 API。
- 风险：路由可由详情或巡检记录权限进入，但页面初始化仍请求 `equipment:list`；细粒度角色可能在首屏即失败。

## 状态流

- 初始化设备分页；筛选或分页重新查询。
- 新建：清空表单 → 输入设备信息 → 校验名称 → 保存 → 关闭编辑层并刷新列表。
- 编辑：把行数据回填表单并锁定设备编码 → 保存 → 刷新列表。
- 详情：先加载设备详情，再按设备 ID 加载最多 20 条巡检记录。
- 停用：消息框确认 → 调用停用接口 → 刷新列表；状态由启用变为停用。

## 加载空错态

- 列表请求前清空旧行；加载、真实空态、401、403、网络异常与 5xx 失败互斥呈现，失败面板可重试。
- 详情和巡检记录打开前分别清空旧数据，各自使用 request-id，并互斥呈现 loading、真实空态、401、403、网络异常与 5xx 失败；失败均可重试。
- 保存、停用、导出失败不会改写服务端状态；迁移时应给提交命令独立 loading，避免重复点击。

## UI 控件现状

- 筛选和编辑表单使用 `ElForm`、`ElInput`、`ElInputNumber`、`ElSelect`；命令使用 `ElButton`。
- 列表、分页、编辑和详情分别使用 `ElTable`、`ElPagination`、`ElDrawer`；状态使用 `ElTag`，加载和空态使用 `v-loading`、`ElEmpty`。
- 导出继续使用项目 Excel 工具，但输入为显式 headers 和当前页 rows，不读取 `ElTable` DOM。

## Element Plus 替换与保留项

- 替换：筛选与表单→`ElForm`、`ElInput`、`ElInputNumber`、`ElSelect`；命令→`ElButton`。
- 替换：列表→`ElTable`，分页→`ElPagination`，编辑/详情→`ElDrawer`，状态→`ElTag`。
- 替换：加载和空态→`v-loading` 与 `ElEmpty`；停用确认继续使用 `ElMessageBox` 或 `ElPopconfirm`。
- 保留：设备编码编辑锁定、巡检周期默认语义、详情与巡检记录的主从关系、导出字段与文件名。

## 风险

- 编辑时设备编码禁用是业务约束；替换表单后不得仅靠视觉禁用而把编码重新提交为可修改值。
- 停用会影响现场扫码巡检，确认文案、权限和后端拒绝信息不能弱化。
- 路由允许仅有详情或巡检权限的角色进入，但页面初始化仍请求设备列表；细粒度角色的首屏体验仍需浏览器验证。
- 详情与巡检记录虽已有独立 request-id，后续修改仍需保持两套序号和关闭抽屉时的失效处理。
- 导出字段由显式 headers/rows 维护；列表列变更时必须同步更新导出映射，避免页面与文件字段漂移。
- 巡检周期显示缺省为 7 天；要区分后端空值与合法数值，避免 `|| 7` 掩盖边界值。

## 验证清单

- [ ] 三种路由读取权限与保存/停用权限分别验证。
- [x] 源码契约测试验证无 `equipment:save` 时新建、编辑、停用和保存保持可见、禁用并显示原因。
- [ ] 关键字、状态、分页和空列表正确。
- [ ] 新建自动编码、编辑锁定编码、必填校验和重复提交防护正确。
- [x] 源码契约测试验证详情和巡检记录清旧数据、双 request-id 与过期响应防护。
- [ ] 停用确认、取消、成功、后端拒绝均可辨识。
- [x] 行为测试验证当前页显式 headers/rows、字段值和 fixed 操作列克隆不会重复导出行。
- [ ] 桌面及窄屏表格、编辑层、详情层无溢出。
