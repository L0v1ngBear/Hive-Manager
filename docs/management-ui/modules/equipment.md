# 设备巡检

## 源码 / 路由 / 批次

- 页面：`management-ui/src/views/function/equipment/equipment.vue`
- API：`management-ui/src/views/function/equipment/api/equipment.js`
- 路由：`/function/equipment`，路由名 `Equipment`，功能开关 `module.equipment`。
- 迁移批次：Batch 1；状态为 `Element Plus migrated`。

### 迁移结果（2026-07-13）

- 当前页 Excel 使用显式 headers/rows，不读取 `ElTable` DOM，fixed 操作列克隆不会重复导出行。
- 打开详情前清空旧详情和巡检；详情、巡检各自互斥呈现 loading、empty、401/403、网络/5xx 失败并可重试。
- 详情和巡检分别使用 request-id，跨设备慢响应与旧 finally 不覆盖新设备状态。
- 新建、编辑、停用、保存按 `equipment:save` 保持可见但禁用，并用 tooltip 说明原因。

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
- 当前页面没有设备命令级 `v-permission`，新建、编辑和停用入口对能进入页面的用户可见，最终由后端拒绝无权请求。
- 风险：路由可由详情或巡检记录权限进入，但页面初始化仍请求 `equipment:list`；细粒度角色可能在首屏即失败。

## 状态流

- 初始化设备分页；筛选或分页重新查询。
- 新建：清空表单 → 输入设备信息 → 校验名称 → 保存 → 关闭编辑层并刷新列表。
- 编辑：把行数据回填表单并锁定设备编码 → 保存 → 刷新列表。
- 详情：先加载设备详情，再按设备 ID 加载最多 20 条巡检记录。
- 停用：消息框确认 → 调用停用接口 → 刷新列表；状态由启用变为停用。

## 加载空错态

- 列表有加载标志，空列表显示无设备数据；详情打开后按请求结果展示。
- 请求错误主要依赖全局请求拦截与 `ElMessage`，页面没有独立错误占位或重试面板。
- 保存、停用、导出失败不会改写服务端状态；迁移时应给提交命令独立 loading，避免重复点击。

## UI 控件现状

- 筛选和编辑表单使用 `ElForm`、`ElInput`、`ElInputNumber`、`ElSelect`；命令使用 `ElButton`。
- 列表、分页、编辑和详情分别使用 `ElTable`、`ElPagination`、`ElDrawer`；状态使用 `ElTag`，加载和空态使用 `v-loading`、`ElEmpty`。
- 导出继续使用项目表格导出工具，并从 `ElTable` 的真实 DOM 根节点读取当前页可见列与数据。

## Element Plus 替换与保留项

- 替换：筛选与表单→`ElForm`、`ElInput`、`ElInputNumber`、`ElSelect`；命令→`ElButton`。
- 替换：列表→`ElTable`，分页→`ElPagination`，编辑/详情→`ElDrawer`，状态→`ElTag`。
- 替换：加载和空态→`v-loading` 与 `ElEmpty`；停用确认继续使用 `ElMessageBox` 或 `ElPopconfirm`。
- 保留：设备编码编辑锁定、巡检周期默认语义、详情与巡检记录的主从关系、导出字段与文件名。

## 风险

- 编辑时设备编码禁用是业务约束；替换表单后不得仅靠视觉禁用而把编码重新提交为可修改值。
- 停用会影响现场扫码巡检，确认文案、权限和后端拒绝信息不能弱化。
- 页面缺少 `equipment:save` 的命令级禁用状态；Element Plus 迁移不能把这个现状误记为已具备前端权限保护。
- 详情与巡检记录是两个请求，切换设备时应清空上一设备数据并防止慢请求覆盖新选择。
- 当前导出绑定真实 DOM 表格；切换 `ElTable` 后需确认导出工具能读到完整列与当前数据，而非虚拟/隐藏结构。
- 巡检周期显示缺省为 7 天；要区分后端空值与合法数值，避免 `|| 7` 掩盖边界值。

## 验证清单

- [ ] 三种路由读取权限与保存/停用权限分别验证。
- [ ] 无 `equipment:save` 时新建、编辑、停用入口和后端拒绝行为被明确处理。
- [ ] 关键字、状态、分页和空列表正确。
- [ ] 新建自动编码、编辑锁定编码、必填校验和重复提交防护正确。
- [ ] 详情快速切换时设备和巡检记录不串数据。
- [ ] 停用确认、取消、成功、后端拒绝均可辨识。
- [ ] Excel 导出列、值和文件内容与迁移前一致。
- [ ] 桌面及窄屏表格、编辑层、详情层无溢出。
