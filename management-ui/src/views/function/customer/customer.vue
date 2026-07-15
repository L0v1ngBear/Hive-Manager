<template>
  <div class="function-page-shell h-full min-h-0">
    <div class="function-page-container space-y-6">
      <header class="function-page-header mb-8">
        <div>
          <div class="function-page-eyebrow">
            <span class="material-symbols-outlined">handshake</span>
            客户经营中心
          </div>
          <h1 class="function-page-title">客户档案库</h1>
          <p class="function-page-desc">
            管理客户基础信息、联系人和合作项目，施工区域按项目维度维护。
          </p>
        </div>

        <div class="flex flex-wrap gap-3">
          <el-input
            v-model.trim="filters.keyword"
            class="w-full sm:w-[260px]"
            placeholder="搜索客户名称、联系人、项目或负责人"
            clearable
            @keyup.enter="handleFilter"
          >
            <template #prefix>
              <span class="material-symbols-outlined text-[18px]">search</span>
            </template>
          </el-input>
          <el-select v-model="filters.customerType" class="w-full sm:w-[180px]" placeholder="全部客户类型" clearable :value-on-clear="''">
            <el-option label="直客（甲方）" value="1" />
            <el-option label="总包方" value="2" />
            <el-option label="分包方" value="3" />
          </el-select>
          <el-date-picker
            v-model="filters.createStart"
            class="w-full sm:w-[150px]"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="创建开始"
          />
          <el-date-picker
            v-model="filters.createEnd"
            class="w-full sm:w-[150px]"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="创建结束"
          />
          <el-button :loading="loading" @click="handleFilter">
            <span class="material-symbols-outlined text-[20px]">refresh</span>
            刷新
          </el-button>
          <el-button :disabled="loading" @click="resetFilter">
            <span class="material-symbols-outlined text-[20px]">restart_alt</span>
            重置
          </el-button>
          <TableColumnSettings
            :columns="visibleCustomerColumns"
            :export-rows="customerList"
            :export-cell="customerExportCell"
            export-file-name="客户档案库"
            export-sheet-name="客户档案库"
            export-module="customer"
            :export-disabled="!canExportTable"
            export-disabled-reason="当前账号暂无表格导出权限"
            @move="moveCustomerTableColumn"
            @reset="resetCustomerTableColumns"
          />
          <el-button
            type="primary"
            :disabled="!canCreateCustomer"
            :class="permissionDisabledClass(!canCreateCustomer)"
            :title="canCreateCustomer ? '新建客户' : '当前账号暂无新增客户权限'"
            @click="openCreateDrawer"
          >
            <span class="material-symbols-outlined text-[20px]">domain_add</span>
            新建客户
          </el-button>
        </div>
      </header>

      <section class="mb-8 grid grid-cols-1 gap-4 md:grid-cols-4">
        <div class="group relative overflow-hidden rounded-xl bg-primary-container p-6 shadow-sm">
          <div class="absolute top-0 right-0 p-4 opacity-10 transition-transform group-hover:scale-110">
            <span class="material-symbols-outlined text-[80px]">corporate_fare</span>
          </div>
          <p class="text-xs font-bold tracking-widest text-on-primary/80 uppercase">客户总数</p>
          <h3 class="mt-2 text-4xl font-black text-white">{{ total }}</h3>
        </div>
      </section>

      <section class="relative overflow-hidden rounded-xl border border-outline-variant/20 bg-surface-container-lowest shadow-sm">
        <el-result v-if="listError" :icon="listError.icon" :title="listError.title" :sub-title="listError.message">
          <template #extra>
            <el-button type="primary" @click="fetchCustomerList">重试</el-button>
          </template>
        </el-result>
        <template v-else>
          <el-table
            :data="customerList"
            row-key="id"
            v-loading="loading"
            class="w-full"
            @row-click="handleCustomerRowClick"
          >
          <el-table-column
            v-for="field in visibleCustomerColumns"
            :key="field.key"
            :label="field.label"
            :min-width="field.key === 'customerName' ? 180 : 130"
          >
            <template #default="{ row: customer }">
              <template v-if="field.key === 'customerName'">
                <div class="min-w-0">
                  <div class="truncate leading-tight font-bold text-primary">{{ customer.customerName }}</div>
                  <div class="mt-1 text-xs text-on-surface-variant">客户编号 #{{ customer.id }}</div>
                </div>
              </template>
              <span v-else-if="field.key === 'customerType'" class="text-sm font-bold text-secondary">
                {{ getTypeLabel(customer.customerType) }}
              </span>
              <template v-else-if="field.key === 'projectName'">
                <div v-if="customer.projects?.length" class="max-w-[180px] truncate text-sm font-bold text-primary" :title="customer.projects[0].projectName">
                  {{ customer.projects[0].projectName }}
                </div>
                <span v-else class="text-xs text-on-surface-variant/50">暂无项目</span>
              </template>
              <template v-else>{{ customerColumnText(customer, field.key) }}</template>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="128" align="right">
            <template #default="{ row: customer }">
              <div class="flex justify-end gap-1">
                <el-button
                  circle
                  text
                  :disabled="!canUpdateCustomer"
                  :class="permissionDisabledClass(!canUpdateCustomer)"
                  :title="canUpdateCustomer ? '编辑客户' : '当前账号暂无编辑客户权限'"
                  @click.stop="openEditDrawer(customer.id)"
                >
                  <span class="material-symbols-outlined text-[18px]">edit</span>
                </el-button>
                <el-button
                  circle
                  text
                  type="primary"
                  :disabled="!canViewCustomerDetail"
                  :class="permissionDisabledClass(!canViewCustomerDetail)"
                  :title="canViewCustomerDetail ? '查看详情' : '当前账号暂无查看客户详情权限'"
                  @click.stop="openDetail(customer.id)"
                >
                  <span class="material-symbols-outlined text-[18px]">visibility</span>
                </el-button>
              </div>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty v-if="!loading" :description="customerEmptyDescription" />
          </template>
        </el-table>

          <div class="flex flex-wrap items-center justify-between gap-3 border-t border-outline-variant/10 bg-surface-container-low/30 px-6 py-4 text-sm">
            <span class="text-on-surface-variant">共 {{ total }} 条</span>
            <el-pagination
              :current-page="pageNum"
              :page-size="pageSize"
              :total="total"
              :disabled="loading"
              layout="prev, pager, next"
              @current-change="changePage"
            />
          </div>
        </template>
      </section>

      <CustomerCreateDrawer
        v-model:visible="isDrawerOpen"
        :customer-id="editingCustomerId"
        @success="handleCustomerCreated"
      />

      <el-dialog v-model="detailVisible" title="客户详情" width="min(760px, calc(100vw - 2rem))" destroy-on-close @close="invalidateCustomerDetail">
        <p class="mb-6 text-sm text-on-surface-variant">查看客户基础信息、联系人和合作项目。</p>
        <div v-if="detailLoading" class="py-16 text-center text-on-surface-variant">
          <span class="material-symbols-outlined animate-spin text-3xl text-primary">progress_activity</span>
        </div>
        <el-result v-else-if="detailError" :icon="detailError.icon" :title="detailError.title" :sub-title="detailError.message">
          <template #extra>
            <el-button type="primary" @click="retryCustomerDetail">重试</el-button>
          </template>
        </el-result>
        <el-empty v-else-if="detailEmpty" description="未找到客户详情" />
        <div v-else-if="detailData" class="space-y-6">
          <section class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div v-if="isCustomerFieldVisible('customerName')" class="rounded-xl bg-surface-container-low p-4">
              <div class="text-xs text-on-surface-variant">{{ fieldLabel('customerName', '客户名称') }}</div>
              <div class="mt-2 text-base font-bold text-primary">{{ detailData.customerName }}</div>
            </div>
            <div v-if="isCustomerFieldVisible('customerType')" class="rounded-xl bg-surface-container-low p-4">
              <div class="text-xs text-on-surface-variant">{{ fieldLabel('customerType', '客户类型') }}</div>
              <div class="mt-2 text-base font-bold text-secondary">{{ getTypeLabel(detailData.customerType) }}</div>
            </div>
          </section>

          <section v-if="isCustomerFieldVisible('contactName') || isCustomerFieldVisible('contactPhone')">
            <h4 class="mb-3 text-sm font-bold text-primary">{{ fieldLabel('contactName', '联系人') }}</h4>
            <div v-if="detailData.contacts?.length" class="space-y-3">
              <div v-for="(contact, index) in detailData.contacts" :key="index" class="rounded-xl border border-outline-variant/15 bg-surface-container-lowest p-4">
                <div v-if="isCustomerFieldVisible('contactName')" class="font-bold text-primary">{{ contact.contactName || '未命名联系人' }}</div>
                <div v-if="isCustomerFieldVisible('contactPhone')" class="mt-1 text-sm text-on-surface-variant">{{ contact.contactPhone || '未填写电话' }}</div>
              </div>
            </div>
            <div v-else class="text-sm text-on-surface-variant">暂无联系人</div>
          </section>

          <section v-if="isCustomerFieldVisible('projectName') || isCustomerFieldVisible('constructionArea') || isCustomerFieldVisible('projectOwner')">
            <h4 class="mb-3 text-sm font-bold text-primary">{{ fieldLabel('projectName', '合作项目') }}</h4>
            <div v-if="detailData.projects?.length" class="space-y-3">
              <div v-for="(project, index) in detailData.projects" :key="index" class="rounded-xl border border-outline-variant/15 bg-surface-container-lowest p-4">
                <div v-if="isCustomerFieldVisible('projectName')" class="font-bold text-primary">{{ project.projectName || '未命名项目' }}</div>
                <div v-if="isCustomerFieldVisible('constructionArea')" class="mt-1 text-sm text-on-surface-variant">
                  {{ fieldLabel('constructionArea', '施工区域') }}：{{ project.constructionArea || '未填写' }}
                </div>
                <div v-if="isCustomerFieldVisible('projectOwner')" class="mt-1 text-sm text-on-surface-variant">
                  {{ fieldLabel('projectOwner', '项目负责人') }}：{{ project.projectOwner || '未填写' }}
                </div>
              </div>
            </div>
            <div v-else class="text-sm text-on-surface-variant">暂无项目</div>
          </section>
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  ElButton,
  ElDatePicker,
  ElDialog,
  ElEmpty,
  ElInput,
  ElMessage,
  ElOption,
  ElPagination,
  ElResult,
  ElSelect,
  ElTable,
  ElTableColumn
} from 'element-plus'
import { useRoute } from 'vue-router'
import { getCurrentTenantFieldConfig } from '@/api/tenantFieldConfig'
import {
  defaultTenantFieldConfig,
  mergeTenantFieldConfig,
  tenantFieldLabel,
  tenantFieldVisible,
  visibleTenantFields
} from '@/utils/tenantFieldConfig'
import TableColumnSettings from '@/components/TableColumnSettings.vue'
import { useLocalTableColumns } from '@/composables/useLocalTableColumns'
import { useUserStore } from '@/stores/user'
import { createLatestRequestRunner } from '@/utils/latestRequest'
import CustomerCreateDrawer from './customerCreate.vue'
import { getCustomerDetail, getCustomerPage } from './api/customer'
import { resolveCustomerDetailOutcome } from './customerState'

const route = useRoute()
const userStore = useUserStore()
const isDrawerOpen = ref(false)
const editingCustomerId = ref(null)
const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const detailData = ref(null)
const detailError = ref(null)
const detailEmpty = ref(false)
const selectedCustomerId = ref(null)
const filters = reactive({ keyword: '', customerType: '', createStart: '', createEnd: '' })
const customerList = ref([])
const listError = ref(null)
const total = ref(0)
const totalPages = ref(1)
const pageNum = ref(1)
const pageSize = ref(10)
const customerFieldConfig = ref(defaultTenantFieldConfig('customer'))
const canCreateCustomer = computed(() => userStore.hasPermission('customer:create'))
const canUpdateCustomer = computed(() => userStore.hasPermission('customer:update'))
const canViewCustomerDetail = computed(() => userStore.hasPermission('customer:detail'))
const canExportTable = computed(() => userStore.hasPermission('table:export'))
const hasCustomerFilters = computed(() => Boolean(
  filters.keyword || filters.customerType || filters.createStart || filters.createEnd
))
const customerEmptyDescription = computed(() => hasCustomerFilters.value ? '没有符合筛选条件的客户' : '暂无客户数据')

const customerListRunner = createLatestRequestRunner({
  onLoading(value) {
    loading.value = value
    if (value) {
      listError.value = null
      customerList.value = []
      total.value = 0
      totalPages.value = 1
    }
  },
  onSuccess(page) {
    total.value = Number(page?.total || 0)
    totalPages.value = Math.max(1, Number(page?.pages || 1))
    customerList.value = page?.data || []
  },
  onError(error) {
    listError.value = resolveCustomerListError(error)
  }
})

const customerDetailRunner = createLatestRequestRunner({
  onLoading(value) {
    detailLoading.value = value
    if (value) {
      detailData.value = null
      detailError.value = null
      detailEmpty.value = false
    }
  },
  onSuccess(detail) {
    if (detail) {
      detailData.value = detail
      return
    }
    detailEmpty.value = true
  },
  onError(error) {
    detailError.value = resolveCustomerDetailOutcome(error).error
  }
})

const customerColumnRenderers = new Set(['customerName', 'customerType', 'contactName', 'contactPhone', 'projectName', 'projectOwner', 'projectCount', 'constructionArea'])
const defaultCustomerColumns = computed(() => visibleTenantFields(customerFieldConfig.value, 'customerName').filter((field) => customerColumnRenderers.has(field.key)))
const {
  orderedColumns: visibleCustomerColumns,
  moveColumn: moveCustomerTableColumn,
  resetColumns: resetCustomerTableColumns
} = useLocalTableColumns('customer.list', defaultCustomerColumns)

const getTypeLabel = (type) => {
  const map = { 1: '直客（甲方）', 2: '总包方', 3: '分包方' }
  return map[type] || '未知类型'
}

const firstContact = (customer) => Array.isArray(customer?.contacts) && customer.contacts.length > 0 ? customer.contacts[0] : null
const firstProject = (customer) => Array.isArray(customer?.projects) && customer.projects.length > 0 ? customer.projects[0] : null
const isCustomerFieldVisible = (key) => tenantFieldVisible(customerFieldConfig.value, key)
const fieldLabel = (key, fallback) => tenantFieldLabel(customerFieldConfig.value, key, fallback)

function customerColumnText(customer, key) {
  if (key === 'contactName') return firstContact(customer)?.contactName || '--'
  if (key === 'contactPhone') return firstContact(customer)?.contactPhone || '--'
  if (key === 'projectCount') return `${customer?.projects?.length || customer?.projectCount || 0} 个`
  if (key === 'constructionArea') return firstProject(customer)?.constructionArea || customer?.constructionArea || '--'
  if (key === 'projectOwner') return firstProject(customer)?.projectOwner || '--'
  return customer?.[key] || '--'
}

function customerExportCell(customer, field) {
  if (field.key === 'customerName') {
    return [customer?.customerName, customer?.id ? `客户编号 #${customer.id}` : ''].filter(Boolean).join(' ')
  }
  if (field.key === 'customerType') return getTypeLabel(customer?.customerType)
  if (field.key === 'projectName') return firstProject(customer)?.projectName || '暂无项目'
  return customerColumnText(customer, field.key)
}

async function fetchCustomerFieldConfig() {
  try {
    const rows = await getCurrentTenantFieldConfig('customer')
    customerFieldConfig.value = mergeTenantFieldConfig('customer', rows)
  } catch (error) {
    customerFieldConfig.value = defaultTenantFieldConfig('customer')
  }
}

async function fetchCustomerList() {
  await customerListRunner.run(() => getCustomerPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: filters.keyword || undefined,
      customerType: filters.customerType === '' ? undefined : Number(filters.customerType),
      createStart: filters.createStart || undefined,
      createEnd: filters.createEnd || undefined
    }))
}

async function handleCustomerCreated(payload) {
  editingCustomerId.value = null
  pageNum.value = 1
  await fetchCustomerList()
  ElMessage.success(`客户“${payload.customerName}”已保存`)
}

function openCreateDrawer() {
  if (!canCreateCustomer.value) return
  editingCustomerId.value = null
  isDrawerOpen.value = true
}

function openEditDrawer(id) {
  if (!canUpdateCustomer.value) return
  editingCustomerId.value = id
  isDrawerOpen.value = true
}

async function openDetail(id) {
  if (!canViewCustomerDetail.value) return
  selectedCustomerId.value = id
  detailVisible.value = true
  await customerDetailRunner.run(() => getCustomerDetail(id))
}

async function retryCustomerDetail() {
  if (!selectedCustomerId.value) return
  await customerDetailRunner.run(() => getCustomerDetail(selectedCustomerId.value))
}

function invalidateCustomerDetail() {
  customerDetailRunner.invalidate()
  selectedCustomerId.value = null
  detailData.value = null
  detailError.value = null
  detailEmpty.value = false
}

function handleCustomerRowClick(customer) {
  if (!canViewCustomerDetail.value) return
  openDetail(customer.id)
}

async function changePage(nextPage) {
  if (nextPage < 1 || nextPage > totalPages.value || nextPage === pageNum.value) {
    return
  }
  pageNum.value = nextPage
  await fetchCustomerList()
}

function applyRouteKeyword() {
  const routeKeyword = String(route.query.keyword || route.query.q || '').trim()
  if (routeKeyword !== filters.keyword) {
    filters.keyword = routeKeyword
    pageNum.value = 1
  }
}

function handleFilter() {
  pageNum.value = 1
  fetchCustomerList()
}

function resetFilter() {
  filters.keyword = ''
  filters.customerType = ''
  filters.createStart = ''
  filters.createEnd = ''
  pageNum.value = 1
  fetchCustomerList()
}

function resolveCustomerListError(error) {
  const status = Number(error?.response?.status || error?.status || error?.code || 0)
  if (status === 401) {
    return {
      icon: 'warning',
      title: '登录状态已失效',
      message: '请重新登录后再加载客户列表。'
    }
  }
  if (status === 403) {
    return {
      icon: 'warning',
      title: '暂无客户列表权限',
      message: '当前账号没有 customer:page 权限，请联系管理员。'
    }
  }
  if (status >= 500) {
    return {
      icon: 'error',
      title: '客户服务暂时不可用',
      message: '服务器处理失败，请稍后重试。'
    }
  }
  return {
    icon: 'error',
    title: '客户列表加载失败',
    message: '网络连接异常，请检查网络后重试。'
  }
}

function permissionDisabledClass(disabled) {
  return disabled ? 'cursor-not-allowed opacity-50 grayscale' : ''
}

onMounted(async () => {
  applyRouteKeyword()
  await Promise.all([fetchCustomerFieldConfig(), fetchCustomerList()])
})

watch(
  () => [route.query.keyword, route.query.q],
  async () => {
    applyRouteKeyword()
    await fetchCustomerList()
  }
)
</script>
