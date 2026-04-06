<template>
  <div class="min-h-screen bg-surface text-on-surface p-4 md:p-8 overflow-x-hidden">
    <div class="max-w-7xl mx-auto space-y-8">

      <header class="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-8">
        <div>
          <nav class="flex items-center gap-2 text-xs font-medium text-on-surface-variant mb-2 tracking-widest uppercase">
            <span>客户关系 (CRM)</span>
            <span class="material-symbols-outlined text-xs">chevron_right</span>
            <span class="text-primary font-bold">客户管理</span>
          </nav>
          <h1 class="text-3xl md:text-4xl font-extrabold tracking-tight text-primary leading-none">客户档案库</h1>
          <p class="text-sm md:text-base text-on-surface-variant mt-3 max-w-lg">
            管理客户信息、多联系人及关联的合作工程项目。
          </p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button class="bg-surface-container-high text-on-surface flex items-center gap-2 px-5 py-2.5 rounded-lg text-sm font-bold shadow-sm hover:bg-surface-container-highest transition-colors active:scale-95">
            <span class="material-symbols-outlined text-[20px]">ios_share</span>导出数据
          </button>
          <button @click="openCreateDrawer" class="bg-primary text-on-primary flex items-center gap-2 px-5 py-2.5 rounded-lg text-sm font-bold shadow-md hover:shadow-lg hover:bg-primary/90 transition-all active:scale-95">
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

        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm border-l-4 border-primary relative overflow-hidden">
          <p class="text-on-surface-variant text-xs font-bold uppercase tracking-widest">T1 战略级客户</p>
          <div class="flex items-end justify-between mt-2">
            <h3 class="text-4xl font-black text-primary">15</h3>
          </div>
          <div class="absolute -right-4 -bottom-4 opacity-5 pointer-events-none">
            <span class="material-symbols-outlined text-[100px]" style="font-variation-settings: 'FILL' 1;">workspace_premium</span>
          </div>
        </div>

        <div class="bg-surface-container-lowest p-6 rounded-xl shadow-sm border border-outline-variant/10">
          <p class="text-on-surface-variant text-xs font-bold uppercase tracking-widest">进行中项目</p>
          <h3 class="text-4xl font-black text-primary mt-2">42</h3>
          <p class="text-on-surface-variant text-xs mt-3 font-medium">覆盖 12 个施工区域</p>
        </div>
      </section>

      <section class="bg-surface-container-lowest rounded-xl overflow-hidden shadow-sm border border-outline-variant/20">

        <div class="p-4 md:p-6 flex flex-col lg:flex-row lg:items-center justify-between border-b border-outline-variant/20 gap-4 bg-surface-container-lowest">
          <div class="relative w-full lg:w-80 shrink-0">
            <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant/50 text-[18px]">search</span>
            <input type="text" placeholder="搜索客户名称、区域或联系人..." class="w-full pl-9 pr-4 py-2.5 bg-surface-container-low border-none rounded-lg text-sm focus:ring-2 focus:ring-primary transition-shadow font-medium" />
          </div>

          <div class="flex flex-wrap items-center gap-3 w-full lg:w-auto">
            <select class="pl-3 pr-8 py-2.5 bg-surface-container-low border-none rounded-lg text-sm focus:ring-2 focus:ring-primary font-bold text-primary cursor-pointer appearance-none">
              <option value="">所有级别</option>
              <option value="T1">T1 战略级</option>
              <option value="T2">T2 大宗客户</option>
              <option value="T3">T3 标准客户</option>
            </select>
            <select class="pl-3 pr-8 py-2.5 bg-surface-container-low border-none rounded-lg text-sm focus:ring-2 focus:ring-primary font-bold text-primary cursor-pointer appearance-none">
              <option value="">所有类型</option>
              <option value="1">直客 (甲方)</option>
              <option value="2">总包方</option>
              <option value="3">分包方</option>
            </select>
          </div>
        </div>

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
    </div>

    <Teleport defer to="body">
      <transition name="fade">
        <div v-if="isDrawerOpen" @click="closeDrawer" class="fixed inset-0 bg-primary/20 backdrop-blur-sm z-[60]"></div>
      </transition>

      <transition name="slide">
        <div v-if="isDrawerOpen" class="fixed right-0 top-0 h-full w-full sm:w-[550px] bg-white/95 backdrop-blur-2xl z-[70] shadow-[-20px_0_40px_rgba(0,32,69,0.1)] border-l-4 border-primary flex flex-col">

          <div class="p-6 flex items-center justify-between border-b border-outline-variant/20 bg-white">
            <div>
              <h2 class="text-xl font-bold text-primary tracking-tight">新建客户档案</h2>
              <p class="text-xs text-on-surface-variant mt-1">录入客户主数据、联系人及初始项目</p>
            </div>
            <button @click="closeDrawer" class="p-2 hover:bg-surface-container-high rounded-full transition-colors text-on-surface-variant">
              <span class="material-symbols-outlined text-[20px]">close</span>
            </button>
          </div>

          <div class="flex-1 overflow-y-auto p-6 space-y-8">

            <section class="space-y-4">
              <h3 class="text-sm font-bold text-primary flex items-center gap-2">
                <span class="w-1 h-4 bg-primary rounded-full"></span>客户基础信息
              </h3>

              <div>
                <label class="block text-xs font-bold text-on-surface-variant mb-1.5 ml-1">公司名称 <span class="text-error">*</span></label>
                <input v-model="formData.customerName" type="text" class="w-full bg-surface-container-low border-none focus:ring-2 focus:ring-primary rounded-lg py-2.5 px-4 text-sm font-bold text-primary transition-colors" placeholder="输入企业完整名称" />
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-xs font-bold text-on-surface-variant mb-1.5 ml-1">客户级别 <span class="text-error">*</span></label>
                  <select v-model="formData.customerLevel" class="w-full bg-surface-container-low border-none focus:ring-2 focus:ring-primary rounded-lg py-2.5 px-3 text-sm font-bold text-primary cursor-pointer">
                    <option value="T3">T3 标准客户</option>
                    <option value="T2">T2 大宗客户</option>
                    <option value="T1">T1 战略级客户</option>
                  </select>
                </div>
                <div>
                  <label class="block text-xs font-bold text-on-surface-variant mb-1.5 ml-1">客户类型 <span class="text-error">*</span></label>
                  <select v-model="formData.customerType" class="w-full bg-surface-container-low border-none focus:ring-2 focus:ring-primary rounded-lg py-2.5 px-3 text-sm font-bold text-primary cursor-pointer">
                    <option value="1">直客 (甲方)</option>
                    <option value="2">总包方</option>
                    <option value="3">分包方</option>
                  </select>
                </div>
              </div>

              <div>
                <label class="block text-xs font-bold text-on-surface-variant mb-1.5 ml-1">施工区域 <span class="text-error">*</span></label>
                <input v-model="formData.constructionArea" type="text" class="w-full bg-surface-container-low border-none focus:ring-2 focus:ring-primary rounded-lg py-2.5 px-4 text-sm font-bold text-primary transition-colors" placeholder="例如：浙江省杭州市" />
              </div>
            </section>

            <section class="space-y-3">
              <div class="flex justify-between items-end mb-2">
                <h3 class="text-sm font-bold text-primary flex items-center gap-2">
                  <span class="w-1 h-4 bg-primary rounded-full"></span>联系人列表
                </h3>
                <button @click="addContact" class="text-xs font-bold text-primary hover:bg-primary/10 px-2 py-1 rounded transition-colors flex items-center gap-1">
                  <span class="material-symbols-outlined text-[16px]">person_add</span> 添加联系人
                </button>
              </div>

              <div v-for="(contact, index) in formData.contacts" :key="'contact'+index" class="flex items-center gap-3 bg-surface-container-low p-3 rounded-xl border border-outline-variant/10">
                <div class="flex-1 grid grid-cols-2 gap-3">
                  <input v-model="contact.contactName" type="text" class="w-full bg-white border border-outline-variant/20 focus:ring-1 focus:ring-primary rounded py-2 px-3 text-xs font-bold text-primary" placeholder="联系人姓名" />
                  <input v-model="contact.contactPhone" type="text" class="w-full bg-white border border-outline-variant/20 focus:ring-1 focus:ring-primary rounded py-2 px-3 text-xs font-bold text-primary" placeholder="联系电话" />
                </div>
                <button @click="removeContact(index)" class="p-1.5 text-on-surface-variant hover:text-error hover:bg-error-container rounded transition-colors" title="移除">
                  <span class="material-symbols-outlined text-[18px]">delete</span>
                </button>
              </div>

              <div v-if="formData.contacts.length === 0" class="text-center py-4 border border-dashed border-outline-variant/30 rounded-xl text-xs text-on-surface-variant/60 font-medium">
                暂无联系人，请添加
              </div>
            </section>

            <section class="space-y-3">
              <div class="flex justify-between items-end mb-2">
                <h3 class="text-sm font-bold text-tertiary flex items-center gap-2">
                  <span class="w-1 h-4 bg-tertiary rounded-full"></span>合作项目列表
                </h3>
                <button @click="addProject" class="text-xs font-bold text-tertiary hover:bg-tertiary/10 px-2 py-1 rounded transition-colors flex items-center gap-1">
                  <span class="material-symbols-outlined text-[16px]">post_add</span> 添加项目
                </button>
              </div>

              <div v-for="(project, index) in formData.projects" :key="'proj'+index" class="flex items-center gap-3 bg-tertiary-fixed/10 p-3 rounded-xl border border-tertiary/20">
                <div class="flex-1">
                  <input v-model="project.projectName" type="text" class="w-full bg-white border border-outline-variant/20 focus:ring-1 focus:ring-tertiary rounded py-2 px-3 text-xs font-bold text-tertiary" placeholder="输入工程项目名称" />
                </div>
                <button @click="removeProject(index)" class="p-1.5 text-on-surface-variant hover:text-error hover:bg-error-container rounded transition-colors" title="移除">
                  <span class="material-symbols-outlined text-[18px]">close</span>
                </button>
              </div>
            </section>

          </div>

          <div class="p-6 border-t border-outline-variant/20 bg-surface-container-lowest flex items-center justify-end gap-3 shrink-0">
            <button @click="closeDrawer" class="px-5 py-2.5 text-sm font-bold text-secondary hover:bg-surface-container-high rounded-lg transition-colors">
              取消
            </button>
            <button @click="submit" :disabled="!isFormValid" class="px-6 py-2.5 text-sm font-bold bg-primary text-on-primary rounded-lg shadow-md hover:shadow-lg hover:bg-primary/90 transition-all disabled:opacity-50 disabled:cursor-not-allowed">
              保存客户及关联信息
            </button>
          </div>

        </div>
      </transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'

// ==========================================
// 1. 列表页数据与逻辑
// ==========================================

// 模拟从后端获取的 Customer 聚合列表数据
const customerList = ref([
  {
    id: 1,
    customerName: '中建八局第一建设有限公司',
    customerLevel: 'T1',
    customerType: 2, // 2代表总包方
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
    customerType: 3, // 3代表分包方
    constructionArea: '浙江省杭州市',
    contacts: [
      { contactName: '张伟', contactPhone: '150-8888-9999' }
    ],
    projects: [
      { projectName: '绿城春风金沙内装工程' }
    ]
  }
])

// 字典映射方法：客户类型
const getTypeLabel = (type) => {
  const map = { 1: '直客 (甲方)', 2: '总包方', 3: '分包方' }
  return map[type] || '未知类型'
}

// 字典映射方法：客户等级UI主题
const getLevelTheme = (level) => {
  const themes = {
    'T1': { class: 'bg-primary/10 text-primary border-primary/20', label: '战略级' },
    'T2': { class: 'bg-secondary/10 text-secondary border-secondary/20', label: '大宗客户' },
    'T3': { class: 'bg-surface-container-highest text-on-surface-variant border-outline-variant/30', label: '标准客户' }
  }
  return themes[level] || themes['T3']
}


// ==========================================
// 2. 抽屉表单数据与逻辑 (对应 CustomerAddRequest)
// ==========================================

const isDrawerOpen = ref(false)
const openCreateDrawer = () => { isDrawerOpen.value = true }
const closeDrawer = () => {
  isDrawerOpen.value = false
  resetForm()
}

// 严格按照 DTO CustomerAddRequest 设计
const formData = reactive({
  customerName: '',
  customerLevel: 'T3', // 实体中的 level
  customerType: 1,     // 实体中的 type (Integer)
  constructionArea: '',
  contacts: [
    { contactName: '', contactPhone: '' } // 默认给一个空联系人槽位
  ],
  projects: [] // 初始没有项目
})

// 动态增删联系人
const addContact = () => formData.contacts.push({ contactName: '', contactPhone: '' })
const removeContact = (index) => formData.contacts.splice(index, 1)

// 动态增删项目
const addProject = () => formData.projects.push({ projectName: '' })
const removeProject = (index) => formData.projects.splice(index, 1)

// 基础表单校验 (只校验必填的客户主表字段)
const isFormValid = computed(() => {
  return formData.customerName && formData.customerType && formData.constructionArea && formData.customerLevel
})

// 提交数据
const submit = () => {
  if (!isFormValid.value) return

  // 深拷贝组装成标准的 CustomerAddRequest JSON
  const requestPayload = JSON.parse(JSON.stringify(formData))

  // 清理空的联系人和项目，防止脏数据入库
  requestPayload.contacts = requestPayload.contacts.filter(c => c.contactName || c.contactPhone)
  requestPayload.projects = requestPayload.projects.filter(p => p.projectName)

  console.log('发送给后端的 CustomerAddRequest 数据:', requestPayload)

  // TODO: axios.post('/api/customer/add', requestPayload).then(...)
  closeDrawer()
}

const resetForm = () => {
  formData.customerName = ''
  formData.customerLevel = 'T3'
  formData.customerType = 1
  formData.constructionArea = ''
  formData.contacts = [{ contactName: '', contactPhone: '' }]
  formData.projects = []
}
</script>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.slide-enter-active, .slide-leave-active { transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.slide-enter-from, .slide-leave-to { transform: translateX(100%); }
</style>
