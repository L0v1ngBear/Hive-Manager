<template>
  <transition
      enter-active-class="transition-opacity duration-300"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-300"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
  >
    <div
        v-if="visible"
        class="fixed inset-0 z-40 bg-slate-900/30 backdrop-blur-[2px]"
        @click="close"
    ></div>
  </transition>

  <el-drawer
      v-model="visible"
      :with-header="false"
      size="680px"
  >
    <div class="flex h-full flex-col">
    <div class="border-t-[4px] border-primary px-8 py-6">
      <div class="flex items-start justify-between gap-4">
        <div>
          <h2 class="text-2xl font-black tracking-tight text-primary">员工单独权限</h2>
          <p class="mt-1 text-sm text-on-surface-variant">
            {{ currentEmployee?.name || '--' }} 的个人权限覆盖，不会修改角色权限。
          </p>
        </div>
        <el-button
            text
            class="rounded-full p-2 text-on-surface-variant transition-colors hover:bg-surface-container-high hover:text-primary"
            @click="close"
        >
          <span class="material-symbols-outlined">close</span>
        </el-button>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto px-8 py-5">
      <div v-if="permissionState.kind === 'loading'" class="flex min-h-[320px] items-center justify-center text-primary">
        <span class="material-symbols-outlined animate-spin text-4xl">progress_activity</span>
      </div>

      <div v-else-if="permissionState.kind === 'forbidden'" class="flex min-h-[320px] flex-col items-center justify-center gap-3 text-center">
        <span class="material-symbols-outlined text-5xl text-primary">lock</span>
        <p class="font-bold text-on-surface">{{ permissionState.title }}</p>
        <p class="text-sm text-on-surface-variant">{{ permissionState.message }}</p>
      </div>
      <div v-else-if="permissionState.kind === 'failed'" class="flex min-h-[320px] flex-col items-center justify-center gap-3 text-center">
        <span class="material-symbols-outlined text-5xl text-primary">cloud_off</span>
        <p class="font-bold text-on-surface">{{ permissionState.title }}</p>
        <p class="text-sm text-on-surface-variant">{{ permissionState.message }}</p>
        <el-button type="primary" @click="retry">重新加载</el-button>
      </div>
      <div v-else-if="permissionState.kind === 'empty'" class="flex min-h-[320px] items-center justify-center">
        <el-empty description="暂无可配置权限" />
      </div>

      <template v-else-if="permissionState.kind === 'ready'">
      <div class="space-y-5">

        <section class="rounded-lg border border-outline-variant/20 bg-white p-4">
          <div class="flex items-center justify-between">
            <div>
              <h3 class="text-sm font-black text-primary">角色继承权限</h3>
              <p class="mt-1 text-xs text-on-surface-variant">来自员工已分配角色，仅作参考。</p>
            </div>
            <span class="rounded bg-primary/10 px-3 py-1 text-xs font-black text-primary">
              {{ rolePermissionIds.length }} 项
            </span>
          </div>
          <div
              v-if="rolePermissionIds.length > 0"
              class="mt-3 max-h-56 overflow-y-auto rounded-md border border-primary/10 bg-surface-container-lowest p-2"
          >
            <el-tree
                ref="roleTreeRef"
                :data="treeData"
                node-key="id"
                show-checkbox
                default-expand-all
                :default-checked-keys="rolePermissionIds"
                :expand-on-click-node="false"
                :render-after-expand="false"
                :props="readonlyTreeProps"
                class="permission-readonly-tree"
            />
          </div>
          <p v-else class="mt-3 rounded-md bg-surface-container-high px-3 py-2 text-xs font-bold text-on-surface-variant">
            当前角色暂无继承权限
          </p>
        </section>

        <section class="rounded-lg border border-emerald-200 bg-emerald-50/50 p-4">
          <div class="mb-3 flex items-center justify-between">
            <div>
              <h3 class="text-sm font-black text-emerald-700">额外允许</h3>
              <p class="mt-1 text-xs text-emerald-700/70">角色没有给，但这个员工个人可以使用。</p>
            </div>
            <span class="rounded bg-emerald-100 px-2 py-1 text-xs font-black text-emerald-700">
              {{ grantPermissionIds.length }} 项
            </span>
          </div>
          <el-tree-select
              v-model="grantPermissionIds"
              :data="treeData"
              node-key="id"
              value-key="id"
              multiple
              show-checkbox
              collapse-tags
              collapse-tags-tooltip
              filterable
              check-on-click-node
              :render-after-expand="false"
              :props="treeProps"
              placeholder="选择要额外允许的权限"
              class="permission-tree-select w-full"
          />
        </section>

        <section class="rounded-lg border border-amber-200 bg-amber-50/60 p-4">
          <div class="mb-3 flex items-center justify-between">
            <div>
              <h3 class="text-sm font-black text-amber-800">单独禁用</h3>
              <p class="mt-1 text-xs text-amber-800/70">即使角色包含该权限，这个员工也不能使用。</p>
            </div>
            <span class="rounded bg-amber-100 px-2 py-1 text-xs font-black text-amber-800">
              {{ denyPermissionIds.length }} 项
            </span>
          </div>
          <el-tree-select
              v-model="denyPermissionIds"
              :data="treeData"
              node-key="id"
              value-key="id"
              multiple
              show-checkbox
              collapse-tags
              collapse-tags-tooltip
              filterable
              check-on-click-node
              :render-after-expand="false"
              :props="treeProps"
              placeholder="选择要单独禁用的权限"
              class="permission-tree-select w-full"
          />
        </section>

        <div v-if="overlapCount > 0" class="rounded-lg bg-error-container px-4 py-3 text-sm font-bold text-error">
          有 {{ overlapCount }} 项权限同时出现在允许和禁用里，请先取消其中一边。
        </div>
      </div>
      </template>
    </div>

    <div class="grid grid-cols-2 gap-4 border-t border-outline-variant/20 bg-white px-8 py-6">
      <el-button
          class="rounded-lg px-6 py-3 text-sm font-bold text-on-surface-variant transition-colors hover:bg-surface-container-high"
          @click="close"
      >
        取消
      </el-button>
      <el-button
          type="primary"
          :loading="submitting"
          :disabled="submitting || permissionState.kind !== 'ready' || overlapCount > 0"
          class="flex items-center justify-center gap-2 rounded-lg bg-primary px-6 py-3 text-sm font-bold text-white shadow-lg shadow-primary/20 transition-all hover:bg-primary/90 active:scale-95 disabled:cursor-not-allowed disabled:opacity-50"
          @click="save"
      >
        保存权限
      </el-button>
    </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { ElButton, ElDrawer, ElEmpty, ElMessage, ElTree, ElTreeSelect } from 'element-plus'
import { createLatestRequest } from '@/utils/latestRequest'
import { getAllPermissions } from '../role/api/role.js'
import {
  getEmployeePermissionOverrides,
  updateEmployeePermissionOverrides
} from './api/employee.js'

const emit = defineEmits(['updated', 'closed'])

const visible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const permissionState = ref({ kind: 'loading', title: '', message: '' })
const currentEmployee = ref(null)
const treeData = ref([])
const rolePermissionIds = ref([])
const grantPermissionIds = ref([])
const denyPermissionIds = ref([])
const treeProps = { value: 'id', label: 'permName', children: 'children' }
const readonlyTreeProps = { value: 'id', label: 'permName', children: 'children', disabled: () => true }
const roleTreeRef = ref(null)
const permissionRequest = createLatestRequest()

const overlapCount = computed(() => {
  const denySet = new Set(denyPermissionIds.value.map((id) => Number(id)))
  return grantPermissionIds.value.filter((id) => denySet.has(Number(id))).length
})

async function open(employee) {
  if (!employee?.id) return
  currentEmployee.value = employee
  visible.value = true
  await load(employee)
}

async function load(employee) {
  const request = permissionRequest.begin()
  loading.value = true
  permissionState.value = { kind: 'loading', title: '', message: '' }
  treeData.value = []
  rolePermissionIds.value = []
  grantPermissionIds.value = []
  denyPermissionIds.value = []

  try {
    const [permissions, overrides] = await Promise.all([
      getAllPermissions(),
      getEmployeePermissionOverrides(employee.id)
    ])
    const permissionTree = permissions?.data?.data || permissions?.data || permissions || []
    const overrideData = overrides?.data?.data || overrides?.data || overrides || {}
    if (!request.isLatest()) return
    treeData.value = Array.isArray(permissionTree) ? permissionTree : []
    await nextTick()
    if (!request.isLatest()) return
    rolePermissionIds.value = normalizeIds(overrideData?.rolePermissionIds)
    grantPermissionIds.value = normalizeIds(overrideData?.grantPermissionIds)
    denyPermissionIds.value = normalizeIds(overrideData?.denyPermissionIds)
    await nextTick()
    if (!request.isLatest()) return
    roleTreeRef.value?.setCheckedKeys(rolePermissionIds.value, false)
    request.commit(() => {
      permissionState.value = treeData.value.length
        ? { kind: 'ready', title: '', message: '' }
        : { kind: 'empty', title: '暂无可配置权限', message: '当前组织尚未配置可分配权限。' }
    })
  } catch (error) {
    console.error('[Hive Auth] load employee permission overrides failed:', error)
    request.commit(() => { permissionState.value = resolvePermissionFailure(error) })
  } finally {
    request.commit(() => { loading.value = false })
  }
}

function resolvePermissionFailure(error) {
  const status = Number(error?.response?.status ?? error?.response?.data?.code ?? error?.status ?? error?.code ?? 0)
  if (status === 401 || status === 403) {
    return {
      kind: 'forbidden',
      title: status === 401 ? '登录状态已失效' : '暂无权限查看员工权限',
      message: status === 401 ? '请重新登录后再试。' : '请联系管理员分配员工编辑和角色权限查看权限。'
    }
  }
  return {
    kind: 'failed',
    title: status >= 500 ? '权限服务暂时不可用' : '权限数据加载失败',
    message: status >= 500 ? '服务处理失败，请稍后重试。' : '请检查网络连接后重试。'
  }
}

function retry() {
  if (currentEmployee.value) load(currentEmployee.value)
}

function close() {
  visible.value = false
  emit('closed')
}

async function save() {
  if (!currentEmployee.value?.id || overlapCount.value > 0) return
  submitting.value = true
  try {
    await updateEmployeePermissionOverrides({
      userId: Number(currentEmployee.value.id),
      grantPermissionIds: uniqueIds(grantPermissionIds.value),
      denyPermissionIds: uniqueIds(denyPermissionIds.value)
    })
    ElMessage.success('员工单独权限已保存')
    emit('updated')
    close()
  } finally {
    submitting.value = false
  }
}

function normalizeIds(value) {
  if (!Array.isArray(value)) return []
  return uniqueIds(value)
}

function uniqueIds(value) {
  return [...new Set((value || []).map((id) => Number(id)).filter((id) => Number.isFinite(id) && id > 0))]
}

defineExpose({ open })
</script>

<style scoped>
:deep(.permission-tree-select .el-select__wrapper) {
  min-height: 44px;
  border-radius: 6px;
  box-shadow: 0 0 0 1px rgba(148, 163, 184, .25) inset;
}

:deep(.permission-readonly-tree) {
  background: transparent;
}

:deep(.permission-readonly-tree .el-tree-node__content) {
  min-height: 30px;
}

:deep(.permission-readonly-tree .el-checkbox__input.is-disabled.is-checked .el-checkbox__inner) {
  background-color: #1f3f5f;
  border-color: #1f3f5f;
}

:deep(.permission-readonly-tree .el-checkbox__input.is-disabled.is-checked .el-checkbox__inner::after) {
  border-color: #ffffff;
}
</style>
