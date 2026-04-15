<template>
  <div class="h-full min-h-0 bg-surface text-on-surface overflow-x-hidden">
    <div class="max-w-7xl mx-auto space-y-6">
      <header class="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-8">
        <div>
          <h1 class="text-3xl md:text-4xl font-extrabold tracking-tight text-primary leading-none">客户档案库</h1>
          <p class="text-sm md:text-base text-on-surface-variant mt-3 max-w-lg">
            管理客户信息、多联系人及关联的合作工程项目。
          </p>
        </div>
        <div class="flex flex-wrap gap-3">
          <div class="relative min-w-[260px]">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-[18px]">search</span>
            <input
              v-model.trim="keyword"
              @keyup.enter="fetchCustomerList"
              type="text"
              class="w-full bg-surface-container-low text-sm rounded-lg pl-10 pr-4 py-2.5 border border-outline-variant/20 focus:outline-none focus:ring-2 focus:ring-primary/30"
              placeholder="搜索客户名称、联系人或项目"
            />
          </div>

          <button
            @click="fetchCustomerList"
            class="bg-surface-container-high text-on-surface flex items-center gap-2 px-5 py-2.5 rounded-lg text-sm font-bold shadow-sm hover:bg-surface-container-highest transition-colors active:scale-95"
          >
            <span class="material-symbols-outlined text-[20px]">refresh</span>刷新
          </button>

          <button
            @click="isDrawerOpen = true"
            class="bg-primary text-on-primary flex items-center gap-2 px-5 py-2.5 rounded-lg text-sm font-bold shadow-md hover:shadow-lg hover:bg-primary/90 transition-all active:scale-95"
          >
            <span class="material-symbols-outlined text-[20px]">domain_add</span>新建客户
          </button>
        </div>
      </header>

      <section class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
        <div class="bg-primary-container p-6 rounded-xl relative overflow-hidden group shadow-sm">
          <div class="absolute top-0 right-0 p-4 opacity-10 group-hover:scale-110 transition-transform">
            <span class="material-symbols-outlined text-[80px]">corporate_fare</span>
          </div>
          <p class="text-on-primary-container text-xs font-bold uppercase tracking-widest">客户总数</p>
          <h3 class="text-4xl font-black text-white mt-2">{{ total }}</h3>
        </div>
      </section>

      <section class="bg-surface-container-lowest rounded-xl overflow-hidden shadow-sm border border-outline-variant/20 relative">
        <div v-if="loading" class="absolute inset-0 bg-white/50 backdrop-blur-sm z-10 flex items-center justify-center">
          <span class="material-symbols-outlined text-primary text-3xl animate-spin">progress_activity</span>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse min-w-[1000px]">
            <thead class="bg-surface-container-low/50">
              <tr>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">客户名称</th>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">客户类型</th>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">施工区域</th>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">首要联系人</th>
                <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">合作项目</th>
                <th class="px-6 py-4 text-right text-xs font-black text-on-surface-variant uppercase tracking-wider">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/10">
              <tr v-for="customer in customerList" :key="customer.id" class="group transition-colors hover:bg-surface-container-high/30">
                <td class="px-6 py-4">
                  <div class="flex items-center gap-3">
                    <div class="min-w-0">
                      <div class="font-bold text-primary leading-tight truncate">
                        {{ customer.customerName }}
                      </div>
                      <div class="mt-1 flex items-center gap-2 text-xs text-on-surface-variant">
                        <span>客户编号 #{{ customer.id }}</span>
                        <span class="inline-flex items-center px-2 py-0.5 rounded bg-primary/10 text-primary font-bold">
                          项目 {{ customer.projectCount || 0 }} 个
                        </span>
                      </div>
                    </div>
                  </div>
                </td>

                <td class="px-6 py-4 text-sm font-bold text-secondary">
                  {{ getTypeLabel(customer.customerType) }}
                </td>

                <td class="px-6 py-4 text-sm text-on-surface-variant font-medium flex items-center gap-1">
                  <span class="material-symbols-outlined text-[16px] opacity-50">location_on</span>
                  {{ customer.constructionArea || '未填写' }}
                </td>

                <td class="px-6 py-4">
                  <div v-if="customer.contacts && customer.contacts.length > 0">
                    <div class="text-sm font-bold text-primary">{{ customer.contacts[0].contactName }}</div>
                    <div class="text-xs text-on-surface-variant mt-0.5">{{ customer.contacts[0].contactPhone || '未填写电话' }}</div>
                  </div>
                  <span v-else class="text-xs text-on-surface-variant/50">暂无联系人</span>
                </td>

                <td class="px-6 py-4">
                  <div v-if="customer.projects && customer.projects.length > 0">
                    <div class="text-sm font-bold text-primary truncate max-w-[150px]" :title="customer.projects[0].projectName">
                      {{ customer.projects[0].projectName }}
                    </div>
                    <div class="text-[10px] text-tertiary font-bold mt-0.5 bg-tertiary-fixed/30 inline-block px-1.5 rounded">
                      共 {{ customer.projects.length }} 个项目
                    </div>
                  </div>
                  <span v-else class="text-xs text-on-surface-variant/50">暂无项目</span>
                </td>

                <td class="px-6 py-4 text-right">
                  <div class="flex justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button @click="openDetail(customer.id)" class="p-1.5 hover:bg-white rounded-md text-primary transition-colors" title="查看详情">
                      <span class="material-symbols-outlined text-[18px]">visibility</span>
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="!loading && customerList.length === 0">
                <td colspan="6" class="px-6 py-12 text-center text-sm text-on-surface-variant">暂无客户数据</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="flex items-center justify-between px-6 py-4 border-t border-outline-variant/10 bg-surface-container-low/30 text-sm">
          <span class="text-on-surface-variant">共 {{ total }} 条</span>
          <div class="flex items-center gap-2">
            <button
              @click="changePage(pageNum - 1)"
              :disabled="pageNum <= 1 || loading"
              class="px-3 py-1.5 rounded border border-outline-variant/20 disabled:opacity-50"
            >上一页</button>
            <span>{{ pageNum }} / {{ totalPages }}</span>
            <button
              @click="changePage(pageNum + 1)"
              :disabled="pageNum >= totalPages || loading"
              class="px-3 py-1.5 rounded border border-outline-variant/20 disabled:opacity-50"
            >下一页</button>
          </div>
        </div>
      </section>

      <CustomerCreateDrawer v-model:visible="isDrawerOpen" @success="handleCustomerCreated" />

      <Teleport defer to="body">
        <transition name="fade">
          <div v-if="detailVisible" class="fixed inset-0 bg-primary/20 backdrop-blur-sm z-[60]" @click="detailVisible = false"></div>
        </transition>
        <transition name="fade">
          <div v-if="detailVisible" class="fixed inset-x-0 top-[8vh] mx-auto w-[min(760px,calc(100vw-2rem))] max-h-[84vh] overflow-y-auto bg-white rounded-2xl shadow-2xl z-[70] border border-outline-variant/20 p-6">
            <div class="flex items-center justify-between mb-6">
              <div>
                <h3 class="text-xl font-bold text-primary">客户详情</h3>
                <p class="text-sm text-on-surface-variant mt-1">查看客户基础信息、联系人和项目</p>
              </div>
              <button @click="detailVisible = false" class="p-2 hover:bg-surface-container-high rounded-full">
                <span class="material-symbols-outlined text-[20px]">close</span>
              </button>
            </div>

            <div v-if="detailLoading" class="py-16 text-center text-on-surface-variant">
              <span class="material-symbols-outlined text-primary text-3xl animate-spin">progress_activity</span>
            </div>

            <div v-else-if="detailData" class="space-y-6">
              <section class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div class="bg-surface-container-low rounded-xl p-4">
                  <div class="text-xs text-on-surface-variant">客户名称</div>
                  <div class="mt-2 text-base font-bold text-primary">{{ detailData.customerName }}</div>
                </div>
                <div class="bg-surface-container-low rounded-xl p-4">
                  <div class="text-xs text-on-surface-variant">客户类型</div>
                  <div class="mt-2 text-base font-bold text-secondary">{{ getTypeLabel(detailData.customerType) }}</div>
                </div>
                <div class="bg-surface-container-low rounded-xl p-4 md:col-span-2">
                  <div class="text-xs text-on-surface-variant">施工区域</div>
                  <div class="mt-2 text-base font-bold text-primary">{{ detailData.constructionArea || '未填写' }}</div>
                </div>
              </section>

              <section>
                <h4 class="text-sm font-bold text-primary mb-3">联系人</h4>
                <div v-if="detailData.contacts?.length" class="space-y-3">
                  <div v-for="(contact, index) in detailData.contacts" :key="index" class="rounded-xl border border-outline-variant/15 p-4 bg-surface-container-lowest">
                    <div class="font-bold text-primary">{{ contact.contactName || '未命名联系人' }}</div>
                    <div class="text-sm text-on-surface-variant mt-1">{{ contact.contactPhone || '未填写电话' }}</div>
                  </div>
                </div>
                <div v-else class="text-sm text-on-surface-variant">暂无联系人</div>
              </section>

              <section>
                <h4 class="text-sm font-bold text-primary mb-3">合作项目</h4>
                <div v-if="detailData.projects?.length" class="space-y-3">
                  <div v-for="(project, index) in detailData.projects" :key="index" class="rounded-xl border border-outline-variant/15 p-4 bg-surface-container-lowest">
                    <div class="font-bold text-primary">{{ project.projectName || '未命名项目' }}</div>
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
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import CustomerCreateDrawer from './customerCreate.vue'
import { getCustomerDetail, getCustomerPage } from './api/customer'

const isDrawerOpen = ref(false)
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
  const map = { 1: '直客 (甲方)', 2: '总包方', 3: '分包方' }
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

    const baseList = page?.data || []
    total.value = Number(page?.total || 0)
    totalPages.value = Math.max(1, Number(page?.pages || 1))

    const detailList = await Promise.all(
      baseList.map(async (item) => {
        try {
          const detail = await getCustomerDetail(item.id)
          return {
            ...item,
            contacts: detail?.contacts || [],
            projects: detail?.projects || (item.projectNames || []).map((name) => ({ projectName: name }))
          }
        } catch (error) {
          return {
            ...item,
            contacts: [],
            projects: (item.projectNames || []).map((name) => ({ projectName: name }))
          }
        }
      })
    )

    customerList.value = detailList
  } finally {
    loading.value = false
  }
}

async function handleCustomerCreated(payload) {
  pageNum.value = 1
  await fetchCustomerList()
  ElMessage.success(`客户“${payload.customerName}”已保存`)
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

onMounted(fetchCustomerList)
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
