<template>
  <el-drawer
      v-model="visible"
      :title="null"
      size="500px"
      direction="rtl"
      :with-header="false"
      aria-labelledby="role-permission-title"
      class="atelier-drawer"
      append-to-body
      destroy-on-close
  >
    <div class="h-1 bg-primary w-full sticky top-0 z-10"></div>

    <div class="flex flex-col h-full bg-surface-container-lowest relative">
      <div v-if="permissionLoader.state.loading" class="absolute inset-0 bg-white/60 backdrop-blur-sm z-20 flex flex-col items-center justify-center">
        <span class="material-symbols-outlined text-primary text-4xl animate-spin">progress_activity</span>
        <span class="text-sm font-bold text-primary mt-2">正在同步权限矩阵...</span>
      </div>

      <div class="px-5 py-6 sm:px-8 sm:py-8 border-b border-surface-variant/30 flex-shrink-0">
        <div class="flex justify-between items-start mb-6">
          <div class="p-3 bg-primary-container text-white rounded-xl shadow-lg shadow-primary/10">
            <span class="material-symbols-outlined text-3xl">security</span>
          </div>
          <el-button circle text class="p-2 hover:bg-surface-container-high rounded-full transition-colors group" @click="close">
            <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary">close</span>
          </el-button>
        </div>
        <h2 id="role-permission-title" class="text-2xl font-black text-primary tracking-tight">分配权限</h2>
        <div class="flex items-center gap-2 mt-2">
          <span class="text-xs font-bold uppercase tracking-widest text-on-surface-variant opacity-70">当前角色:</span>
          <span class="font-bold text-primary bg-primary-fixed px-2 py-0.5 rounded text-xs">{{ currentRole?.roleName || '未指定' }}</span>
        </div>
      </div>

      <div class="flex-1 overflow-y-auto p-5 sm:p-8 space-y-3 no-scrollbar">
        <el-alert
            v-if="permissionLoader.state.loadState === 'forbidden'"
            title="无权查看角色权限"
            description="当前账号缺少角色权限查看能力，请联系管理员分配 role:permission:list 权限。"
            type="warning"
            :closable="false"
            show-icon
        />

        <div v-else-if="permissionLoader.state.loadState === 'failed'" class="space-y-4">
          <el-alert
              title="权限数据加载失败"
              description="无法连接权限服务，请检查网络或稍后重试。"
              type="error"
              :closable="false"
              show-icon
          />
          <el-button type="primary" plain @click="loadPermissionData">
            <span class="material-symbols-outlined text-[18px]">refresh</span>
            重新加载
          </el-button>
        </div>

        <template v-else-if="permissionLoader.state.treeData.length > 0">
          <div class="flex flex-wrap items-center justify-between gap-3 mb-3">
            <label class="text-xs font-black uppercase tracking-widest text-on-surface-variant">权限列表</label>
            <span class="text-xs font-bold text-primary">已选 {{ checkedPermissionIds.length }} 项</span>
          </div>

          <el-input
              v-model="permissionKeyword"
              aria-label="搜索权限"
              clearable
              placeholder="搜索权限名称或编码"
              class="w-full"
          >
            <template #prefix>
              <span class="material-symbols-outlined text-[18px]">search</span>
            </template>
          </el-input>

          <div class="flex items-center justify-between gap-3 py-3">
            <span class="text-sm font-medium text-on-surface">只看已选</span>
            <el-switch v-model="selectedOnly" aria-label="只看已选" />
          </div>

          <div v-if="visiblePermissionGroups.length" class="permission-group-list space-y-3 pr-1">
            <section v-for="group in visiblePermissionGroups" :key="group.id" class="permission-group-card">
              <div class="permission-group-header">
                <span class="min-w-0 truncate font-bold text-on-surface">{{ group.name }}</span>
                <el-button text type="primary" size="small" @click="toggleGroupSelection(group)">
                  {{ isGroupSelected(group) ? '取消本组' : '全选本组' }}
                </el-button>
              </div>

              <el-checkbox-group v-model="checkedPermissionIds" class="permission-checkbox-list">
                <el-checkbox v-for="permission in group.permissions" :key="permission.id" :label="permission.id">
                  <span class="permission-name">{{ permission.name }}</span>
                  <span v-if="permission.code" class="permission-code">{{ permission.code }}</span>
                </el-checkbox>
              </el-checkbox-group>
            </section>
          </div>

          <el-empty v-else description="未找到匹配权限" :image-size="88" />
        </template>

        <el-empty v-else-if="permissionLoader.state.loadState === 'empty'" description="暂无权限数据结构" />
      </div>

      <div class="p-5 sm:p-8 bg-surface-container-low border-t border-surface-variant/30 grid grid-cols-2 gap-4 flex-shrink-0">
        <el-button @click="close">取消返回</el-button>
        <el-button
            type="primary"
            :loading="isSubmitting"
            :disabled="permissionLoader.state.treeData.length === 0 || permissionLoader.state.loadState !== 'ready'"
            @click="save"
        >
          {{ isSubmitting ? '保存中...' : '确认分配' }}
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ElAlert, ElButton, ElCheckbox, ElCheckboxGroup, ElDrawer, ElEmpty, ElInput, ElMessage, ElSwitch } from 'element-plus'
import { computed, ref, nextTick } from 'vue'
import {getAllPermissions, getRolePermissionIds, updateRolePermissions} from './api/role.js'
import { createRolePermissionLoader, syncCommittedPermissionIds } from './permissionLoaders.js'
import { groupLeafIds, permissionGroups } from './permissionPresentation.js'

const visible = ref(false)
const currentRole = ref(null)
const emit = defineEmits(['updated', 'closed'])

const isSubmitting = ref(false)
const checkedPermissionIds = ref([])
const permissionKeyword = ref('')
const selectedOnly = ref(false)
const permissionLoader = createRolePermissionLoader({
  getAllPermissions,
  getRolePermissionIds,
  afterTreeReady: nextTick
})
const visiblePermissionGroups = computed(() => permissionGroups(
  permissionLoader.state.treeData,
  permissionKeyword.value,
  selectedOnly.value,
  checkedPermissionIds.value
))

async function open(role) {
  if (!role) return
  currentRole.value = role
  visible.value = true
  await loadPermissionData()
}

async function loadPermissionData() {
  if (!currentRole.value) return

  checkedPermissionIds.value = []
  const result = await permissionLoader.load(currentRole.value.id)
  checkedPermissionIds.value = syncCommittedPermissionIds(checkedPermissionIds.value, result)
}

function close() {
  visible.value = false
  emit('closed')
}

function isGroupSelected(group) {
  const ids = groupLeafIds(group)
  const selected = new Set((checkedPermissionIds.value || []).map(Number))
  return ids.length > 0 && ids.every((id) => selected.has(id))
}

function toggleGroupSelection(group) {
  const groupIds = groupLeafIds(group)
  const selected = new Set((checkedPermissionIds.value || []).map(Number))
  const shouldSelect = groupIds.some((id) => !selected.has(id))
  groupIds.forEach((id) => shouldSelect ? selected.add(id) : selected.delete(id))
  checkedPermissionIds.value = [...selected]
}

async function save() {
  if (!currentRole.value) return

  const permissionIds = [...new Set(checkedPermissionIds.value || [])]

  isSubmitting.value = true
  try {
    const res = await updateRolePermissions({
      roleId: currentRole.value.id,
      permissionIds
    })

    if (res?.code === 200 || res?.success || !res?.code) {
      ElMessage.success(`【${currentRole.value.roleName}】权限已成功同步`)
      emit('updated')
      close()
    } else {
      ElMessage.error(res?.message || '权限保存失败')
    }
  } catch (error) {
    console.error('[Hive Auth] 保存异常:', error)
    ElMessage.error('保存失败，请检查网络环境')
  } finally {
    isSubmitting.value = false
  }
}

defineExpose({open})
</script>

<style scoped>
:deep(.atelier-drawer) {
  box-shadow: 0px 20px 40px rgba(15, 23, 42, 0.08) !important;
  background-color: #ffffff !important;
}

:deep(.el-input__wrapper) {
  background-color: #f1f4f6 !important;
  box-shadow: none !important;
  border-radius: 4px;
  min-height: 44px;
}

:deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--ys-primary) inset !important;
}

.permission-group-list {
  max-height: min(44vh, 32rem);
  overflow-y: auto;
  scrollbar-gutter: stable;
}

.permission-group-card {
  border: 1px solid rgba(200, 211, 223, 0.72);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  padding: 0.75rem;
}

.permission-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  border-bottom: 1px solid rgba(200, 211, 223, 0.58);
  padding-bottom: 0.5rem;
}

.permission-checkbox-list {
  display: grid;
  gap: 0.625rem;
  padding-top: 0.75rem;
}

.permission-checkbox-list :deep(.el-checkbox) {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-items: start;
  width: 100%;
  height: auto;
  margin-right: 0;
  white-space: normal;
}

.permission-checkbox-list :deep(.el-checkbox__label) {
  min-width: 0;
  overflow-wrap: anywhere;
  padding-left: 0.5rem;
}

.permission-name,
.permission-code {
  display: block;
}

.permission-name {
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.permission-code {
  color: var(--el-text-color-secondary);
  font-size: 0.75rem;
  line-height: 1.25rem;
  overflow-wrap: anywhere;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner),
:deep(.el-checkbox__input.is-indeterminate .el-checkbox__inner) {
  background-color: var(--ys-primary);
  border-color: var(--ys-primary);
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}
</style>
