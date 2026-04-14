<template>
  <transition
      enter-active-class="transition-opacity duration-300"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-300"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
  >
    <div v-if="visible" @click="emit('close')" class="fixed inset-0 bg-primary/10 backdrop-blur-sm z-40"></div>
  </transition>

  <div
      :class="visible ? 'translate-x-0' : 'translate-x-full'"
      class="fixed top-0 right-0 h-full w-full max-w-xl bg-surface-container-lowest/95 backdrop-blur-3xl shadow-2xl z-50 flex flex-col border-l border-outline-variant/30 transition-transform duration-300 ease-in-out"
  >
    <div class="p-8 border-t-[4px] border-primary">
        <div class="flex justify-between items-start">
        <div>
          <h2 class="text-2xl font-bold text-primary tracking-tight">{{ isEditMode ? '编辑员工' : '添加员工' }}</h2>
          <p class="text-on-surface-variant text-sm mt-1">{{ isEditMode ? '更新员工信息并重新分配角色。' : '创建员工记录并分配初始组织架构信息。' }}</p>
        </div>
        <button @click="emit('close')" class="p-2 hover:bg-surface-container-high rounded-full transition-colors">
          <span class="material-symbols-outlined text-on-surface-variant">close</span>
        </button>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto px-8 py-4 space-y-10">
      <section>
        <div class="flex items-center gap-2 mb-6">
          <span class="w-1 h-4 bg-primary rounded-full"></span>
          <h3 class="text-xs font-bold uppercase tracking-widest text-on-primary-container">基本信息</h3>
        </div>
        <div class="grid grid-cols-2 gap-6">
          <div class="col-span-2">
            <label class="block text-xs font-semibold text-on-surface-variant mb-1 ml-1">姓名</label>
            <input v-model.trim="form.name" class="w-full bg-surface-container-low border-b-2 border-transparent focus:border-primary px-3 py-2.5 text-sm transition-all outline-none rounded-sm" placeholder="例如：张三" type="text" />
          </div>
          <div>
            <label class="block text-xs font-semibold text-on-surface-variant mb-1 ml-1">工号</label>
            <input class="w-full bg-surface-container-highest/50 border-b-2 border-transparent px-3 py-2.5 text-sm cursor-not-allowed italic text-on-surface-variant rounded-sm" readonly type="text" value="由后端生成" />
          </div>
          <div>
            <label class="block text-xs font-semibold text-on-surface-variant mb-1 ml-1">电话</label>
            <input v-model.trim="form.phone" class="w-full bg-surface-container-low border-b-2 border-transparent focus:border-primary px-3 py-2.5 text-sm transition-all outline-none rounded-sm" placeholder="+86 138 0000 0000" type="tel" />
          </div>
          <div class="col-span-2">
            <label class="block text-xs font-semibold text-on-surface-variant mb-1 ml-1">邮箱</label>
            <input v-model.trim="form.email" class="w-full bg-surface-container-low border-b-2 border-transparent focus:border-primary px-3 py-2.5 text-sm transition-all outline-none rounded-sm" placeholder="name@company.com" type="email" />
          </div>
        </div>
      </section>

      <section>
        <div class="flex items-center gap-2 mb-6">
          <span class="w-1 h-4 bg-primary rounded-full"></span>
          <h3 class="text-xs font-bold uppercase tracking-widest text-on-primary-container">工作信息</h3>
        </div>
        <div class="grid grid-cols-2 gap-6">
          <div>
            <label class="block text-xs font-semibold text-on-surface-variant mb-1 ml-1">部门</label>
            <select v-model="form.departmentId" class="w-full bg-surface-container-low border-b-2 border-transparent focus:border-primary px-3 py-2.5 text-sm transition-all outline-none rounded-sm appearance-none">
              <option value="">选择部门</option>
              <option v-for="department in departments" :key="department.id" :value="department.id">{{ department.name }}</option>
            </select>
          </div>
          <div>
            <label class="block text-xs font-semibold text-on-surface-variant mb-1 ml-1">入职日期</label>
            <input v-model="form.entryDate" class="w-full bg-surface-container-low border-b-2 border-transparent focus:border-primary px-3 py-2.5 text-sm transition-all outline-none rounded-sm" type="date" />
          </div>
          <div>
            <label class="block text-xs font-semibold text-on-surface-variant mb-1 ml-1">员工类型</label>
            <select v-model="form.employeeType" class="w-full bg-surface-container-low border-b-2 border-transparent focus:border-primary px-3 py-2.5 text-sm transition-all outline-none rounded-sm appearance-none">
              <option value="">选择类型</option>
              <option v-for="type in employeeTypes" :key="type.value" :value="type.value">{{ type.label }}</option>
            </select>
          </div>
          <div>
            <label class="block text-xs font-semibold text-on-surface-variant mb-1 ml-1">职位</label>
            <select v-model="form.positionId" class="w-full bg-surface-container-low border-b-2 border-transparent focus:border-primary px-3 py-2.5 text-sm transition-all outline-none rounded-sm appearance-none">
              <option value="">选择职位</option>
              <option v-for="position in filteredPositions" :key="position.id" :value="position.id">{{ position.name }}</option>
            </select>
          </div>
          <div class="col-span-2">
            <label class="block text-xs font-semibold text-on-surface-variant mb-1 ml-1">角色</label>
            <select
              v-model="form.roleIds"
              multiple
              class="w-full min-h-[120px] bg-surface-container-low border-b-2 border-transparent focus:border-primary px-3 py-2.5 text-sm transition-all outline-none rounded-sm"
            >
              <option v-for="role in roles" :key="role.id" :value="role.id">{{ role.name }}</option>
            </select>
            <p class="text-[11px] text-on-surface-variant mt-2">可按住 Ctrl 或 Command 进行多选。</p>
          </div>

          <div class="col-span-2 relative">
            <label class="block text-xs font-semibold text-on-surface-variant mb-1 ml-1">直属领导</label>
            <div class="relative group">
              <span class="absolute left-3 top-2.5 material-symbols-outlined text-lg text-on-surface-variant">person_search</span>
              <input
                  v-model.trim="leaderKeyword"
                  @input="handleLeaderSearch"
                  class="w-full bg-surface-container-low border-b-2 border-transparent focus:border-primary pl-10 pr-3 py-2.5 text-sm transition-all outline-none rounded-sm"
                  placeholder="按姓名或工号搜索领导"
                  type="text"
              />
            </div>
            <div v-if="leaderOptions.length > 0" class="mt-2 border border-outline-variant/20 rounded-lg overflow-hidden bg-white">
              <button
                  v-for="leader in leaderOptions"
                  :key="leader.id"
                  @click="selectLeader(leader)"
                  class="w-full text-left px-3 py-2 hover:bg-surface-container-low transition-colors"
              >
                <div class="text-sm font-semibold text-primary">{{ leader.name }}</div>
                <div class="text-xs text-on-surface-variant">{{ leader.empNo || '--' }} / {{ leader.departmentName || '--' }} / {{ leader.positionName || '--' }}</div>
              </button>
            </div>
            <p v-if="selectedLeaderLabel" class="text-xs text-on-surface-variant mt-2">已选领导：{{ selectedLeaderLabel }}</p>
          </div>

          <div class="col-span-2">
            <label class="block text-xs font-semibold text-on-surface-variant mb-1 ml-1">备注</label>
            <textarea v-model.trim="form.remark" rows="3" class="w-full bg-surface-container-low border-b-2 border-transparent focus:border-primary px-3 py-2.5 text-sm transition-all outline-none rounded-sm resize-none" placeholder="选填备注"></textarea>
          </div>
        </div>
      </section>

      <section>
        <div class="flex items-center gap-2 mb-4">
          <span class="w-1 h-4 bg-primary rounded-full"></span>
          <h3 class="text-xs font-bold uppercase tracking-widest text-on-primary-container">雇佣状态</h3>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <label v-for="status in employmentStatuses" :key="status.value" class="cursor-pointer">
            <input v-model="form.status" :value="Number(status.value)" class="hidden peer" name="status" type="radio" />
            <div class="p-4 rounded-xl border border-outline-variant/20 bg-surface-container-low peer-checked:bg-primary-container peer-checked:text-white transition-all h-full">
              <div class="flex items-center justify-between mb-1">
                <span class="text-sm font-bold">{{ status.label }}</span>
                <span class="material-symbols-outlined text-lg opacity-40 peer-checked:opacity-100" style="font-variation-settings: 'FILL' 1;">check_circle</span>
              </div>
              <p class="text-[10px] opacity-70">初始状态将同步至员工主数据。</p>
            </div>
          </label>
        </div>
      </section>
    </div>

    <div class="p-8 border-t border-outline-variant/15 flex items-center justify-between bg-surface-container-lowest">
      <button @click="emit('close')" class="px-6 py-2.5 text-sm font-bold text-on-surface-variant hover:text-primary transition-colors">取消</button>
      <button @click="submit" :disabled="submitting" class="bg-primary hover:bg-primary-container disabled:opacity-60 text-white px-10 py-3 rounded shadow-xl shadow-primary/20 flex items-center gap-2 font-bold transition-all active:scale-95">
        <span class="material-symbols-outlined text-lg" style="font-variation-settings: 'FILL' 1;">save</span>
        {{ submitting ? '保存中...' : isEditMode ? '更新员工' : '保存员工' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createEmployee, getEmployeeDetail, getEmployeeFormOptions, searchEmployeeLeaders, updateEmployee } from './api/employee.js'

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

const submitting = ref(false)
const initialized = ref(false)
const departments = ref([])
const positions = ref([])
const roles = ref([])
const employeeTypes = ref([])
const employmentStatuses = ref([])
const leaderOptions = ref([])
const leaderKeyword = ref('')
const selectedLeaderLabel = ref('')
const form = reactive(createDefaultForm())
let leaderSearchTimer = null

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
      if (!visible) return
      resetForm()
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

async function loadOptions() {
  const data = await getEmployeeFormOptions()
  departments.value = data.departments || []
  positions.value = data.positions || []
  roles.value = data.roles || []
  employeeTypes.value = data.employeeTypes || []
  employmentStatuses.value = data.employmentStatuses || []
  if (!form.employeeType && employeeTypes.value[0]) {
    form.employeeType = employeeTypes.value[0].value
  }
  if (employmentStatuses.value.length > 0) {
    form.status = Number(employmentStatuses.value[0].value)
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
    employeeType: 'FULL_TIME',
    departmentId: '',
    positionId: '',
    leaderId: null,
    entryDate: new Date().toISOString().slice(0, 10),
      status: 1,
      avatarUrl: '',
      remark: '',
      roleIds: []
    }
}

async function loadEmployeeDetail() {
  const detail = await getEmployeeDetail(props.employeeId)
  form.name = detail.name || ''
  form.phone = detail.phone || ''
  form.email = detail.email || ''
  form.employeeType = detail.employeeType || 'FULL_TIME'
  form.departmentId = detail.departmentId || ''
  form.positionId = detail.positionId || ''
  form.leaderId = detail.leaderId || null
  form.entryDate = detail.entryDate || new Date().toISOString().slice(0, 10)
  form.status = Number(detail.status ?? 1)
  form.avatarUrl = detail.avatarUrl || ''
  form.remark = detail.remark || ''
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
    form.leaderId = null
    leaderOptions.value = []
    selectedLeaderLabel.value = ''
    return
  }
  leaderSearchTimer = setTimeout(async () => {
    leaderOptions.value = await searchEmployeeLeaders({ keyword: leaderKeyword.value, limit: 8 })
  }, 250)
}

function selectLeader(leader) {
  form.leaderId = leader.id
  leaderKeyword.value = leader.name
  selectedLeaderLabel.value = `${leader.name} / ${leader.empNo || '--'}`
  leaderOptions.value = []
}

async function submit() {
  if (!form.name || !form.phone || !form.departmentId || !form.positionId || !form.entryDate || !form.employeeType) {
    ElMessage.warning('请填写必填项。')
    return
  }

  submitting.value = true
  try {
    const payload = {
      ...form,
      departmentId: Number(form.departmentId),
      positionId: Number(form.positionId),
      leaderId: form.leaderId ? Number(form.leaderId) : null,
      status: Number(form.status),
      roleIds: (form.roleIds || []).map((id) => Number(id))
    }
    if (isEditMode.value) {
      await updateEmployee({
        ...payload,
        id: Number(props.employeeId)
      })
      ElMessage.success('员工更新成功。')
    } else {
      await createEmployee(payload)
      ElMessage.success('员工创建成功。')
    }
    emit('success')
    emit('close')
  } finally {
    submitting.value = false
  }
}
</script>
