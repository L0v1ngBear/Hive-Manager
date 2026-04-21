<template>
  <div class="label-page">
    <header class="label-header">
      <div class="header-left">
        <div class="header-icon">
          <span class="material-symbols-outlined text-xl">sell</span>
        </div>
        <div>
          <input v-model="templateName" class="template-name-input" placeholder="输入模板名称" />
          <p class="header-desc">管理端设计和维护标签模板，小程序端选择同一批模板进行打印。</p>
        </div>
      </div>

      <div class="header-actions">
        <div class="mode-switch">
          <button :class="{ active: editMode === 'visual' }" @click="switchMode('visual')">可视化设计</button>
          <button :class="{ active: editMode === 'source' }" @click="switchMode('source')">源码模式</button>
        </div>

        <input type="file" ref="fileInputRef" accept=".prn,.txt" class="hidden" @change="handleFileUpload" />
        <button @click="triggerUpload" class="secondary-action">
          <span class="material-symbols-outlined text-lg">upload_file</span>
          上传 PRN
        </button>

        <label class="default-check">
          <input v-model="isDefault" type="checkbox" />
          设为默认
        </label>

        <button :disabled="saving" @click="saveTemplate" class="primary-action">
          <span class="material-symbols-outlined text-lg">save</span>
          {{ saving ? '保存中...' : currentTemplateId ? '保存修改' : '保存模板' }}
        </button>
      </div>
    </header>

    <main class="label-main">
      <section v-if="editMode === 'visual'" class="designer-area">
        <aside class="tool-panel">
          <div class="panel-head">
            <h3 class="panel-title">字段组件</h3>
            <p class="panel-tip">点击添加到画布，拖动元素调整位置。</p>
          </div>

          <div class="tool-list">
            <button v-for="field in fieldOptions" :key="field.field" class="tool-item" @click="addElement(field)">
              <span class="material-symbols-outlined text-lg">{{ field.type === 'barcode' ? 'barcode' : 'text_fields' }}</span>
              <span>{{ field.label }}</span>
            </button>
          </div>

          <div class="size-card">
            <label>
              标签宽度(mm)
              <input v-model.number="canvas.widthMm" type="number" min="20" max="120" />
            </label>
            <label>
              标签高度(mm)
              <input v-model.number="canvas.heightMm" type="number" min="20" max="100" />
            </label>
          </div>

          <div class="hint-card">
            <span class="material-symbols-outlined">tips_and_updates</span>
            <p>保存后会自动生成 TSPL 指令到模板内容，小程序端无需知道可视化结构。</p>
          </div>
        </aside>

        <section class="canvas-wrap">
          <div class="canvas-toolbar">
            <div>
              <h3>标签画布</h3>
              <p>{{ canvas.widthMm }}mm × {{ canvas.heightMm }}mm，当前元素 {{ designElements.length }} 个。</p>
            </div>
            <div class="toolbar-actions">
              <button class="ghost-action" @click="resetVisualTemplate">恢复默认布局</button>
              <button class="ghost-action" @click="copyTspl">复制 TSPL</button>
            </div>
          </div>

          <div class="canvas-scroll">
            <div class="label-canvas" :style="canvasStyle" @click="selectedElementId = ''">
              <div
                v-for="element in designElements"
                :key="element.id"
                class="canvas-element"
                :class="{ selected: selectedElementId === element.id, barcode: element.type === 'barcode' }"
                :style="elementStyle(element)"
                @click.stop="selectedElementId = element.id"
                @mousedown.stop="startDrag($event, element)"
              >
                <template v-if="element.type === 'barcode'">
                  <div class="barcode-preview"></div>
                  <span>{{ sampleValue(element.field) }}</span>
                </template>
                <template v-else>
                  {{ element.label }}: {{ sampleValue(element.field) }}
                </template>
              </div>
            </div>
          </div>
        </section>

        <aside class="property-panel">
          <h3 class="panel-title">属性</h3>
          <template v-if="selectedElement">
            <label>
              显示名称
              <input v-model="selectedElement.label" />
            </label>
            <label>
              字段
              <select v-model="selectedElement.field" @change="syncSelectedLabel">
                <option v-for="field in fieldOptions" :key="field.field" :value="field.field">{{ field.label }}</option>
              </select>
            </label>
            <div class="property-grid">
              <label>
                X
                <input v-model.number="selectedElement.x" type="number" min="0" />
              </label>
              <label>
                Y
                <input v-model.number="selectedElement.y" type="number" min="0" />
              </label>
            </div>
            <template v-if="selectedElement.type === 'text'">
              <label>
                字号倍数
                <input v-model.number="selectedElement.fontSize" type="number" min="1" max="3" />
              </label>
            </template>
            <template v-else>
              <label>
                条码高度
                <input v-model.number="selectedElement.height" type="number" min="40" max="160" />
              </label>
            </template>
            <button class="danger-action" @click="removeSelectedElement">删除元素</button>
          </template>
          <div v-else class="empty-property">
            <span class="material-symbols-outlined">ads_click</span>
            <p>请选择画布元素后编辑属性。</p>
          </div>
        </aside>
      </section>

      <section v-else class="source-area">
        <div class="source-editor">
          <div class="editor-head">
            <span class="material-symbols-outlined text-sm">code</span>
            TEMPLATE_SOURCE_CODE
            <span v-if="uploadedFile" class="ml-4 text-xs text-gray-400">已选择：{{ uploadedFile.name }}</span>
          </div>
          <textarea
            v-model="templateCode"
            spellcheck="false"
            placeholder="在这里粘贴蓝牙标签打印机底层指令，例如 TSPL/PRN。支持 ${modelCode} 或 {modelCode} 占位符。"
          ></textarea>
        </div>
      </section>

      <aside class="template-sidebar">
        <section class="sidebar-card">
          <div class="flex items-center justify-between mb-4">
            <h3 class="panel-title">动态变量</h3>
            <span class="text-xs text-on-surface-variant">{{ detectedVariables.length }} 个</span>
          </div>
          <div v-if="detectedVariables.length" class="space-y-2">
            <div v-for="variable in detectedVariables" :key="variable" class="variable-chip">{{ variable }}</div>
          </div>
          <div v-else class="empty-state">暂无变量</div>
        </section>

        <section class="sidebar-card flex-1 overflow-hidden">
          <div class="flex items-center justify-between mb-4">
            <h3 class="panel-title">已保存模板</h3>
            <button @click="fetchTemplates" class="text-xs font-bold text-primary hover:underline">刷新</button>
          </div>

          <div class="template-list">
            <div v-if="templates.length === 0" class="empty-state">暂无模板，请先保存</div>
            <div v-for="item in templates" :key="item.id" class="template-card" :class="{ active: currentTemplateId === item.id }">
              <div class="flex items-start justify-between gap-3">
                <div>
                  <div class="font-bold text-sm text-on-surface">{{ item.name }}</div>
                  <div class="text-xs text-on-surface-variant mt-1">{{ item.fileName || (item.designJson ? '可视化设计' : '手动保存') }}</div>
                </div>
                <span v-if="item.isDefault === 1" class="default-badge">默认</span>
              </div>
              <div class="mt-3 flex flex-wrap gap-2">
                <button @click="loadTemplate(item)" class="mini-action">编辑</button>
                <button @click="setDefault(item.id)" class="mini-action primary">设默认</button>
                <button @click="removeTemplate(item.id)" class="mini-action danger">停用</button>
              </div>
            </div>
          </div>
        </section>
      </aside>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteLabelTemplate,
  listLabelTemplateVariables,
  listLabelTemplates,
  saveLabelTemplate,
  setDefaultLabelTemplate,
  uploadLabelTemplate
} from './label/api/label'

defineOptions({ name: 'LabelTemplateDesigner' })

type EditMode = 'visual' | 'source'
type ElementType = 'text' | 'barcode'

interface FieldOption {
  label: string
  field: string
  type: ElementType
  sampleValue?: string
}

interface DesignElement {
  id: string
  type: ElementType
  label: string
  field: string
  x: number
  y: number
  fontSize: number
  height: number
}

const DOTS_PER_MM = 8
const PREVIEW_SCALE = 0.75

const fallbackFields: FieldOption[] = [
  { label: '条码', field: 'barcode', type: 'barcode', sampleValue: 'CL20260421001' },
  { label: '型号', field: 'modelCode', type: 'text', sampleValue: 'M-2026-A' },
  { label: '米数', field: 'meters', type: 'text', sampleValue: '120.50' },
  { label: '规格', field: 'spec', type: 'text', sampleValue: '160' }
]

const fieldOptions = ref<FieldOption[]>(fallbackFields)
const templateName = ref('面料入库标签')
const templateCode = ref('')
const editMode = ref<EditMode>('visual')
const isDefault = ref(false)
const saving = ref(false)
const uploadedFile = ref<File | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const templates = ref<any[]>([])
const selectedElementId = ref('')
const currentTemplateId = ref<number | null>(null)

const canvas = reactive({
  widthMm: 70,
  heightMm: 50
})

const designElements = ref<DesignElement[]>(defaultElements())

const canvasStyle = computed(() => ({
  width: `${canvas.widthMm * DOTS_PER_MM * PREVIEW_SCALE}px`,
  height: `${canvas.heightMm * DOTS_PER_MM * PREVIEW_SCALE}px`
}))

const selectedElement = computed(() => designElements.value.find((item) => item.id === selectedElementId.value))

const visualTemplateCode = computed(() => generateTspl())

const detectedVariables = computed(() => {
  const source = editMode.value === 'visual' ? visualTemplateCode.value : templateCode.value
  if (!source) return []
  const vars = new Set<string>()
  const collect = (regex: RegExp) => {
    let match
    while ((match = regex.exec(source)) !== null) {
      vars.add(match[1].trim())
    }
  }
  collect(/\$\{([^}]+)\}/g)
  collect(/\{([^}]+)\}/g)
  return Array.from(vars)
})

watch(visualTemplateCode, (value) => {
  if (editMode.value === 'visual') {
    templateCode.value = value
  }
}, { immediate: true })

function defaultElements(): DesignElement[] {
  return [
    { id: cryptoId(), type: 'text', label: '型号', field: 'modelCode', x: 30, y: 30, fontSize: 1, height: 80 },
    { id: cryptoId(), type: 'text', label: '米数', field: 'meters', x: 30, y: 70, fontSize: 1, height: 80 },
    { id: cryptoId(), type: 'text', label: '规格', field: 'spec', x: 30, y: 110, fontSize: 1, height: 80 },
    { id: cryptoId(), type: 'barcode', label: '条码', field: 'barcode', x: 30, y: 160, fontSize: 1, height: 80 }
  ]
}

function cryptoId() {
  return `el_${Date.now()}_${Math.random().toString(16).slice(2)}`
}

function switchMode(mode: EditMode) {
  editMode.value = mode
  if (mode === 'visual') {
    uploadedFile.value = null
    templateCode.value = visualTemplateCode.value
  }
}

function triggerUpload() {
  fileInputRef.value?.click()
}

function handleFileUpload(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  uploadedFile.value = file
  editMode.value = 'source'
  currentTemplateId.value = null
  if (!templateName.value.trim()) {
    templateName.value = file.name.replace(/\.(prn|txt)$/i, '')
  }

  const reader = new FileReader()
  reader.onload = (e) => {
    templateCode.value = e.target?.result as string
    ElMessage.success(`成功读取文件: ${file.name}`)
    target.value = ''
  }
  reader.onerror = () => ElMessage.error('文件读取失败')
  reader.readAsText(file, 'UTF-8')
}

function addElement(field: FieldOption) {
  const element: DesignElement = {
    id: cryptoId(),
    type: field.type,
    label: field.label,
    field: field.field,
    x: 50,
    y: 50 + designElements.value.length * 34,
    fontSize: 1,
    height: field.type === 'barcode' ? 80 : 80
  }
  designElements.value.push(element)
  selectedElementId.value = element.id
}

function syncSelectedLabel() {
  if (!selectedElement.value) return
  const field = fieldOptions.value.find((item) => item.field === selectedElement.value?.field)
  if (field) {
    selectedElement.value.label = field.label
    selectedElement.value.type = field.type
  }
}

function removeSelectedElement() {
  if (!selectedElement.value) return
  designElements.value = designElements.value.filter((item) => item.id !== selectedElement.value?.id)
  selectedElementId.value = ''
}

function resetVisualTemplate() {
  designElements.value = defaultElements()
  selectedElementId.value = designElements.value[0]?.id || ''
}

function sampleValue(field: string) {
  return fieldOptions.value.find((item) => item.field === field)?.sampleValue || field
}

function elementStyle(element: DesignElement) {
  const common = {
    left: `${element.x * PREVIEW_SCALE}px`,
    top: `${element.y * PREVIEW_SCALE}px`
  }
  if (element.type === 'barcode') {
    return {
      ...common,
      width: '180px',
      height: `${Math.max(element.height * PREVIEW_SCALE, 42)}px`
    }
  }
  return {
    ...common,
    fontSize: `${12 + element.fontSize * 3}px`
  }
}

function startDrag(event: MouseEvent, element: DesignElement) {
  selectedElementId.value = element.id
  const startX = event.clientX
  const startY = event.clientY
  const originX = element.x
  const originY = element.y

  const onMove = (moveEvent: MouseEvent) => {
    const deltaX = Math.round((moveEvent.clientX - startX) / PREVIEW_SCALE)
    const deltaY = Math.round((moveEvent.clientY - startY) / PREVIEW_SCALE)
    element.x = Math.max(0, originX + deltaX)
    element.y = Math.max(0, originY + deltaY)
  }

  const onUp = () => {
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }

  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

function generateTspl() {
  const lines = [
    `SIZE ${canvas.widthMm} mm,${canvas.heightMm} mm`,
    'GAP 2 mm,0 mm',
    'DIRECTION 1',
    'CLS'
  ]

  for (const element of designElements.value) {
    const safeLabel = element.label.replace(/"/g, '')
    if (element.type === 'barcode') {
      lines.push(`BARCODE ${element.x},${element.y},"128",${element.height},1,0,2,2,"\${${element.field}}"`)
      lines.push(`TEXT ${element.x},${element.y + element.height + 12},"TSS24.BF2",0,1,1,"\${${element.field}}"`)
    } else {
      lines.push(`TEXT ${element.x},${element.y},"TSS24.BF2",0,${element.fontSize},${element.fontSize},"${safeLabel}: \${${element.field}}"`)
    }
  }
  lines.push('PRINT 1,1')
  return lines.join('\r\n')
}

function buildDesignJson() {
  return JSON.stringify({
    version: 1,
    widthMm: canvas.widthMm,
    heightMm: canvas.heightMm,
    elements: designElements.value
  })
}

function loadDesignJson(designJson?: string) {
  if (!designJson) return false
  try {
    const parsed = JSON.parse(designJson)
    canvas.widthMm = Number(parsed.widthMm || 70)
    canvas.heightMm = Number(parsed.heightMm || 50)
    designElements.value = Array.isArray(parsed.elements) && parsed.elements.length ? parsed.elements : defaultElements()
    selectedElementId.value = designElements.value[0]?.id || ''
    return true
  } catch {
    return false
  }
}

async function copyTspl() {
  templateCode.value = visualTemplateCode.value
  await navigator.clipboard?.writeText(templateCode.value)
  ElMessage.success('TSPL 指令已复制')
}

async function fetchVariables() {
  try {
    const variables = await listLabelTemplateVariables({ printType: 'label' })
    if (Array.isArray(variables) && variables.length) {
      fieldOptions.value = variables
    }
  } catch {
    fieldOptions.value = fallbackFields
  }
}

async function fetchTemplates() {
  templates.value = await listLabelTemplates({ printType: 'label' })
}

async function saveTemplate() {
  if (!templateName.value.trim()) {
    ElMessage.warning('模板名称不能为空')
    return
  }
  if (editMode.value === 'visual') {
    templateCode.value = visualTemplateCode.value
  }
  if (!templateCode.value.trim()) {
    ElMessage.warning('模板代码不能为空')
    return
  }

  saving.value = true
  try {
    if (uploadedFile.value && editMode.value === 'source' && currentTemplateId.value == null) {
      const fileForUpload = new File([templateCode.value], uploadedFile.value.name, { type: uploadedFile.value.type || 'text/plain' })
      const formData = new FormData()
      formData.append('file', fileForUpload)
      formData.append('name', templateName.value.trim())
      formData.append('printType', 'label')
      formData.append('isDefault', isDefault.value ? '1' : '0')
      const saved = await uploadLabelTemplate(formData)
      currentTemplateId.value = saved?.id || null
    } else {
      const saved = await saveLabelTemplate({
        id: currentTemplateId.value,
        name: templateName.value.trim(),
        printType: 'label',
        content: templateCode.value,
        designJson: editMode.value === 'visual' ? buildDesignJson() : undefined,
        widthMm: canvas.widthMm,
        heightMm: canvas.heightMm,
        isDefault: isDefault.value ? 1 : 0
      })
      currentTemplateId.value = saved?.id || currentTemplateId.value
    }
    ElMessage.success('模板保存成功，小程序端已可选择使用')
    uploadedFile.value = null
    await fetchTemplates()
  } finally {
    saving.value = false
  }
}

function loadTemplate(item: any) {
  currentTemplateId.value = item.id
  templateName.value = item.name
  templateCode.value = item.content || ''
  isDefault.value = item.isDefault === 1
  uploadedFile.value = null

  if (loadDesignJson(item.designJson)) {
    editMode.value = 'visual'
    templateCode.value = visualTemplateCode.value
  } else {
    editMode.value = 'source'
  }
}

async function setDefault(id: number) {
  await setDefaultLabelTemplate(id)
  ElMessage.success('默认模板已更新')
  await fetchTemplates()
}

async function removeTemplate(id: number) {
  await ElMessageBox.confirm('停用后小程序端将不再显示该模板，确认继续吗？', '停用模板', {
    confirmButtonText: '确认停用',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteLabelTemplate(id)
  if (currentTemplateId.value === id) {
    currentTemplateId.value = null
  }
  ElMessage.success('模板已停用')
  await fetchTemplates()
}

onMounted(async () => {
  await fetchVariables()
  await fetchTemplates()
})
</script>

<style scoped>
.label-page {
  height: calc(100vh - 8rem);
  display: flex;
  flex-direction: column;
  margin: -1rem;
  background: linear-gradient(135deg, #f7f9fc 0%, #edf3f8 48%, #f8fafc 100%);
}

@media (min-width: 768px) {
  .label-page {
    margin: -2rem;
  }
}

.label-header {
  min-height: 76px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.header-left,
.header-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.header-icon {
  width: 40px;
  height: 40px;
  border-radius: 18px;
  color: #fff;
  background: #0f4c81;
  display: grid;
  place-items: center;
  box-shadow: 0 12px 24px rgba(15, 76, 129, 0.18);
}

.header-desc {
  color: #64748b;
  font-size: 12px;
  padding: 0 8px;
}

.template-name-input {
  min-width: 280px;
  border: none;
  background: transparent;
  color: #111827;
  font-size: 18px;
  font-weight: 900;
  outline: none;
  padding: 2px 8px;
  border-radius: 10px;
}

.template-name-input:hover,
.template-name-input:focus {
  background: rgba(226, 232, 240, 0.65);
}

.mode-switch {
  display: flex;
  padding: 4px;
  border-radius: 14px;
  background: #e8eef6;
}

.mode-switch button {
  border: none;
  padding: 8px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 800;
  color: #64748b;
  background: transparent;
}

.mode-switch button.active {
  color: #0f4c81;
  background: #fff;
  box-shadow: 0 6px 18px rgba(15, 76, 129, 0.12);
}

.default-check {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #475569;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.default-check input {
  accent-color: #0f4c81;
}

.primary-action,
.secondary-action,
.ghost-action,
.danger-action,
.mini-action {
  border: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-weight: 800;
  transition: 0.2s ease;
}

.primary-action {
  border-radius: 14px;
  padding: 10px 18px;
  color: #fff;
  background: #0f4c81;
  box-shadow: 0 12px 24px rgba(15, 76, 129, 0.2);
}

.secondary-action,
.ghost-action {
  border-radius: 14px;
  padding: 10px 14px;
  color: #0f4c81;
  background: #dbeafe;
}

.danger-action {
  width: 100%;
  border-radius: 12px;
  padding: 10px 12px;
  color: #dc2626;
  background: #fee2e2;
}

.label-main {
  min-height: 0;
  flex: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 20px;
  padding: 20px;
  overflow: hidden;
}

.designer-area {
  min-width: 0;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 260px;
  gap: 16px;
}

.tool-panel,
.canvas-wrap,
.property-panel,
.sidebar-card,
.source-editor {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 24px;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.08);
}

.tool-panel,
.property-panel,
.sidebar-card {
  padding: 18px;
}

.panel-title {
  color: #111827;
  font-size: 14px;
  font-weight: 900;
}

.panel-tip {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.tool-list {
  margin-top: 16px;
  display: grid;
  gap: 10px;
}

.tool-item {
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 14px;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #334155;
  background: #f8fafc;
  font-weight: 800;
  text-align: left;
}

.tool-item:hover {
  color: #0f4c81;
  border-color: rgba(15, 76, 129, 0.24);
  background: #eef6ff;
}

.size-card,
.hint-card {
  margin-top: 18px;
  padding: 14px;
  border-radius: 18px;
  background: #f8fafc;
}

.size-card {
  display: grid;
  gap: 12px;
}

.hint-card {
  display: flex;
  gap: 10px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.7;
}

.size-card label,
.property-panel label {
  display: grid;
  gap: 6px;
  min-width: 0;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.size-card input,
.property-panel input,
.property-panel select {
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 12px;
  padding: 9px 10px;
  color: #111827;
  background: #fff;
  outline: none;
}

.property-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
  overflow: hidden;
}

.property-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
  min-width: 0;
}

.canvas-wrap {
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.canvas-toolbar {
  min-height: 74px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 18px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

.canvas-toolbar h3 {
  color: #111827;
  font-weight: 900;
}

.canvas-toolbar p {
  color: #64748b;
  font-size: 12px;
  margin-top: 4px;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.canvas-scroll {
  flex: 1;
  overflow: auto;
  display: grid;
  place-items: center;
  padding: 36px;
  background:
    linear-gradient(90deg, rgba(148, 163, 184, 0.12) 1px, transparent 1px),
    linear-gradient(rgba(148, 163, 184, 0.12) 1px, transparent 1px);
  background-size: 18px 18px;
}

.label-canvas {
  position: relative;
  background: #fff;
  border: 1px solid #111827;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.18);
}

.canvas-element {
  position: absolute;
  user-select: none;
  cursor: move;
  color: #111827;
  font-weight: 800;
  line-height: 1.2;
  padding: 2px 4px;
  border: 1px dashed transparent;
  white-space: nowrap;
}

.canvas-element.selected {
  border-color: #2563eb;
  background: rgba(219, 234, 254, 0.55);
}

.canvas-element.barcode {
  display: grid;
  gap: 4px;
  text-align: center;
  font-size: 11px;
}

.barcode-preview {
  width: 100%;
  height: 100%;
  min-height: 34px;
  background: repeating-linear-gradient(90deg, #111827 0 3px, #fff 3px 5px, #111827 5px 7px, #fff 7px 11px);
}

.empty-property,
.empty-state {
  color: #94a3b8;
  font-size: 13px;
  text-align: center;
  line-height: 1.8;
  padding: 28px 10px;
}

.empty-property .material-symbols-outlined {
  display: block;
  color: #cbd5e1;
  font-size: 42px;
  margin-bottom: 8px;
}

.source-area {
  min-width: 0;
  display: flex;
}

.source-editor {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #1e1e1e;
  border-color: #111827;
}

.editor-head {
  height: 42px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  color: #9ca3af;
  font-size: 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  background: #2d2d2d;
  border-bottom: 1px solid #404040;
}

.source-editor textarea {
  flex: 1;
  width: 100%;
  border: none;
  resize: none;
  outline: none;
  padding: 18px;
  color: #4ade80;
  background: transparent;
  font-size: 13px;
  line-height: 1.7;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.template-sidebar {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
}

.variable-chip {
  padding: 10px 12px;
  border-left: 4px solid #0f4c81;
  border-radius: 12px;
  color: #111827;
  background: #f1f5f9;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  font-weight: 900;
}

.template-list {
  height: calc(100% - 36px);
  overflow-y: auto;
  padding-right: 4px;
}

.template-card {
  padding: 14px;
  margin-bottom: 12px;
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: #f8fafc;
}

.template-card.active {
  border-color: rgba(15, 76, 129, 0.42);
  box-shadow: 0 10px 24px rgba(15, 76, 129, 0.12);
}

.default-badge {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 3px 8px;
  color: #fff;
  background: #0f4c81;
  font-size: 11px;
  font-weight: 900;
}

.mini-action {
  border-radius: 10px;
  padding: 6px 10px;
  color: #334155;
  background: #e2e8f0;
  font-size: 12px;
}

.mini-action.primary {
  color: #0f4c81;
  background: #dbeafe;
}

.mini-action.danger {
  color: #dc2626;
  background: #fee2e2;
}

@media (max-width: 1280px) {
  .label-main {
    grid-template-columns: 1fr;
    overflow-y: auto;
  }

  .designer-area {
    grid-template-columns: 1fr;
  }

  .template-sidebar {
    min-height: 320px;
  }
}
</style>
