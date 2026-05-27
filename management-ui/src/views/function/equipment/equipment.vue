<template>
  <div class="function-page-shell h-full min-h-0">
    <div class="function-page-container space-y-6">
      <header class="function-page-header mb-8">
        <div>
          <div class="function-page-eyebrow">
            <span class="material-symbols-outlined">qr_code_scanner</span>
            设备巡检中心
          </div>
          <h1 class="function-page-title">设备巡检记录</h1>
          <p class="function-page-desc">
            建立固定设备二维码，贴到设备后由小程序扫码巡检，巡检记录自动沉淀到设备档案。
          </p>
        </div>
        <div class="flex flex-wrap gap-3">
          <div class="relative min-w-[280px]">
            <span class="material-symbols-outlined absolute top-1/2 left-3 -translate-y-1/2 text-[18px] text-on-surface-variant">search</span>
            <input
              v-model.trim="filters.keyword"
              class="w-full rounded-lg border border-outline-variant/20 bg-surface-container-low py-2.5 pr-4 pl-10 text-sm focus:ring-2 focus:ring-primary/30 focus:outline-none"
              placeholder="搜索设备编码、名称、位置或负责人"
              type="text"
              @keyup.enter="fetchDevices"
            />
          </div>
          <select
            v-model="filters.status"
            class="rounded-lg border border-outline-variant/20 bg-surface-container-low px-4 py-2.5 text-sm focus:ring-2 focus:ring-primary/30 focus:outline-none"
          >
            <option value="">全部状态</option>
            <option value="enabled">启用中</option>
            <option value="disabled">已停用</option>
          </select>
          <button class="function-action-secondary" @click="handleSearch">
            <span class="material-symbols-outlined text-[20px]">search</span>查询
          </button>
          <button class="function-action-secondary" @click="resetSearch">
            <span class="material-symbols-outlined text-[20px]">restart_alt</span>重置
          </button>
          <button class="function-action-secondary" @click="exportEquipmentExcel">
            <span class="material-symbols-outlined text-[20px]">file_download</span>导出 Excel
          </button>
          <button class="function-action-primary" @click="openCreate">
            <span class="material-symbols-outlined text-[20px]">add_circle</span>新增设备
          </button>
        </div>
      </header>

      <section class="grid grid-cols-1 gap-4 md:grid-cols-3">
        <div class="rounded-2xl border border-outline-variant/20 bg-white/80 p-6 shadow-sm">
          <p class="text-sm font-bold text-on-surface-variant">设备总数</p>
          <p class="mt-2 text-4xl font-black text-slate-950">{{ total }}</p>
        </div>
        <div class="rounded-2xl border border-emerald-200 bg-emerald-50/80 p-6 shadow-sm">
          <p class="text-sm font-bold text-emerald-700">固定二维码</p>
          <p class="mt-2 text-4xl font-black text-emerald-700">一次打印</p>
        </div>
        <div class="rounded-2xl border border-blue-200 bg-blue-50/80 p-6 shadow-sm">
          <p class="text-sm font-bold text-blue-700">巡检方式</p>
          <p class="mt-2 text-4xl font-black text-blue-700">扫码记录</p>
        </div>
      </section>

      <section class="relative overflow-hidden rounded-2xl border border-outline-variant/20 bg-white shadow-sm">
        <div v-if="loading" class="absolute inset-0 z-10 flex items-center justify-center bg-white/60 backdrop-blur-sm">
          <span class="material-symbols-outlined animate-spin text-3xl text-primary">progress_activity</span>
        </div>

        <div class="responsive-table-wrap">
          <table ref="equipmentTableRef" class="responsive-data-table w-full border-collapse text-left">
            <thead class="bg-surface-container-low/60">
              <tr>
                <th class="px-6 py-4 text-xs font-black tracking-wider text-on-surface-variant uppercase">设备</th>
                <th class="px-6 py-4 text-xs font-black tracking-wider text-on-surface-variant uppercase">类型/位置</th>
                <th class="px-6 py-4 text-xs font-black tracking-wider text-on-surface-variant uppercase">负责人</th>
                <th class="px-6 py-4 text-xs font-black tracking-wider text-on-surface-variant uppercase">巡检周期</th>
                <th class="px-6 py-4 text-xs font-black tracking-wider text-on-surface-variant uppercase">最近巡检</th>
                <th class="px-6 py-4 text-xs font-black tracking-wider text-on-surface-variant uppercase">状态</th>
                <th class="px-6 py-4 text-right text-xs font-black tracking-wider text-on-surface-variant uppercase">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/10">
              <tr v-for="device in devices" :key="device.id" class="group transition-colors hover:bg-surface-container-low/40">
                <td class="px-6 py-4" data-label="设备">
                  <button class="text-left" @click="openDetail(device)">
                    <div class="font-black text-primary">{{ device.equipmentName }}</div>
                    <div class="mt-1 text-xs text-on-surface-variant">{{ device.equipmentCode }}</div>
                  </button>
                </td>
                <td class="px-6 py-4" data-label="类型/位置">
                  <div class="font-bold text-slate-900">{{ device.equipmentType || '--' }}</div>
                  <div class="mt-1 text-xs text-on-surface-variant">{{ device.location || '--' }}</div>
                </td>
                <td class="px-6 py-4 text-sm" data-label="负责人">{{ device.responsiblePerson || '--' }}</td>
                <td class="px-6 py-4 text-sm" data-label="巡检周期">{{ device.inspectionCycleDays || 7 }} 天</td>
                <td class="px-6 py-4 text-sm" data-label="最近巡检">{{ formatDateTime(device.lastInspectionTime) }}</td>
                <td class="px-6 py-4" data-label="状态">
                  <span class="rounded-full px-3 py-1 text-xs font-black" :class="device.status === 'enabled' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'">
                    {{ device.status === 'enabled' ? '启用中' : '已停用' }}
                  </span>
                </td>
                <td class="px-6 py-4" data-label="操作">
                  <div class="flex justify-end gap-2">
                    <button class="rounded-lg px-3 py-2 text-sm font-bold text-primary hover:bg-primary/10" @click="openDetail(device)">详情</button>
                    <button class="rounded-lg px-3 py-2 text-sm font-bold text-secondary hover:bg-secondary/10" @click="openEdit(device)">编辑</button>
                    <button
                      v-if="device.status === 'enabled'"
                      class="rounded-lg px-3 py-2 text-sm font-bold text-rose-600 hover:bg-rose-50"
                      @click="handleDisable(device)"
                    >
                      停用
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="!loading && devices.length === 0">
                <td colspan="7" class="px-6 py-16 text-center text-sm text-on-surface-variant">暂无设备档案</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="flex items-center justify-between border-t border-outline-variant/10 bg-surface-container-low/30 px-6 py-4 text-sm">
          <span class="text-on-surface-variant">共 {{ total }} 条</span>
          <div class="flex items-center gap-2">
            <button :disabled="pageNum <= 1 || loading" class="rounded border border-outline-variant/20 px-3 py-1.5 disabled:opacity-50" @click="changePage(pageNum - 1)">上一页</button>
            <span>{{ pageNum }} / {{ totalPages }}</span>
            <button :disabled="pageNum >= totalPages || loading" class="rounded border border-outline-variant/20 px-3 py-1.5 disabled:opacity-50" @click="changePage(pageNum + 1)">下一页</button>
          </div>
        </div>
      </section>
    </div>

    <Teleport defer to="body">
      <transition name="fade">
        <div v-if="editorVisible || detailVisible" class="fixed inset-0 z-[60] bg-primary/20 backdrop-blur-sm" @click="closeDrawers"></div>
      </transition>

      <transition name="slide">
        <aside v-if="editorVisible" class="fixed top-0 right-0 z-[70] flex h-screen w-[min(720px,100vw)] flex-col border-l border-outline-variant/20 bg-white shadow-2xl">
          <header class="border-b border-outline-variant/10 px-8 py-6">
            <div class="flex items-start justify-between">
              <div>
                <h2 class="text-3xl font-black text-primary">{{ editingId ? '编辑设备' : '新增设备' }}</h2>
                <p class="mt-2 text-sm text-on-surface-variant">设备编码可自动生成；二维码固定，贴码后无需重复打印。</p>
              </div>
              <button class="rounded-full p-2 hover:bg-surface-container-high" @click="closeDrawers">
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
          </header>
          <div class="flex-1 space-y-5 overflow-y-auto px-8 py-6">
            <label class="block">
              <span class="text-sm font-black text-slate-900">设备名称 *</span>
              <input v-model.trim="form.equipmentName" class="mt-2 w-full rounded-xl border border-outline-variant/20 bg-surface-container-low px-4 py-3 focus:ring-2 focus:ring-primary/30 focus:outline-none" placeholder="例如：定型机01" />
            </label>
            <label class="block">
              <span class="text-sm font-black text-slate-900">设备编码</span>
              <input v-model.trim="form.equipmentCode" :disabled="!!editingId" class="mt-2 w-full rounded-xl border border-outline-variant/20 bg-surface-container-low px-4 py-3 focus:ring-2 focus:ring-primary/30 focus:outline-none disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-500" placeholder="不填则系统自动生成" />
              <p v-if="editingId" class="mt-2 text-xs text-on-surface-variant">设备码已用于固定二维码，创建后不可修改。</p>
            </label>
            <div class="grid grid-cols-1 gap-5 md:grid-cols-2">
              <label class="block">
                <span class="text-sm font-black text-slate-900">设备类型</span>
                <input v-model.trim="form.equipmentType" class="mt-2 w-full rounded-xl border border-outline-variant/20 bg-surface-container-low px-4 py-3 focus:ring-2 focus:ring-primary/30 focus:outline-none" placeholder="生产设备/仓储设备" />
              </label>
              <label class="block">
                <span class="text-sm font-black text-slate-900">巡检周期（天）</span>
                <input v-model.number="form.inspectionCycleDays" type="number" min="1" max="3650" class="mt-2 w-full rounded-xl border border-outline-variant/20 bg-surface-container-low px-4 py-3 focus:ring-2 focus:ring-primary/30 focus:outline-none" />
              </label>
            </div>
            <label class="block">
              <span class="text-sm font-black text-slate-900">设备位置</span>
              <input v-model.trim="form.location" class="mt-2 w-full rounded-xl border border-outline-variant/20 bg-surface-container-low px-4 py-3 focus:ring-2 focus:ring-primary/30 focus:outline-none" placeholder="例如：一车间 A 区" />
            </label>
            <label class="block">
              <span class="text-sm font-black text-slate-900">负责人</span>
              <input v-model.trim="form.responsiblePerson" class="mt-2 w-full rounded-xl border border-outline-variant/20 bg-surface-container-low px-4 py-3 focus:ring-2 focus:ring-primary/30 focus:outline-none" placeholder="设备责任人" />
            </label>
            <label class="block">
              <span class="text-sm font-black text-slate-900">状态</span>
              <select v-model="form.status" class="mt-2 w-full rounded-xl border border-outline-variant/20 bg-surface-container-low px-4 py-3 focus:ring-2 focus:ring-primary/30 focus:outline-none">
                <option value="enabled">启用中</option>
                <option value="disabled">已停用</option>
              </select>
            </label>
            <label class="block">
              <span class="text-sm font-black text-slate-900">备注</span>
              <textarea v-model.trim="form.remark" rows="4" class="mt-2 w-full rounded-xl border border-outline-variant/20 bg-surface-container-low px-4 py-3 focus:ring-2 focus:ring-primary/30 focus:outline-none" placeholder="巡检重点、注意事项等"></textarea>
            </label>
          </div>
          <footer class="flex items-center justify-between border-t border-outline-variant/10 px-8 py-6">
            <button class="function-action-secondary" @click="closeDrawers">取消</button>
            <button class="function-action-primary" :disabled="saving" @click="submitForm">
              <span class="material-symbols-outlined text-[20px]">save</span>{{ saving ? '保存中...' : '保存设备' }}
            </button>
          </footer>
        </aside>
      </transition>

      <transition name="slide">
        <aside v-if="detailVisible" class="fixed top-0 right-0 z-[70] flex h-screen w-[min(760px,100vw)] flex-col border-l border-outline-variant/20 bg-white shadow-2xl">
          <header class="border-b border-outline-variant/10 px-8 py-6">
            <div class="flex items-start justify-between">
              <div>
                <h2 class="text-3xl font-black text-primary">{{ detail?.equipmentName || '设备详情' }}</h2>
                <p class="mt-2 text-sm text-on-surface-variant">{{ detail?.equipmentCode }}</p>
              </div>
              <button class="rounded-full p-2 hover:bg-surface-container-high" @click="closeDrawers">
                <span class="material-symbols-outlined">close</span>
              </button>
            </div>
          </header>
          <div class="flex-1 overflow-y-auto px-8 py-6">
            <section class="rounded-2xl border border-primary/20 bg-primary/5 p-5">
              <div class="flex items-center justify-between gap-4">
                <div>
                  <h3 class="text-lg font-black text-primary">固定巡检二维码</h3>
                  <p class="mt-1 text-sm text-on-surface-variant">打印一次后贴在设备上，员工扫码即可填写巡检记录。</p>
                </div>
                <span class="rounded-full bg-white px-3 py-1 text-xs font-black text-primary">已生成</span>
              </div>
              <div class="mt-4 rounded-xl bg-white p-4 text-sm text-on-surface-variant">
                现场只需要打印并张贴到设备上，员工扫码即可填写巡检记录。
              </div>
            </section>

            <section class="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2">
              <div class="rounded-xl bg-surface-container-low p-4">
                <div class="text-xs text-on-surface-variant">设备类型</div>
                <div class="mt-2 font-bold">{{ detail?.equipmentType || '--' }}</div>
              </div>
              <div class="rounded-xl bg-surface-container-low p-4">
                <div class="text-xs text-on-surface-variant">设备位置</div>
                <div class="mt-2 font-bold">{{ detail?.location || '--' }}</div>
              </div>
              <div class="rounded-xl bg-surface-container-low p-4">
                <div class="text-xs text-on-surface-variant">负责人</div>
                <div class="mt-2 font-bold">{{ detail?.responsiblePerson || '--' }}</div>
              </div>
              <div class="rounded-xl bg-surface-container-low p-4">
                <div class="text-xs text-on-surface-variant">最近巡检</div>
                <div class="mt-2 font-bold">{{ formatDateTime(detail?.lastInspectionTime) }}</div>
              </div>
            </section>

            <section class="mt-8">
              <div class="mb-4 flex items-center justify-between">
                <h3 class="text-lg font-black text-slate-950">巡检记录</h3>
                <button class="rounded-lg px-3 py-2 text-sm font-bold text-primary hover:bg-primary/10" @click="fetchRecords">刷新</button>
              </div>
              <div v-if="recordsLoading" class="py-8 text-center text-on-surface-variant">正在加载...</div>
              <div v-else-if="records.length === 0" class="rounded-xl bg-surface-container-low p-8 text-center text-sm text-on-surface-variant">暂无巡检记录</div>
              <div v-else class="space-y-3">
                <article v-for="record in records" :key="record.id" class="rounded-xl border border-outline-variant/15 p-4">
                  <div class="flex items-center justify-between gap-3">
                    <span class="rounded-full px-3 py-1 text-xs font-black" :class="record.inspectionResult === 'normal' ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'">
                      {{ record.inspectionResult === 'normal' ? '正常' : '异常' }}
                    </span>
                    <span class="text-xs text-on-surface-variant">{{ formatDateTime(record.inspectionTime) }}</span>
                  </div>
                  <p v-if="record.abnormalDesc" class="mt-3 text-sm text-rose-700">{{ record.abnormalDesc }}</p>
                  <p v-if="record.remark" class="mt-2 text-sm text-slate-700">{{ record.remark }}</p>
                  <p class="mt-3 text-xs text-on-surface-variant">巡检人：{{ record.inspectorName || '--' }}</p>
                </article>
              </div>
            </section>
          </div>
        </aside>
      </transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, reactive, ref } from 'vue'
import { exportTableElementToExcel } from '@/utils/tableExport'
import {
  disableEquipment,
  getEquipmentDetail,
  getEquipmentInspectionRecords,
  getEquipmentPage,
  saveEquipment
} from './api/equipment'

const loading = ref(false)
const saving = ref(false)
const devices = ref([])
const total = ref(0)
const totalPages = ref(1)
const pageNum = ref(1)
const pageSize = ref(10)
const filters = reactive({ keyword: '', status: '' })
const editorVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref(null)
const detail = ref(null)
const records = ref([])
const recordsLoading = ref(false)
const equipmentTableRef = ref(null)

const defaultForm = () => ({
  equipmentCode: '',
  equipmentName: '',
  equipmentType: '',
  location: '',
  responsiblePerson: '',
  inspectionCycleDays: 7,
  status: 'enabled',
  remark: ''
})

const form = reactive(defaultForm())

const queryParams = computed(() => ({
  pageNum: pageNum.value,
  pageSize: pageSize.value,
  keyword: filters.keyword || undefined,
  status: filters.status || undefined
}))

async function fetchDevices() {
  loading.value = true
  try {
    const page = await getEquipmentPage(queryParams.value)
    devices.value = page?.data || []
    total.value = Number(page?.total || 0)
    totalPages.value = Math.max(1, Number(page?.pages || 1))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  fetchDevices()
}

function resetSearch() {
  filters.keyword = ''
  filters.status = ''
  pageNum.value = 1
  fetchDevices()
}

async function exportEquipmentExcel() {
  try {
    await exportTableElementToExcel(equipmentTableRef.value, {
      fileName: '设备巡检记录',
      sheetName: '设备巡检记录'
    })
    ElMessage.success('Excel 已导出')
  } catch (error) {
    ElMessage.warning(error?.message || '导出失败，请稍后重试')
  }
}

function changePage(nextPage) {
  pageNum.value = Math.min(Math.max(1, nextPage), totalPages.value)
  fetchDevices()
}

function resetForm(data = {}) {
  Object.assign(form, defaultForm(), data)
}

function openCreate() {
  editingId.value = null
  resetForm()
  editorVisible.value = true
}

function openEdit(device) {
  editingId.value = device.id
  resetForm(device)
  editorVisible.value = true
}

async function submitForm() {
  if (!form.equipmentName?.trim()) {
    ElMessage.warning('请填写设备名称')
    return
  }
  saving.value = true
  try {
    await saveEquipment({ ...form, id: editingId.value || undefined })
    ElMessage.success('设备档案已保存')
    editorVisible.value = false
    await fetchDevices()
  } finally {
    saving.value = false
  }
}

async function handleDisable(device) {
  await ElMessageBox.confirm(`确认停用设备「${device.equipmentName}」？停用后小程序无法继续扫码巡检。`, '停用设备', {
    confirmButtonText: '确认停用',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await disableEquipment(device.id)
  ElMessage.success('设备已停用')
  fetchDevices()
}

async function openDetail(device) {
  detailVisible.value = true
  detail.value = await getEquipmentDetail(device.id)
  await fetchRecords()
}

async function fetchRecords() {
  if (!detail.value?.id) {
    records.value = []
    return
  }
  recordsLoading.value = true
  try {
    const page = await getEquipmentInspectionRecords({
      equipmentId: detail.value.id,
      pageNum: 1,
      pageSize: 20
    })
    records.value = page?.data || []
  } finally {
    recordsLoading.value = false
  }
}

function closeDrawers() {
  editorVisible.value = false
  detailVisible.value = false
}

function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

fetchDevices()
</script>
