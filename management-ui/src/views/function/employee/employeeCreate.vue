<template>
  <transition
      enter-active-class="transition-opacity duration-300"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-300"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
  >
    <div
        v-if="visible"
        class="fixed inset-0 z-40 bg-primary/10 backdrop-blur-sm"
        @click="emit('close')"
    ></div>
  </transition>

  <el-drawer
      :model-value="visible"
      :with-header="false"
      size="560px"
      @update:model-value="(value) => !value && emit('close')"
  >
    <el-form :model="form" class="flex h-full flex-col" @submit.prevent="submit">
    <div class="border-t-[4px] border-primary p-8">
      <div class="flex items-start justify-between">
        <div>
          <h2 class="text-2xl font-bold tracking-tight text-primary">
            {{ isEditMode ? '编辑员工' : '添加员工' }}
          </h2>
          <p class="mt-1 text-sm text-on-surface-variant">
            {{ isEditMode ? '更新员工信息并重新分配角色。' : '创建员工档案并分配初始组织信息。' }}
          </p>
        </div>
        <button
            type="button"
            class="rounded-full p-2 transition-colors hover:bg-surface-container-high"
            @click="emit('close')"
        >
          <span class="material-symbols-outlined text-on-surface-variant">close</span>
        </button>
      </div>
    </div>

    <div class="flex-1 space-y-10 overflow-y-auto px-8 py-4">
      <section>
        <div class="mb-6 flex items-center gap-2">
          <span class="h-4 w-1 rounded-full bg-primary"></span>
          <h3 class="text-xs font-bold uppercase tracking-widest text-on-primary-container">基本信息</h3>
        </div>
        <div class="grid grid-cols-2 gap-6">
          <div class="col-span-2">
            <label class="ml-1 mb-1 block text-xs font-semibold text-on-surface-variant">
              {{ fieldLabel('name', '姓名') }} <span v-if="fieldRequired('name')" class="text-error">*</span>
            </label>
            <el-input
                v-model.trim="form.name"
                data-field="employee.name"
                class="w-full rounded-sm border-b-2 border-transparent bg-surface-container-low px-3 py-2.5 text-sm outline-none transition-all focus:border-primary"
                placeholder="例如：张三"
                type="text"
            />
          </div>
          <div v-if="fieldVisible('empNo')">
            <label class="ml-1 mb-1 block text-xs font-semibold text-on-surface-variant">{{ fieldLabel('empNo', '工号') }}</label>
            <el-input
                class="w-full cursor-not-allowed rounded-sm border-b-2 border-transparent bg-surface-container-highest/50 px-3 py-2.5 text-sm italic text-on-surface-variant"
                readonly
                type="text"
                value="系统自动生成"
            />
          </div>
          <div>
            <label class="ml-1 mb-1 block text-xs font-semibold text-on-surface-variant">
              {{ fieldLabel('phone', '电话') }} <span v-if="fieldRequired('phone')" class="text-error">*</span>
            </label>
            <el-input
                v-model.trim="form.phone"
                data-field="employee.phone"
                class="w-full rounded-sm border-b-2 border-transparent bg-surface-container-low px-3 py-2.5 text-sm outline-none transition-all focus:border-primary"
                placeholder="例如：138 0000 0000"
                type="tel"
            />
          </div>
          <div v-if="fieldVisible('email')" class="col-span-2">
            <label class="ml-1 mb-1 block text-xs font-semibold text-on-surface-variant">{{ fieldLabel('email', '邮箱') }}</label>
            <el-input
                v-model.trim="form.email"
                class="w-full rounded-sm border-b-2 border-transparent bg-surface-container-low px-3 py-2.5 text-sm outline-none transition-all focus:border-primary"
                placeholder="例如：zhangsan@company.com"
                type="email"
            />
          </div>
        </div>
      </section>

      <section>
        <div class="mb-6 flex items-center gap-2">
          <span class="h-4 w-1 rounded-full bg-primary"></span>
          <h3 class="text-xs font-bold uppercase tracking-widest text-on-primary-container">工作信息</h3>
        </div>
        <div class="grid grid-cols-2 gap-6">
          <div>
            <label class="ml-1 mb-1 block text-xs font-semibold text-on-surface-variant">
              {{ fieldLabel('departmentName', '部门') }} <span v-if="fieldRequired('departmentName')" class="text-error">*</span>
            </label>
            <el-select
                v-model="form.departmentId"
                data-field="employee.departmentId"
                class="w-full appearance-none rounded-sm border-b-2 border-transparent bg-surface-container-low px-3 py-2.5 text-sm outline-none transition-all focus:border-primary"
                placeholder="请选择部门"
            >
              <el-option label="请选择部门" value="" />
              <el-option
                  v-for="department in departments"
                  :key="department.id"
                  :label="department.name"
                  :value="department.id"
              />
            </el-select>
          </div>
          <div>
            <label class="ml-1 mb-1 block text-xs font-semibold text-on-surface-variant">
              {{ fieldLabel('entryDate', '入职日期') }} <span v-if="fieldRequired('entryDate')" class="text-error">*</span>
            </label>
            <el-date-picker
                v-model="form.entryDate"
                data-field="employee.entryDate"
                class="w-full rounded-sm border-b-2 border-transparent bg-surface-container-low px-3 py-2.5 text-sm outline-none transition-all focus:border-primary"
                type="date"
                value-format="YYYY-MM-DD"
            />
          </div>
          <div class="col-span-2">
            <label class="ml-1 mb-1 block text-xs font-semibold text-on-surface-variant">
              {{ fieldLabel('positionName', '职位') }} <span v-if="fieldRequired('positionName')" class="text-error">*</span>
            </label>
            <el-select
                v-model="form.positionId"
                data-field="employee.positionId"
                class="w-full appearance-none rounded-sm border-b-2 border-transparent bg-surface-container-low px-3 py-2.5 text-sm outline-none transition-all focus:border-primary"
                placeholder="请选择职位"
            >
              <el-option label="请选择职位" value="" />
              <el-option
                  v-for="position in filteredPositions"
                  :key="position.id"
                  :label="position.name"
                  :value="position.id"
              />
            </el-select>
          </div>

          <div class="col-span-2">
            <label class="ml-1 mb-2 block text-xs font-semibold text-on-surface-variant">考勤要求</label>
            <el-radio-group v-model="form.attendanceRequired" class="grid grid-cols-2 gap-3">
              <el-radio
                  :value="1"
                  class="!m-0 !h-auto [&_.el-radio__input]:hidden [&_.el-radio__label]:w-full [&_.el-radio__label]:p-0"
              >
                <div
                    class="rounded-xl border p-4 transition-all"
                    :class="Number(form.attendanceRequired ?? 1) === 1
                      ? 'border-primary bg-primary/10 text-primary shadow-sm'
                      : 'border-outline-variant/30 bg-white text-on-surface hover:border-primary/40'"
                >
                  <div class="text-sm font-black">需要打卡</div>
                  <div class="mt-1 text-xs opacity-70">参与打卡、缺勤和异常统计。</div>
                </div>
              </el-radio>
              <el-radio
                  :value="0"
                  class="!m-0 !h-auto [&_.el-radio__input]:hidden [&_.el-radio__label]:w-full [&_.el-radio__label]:p-0"
              >
                <div
                    class="rounded-xl border p-4 transition-all"
                    :class="Number(form.attendanceRequired ?? 1) === 0
                      ? 'border-amber-400 bg-amber-50 text-amber-800 shadow-sm'
                      : 'border-outline-variant/30 bg-white text-on-surface hover:border-amber-300'"
                >
                  <div class="text-sm font-black">免打卡</div>
                  <div class="mt-1 text-xs opacity-70">不参与缺卡、缺勤和考勤日报补录。</div>
                </div>
              </el-radio>
            </el-radio-group>
          </div>

          <div v-if="Number(form.attendanceRequired ?? 1) === 1" class="col-span-2">
            <div class="mb-2 ml-1 flex items-center justify-between">
              <label class="block text-xs font-semibold text-on-surface-variant">允许打卡地点</label>
              <span class="text-[11px] text-on-surface-variant">不选择则默认允许全部启用地点</span>
            </div>
            <div class="rounded-xl border border-outline-variant/20 bg-surface-container-low p-4">
              <div v-if="attendanceLocations.length === 0" class="text-xs text-on-surface-variant">
                暂无可选打卡点，请先到考勤规则中维护打卡地点。
              </div>
              <div v-else class="flex flex-wrap gap-2">
                <button
                    v-for="location in attendanceLocations"
                    :key="location.value"
                    type="button"
                    class="rounded-lg border px-3 py-1.5 text-xs font-bold transition-all"
                    :class="form.attendanceLocationIds.includes(Number(location.value))
                      ? 'border-primary bg-primary text-white shadow-md shadow-primary/20'
                      : 'border-outline-variant/30 bg-white text-on-surface-variant hover:border-primary/50 hover:text-primary'"
                    @click.prevent="toggleAttendanceLocation(location.value)"
                >
                  {{ location.label }}
                </button>
              </div>
            </div>
          </div>

          <div class="col-span-2">
            <div class="mb-2 ml-1 flex items-center justify-between">
              <label class="block text-xs font-semibold text-on-surface-variant">系统角色分配</label>
              <span
                  v-if="form.roleIds.length > 0"
                  class="rounded bg-primary/10 px-2 py-0.5 text-[10px] font-bold text-primary"
              >
                已选 {{ form.roleIds.length }} 项
              </span>
            </div>

            <div class="max-h-[180px] overflow-y-auto rounded-xl border border-outline-variant/20 bg-surface-container-low p-4">
              <div v-if="roles.length === 0" class="py-4 text-center text-xs text-on-surface-variant">
                系统暂无可用角色
              </div>
              <div v-else class="flex flex-wrap gap-2">
                <button
                    v-for="role in roles"
                    :key="role.id"
                    type="button"
                    class="flex items-center gap-1 rounded-lg border px-3 py-1.5 text-xs font-bold transition-all"
                    :class="form.roleIds.includes(role.id)
                    ? 'border-primary bg-primary text-white shadow-md shadow-primary/30'
                    : 'border-outline-variant/30 bg-white text-on-surface-variant hover:border-primary/50 hover:text-primary'"
                    @click.prevent="toggleRole(role.id)"
                >
                  <span
                      v-if="form.roleIds.includes(role.id)"
                      class="material-symbols-outlined text-[14px]"
                  >
                    check
                  </span>
                  {{ role.name }}
                </button>
              </div>
            </div>
          </div>

          <div v-if="fieldVisible('leaderName')" class="relative col-span-2">
            <label class="ml-1 mb-1 block text-xs font-semibold text-on-surface-variant">{{ fieldLabel('leaderName', '直属领导') }}</label>
            <div class="group relative">
              <span class="material-symbols-outlined absolute top-2.5 left-3 text-lg text-on-surface-variant">
                person_search
              </span>
              <input
                  v-model.trim="leaderKeyword"
                  class="w-full rounded-sm border-b-2 border-transparent bg-surface-container-low py-2.5 pr-3 pl-10 text-sm outline-none transition-all focus:border-primary"
                  placeholder="请输入姓名或工号搜索直属领导"
                  type="text"
                  @input="handleLeaderSearch"
              />
            </div>
            <div
                v-if="leaderOptions.length > 0"
                class="absolute z-10 mt-2 w-full overflow-hidden rounded-lg border border-outline-variant/20 bg-white shadow-lg"
            >
              <button
                  v-for="leader in leaderOptions"
                  :key="leader.id"
                  type="button"
                  class="w-full px-3 py-2 text-left transition-colors hover:bg-surface-container-low"
                  @click="selectLeader(leader)"
              >
                <div class="text-sm font-semibold text-primary">{{ leader.name }}</div>
                <div class="text-xs text-on-surface-variant">
                  {{ leader.empNo || '无工号' }} / {{ leader.departmentName || '未分配部门' }} /
                  {{ leader.positionName || '未分配职位' }}
                </div>
              </button>
            </div>
            <p v-if="selectedLeaderLabel" class="mt-2 text-xs text-on-surface-variant">
              当前已选：{{ selectedLeaderLabel }}
            </p>
          </div>

          <div v-if="fieldVisible('remark')" class="col-span-2">
            <label class="ml-1 mb-1 block text-xs font-semibold text-on-surface-variant">{{ fieldLabel('remark', '补充备注') }}</label>
            <textarea
                v-model.trim="form.remark"
                class="w-full resize-none rounded-sm border-b-2 border-transparent bg-surface-container-low px-3 py-2.5 text-sm outline-none transition-all focus:border-primary"
                placeholder="请输入任何需要补充的备注信息（选填）"
                rows="3"
            ></textarea>
          </div>
        </div>
      </section>

      <section>
        <div class="mb-4 flex items-center gap-2">
          <span class="h-4 w-1 rounded-full bg-primary"></span>
          <h3 class="text-xs font-bold uppercase tracking-widest text-on-primary-container">
            {{ fieldLabel('status', '雇佣状态') }} <span v-if="fieldRequired('status')" class="text-error">*</span>
          </h3>
        </div>
        <el-radio-group v-model="form.status" class="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <el-radio
              v-for="status in employmentStatuses"
              :key="status.value"
              :value="Number(status.value)"
              class="!m-0 !h-auto [&_.el-radio__input]:hidden [&_.el-radio__label]:w-full [&_.el-radio__label]:p-0"
          >
            <div
                class="h-full rounded-xl border p-4 shadow-sm transition-all"
                :class="Number(form.status) === Number(status.value)
                  ? 'border-primary bg-primary text-white shadow-lg shadow-primary/25'
                  : 'border-outline-variant/30 bg-white text-on-surface hover:border-primary/40'"
            >
              <div class="mb-1 flex items-center justify-between">
                <span class="text-sm font-bold">{{ status.label }}</span>
                <span
                    class="material-symbols-outlined text-lg text-current"
                    :class="Number(form.status) === Number(status.value) ? 'opacity-100' : 'opacity-40'"
                    style="font-variation-settings: 'FILL' 1;"
                >
                  check_circle
                </span>
              </div>
              <p class="text-[10px] text-current opacity-70">保存后将同步至系统主数据。</p>
            </div>
          </el-radio>
        </el-radio-group>
      </section>
    </div>

    <div class="flex items-center justify-between border-t border-outline-variant/15 bg-surface-container-lowest p-8">
      <button
          type="button"
          class="px-6 py-2.5 text-sm font-bold text-on-surface-variant transition-colors hover:text-primary"
          @click="emit('close')"
      >
        取消
      </button>
      <button
          type="submit"
          :disabled="submitting"
          class="flex items-center gap-2 rounded bg-primary px-10 py-3 font-bold text-white shadow-xl shadow-primary/20 transition-all active:scale-95 hover:bg-primary-container disabled:opacity-60"
      >
        <span class="material-symbols-outlined text-lg" style="font-variation-settings: 'FILL' 1;">
          save
        </span>
        {{ submitting ? '正在保存...' : isEditMode ? '确认更新' : '确认添加' }}
      </button>
    </div>
    </el-form>
  </el-drawer>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElDatePicker, ElDrawer, ElForm, ElInput, ElMessage, ElOption, ElRadio, ElRadioGroup, ElSelect } from 'element-plus'
import { useTenantFieldConfig } from '@/composables/useTenantFieldConfig'
import { warnAndFocusField } from '@/utils/formFocus'
import {
  createEmployee,
  getEmployeeDetail,
  getEmployeeFormOptions,
  searchEmployeeLeaders,
  updateEmployee
} from './api/employee.js'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  employeeId: {
    type: [Number, String],
    default: null
  }
})

const emit = defineEmits(['close', 'success'])

const {
  loadFieldConfig,
  fieldLabel,
  fieldRequired,
  fieldVisible
} = useTenantFieldConfig('employee', {
  backendRequiredFields: ['name', 'phone', 'departmentName', 'positionName', 'entryDate', 'status'],
  fallbackKey: 'name'
})
const submitting = ref(false)
const initialized = ref(false)
const departments = ref([])
const positions = ref([])
const roles = ref([])
const attendanceLocations = ref([])
const leaderOptions = ref([])
const leaderKeyword = ref('')
const selectedLeaderLabel = ref('')
let leaderSearchTimer = null

// 雇佣状态与后端员工状态枚举值保持一致。
const employmentStatuses = ref([
  { label: '正式在职', value: 1 },
  { label: '试用期', value: 2 },
  { label: '已离职', value: 0 }
])

const form = reactive(createDefaultForm())

const filteredPositions = computed(() => {
  if (!form.departmentId) {
    return positions.value
  }
  return positions.value.filter((item) => !item.departmentId || Number(item.departmentId) === Number(form.departmentId))
})

const isEditMode = computed(() => props.employeeId !== null && props.employeeId !== undefined && props.employeeId !== '')

watch(
    () => props.visible,
    async (visible) => {
      if (!visible) {
        return
      }
      resetForm()
      await loadFieldConfig()
      if (!initialized.value) {
        await loadOptions()
        initialized.value = true
      }
      if (isEditMode.value) {
        await loadEmployeeDetail()
      }
    }
)

watch(
    () => form.departmentId,
    () => {
      if (!filteredPositions.value.find((item) => Number(item.id) === Number(form.positionId))) {
        form.positionId = ''
      }
    }
)

onBeforeUnmount(() => {
  if (leaderSearchTimer) {
    clearTimeout(leaderSearchTimer)
  }
})

function toggleRole(roleId) {
  const index = form.roleIds.indexOf(roleId)
  if (index === -1) {
    form.roleIds.push(roleId)
    return
  }
  form.roleIds.splice(index, 1)
}

function toggleAttendanceLocation(locationId) {
  const id = Number(locationId)
  if (!Number.isFinite(id) || id <= 0) {
    return
  }
  const index = form.attendanceLocationIds.indexOf(id)
  if (index === -1) {
    form.attendanceLocationIds.push(id)
    return
  }
  form.attendanceLocationIds.splice(index, 1)
}

async function loadOptions() {
  const data = await getEmployeeFormOptions()
  departments.value = data.departments || []
  positions.value = data.positions || []
  roles.value = data.roles || []
  attendanceLocations.value = data.attendanceLocations || []

  // 强制防呆：只有当后端明确返回了包含中文字符的 label 时，才使用后端的数据
  if (data.employmentStatuses?.length > 0 && /[\u4e00-\u9fa5]/.test(data.employmentStatuses[0].label)) {
    employmentStatuses.value = data.employmentStatuses
  }
}

function resetForm() {
  Object.assign(form, createDefaultForm())
  leaderOptions.value = []
  leaderKeyword.value = ''
  selectedLeaderLabel.value = ''
}

function createDefaultForm() {
  return {
    name: '',
    phone: '',
    email: '',
    departmentId: '',
    positionId: '',
    leaderName: '',
    entryDate: new Date().toISOString().slice(0, 10),
    status: 1,
    attendanceRequired: 1,
    remark: '',
    attendanceLocationIds: [],
    roleIds: []
  }
}

async function loadEmployeeDetail() {
  const detail = await getEmployeeDetail(props.employeeId)
  form.name = detail.name || ''
  form.phone = detail.phone || ''
  form.email = detail.email || ''
  form.departmentId = detail.departmentId || ''
  form.positionId = detail.positionId || ''
  form.leaderName = detail.leaderName || ''
  form.entryDate = detail.entryDate || new Date().toISOString().slice(0, 10)
  form.status = Number(detail.status ?? 1)
  form.attendanceRequired = Number(detail.attendanceRequired ?? 1)
  form.remark = detail.remark || ''
  form.attendanceLocationIds = Array.isArray(detail.attendanceLocationIds) ? detail.attendanceLocationIds.map((id) => Number(id)) : []
  form.roleIds = Array.isArray(detail.roleIds) ? detail.roleIds.map((id) => Number(id)) : []

  if (detail.leaderName) {
    leaderKeyword.value = detail.leaderName
    selectedLeaderLabel.value = detail.leaderName
  }
}

function handleLeaderSearch() {
  if (leaderSearchTimer) {
    clearTimeout(leaderSearchTimer)
  }
  if (!leaderKeyword.value) {
    form.leaderName = ''
    leaderOptions.value = []
    selectedLeaderLabel.value = ''
    return
  }
  leaderSearchTimer = setTimeout(async () => {
    leaderOptions.value = await searchEmployeeLeaders({ keyword: leaderKeyword.value, limit: 8 })
  }, 250)
}

function selectLeader(leader) {
  form.leaderName = leader.name
  leaderKeyword.value = leader.name
  selectedLeaderLabel.value = `${leader.name} / ${leader.empNo || '--'}`
  leaderOptions.value = []
}

async function submit() {
  // 移除了 form.employeeType 的校验
  if (!form.name) return warnAndFocusField('请填写姓名。', 'employee.name')
  if (!form.phone) return warnAndFocusField('请填写电话。', 'employee.phone')
  if (!form.departmentId) return warnAndFocusField('请选择部门。', 'employee.departmentId')
  if (!form.positionId) return warnAndFocusField('请选择职位。', 'employee.positionId')
  if (!form.entryDate) return warnAndFocusField('请选择入职日期。', 'employee.entryDate')

  submitting.value = true
  try {
    const payload = {
      ...form,
      departmentId: Number(form.departmentId),
      positionId: Number(form.positionId),
      leaderName: form.leaderName || null,
      status: Number(form.status),
      attendanceRequired: Number(form.attendanceRequired ?? 1),
      attendanceLocationIds: Number(form.attendanceRequired ?? 1) === 1 ? (form.attendanceLocationIds || []).map((id) => Number(id)) : [],
      roleIds: (form.roleIds || []).map((id) => Number(id))
    }

    if (isEditMode.value) {
      await updateEmployee({
        ...payload,
        id: Number(props.employeeId)
      })
      ElMessage.success('员工基本信息更新成功。')
    } else {
      await createEmployee(payload)
      ElMessage.success('员工档案创建成功。')
    }
    emit('success')
    emit('close')
  } finally {
    submitting.value = false
  }
}
</script>
