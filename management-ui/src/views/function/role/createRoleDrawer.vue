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

    <div class="flex flex-col h-full bg-surface-container-lowest font-body">
      <div class="px-8 py-8 border-b border-surface-variant/30">
        <div class="flex justify-between items-start mb-6">
          <div class="p-3 bg-primary-container text-white rounded-xl shadow-lg shadow-primary/10">
            <span class="material-symbols-outlined text-3xl">person_add</span>
          </div>
          <button @click="close" class="p-2 hover:bg-surface-container-high rounded-full transition-colors group">
            <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary">close</span>
          </button>
        </div>
        <h2 class="text-2xl font-black text-primary tracking-tight">新建角色</h2>
        <p class="text-sm text-on-surface-variant mt-1">定义职能岗位并从系统权限中关联识别编码</p>
      </div>

      <div class="flex-1 overflow-y-auto p-8 space-y-8 no-scrollbar">
        <div class="space-y-2">
          <label class="text-xs font-black uppercase tracking-widest text-on-surface-variant">角色名称</label>
          <input
            v-model="form.roleName"
            type="text"
            placeholder="例如：成品检验员"
            class="w-full px-4 py-3.5 rounded bg-surface-container-low border-none focus:ring-2 focus:ring-primary/20 text-on-surface transition-all placeholder:text-on-surface-variant/40"
          />
        </div>

        <div class="space-y-2">
          <label class="text-xs font-black uppercase tracking-widest text-on-surface-variant flex justify-between">
            关联识别编码
            <span class="text-[10px] font-normal lowercase opacity-60">来源于系统全量权限</span>
          </label>

          <div class="atelier-tree-select-wrapper">
            <el-tree-select
              v-model="form.roleCode"
              :data="allPermissions"
              :render-after-expand="false"
              show-checkbox
              check-strictly
              node-key="value"
              placeholder="请选择或搜索权限编码"
              class="atelier-tree-select w-full"
              filterable
            >
              <template #default="{ data }">
                <div class="flex items-center gap-2">
                  <span class="material-symbols-outlined text-sm opacity-50">{{ data.icon || 'terminal' }}</span>
                  <span class="text-sm font-mono uppercase">{{ data.value }}</span>
                  <span class="text-[10px] text-on-surface-variant opacity-60">- {{ data.label }}</span>
                </div>
              </template>
            </el-tree-select>
          </div>
        </div>
      </div>

      <div class="p-8 bg-surface-container-low border-t border-surface-variant/30 grid grid-cols-2 gap-4">
        <button
          @click="close"
          class="py-3 px-6 rounded-lg text-sm font-bold text-on-surface-variant hover:bg-surface-container-high transition-all"
        >
          取消返回
        </button>
        <button
          @click="submit"
          class="py-3 px-6 rounded-lg text-sm font-bold text-white bg-primary shadow-lg shadow-primary/30 active:scale-95 hover:opacity-90 transition-all"
        >
          确认创建
        </button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElDrawer, ElTreeSelect } from 'element-plus'

const emit = defineEmits(['success'])
const visible = ref(false)
const form = ref({
  roleName: '',
  roleCode: null
})

// 模拟来源于整个系统的全量权限数据
const allPermissions = ref([
  {
    label: '库存模块',
    value: 'INVENTORY_ROOT',
    icon: 'warehouse',
    children: [
      { label: '面料入库', value: 'FABRIC_INBOUND', icon: 'texture' },
      { label: '损耗记录', value: 'WASTAGE_LOG', icon: 'analytics' }
    ]
  },
  {
    label: '生产模块',
    value: 'PRODUCTION_ROOT',
    icon: 'conveyor_belt',
    children: [
      { label: '生产排期', value: 'PROD_SCHEDULE', icon: 'calendar_today' },
      { label: '质量检测', value: 'QC_INSPECT', icon: 'fact_check' }
    ]
  },
  {
    label: '管理员权限',
    value: 'ADMIN_ROOT',
    icon: 'admin_panel_settings',
    children: [
      { label: '角色管理', value: 'ROLE_MGR', icon: 'badge' },
      { label: '系统设置', value: 'SYS_CONFIG', icon: 'settings' }
    ]
  }
])

const open = () => {
  visible.value = true
  form.value = { roleName: '', roleCode: null }
}

const close = () => {
  visible.value = false
}

const submit = async () => {
  if (!form.value.roleName || !form.value.roleCode) {
    ElMessage.warning('请填写角色名称并选择识别编码')
    return
  }

  ElMessage.success(`角色 [${form.value.roleName}] 已创建并关联编码: ${form.value.roleCode}`)
  emit('success')
  close()
}

defineExpose({ open })
</script>

<style scoped>
/* 深度定制 TreeSelect 以符合 Digital Atelier 规范 */
:deep(.atelier-drawer) {
  box-shadow: 0px 20px 40px rgba(0, 32, 69, 0.06) !important;
  background-color: #ffffff !important;
}

:deep(.atelier-tree-select .el-input__wrapper) {
  background-color: #f1f4f6 !important; /* surface-container-low */
  box-shadow: none !important;
  border-radius: 4px;
  padding: 8px 12px;
}

:deep(.atelier-tree-select .el-input__inner) {
  font-family: 'Inter', monospace;
  font-size: 0.875rem;
  color: #181c1e;
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}
</style>
