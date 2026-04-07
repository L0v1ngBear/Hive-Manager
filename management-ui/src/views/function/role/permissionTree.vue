<template>
  <el-drawer
    v-model="visible"
    :title="`编辑配置 - ${currentRole?.roleName || ''}`"
    size="400px"
    @closed="handleClosed"
  >
    <div class="flex flex-col h-full -mt-4">
      <div class="bg-surface-container-low p-3 rounded-lg mb-4 flex gap-3 items-start border border-outline-variant/20">
        <span class="material-symbols-outlined text-primary mt-0.5">info</span>
        <div>
          <p class="text-sm font-bold text-on-surface">当前为 <span class="text-primary">{{ currentRole?.roleName }}</span> 分配权限</p>
          <p class="text-xs text-on-surface-variant mt-1">勾选下方树形菜单赋予对应的模块和按钮级操作权限。</p>
        </div>
      </div>

      <div class="flex-1 overflow-y-auto border border-outline-variant/20 rounded-lg p-2" v-loading="treeLoading">
        <el-tree
          ref="permissionTreeRef"
          :data="permissionTreeData"
          show-checkbox
          node-key="id"
          :props="defaultProps"
          :default-checked-keys="currentRolePerms"
          highlight-current
          default-expand-all
        >
          <template #default="scope">
            <div v-if="scope && scope.data" class="flex items-center gap-2 text-sm text-on-surface">
              <span class="material-symbols-outlined text-[16px] text-on-surface-variant/70">
                {{ scope.data.icon || (scope.data.children ? 'folder' : 'description') }}
              </span>
              <span>{{ scope.node.label }}</span>
            </div>
          </template>
        </el-tree>
      </div>

      <div class="pt-4 mt-4 border-t border-outline-variant/20 flex justify-end gap-3">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="savePermissions" :loading="saving">保存配置</el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

// 定义向父组件派发的事件
const emit = defineEmits(['success'])

// --- 组件内部状态 ---
const visible = ref(false)
const treeLoading = ref(false)
const saving = ref(false)
const currentRole = ref(null)
const permissionTreeRef = ref(null)
const currentRolePerms = ref([])

const defaultProps = {
  children: 'children',
  label: 'name'
}

// 模拟后端的全量权限树数据
const permissionTreeData = ref([
  { id: 1, name: '工作台', icon: 'dashboard', permCode: 'dashboard:view' },
  { id: 2, name: '订单管理', icon: 'receipt_long', children: [
      { id: 21, name: '客户列表', permCode: 'customer:list' },
      { id: 22, name: '订单列表', permCode: 'order:list' },
      { id: 23, name: '退款审核', permCode: 'order:refund' }
    ]},
  { id: 3, name: '仓储管理', icon: 'inventory_2', children: [
      { id: 31, name: '入库登记', permCode: 'inventory:in' },
      { id: 32, name: '出库审核', permCode: 'inventory:out' },
      { id: 33, name: '盘点记录', permCode: 'inventory:check' }
    ]},
  { id: 4, name: '系统设置', icon: 'settings', children: [
      { id: 41, name: '角色管理', permCode: 'sys:role' },
      { id: 42, name: '员工管理', permCode: 'sys:user' }
    ]}
])

// --- 核心方法：暴露给父组件调用 ---
const open = async (role) => {
  currentRole.value = Object.assign({}, role) // 浅拷贝，防止直接修改父组件对象
  visible.value = true
  treeLoading.value = true

  try {
    // 模拟请求接口获取该角色的最新权限
    await new Promise(resolve => setTimeout(resolve, 300))
    currentRolePerms.value = role.permIds || []
    if (permissionTreeRef.value) {
      permissionTreeRef.value.setCheckedKeys(currentRolePerms.value)
    }
  } catch (error) {
    ElMessage.error('获取权限数据失败')
  } finally {
    treeLoading.value = false
  }
}

// 暴露 open 方法
defineExpose({
  open
})

// --- 清理逻辑 ---
const handleClosed = () => {
  currentRole.value = null
  currentRolePerms.value = []
  if (permissionTreeRef.value) {
    permissionTreeRef.value.setCheckedKeys([])
  }
}

// --- 保存逻辑 ---
const savePermissions = async () => {
  if (!permissionTreeRef.value) return

  saving.value = true
  try {
    const checkedKeys = permissionTreeRef.value.getCheckedKeys()
    const halfCheckedKeys = permissionTreeRef.value.getHalfCheckedKeys()
    const allSelectedIds = [...checkedKeys, ...halfCheckedKeys]

    // 模拟提交给后端
    await new Promise(resolve => setTimeout(resolve, 500))

    ElMessage.success(`${currentRole.value?.roleName} 权限配置成功`)

    // 通知父组件保存成功，并将最新权限传回去
    emit('success', { roleId: currentRole.value.id, newPermIds: checkedKeys })

    nextTick(() => {
      visible.value = false
    })
  } catch (error) {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
:deep(.el-tree-node__content) {
  height: 36px;
  border-radius: 6px;
  margin-bottom: 2px;
}
:deep(.el-tree-node__content:hover) {
  background-color: var(--el-fill-color-light);
}
:deep(.el-tree--highlight-current .el-tree-node.is-current > .el-tree-node__content) {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: bold;
}
:deep(.el-tree-node__expand-icon) {
  color: #64748b;
}
</style>
