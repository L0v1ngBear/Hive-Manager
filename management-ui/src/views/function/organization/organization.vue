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
          <p class="function-page-desc">
            统一维护部门层级、负责人和成员归属。员工管理、考勤筛选等需要部门数据的功能都会读取这里的数据。
          </p>
        </div>
        <button class="function-action-primary" @click="openCreate(null)">
          <span class="material-symbols-outlined text-[18px] align-middle mr-1">add</span>
          新增部门
        </button>
      </header>

      <section class="grid grid-cols-2 gap-4 md:grid-cols-4">
        <article class="stat-card">
          <p class="stat-label">部门总数</p>
          <p class="stat-value">{{ stats.departmentCount }}</p>
        </article>
        <article class="stat-card">
          <p class="stat-label">启用部门</p>
          <p class="stat-value">{{ stats.enabledDepartmentCount }}</p>
        </article>
        <article class="stat-card">
          <p class="stat-label">员工总数</p>
          <p class="stat-value">{{ stats.employeeCount }}</p>
        </article>
        <article class="stat-card stat-card--warning">
          <p class="stat-label text-amber-700">空部门</p>
          <p class="stat-value text-amber-700">{{ stats.emptyDepartmentCount }}</p>
        </article>
      </section>

      <section class="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_420px]">
        <div class="panel-card overflow-hidden">
          <div class="flex items-center justify-between border-b border-outline-variant/10 px-6 py-4">
            <div>
              <h2 class="text-lg font-black text-on-surface">部门层级</h2>
              <p class="mt-1 text-xs text-on-surface-variant">点击部门查看成员；可新增下级、编辑、停用或删除空部门。</p>
            </div>
            <button class="rounded-lg bg-surface-container-high px-4 py-2 text-xs font-bold text-on-surface" @click="fetchOverview">
              刷新
            </button>
          </div>

          <div class="relative min-h-[420px] p-5">
            <div v-if="loading" class="loading-mask">
              <span class="material-symbols-outlined animate-spin text-3xl text-primary">progress_activity</span>
            </div>

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

            <div v-else-if="!loading" class="empty-card">
              <span class="material-symbols-outlined text-5xl text-primary">account_tree</span>
              <p class="mt-3 text-sm font-bold text-on-surface">还没有部门</p>
              <p class="mt-1 text-xs text-on-surface-variant">先创建第一个部门，再把员工分配进来。</p>
              <button class="mt-4 rounded-xl bg-primary px-5 py-2 text-sm font-bold text-white" @click="openCreate(null)">
                新增部门
              </button>
            </div>
          </div>
        </div>

        <aside class="panel-card overflow-hidden">
          <div class="border-b border-outline-variant/10 px-6 py-4">
            <h2 class="text-lg font-black text-on-surface">{{ activeDepartment?.deptName || '部门成员' }}</h2>
            <p class="mt-1 text-xs text-on-surface-variant">
              {{ activeDepartment ? `负责人：${activeDepartment.leaderName || '未设置'} · 岗位 ${activeDepartment.positionCount || 0} 个` : '请选择左侧部门查看成员' }}
            </p>
          </div>
          <div class="max-h-[560px] space-y-3 overflow-y-auto p-5">
            <div v-if="memberLoading" class="py-10 text-center text-on-surface-variant">成员加载中...</div>
            <div v-else-if="members.length" class="space-y-3">
              <div v-for="item in members" :key="item.id" class="rounded-xl border border-outline-variant/20 bg-surface-container-low p-4">
                <div class="flex items-start justify-between gap-3">
                  <div>
                    <p class="font-black text-primary">{{ item.name }}</p>
                    <p class="mt-1 text-xs text-on-surface-variant">
                      {{ item.empNo || '无工号' }} / {{ item.positionName || '未设置职位' }}
                    </p>
                  </div>
                  <span :class="employeeStatusClass(item.status)" class="rounded-full px-2 py-0.5 text-[10px] font-bold">
                    {{ employeeStatusLabel(item.status) }}
                  </span>
                </div>
                <p class="mt-3 text-xs text-on-surface-variant">{{ item.phone || '未填写手机号' }}</p>
              </div>
            </div>
            <div v-else class="rounded-2xl bg-surface-container-low p-8 text-center text-sm text-on-surface-variant">
              {{ activeDepartment ? '该部门暂无员工。' : '请选择部门。' }}
            </div>
          </div>
        </aside>
      </section>
    </div>

    <transition name="fade">
      <div v-if="drawerVisible" class="fixed inset-0 z-40 bg-black/20 backdrop-blur-[2px]" @click="closeDrawer"></div>
    </transition>

    <aside
      class="fixed right-0 top-0 z-50 flex h-full w-full max-w-[460px] flex-col border-l border-outline-variant/30 bg-white/95 shadow-2xl backdrop-blur-2xl transition-transform duration-300"
      :class="drawerVisible ? 'translate-x-0' : 'translate-x-full'"
    >
      <div class="h-1 bg-primary"></div>
      <div class="flex items-start justify-between border-b p-6">
        <div>
          <h3 class="text-lg font-black text-primary">{{ form.id ? '编辑部门' : '新增部门' }}</h3>
          <p class="mt-1 text-xs text-on-surface-variant">部门名称会同步影响员工管理中的部门归属展示。</p>
        </div>
        <button class="rounded-full p-1 hover:bg-surface-container-high" @click="closeDrawer">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>

      <div class="flex-1 space-y-5 overflow-y-auto p-6">
        <label class="block">
          <span class="field-label">部门名称 *</span>
          <input
            v-model.trim="form.deptName"
            data-field="organization.deptName"
            class="box-input"
            maxlength="64"
            placeholder="例如：销售部、仓储部"
          />
        </label>
        <label class="block">
          <span class="field-label">部门编码</span>
          <input v-model.trim="form.deptCode" class="box-input" maxlength="64" placeholder="不填则自动生成" />
        </label>
        <label class="block">
          <span class="field-label">上级部门</span>
          <select v-model="form.parentId" class="box-input">
            <option value="">作为一级部门</option>
            <option v-for="option in parentOptions" :key="option.id" :value="option.id" :disabled="option.id === form.id">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label class="block">
          <span class="field-label">负责人</span>
          <input v-model.trim="form.leaderName" class="box-input" maxlength="50" placeholder="请输入负责人姓名" />
        </label>
        <div class="grid grid-cols-2 gap-4">
          <label class="block">
            <span class="field-label">排序</span>
            <input v-model.number="form.sortNo" type="number" class="box-input" />
          </label>
          <label class="block">
            <span class="field-label">状态</span>
            <select v-model.number="form.status" class="box-input">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </label>
        </div>
      </div>

      <div class="flex gap-3 border-t p-6">
        <button class="flex-1 rounded-xl bg-surface-container-high px-4 py-3 text-sm font-bold" @click="closeDrawer">取消</button>
        <button v-if="form.id" class="rounded-xl bg-error-container px-4 py-3 text-sm font-bold text-error" @click="handleDelete">删除</button>
        <button class="flex-1 rounded-xl bg-primary px-4 py-3 text-sm font-bold text-white" @click="submitDepartment">保存</button>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
        h('div', { class: 'flex min-w-0 flex-1 items-center gap-3' }, [
          h('span', { class: 'material-symbols-outlined org-node-icon' }, props.node.children?.length ? 'account_tree' : 'domain'),
          h('div', { class: 'min-w-0' }, [
            h('p', { class: 'truncate text-sm font-black text-primary' }, props.node.deptName || '未命名部门'),
            h('p', { class: 'mt-1 truncate text-xs text-on-surface-variant' }, `负责人：${props.node.leaderName || '未设置'} / 员工 ${props.node.employeeCount || 0} 人`)
          ])
        ]),
        h('div', { class: 'flex shrink-0 items-center gap-2' }, [
          h('span', { class: ['rounded-full px-2 py-0.5 text-[10px] font-bold', Number(props.node.status) === 1 ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-600'] }, Number(props.node.status) === 1 ? '启用' : '停用'),
          h('button', { class: 'org-node-btn', onClick: (event) => { event.stopPropagation(); emit('create-child', props.node) } }, '+'),
          h('button', { class: 'org-node-btn', onClick: (event) => { event.stopPropagation(); emit('edit', props.node) } }, '编辑')
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
const drawerVisible = ref(false)
const departments = ref([])
const members = ref([])
const activeDepartment = ref(null)
const stats = reactive({ departmentCount: 0, employeeCount: 0, enabledDepartmentCount: 0, emptyDepartmentCount: 0 })
const form = reactive(createEmptyForm())

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
      if (latest) {
        await selectDepartment(latest)
      } else {
        activeDepartment.value = null
        members.value = []
      }
    } else if (departments.value.length) {
      await selectDepartment(departments.value[0])
    }
  } finally {
    loading.value = false
  }
}

async function selectDepartment(node) {
  if (!node?.id) {
    return
  }
  activeDepartment.value = node
  memberLoading.value = true
  try {
    members.value = await getDepartmentEmployees(node.id)
  } finally {
    memberLoading.value = false
  }
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
    sortNo: Number(form.sortNo || 99),
    status: Number(form.status ?? 1)
  })
  ElMessage.success('部门已保存')
  closeDrawer()
  await fetchOverview()
}

async function handleDelete() {
  if (!form.id) {
    return
  }
  await ElMessageBox.confirm('删除部门前请确认该部门下没有员工和下级部门。', '删除部门', { type: 'warning' })
  await deleteDepartment(form.id)
  ElMessage.success('部门已删除')
  closeDrawer()
  activeDepartment.value = null
  members.value = []
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
    if (node.id === id) {
      return node
    }
    const found = findDepartmentById(node.children || [], id)
    if (found) {
      return found
    }
  }
  return null
}

function isSelfOrDescendant(candidateId, currentId) {
  if (!currentId || !candidateId) {
    return false
  }
  if (candidateId === currentId) {
    return true
  }
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

function employeeStatusClass(status) {
  if (Number(status) === 1) return 'bg-emerald-100 text-emerald-700'
  if (Number(status) === 2) return 'bg-amber-100 text-amber-700'
  return 'bg-slate-100 text-slate-600'
}
</script>

<style scoped>
.panel-card {
  border-radius: 1rem;
  border: 1px solid rgba(148, 163, 184, .18);
  background: rgb(var(--surface-container-lowest));
  box-shadow: 0 1px 2px rgba(15, 23, 42, .05);
}

.stat-card {
  border-radius: 1rem;
  border: 1px solid rgba(0, 82, 204, .1);
  background: rgb(var(--surface-container-lowest));
  padding: 1.25rem;
  box-shadow: 0 1px 2px rgba(15, 23, 42, .05);
}

.stat-card--warning {
  border-color: rgba(251, 191, 36, .35);
  background: rgb(255 251 235);
}

.stat-label {
  font-size: .7rem;
  font-weight: 900;
  letter-spacing: .18em;
  text-transform: uppercase;
  color: rgb(var(--on-surface-variant));
}

.stat-value {
  margin-top: .6rem;
  font-size: 2rem;
  line-height: 2.2rem;
  font-weight: 900;
  color: rgb(var(--primary));
}

.loading-mask {
  position: absolute;
  inset: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, .62);
  backdrop-filter: blur(4px);
}

.empty-card {
  display: flex;
  min-height: 360px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 1rem;
  background: rgb(var(--surface-container-low));
  text-align: center;
}

.box-input {
  width: 100%;
  border-radius: .75rem;
  border: 1px solid rgba(148, 163, 184, .28);
  background: rgb(var(--surface-container-low));
  padding: .75rem 1rem;
  font-size: .875rem;
  outline: none;
}

.box-input:focus {
  border-color: rgb(var(--primary));
  box-shadow: 0 0 0 3px rgba(0, 82, 204, .12);
}

.field-label {
  margin-bottom: .5rem;
  display: block;
  font-size: .75rem;
  font-weight: 900;
  letter-spacing: .08em;
  text-transform: uppercase;
  color: rgb(var(--primary));
}

.org-node-wrap {
  position: relative;
}

.org-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  border-radius: 1rem;
  border: 1px solid rgba(148, 163, 184, .18);
  background: white;
  padding: 1rem;
  transition: all .2s ease;
}

.org-node:hover {
  border-color: rgba(0, 82, 204, .28);
  box-shadow: 0 8px 24px rgba(15, 23, 42, .06);
  transform: translateY(-1px);
}

.org-node.active {
  border-color: rgb(var(--primary));
  background: rgba(0, 82, 204, .05);
}

.org-node-icon {
  display: flex;
  height: 2.75rem;
  width: 2.75rem;
  align-items: center;
  justify-content: center;
  border-radius: .9rem;
  background: rgba(0, 82, 204, .1);
  font-size: 1.8rem;
  line-height: 1;
  color: rgb(var(--primary));
}

.org-node-btn {
  border-radius: .6rem;
  background: rgb(var(--surface-container-high));
  padding: .35rem .6rem;
  font-size: .75rem;
  font-weight: 900;
  color: rgb(var(--primary));
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity .25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
