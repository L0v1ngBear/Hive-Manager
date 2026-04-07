<template>
  <div class="min-h-screen bg-surface text-on-surface p-4 md:p-8 overflow-x-hidden">
    <div class="max-w-7xl mx-auto space-y-8">

      <header class="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-8">
        <div>
          <h1 class="text-3xl md:text-4xl font-extrabold tracking-tight text-primary leading-none">角色权限管理</h1>
          <p class="text-sm md:text-base text-on-surface-variant mt-3 max-w-lg">
            配置当前组织的职能角色，定义员工的操作权限范围。
          </p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button class="bg-primary text-on-primary flex items-center gap-2 px-6 py-2.5 rounded-lg text-sm font-bold shadow-md hover:shadow-lg hover:opacity-90 transition-all active:scale-95">
            <span class="material-symbols-outlined text-[20px]">add</span>新建角色
          </button>
        </div>
      </header>

      <section class="bg-surface-container-lowest rounded-xl overflow-hidden shadow-sm border border-outline-variant/20 relative">
        <div v-if="loading" class="absolute inset-0 bg-white/50 backdrop-blur-sm z-20 flex items-center justify-center">
          <span class="material-symbols-outlined text-primary text-3xl animate-spin">progress_activity</span>
        </div>

        <div class="p-4 md:p-6 flex flex-col sm:flex-row sm:items-center justify-between border-b border-outline-variant/20 gap-4">
          <div class="relative w-full sm:w-80">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant/50 text-[18px]">search</span>
            <input
              v-model="searchQuery"
              @keyup.enter="handleSearch"
              type="text"
              placeholder="搜索角色名称..."
              class="w-full pl-10 pr-4 py-2.5 bg-surface-container-low border-none rounded-xl text-sm focus:ring-2 focus:ring-primary transition-shadow"
            />
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse min-w-[900px]">
            <thead class="bg-surface-container-low/50">
            <tr>
              <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">角色名称</th>
              <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">角色类型</th>
              <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">关联人数</th>
              <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider w-1/3">权限概览</th>
              <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">创建日期</th>
              <th class="px-6 py-4 text-right text-xs font-black text-on-surface-variant uppercase tracking-wider">操作</th>
            </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/10">
            <tr v-for="role in roles" :key="role.id" class="group transition-colors hover:bg-surface-container-high/30">

              <td class="px-6 py-4">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-lg flex items-center justify-center shrink-0 bg-primary/10 text-primary">
                    <span class="material-symbols-outlined text-xl" style="font-variation-settings: 'FILL' 1;">
                      {{ role.isSystem === 1 ? 'admin_panel_settings' : 'badge' }}
                    </span>
                  </div>
                  <div class="font-bold text-primary text-sm">
                    {{ role.roleName }}
                  </div>
                </div>
              </td>

              <td class="px-6 py-4">
                <span v-if="role.isSystem === 1" class="text-[11px] font-bold px-2 py-1 rounded bg-secondary/10 text-secondary border border-secondary/20">系统内置</span>
                <span v-else class="text-[11px] font-bold px-2 py-1 rounded bg-surface-container-high text-on-surface-variant border border-outline-variant/20">自定义</span>
              </td>

              <td class="px-6 py-4">
                <div class="flex items-center gap-1.5 cursor-pointer hover:text-primary transition-colors">
                  <span class="material-symbols-outlined text-[16px] text-on-surface-variant">group</span>
                  <span :class="['font-bold text-sm', role.userCount > 0 ? 'text-primary' : 'text-on-surface-variant/50']">
                    {{ role.userCount }} 人
                  </span>
                </div>
              </td>

              <td class="px-6 py-4">
                <div class="flex flex-wrap gap-1.5">
                  <span v-if="role.isSystem === 1 && role.roleCode === 'SUPER_ADMIN'" class="text-[11px] px-2 py-1 bg-error/10 text-error rounded font-medium">最高权限 (所有模块)</span>
                  <template v-else>
                    <span v-for="(perm, idx) in role.permissionSummary.slice(0, 3)" :key="idx" class="text-[11px] px-2 py-1 bg-surface-container text-on-surface rounded font-medium">
                      {{ perm }}
                    </span>
                    <span v-if="role.permissionSummary.length > 3" class="text-[11px] px-2 py-1 bg-surface-container text-on-surface-variant rounded font-medium">+{{ role.permissionSummary.length - 3 }}</span>
                    <span v-if="role.permissionSummary.length === 0" class="text-xs text-on-surface-variant/50">未分配权限</span>
                  </template>
                </div>
              </td>

              <td class="px-6 py-4 text-sm text-on-surface-variant">
                {{ role.createTime.split(' ')[0] }}
              </td>

              <td class="px-6 py-4 text-right">
                <div class="flex justify-end gap-2">
                  <button
                    @click="handleOpenDrawer(role)"
                    class="text-primary hover:bg-primary/10 px-3 py-1.5 rounded-lg text-sm font-bold transition-colors"
                  >
                    编辑配置
                  </button>
                  <button
                    v-if="role.isSystem !== 1"
                    @click="handleDelete(role)"
                    class="p-1.5 text-on-surface-variant hover:bg-error-container hover:text-error rounded-lg transition-colors"
                    title="删除"
                  >
                    <span class="material-symbols-outlined text-[20px]">delete</span>
                  </button>
                </div>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </section>

      <PermissionDrawer ref="drawerRef" @success="handlePermissionSuccess" />

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PermissionDrawer from './permissionTree.vue' // 注意这里调整为你实际的相对路径

// --- 状态控制 ---
const loading = ref(false)
const searchQuery = ref('')
const roles = ref([])
const drawerRef = ref(null) // 指向子组件实例

// --- 生命周期与数据加载 ---
onMounted(() => {
  fetchData()
})

const fetchData = async () => {
  loading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 400))
    roles.value = [
      {
        id: 1, roleCode: 'SUPER_ADMIN', roleName: '超级管理员', isSystem: 1, createTime: '2024-01-12 10:00:00',
        userCount: 2, permissionSummary: ['所有权限'], permIds: [1, 2, 21, 22, 23, 3, 31, 32, 33, 4, 41, 42]
      },
      {
        id: 2, roleCode: 'SALES_MGR', roleName: '销售主管', isSystem: 0, createTime: '2024-02-05 14:30:00',
        userCount: 5, permissionSummary: ['客户列表', '订单管理', '退款审核'], permIds: [1, 2, 21, 22, 23]
      },
      {
        id: 3, roleCode: 'WH_ADMIN', roleName: '仓储专员', isSystem: 0, createTime: '2024-04-06 16:20:00',
        userCount: 0, permissionSummary: ['入库登记', '出库审核', '盘点记录'], permIds: [3, 31, 32, 33]
      }
    ].filter(r => r.roleName.includes(searchQuery.value))
  } finally {
    loading.value = false
  }
}

// --- 业务操作 ---
const handleSearch = () => fetchData()

const handleDelete = (role) => {
  if (role.userCount > 0) {
    ElMessage.warning(`该角色下还有 ${role.userCount} 名员工，请先移除员工后再删除！`)
    return
  }
  if (confirm(`确定要删除角色 [${role.roleName}] 吗？`)) {
    roles.value = roles.value.filter(r => r.id !== role.id)
    ElMessage.success('删除成功')
  }
}

// --- 子组件调用逻辑 ---
const handleOpenDrawer = (role) => {
  if (drawerRef.value) {
    drawerRef.value.open(role)
  }
}

// 接收子组件抛出的成功事件
const handlePermissionSuccess = ({ roleId, newPermIds }) => {
  // 可以在这里重新请求接口 fetchData() 刷新数据
  // 或者在前端直接修改状态（下面是本地修改的演示）
  const targetRole = roles.value.find(r => r.id === roleId)
  if (targetRole) {
    targetRole.permIds = newPermIds
  }
}
</script>
