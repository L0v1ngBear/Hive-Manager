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

  <aside
      class="fixed right-0 top-0 z-50 flex h-full w-full max-w-4xl flex-col border-l border-outline-variant/30 bg-surface-container-lowest shadow-2xl transition-transform duration-300"
      :class="visible ? 'translate-x-0' : 'translate-x-full'"
  >
    <header class="border-t-[4px] border-primary px-7 py-5">
      <div class="flex items-start justify-between gap-4">
        <div class="min-w-0">
          <h2 class="text-2xl font-black text-primary">员工个人权限</h2>
          <p class="mt-1 text-sm text-on-surface-variant">
            {{ currentEmployee?.name || '--' }} 的个人权限仅影响本人，不修改所属角色。
          </p>
        </div>
        <button
            type="button"
            class="rounded-full p-2 text-on-surface-variant transition-colors hover:bg-surface-container-high hover:text-primary"
            title="关闭"
            @click="close"
        >
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
    </header>

    <div class="flex-1 overflow-y-auto border-t border-outline-variant/20">
      <div v-if="loading" class="flex min-h-[360px] items-center justify-center text-primary">
        <span class="material-symbols-outlined animate-spin text-4xl">progress_activity</span>
      </div>

      <div v-else>
        <div v-if="loadError" class="m-6 rounded-md bg-error-container px-4 py-3 text-sm font-bold text-error">
          {{ loadError }}
        </div>

        <template v-else>
          <section class="border-b border-outline-variant/20 px-7 py-4">
            <div class="flex flex-wrap items-center gap-2">
              <span class="text-xs font-bold text-on-surface-variant">所属角色</span>
              <span
                  v-for="role in roles"
                  :key="role.roleId"
                  class="rounded bg-primary/10 px-2.5 py-1 text-xs font-bold text-primary"
              >
                {{ role.roleName }}
              </span>
              <span v-if="roles.length === 0" class="text-xs text-on-surface-variant">未配置角色</span>
              <span class="ml-auto text-xs text-on-surface-variant">配置版本 {{ permissionVersion }}</span>
            </div>
          </section>

          <section class="sticky top-0 z-10 border-b border-outline-variant/20 bg-white/95 px-7 py-3 backdrop-blur">
            <div class="flex items-center gap-3">
              <el-input
                  v-model="keyword"
                  clearable
                  placeholder="搜索权限名称或编码"
                  class="max-w-md"
                  @input="filterTree"
              >
                <template #prefix>
                  <span class="material-symbols-outlined text-[18px]">search</span>
                </template>
              </el-input>
              <button type="button" class="permission-tool" title="全部展开" @click="setExpanded(true)">
                <span class="material-symbols-outlined">unfold_more</span>
              </button>
              <button type="button" class="permission-tool" title="全部收起" @click="setExpanded(false)">
                <span class="material-symbols-outlined">unfold_less</span>
              </button>
              <div class="ml-auto hidden items-center gap-4 text-xs font-bold text-on-surface-variant sm:flex">
                <span>个人允许 {{ grantCount }}</span>
                <span>个人禁用 {{ denyCount }}</span>
                <span>最终有效 {{ effectiveCount }}</span>
              </div>
            </div>
          </section>

          <section class="px-5 py-4">
            <el-tree
                ref="permissionTreeRef"
                :data="treeData"
                node-key="code"
                default-expand-all
                :expand-on-click-node="false"
                :filter-node-method="filterPermissionNode"
                :props="treeProps"
                class="permission-profile-tree"
            >
              <template #default="{ data }">
                <div
                    class="permission-row"
                    :class="data.assignable ? 'permission-leaf' : 'permission-group'"
                >
                  <div class="min-w-0 flex-1">
                    <div class="flex flex-wrap items-center gap-2">
                      <span class="truncate font-bold text-primary">{{ data.name }}</span>
                      <code v-if="data.assignable" class="permission-code">{{ data.code }}</code>
                      <span
                          v-if="data.assignable"
                          class="permission-result"
                          :class="isEffective(data) ? 'is-enabled' : 'is-disabled'"
                      >
                        {{ isEffective(data) ? '已生效' : '未生效' }}
                      </span>
                    </div>
                    <p v-if="data.assignable" class="mt-1 truncate text-xs text-on-surface-variant">
                      {{ roleSourceText(data) }}
                    </p>
                  </div>

                  <el-segmented
                      v-if="data.assignable"
                      v-model="overrideByCode[data.code]"
                      :options="overrideOptions"
                      size="small"
                      class="permission-segmented"
                  />
                </div>
              </template>
            </el-tree>
          </section>
        </template>
      </div>
    </div>

    <footer class="grid grid-cols-2 gap-4 border-t border-outline-variant/20 bg-white px-7 py-5">
      <button
          type="button"
          class="rounded-md px-6 py-3 text-sm font-bold text-on-surface-variant transition-colors hover:bg-surface-container-high"
          @click="close"
      >
        取消
      </button>
      <button
          type="button"
          :disabled="submitting || loading || !!loadError"
          class="flex items-center justify-center gap-2 rounded-md bg-primary px-6 py-3 text-sm font-bold text-white shadow-lg shadow-primary/20 transition-all hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
          @click="save"
      >
        <span v-if="submitting" class="material-symbols-outlined animate-spin text-[18px]">progress_activity</span>
        保存个人权限
      </button>
    </footer>
  </aside>
</template>

<script setup>
import { computed, nextTick, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getEmployeePermissionProfile,
  updateEmployeePermissionOverrides
} from './api/employee.js'

const emit = defineEmits(['updated', 'closed'])
const visible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const loadError = ref('')
const currentEmployee = ref(null)
const permissionVersion = ref(0)
const roles = ref([])
const treeData = ref([])
const keyword = ref('')
const permissionTreeRef = ref(null)
const overrideByCode = reactive({})

const treeProps = { children: 'children', label: 'name' }
const overrideOptions = [
  { label: '继承角色', value: 'INHERIT' },
  { label: '个人允许', value: 'GRANT' },
  { label: '个人禁用', value: 'DENY' }
]

const leafNodes = computed(() => flatten(treeData.value).filter((node) => node.assignable))
const grantCount = computed(() => leafNodes.value.filter((node) => overrideByCode[node.code] === 'GRANT').length)
const denyCount = computed(() => leafNodes.value.filter((node) => overrideByCode[node.code] === 'DENY').length)
const effectiveCount = computed(() => leafNodes.value.filter(isEffective).length)

async function open(employee) {
  if (!employee?.id) return
  currentEmployee.value = employee
  visible.value = true
  await loadProfile()
}

async function loadProfile() {
  loading.value = true
  loadError.value = ''
  keyword.value = ''
  clearOverrides()
  try {
    const response = await getEmployeePermissionProfile(currentEmployee.value.id)
    const profile = response?.data?.data || response?.data || response || {}
    permissionVersion.value = Number(profile.permissionVersion || 0)
    roles.value = Array.isArray(profile.roles) ? profile.roles : []
    treeData.value = Array.isArray(profile.permissions) ? profile.permissions : []
    for (const node of flatten(treeData.value)) {
      if (!node.assignable) continue
      overrideByCode[node.code] = node.personalEffect === 'GRANT' || node.personalEffect === 'DENY'
        ? node.personalEffect
        : 'INHERIT'
    }
    await nextTick()
    permissionTreeRef.value?.filter('')
  } catch (error) {
    console.error('[Hive Auth] load employee permission profile failed:', error)
    loadError.value = '权限档案加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

function close() {
  visible.value = false
  emit('closed')
}

async function save() {
  if (!currentEmployee.value?.id || !permissionVersion.value) return
  submitting.value = true
  try {
    const grants = leafNodes.value
      .filter((node) => overrideByCode[node.code] === 'GRANT')
      .map((node) => node.code)
    const denies = leafNodes.value
      .filter((node) => overrideByCode[node.code] === 'DENY')
      .map((node) => node.code)
    await updateEmployeePermissionOverrides(currentEmployee.value.id, {
      permissionVersion: permissionVersion.value,
      grants,
      denies
    })
    ElMessage.success('员工个人权限已保存')
    emit('updated')
    close()
  } catch (error) {
    if (Number(error?.code || error?.response?.data?.code || error?.statusCode) === 409) {
      ElMessage.warning('权限已被其他人修改，已为你刷新最新配置')
      await loadProfile()
      return
    }
    throw error
  } finally {
    submitting.value = false
  }
}

function isEffective(node) {
  const override = overrideByCode[node.code]
  if (override === 'DENY') return false
  if (override === 'GRANT') return true
  return Boolean(node.roleGranted)
}

function roleSourceText(node) {
  const names = (node.roleSources || []).map((role) => role.roleName).filter(Boolean)
  return names.length > 0 ? `角色来源：${names.join('、')}` : '角色未授权'
}

function filterTree() {
  permissionTreeRef.value?.filter(keyword.value.trim())
}

function filterPermissionNode(value, data) {
  if (!value) return true
  const normalized = value.toLowerCase()
  return String(data.name || '').toLowerCase().includes(normalized)
    || String(data.code || '').toLowerCase().includes(normalized)
}

function setExpanded(expanded) {
  const visit = (nodes) => {
    for (const node of nodes || []) {
      node.expanded = expanded
      visit(node.childNodes)
    }
  }
  visit(permissionTreeRef.value?.store?.root?.childNodes)
}

function flatten(nodes) {
  return (nodes || []).flatMap((node) => [node, ...flatten(node.children)])
}

function clearOverrides() {
  Object.keys(overrideByCode).forEach((code) => delete overrideByCode[code])
}

defineExpose({ open })
</script>

<style scoped>
.permission-tool {
  display: inline-flex;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(148, 163, 184, .35);
  border-radius: 6px;
  color: #1f3f5f;
  background: white;
}

.permission-row {
  display: flex;
  min-height: 52px;
  width: 100%;
  align-items: center;
  gap: 16px;
  padding: 7px 8px 7px 2px;
}

.permission-group {
  min-height: 42px;
  border-bottom: 1px solid rgba(148, 163, 184, .18);
}

.permission-leaf {
  border-bottom: 1px solid rgba(148, 163, 184, .12);
}

.permission-code {
  border-radius: 4px;
  background: #eef3f8;
  padding: 2px 6px;
  color: #536b82;
  font-size: 11px;
}

.permission-result {
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 11px;
  font-weight: 700;
}

.permission-result.is-enabled {
  background: #dcfce7;
  color: #047857;
}

.permission-result.is-disabled {
  background: #f1f5f9;
  color: #64748b;
}

:deep(.permission-profile-tree) {
  background: transparent;
}

:deep(.permission-profile-tree .el-tree-node__content) {
  height: auto;
  align-items: stretch;
}

:deep(.permission-profile-tree .el-tree-node__expand-icon) {
  align-self: center;
}

:deep(.permission-segmented) {
  flex: 0 0 auto;
  min-width: 250px;
}

@media (max-width: 640px) {
  .permission-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  :deep(.permission-segmented) {
    width: 100%;
    min-width: 0;
  }
}
</style>
