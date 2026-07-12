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
        <div v-if="permissionLoadError" class="rounded-lg bg-error-container text-error px-4 py-3 text-sm">
          {{ permissionLoadError }}
        </div>

        <template v-else-if="treeData.length > 0">
          <label class="text-xs font-black uppercase tracking-widest text-on-surface-variant flex justify-between mb-2">
            权限结构树
            <span class="text-[10px] font-normal lowercase opacity-60">支持搜索与折叠</span>
          </label>

          <div class="atelier-tree-select-wrapper">
            <el-tree-select
                v-model="checkedPermissionIds"
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

        <el-empty v-else-if="!isLoadingData" description="暂无权限数据结构" />
      </div>

      <div class="p-8 bg-surface-container-low border-t border-surface-variant/30 grid grid-cols-2 gap-4 flex-shrink-0">
        <el-button @click="close">取消返回</el-button>
        <el-button
            type="primary"
            :loading="isSubmitting"
            :disabled="treeData.length === 0 || !!permissionLoadError"
            @click="save"
        >
          {{ isSubmitting ? '保存中...' : '确认分配' }}
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElButton, ElDrawer, ElEmpty, ElMessage, ElTreeSelect } from 'element-plus'
import {getAllPermissions, getRolePermissionIds, updateRolePermissions} from './api/role.js'

const visible = ref(false)
const currentRole = ref(null)
const emit = defineEmits(['updated', 'closed'])

const isLoadingData = ref(false)
const isSubmitting = ref(false)
const treeData = ref([])
const checkedPermissionIds = ref([])
const permissionLoadError = ref('')

async function open(role) {
  if (!role) return
  currentRole.value = role

  treeData.value = []
  checkedPermissionIds.value = []
  permissionLoadError.value = ''
  visible.value = true
  isLoadingData.value = true

  try {
    const [permissionsRes, ownedIdsRes] = await Promise.all([
      getAllPermissions(),
      getRolePermissionIds(role.id)
    ])

    // 剥离包裹层，拿到后端返回的原始树形数据
    const rawList = permissionsRes?.data?.data || permissionsRes?.data || permissionsRes || []
    const ownedIds = ownedIdsRes?.data?.data || ownedIdsRes?.data || ownedIdsRes || []

    // 🌟 核心修改：直接把后端返回的自带 children 的数组扔给组件，不需要再 buildTree 了！
    treeData.value = Array.isArray(rawList) ? rawList : []

    // 等待 DOM 树渲染完成，再赋选中值，确保能正确回显文字而不是 ID
    await nextTick()

    if (Array.isArray(ownedIds)) {
      checkedPermissionIds.value = ownedIds.map(id => Number(id))
    } else {
      checkedPermissionIds.value = []
    }

  } catch (error) {
    console.error('[Hive Auth] 权限初始化失败:', error)
    permissionLoadError.value = '无法连接到权限服务或无权查看，请稍后再试。'
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
