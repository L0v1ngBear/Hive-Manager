<template>
  <el-drawer
      v-model="visible"
      :title="null"
      size="500px"
      direction="rtl"
      :with-header="false"
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

      <div class="px-8 py-8 border-b border-surface-variant/30 flex-shrink-0">
        <div class="flex justify-between items-start mb-6">
          <div class="p-3 bg-primary-container text-white rounded-xl shadow-lg shadow-primary/10">
            <span class="material-symbols-outlined text-3xl">security</span>
          </div>
          <el-button circle text class="p-2 hover:bg-surface-container-high rounded-full transition-colors group" @click="close">
            <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary">close</span>
          </el-button>
        </div>
        <h2 class="text-2xl font-black text-primary tracking-tight">分配权限</h2>
        <div class="flex items-center gap-2 mt-2">
          <span class="text-xs font-bold uppercase tracking-widest text-on-surface-variant opacity-70">当前角色:</span>
          <span class="font-bold text-primary bg-primary-fixed px-2 py-0.5 rounded text-xs">{{ currentRole?.roleName || '未指定' }}</span>
        </div>
      </div>

      <div class="flex-1 overflow-y-auto p-8 space-y-3 no-scrollbar">
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
          <label class="text-xs font-black uppercase tracking-widest text-on-surface-variant flex justify-between mb-2">
            权限结构树
            <span class="text-[10px] font-normal lowercase opacity-60">支持搜索与折叠</span>
          </label>

          <div class="atelier-tree-select-wrapper">
            <el-tree-select
                v-model="checkedPermissionIds"
                :data="permissionLoader.state.treeData"
                node-key="id"
                value-key="id"
                multiple
                show-checkbox
                collapse-tags
                collapse-tags-tooltip
                filterable
                check-on-click-node
                :render-after-expand="false"
                :props="{ value: 'id', label: 'permName', children: 'children' }"
                placeholder="请展开选择系统权限"
                class="atelier-tree-select w-full"
            >
              <template #default="scope">
                <div v-if="scope && scope.data" class="flex items-center gap-2">
                  <span class="material-symbols-outlined text-[18px] opacity-40">
                    {{ scope.data.children?.length ? 'folder' : 'description' }}
                  </span>
                  <span class="text-sm font-medium text-on-surface">
                    {{ scope.data.permName || scope.data.label }}
                  </span>
                </div>
              </template>
            </el-tree-select>
          </div>
        </template>

        <el-empty v-else-if="permissionLoader.state.loadState === 'empty'" description="暂无权限数据结构" />
      </div>

      <div class="p-8 bg-surface-container-low border-t border-surface-variant/30 grid grid-cols-2 gap-4 flex-shrink-0">
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
import { ElAlert, ElButton, ElDrawer, ElEmpty, ElMessage, ElTreeSelect } from 'element-plus'
import { ref, nextTick } from 'vue'
import {getAllPermissions, getRolePermissionIds, updateRolePermissions} from './api/role.js'
import { createRolePermissionLoader, syncCommittedPermissionIds } from './permissionLoaders.js'

const visible = ref(false)
const currentRole = ref(null)
const emit = defineEmits(['updated', 'closed'])

const isSubmitting = ref(false)
const checkedPermissionIds = ref([])
const permissionLoader = createRolePermissionLoader({
  getAllPermissions,
  getRolePermissionIds,
  afterTreeReady: nextTick
})

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

:deep(.atelier-tree-select .el-select__wrapper) {
  background-color: #f1f4f6 !important;
  box-shadow: none !important;
  border-radius: 4px;
  min-height: 48px;
  padding: 4px 12px;
}

:deep(.atelier-tree-select.is-focus .el-select__wrapper) {
  box-shadow: 0 0 0 1px #1f3f5f inset !important;
}

:deep(.el-tree-node__content) {
  height: 36px;
  border-radius: 6px;
  margin-bottom: 2px;
}

:deep(.el-tree-node__content:hover) {
  background-color: #ebeef0;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner),
:deep(.el-checkbox__input.is-indeterminate .el-checkbox__inner) {
  background-color: #1f3f5f;
  border-color: #1f3f5f;
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}
</style>
