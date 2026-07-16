<template>
  <div class="function-page-shell h-full min-h-0 font-body">
    <div class="function-page-container space-y-6">
      <header class="function-page-header">
        <div>
          <div class="function-page-eyebrow">
            <span class="material-symbols-outlined">account_tree</span>
            组织治理中心
          </div>
          <h1 class="function-page-title">组织管理</h1>
          <p class="function-page-desc">统一维护部门层级、职位体系、负责人和员工归属。</p>
        </div>
        <el-tooltip :disabled="canDepartmentManage" content="暂无 organization:department:manage 权限" placement="bottom">
          <span><el-button type="primary" :disabled="!canDepartmentManage" @click="openCreate(null)">新增部门</el-button></span>
        </el-tooltip>
      </header>

      <section v-if="!loading && !overviewFailure" class="grid grid-cols-2 gap-4 md:grid-cols-4">
        <article class="stat-card"><p>部门总数</p><strong>{{ stats.departmentCount }}</strong></article>
        <article class="stat-card"><p>启用部门</p><strong>{{ stats.enabledDepartmentCount }}</strong></article>
        <article class="stat-card"><p>员工总数</p><strong>{{ stats.employeeCount }}</strong></article>
        <article class="stat-card stat-card--warning"><p>空部门</p><strong>{{ stats.emptyDepartmentCount }}</strong></article>
      </section>

      <section class="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_560px]">
        <section class="panel-card" v-loading="loading">
          <header class="panel-header">
            <div>
              <h2>部门层级</h2>
              <p>选择部门后，可在右侧查看成员并维护该部门的职位。</p>
            </div>
            <el-button @click="fetchOverview">刷新</el-button>
          </header>
          <div class="p-5">
            <el-result v-if="overviewFailure" :icon="overviewFailure.kind === 'forbidden' ? 'warning' : 'error'" :title="overviewFailure.title" :sub-title="overviewFailure.message">
              <template #extra><el-button type="primary" :loading="loading" @click="fetchOverview">重试</el-button></template>
            </el-result>
            <div v-else-if="departments.length" class="space-y-3">
              <DepartmentNode
                v-for="node in departments"
                :key="node.id"
                :node="node"
                :active-id="activeDepartment?.id"
                :can-update="canDepartmentManage"
                @select="selectDepartment"
                @create-child="openCreate"
                @edit="openEdit"
              />
            </div>
            <el-empty v-else-if="!loading" description="还没有部门">
              <el-tooltip :disabled="canDepartmentManage" content="暂无 organization:department:manage 权限">
                <span><el-button type="primary" :disabled="!canDepartmentManage" @click="openCreate(null)">新增部门</el-button></span>
              </el-tooltip>
            </el-empty>
          </div>
        </section>

        <aside class="panel-card min-w-0">
          <header class="panel-header">
            <div class="min-w-0">
              <h2 class="truncate">{{ activeDepartment?.deptName || '部门详情' }}</h2>
              <p>{{ activeDepartment ? `负责人：${activeDepartment.leaderName || '未设置'} · 成员 ${activeDepartment.employeeCount || 0} 人 · 职位 ${activeDepartment.positionCount || 0} 个` : '请选择左侧部门' }}</p>
            </div>
          </header>

          <el-tabs v-model="activeDetailTab" class="organization-tabs">
            <el-tab-pane label="成员" name="members">
              <div class="detail-scroll space-y-3" v-loading="memberLoading">
                <el-result v-if="memberFailure" :icon="memberFailure.kind === 'forbidden' ? 'warning' : 'error'" :title="memberFailure.title" :sub-title="memberFailure.message">
                  <template #extra><el-button type="primary" :loading="memberLoading" @click="retryMembers">重试</el-button></template>
                </el-result>
                <div v-else-if="members.length" class="space-y-3">
                  <article v-for="item in members" :key="item.id" class="member-card">
                    <div class="flex items-start justify-between gap-3">
                      <div class="min-w-0">
                        <p class="truncate font-bold text-primary">{{ item.name }}</p>
                        <p class="mt-1 truncate text-xs text-on-surface-variant">{{ item.empNo || '无工号' }} / {{ item.positionName || '未设置职位' }}</p>
                      </div>
                      <el-tag :type="employeeStatusType(item.status)">{{ employeeStatusLabel(item.status) }}</el-tag>
                    </div>
                    <p class="mt-3 text-xs text-on-surface-variant">{{ item.phone || '未填写手机号' }}</p>
                  </article>
                </div>
                <el-empty v-else-if="!memberLoading" :description="activeDepartment ? '该部门暂无员工' : '请选择部门'" />
              </div>
            </el-tab-pane>

            <el-tab-pane label="职位" name="positions">
              <div class="position-toolbar">
                <p>职位仅属于当前部门；改名后会同步该部门员工。</p>
                <el-tooltip :disabled="canPositionManage" content="暂无 organization:position:manage 权限">
                  <span><el-button type="primary" size="small" :disabled="!canPositionManage || !activeDepartment" @click="openPositionCreate">新增职位</el-button></span>
                </el-tooltip>
              </div>
              <div class="detail-scroll position-table-wrap" v-loading="positionLoading">
                <el-result v-if="positionFailure" :icon="positionFailure.kind === 'forbidden' ? 'warning' : 'error'" :title="positionFailure.title" :sub-title="positionFailure.message">
                  <template #extra><el-button type="primary" :loading="positionLoading" @click="retryPositions">重试</el-button></template>
                </el-result>
                <el-table v-else :data="positions" table-layout="fixed" empty-text="该部门暂无职位">
                  <el-table-column prop="positionName" label="职位" min-width="126" show-overflow-tooltip />
                  <el-table-column prop="positionCode" label="编码" min-width="108" show-overflow-tooltip>
                    <template #default="{ row }">{{ row.positionCode || '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="employeeCount" label="员工" width="72" align="center" />
                  <el-table-column label="状态" width="78" align="center">
                    <template #default="{ row }"><el-tag :type="Number(row.status) === 1 ? 'success' : 'info'" size="small">{{ Number(row.status) === 1 ? '启用' : '停用' }}</el-tag></template>
                  </el-table-column>
                  <el-table-column label="操作" width="132" align="right">
                    <template #default="{ row }">
                      <el-tooltip :disabled="canPositionManage" content="暂无 organization:position:manage 权限">
                        <span><el-button link type="primary" :disabled="!canPositionManage" @click="openPositionEdit(row)">编辑</el-button></span>
                      </el-tooltip>
                      <el-tooltip :disabled="canPositionDelete && Number(row.employeeCount || 0) === 0" :content="Number(row.employeeCount || 0) > 0 ? '仍有员工使用该职位' : '暂无 organization:position:delete 权限'">
                        <span><el-button link type="danger" :disabled="!canPositionDelete || Number(row.employeeCount || 0) > 0" @click="handlePositionDelete(row)">删除</el-button></span>
                      </el-tooltip>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-tab-pane>
          </el-tabs>
        </aside>
      </section>
    </div>

    <el-drawer v-model="drawerVisible" :title="form.id ? '编辑部门' : '新增部门'" size="460px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="部门名称" required>
          <el-input v-model.trim="form.deptName" data-field="organization.deptName" maxlength="64" placeholder="例如：销售部、仓储部" />
        </el-form-item>
        <el-form-item label="部门编码">
          <el-input v-model.trim="form.deptCode" maxlength="64" placeholder="不填则自动生成" />
        </el-form-item>
        <el-form-item label="上级部门">
          <el-select v-model="form.parentId" :value-on-clear="''" class="w-full" placeholder="作为一级部门" clearable>
            <el-option v-for="option in parentOptions" :key="option.id" :label="option.label" :value="option.id" :disabled="option.id === form.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model.trim="form.leaderName" maxlength="50" placeholder="请输入负责人姓名" />
        </el-form-item>
        <div class="grid grid-cols-2 gap-4">
          <el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="0" :precision="0" class="w-full" /></el-form-item>
          <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="flex justify-between gap-3">
          <el-button @click="closeDrawer">取消</el-button>
          <div class="flex gap-3">
            <el-tooltip v-if="form.id" :disabled="canDepartmentDelete" content="暂无 organization:department:delete 权限"><span><el-button type="danger" plain :disabled="!canDepartmentDelete" @click="handleDelete">删除</el-button></span></el-tooltip>
            <el-tooltip :disabled="canDepartmentManage" content="暂无 organization:department:manage 权限"><span><el-button type="primary" :loading="departmentSaving" :disabled="!canDepartmentManage" @click="submitDepartment">保存</el-button></span></el-tooltip>
          </div>
        </div>
      </template>
    </el-drawer>

    <el-drawer v-model="positionDrawerVisible" :title="positionForm.id ? '编辑职位' : '新增职位'" size="460px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="所属部门" required>
          <el-select v-model="positionForm.departmentId" class="w-full" placeholder="请选择部门">
            <el-option v-for="option in departmentOptions" :key="option.id" :label="option.label" :value="option.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="职位名称" required>
          <el-input v-model.trim="positionForm.positionName" data-field="organization.positionName" maxlength="64" placeholder="例如：销售经理、仓库管理员" />
        </el-form-item>
        <el-form-item label="职位编码">
          <el-input v-model.trim="positionForm.positionCode" maxlength="64" placeholder="不填则自动生成" />
        </el-form-item>
        <div class="grid grid-cols-2 gap-4">
          <el-form-item label="排序"><el-input-number v-model="positionForm.sortNo" :min="0" :precision="0" class="w-full" /></el-form-item>
          <el-form-item label="状态"><el-switch v-model="positionForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="positionDrawerVisible = false">取消</el-button>
          <el-tooltip :disabled="canPositionManage" content="暂无 organization:position:manage 权限"><span><el-button type="primary" :loading="positionSaving" :disabled="!canPositionManage" @click="submitPosition">保存</el-button></span></el-tooltip>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue'
import { ElButton, ElDrawer, ElEmpty, ElForm, ElFormItem, ElInput, ElInputNumber, ElMessage, ElMessageBox, ElOption, ElResult, ElSelect, ElSwitch, ElTabPane, ElTable, ElTableColumn, ElTabs, ElTag, ElTooltip } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { warnAndFocusField } from '@/utils/formFocus'
import { deleteDepartment, deletePosition, getDepartmentEmployees, getDepartmentPositions, getOrganizationOverview, saveDepartment, savePosition } from './api/organization.js'
import { createOverviewRequestGate } from './overviewRequestGate.js'

const DepartmentNode = defineComponent({
  name: 'DepartmentNode',
  props: {
    node: { type: Object, required: true },
    activeId: { type: Number, default: null },
    level: { type: Number, default: 0 },
    canUpdate: { type: Boolean, default: false }
  },
  emits: ['select', 'create-child', 'edit'],
  setup(props, { emit }) {
    return () => h('div', { class: 'org-node-wrap' }, [
      h('div', {
        class: ['org-node', props.activeId === props.node.id ? 'active' : ''],
        style: { marginLeft: `${props.level * 24}px` },
        onClick: () => emit('select', props.node)
      }, [
        h('div', { class: 'min-w-0 flex-1' }, [
          h('p', { class: 'truncate font-bold text-primary' }, props.node.deptName || '未命名部门'),
          h('p', { class: 'mt-1 truncate text-xs text-on-surface-variant' }, `负责人：${props.node.leaderName || '未设置'} / 员工 ${props.node.employeeCount || 0} 人 / 职位 ${props.node.positionCount || 0} 个`)
        ]),
        h('div', { class: 'flex shrink-0 items-center gap-2' }, [
          h(ElTag, { type: Number(props.node.status) === 1 ? 'success' : 'info', size: 'small' }, () => Number(props.node.status) === 1 ? '启用' : '停用'),
          h(ElTooltip, { disabled: props.canUpdate, content: '暂无 organization:department:manage 权限' }, () => h('span', [h(ElButton, { size: 'small', disabled: !props.canUpdate, onClick: (event) => { event.stopPropagation(); emit('create-child', props.node) } }, () => '新增下级')])),
          h(ElTooltip, { disabled: props.canUpdate, content: '暂无 organization:department:manage 权限' }, () => h('span', [h(ElButton, { size: 'small', type: 'primary', plain: true, disabled: !props.canUpdate, onClick: (event) => { event.stopPropagation(); emit('edit', props.node) } }, () => '编辑')]))
        ])
      ]),
      ...(props.node.children || []).map(child => h(DepartmentNode, {
        node: child,
        activeId: props.activeId,
        level: props.level + 1,
        canUpdate: props.canUpdate,
        onSelect: node => emit('select', node),
        onCreateChild: node => emit('create-child', node),
        onEdit: node => emit('edit', node)
      }))
    ])
  }
})

const loading = ref(false)
const userStore = useUserStore()
const canDepartmentManage = computed(() => userStore.hasPermission('organization:department:manage'))
const canDepartmentDelete = computed(() => userStore.hasPermission('organization:department:delete'))
const canPositionManage = computed(() => userStore.hasPermission('organization:position:manage'))
const canPositionDelete = computed(() => userStore.hasPermission('organization:position:delete'))
const overviewFailure = ref(null)
const memberLoading = ref(false)
const memberFailure = ref(null)
const positionLoading = ref(false)
const positionFailure = ref(null)
const departmentSaving = ref(false)
const positionSaving = ref(false)
const drawerVisible = ref(false)
const positionDrawerVisible = ref(false)
const activeDetailTab = ref('members')
const departments = ref([])
const members = ref([])
const positions = ref([])
const activeDepartment = ref(null)
const stats = reactive({ departmentCount: 0, employeeCount: 0, enabledDepartmentCount: 0, emptyDepartmentCount: 0 })
const form = reactive(createEmptyForm())
const positionForm = reactive(createEmptyPositionForm())
let memberRequestId = 0
let positionRequestId = 0
const overviewRequestGate = createOverviewRequestGate()

const parentOptions = computed(() => flattenDepartments(departments.value).filter(option => !isSelfOrDescendant(option.id, form.id)))
const departmentOptions = computed(() => flattenDepartments(departments.value))

onMounted(fetchOverview)

async function fetchOverview() {
  const requestId = overviewRequestGate.begin()
  const activeDepartmentId = activeDepartment.value?.id
  loading.value = true
  memberRequestId += 1
  positionRequestId += 1
  activeDepartment.value = null
  members.value = []
  positions.value = []
  memberFailure.value = null
  positionFailure.value = null
  memberLoading.value = false
  positionLoading.value = false
  departments.value = []
  Object.assign(stats, { departmentCount: 0, employeeCount: 0, enabledDepartmentCount: 0, emptyDepartmentCount: 0 })
  overviewFailure.value = null
  try {
    const data = await getOrganizationOverview()
    if (!overviewRequestGate.isLatest(requestId)) return
    departments.value = Array.isArray(data?.departments) ? data.departments : []
    Object.assign(stats, data?.stats || {})
    if (activeDepartmentId) {
      const latest = findDepartmentById(departments.value, activeDepartmentId)
      if (latest) await selectDepartment(latest)
    } else if (departments.value.length) await selectDepartment(departments.value[0])
  } catch (error) {
    if (!overviewRequestGate.isLatest(requestId)) return
    overviewFailure.value = resolveOverviewFailure(error)
  } finally {
    if (overviewRequestGate.isLatest(requestId)) loading.value = false
  }
}

async function selectDepartment(node) {
  if (!node?.id) return
  const requestId = ++memberRequestId
  activeDepartment.value = node
  members.value = []
  memberFailure.value = null
  memberLoading.value = true
  fetchPositions(node.id)
  try {
    const nextMembers = await getDepartmentEmployees(node.id)
    if (requestId !== memberRequestId) return
    members.value = Array.isArray(nextMembers) ? nextMembers : []
  } catch (error) {
    if (requestId !== memberRequestId) return
    memberFailure.value = resolveMemberFailure(error)
  } finally {
    if (requestId === memberRequestId) memberLoading.value = false
  }
}

async function fetchPositions(departmentId) {
  const requestId = ++positionRequestId
  positions.value = []
  positionFailure.value = null
  if (!departmentId) {
    positionLoading.value = false
    return
  }
  positionLoading.value = true
  try {
    const data = await getDepartmentPositions(departmentId)
    if (requestId !== positionRequestId) return
    positions.value = Array.isArray(data) ? data : []
  } catch (error) {
    if (requestId !== positionRequestId) return
    positionFailure.value = resolvePositionFailure(error)
  } finally {
    if (requestId === positionRequestId) positionLoading.value = false
  }
}

function retryMembers() {
  if (activeDepartment.value) selectDepartment(activeDepartment.value)
}

function retryPositions() {
  if (activeDepartment.value) fetchPositions(activeDepartment.value.id)
}

function openCreate(parent) {
  if (!canDepartmentManage.value) return
  Object.assign(form, createEmptyForm())
  form.parentId = parent?.id || ''
  drawerVisible.value = true
}

function openEdit(node) {
  if (!canDepartmentManage.value) return
  Object.assign(form, {
    id: node.id,
    parentId: node.parentId || '',
    deptName: node.deptName || '',
    deptCode: node.deptCode || '',
    leaderName: node.leaderName || '',
    sortNo: node.sortNo ?? 99,
    status: node.status ?? 1
  })
  drawerVisible.value = true
}

function closeDrawer() {
  drawerVisible.value = false
}

async function submitDepartment() {
  if (!canDepartmentManage.value || departmentSaving.value) return
  const deptName = (form.deptName || '').trim()
  if (!deptName) {
    warnAndFocusField('请输入部门名称', 'organization.deptName')
    return
  }
  departmentSaving.value = true
  try {
    await saveDepartment({ ...form, deptName, parentId: form.parentId === '' ? null : Number(form.parentId), sortNo: Number(form.sortNo ?? 99), status: Number(form.status ?? 1) })
    ElMessage.success('部门已保存')
    closeDrawer()
    await fetchOverview()
  } finally {
    departmentSaving.value = false
  }
}

async function handleDelete() {
  if (!canDepartmentDelete.value || !form.id) return
  await ElMessageBox.confirm('仅空部门可以删除。请先处理下级部门、员工和职位。', '删除部门', { type: 'warning' })
  await deleteDepartment(form.id)
  ElMessage.success('部门已删除')
  closeDrawer()
  await fetchOverview()
}

function openPositionCreate() {
  if (!canPositionManage.value || !activeDepartment.value) return
  Object.assign(positionForm, createEmptyPositionForm(), { departmentId: activeDepartment.value.id })
  positionDrawerVisible.value = true
}

function openPositionEdit(row) {
  if (!canPositionManage.value) return
  Object.assign(positionForm, {
    id: row.id,
    departmentId: row.departmentId,
    positionName: row.positionName || '',
    positionCode: row.positionCode || '',
    sortNo: row.sortNo ?? 99,
    status: row.status ?? 1
  })
  positionDrawerVisible.value = true
}

async function submitPosition() {
  if (!canPositionManage.value || positionSaving.value) return
  const positionName = (positionForm.positionName || '').trim()
  if (!positionForm.departmentId) {
    ElMessage.warning('请选择所属部门')
    return
  }
  if (!positionName) {
    warnAndFocusField('请输入职位名称', 'organization.positionName')
    return
  }
  positionSaving.value = true
  try {
    await savePosition({ ...positionForm, departmentId: Number(positionForm.departmentId), positionName, sortNo: Number(positionForm.sortNo ?? 99), status: Number(positionForm.status ?? 1) })
    ElMessage.success('职位已保存')
    positionDrawerVisible.value = false
    await fetchOverview()
  } finally {
    positionSaving.value = false
  }
}

async function handlePositionDelete(row) {
  if (!canPositionDelete.value || Number(row.employeeCount || 0) > 0) return
  await ElMessageBox.confirm(`确定删除职位“${row.positionName}”吗？`, '删除职位', { type: 'warning' })
  await deletePosition(row.id)
  ElMessage.success('职位已删除')
  await fetchOverview()
}

function createEmptyForm() {
  return { id: null, parentId: '', deptName: '', deptCode: '', leaderName: '', sortNo: 99, status: 1 }
}

function createEmptyPositionForm() {
  return { id: null, departmentId: null, positionName: '', positionCode: '', sortNo: 99, status: 1 }
}

function flattenDepartments(nodes, level = 0) {
  return (nodes || []).flatMap(node => [{ id: node.id, label: `${'　'.repeat(level)}${node.deptName || '未命名部门'}` }, ...flattenDepartments(node.children || [], level + 1)])
}

function findDepartmentById(nodes, id) {
  for (const node of nodes || []) {
    if (node.id === id) return node
    const found = findDepartmentById(node.children || [], id)
    if (found) return found
  }
  return null
}

function isSelfOrDescendant(candidateId, currentId) {
  if (!currentId || !candidateId) return false
  if (candidateId === currentId) return true
  const current = findDepartmentById(departments.value, currentId)
  return hasDescendant(current?.children || [], candidateId)
}

function hasDescendant(nodes, id) {
  return (nodes || []).some(node => node.id === id || hasDescendant(node.children || [], id))
}

function employeeStatusLabel(status) {
  if (Number(status) === 1) return '在职'
  if (Number(status) === 2) return '试用'
  if (Number(status) === 0) return '离职'
  return '未知'
}

function employeeStatusType(status) {
  if (Number(status) === 1) return 'success'
  if (Number(status) === 2) return 'warning'
  return 'info'
}

function getRequestStatusCode(error) {
  const rawStatusCode = error?.response?.status ?? error?.response?.data?.code ?? error?.statusCode ?? error?.code
  const statusCode = Number(rawStatusCode)
  return Number.isFinite(statusCode) ? statusCode : 0
}

function resolveMemberFailure(error) {
  const statusCode = getRequestStatusCode(error)
  if (statusCode === 401) return { kind: 'unauthorized', title: '登录状态已失效', message: '请重新登录后再重试部门成员。' }
  if (statusCode === 403) return { kind: 'forbidden', title: '暂无权限查看部门成员', message: '请联系管理员配置组织查看权限。' }
  if (statusCode >= 500) return { kind: 'request', title: '部门成员加载失败', message: '服务暂时不可用，请稍后重试。' }
  return { kind: 'request', title: '部门成员加载失败', message: '网络连接异常，请检查网络后重试。' }
}

function resolvePositionFailure(error) {
  const statusCode = getRequestStatusCode(error)
  if (statusCode === 401) return { kind: 'unauthorized', title: '登录状态已失效', message: '请重新登录后再重试职位列表。' }
  if (statusCode === 403) return { kind: 'forbidden', title: '暂无权限查看职位', message: '请联系管理员配置组织查看权限。' }
  if (statusCode >= 500) return { kind: 'request', title: '职位列表加载失败', message: '服务暂时不可用，请稍后重试。' }
  return { kind: 'request', title: '职位列表加载失败', message: '网络连接异常，请检查网络后重试。' }
}

function resolveOverviewFailure(error) {
  const statusCode = getRequestStatusCode(error)
  if (statusCode === 401) return { kind: 'unauthorized', title: '登录状态已失效', message: '请重新登录后再重试组织管理。' }
  if (statusCode === 403) return { kind: 'forbidden', title: '暂无权限查看组织管理', message: '请联系管理员配置 organization:view 权限。' }
  if (statusCode >= 500) return { kind: 'request', title: '组织管理加载失败', message: '服务暂时不可用，请稍后重试。' }
  return { kind: 'request', title: '组织管理加载失败', message: '网络连接异常，请检查网络后重试。' }
}
</script>

<style scoped>
.panel-card { overflow: hidden; border: 1px solid rgb(148 163 184 / 0.18); border-radius: 8px; background: rgb(var(--surface-container-lowest)); }
.panel-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; border-bottom: 1px solid rgb(148 163 184 / 0.16); padding: 1.25rem 1.5rem; }
.panel-header h2 { font-size: 1rem; font-weight: 800; color: rgb(var(--on-surface)); }
.panel-header p { margin-top: .25rem; font-size: .75rem; color: rgb(var(--on-surface-variant)); }
.stat-card { border: 1px solid rgb(148 163 184 / 0.18); border-radius: 8px; background: rgb(var(--surface-container-lowest)); padding: 1.25rem; }
.stat-card p { font-size: .75rem; font-weight: 700; color: rgb(var(--on-surface-variant)); }
.stat-card strong { display: block; margin-top: .5rem; font-size: 1.75rem; color: rgb(var(--primary)); }
.stat-card--warning strong { color: #b45309; }
.organization-tabs { min-width: 0; padding: 0 1.25rem 1.25rem; }
.detail-scroll { min-height: 300px; max-height: 520px; overflow-y: auto; }
.member-card { border: 1px solid rgb(148 163 184 / 0.18); border-radius: 8px; padding: 1rem; }
.position-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding-bottom: .75rem; }
.position-toolbar p { font-size: .75rem; color: rgb(var(--on-surface-variant)); }
.position-table-wrap { min-height: 360px; }
.org-node { display: flex; align-items: center; gap: 1rem; border: 1px solid rgb(148 163 184 / 0.18); border-radius: 8px; background: rgb(var(--surface-container-lowest)); padding: 1rem; cursor: pointer; }
.org-node.active { border-color: rgb(var(--primary)); background: rgb(var(--primary) / 0.05); }
:deep(.organization-tabs > .el-tabs__header) { margin-bottom: 1rem; }
@media (max-width: 640px) {
  .position-toolbar { align-items: flex-start; flex-direction: column; }
}
</style>
