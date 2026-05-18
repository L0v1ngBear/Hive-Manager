<template>
  <Teleport defer to="body">
    <transition name="fade">
      <div
        v-if="visible"
        class="fixed inset-0 z-[60] bg-primary/20 backdrop-blur-sm"
        @click="closeDrawer"
      ></div>
    </transition>

    <transition name="slide">
      <div
        v-if="visible"
        class="fixed top-0 right-0 z-[70] flex h-full w-full flex-col border-l-4 border-primary bg-white/95 shadow-[-20px_0_40px_rgba(0,32,69,0.1)] backdrop-blur-2xl sm:w-[550px]"
      >
        <div class="flex items-center justify-between border-b border-outline-variant/20 bg-white p-6">
          <div>
            <h2 class="text-xl font-bold tracking-tight text-primary">{{ isEditMode ? '编辑客户档案' : '新建客户档案' }}</h2>
            <p class="mt-1 text-xs text-on-surface-variant">
              {{ isEditMode ? '更新客户基础信息、联系人和合作项目。' : '录入客户基础信息、联系人和合作项目。' }}
            </p>
          </div>
          <button
            class="rounded-full p-2 text-on-surface-variant transition-colors hover:bg-surface-container-high"
            @click="closeDrawer"
          >
            <span class="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>

        <div class="flex-1 space-y-8 overflow-y-auto p-6">
          <section class="space-y-4">
            <h3 class="flex items-center gap-2 text-sm font-bold text-primary">
              <span class="h-4 w-1 rounded-full bg-primary"></span>客户基础信息
            </h3>

            <div>
              <label class="ml-1 mb-1.5 block text-xs font-bold text-on-surface-variant">
                {{ fieldLabel('customerName', '客户名称') }} <span v-if="fieldRequired('customerName')" class="text-error">*</span>
              </label>
              <input
                v-model="formData.customerName"
                data-field="customer.customerName"
                class="w-full rounded-lg bg-surface-container-low px-4 py-2.5 text-sm font-bold text-primary transition-colors focus:ring-2 focus:ring-primary"
                placeholder="输入企业完整名称"
                type="text"
              />
            </div>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label class="ml-1 mb-1.5 block text-xs font-bold text-on-surface-variant">
                  {{ fieldLabel('customerType', '客户类型') }} <span v-if="fieldRequired('customerType')" class="text-error">*</span>
                </label>
                <select
                  v-model="formData.customerType"
                  data-field="customer.customerType"
                  class="w-full cursor-pointer rounded-lg bg-surface-container-low px-3 py-2.5 text-sm font-bold text-primary focus:ring-2 focus:ring-primary"
                >
                  <option :value="1">直客（甲方）</option>
                  <option :value="2">总包方</option>
                  <option :value="3">分包方</option>
                </select>
              </div>
            </div>
          </section>

          <section v-if="fieldVisible('contactName') || fieldVisible('contactPhone')" class="space-y-3">
            <div class="mb-2 flex items-end justify-between">
              <h3 class="flex items-center gap-2 text-sm font-bold text-primary">
                <span class="h-4 w-1 rounded-full bg-primary"></span>{{ fieldLabel('contactName', '联系人列表') }}
              </h3>
              <button
                class="flex items-center gap-1 rounded px-2 py-1 text-xs font-bold text-primary transition-colors hover:bg-primary/10"
                @click="addContact"
              >
                <span class="material-symbols-outlined text-[16px]">person_add</span> 添加联系人
              </button>
            </div>

            <div
              v-for="(contact, index) in formData.contacts"
              :key="'contact' + index"
              class="flex items-center gap-3 rounded-xl border border-outline-variant/10 bg-surface-container-low p-3"
            >
              <div class="grid flex-1 grid-cols-2 gap-3">
                <input
                  v-if="fieldVisible('contactName')"
                  v-model="contact.contactName"
                  class="w-full rounded border border-outline-variant/20 bg-white px-3 py-2 text-xs font-bold text-primary focus:ring-1 focus:ring-primary"
                  :placeholder="fieldLabel('contactName', '联系人姓名')"
                  type="text"
                />
                <input
                  v-if="fieldVisible('contactPhone')"
                  v-model="contact.contactPhone"
                  class="w-full rounded border border-outline-variant/20 bg-white px-3 py-2 text-xs font-bold text-primary focus:ring-1 focus:ring-primary"
                  :placeholder="fieldLabel('contactPhone', '联系电话')"
                  type="text"
                />
              </div>
              <button
                class="rounded p-1.5 text-on-surface-variant transition-colors hover:bg-error-container hover:text-error"
                title="移除"
                @click="removeContact(index)"
              >
                <span class="material-symbols-outlined text-[18px]">delete</span>
              </button>
            </div>

            <div
              v-if="formData.contacts.length === 0"
              class="rounded-xl border border-dashed border-outline-variant/30 py-4 text-center text-xs font-medium text-on-surface-variant/60"
            >
              暂无联系人，请添加
            </div>
          </section>

          <section v-if="fieldVisible('projectName')" class="space-y-3">
            <div class="mb-2 flex items-end justify-between">
              <h3 class="flex items-center gap-2 text-sm font-bold text-tertiary">
                <span class="h-4 w-1 rounded-full bg-tertiary"></span>{{ fieldLabel('projectName', '合作项目列表') }}
              </h3>
              <button
                class="flex items-center gap-1 rounded px-2 py-1 text-xs font-bold text-tertiary transition-colors hover:bg-tertiary/10"
                @click="addProject"
              >
                <span class="material-symbols-outlined text-[16px]">post_add</span> 添加项目
              </button>
            </div>

            <div
              v-for="(project, index) in formData.projects"
              :key="'proj' + index"
              class="space-y-3 rounded-xl border border-tertiary/20 bg-tertiary-fixed/10 p-3"
            >
              <div class="flex items-start gap-3">
                <div class="grid flex-1 grid-cols-1 gap-3 sm:grid-cols-2">
                  <input
                    v-if="fieldVisible('projectName')"
                    v-model="project.projectName"
                    class="w-full rounded border border-outline-variant/20 bg-white px-3 py-2 text-xs font-bold text-tertiary focus:ring-1 focus:ring-tertiary"
                    :placeholder="fieldLabel('projectName', '输入合作项目名称')"
                    type="text"
                  />
                  <input
                    v-if="fieldVisible('constructionArea')"
                    v-model="project.constructionArea"
                    class="w-full rounded border border-outline-variant/20 bg-white px-3 py-2 text-xs font-bold text-tertiary focus:ring-1 focus:ring-tertiary"
                    :placeholder="fieldLabel('constructionArea', '输入该项目的施工区域')"
                    type="text"
                  />
                  <input
                    v-if="fieldVisible('projectOwner')"
                    v-model="project.projectOwner"
                    class="w-full rounded border border-outline-variant/20 bg-white px-3 py-2 text-xs font-bold text-tertiary focus:ring-1 focus:ring-tertiary"
                    :placeholder="fieldLabel('projectOwner', '输入项目负责人')"
                    type="text"
                  />
                </div>
                <button
                  class="rounded p-1.5 text-on-surface-variant transition-colors hover:bg-error-container hover:text-error"
                  title="移除"
                  @click="removeProject(index)"
                >
                  <span class="material-symbols-outlined text-[18px]">close</span>
                </button>
              </div>
            </div>

            <div
              v-if="formData.projects.length === 0"
              class="rounded-xl border border-dashed border-outline-variant/30 py-4 text-center text-xs font-medium text-on-surface-variant/60"
            >
              暂无项目，请添加
            </div>
          </section>
        </div>

        <div class="flex shrink-0 items-center justify-end gap-3 border-t border-outline-variant/20 bg-surface-container-lowest p-6">
          <button
            class="rounded-lg px-5 py-2.5 text-sm font-bold text-secondary transition-colors hover:bg-surface-container-high"
            @click="closeDrawer"
          >
            取消
          </button>
          <button
            :disabled="submitting || loadingDetail"
            class="rounded-lg bg-primary px-6 py-2.5 text-sm font-bold text-on-primary shadow-md transition-all hover:bg-primary/90 hover:shadow-lg disabled:cursor-not-allowed disabled:opacity-50"
            @click="submit"
          >
            {{ submitButtonText }}
          </button>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useTenantFieldConfig } from '@/composables/useTenantFieldConfig'
import { warnAndFocusField } from '@/utils/formFocus'
import { createCustomer, getCustomerDetail, updateCustomer } from './api/customer'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  customerId: {
    type: [Number, String],
    default: null
  }
})

const emit = defineEmits(['update:visible', 'success'])
const {
  loadFieldConfig,
  fieldLabel,
  fieldRequired,
  fieldVisible
} = useTenantFieldConfig('customer', {
  backendRequiredFields: ['customerName', 'customerType'],
  fallbackKey: 'customerName'
})
const submitting = ref(false)
const loadingDetail = ref(false)
const formData = reactive(createDefaultForm())

const isEditMode = computed(() => props.customerId !== null && props.customerId !== undefined && props.customerId !== '')
const isFormValid = computed(() => formData.customerName.trim() && formData.customerType)
const submitButtonText = computed(() => {
  if (loadingDetail.value) {
    return '加载中...'
  }
  if (submitting.value) {
    return isEditMode.value ? '保存中...' : '创建中...'
  }
  return isEditMode.value ? '保存修改' : '保存客户及关联信息'
})

watch(
  () => props.visible,
  async (visible) => {
    if (!visible) {
      return
    }
    resetForm()
    await loadFieldConfig()
    if (isEditMode.value) {
      await loadCustomerDetail()
    }
  }
)

const closeDrawer = () => {
  if (submitting.value || loadingDetail.value) {
    return
  }
  emit('update:visible', false)
  resetForm()
}

const addContact = () => formData.contacts.push({ contactName: '', contactPhone: '' })
const removeContact = (index) => formData.contacts.splice(index, 1)
const addProject = () => formData.projects.push({ projectName: '', constructionArea: '', projectOwner: '' })
const removeProject = (index) => formData.projects.splice(index, 1)

async function loadCustomerDetail() {
  loadingDetail.value = true
  try {
    const detail = await getCustomerDetail(props.customerId)
    formData.customerName = detail?.customerName || ''
    formData.customerType = Number(detail?.customerType || 1)
    formData.contacts = Array.isArray(detail?.contacts) && detail.contacts.length > 0
      ? detail.contacts.map((item) => ({
        contactName: item.contactName || '',
        contactPhone: item.contactPhone || ''
      }))
      : [{ contactName: '', contactPhone: '' }]
    formData.projects = Array.isArray(detail?.projects)
      ? detail.projects.map((item) => ({
        projectName: item.projectName || '',
        constructionArea: item.constructionArea || '',
        projectOwner: item.projectOwner || ''
      }))
      : []
  } finally {
    loadingDetail.value = false
  }
}

async function submit() {
  if (submitting.value || loadingDetail.value) {
    return
  }
  if (!formData.customerName.trim()) return warnAndFocusField('请填写客户名称。', 'customer.customerName')
  if (!formData.customerType) return warnAndFocusField('请选择客户类型。', 'customer.customerType')

  const requestPayload = {
    customerName: formData.customerName.trim(),
    customerType: Number(formData.customerType),
    contacts: formData.contacts
      .map((item) => ({
        contactName: item.contactName?.trim() || '',
        contactPhone: item.contactPhone?.trim() || ''
      }))
      .filter((item) => item.contactName || item.contactPhone),
    projects: formData.projects
      .map((item) => ({
        projectName: item.projectName?.trim() || '',
        constructionArea: item.constructionArea?.trim() || '',
        projectOwner: item.projectOwner?.trim() || ''
      }))
      .filter((item) => item.projectName)
  }

  submitting.value = true
  try {
    if (isEditMode.value) {
      await updateCustomer({
        ...requestPayload,
        id: Number(props.customerId)
      })
      ElMessage.success('客户信息已更新')
    } else {
      await createCustomer(requestPayload)
      ElMessage.success('客户保存成功')
    }
    emit('success', requestPayload)
    emit('update:visible', false)
    resetForm()
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  Object.assign(formData, createDefaultForm())
}

function createDefaultForm() {
  return {
    customerName: '',
    customerType: 1,
    contacts: [{ contactName: '', contactPhone: '' }],
    projects: []
  }
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-enter-active,
.slide-leave-active {
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
}
</style>
