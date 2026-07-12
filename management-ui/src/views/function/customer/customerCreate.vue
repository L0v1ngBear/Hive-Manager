<template>
  <el-drawer v-model="drawerVisible" size="550px" direction="rtl" :with-header="false" append-to-body destroy-on-close>
    <div class="flex h-full flex-col bg-white/95 backdrop-blur-2xl">
      <div class="flex items-center justify-between border-b border-outline-variant/20 bg-white p-6">
        <div>
          <h2 class="text-xl font-bold tracking-tight text-primary">{{ isEditMode ? '编辑客户档案' : '新建客户档案' }}</h2>
          <p class="mt-1 text-xs text-on-surface-variant">
            {{ isEditMode ? '更新客户基础信息、联系人和合作项目。' : '录入客户基础信息、联系人和合作项目。' }}
          </p>
        </div>
        <el-button circle text native-type="button" title="关闭" @click="closeDrawer">
          <span class="material-symbols-outlined text-[20px]">close</span>
        </el-button>
      </div>

      <el-form :model="formData" class="flex-1 space-y-8 overflow-y-auto p-6" label-position="top" @submit.prevent="submit">
        <section class="space-y-4">
          <h3 class="flex items-center gap-2 text-sm font-bold text-primary">
            <span class="h-4 w-1 rounded-full bg-primary"></span>客户基础信息
          </h3>
          <el-form-item :label="fieldLabel('customerName', '客户名称')" :required="fieldRequired('customerName')">
            <el-input
              v-model="formData.customerName"
              data-field="customer.customerName"
              placeholder="输入企业完整名称"
            />
          </el-form-item>
          <el-form-item :label="fieldLabel('customerType', '客户类型')" :required="fieldRequired('customerType')">
            <el-select v-model="formData.customerType" data-field="customer.customerType" class="w-full">
              <el-option label="直客（甲方）" :value="1" />
              <el-option label="总包方" :value="2" />
              <el-option label="分包方" :value="3" />
            </el-select>
          </el-form-item>
        </section>

        <section v-if="fieldVisible('contactName') || fieldVisible('contactPhone')" class="space-y-3">
          <div class="mb-2 flex items-end justify-between">
            <h3 class="flex items-center gap-2 text-sm font-bold text-primary">
              <span class="h-4 w-1 rounded-full bg-primary"></span>{{ fieldLabel('contactName', '联系人列表') }}
            </h3>
            <el-button text type="primary" native-type="button" @click="addContact">
              <span class="material-symbols-outlined text-[16px]">person_add</span>
              添加联系人
            </el-button>
          </div>

          <div v-for="(contact, index) in formData.contacts" :key="`contact${index}`" class="flex items-center gap-3 rounded-xl border border-outline-variant/10 bg-surface-container-low p-3">
            <div class="grid flex-1 grid-cols-1 gap-3 sm:grid-cols-2">
              <el-input
                v-if="fieldVisible('contactName')"
                v-model="contact.contactName"
                :placeholder="fieldLabel('contactName', '联系人姓名')"
              />
              <el-input
                v-if="fieldVisible('contactPhone')"
                v-model="contact.contactPhone"
                :placeholder="fieldLabel('contactPhone', '联系电话')"
              />
            </div>
            <el-button circle text type="danger" native-type="button" title="移除联系人" @click="removeContact(index)">
              <span class="material-symbols-outlined text-[18px]">delete</span>
            </el-button>
          </div>

          <div v-if="formData.contacts.length === 0" class="rounded-xl border border-dashed border-outline-variant/30 py-4 text-center text-xs font-medium text-on-surface-variant/60">
            暂无联系人，请添加
          </div>
        </section>

        <section v-if="fieldVisible('projectName')" class="space-y-3">
          <div class="mb-2 flex items-end justify-between">
            <h3 class="flex items-center gap-2 text-sm font-bold text-tertiary">
              <span class="h-4 w-1 rounded-full bg-tertiary"></span>{{ fieldLabel('projectName', '合作项目列表') }}
            </h3>
            <el-button text type="primary" native-type="button" @click="addProject">
              <span class="material-symbols-outlined text-[16px]">post_add</span>
              添加项目
            </el-button>
          </div>

          <div v-for="(project, index) in formData.projects" :key="`project${index}`" class="space-y-3 rounded-xl border border-tertiary/20 bg-tertiary-fixed/10 p-3">
            <div class="flex items-start gap-3">
              <div class="grid flex-1 grid-cols-1 gap-3 sm:grid-cols-2">
                <el-input
                  v-if="fieldVisible('projectName')"
                  v-model="project.projectName"
                  :placeholder="fieldLabel('projectName', '输入合作项目名称')"
                />
                <el-input
                  v-if="fieldVisible('constructionArea')"
                  v-model="project.constructionArea"
                  :placeholder="fieldLabel('constructionArea', '输入该项目的施工区域')"
                />
                <el-input
                  v-if="fieldVisible('projectOwner')"
                  v-model="project.projectOwner"
                  :placeholder="fieldLabel('projectOwner', '输入项目负责人')"
                />
              </div>
              <el-button circle text type="danger" native-type="button" title="移除项目" @click="removeProject(index)">
                <span class="material-symbols-outlined text-[18px]">close</span>
              </el-button>
            </div>
          </div>

          <div v-if="formData.projects.length === 0" class="rounded-xl border border-dashed border-outline-variant/30 py-4 text-center text-xs font-medium text-on-surface-variant/60">
            暂无项目，请添加
          </div>
        </section>
      </el-form>

      <div class="flex shrink-0 items-center justify-end gap-3 border-t border-outline-variant/20 bg-surface-container-lowest p-6">
        <el-button native-type="button" :disabled="submitting || loadingDetail" @click="closeDrawer">取消</el-button>
        <el-button
          type="primary"
          native-type="button"
          :loading="submitting || loadingDetail"
          :disabled="submitting || loadingDetail || !canSubmitCustomer"
          :class="permissionDisabledClass(!canSubmitCustomer)"
          :title="canSubmitCustomer ? submitButtonText : submitPermissionReason"
          @click="submit"
        >
          {{ submitButtonText }}
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElButton, ElDrawer, ElForm, ElFormItem, ElInput, ElMessage, ElOption, ElSelect } from 'element-plus'
import { useTenantFieldConfig } from '@/composables/useTenantFieldConfig'
import { useUserStore } from '@/stores/user'
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
const userStore = useUserStore()
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

const drawerVisible = computed({
  get: () => props.visible,
  set: (visible) => {
    if (!visible) {
      closeDrawer()
      return
    }
    emit('update:visible', true)
  }
})
const isEditMode = computed(() => props.customerId !== null && props.customerId !== undefined && props.customerId !== '')
const canSubmitCustomer = computed(() => userStore.hasPermission(isEditMode.value ? 'customer:update' : 'customer:add'))
const submitPermissionReason = computed(() => isEditMode.value ? '当前账号暂无编辑客户权限' : '当前账号暂无新增客户权限')
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
  if (!canSubmitCustomer.value) return
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

function permissionDisabledClass(disabled) {
  return disabled ? 'cursor-not-allowed opacity-50 grayscale' : ''
}
</script>
