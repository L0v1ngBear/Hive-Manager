<template>
  <div class="h-[calc(100vh-8rem)] flex flex-col bg-surface-container-low -m-4 md:-m-8">
    <header class="h-16 bg-surface-container-lowest flex items-center justify-between px-6 shrink-0 relative z-10 shadow-sm ring-1 ring-outline-variant/10">
      <div class="flex items-center gap-4">
        <div class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center text-white shadow-sm">
          <span class="material-symbols-outlined text-sm">terminal</span>
        </div>
        <div>
          <input v-model="templateName" class="text-lg font-bold text-on-surface bg-transparent border-none focus:ring-0 p-0 hover:bg-surface-container-highest rounded px-2 transition-colors cursor-text" placeholder="输入模板名称" />
          <p class="text-xs text-on-surface-variant px-2">上传 PRN 后会保存到后端，小程序打印时可选择模板</p>
        </div>
      </div>

      <div class="flex items-center gap-3">
        <input type="file" ref="fileInputRef" accept=".prn,.txt" class="hidden" @change="handleFileUpload" />

        <button @click="triggerUpload" class="px-4 py-2 rounded-xl text-sm font-bold text-primary bg-primary-container hover:bg-primary-container/80 transition-colors flex items-center gap-2">
          <span class="material-symbols-outlined text-lg">upload_file</span>
          上传 Bartender 导出文件 (.prn)
        </button>

        <label class="flex items-center gap-2 text-sm font-bold text-on-surface-variant cursor-pointer select-none">
          <input v-model="isDefault" type="checkbox" class="accent-current" />
          设为默认
        </label>

        <button :disabled="saving" @click="saveTemplate" class="bg-primary text-white px-5 py-2 rounded-xl text-sm font-bold shadow-sm hover:bg-primary/90 transition-colors flex items-center gap-2 disabled:opacity-60">
          <span class="material-symbols-outlined text-lg">save</span>
          {{ saving ? '保存中...' : '保存系统模板' }}
        </button>
      </div>
    </header>

    <main class="flex-1 flex overflow-hidden p-6 gap-6">
      <section class="flex-[2] flex flex-col bg-[#1e1e1e] rounded-2xl shadow-inner overflow-hidden ring-1 ring-gray-800">
        <div class="h-10 bg-[#2d2d2d] flex items-center px-4 border-b border-[#404040]">
          <span class="text-xs font-mono text-gray-400 flex items-center gap-2">
            <span class="material-symbols-outlined text-sm">code</span>
            TEMPLATE_SOURCE_CODE
          </span>
          <span v-if="uploadedFile" class="ml-4 text-xs text-gray-400">已选择：{{ uploadedFile.name }}</span>
          <div class="ml-auto flex gap-2">
            <span class="w-3 h-3 rounded-full bg-error"></span>
            <span class="w-3 h-3 rounded-full bg-orange-400"></span>
            <span class="w-3 h-3 rounded-full bg-green-400"></span>
          </div>
        </div>

        <div class="flex-1 relative">
          <textarea
            v-model="templateCode"
            spellcheck="false"
            placeholder="请在此粘贴蓝牙打印机底层指令（如 TSPL），或点击右上角上传 Bartender 生成的 .prn 文件...&#10;&#10;占位符支持 ${modelCode} 或 {modelCode}。&#10;示例：TEXT 20,30,&#34;TSS24.BF2&#34;,0,1,1,&#34;型号: ${modelCode}&#34;"
            class="absolute inset-0 w-full h-full bg-transparent text-[#4ade80] font-mono text-sm p-4 border-none focus:ring-0 resize-none leading-relaxed tracking-wide placeholder:text-gray-600"
          ></textarea>
        </div>
      </section>

      <aside class="flex-[1] bg-surface-container-lowest rounded-2xl shadow-sm ring-1 ring-outline-variant/20 flex flex-col overflow-hidden">
        <div class="p-5 border-b border-outline-variant/20">
          <h3 class="text-sm font-bold text-primary flex items-center gap-2">
            <span class="material-symbols-outlined text-base">data_object</span> 动态变量解析
          </h3>
          <p class="text-xs text-on-surface-variant mt-1">系统会自动提取 <code class="bg-surface-container-high px-1 rounded text-primary">${xxx}</code> 或 <code class="bg-surface-container-high px-1 rounded text-primary">{xxx}</code> 占位符。</p>
        </div>

        <div class="p-5 space-y-4 border-b border-outline-variant/20 max-h-[45%] overflow-y-auto">
          <template v-if="detectedVariables.length > 0">
            <div v-for="variable in detectedVariables" :key="variable" class="p-4 bg-surface-container-low rounded-xl border-l-4 border-primary group">
              <div class="flex justify-between items-center">
                <span class="font-mono text-sm font-bold text-on-surface">{{ variable }}</span>
                <span class="text-[11px] px-2 py-0.5 rounded bg-primary-container text-on-primary-container">已捕获</span>
              </div>
            </div>
          </template>

          <div v-else class="py-12 flex flex-col items-center justify-center text-on-surface-variant/40">
            <span class="material-symbols-outlined text-4xl mb-2">manage_search</span>
            <p class="text-sm">未检测到任何动态变量</p>
          </div>
        </div>

        <div class="flex-1 overflow-y-auto p-5">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-bold text-on-surface">已保存模板</h3>
            <button @click="fetchTemplates" class="text-xs font-bold text-primary hover:underline">刷新</button>
          </div>

          <div v-if="templates.length === 0" class="text-sm text-on-surface-variant/50 py-8 text-center">暂无模板，请先保存</div>

          <div v-for="item in templates" :key="item.id" class="p-4 mb-3 rounded-xl bg-surface-container-low border border-outline-variant/20">
            <div class="flex items-start justify-between gap-3">
              <div>
                <div class="font-bold text-sm text-on-surface">{{ item.name }}</div>
                <div class="text-xs text-on-surface-variant mt-1">{{ item.fileName || '手动保存' }}</div>
              </div>
              <span v-if="item.isDefault === 1" class="text-[11px] px-2 py-0.5 rounded bg-primary text-white shrink-0">默认</span>
            </div>
            <div class="mt-3 flex gap-2">
              <button @click="loadTemplate(item)" class="text-xs font-bold px-3 py-1 rounded bg-surface-container-high text-on-surface">编辑</button>
              <button @click="setDefault(item.id)" class="text-xs font-bold px-3 py-1 rounded bg-primary-container text-primary">设默认</button>
              <button @click="removeTemplate(item.id)" class="text-xs font-bold px-3 py-1 rounded bg-error/10 text-error">停用</button>
            </div>
          </div>
        </div>
      </aside>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteLabelTemplate, listLabelTemplates, saveLabelTemplate, setDefaultLabelTemplate, uploadLabelTemplate } from './label/api/label'

defineOptions({ name: 'BartenderTemplate' })

const templateName = ref('面料入库标签(Bartender版)')
const templateCode = ref('')
const isDefault = ref(false)
const saving = ref(false)
const uploadedFile = ref<File | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const templates = ref<any[]>([])

const detectedVariables = computed(() => {
  if (!templateCode.value) return []
  const vars = new Set<string>()
  const collect = (regex: RegExp) => {
    let match
    while ((match = regex.exec(templateCode.value)) !== null) {
      vars.add(match[1].trim())
    }
  }
  collect(/\$\{([^}]+)\}/g)
  collect(/\{([^}]+)\}/g)
  return Array.from(vars)
})

const triggerUpload = () => {
  fileInputRef.value?.click()
}

const handleFileUpload = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  uploadedFile.value = file
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

const fetchTemplates = async () => {
  templates.value = await listLabelTemplates({ printType: 'label' })
}

const saveTemplate = async () => {
  if (!templateName.value.trim()) {
    ElMessage.warning('模板名称不能为空')
    return
  }
  if (!templateCode.value.trim()) {
    ElMessage.warning('模板代码不能为空')
    return
  }

  saving.value = true
  try {
    if (uploadedFile.value) {
      const fileForUpload = new File([templateCode.value], uploadedFile.value.name, { type: uploadedFile.value.type || 'text/plain' })
      const formData = new FormData()
      formData.append('file', fileForUpload)
      formData.append('name', templateName.value.trim())
      formData.append('printType', 'label')
      formData.append('isDefault', isDefault.value ? '1' : '0')
      await uploadLabelTemplate(formData)
    } else {
      await saveLabelTemplate({
        name: templateName.value.trim(),
        printType: 'label',
        content: templateCode.value,
        isDefault: isDefault.value ? 1 : 0
      })
    }
    ElMessage.success('模板保存成功，小程序端已可选择使用')
    uploadedFile.value = null
    await fetchTemplates()
  } finally {
    saving.value = false
  }
}

const loadTemplate = (item: any) => {
  templateName.value = item.name
  templateCode.value = item.content || ''
  isDefault.value = item.isDefault === 1
  uploadedFile.value = null
}

const setDefault = async (id: number) => {
  await setDefaultLabelTemplate(id)
  ElMessage.success('默认模板已更新')
  await fetchTemplates()
}

const removeTemplate = async (id: number) => {
  await ElMessageBox.confirm('停用后小程序端将不再显示该模板，确认继续吗？', '停用模板', {
    confirmButtonText: '确认停用',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteLabelTemplate(id)
  ElMessage.success('模板已停用')
  await fetchTemplates()
}

onMounted(fetchTemplates)
</script>

<style scoped>
textarea:focus, input:focus {
  outline: none;
}
</style>