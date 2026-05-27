<template>
  <div class="function-page-shell h-full min-h-0 font-body">
    <div class="function-page-container space-y-6">
      <header class="function-page-header mb-8">
        <div>
          <div class="function-page-eyebrow">
            <span class="material-symbols-outlined">admin_panel_settings</span>
            权限治理中心
          </div>
          <h1 class="function-page-title">角色权限管理</h1>
          <p class="function-page-desc">配置组织职能角色，定义操作权限范围。</p>
        </div>
        <div class="flex flex-wrap items-center gap-3">
          <TableColumnSettings
            :columns="roleTableColumns"
            export-module="role"
            @move="moveRoleTableColumn"
            @reset="resetRoleTableColumns"
          />
          <button v-permission="'role:create'" @click="openCreateRole" class="function-action-primary">
            <span class="material-symbols-outlined text-[20px]">add</span>新建角色
          </button>
        </div>
      </header>

      <section class="bg-surface-container-lowest rounded-xl overflow-hidden shadow-sm border-l-4 border-primary/20 relative">
        <div v-if="loading" class="absolute inset-0 bg-white/50 backdrop-blur-sm z-20 flex items-center justify-center">
          <span class="material-symbols-outlined text-primary text-3xl animate-spin">progress_activity</span>
        </div>

        <div class="responsive-table-wrap">
          <table class="responsive-data-table w-full text-left border-collapse">
            <thead class="bg-surface-container-low/50">
              <tr>
                <th
                  v-for="column in roleTableColumns"
                  :key="column.key"
                  class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider"
                >
                  {{ column.label }}
                </th>
                <th class="px-6 py-4 text-right text-xs font-black text-on-surface-variant uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/10">
              <tr v-for="role in roles" :key="role.id" class="group transition-colors hover:bg-surface-container-high/30">
                <td
                  v-for="column in roleTableColumns"
                  :key="column.key"
                  :data-label="column.label"
                  class="px-6 py-4"
                  :class="column.key === 'createTime' ? 'text-sm text-on-surface-variant' : ''"
                >
                  <template v-if="column.key === 'roleName'">
                    <div class="flex items-center gap-3">
                      <div class="w-10 h-10 rounded-lg flex items-center justify-center shrink-0 bg-primary/10 text-primary">
                        <span class="material-symbols-outlined text-xl">badge</span>
                      </div>
                      <div class="font-bold text-primary text-sm">{{ role.roleName }}</div>
                    </div>
                  </template>
                  <template v-else-if="column.key === 'roleType'">
                    <span v-if="role.isSystem" class="text-[11px] font-bold px-2 py-1 rounded bg-secondary/10 text-secondary border border-secondary/20">系统内置</span>
                    <span v-else class="text-[11px] font-bold px-2 py-1 rounded bg-surface-container-high text-on-surface-variant border border-outline-variant/20">自定义</span>
                  </template>
                  <template v-else-if="column.key === 'createTime'">{{ formatTime(role.createTime) }}</template>
                </td>
                <td class="px-6 py-4 text-right" data-label="操作">
                  <button v-permission="'role:update'" @click="openPermission(role)" class="text-primary hover:bg-primary/10 px-4 py-2 rounded-lg text-sm font-bold transition-colors">配置权限</button>
                </td>
              </tr>
              <tr v-if="!loading && roles.length === 0">
                <td :colspan="roleTableColumnCount" class="px-6 py-12 text-center text-on-surface-variant">暂无角色数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <PermissionDrawer
        v-if="showPermissionDrawer"
        ref="drawerRef"
        @updated="fetchData"
        @closed="handlePermissionClosed"
      />
      <CreateRoleDrawer
        v-if="showCreateDrawer"
        ref="createRoleRef"
        @success="fetchData"
        @closed="handleCreateClosed"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import PermissionDrawer from './permissionDrawer.vue'
import CreateRoleDrawer from './createRoleDrawer.vue'
import { getRolePage } from './api/role.js'
import TableColumnSettings from '@/components/TableColumnSettings.vue'
import { useLocalTableColumns } from '@/composables/useLocalTableColumns'

const defaultRoleTableColumns = [
  { key: 'roleName', label: '角色名称' },
  { key: 'roleType', label: '角色类型' },
  { key: 'createTime', label: '创建日期' }
]
const {
  orderedColumns: roleTableColumns,
  moveColumn: moveRoleTableColumn,
  resetColumns: resetRoleTableColumns
} = useLocalTableColumns('role.list', defaultRoleTableColumns)
const roleTableColumnCount = computed(() => roleTableColumns.value.length + 1)
const roles = ref([])
const loading = ref(false)
const drawerRef = ref(null)
const createRoleRef = ref(null)
const showPermissionDrawer = ref(false)
const showCreateDrawer = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const page = await getRolePage({ page: 1, size: 100 })
    roles.value = page?.data || []
  } finally {
    loading.value = false
  }
}

async function openPermission(role) {
  showPermissionDrawer.value = true
  await nextTick()
  drawerRef.value?.open(role)
}

async function openCreateRole() {
  showCreateDrawer.value = true
  await nextTick()
  createRoleRef.value?.open()
}

function handlePermissionClosed() {
  showPermissionDrawer.value = false
}

function handleCreateClosed() {
  showCreateDrawer.value = false
}

function formatTime(value) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 19)
}

onMounted(fetchData)
</script>
