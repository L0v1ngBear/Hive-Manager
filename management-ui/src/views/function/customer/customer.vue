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
          <div class="relative min-w-[260px]">
            <span
              class="material-symbols-outlined absolute top-1/2 left-3 -translate-y-1/2 text-[18px] text-on-surface-variant"
            >
              search
            </span>
            <input
              v-model.trim="keyword"
              class="w-full rounded-lg border border-outline-variant/20 bg-surface-container-low py-2.5 pr-4 pl-10 text-sm focus:ring-2 focus:ring-primary/30 focus:outline-none"
              placeholder="搜索客户名称、联系人或项目"
              type="text"
              @keyup.enter="fetchCustomerList"
            />
          </div>

          <button
            class="function-action-secondary"
            @click="fetchCustomerList"
          >
            <span class="material-symbols-outlined text-[20px]">refresh</span>刷新
          </button>

          <button
            class="function-action-primary"
            @click="openCreateDrawer"
          >
            <span class="material-symbols-outlined text-[20px]">domain_add</span>新建客户
          </button>
        </div>
      </header>

      <section class="mb-8 grid grid-cols-1 gap-4 md:grid-cols-4">
        <div class="group relative overflow-hidden rounded-xl bg-primary-container p-6 shadow-sm">
          <div class="absolute top-0 right-0 p-4 opacity-10 transition-transform group-hover:scale-110">
            <span class="material-symbols-outlined text-[80px]">corporate_fare</span>
          </div>
          <p class="text-xs font-bold tracking-widest text-on-primary-container uppercase black">客户总数</p>
          <h3 class="mt-2 text-4xl font-black text-black">{{ total }}</h3>
        </div>
      </section>

      <section class="relative overflow-hidden rounded-xl border border-outline-variant/20 bg-surface-container-lowest shadow-sm">
        <div
          v-if="loading"
          class="absolute inset-0 z-10 flex items-center justify-center bg-white/50 backdrop-blur-sm"
        >
          <span class="material-symbols-outlined animate-spin text-3xl text-primary">progress_activity</span>
        </div>

        <div class="overflow-x-auto">
          <table class="min-w-[1000px] w-full border-collapse text-left">
            <thead class="bg-surface-container-low/50">
              <tr>
                <th class="px-6 py-4 text-xs font-black tracking-wider text-on-surface-variant uppercase">客户名称</th>
                <th class="px-6 py-4 text-xs font-black tracking-wider text-on-surface-variant uppercase">客户类型</th>
                <th class="px-6 py-4 text-xs font-black tracking-wider text-on-surface-variant uppercase">首要联系人</th>
                <th class="px-6 py-4 text-xs font-black tracking-wider text-on-surface-variant uppercase">合作项目</th>
                <th class="px-6 py-4 text-right text-xs font-black tracking-wider text-on-surface-variant uppercase">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/10">
              <tr
                v-for="customer in customerList"
                :key="customer.id"
                class="group cursor-pointer transition-colors hover:bg-surface-container-high/30"
                @click="openDetail(customer.id)"
              >
                <td class="px-6 py-4">
                  <div class="flex items-center gap-3">
                    <div class="min-w-0">
                      <div class="truncate leading-tight font-bold text-primary">
                        {{ customer.customerName }}
                      </div>
                      <div class="mt-1 flex items-center gap-2 text-xs text-on-surface-variant">
                        <span>客户编号 #{{ customer.id }}</span>
                        <span class="inline-flex items-center rounded bg-primary/10 px-2 py-0.5 font-bold text-primary">
                          项目 {{ customer.projects?.length || customer.projectCount || 0 }} 个
                        </span>
                      </div>
                    </div>
                  </div>
                </td>

                <td class="px-6 py-4 text-sm font-bold text-secondary">
                  {{ getTypeLabel(customer.customerType) }}
                </td>

                <td class="px-6 py-4">
                  <div v-if="customer.contacts?.length">
                    <div class="text-sm font-bold text-primary">{{ customer.contacts[0].contactName || '未命名联系人' }}</div>
                    <div class="mt-0.5 text-xs text-on-surface-variant">{{ customer.contacts[0].contactPhone || '未填写电话' }}</div>
                  </div>
                  <span v-else class="text-xs text-on-surface-variant/50">暂无联系人</span>
                </td>

                <td class="px-6 py-4">
                  <div v-if="customer.projects?.length">
                    <div
                      class="max-w-[180px] truncate text-sm font-bold text-primary"
                      :title="customer.projects[0].projectName"
                    >
                      {{ customer.projects[0].projectName }}
                    </div>
                    <div class="mt-1 text-xs text-on-surface-variant">
                      {{ customer.projects[0].constructionArea || '未填写施工区域' }}
                    </div>
                  </div>
                  <span v-else class="text-xs text-on-surface-variant/50">暂无项目</span>
                </td>

                <td class="px-6 py-4 text-right">
                  <div class="flex justify-end gap-1 opacity-0 transition-opacity group-hover:opacity-100">
                    <button
                      class="rounded-md p-1.5 text-secondary transition-colors hover:bg-white"
                      title="编辑客户"
                      @click.stop="openEditDrawer(customer.id)"
                    >
                      <span class="material-symbols-outlined text-[18px]">edit</span>
                    </button>
                    <button
                      class="rounded-md p-1.5 text-primary transition-colors hover:bg-white"
                      title="查看详情"
                      @click.stop="openDetail(customer.id)"
                    >
                      <span class="material-symbols-outlined text-[18px]">visibility</span>
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="!loading && customerList.length === 0">
                <td colspan="5" class="px-6 py-12 text-center text-sm text-on-surface-variant">暂无客户数据</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="flex items-center justify-between border-t border-outline-variant/10 bg-surface-container-low/30 px-6 py-4 text-sm">
          <span class="text-on-surface-variant">共 {{ total }} 条</span>
          <div class="flex items-center gap-2">
            <button
              :disabled="pageNum <= 1 || loading"
              class="rounded border border-outline-variant/20 px-3 py-1.5 disabled:opacity-50"
              @click="changePage(pageNum - 1)"
            >
              上一页
            </button>
            <span>{{ pageNum }} / {{ totalPages }}</span>
            <button
              :disabled="pageNum >= totalPages || loading"
              class="rounded border border-outline-variant/20 px-3 py-1.5 disabled:opacity-50"
              @click="changePage(pageNum + 1)"
            >
              下一页
            </button>
          </div>
        </div>
      </section>

      <CustomerCreateDrawer
        v-model:visible="isDrawerOpen"
        :customer-id="editingCustomerId"
        @success="handleCustomerCreated"
      />

      <Teleport defer to="body">
        <transition name="fade">
          <div
            v-if="detailVisible"
            class="fixed inset-0 z-[60] bg-primary/20 backdrop-blur-sm"
            @click="detailVisible = false"
          ></div>
        </transition>
        <transition name="fade">
          <div
            v-if="detailVisible"
            class="fixed top-[8vh] inset-x-0 z-[70] mx-auto max-h-[84vh] w-[min(760px,calc(100vw-2rem))] overflow-y-auto rounded-2xl border border-outline-variant/20 bg-white p-6 shadow-2xl"
          >
            <div class="mb-6 flex items-center justify-between">
              <div>
                <h3 class="text-xl font-bold text-primary">客户详情</h3>
                <p class="mt-1 text-sm text-on-surface-variant">查看客户基础信息、联系人和合作项目。</p>
              </div>
              <button class="rounded-full p-2 hover:bg-surface-container-high" @click="detailVisible = false">
                <span class="material-symbols-outlined text-[20px]">close</span>
              </button>
            </div>

            <div v-if="detailLoading" class="py-16 text-center text-on-surface-variant">
              <span class="material-symbols-outlined animate-spin text-3xl text-primary">progress_activity</span>
            </div>

            <div v-else-if="detailData" class="space-y-6">
              <section class="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div class="rounded-xl bg-surface-container-low p-4">
                  <div class="text-xs text-on-surface-variant">客户名称</div>
                  <div class="mt-2 text-base font-bold text-primary">{{ detailData.customerName }}</div>
                </div>
                <div class="rounded-xl bg-surface-container-low p-4">
                  <div class="text-xs text-on-surface-variant">客户类型</div>
                  <div class="mt-2 text-base font-bold text-secondary">{{ getTypeLabel(detailData.customerType) }}</div>
                </div>
              </section>

              <section>
                <h4 class="mb-3 text-sm font-bold text-primary">联系人</h4>
                <div v-if="detailData.contacts?.length" class="space-y-3">
                  <div
                    v-for="(contact, index) in detailData.contacts"
                    :key="index"
                    class="rounded-xl border border-outline-variant/15 bg-surface-container-lowest p-4"
                  >
                    <div class="font-bold text-primary">{{ contact.contactName || '未命名联系人' }}</div>
                    <div class="mt-1 text-sm text-on-surface-variant">{{ contact.contactPhone || '未填写电话' }}</div>
                  </div>
                </div>
                <div v-else class="text-sm text-on-surface-variant">暂无联系人</div>
              </section>

              <section>
                <h4 class="mb-3 text-sm font-bold text-primary">合作项目</h4>
                <div v-if="detailData.projects?.length" class="space-y-3">
                  <div
                    v-for="(project, index) in detailData.projects"
                    :key="index"
                    class="rounded-xl border border-outline-variant/15 bg-surface-container-lowest p-4"
                  >
                    <div class="font-bold text-primary">{{ project.projectName || '未命名项目' }}</div>
                    <div class="mt-1 text-sm text-on-surface-variant">
                      施工区域：{{ project.constructionArea || '未填写' }}
                    </div>
                  </div>
                </div>
                <div v-else class="text-sm text-on-surface-variant">暂无项目</div>
              </section>
            </div>
          </div>
        </transition>
      </Teleport>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import CustomerCreateDrawer from './customerCreate.vue'
import { getCustomerDetail, getCustomerPage } from './api/customer'

const route = useRoute()
const isDrawerOpen = ref(false)
const editingCustomerId = ref(null)
const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const detailData = ref(null)
const keyword = ref('')
const customerList = ref([])
const total = ref(0)
const totalPages = ref(1)
const pageNum = ref(1)
const pageSize = ref(10)

const getTypeLabel = (type) => {
  const map = { 1: '直客（甲方）', 2: '总包方', 3: '分包方' }
  return map[type] || '未知类型'
}

async function fetchCustomerList() {
  loading.value = true
  try {
    const page = await getCustomerPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined
    })

    total.value = Number(page?.total || 0)
    totalPages.value = Math.max(1, Number(page?.pages || 1))
    customerList.value = page?.data || []
  } finally {
    loading.value = false
  }
}

async function handleCustomerCreated(payload) {
  editingCustomerId.value = null
  pageNum.value = 1
  await fetchCustomerList()
  ElMessage.success(`客户“${payload.customerName}”已保存`)
}

function openCreateDrawer() {
  editingCustomerId.value = null
  isDrawerOpen.value = true
}

function openEditDrawer(id) {
  editingCustomerId.value = id
  isDrawerOpen.value = true
}

async function openDetail(id) {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    detailData.value = await getCustomerDetail(id)
  } finally {
    detailLoading.value = false
  }
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
  if (routeKeyword !== keyword.value) {
    keyword.value = routeKeyword
    pageNum.value = 1
  }
}

onMounted(() => {
  applyRouteKeyword()
  fetchCustomerList()
})

watch(
  () => [route.query.keyword, route.query.q],
  async () => {
    applyRouteKeyword()
    await fetchCustomerList()
  }
)
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
