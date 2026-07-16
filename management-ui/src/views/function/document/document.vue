<template>
  <div class="document-page flex overflow-hidden bg-surface font-sans text-on-surface">
    <aside class="flex w-64 shrink-0 flex-col border-r border-outline-variant/20 bg-surface-container-lowest">
      <div class="flex h-16 shrink-0 items-center border-b border-outline-variant/20 px-6">
        <span class="material-symbols-outlined mr-2 text-2xl text-primary">corporate_fare</span>
        <h1 class="text-lg font-black tracking-tight text-primary">企业文档中心</h1>
      </div>
      <div class="flex-1 space-y-6 overflow-y-auto p-4">
        <div class="space-y-1">
          <h3 class="mb-2 px-2 text-[10px] font-bold tracking-widest text-on-surface-variant uppercase">当前目录</h3>
          <el-button
            text
            class="w-full justify-start"
            :disabled="!canBrowseDocuments"
            :class="permissionDisabledClass(!canBrowseDocuments)"
            :title="canBrowseDocuments ? '返回根目录' : breadcrumbPermissionReason"
            @click="goRoot"
          >
            <span class="material-symbols-outlined text-[18px]">home</span>
            {{ currentFolderName }}
          </el-button>
        </div>
      </div>
    </aside>

    <main class="flex min-w-0 flex-1 flex-col bg-surface">
      <header class="flex min-h-16 shrink-0 flex-wrap items-center justify-between gap-3 border-b border-outline-variant/20 bg-surface-container-lowest px-4 py-3 sm:px-6">
        <div class="flex flex-wrap items-center gap-2">
          <el-button
            circle
            :disabled="currentParentId === 0 || !canBrowseDocuments"
            :class="permissionDisabledClass(!canBrowseDocuments)"
            :title="canBrowseDocuments ? '上一级' : breadcrumbPermissionReason"
            @click="navigateUp"
          >
            <span class="material-symbols-outlined">arrow_upward</span>
          </el-button>
          <el-button
            :disabled="!canCreateFolder"
            :class="permissionDisabledClass(!canCreateFolder)"
            :title="canCreateFolder ? '新建文件夹' : '当前账号暂无新建文件夹权限'"
            @click="promptCreateFolder"
          >
            <span class="material-symbols-outlined text-[18px]">create_new_folder</span>
            新建文件夹
          </el-button>
          <el-button type="primary" :loading="loading" @click="fetchDocuments(currentParentId)">
            <span class="material-symbols-outlined text-[18px]">refresh</span>
            刷新
          </el-button>
          <el-input v-model.trim="filters.keyword" class="w-full sm:w-56" placeholder="搜索文件名或扩展名" clearable>
            <template #prefix>
              <span class="material-symbols-outlined text-[18px]">search</span>
            </template>
          </el-input>
          <el-select v-model="filters.type" class="w-full sm:w-32" placeholder="全部类型" clearable>
            <el-option label="文件夹" value="folder" />
            <el-option label="文件" value="file" />
          </el-select>
          <TableColumnSettings
            :columns="documentTableColumns"
            :export-rows="filteredDocumentList"
            :export-cell="documentExportCell"
            export-file-name="企业文档中心"
            export-sheet-name="企业文档中心"
            export-module="document"
            :export-disabled="!canExportTable"
            export-disabled-reason="当前账号暂无表格导出权限"
            @move="moveDocumentTableColumn"
            @reset="resetDocumentTableColumns"
          />
        </div>
      </header>

      <div class="flex shrink-0 items-center gap-1 overflow-x-auto border-b border-outline-variant/10 bg-surface-container-low/30 px-6 py-3 text-sm">
        <el-button
          text
          :disabled="!canBrowseDocuments"
          :class="permissionDisabledClass(!canBrowseDocuments)"
          :title="canBrowseDocuments ? '返回根目录' : breadcrumbPermissionReason"
          @click="goRoot"
        >
          <span class="material-symbols-outlined text-[16px]">home</span>
          根目录
        </el-button>
        <template v-for="crumb in breadcrumbs" :key="crumb.id">
          <span class="material-symbols-outlined text-[16px] text-on-surface-variant/40">chevron_right</span>
          <el-button
            text
            :disabled="!canBrowseDocuments"
            :class="permissionDisabledClass(!canBrowseDocuments)"
            :title="canBrowseDocuments ? `进入${crumb.name}` : breadcrumbPermissionReason"
            @click="navigateTo(crumb.id)"
          >{{ crumb.name }}</el-button>
        </template>
      </div>

      <div class="flex-1 overflow-y-auto p-4 sm:p-6">
        <DragAttachmentUpload
          class="mb-4"
          title="点击或拖拽文件上传到当前目录"
          helper-text="支持图片、PDF、Word、Excel、PPT、文本或压缩包"
          accept=".pdf,.png,.jpg,.jpeg,.webp,.doc,.docx,.xls,.xlsx,.csv,.ppt,.pptx,.txt,.zip,.rar,.7z"
          :uploading="documentUploading"
          :downloadable="false"
          :disabled="!canUploadDocument"
          disabled-reason="当前账号暂无上传文档权限"
          @select="handleDocumentUpload"
        />
        <div class="min-h-full overflow-hidden rounded-xl border border-outline-variant/20 bg-surface-container-lowest shadow-sm">
          <el-result v-if="documentError" :icon="documentError.icon" :title="documentError.title" :sub-title="documentError.message">
            <template #extra>
              <el-button type="primary" @click="retryDocuments">重试</el-button>
            </template>
          </el-result>
          <el-table
            v-else
            :data="filteredDocumentList"
            row-key="id"
            v-loading="loading"
            class="w-full"
            @row-dblclick="handleDoubleClick"
          >
            <el-table-column
              v-for="column in documentTableColumns"
              :key="column.key"
              :label="column.label"
              :min-width="column.key === 'name' ? 280 : 140"
              :align="column.align || 'left'"
              :class-name="documentCellClass(column.key)"
              :show-overflow-tooltip="column.key === 'name'"
            >
              <template #default="{ row: doc }">
                <template v-if="column.key === 'name'">
                  <div class="flex min-w-0 items-center gap-3">
                    <span v-if="isFolder(doc)" class="material-symbols-outlined text-3xl text-amber-400" style="font-variation-settings: 'FILL' 1;">folder</span>
                    <div v-else class="flex h-8 w-8 items-center justify-center rounded text-[10px] font-black text-white" :class="getFileIconColor(doc.fileExt)">
                      {{ (doc.fileExt || 'FILE').toUpperCase() }}
                    </div>
                    <span
                      class="truncate font-bold"
                      :class="isFolder(doc) && !canBrowseDocuments ? 'cursor-not-allowed text-[var(--ys-disabled-text)] grayscale' : 'text-primary'"
                      :title="isFolder(doc) && !canBrowseDocuments ? breadcrumbPermissionReason : doc.name"
                    >{{ doc.name }}</span>
                  </div>
                </template>
                <template v-else-if="column.key === 'createTime'">{{ formatTime(doc.createTime) }}</template>
                <template v-else-if="column.key === 'type'">{{ isFolder(doc) ? '文件夹' : `${(doc.fileExt || 'unknown').toUpperCase()} 文件` }}</template>
                <template v-else-if="column.key === 'size'">{{ isFolder(doc) ? '--' : formatBytes(doc.fileSize) }}</template>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty v-if="!loading" :description="documentEmptyDescription" />
            </template>
          </el-table>
        </div>
      </div>
    </main>

    <el-dialog v-model="folderDialogVisible" title="新建文件夹" width="min(420px, calc(100vw - 2rem))" destroy-on-close>
      <el-form @submit.prevent="createFolderFromDialog">
        <el-form-item label="文件夹名称">
          <el-input v-model.trim="folderName" placeholder="请输入文件夹名称" autofocus @keyup.enter="createFolderFromDialog" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button native-type="button" :disabled="creatingFolder" @click="folderDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          native-type="button"
          :loading="creatingFolder"
          :disabled="creatingFolder || !canCreateFolder"
          :class="permissionDisabledClass(!canCreateFolder)"
          :title="canCreateFolder ? '创建文件夹' : '当前账号暂无新建文件夹权限'"
          @click="createFolderFromDialog"
        >
          创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import {
  ElButton,
  ElDialog,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElResult,
  ElSelect,
  ElTable,
  ElTableColumn
} from 'element-plus'
import { createFolder, getBreadcrumbs, getDocumentList, uploadDocumentFile } from './api/document.js'
import DragAttachmentUpload from '@/components/DragAttachmentUpload.vue'
import TableColumnSettings from '@/components/TableColumnSettings.vue'
import { useLocalTableColumns } from '@/composables/useLocalTableColumns'
import { useUserStore } from '@/stores/user'
import { createDocumentNavigator } from './documentNavigation'

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
const userStore = useUserStore()
const loading = ref(false)
const documentUploading = ref(false)
const creatingFolder = ref(false)
const folderDialogVisible = ref(false)
const folderName = ref('')
const currentParentId = ref(0)
const documentList = ref([])
const documentError = ref(null)
const breadcrumbs = ref([])
const filters = reactive({ keyword: '', type: '' })
let documentRequestId = 0

const canCreateFolder = computed(() => userStore.hasPermission('document:folder:create'))
const canUploadDocument = computed(() => userStore.hasPermission('document:file:upload'))
const canExportTable = computed(() => userStore.hasPermission('document:export'))
const canBrowseDocuments = computed(() => userStore.hasPermission('document:list'))
const breadcrumbPermissionReason = '当前账号暂无文档目录导航权限'
const currentFolderName = computed(() => breadcrumbs.value.at(-1)?.name || '根目录')
const hasDocumentFilters = computed(() => Boolean(filters.keyword || filters.type))
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
const documentEmptyDescription = computed(() => {
  if (documentList.value.length > 0 && hasDocumentFilters.value) {
    return '没有符合筛选条件的文档'
  }
  return '当前目录为空'
})

const fetchDocuments = async (parentId = 0) => {
  const requestId = ++documentRequestId
  loading.value = true
  documentError.value = null
  documentList.value = []
  breadcrumbs.value = []
  currentParentId.value = parentId
  try {
    const nextDocuments = await getDocumentList(parentId)
    const nextBreadcrumbs = parentId > 0 ? await getBreadcrumbs(parentId) : []
    if (requestId !== documentRequestId) return
    documentList.value = Array.isArray(nextDocuments) ? nextDocuments : []
    breadcrumbs.value = Array.isArray(nextBreadcrumbs) ? nextBreadcrumbs : []
  } catch (error) {
    if (requestId !== documentRequestId) return
    documentError.value = resolveDocumentLoadError(error)
  } finally {
    if (requestId === documentRequestId) {
      loading.value = false
    }
  }
}

const documentNavigator = createDocumentNavigator({
  canNavigate: () => canBrowseDocuments.value,
  fetchDocuments
})

const isFolder = (doc) => Number(doc.type) === 0

const documentCellClass = (key) => {
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
    await documentNavigator.openFolder(doc.id)
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
  await documentNavigator.navigateUp(parent)
}

const navigateTo = async (id) => {
  await documentNavigator.navigateTo(id)
}

const goRoot = async () => {
  await documentNavigator.goRoot()
}

const promptCreateFolder = () => {
  if (!canCreateFolder.value) return
  folderName.value = ''
  folderDialogVisible.value = true
}

const createFolderFromDialog = async () => {
  if (!canCreateFolder.value) return
  const name = folderName.value.trim()
  if (!name || creatingFolder.value) return
  creatingFolder.value = true
  try {
    await createFolder({ parentId: currentParentId.value, name })
    ElMessage.success('文件夹创建成功')
    folderDialogVisible.value = false
    await fetchDocuments(currentParentId.value)
  } finally {
    creatingFolder.value = false
  }
}

const handleDocumentUpload = async (file) => {
  if (!canUploadDocument.value) return
  if (!file) return
  const maxBytes = 20 * 1024 * 1024
  if (file.size > maxBytes) {
    ElMessage.warning('文档文件不能超过 20MB')
    return
  }
  const formData = new FormData()
  formData.append('file', file)
  formData.append('parentId', String(currentParentId.value || 0))
  documentUploading.value = true
  try {
    await uploadDocumentFile(formData)
    ElMessage.success('文件上传成功')
    await fetchDocuments(currentParentId.value)
  } finally {
    documentUploading.value = false
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

const documentExportCell = (document, column) => {
  if (column.key === 'name') return document?.name || ''
  if (column.key === 'createTime') return formatTime(document?.createTime)
  if (column.key === 'type') return isFolder(document) ? '文件夹' : `${(document?.fileExt || 'unknown').toUpperCase()} 文件`
  if (column.key === 'size') return isFolder(document) ? '--' : formatBytes(document?.fileSize)
  return ''
}

const retryDocuments = () => fetchDocuments(currentParentId.value)

function resolveDocumentLoadError(error) {
  const status = Number(error?.response?.status || error?.status || error?.code || 0)
  if (status === 401) {
    return {
      icon: 'warning',
      title: '登录状态已失效',
      message: '请重新登录后再加载文档目录。'
    }
  }
  if (status === 403) {
    return {
      icon: 'warning',
      title: '暂无目录访问权限',
      message: '当前账号缺少文档列表或面包屑权限，请联系管理员。'
    }
  }
  if (status >= 500) {
    return {
      icon: 'error',
      title: '文档服务暂时不可用',
      message: '服务器处理失败，请稍后重试。'
    }
  }
  return {
    icon: 'error',
    title: '文档目录加载失败',
    message: '网络连接异常，请检查网络后重试。'
  }
}

function permissionDisabledClass(disabled) {
  return disabled ? 'cursor-not-allowed grayscale' : ''
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
    ppt: 'bg-orange-500',
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
