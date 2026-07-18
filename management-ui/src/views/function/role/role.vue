<template>
  <div class="function-page-shell function-page-shell--compact h-full min-h-0 font-body">
    <div class="function-page-container space-y-6">
      <header class="function-page-header">
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
          <el-button v-permission="'role:create'" type="primary" class="function-action-primary" @click="openCreateRole">
            <span class="material-symbols-outlined text-[20px]">add</span>新建角色
          </el-button>
        </div>
      </header>

      <section class="function-list-panel relative border-l-4 border-primary/20">
        <div class="function-table-scroll">
        <el-table v-loading="loading" :data="roles" row-key="id" class="w-full" table-layout="auto">
          <el-table-column
              v-for="column in roleTableColumns"
              :key="column.key"
              :prop="column.key"
              :label="column.label"
              :min-width="column.key === 'roleName' ? 240 : 160"
          >
            <template #default="{ row: role }">
              <div v-if="column.key === 'roleName'" class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-lg flex items-center justify-center shrink-0 bg-primary/10 text-primary">
                  <span class="material-symbols-outlined text-xl">badge</span>
                </div>
                <div class="font-bold text-primary text-sm">{{ role.roleName }}</div>
              </div>
              <template v-else-if="column.key === 'roleType'">
                <el-tag v-if="role.isSystem" type="warning" effect="light">系统内置</el-tag>
                <el-tag v-else type="info" effect="plain">自定义</el-tag>
              </template>
              <span v-else-if="column.key === 'createTime'" class="text-sm text-on-surface-variant">
                {{ formatTime(role.createTime) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="150" align="right">
            <template #default="{ row: role }">
              <el-button v-permission="'role:update'" link type="primary" @click="openPermission(role)">配置权限</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div v-if="loading" class="py-6" aria-hidden="true"></div>
            <div v-else-if="roleLoadError" class="space-y-4 px-4 py-6">
              <el-alert
                  title="角色列表加载失败"
                  :description="roleLoadError"
                  type="error"
                  :closable="false"
                  show-icon
              />
              <el-button type="primary" plain @click="fetchData">
                <span class="material-symbols-outlined text-[18px]">refresh</span>
                重新加载
              </el-button>
            </div>
            <el-empty v-else description="暂无角色数据" />
          </template>
        </el-table>
        </div>
        <div v-if="!loading && !roleLoadError" class="flex justify-end px-6 py-4">
          <el-pagination :current-page="1" :page-size="100" :total="roles.length" layout="total" />
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
import { nextTick, onMounted, ref } from 'vue'
import { ElAlert, ElButton, ElEmpty, ElPagination, ElTable, ElTableColumn, ElTag } from 'element-plus'
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
const roles = ref([])
const loading = ref(false)
const roleLoadError = ref('')
const drawerRef = ref(null)
const createRoleRef = ref(null)
const showPermissionDrawer = ref(false)
const showCreateDrawer = ref(false)

async function fetchData() {
  roleLoadError.value = ''
  loading.value = true
  try {
    const page = await getRolePage({ page: 1, size: 100 })
    roles.value = page?.data || []
  } catch (error) {
    roles.value = []
    roleLoadError.value = error?.msg || error?.message || '角色列表加载失败，请稍后重试。'
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
