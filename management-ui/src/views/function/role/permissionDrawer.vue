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
      <div v-if="isLoadingData" class="absolute inset-0 bg-white/60 backdrop-blur-sm z-20 flex flex-col items-center justify-center">
        <span class="material-symbols-outlined text-primary text-4xl animate-spin">progress_activity</span>
        <span class="text-sm font-bold text-primary mt-2">正在处理权限矩阵...</span>
      </div>

      <div class="px-8 py-8 border-b border-surface-variant/30">
        <div class="flex justify-between items-start mb-6">
          <div class="p-3 bg-primary-container text-white rounded-xl shadow-lg shadow-primary/10">
            <span class="material-symbols-outlined text-3xl">security</span>
          </div>
          <button @click="close" class="p-2 hover:bg-surface-container-high rounded-full transition-colors group">
            <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary">close</span>
          </button>
        </div>
        <h2 class="text-2xl font-black text-primary tracking-tight">分配权限</h2>
        <div class="flex items-center gap-2 mt-2">
          <span class="text-xs font-bold uppercase tracking-widest text-on-surface-variant opacity-70">当前角色:</span>
          <span class="font-bold text-primary bg-primary-fixed px-2 py-0.5 rounded text-xs">{{ currentRole?.roleName || '未指定' }}</span>
        </div>
      </div>

      <div class="flex-1 overflow-y-auto p-8 no-scrollbar space-y-3">
        <div v-if="permissionLoadError" class="rounded-lg bg-amber-50 text-amber-700 px-4 py-3 text-sm">
          {{ permissionLoadError }}
        </div>

        <template v-else-if="treeData.length > 0">
          <label class="text-xs font-black uppercase tracking-widest text-on-surface-variant flex justify-between">
            权限树
            <span class="text-[10px] font-normal lowercase opacity-60">支持多选、搜索、折叠标签</span>
          </label>

          <div class="atelier-tree-select-wrapper">
            <el-tree-select
              v-model="checkedPermissionIds"
              :data="treeData"
              node-key="id"
              multiple
              collapse-tags
              collapse-tags-tooltip
              check-strictly
              filterable
              :loading="isLoadingData"
              :props="{ value: 'id', label: 'permName', children: 'children' }"
              class="atelier-tree-select w-full"
              placeholder="请选择要分配的权限"
            >
              <template #default="scope">
                <div v-if="scope?.data" class="flex items-center gap-2">
                  <span class="material-symbols-outlined text-[18px] opacity-40">
                    {{ scope.data.children?.length ? 'folder' : 'description' }}
                  </span>
                  <span class="text-sm font-medium text-on-surface">{{ scope.data.permName }}</span>
                </div>
              </template>
            </el-tree-select>
          </div>
        </template>

        <el-empty v-else-if="!isLoadingData" description="暂无权限配置项" />
      </div>

      <div class="p-8 bg-surface-container-low border-t border-surface-variant/30 grid grid-cols-2 gap-4">
        <button @click="close" class="py-3 px-6 rounded-lg text-sm font-bold text-on-surface-variant hover:bg-surface-container-high transition-all">取消</button>
        <button
          @click="save"
          :disabled="isSubmitting || treeData.length === 0 || !!permissionLoadError"
          class="py-3 px-6 rounded-lg text-sm font-bold text-white bg-primary shadow-lg shadow-primary/30 active:scale-95 hover:opacity-90 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
        >
          <span v-if="isSubmitting" class="material-symbols-outlined text-[18px] animate-spin">progress_activity</span>
          {{ isSubmitting ? '正在保存...' : '确认分配' }}
        </button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElDrawer } from 'element-plus'
import { getAllPermissions, getRolePermissionIds, updateRolePermissions } from './api/role.js'

const visible = ref(false)
const currentRole = ref(null)
const emit = defineEmits(['updated', 'closed'])
const isLoadingData = ref(false)
const isSubmitting = ref(false)
const treeData = ref([])
const permissionLoadError = ref('')
const checkedPermissionIds = ref([])

async function open(role) {
  if (!role) return
  currentRole.value = role
  treeData.value = []
  checkedPermissionIds.value = []
  permissionLoadError.value = ''
  visible.value = true
  isLoadingData.value = true

  try {
    const [permissionTree, ownedIds] = await Promise.all([
      getAllPermissions(),
      getRolePermissionIds(role.id)
    ])
    treeData.value = Array.isArray(permissionTree) ? permissionTree : []
    checkedPermissionIds.value = Array.isArray(ownedIds) ? ownedIds : []
  } catch (error) {
    console.error('[Hive Auth] 初始化失败', error)
    treeData.value = []
    permissionLoadError.value = error?.response?.status === 403
      ? '您暂无权限查看角色权限树，请联系管理员开通“角色权限查看”权限。'
      : '无法连接到权限服务，请稍后再试。'
  } finally {
    isLoadingData.value = false
  }
}

function close() {
  visible.value = false
  emit('closed')
}

async function save() {
  if (!currentRole.value) return
  if (permissionLoadError.value) {
    ElMessage.warning(permissionLoadError.value)
    return
  }

  const permissionIds = [...new Set(checkedPermissionIds.value || [])]
  isSubmitting.value = true
  try {
    await updateRolePermissions({
      roleId: currentRole.value.id,
      permissionIds
    })
    ElMessage.success(`角色【${currentRole.value.roleName}】权限已同步`)
    emit('updated')
    close()
  } catch (error) {
    console.error('[Hive Auth] 保存异常:', error)
    ElMessage.error('网络通讯异常，保存未成功')
  } finally {
    isSubmitting.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
:deep(.atelier-drawer) {
  box-shadow: 0px 20px 40px rgba(0, 32, 69, 0.06) !important;
  background-color: #ffffff !important;
}

:deep(.atelier-tree-select .el-select__wrapper) {
  background-color: #f1f4f6 !important;
  box-shadow: none !important;
  border-radius: 4px;
  min-height: 48px;
  padding: 4px 12px;
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}
</style>
