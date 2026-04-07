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

    <div class="flex flex-col h-full bg-surface-container-lowest">
      <div class="px-8 py-8 border-b border-surface-variant/30">
        <div class="flex justify-between items-start mb-6">
          <div class="p-3 bg-primary-container text-white rounded-xl shadow-lg shadow-primary/10">
            <span class="material-symbols-outlined text-3xl">security</span>
          </div>
          <button @click="visible = false" class="p-2 hover:bg-surface-container-high rounded-full transition-colors group">
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
              <span class="text-sm font-medium tracking-tight text-on-surface">{{ node.label }}</span>
            </div>
          </template>
        </el-tree>
      </div>

      <div class="p-8 bg-surface-container-low border-t border-surface-variant/30 grid grid-cols-2 gap-4">
        <button @click="visible = false" class="py-3 px-6 rounded-lg text-sm font-bold text-on-surface-variant hover:bg-surface-container-high transition-all">取消</button>
        <button @click="save" class="py-3 px-6 rounded-lg text-sm font-bold text-white bg-primary shadow-lg shadow-primary/30 active:scale-95 hover:opacity-90 transition-all">保存配置</button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import {ref, nextTick} from 'vue'
import {ElMessage, ElTree, ElDrawer} from 'element-plus'

const visible = ref(false)
const currentRole = ref(null)
const treeRef = ref(null)

// 模拟不同角色的已有权限 ID 映射
const rolePermissionMap = {
  101: [1, 11, 13], 102: [2, 21, 22], 103: [1, 11, 12, 13, 2, 21, 22, 3, 31, 32]
}

// 模拟全量权限树数据
const treeData = ref([
  {
    id: 1, label: '库存管理', icon: 'warehouse', children: [
      {id: 11, label: '面料入库'}, {id: 12, label: '化工原料监控'}, {id: 13, label: '损耗记录'}
    ]
  },
  {
    id: 2, label: '生产流程', icon: 'conveyor_belt', children: [
      {id: 21, label: '生产排期'}, {id: 22, label: '质量控制'}
    ]
  },
  {
    id: 3, label: '物流与发货', icon: 'local_shipping', children: [
      {id: 31, label: '承运商指派'}, {id: 32, label: '标签打印'}
    ]
  }
])

const open = (role) => {
  currentRole.value = role
  visible.value = true
  nextTick(() => {
    if (treeRef.value) {
      treeRef.value.setCheckedKeys([]) // 清空上次选择
      const permissions = rolePermissionMap[role.id] || []
      permissions.forEach(key => treeRef.value.setChecked(key, true, false)) // 勾选回显
    }
  })
}

const emit = defineEmits(['updated'])
const save = () => {
  ElMessage.success(`[${currentRole.value.roleName}] 权限配置已保存`)
  emit('updated')
  visible.value = false
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
  background-color: #ebeef0; /* surface-container */
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #002045; /* primary */
  border-color: #002045;
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}
</style>
