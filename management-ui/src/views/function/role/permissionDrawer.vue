<template>
  <el-drawer
      v-model="visible"
      :title="null"
      size="500px"
      direction="rtl"
      :with-header="false"
      class="atelier-drawer"
      append-to-body
  >
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
          <span class="font-bold text-primary bg-primary-fixed px-2 py-0.5 rounded text-xs">
            {{ currentRole?.roleName || '未指定' }}
          </span>
        </div>
      </div>

      <div class="flex-1 overflow-y-auto p-8 no-scrollbar">
        <el-tree
            ref="treeRef"
            :data="treeData"
            show-checkbox
            node-key="id"
            default-expand-all
            :props="{ children: 'children', label: 'label' }"
            class="atelier-tree"
        >
          <template #default="{ node, data }">
            <div class="flex items-center gap-2 py-1">
              <span class="material-symbols-outlined text-lg opacity-50" v-if="data.icon">{{ data.icon }}</span>
              <span class="text-sm font-medium tracking-tight text-on-surface">{{ node.label || data.name || data.permName }}</span>
            </div>
          </template>
        </el-tree>
      </div>

      <div class="p-8 bg-surface-container-low border-t border-surface-variant/30 grid grid-cols-2 gap-4">
        <button @click="close" class="py-3 px-6 rounded-lg text-sm font-bold text-on-surface-variant hover:bg-surface-container-high transition-all">取消</button>
        <button
            @click="save"
            :disabled="isSubmitting"
            class="py-3 px-6 rounded-lg text-sm font-bold text-white bg-primary shadow-lg shadow-primary/30 active:scale-95 hover:opacity-90 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
        >
          <span v-if="isSubmitting" class="material-symbols-outlined text-[18px] animate-spin">progress_activity</span>
          {{ isSubmitting ? '保存中...' : '保存配置' }}
        </button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage, ElTree, ElDrawer } from 'element-plus'

// ⚠️ 引入你的真实接口
import { getAllPermissions, getRolePermissionIds, updateRolePermissions } from './api/role.js'

const visible = ref(false)
const currentRole = ref(null)
const treeRef = ref(null)
const emit = defineEmits(['updated'])

const isLoadingData = ref(false)
const isSubmitting = ref(false)
const treeData = ref([])

const open = async (role) => {
  currentRole.value = role
  visible.value = true
  isLoadingData.value = true

  try {
    // 利用 Promise.all 并发调用你的两个接口：全量树 + 已拥有的权限 ID
    const [treeRes, idsRes] = await Promise.all([
      getAllPermissions(),
      getRolePermissionIds(role.id) // 确保你的后端可以直接解析这个 URL
    ])

    if (treeRes.code === 200) {
      treeData.value = treeRes.data
    }

    const ownedPermissionIds = idsRes.code === 200 ? idsRes.data : []

    // 数据拉取完后进行打勾回显
    nextTick(() => {
      if (treeRef.value) {
        treeRef.value.setCheckedKeys([])
        ownedPermissionIds.forEach(id => {
          treeRef.value.setChecked(id, true, false)
        })
      }
    })
  } catch (error) {
    console.error('初始化权限配置异常:', error)
    ElMessage.error('获取权限数据失败')
  } finally {
    isLoadingData.value = false
  }
}

const close = () => {
  visible.value = false
}

const save = async () => {
  if (!treeRef.value) return

  // 组装提交的 IDs，包含了全选和半选状态的节点
  const checkedKeys = treeRef.value.getCheckedKeys()
  const halfCheckedKeys = treeRef.value.getHalfCheckedKeys()
  const allSelectedIds = [...checkedKeys, ...halfCheckedKeys]

  isSubmitting.value = true

  try {
    // 调用你的更新接口
    const res = await updateRolePermissions({
      roleId: currentRole.value.id,
      permissionIds: allSelectedIds
    })

    if (res.code === 200) {
      ElMessage.success(`[${currentRole.value.roleName}] 权限配置已保存`)
      emit('updated')
      close()
    } else {
      ElMessage.error(res.message || '权限保存失败')
    }
  } catch (error) {
    console.error('保存权限异常:', error)
  } finally {
    isSubmitting.value = false
  }
}

defineExpose({open})
</script>

<style scoped>
:deep(.atelier-drawer) {
  box-shadow: 0px 20px 40px rgba(0, 32, 69, 0.06) !important;
  background-color: #ffffff !important;
}

:deep(.el-tree-node__content) {
  height: 44px;
  border-radius: 8px;
  margin-bottom: 4px;
  transition: all 0.2s ease;
}

:deep(.el-tree-node__content:hover) {
  background-color: #ebeef0;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #002045;
  border-color: #002045;
}

:deep(.el-checkbox__input.is-indeterminate .el-checkbox__inner) {
  background-color: #002045;
  border-color: #002045;
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}
</style>