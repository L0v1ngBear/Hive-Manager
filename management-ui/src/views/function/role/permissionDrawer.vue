<template>
  <el-drawer v-model="visible" :title="null" size="500px" direction="rtl" :with-header="false" class="atelier-drawer" append-to-body>
    <div class="h-1 bg-primary w-full sticky top-0 z-10"></div>
    <div class="flex flex-col h-full bg-surface-container-lowest relative">
      <div v-if="isLoadingData" class="absolute inset-0 bg-white/60 backdrop-blur-sm z-20 flex flex-col items-center justify-center">
        <span class="material-symbols-outlined text-primary text-4xl animate-spin">progress_activity</span>
        <span class="text-sm font-bold text-primary mt-2">正在加载权限数据...</span>
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

      <div class="flex-1 overflow-y-auto p-8 no-scrollbar">
        <el-tree
          ref="treeRef"
          :data="treeData"
          show-checkbox
          node-key="id"
          default-expand-all
          :props="{ children: 'children', label: 'permName' }"
          class="atelier-tree"
        >
          <template #default="{ data }">
            <div class="flex items-center gap-2 py-1">
              <span class="material-symbols-outlined text-lg opacity-50">{{ data.children?.length ? 'folder' : 'description' }}</span>
              <span class="text-sm font-medium tracking-tight text-on-surface">{{ data.permName }}</span>
            </div>
          </template>
        </el-tree>
      </div>

      <div class="p-8 bg-surface-container-low border-t border-surface-variant/30 grid grid-cols-2 gap-4">
        <button @click="close" class="py-3 px-6 rounded-lg text-sm font-bold text-on-surface-variant hover:bg-surface-container-high transition-all">取消</button>
        <button @click="save" :disabled="isSubmitting" class="py-3 px-6 rounded-lg text-sm font-bold text-white bg-primary shadow-lg shadow-primary/30 active:scale-95 hover:opacity-90 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2">
          <span v-if="isSubmitting" class="material-symbols-outlined text-[18px] animate-spin">progress_activity</span>
          {{ isSubmitting ? '保存中...' : '保存配置' }}
        </button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAllPermissions, getRolePermissionIds, updateRolePermissions } from './api/role.js'

const visible = ref(false)
const currentRole = ref(null)
const treeRef = ref(null)
const emit = defineEmits(['updated'])
const isLoadingData = ref(false)
const isSubmitting = ref(false)
const treeData = ref([])

function buildTree(list = []) {
  const nodeMap = new Map()
  const roots = []
  list.forEach((item) => nodeMap.set(item.id, { ...item, children: [] }))
  nodeMap.forEach((node) => {
    if (node.parentId && nodeMap.has(node.parentId)) {
      nodeMap.get(node.parentId).children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

async function open(role) {
  currentRole.value = role
  visible.value = true
  isLoadingData.value = true
  try {
    const [permissions, ownedPermissionIds] = await Promise.all([
      getAllPermissions(),
      getRolePermissionIds(role.id)
    ])
    treeData.value = buildTree(permissions || [])
    nextTick(() => {
      if (treeRef.value) {
        treeRef.value.setCheckedKeys(Array.isArray(ownedPermissionIds) ? ownedPermissionIds : [])
      }
    })
  } catch (error) {
    console.error('初始化权限配置异常:', error)
    ElMessage.error('获取权限数据失败')
  } finally {
    isLoadingData.value = false
  }
}

function close() {
  visible.value = false
}

async function save() {
  if (!treeRef.value || !currentRole.value) return
  const checkedKeys = treeRef.value.getCheckedKeys()
  const halfCheckedKeys = treeRef.value.getHalfCheckedKeys()
  const permissionIds = [...new Set([...checkedKeys, ...halfCheckedKeys])]
  isSubmitting.value = true
  try {
    await updateRolePermissions({ roleId: currentRole.value.id, permissionIds })
    ElMessage.success(`【${currentRole.value.roleName}】权限配置已保存`)
    emit('updated')
    close()
  } catch (error) {
    console.error('保存权限异常:', error)
  } finally {
    isSubmitting.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
:deep(.atelier-drawer) { box-shadow: 0px 20px 40px rgba(0, 32, 69, 0.06) !important; background-color: #ffffff !important; }
:deep(.el-tree-node__content) { height: 44px; border-radius: 8px; margin-bottom: 4px; transition: all 0.2s ease; }
:deep(.el-tree-node__content:hover) { background-color: #ebeef0; }
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) { background-color: #002045; border-color: #002045; }
:deep(.el-checkbox__input.is-indeterminate .el-checkbox__inner) { background-color: #002045; border-color: #002045; }
.no-scrollbar::-webkit-scrollbar { display: none; }
</style>