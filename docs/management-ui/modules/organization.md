# 组织管理

## 责任边界

组织管理负责部门层级、职位目录和部门成员只读查看。员工资料、直属负责人和单独权限仍在“员工管理”维护；员工页中的组织架构图表示人员汇报关系，不承担部门或职位维护。

## 页面结构

- 左侧：递归部门树，显示负责人、员工数、职位数和启停状态。
- 右侧“成员”：查看当前部门员工、工号、职位、手机号和在职状态。
- 右侧“职位”：查看当前部门职位编码、员工数、状态，并新增、编辑或删除职位。
- 部门抽屉：维护名称、编码、上级、负责人、排序和状态。
- 职位抽屉：维护所属部门、名称、编码、排序和状态。

## 数据规则

1. 部门名称在租户内不能重复，部门不能将自身或下级设为上级。
2. 有下级部门、员工或职位的部门不允许删除。
3. 职位归属于一个部门，同部门职位名称不能重复。
4. 有员工使用的职位不允许删除，也不允许移动到其他部门。
5. 同部门职位改名会同步该部门员工的职位名称。
6. 新增、编辑和批量调整员工时，所选职位必须属于所选部门。
7. 部门和职位均使用逻辑删除，不提供级联清理。

## API

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/organization/overview` | 部门树和组织统计 |
| GET | `/organization/department/{id}/employees` | 部门成员 |
| POST | `/organization/department/save` | 新增或编辑部门 |
| DELETE | `/organization/department/{id}` | 删除空部门 |
| GET | `/organization/department/{id}/positions` | 部门职位 |
| POST | `/organization/position/save` | 新增或编辑职位 |
| DELETE | `/organization/position/{id}` | 删除空职位 |

## 权限

| Permission | Capability |
| --- | --- |
| `organization:view` | 进入组织管理并查看部门、成员和职位 |
| `organization:department:manage` | 新增和编辑部门 |
| `organization:department:delete` | 删除空部门 |
| `organization:position:manage` | 新增和编辑职位 |
| `organization:position:delete` | 删除空职位 |

没有变更权限时，命令保持可见但置灰，并显示缺少的权限码。内置“人事专员”默认仅查看，“人事负责人”默认拥有全部组织管理权限；用户单独权限仍可覆盖角色结果。

## Source

- Frontend: `management-ui/src/views/function/organization/organization.vue`
- Frontend API: `management-ui/src/views/function/organization/api/organization.js`
- Controller: `management/src/main/java/my/hive/api/organization/OrganizationController.java`
- Service: `management/src/main/java/my/hive/domain/organization/service/OrganizationService.java`
- Permission catalog: `management/src/main/java/my/hive/shared/permission/PermissionCatalogV3.java`

## Verification

- 部门切换会先清空旧成员和职位，并拒绝过期请求回写。
- 总览刷新会使成员和职位请求同时失效，避免跨部门显示旧数据。
- 保存按钮有提交中状态，重复点击不会重复提交。
- 删除仍以服务端约束为最终判定，前端员工数置灰只用于提前提示。
