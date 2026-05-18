<template>
  <div class="document-page bg-surface text-on-surface flex overflow-hidden font-sans">
    <aside class="w-64 bg-surface-container-lowest border-r border-outline-variant/20 flex flex-col shrink-0">
      <div class="h-16 flex items-center px-6 border-b border-outline-variant/20 shrink-0">
        <span class="material-symbols-outlined text-primary text-2xl mr-2">corporate_fare</span>
        <h1 class="text-lg font-black text-primary tracking-tight">企业文档中心</h1>
      </div>
      <div class="flex-1 overflow-y-auto p-4 space-y-6">
        <div class="space-y-1">
          <h3 class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest px-2 mb-2">当前目录</h3>
          <button class="w-full flex items-center gap-3 px-3 py-2 bg-primary/10 text-primary rounded-lg font-bold text-sm transition-colors">
            <span class="material-symbols-outlined text-[18px]">home</span>
            {{ currentFolderName }}
          </button>
        </div>
      </div>
    </aside>

    <main class="flex-1 flex flex-col min-w-0 bg-surface">
      <header class="h-16 flex items-center justify-between px-4 sm:px-6 border-b border-outline-variant/20 bg-surface-container-lowest shrink-0">
        <div class="flex items-center gap-2">
          <button @click="navigateUp" :disabled="currentParentId === 0" class="p-2 rounded-lg text-on-surface-variant hover:bg-surface-container hover:text-primary transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
            <span class="material-symbols-outlined">arrow_upward</span>
          </button>
          <button @click="promptCreateFolder" class="flex items-center gap-2 px-4 py-2 bg-surface-container border border-outline-variant/30 text-primary rounded-lg text-sm font-bold shadow-sm hover:bg-surface-container-high transition-all active:scale-95">
            <span class="material-symbols-outlined text-[18px]">create_new_folder</span>
            新建文件夹
          </button>
          <button @click="fetchDocuments(currentParentId)" class="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg text-sm font-bold shadow-sm hover:bg-primary/90 transition-all active:scale-95">
            <span class="material-symbols-outlined text-[18px]">refresh</span>
            刷新
          </button>
          <input
            v-model.trim="filters.keyword"
            class="w-56 rounded-lg border border-outline-variant/30 bg-white px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/30"
            placeholder="搜索文件名或扩展名"
          />
          <select
            v-model="filters.type"
            class="rounded-lg border border-outline-variant/30 bg-white px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/30"
          >
            <option value="">全部类型</option>
            <option value="folder">文件夹</option>
            <option value="file">文件</option>
          </select>
          <TableColumnSettings
            :columns="documentTableColumns"
            @move="moveDocumentTableColumn"
            @reset="resetDocumentTableColumns"
          />
        </div>
      </header>

      <div class="px-6 py-3 bg-surface-container-low/30 border-b border-outline-variant/10 flex items-center gap-1 text-sm shrink-0 overflow-x-auto">
        <button @click="goRoot" class="px-2 py-1 hover:bg-surface-container rounded transition-colors text-on-surface-variant hover:text-primary font-medium whitespace-nowrap flex items-center gap-1">
          <span class="material-symbols-outlined text-[16px]">home</span>
          根目录
        </button>
        <template v-for="crumb in breadcrumbs" :key="crumb.id">
          <span class="material-symbols-outlined text-on-surface-variant/40 text-[16px]">chevron_right</span>
          <button @click="navigateTo(crumb.id)" class="px-2 py-1 hover:bg-surface-container rounded transition-colors text-on-surface-variant hover:text-primary font-medium whitespace-nowrap">{{ crumb.name }}</button>
        </template>
      </div>

      <div class="flex-1 overflow-y-auto p-4 sm:p-6">
        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant/20 shadow-sm overflow-hidden min-h-full relative">
          <div v-if="loading" class="absolute inset-0 bg-white/60 backdrop-blur-sm z-10 flex items-center justify-center">
            <span class="material-symbols-outlined text-3xl text-primary animate-spin">progress_activity</span>
          </div>

          <table class="w-full text-left text-sm whitespace-nowrap">
            <thead class="bg-surface-container-low text-on-surface-variant font-bold text-xs sticky top-0 z-10 shadow-sm">
              <tr>
                <th
                  v-for="column in documentTableColumns"
                  :key="column.key"
                  class="px-4 py-3"
                  :class="[column.widthClass, column.align === 'right' ? 'text-right' : '']"
                >
                  {{ column.label }}
                </th>
              </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/10">
              <tr v-for="doc in filteredDocumentList" :key="doc.id" @dblclick="handleDoubleClick(doc)" class="hover:bg-primary/5 transition-colors group select-none cursor-default">
                <td
                  v-for="column in documentTableColumns"
                  :key="column.key"
                  class="px-4 py-3"
                  :class="documentCellClass(column.key)"
                >
                  <template v-if="column.key === 'name'">
                    <span v-if="isFolder(doc)" class="material-symbols-outlined text-3xl text-amber-400" style="font-variation-settings: 'FILL' 1;">folder</span>
                    <div v-else class="w-8 h-8 rounded flex items-center justify-center font-black text-[10px] text-white" :class="getFileIconColor(doc.fileExt)">
                      {{ (doc.fileExt || 'FILE').toUpperCase() }}
                    </div>
                    <span class="font-bold text-primary group-hover:text-primary-fixed cursor-pointer truncate max-w-[300px]">{{ doc.name }}</span>
                  </template>
                  <template v-else-if="column.key === 'createTime'">{{ formatTime(doc.createTime) }}</template>
                  <template v-else-if="column.key === 'type'">{{ isFolder(doc) ? '文件夹' : `${(doc.fileExt || 'unknown').toUpperCase()} 文件` }}</template>
                  <template v-else-if="column.key === 'size'">{{ isFolder(doc) ? '--' : formatBytes(doc.fileSize) }}</template>
                </td>
              </tr>
              <tr v-if="!loading && filteredDocumentList.length === 0">
                <td :colspan="documentTableColumns.length" class="px-4 py-16 text-center text-on-surface-variant/50">
                  <span class="material-symbols-outlined text-4xl mb-2 opacity-50">folder_open</span>
                  <p class="font-medium text-sm">当前目录为空</p>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createFolder, getBreadcrumbs, getDocumentList } from './api/document.js'
import TableColumnSettings from '@/components/TableColumnSettings.vue'
import { useLocalTableColumns } from '@/composables/useLocalTableColumns'

const defaultDocumentTableColumns = [
  { key: 'name', label: '名称', widthClass: 'w-1/2' },
  { key: 'createTime', label: '创建时间', widthClass: 'w-1/6' },
  { key: 'type', label: '类型', widthClass: 'w-1/6' },
  { key: 'size', label: '大小', widthClass: 'w-1/6', align: 'right' }
]
const {
  orderedColumns: documentTableColumns,
  moveColumn: moveDocumentTableColumn,
  resetColumns: resetDocumentTableColumns
} = useLocalTableColumns('document.list', defaultDocumentTableColumns)
const loading = ref(false)
const currentParentId = ref(0)
const documentList = ref([])
const breadcrumbs = ref([])
const filters = reactive({ keyword: '', type: '' })

const currentFolderName = computed(() => breadcrumbs.value.at(-1)?.name || '根目录')
const filteredDocumentList = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return documentList.value.filter((doc) => {
    const typeMatched = !filters.type || (filters.type === 'folder' ? isFolder(doc) : !isFolder(doc))
    const keywordMatched = !keyword
        || String(doc.name || '').toLowerCase().includes(keyword)
        || String(doc.fileExt || '').toLowerCase().includes(keyword)
    return typeMatched && keywordMatched
  })
})

const fetchDocuments = async (parentId = 0) => {
  loading.value = true
  try {
    currentParentId.value = parentId
    documentList.value = await getDocumentList(parentId)
    breadcrumbs.value = parentId > 0 ? await getBreadcrumbs(parentId) : []
  } finally {
    loading.value = false
  }
}

const isFolder = (doc) => Number(doc.type) === 0

const documentCellClass = (key) => {
  if (key === 'name') return 'flex items-center gap-3'
  if (key === 'size') return 'text-right text-on-surface-variant font-mono'
  return 'text-on-surface-variant'
}

const openDocumentUrl = (fileUrl) => {
  try {
    const targetUrl = new URL(fileUrl, window.location.origin)
    if (!['http:', 'https:'].includes(targetUrl.protocol)) {
      ElMessage.warning('文件链接格式不合法')
      return
    }
    const opened = window.open(targetUrl.href, '_blank', 'noopener,noreferrer')
    if (opened) {
      opened.opener = null
    }
  } catch (error) {
    ElMessage.warning('文件链接格式不合法')
  }
}

const handleDoubleClick = async (doc) => {
  if (isFolder(doc)) {
    await fetchDocuments(doc.id)
    return
  }
  if (doc.fileUrl) {
    openDocumentUrl(doc.fileUrl)
  } else {
    ElMessage.info('当前文件还没有可访问链接')
  }
}

const navigateUp = async () => {
  if (currentParentId.value === 0) return
  const parent = breadcrumbs.value.length >= 2 ? breadcrumbs.value[breadcrumbs.value.length - 2].id : 0
  await fetchDocuments(parent)
}

const navigateTo = async (id) => {
  await fetchDocuments(id)
}

const goRoot = async () => {
  await fetchDocuments(0)
}

const promptCreateFolder = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入文件夹名称', '新建文件夹', {
      confirmButtonText: '创建',
      cancelButtonText: '取消'
    })
    if (!value) return
    await createFolder({ parentId: currentParentId.value, name: value })
    ElMessage.success('文件夹创建成功')
    await fetchDocuments(currentParentId.value)
  } catch {
    // ignore cancel
  }
}

const formatTime = (value) => {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 19)
}

const formatBytes = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`
}

const getFileIconColor = (ext) => {
  const extLower = ext?.toLowerCase()
  const map = {
    pdf: 'bg-red-500',
    docx: 'bg-blue-500',
    doc: 'bg-blue-500',
    xlsx: 'bg-emerald-500',
    xls: 'bg-emerald-500',
    pptx: 'bg-orange-500',
    png: 'bg-violet-500',
    jpg: 'bg-violet-500',
    jpeg: 'bg-violet-500',
    zip: 'bg-slate-500',
    rar: 'bg-slate-500',
    txt: 'bg-gray-400'
  }
  return map[extLower] || 'bg-slate-400'
}

fetchDocuments(0)
</script>

<style scoped>
.document-page {
  min-height: 100%;
  height: 100%;
  min-width: 0;
}

@media (max-width: 768px) {
  .document-page {
    min-height: 100%;
    height: auto;
    flex-direction: column;
    overflow: visible;
  }

  .document-page > aside {
    width: 100%;
    max-height: 220px;
    border-right: 0;
    border-bottom: 1px solid rgb(219 217 209 / 0.6);
  }
}
</style>
