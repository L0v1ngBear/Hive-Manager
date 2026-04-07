<template>
  <div class="min-h-screen bg-surface text-on-surface p-4 md:p-8 overflow-x-hidden">
    <div class="max-w-7xl mx-auto space-y-8">

      <header class="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-8">
        <div>
          <h1 class="text-3xl md:text-4xl font-extrabold tracking-tight text-primary leading-none">客户档案库</h1>
          <p class="text-sm md:text-base text-on-surface-variant mt-3 max-w-lg">
            管理客户信息、多联系人及关联的合作工程项目。
          </p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button class="bg-surface-container-high text-on-surface flex items-center gap-2 px-5 py-2.5 rounded-lg text-sm font-bold shadow-sm hover:bg-surface-container-highest transition-colors active:scale-95">
            <span class="material-symbols-outlined text-[20px]">ios_share</span>导出数据
          </button>

          <button @click="isDrawerOpen = true" class="bg-primary text-on-primary flex items-center gap-2 px-5 py-2.5 rounded-lg text-sm font-bold shadow-md hover:shadow-lg hover:bg-primary/90 transition-all active:scale-95">
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
          <h3 class="text-4xl font-black text-white mt-2">128</h3>
        </div>
      </section>

      <section class="bg-surface-container-lowest rounded-xl overflow-hidden shadow-sm border border-outline-variant/20">
        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse min-w-[1000px]">
            <thead class="bg-surface-container-low/50">
            <tr>
              <th class="px-6 py-4 text-xs font-black text-on-surface-variant uppercase tracking-wider">客户名称 & 等级</th>
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
                    <div class="font-bold text-primary leading-tight flex items-center gap-2 truncate">
                      {{ customer.customerName }}
                    </div>
                    <div class="mt-1 flex items-center gap-1.5">
                        <span class="px-1.5 py-0.5 rounded text-[10px] font-black tracking-widest border" :class="getLevelTheme(customer.customerLevel).class">
                          {{ customer.customerLevel }}
                        </span>
                      <span class="text-xs text-on-surface-variant font-medium">{{ getLevelTheme(customer.customerLevel).label }}</span>
                    </div>
                  </div>
                </div>
              </td>

              <td class="px-6 py-4 text-sm font-bold text-secondary">
                {{ getTypeLabel(customer.customerType) }}
              </td>

              <td class="px-6 py-4 text-sm text-on-surface-variant font-medium flex items-center gap-1">
                <span class="material-symbols-outlined text-[16px] opacity-50">location_on</span>
                {{ customer.constructionArea }}
              </td>

              <td class="px-6 py-4">
                <div v-if="customer.contacts && customer.contacts.length > 0">
                  <div class="text-sm font-bold text-primary">{{ customer.contacts[0].contactName }}</div>
                  <div class="text-xs text-on-surface-variant mt-0.5">{{ customer.contacts[0].contactPhone }}</div>
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
                  <button class="p-1.5 hover:bg-white rounded-md text-primary transition-colors" title="查看详情">
                    <span class="material-symbols-outlined text-[18px]">visibility</span>
                  </button>
                  <button class="p-1.5 hover:bg-white rounded-md text-primary transition-colors" title="编辑">
                    <span class="material-symbols-outlined text-[18px]">edit</span>
                  </button>
                </div>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </section>

      <CustomerCreateDrawer
        v-model:visible="isDrawerOpen"
        @success="handleCustomerCreated"
      />

    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
// 导入抽屉组件
import CustomerCreateDrawer from './customerCreate.vue'

// ==========================================
// 状态控制
// ==========================================
const isDrawerOpen = ref(false)

// 接收子组件新建成功传回来的数据，并处理（例如发请求或直接塞入列表）
const handleCustomerCreated = (payload) => {
  console.log('接收到子组件传来的新建数据:', payload)
  // TODO: 这里调用 API 或者刷新列表数据
  // axios.post('/api/customer/add', payload).then(() => fetchList())
}

// ==========================================
// 列表展示数据与逻辑
// ==========================================
const customerList = ref([
  {
    id: 1,
    customerName: '中建八局第一建设有限公司',
    customerLevel: 'T1',
    customerType: 2,
    constructionArea: '上海市浦东新区',
    contacts: [
      { contactName: '李建国 (采购总监)', contactPhone: '138-0000-1111' },
      { contactName: '王财务', contactPhone: '139-2222-3333' }
    ],
    projects: [
      { projectName: '上海张江高科技园区二期' },
      { projectName: '临港新片区配套工程' }
    ]
  },
  {
    id: 2,
    customerName: '绿城装饰工程有限公司',
    customerLevel: 'T2',
    customerType: 3,
    constructionArea: '浙江省杭州市',
    contacts: [
      { contactName: '张伟', contactPhone: '150-8888-9999' }
    ],
    projects: [
      { projectName: '绿城春风金沙内装工程' }
    ]
  }
])

const getTypeLabel = (type) => {
  const map = { 1: '直客 (甲方)', 2: '总包方', 3: '分包方' }
  return map[type] || '未知类型'
}

const getLevelTheme = (level) => {
  const themes = {
    'T1': { class: 'bg-primary/10 text-primary border-primary/20', label: '战略级' },
    'T2': { class: 'bg-secondary/10 text-secondary border-secondary/20', label: '大宗客户' },
    'T3': { class: 'bg-surface-container-highest text-on-surface-variant border-outline-variant/30', label: '标准客户' }
  }
  return themes[level] || themes['T3']
}
</script>
