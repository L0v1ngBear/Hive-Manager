<template>
  <Teleport defer to="body">
    <transition name="fade">
      <div v-if="visible" @click="closeDrawer" class="fixed inset-0 bg-primary/20 backdrop-blur-sm z-[60]"></div>
    </transition>

    <transition name="slide">
      <div v-if="visible" class="fixed right-0 top-0 h-full w-full sm:w-[550px] bg-white/95 backdrop-blur-2xl z-[70] shadow-[-20px_0_40px_rgba(0,32,69,0.1)] border-l-4 border-primary flex flex-col">

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
</template>

<script setup>
import { reactive, computed } from 'vue'

// 1. 定义 Props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

// 2. 定义 Emits (支持 v-model:visible 双向绑定)
const emit = defineEmits(['update:visible', 'success'])

const closeDrawer = () => {
  emit('update:visible', false)
  resetForm()
}

// 3. 表单数据模型
const formData = reactive({
  customerName: '',
  customerLevel: 'T3',
  customerType: 1,
  constructionArea: '',
  contacts: [{ contactName: '', contactPhone: '' }],
  projects: []
})

// 4. 动态操作方法
const addContact = () => formData.contacts.push({ contactName: '', contactPhone: '' })
const removeContact = (index) => formData.contacts.splice(index, 1)

const addProject = () => formData.projects.push({ projectName: '' })
const removeProject = (index) => formData.projects.splice(index, 1)

const isFormValid = computed(() => {
  return formData.customerName && formData.customerType && formData.constructionArea && formData.customerLevel
})

// 5. 提交逻辑
const submit = () => {
  if (!isFormValid.value) return

  const requestPayload = JSON.parse(JSON.stringify(formData))
  requestPayload.contacts = requestPayload.contacts.filter(c => c.contactName || c.contactPhone)
  requestPayload.projects = requestPayload.projects.filter(p => p.projectName)

  // 抛出成功事件，把数据给父组件处理
  emit('success', requestPayload)
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
