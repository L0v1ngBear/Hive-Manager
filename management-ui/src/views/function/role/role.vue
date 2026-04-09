<template>
  <div class="min-h-screen bg-surface text-on-surface p-4 md:p-8 overflow-x-hidden font-body">
    <div class="max-w-7xl mx-auto space-y-8">

      <header class="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-8">
        <div>
          <h1 class="text-3xl md:text-4xl font-extrabold tracking-tight text-primary leading-none">角色权限管理</h1>
          <p class="text-sm md:text-base text-on-surface-variant mt-3 max-w-lg">配置组织职能角色，定义操作权限范围。</p>
        </div>
        <button @click="openCreateRole" class="bg-primary text-on-primary flex items-center gap-2 px-6 py-2.5 rounded-lg text-sm font-bold shadow-lg shadow-primary/20 hover:opacity-90 transition-all active:scale-95">
          <span class="material-symbols-outlined text-[20px]">add</span>新建角色
        </button>
      </header>

      <section class="bg-surface-container-lowest rounded-xl overflow-hidden shadow-sm border-l-4 border-primary/20 relative">
        <div v-if="loading" class="absolute inset-0 bg-white/50 backdrop-blur-sm z-20 flex items-center justify-center">
          <span class="material-symbols-outlined text-primary text-3xl animate-spin">progress_activity</span>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse min-w-[900px]">
            <thead class="bg-surface-container-low/50">
            <tr>
              <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">角色名称</th>
              <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">角色类型</th>
              <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">关联人数</th>
              <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">创建日期</th>
              <th class="px-6 py-4 text-right text-xs font-black text-on-surface-variant uppercase tracking-wider">操作</th>
            </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/10">
            <tr v-for="role in roles" :key="role.id" class="group transition-colors hover:bg-surface-container-high/30">
              <td class="px-6 py-4">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-lg flex items-center justify-center shrink-0 bg-primary/10 text-primary">
                    <span class="material-symbols-outlined text-xl">badge</span>
                  </div>
                  <div class="font-bold text-primary text-sm">{{ role.roleName }}</div>
                </div>
              </td>
              <td class="px-6 py-4">
                <span v-if="role.isSystem" class="text-[11px] font-bold px-2 py-1 rounded bg-secondary/10 text-secondary border border-secondary/20">系统内置</span>
                <span v-else class="text-[11px] font-bold px-2 py-1 rounded bg-surface-container-high text-on-surface-variant border border-outline-variant/20">自定义</span>
              </td>
              <td class="px-6 py-4 text-sm font-bold text-on-surface-variant">{{ role.userCount || 0 }} 人</td>
              <td class="px-6 py-4 text-sm text-on-surface-variant">{{ role.createTime }}</td>
              <td class="px-6 py-4 text-right">
                <button @click="openPermission(role)" class="text-primary hover:bg-primary/10 px-4 py-2 rounded-lg text-sm font-bold transition-colors">配置权限</button>
              </td>
            </tr>
            <tr v-if="!loading && roles.length === 0">
              <td colspan="5" class="px-6 py-12 text-center text-on-surface-variant">
                暂无角色数据
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </section>

      <PermissionDrawer ref="drawerRef" @updated="fetchData" />
      <CreateRoleDrawer ref="createRoleRef" @success="fetchData" />

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PermissionDrawer from './permissionDrawer.vue'
import CreateRoleDrawer from './createRoleDrawer.vue'

// ⚠️ 注意：这里的路径请替换为你实际存放 api 方法的文件路径
import { getRolePage } from './api/role.js'

const roles = ref([])
const loading = ref(false)
const drawerRef = ref(null)
const createRoleRef = ref(null)

const fetchData = async () => {
  loading.value = true
  try {
    // 调用你封装的分页接口，这里写死了分页参数，你可以按需绑定到分页组件
    const res = await getRolePage({ page: 1, limit: 100 })

    if (res.code === 200) {
      // 假设后端返回的数据在 res.data.records 或 res.data 里，按需调整
      roles.value = res.data.records || res.data || []
    } else {
      ElMessage.error(res.message || '获取角色列表失败')
    }
  } catch (error) {
    console.error('获取列表异常:', error)
  } finally {
    loading.value = false
  }
}

const openPermission = (role) => {
  if (drawerRef.value) {
    drawerRef.value.open(role)
  }
}

const openCreateRole = () => {
  if (createRoleRef.value) {
    createRoleRef.value.open()
  }
}

onMounted(fetchData)
</script>