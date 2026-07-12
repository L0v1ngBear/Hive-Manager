<template>
  <div class="function-page-shell h-full min-h-0 font-body">
    <div class="function-page-container space-y-6">
      <header class="function-page-header">
        <div>
          <div class="function-page-eyebrow">
            <span class="material-symbols-outlined">account_tree</span>
            组织治理中心
          </div>
          <h1 class="function-page-title">部门管理</h1>
          <p class="function-page-desc">统一维护部门层级、负责人和成员归属。</p>
        </div>
        <el-button type="primary" @click="openCreate(null)">新增部门</el-button>
      </header>

      <section class="grid grid-cols-2 gap-4 md:grid-cols-4">
        <article class="stat-card"><p>部门总数</p><strong>{{ stats.departmentCount }}</strong></article>
        <article class="stat-card"><p>启用部门</p><strong>{{ stats.enabledDepartmentCount }}</strong></article>
        <article class="stat-card"><p>员工总数</p><strong>{{ stats.employeeCount }}</strong></article>
        <article class="stat-card stat-card--warning"><p>空部门</p><strong>{{ stats.emptyDepartmentCount }}</strong></article>
      </section>

      <section class="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_420px]">
        <section class="panel-card" v-loading="loading">
          <header class="panel-header">
            <div>
              <h2>部门层级</h2>
              <p>点击部门查看成员，可新增下级、编辑、停用或删除空部门。</p>
            </div>
            <el-button @click="fetchOverview">刷新</el-button>
          </header>
          <div class="p-5">
            <div v-if="departments.length" class="space-y-3">
              <DepartmentNode
                v-for="node in departments"
                :key="node.id"
                :node="node"
                :active-id="activeDepartment?.id"
                @select="selectDepartment"
                @create-child="openCreate"
                @edit="openEdit"
              />
            </div>
            <el-empty v-else-if="!loading" description="还没有部门">
              <el-button type="primary" @click="openCreate(null)">新增部门</el-button>
            </el-empty>
          </div>
        </section>

        <aside class="panel-card">
          <header class="panel-header">
            <div>
              <h2>{{ activeDepartment?.deptName || '部门成员' }}</h2>
              <p>{{ activeDepartment ? `负责人：${activeDepartment.leaderName || '未设置'} · 岗位 ${activeDepartment.positionCount || 0} 个` : '请选择左侧部门查看成员' }}</p>
            </div>
          </header>
          <div class="max-h-[560px] space-y-3 overflow-y-auto p-5" v-loading="memberLoading">
            <el-result
              v-if="memberFailure"
              :icon="memberFailure.kind === 'forbidden' ? 'warning' : 'error'"
              :title="memberFailure.title"
              :sub-title="memberFailure.message"
            >
              <template #extra>
                <el-button type="primary" :loading="memberLoading" @click="retryMembers">重试</el-button>
              </template>
            </el-result>
            <div v-else-if="members.length" class="space-y-3">
              <article v-for="item in members" :key="item.id" class="member-card">
                <div class="flex items-start justify-between gap-3">
                  <div>
                    <p class="font-bold text-primary">{{ item.name }}</p>
                    <p class="mt-1 text-xs text-on-surface-variant">{{ item.empNo || '无工号' }} / {{ item.positionName || '未设置职位' }}</p>
                  </div>
                  <el-tag :type="employeeStatusType(item.status)">{{ employeeStatusLabel(item.status) }}</el-tag>
                </div>
                <p class="mt-3 text-xs text-on-surface-variant">{{ item.phone || '未填写手机号' }}</p>
              </article>
            </div>
            <el-empty v-else-if="!memberLoading" :description="activeDepartment ? '该部门暂无员工' : '请选择部门'" />
          </div>
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
          <el-form-item label="排序">
            <el-input-number v-model="form.sortNo" :min="0" :precision="0" class="w-full" />
          </el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="flex justify-between gap-3">
          <el-button @click="closeDrawer">取消</el-button>
          <div class="flex gap-3">
            <el-button v-if="form.id" type="danger" plain @click="handleDelete">删除</el-button>
            <el-button type="primary" @click="submitDepartment">保存</el-button>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue'
import { ElButton, ElDrawer, ElEmpty, ElForm, ElFormItem, ElInput, ElInputNumber, ElMessage, ElMessageBox, ElOption, ElResult, ElSelect, ElSwitch, ElTag } from 'element-plus'
import { warnAndFocusField } from '@/utils/formFocus'
import { deleteDepartment, getDepartmentEmployees, getOrganizationOverview, saveDepartment } from './api/organization.js'

const DepartmentNode = defineComponent({
  name: 'DepartmentNode',
  props: {
    node: { type: Object, required: true },
    activeId: { type: Number, default: null },
    level: { type: Number, default: 0 }
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
          h('p', { class: 'mt-1 truncate text-xs text-on-surface-variant' }, `负责人：${props.node.leaderName || '未设置'} / 员工 ${props.node.employeeCount || 0} 人`)
        ]),
        h('div', { class: 'flex shrink-0 items-center gap-2' }, [
          h(ElTag, { type: Number(props.node.status) === 1 ? 'success' : 'info', size: 'small' }, () => Number(props.node.status) === 1 ? '启用' : '停用'),
          h(ElButton, { size: 'small', onClick: (event) => { event.stopPropagation(); emit('create-child', props.node) } }, () => '新增下级'),
          h(ElButton, { size: 'small', type: 'primary', plain: true, onClick: (event) => { event.stopPropagation(); emit('edit', props.node) } }, () => '编辑')
        ])
      ]),
      ...(props.node.children || []).map(child => h(DepartmentNode, {
        node: child,
        activeId: props.activeId,
        level: props.level + 1,
        onSelect: node => emit('select', node),
        onCreateChild: node => emit('create-child', node),
        onEdit: node => emit('edit', node)
      }))
    ])
  }
})

const loading = ref(false)
const memberLoading = ref(false)
const memberFailure = ref(null)
const drawerVisible = ref(false)
const departments = ref([])
const members = ref([])
const activeDepartment = ref(null)
const stats = reactive({ departmentCount: 0, employeeCount: 0, enabledDepartmentCount: 0, emptyDepartmentCount: 0 })
const form = reactive(createEmptyForm())
let memberRequestId = 0

const parentOptions = computed(() => flattenDepartments(departments.value).filter(option => !isSelfOrDescendant(option.id, form.id)))

onMounted(fetchOverview)

async function fetchOverview() {
  loading.value = true
  try {
    const data = await getOrganizationOverview()
    departments.value = Array.isArray(data?.departments) ? data.departments : []
    Object.assign(stats, data?.stats || {})
    if (activeDepartment.value) {
      const latest = findDepartmentById(departments.value, activeDepartment.value.id)
      if (latest) await selectDepartment(latest)
      else {
        memberRequestId += 1
        activeDepartment.value = null
        members.value = []
        memberFailure.value = null
        memberLoading.value = false
      }
    } else if (departments.value.length) await selectDepartment(departments.value[0])
  } finally {
    loading.value = false
  }
}

async function selectDepartment(node) {
  if (!node?.id) return
  const requestId = ++memberRequestId
  activeDepartment.value = node
  members.value = []
  memberFailure.value = null
  memberLoading.value = true
  try {
    const nextMembers = await getDepartmentEmployees(node.id)
    if (requestId !== memberRequestId) return
    members.value = Array.isArray(nextMembers) ? nextMembers : []
  } catch (error) {
    if (requestId !== memberRequestId) return
    memberFailure.value = resolveMemberFailure(error)
  } finally {
    if (requestId === memberRequestId) {
      memberLoading.value = false
    }
  }
}

function retryMembers() {
  if (activeDepartment.value) selectDepartment(activeDepartment.value)
}

function openCreate(parent) {
  Object.assign(form, createEmptyForm())
  form.parentId = parent?.id || ''
  drawerVisible.value = true
}

function openEdit(node) {
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
  const deptName = (form.deptName || '').trim()
  if (!deptName) {
    warnAndFocusField('请输入部门名称', 'organization.deptName')
    return
  }
  await saveDepartment({
    ...form,
    deptName,
    parentId: form.parentId === '' ? null : Number(form.parentId),
    sortNo: Number(form.sortNo ?? 99),
    status: Number(form.status ?? 1)
  })
  ElMessage.success('部门已保存')
  closeDrawer()
  await fetchOverview()
}

async function handleDelete() {
  if (!form.id) return
  await ElMessageBox.confirm('删除部门前请确认该部门下没有员工和下级部门。', '删除部门', { type: 'warning' })
  await deleteDepartment(form.id)
  ElMessage.success('部门已删除')
  closeDrawer()
  memberRequestId += 1
  activeDepartment.value = null
  members.value = []
  memberFailure.value = null
  memberLoading.value = false
  await fetchOverview()
}

function createEmptyForm() {
  return { id: null, parentId: '', deptName: '', deptCode: '', leaderName: '', sortNo: 99, status: 1 }
}

function flattenDepartments(nodes, level = 0) {
  return (nodes || []).flatMap(node => [
    { id: node.id, label: `${'　'.repeat(level)}${node.deptName || '未命名部门'}` },
    ...flattenDepartments(node.children || [], level + 1)
  ])
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
  const rawStatusCode = error?.response?.status
    ?? error?.response?.data?.code
    ?? error?.statusCode
    ?? error?.code
  const statusCode = Number(rawStatusCode)
  return Number.isFinite(statusCode) ? statusCode : 0
}

function resolveMemberFailure(error) {
  const statusCode = getRequestStatusCode(error)
  if (statusCode === 401) {
    return { kind: 'unauthorized', title: '登录状态已失效', message: '请重新登录后再重试部门成员。' }
  }
  if (statusCode === 403) {
    return { kind: 'forbidden', title: '暂无权限查看部门成员', message: '请联系管理员确认员工查看权限。' }
  }
  if (statusCode >= 500) {
    return { kind: 'request', title: '部门成员加载失败', message: '服务暂时不可用，请稍后重试。' }
  }
  return { kind: 'request', title: '部门成员加载失败', message: '网络连接异常，请检查网络后重试。' }
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
.member-card { border: 1px solid rgb(148 163 184 / 0.18); border-radius: 8px; padding: 1rem; }
.org-node { display: flex; align-items: center; gap: 1rem; border: 1px solid rgb(148 163 184 / 0.18); border-radius: 8px; background: white; padding: 1rem; cursor: pointer; }
.org-node.active { border-color: rgb(var(--primary)); background: rgb(var(--primary) / 0.05); }
</style>
