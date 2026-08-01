<template>
  <div class="function-page-shell h-full min-h-0 font-sans">
    <section
      class="document-explorer function-page-container"
      @click="closeContextMenu"
      @dragenter.prevent="documentDragActive = true"
      @dragover.prevent="documentDragActive = true"
      @dragleave="handleDocumentDragLeave"
      @drop.prevent="handleDocumentDrop"
    >
      <aside class="document-navigation-pane">
        <div class="document-navigation-brand">
          <span class="document-navigation-logo material-symbols-outlined">folder_managed</span>
          <div class="min-w-0">
            <h1>企业文档</h1>
            <p>共享文件管理器</p>
          </div>
        </div>

        <nav class="document-quick-access" aria-label="文档快捷访问">
          <p class="document-pane-title">快捷访问</p>
          <button
            type="button"
            class="document-nav-entry"
            :class="{ 'is-active': currentParentId === 0 }"
            :disabled="!canBrowseDocuments"
            :title="canBrowseDocuments ? '返回根目录' : breadcrumbPermissionReason"
            @click="goRoot"
          >
            <span class="material-symbols-outlined">home</span>
            <span>根目录</span>
          </button>
        </nav>

        <div class="document-tree-section">
          <div class="document-tree-heading">
            <p class="document-pane-title">文件夹</p>
            <el-button
              text
              circle
              size="small"
              :loading="folderTreeLoading"
              :disabled="!canBrowseDocuments"
              title="刷新目录树"
              @click="loadFolderTree"
            >
              <span class="material-symbols-outlined">refresh</span>
            </el-button>
          </div>
          <el-tree
            v-if="folderTree.length"
            class="document-folder-tree"
            :data="folderTree"
            node-key="id"
            :current-node-key="currentParentId || undefined"
            :default-expanded-keys="expandedFolderIds"
            :expand-on-click-node="false"
            highlight-current
            @node-click="handleFolderTreeClick"
          >
            <template #default="{ data }">
              <span class="document-tree-node" :title="data.label">
                <span class="material-symbols-outlined">folder</span>
                <span>{{ data.label }}</span>
              </span>
            </template>
          </el-tree>
          <div v-else-if="!folderTreeLoading" class="document-tree-empty">
            <span class="material-symbols-outlined">folder_off</span>
            <span>暂无文件夹</span>
          </div>
        </div>

        <div class="document-pane-footer">
          <span class="material-symbols-outlined">corporate_fare</span>
          <span>企业共享空间</span>
        </div>
      </aside>

      <main class="document-workspace">
        <header class="document-topbar">
          <div class="document-navigation-actions">
            <el-button
              circle
              :disabled="!canGoBack || !canBrowseDocuments"
              :class="permissionDisabledClass(!canBrowseDocuments)"
              :title="canBrowseDocuments ? '后退' : breadcrumbPermissionReason"
              @click="navigateHistory(-1)"
            >
              <span class="material-symbols-outlined">arrow_back</span>
            </el-button>
            <el-button
              circle
              :disabled="!canGoForward || !canBrowseDocuments"
              :class="permissionDisabledClass(!canBrowseDocuments)"
              :title="canBrowseDocuments ? '前进' : breadcrumbPermissionReason"
              @click="navigateHistory(1)"
            >
              <span class="material-symbols-outlined">arrow_forward</span>
            </el-button>
            <el-button
              circle
              :disabled="currentParentId === 0 || !canBrowseDocuments"
              :class="permissionDisabledClass(!canBrowseDocuments)"
              :title="canBrowseDocuments ? '上一级' : breadcrumbPermissionReason"
              @click="navigateUp"
            >
              <span class="material-symbols-outlined">arrow_upward</span>
            </el-button>
            <el-button circle :loading="loading" title="刷新" @click="refreshExplorer">
              <span class="material-symbols-outlined">refresh</span>
            </el-button>
          </div>

          <div class="document-address-bar" aria-label="当前文档路径">
            <button
              type="button"
              :disabled="!canBrowseDocuments"
              :title="canBrowseDocuments ? '返回根目录' : breadcrumbPermissionReason"
              @click="goRoot"
            >
              <span class="material-symbols-outlined">business</span>
              <span>企业文档</span>
            </button>
            <template v-for="crumb in breadcrumbs" :key="crumb.id">
              <span class="material-symbols-outlined document-address-separator">chevron_right</span>
              <button
                type="button"
                :disabled="!canBrowseDocuments"
                :title="canBrowseDocuments ? `进入${crumb.name}` : breadcrumbPermissionReason"
                @click="navigateTo(crumb.id)"
              >{{ crumb.name }}</button>
            </template>
          </div>

          <el-input
            v-model.trim="filters.keyword"
            class="document-search"
            :placeholder="`在“${currentFolderName}”中搜索`"
            clearable
          >
            <template #prefix>
              <span class="material-symbols-outlined">search</span>
            </template>
          </el-input>
        </header>

        <div class="document-command-bar">
          <el-button
            type="primary"
            :disabled="!canCreateFolder"
            :class="permissionDisabledClass(!canCreateFolder)"
            :title="canCreateFolder ? '新建文件夹' : '当前账号暂无新建文件夹权限'"
            @click="promptCreateFolder"
          >
            <span class="material-symbols-outlined">create_new_folder</span>
            新建文件夹
          </el-button>
          <el-button
            :loading="documentUploading"
            :disabled="!canUploadDocument"
            :class="permissionDisabledClass(!canUploadDocument)"
            :title="canUploadDocument ? '上传文件到当前目录' : '当前账号暂无上传文档权限'"
            @click="openDocumentPicker"
          >
            <span class="material-symbols-outlined">upload_file</span>
            上传
          </el-button>
          <span class="document-command-divider" aria-hidden="true"></span>
          <el-button
            class="document-selection-command"
            :disabled="!selectedDocument || !canOpenSelected"
            :title="selectedActionTitle('打开')"
            @click="openSelectedDocument"
          >
            <span class="material-symbols-outlined">open_in_new</span>
            <span class="document-selection-command-label">打开</span>
          </el-button>
          <el-button
            class="document-selection-command"
            :disabled="!selectedDocument || !canRenameDocument"
            :title="selectedActionTitle('重命名', canRenameDocument, '当前账号暂无重命名文档权限')"
            @click="openRenameDialog(selectedDocument)"
          >
            <span class="material-symbols-outlined">edit</span>
            <span class="document-selection-command-label">重命名</span>
          </el-button>
          <el-button
            class="document-selection-command"
            :disabled="!selectedDocument || !canMoveDocument"
            :title="selectedActionTitle('移动', canMoveDocument, '当前账号暂无移动文档权限')"
            @click="openMoveDialog(selectedDocument)"
          >
            <span class="material-symbols-outlined">drive_file_move</span>
            <span class="document-selection-command-label">移动</span>
          </el-button>
          <el-button
            class="document-delete-command document-selection-command"
            :disabled="!selectedDocument || !canDeleteDocument"
            :title="selectedActionTitle('删除', canDeleteDocument, '当前账号暂无删除文档权限')"
            @click="confirmDeleteDocument(selectedDocument)"
          >
            <span class="material-symbols-outlined">delete</span>
            <span class="document-selection-command-label">删除</span>
          </el-button>

          <div class="document-command-spacer"></div>
          <el-select v-model="filters.type" class="document-type-filter" placeholder="全部类型" clearable>
            <el-option label="文件夹" value="folder" />
            <el-option label="文件" value="file" />
          </el-select>
          <div class="document-view-switch" aria-label="视图方式">
            <button type="button" :class="{ 'is-active': viewMode === 'list' }" title="详细信息视图" @click="setViewMode('list')">
              <span class="material-symbols-outlined">view_list</span>
            </button>
            <button type="button" :class="{ 'is-active': viewMode === 'grid' }" title="大图标视图" @click="setViewMode('grid')">
              <span class="material-symbols-outlined">grid_view</span>
            </button>
          </div>
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

        <input
          ref="documentFileInputRef"
          class="document-file-input"
          type="file"
          multiple
          :accept="documentAccept"
          :disabled="!canUploadDocument || documentUploading"
          @change="handleDocumentFileInput"
        >

        <section class="document-content" :class="{ 'is-dragging': documentDragActive }">
          <div v-if="documentDragActive" class="document-drop-overlay">
            <span class="material-symbols-outlined">upload_file</span>
            <strong>释放文件以上传到“{{ currentFolderName }}”</strong>
            <span>支持单个不超过 200MB，可一次选择多个文件</span>
          </div>

          <div class="document-content-heading">
            <div>
              <h2>{{ currentFolderName }}</h2>
              <p>{{ folderCount }} 个文件夹，{{ fileCount }} 个文件</p>
            </div>
            <span v-if="hasDocumentFilters" class="document-filter-summary">
              已筛选 {{ filteredDocumentList.length }} / {{ documentList.length }} 项
            </span>
          </div>

          <el-result
            v-if="documentError"
            :icon="documentError.icon"
            :title="documentError.title"
            :sub-title="documentError.message"
          >
            <template #extra>
              <el-button type="primary" @click="retryDocuments">重试</el-button>
            </template>
          </el-result>

          <div v-else-if="viewMode === 'list'" class="document-list-view">
            <el-table
              :data="filteredDocumentList"
              row-key="id"
              v-loading="loading"
              class="w-full"
              highlight-current-row
              :current-row-key="selectedDocument?.id"
              @current-change="selectDocument"
              @row-dblclick="handleDoubleClick"
              @row-contextmenu="handleTableContextMenu"
            >
              <el-table-column
                v-for="column in documentTableColumns"
                :key="column.key"
                :label="column.label"
                :min-width="column.key === 'name' ? 300 : 140"
                :align="column.align || 'left'"
                :class-name="documentCellClass(column.key)"
                :show-overflow-tooltip="column.key === 'name'"
              >
                <template #default="{ row: doc }">
                  <template v-if="column.key === 'name'">
                    <div class="document-name-cell">
                      <span v-if="isFolder(doc)" class="document-folder-icon material-symbols-outlined">folder</span>
                      <span v-else class="document-file-icon" :class="getFileIconColor(doc.fileExt)">
                        {{ (doc.fileExt || 'FILE').toUpperCase() }}
                      </span>
                      <span
                        class="document-name-text"
                        :class="isFolder(doc) && !canBrowseDocuments ? 'is-disabled' : ''"
                        :title="isFolder(doc) && !canBrowseDocuments ? breadcrumbPermissionReason : doc.name"
                      >{{ doc.name }}</span>
                    </div>
                  </template>
                  <template v-else-if="column.key === 'createTime'">{{ formatTime(doc.createTime) }}</template>
                  <template v-else-if="column.key === 'type'">{{ documentTypeLabel(doc) }}</template>
                  <template v-else-if="column.key === 'size'">{{ isFolder(doc) ? '--' : formatBytes(doc.fileSize) }}</template>
                </template>
              </el-table-column>
              <el-table-column
                v-if="canRenameDocument || canMoveDocument || canDeleteDocument"
                label="操作"
                class-name="document-operation-column"
                label-class-name="document-operation-column"
                fixed="right"
                width="188"
                align="center"
              >
                <template #default="{ row: doc }">
                  <el-button v-if="canRenameDocument" link type="primary" :disabled="documentActionLoading" @click.stop="openRenameDialog(doc)">
                    重命名
                  </el-button>
                  <el-button v-if="canMoveDocument" link type="primary" :disabled="documentActionLoading" @click.stop="openMoveDialog(doc)">
                    移动
                  </el-button>
                  <el-button v-if="canDeleteDocument" link type="danger" :disabled="documentActionLoading" @click.stop="confirmDeleteDocument(doc)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
              <template #empty>
                <el-empty v-if="!loading" :description="documentEmptyDescription" />
              </template>
            </el-table>
          </div>

          <div v-else v-loading="loading" class="document-grid-view">
            <button
              v-for="doc in filteredDocumentList"
              :key="doc.id"
              type="button"
              class="document-grid-item"
              :class="{ 'is-selected': selectedDocument?.id === doc.id }"
              :title="doc.name"
              @click.stop="selectDocument(doc)"
              @dblclick.stop="handleDoubleClick(doc)"
              @contextmenu.prevent.stop="showContextMenu(doc, $event)"
            >
              <span v-if="isFolder(doc)" class="document-grid-folder material-symbols-outlined">folder</span>
              <span v-else class="document-grid-file" :class="getFileIconColor(doc.fileExt)">
                <span class="material-symbols-outlined">description</span>
                <small>{{ (doc.fileExt || 'FILE').toUpperCase() }}</small>
              </span>
              <span class="document-grid-name">{{ doc.name }}</span>
              <span class="document-grid-meta">{{ isFolder(doc) ? '文件夹' : formatBytes(doc.fileSize) }}</span>
            </button>
            <el-empty v-if="!loading && !filteredDocumentList.length" :description="documentEmptyDescription" />
          </div>
        </section>

        <footer class="document-statusbar">
          <span>{{ filteredDocumentList.length }} 个项目</span>
          <span v-if="selectedDocument">已选择 1 个项目 · {{ selectedDocument.name }}</span>
          <span v-else>双击打开，右键查看更多操作</span>
        </footer>
      </main>

      <div
        v-if="contextMenu.visible"
        class="document-context-menu"
        :style="{ left: `${contextMenu.x}px`, top: `${contextMenu.y}px` }"
        role="menu"
        @click.stop
        @contextmenu.prevent
      >
        <button type="button" :disabled="!canOpenContextDocument" @click="runContextAction(openContextDocument)">
          <span class="material-symbols-outlined">open_in_new</span>
          <span>打开</span>
        </button>
        <button
          v-if="contextMenu.document && !isFolder(contextMenu.document)"
          type="button"
          :disabled="!canDownloadDocument"
          @click="runContextAction(() => openDocumentFile(contextMenu.document))"
        >
          <span class="material-symbols-outlined">download</span>
          <span>下载</span>
        </button>
        <span class="document-context-divider"></span>
        <button type="button" :disabled="!canRenameDocument" @click="runContextAction(() => openRenameDialog(contextMenu.document))">
          <span class="material-symbols-outlined">edit</span>
          <span>重命名</span>
        </button>
        <button type="button" :disabled="!canMoveDocument" @click="runContextAction(() => openMoveDialog(contextMenu.document))">
          <span class="material-symbols-outlined">drive_file_move</span>
          <span>移动到</span>
        </button>
        <button class="is-danger" type="button" :disabled="!canDeleteDocument" @click="runContextAction(() => confirmDeleteDocument(contextMenu.document))">
          <span class="material-symbols-outlined">delete</span>
          <span>删除</span>
        </button>
      </div>

      <el-dialog v-model="folderDialogVisible" title="新建文件夹" width="min(420px, calc(100vw - 2rem))" destroy-on-close>
        <el-form label-position="top" @submit.prevent="createFolderFromDialog">
          <el-form-item label="文件夹名称">
            <el-input v-model.trim="folderName" placeholder="请输入文件夹名称" autofocus @keyup.enter="createFolderFromDialog" />
          </el-form-item>
          <p class="document-dialog-location">位置：{{ currentPathLabel }}</p>
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
          >创建</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="renameDialogVisible" title="重命名" width="min(420px, calc(100vw - 2rem))" destroy-on-close>
        <el-form label-position="top" @submit.prevent="confirmRenameDocument">
          <el-form-item label="新名称">
            <el-input v-model.trim="renameValue" placeholder="请输入新名称" autofocus @keyup.enter="confirmRenameDocument" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button :disabled="documentActionLoading" @click="renameDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="documentActionLoading" :disabled="!renameValue.trim()" @click="confirmRenameDocument">保存</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="moveDialogVisible" title="移动到" width="min(480px, calc(100vw - 2rem))" destroy-on-close>
        <el-form label-position="top" @submit.prevent="confirmMoveDocument">
          <el-form-item label="文档">
            <el-input :model-value="movingDocument?.name || ''" disabled />
          </el-form-item>
          <el-form-item label="目标文件夹">
            <el-select v-model="moveTargetParentId" class="w-full" placeholder="请选择目标文件夹" filterable>
              <el-option v-for="folder in moveFolderOptions" :key="folder.value" :label="folder.label" :value="folder.value" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button :disabled="documentActionLoading" @click="moveDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="documentActionLoading" :disabled="moveTargetParentId === null" @click="confirmMoveDocument">
            移动
          </el-button>
        </template>
      </el-dialog>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import {
  ElButton,
  ElDialog,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElMessageBox,
  ElOption,
  ElResult,
  ElSelect,
  ElTable,
  ElTableColumn,
  ElTree
} from 'element-plus'
import {
  completeChunkedDocumentUpload,
  createFolder,
  deleteDocument,
  downloadDocumentFile,
  getBreadcrumbs,
  getDocumentFolders,
  getDocumentList,
  moveDocument,
  renameDocument,
  uploadDocumentFile
} from './api/document.js'
import { buildDocumentFolderTree, pushExplorerLocation, stepExplorerLocation } from './documentExplorer.js'
import { createDocumentNavigator } from './documentNavigation.js'
import TableColumnSettings from '@/components/TableColumnSettings.vue'
import { useLocalTableColumns } from '@/composables/useLocalTableColumns'
import { useUserStore } from '@/stores/user'
import { uploadAttachmentWithChunks } from '@/utils/chunkedAttachmentUpload.js'

const documentAccept = '.pdf,.png,.jpg,.jpeg,.webp,.doc,.docx,.xls,.xlsx,.csv,.ppt,.pptx,.txt,.zip,.rar,.7z,.mp4,.mov,.m4v,.avi,.mkv,.webm,.3gp'
const defaultDocumentTableColumns = [
  { key: 'name', label: '名称', widthClass: 'w-1/2' },
  { key: 'createTime', label: '修改日期', widthClass: 'w-1/6' },
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
const folderTreeLoading = ref(false)
const documentUploading = ref(false)
const documentDragActive = ref(false)
const creatingFolder = ref(false)
const documentActionLoading = ref(false)
const folderDialogVisible = ref(false)
const renameDialogVisible = ref(false)
const moveDialogVisible = ref(false)
const folderName = ref('')
const renameValue = ref('')
const renamingDocument = ref(null)
const movingDocument = ref(null)
const moveTargetParentId = ref(null)
const moveFolderOptions = ref([])
const currentParentId = ref(0)
const documentList = ref([])
const documentError = ref(null)
const breadcrumbs = ref([])
const folderTree = ref([])
const selectedDocument = ref(null)
const documentFileInputRef = ref(null)
const viewMode = ref('list')
const navigationHistory = ref([0])
const navigationHistoryIndex = ref(0)
const filters = reactive({ keyword: '', type: '' })
const contextMenu = reactive({ visible: false, x: 0, y: 0, document: null })
let documentRequestId = 0
let folderTreeRequestId = 0

const canCreateFolder = computed(() => userStore.hasPermission('document:folder:create'))
const canUploadDocument = computed(() => userStore.hasPermission('document:file:upload'))
const canDownloadDocument = computed(() => userStore.hasPermission('document:file:download'))
const canRenameDocument = computed(() => userStore.hasPermission('document:rename'))
const canExportTable = computed(() => userStore.hasPermission('document:export'))
const canBrowseDocuments = computed(() => userStore.hasPermission('document:list'))
const canMoveDocument = computed(() => userStore.hasPermission('document:move'))
const canDeleteDocument = computed(() => userStore.hasPermission('document:delete'))
const breadcrumbPermissionReason = '当前账号暂无文档目录导航权限'
const currentFolderName = computed(() => breadcrumbs.value.at(-1)?.name || '根目录')
const currentPathLabel = computed(() => ['企业文档', ...breadcrumbs.value.map((crumb) => crumb.name)].join(' > '))
const expandedFolderIds = computed(() => breadcrumbs.value.map((crumb) => Number(crumb.id)))
const canGoBack = computed(() => navigationHistoryIndex.value > 0)
const canGoForward = computed(() => navigationHistoryIndex.value < navigationHistory.value.length - 1)
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
const folderCount = computed(() => documentList.value.filter(isFolder).length)
const fileCount = computed(() => documentList.value.length - folderCount.value)
const documentEmptyDescription = computed(() => {
  if (documentList.value.length > 0 && hasDocumentFilters.value) return '没有符合筛选条件的文档'
  return '当前目录为空'
})
const canOpenSelected = computed(() => {
  if (!selectedDocument.value) return false
  return isFolder(selectedDocument.value) ? canBrowseDocuments.value : canDownloadDocument.value
})
const canOpenContextDocument = computed(() => {
  if (!contextMenu.document) return false
  return isFolder(contextMenu.document) ? canBrowseDocuments.value : canDownloadDocument.value
})

const fetchDocuments = async (parentId = 0, options = {}) => {
  const requestId = ++documentRequestId
  const targetId = Number(parentId || 0)
  loading.value = true
  documentError.value = null
  documentList.value = []
  breadcrumbs.value = []
  selectedDocument.value = null
  currentParentId.value = targetId
  closeContextMenu()
  try {
    const nextDocuments = await getDocumentList(targetId)
    const nextBreadcrumbs = targetId > 0 ? await getBreadcrumbs(targetId) : []
    if (requestId !== documentRequestId) return false
    documentList.value = Array.isArray(nextDocuments) ? nextDocuments : []
    breadcrumbs.value = Array.isArray(nextBreadcrumbs) ? nextBreadcrumbs : []
    if (options.recordHistory !== false) {
      const nextLocation = pushExplorerLocation(navigationHistory.value, navigationHistoryIndex.value, targetId)
      navigationHistory.value = nextLocation.history
      navigationHistoryIndex.value = nextLocation.index
    }
    return true
  } catch (error) {
    if (requestId !== documentRequestId) return false
    documentError.value = resolveDocumentLoadError(error)
    return false
  } finally {
    if (requestId === documentRequestId) loading.value = false
  }
}

const loadFolderTree = async () => {
  if (!canBrowseDocuments.value) return
  const requestId = ++folderTreeRequestId
  folderTreeLoading.value = true
  try {
    const folders = await getDocumentFolders()
    if (requestId !== folderTreeRequestId) return
    folderTree.value = buildDocumentFolderTree(Array.isArray(folders) ? folders : [])
  } catch {
    if (requestId === folderTreeRequestId) folderTree.value = []
  } finally {
    if (requestId === folderTreeRequestId) folderTreeLoading.value = false
  }
}

const documentNavigator = createDocumentNavigator({
  canNavigate: () => canBrowseDocuments.value,
  fetchDocuments
})

const isFolder = (doc) => Number(doc?.type) === 0

const selectDocument = (document) => {
  selectedDocument.value = document || null
}

const openDocumentFile = async (doc) => {
  if (!doc || !canDownloadDocument.value) return
  try {
    const blob = await downloadDocumentFile(doc.id)
    const objectUrl = URL.createObjectURL(blob)
    const link = window.document.createElement('a')
    link.href = objectUrl
    link.download = doc.originalName || doc.name || 'document'
    window.document.body.appendChild(link)
    link.click()
    window.document.body.removeChild(link)
    URL.revokeObjectURL(objectUrl)
  } catch {
    ElMessage.error('文件下载失败，请稍后重试')
  }
}

const handleDoubleClick = async (document) => {
  selectDocument(document)
  if (isFolder(document)) {
    await documentNavigator.openFolder(document.id)
    return
  }
  if (!canDownloadDocument.value) {
    ElMessage.warning('当前账号暂无下载文档权限')
    return
  }
  if (document.fileUrl) await openDocumentFile(document)
  else ElMessage.info('当前文件还没有可访问链接')
}

const openSelectedDocument = () => selectedDocument.value && handleDoubleClick(selectedDocument.value)
const openContextDocument = () => contextMenu.document && handleDoubleClick(contextMenu.document)

const navigateUp = async () => {
  if (currentParentId.value === 0) return
  const parent = breadcrumbs.value.length >= 2 ? breadcrumbs.value[breadcrumbs.value.length - 2].id : 0
  await documentNavigator.navigateUp(parent)
}

const navigateTo = async (id) => documentNavigator.navigateTo(id)
const goRoot = async () => documentNavigator.goRoot()

const navigateHistory = async (direction) => {
  if (!canBrowseDocuments.value) return
  const next = stepExplorerLocation(navigationHistory.value, navigationHistoryIndex.value, direction)
  if (!next.changed) return
  if (await fetchDocuments(next.targetId, { recordHistory: false })) navigationHistoryIndex.value = next.index
}

const handleFolderTreeClick = async (node) => {
  if (Number(node?.id) === currentParentId.value) return
  await navigateTo(node.id)
}

const refreshExplorer = async () => {
  await Promise.all([fetchDocuments(currentParentId.value, { recordHistory: false }), loadFolderTree()])
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
    await Promise.all([fetchDocuments(currentParentId.value, { recordHistory: false }), loadFolderTree()])
  } finally {
    creatingFolder.value = false
  }
}

const openDocumentPicker = () => {
  if (!canUploadDocument.value || documentUploading.value) return
  documentFileInputRef.value?.click()
}

const handleDocumentFileInput = async (event) => {
  const files = [...(event.target.files || [])]
  event.target.value = ''
  await uploadDocumentFiles(files)
}

const handleDocumentUpload = async (file, uploadParentId) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('parentId', String(uploadParentId))
  await uploadAttachmentWithChunks(file, () => uploadDocumentFile(formData), 'document', {
    complete: (uploadId) => completeChunkedDocumentUpload(uploadId, uploadParentId)
  })
}

const uploadDocumentFiles = async (files) => {
  if (!canUploadDocument.value || documentUploading.value) return
  const selectedFiles = files.filter(Boolean)
  if (!selectedFiles.length) return
  const maxBytes = 200 * 1024 * 1024
  const oversized = selectedFiles.filter((file) => file.size > maxBytes)
  const acceptedFiles = selectedFiles.filter((file) => file.size <= maxBytes)
  if (oversized.length) ElMessage.warning(`已跳过 ${oversized.length} 个超过 200MB 的文件`)
  if (!acceptedFiles.length) return

  const uploadParentId = currentParentId.value || 0
  let successCount = 0
  documentUploading.value = true
  try {
    for (const file of acceptedFiles) {
      try {
        await handleDocumentUpload(file, uploadParentId)
        successCount += 1
      } catch {
        ElMessage.error(`“${file.name}”上传失败`)
      }
    }
    if (successCount) {
      ElMessage.success(`已上传 ${successCount} 个文件`)
      await fetchDocuments(currentParentId.value, { recordHistory: false })
    }
  } finally {
    documentUploading.value = false
  }
}

const handleDocumentDrop = async (event) => {
  documentDragActive.value = false
  if (!canUploadDocument.value) {
    ElMessage.warning('当前账号暂无上传文档权限')
    return
  }
  await uploadDocumentFiles([...(event.dataTransfer?.files || [])])
}

const handleDocumentDragLeave = (event) => {
  if (!event.relatedTarget || !event.currentTarget.contains(event.relatedTarget)) documentDragActive.value = false
}

const openRenameDialog = (document) => {
  if (!document || !canRenameDocument.value || documentActionLoading.value) return
  closeContextMenu()
  selectDocument(document)
  renamingDocument.value = document
  renameValue.value = document.name || ''
  renameDialogVisible.value = true
}

const confirmRenameDocument = async () => {
  const document = renamingDocument.value
  const name = renameValue.value.trim()
  if (!document || !name || !canRenameDocument.value || documentActionLoading.value) return
  if (name === document.name) {
    renameDialogVisible.value = false
    return
  }
  documentActionLoading.value = true
  try {
    await renameDocument(document.id, name)
    ElMessage.success('重命名成功')
    renameDialogVisible.value = false
    await Promise.all([fetchDocuments(currentParentId.value, { recordHistory: false }), isFolder(document) ? loadFolderTree() : Promise.resolve()])
  } finally {
    documentActionLoading.value = false
  }
}

const openMoveDialog = async (document) => {
  if (!document || !canMoveDocument.value || documentActionLoading.value) return
  closeContextMenu()
  selectDocument(document)
  documentActionLoading.value = true
  try {
    const folders = await getDocumentFolders()
    movingDocument.value = document
    moveFolderOptions.value = buildMoveFolderOptions(Array.isArray(folders) ? folders : [], document)
    moveTargetParentId.value = Number(document.parentId || 0)
    moveDialogVisible.value = true
  } finally {
    documentActionLoading.value = false
  }
}

const confirmMoveDocument = async () => {
  if (!canMoveDocument.value || !movingDocument.value || moveTargetParentId.value === null) return
  if (Number(movingDocument.value.parentId || 0) === Number(moveTargetParentId.value)) {
    ElMessage.info('文档已在该文件夹中')
    return
  }
  documentActionLoading.value = true
  try {
    await moveDocument(movingDocument.value.id, moveTargetParentId.value)
    ElMessage.success('文档移动成功')
    moveDialogVisible.value = false
    await Promise.all([fetchDocuments(currentParentId.value, { recordHistory: false }), loadFolderTree()])
  } finally {
    documentActionLoading.value = false
  }
}

const confirmDeleteDocument = async (document) => {
  if (!document || !canDeleteDocument.value || documentActionLoading.value) return
  closeContextMenu()
  try {
    await ElMessageBox.confirm(
      `确定删除“${document.name}”吗？${isFolder(document) ? '非空文件夹不能删除。' : '删除后无法恢复。'}`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
  } catch {
    return
  }
  documentActionLoading.value = true
  try {
    await deleteDocument(document.id)
    ElMessage.success('删除成功')
    await Promise.all([fetchDocuments(currentParentId.value, { recordHistory: false }), isFolder(document) ? loadFolderTree() : Promise.resolve()])
  } finally {
    documentActionLoading.value = false
  }
}

const buildMoveFolderOptions = (folders, document) => {
  const byId = new Map(folders.map((folder) => [Number(folder.id), folder]))
  const excluded = new Set()
  if (isFolder(document)) {
    excluded.add(Number(document.id))
    let changed = true
    while (changed) {
      changed = false
      folders.forEach((folder) => {
        if (excluded.has(Number(folder.parentId)) && !excluded.has(Number(folder.id))) {
          excluded.add(Number(folder.id))
          changed = true
        }
      })
    }
  }
  const resolvePath = (folder) => {
    const names = [folder.name]
    const visited = new Set([Number(folder.id)])
    let parentId = Number(folder.parentId || 0)
    while (parentId > 0 && byId.has(parentId) && !visited.has(parentId)) {
      visited.add(parentId)
      const parent = byId.get(parentId)
      names.unshift(parent.name)
      parentId = Number(parent.parentId || 0)
    }
    return names.join(' / ')
  }
  return [
    { value: 0, label: '根目录' },
    ...folders
      .filter((folder) => !excluded.has(Number(folder.id)))
      .map((folder) => ({ value: Number(folder.id), label: resolvePath(folder) }))
      .sort((left, right) => left.label.localeCompare(right.label, 'zh-CN'))
  ]
}

const handleTableContextMenu = (document, _column, event) => showContextMenu(document, event)

const showContextMenu = (document, event) => {
  event?.preventDefault?.()
  selectDocument(document)
  const width = 188
  const height = isFolder(document) ? 190 : 230
  contextMenu.document = document
  contextMenu.x = Math.max(8, Math.min(event?.clientX || 0, window.innerWidth - width - 8))
  contextMenu.y = Math.max(8, Math.min(event?.clientY || 0, window.innerHeight - height - 8))
  contextMenu.visible = true
}

const closeContextMenu = () => {
  contextMenu.visible = false
}

const runContextAction = (action) => {
  closeContextMenu()
  action?.()
}

const selectedActionTitle = (action, allowed = true, deniedReason = '') => {
  if (!selectedDocument.value) return `请先选择要${action}的项目`
  return allowed ? action : deniedReason
}

const setViewMode = (mode) => {
  viewMode.value = mode === 'grid' ? 'grid' : 'list'
  window.localStorage?.setItem('hive.document.viewMode', viewMode.value)
}

const documentCellClass = (key) => key === 'size' ? 'text-right text-on-surface-variant font-mono' : 'text-on-surface-variant'
const documentTypeLabel = (document) => isFolder(document) ? '文件夹' : `${(document?.fileExt || 'unknown').toUpperCase()} 文件`

const formatTime = (value) => {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 19)
}

const formatBytes = (bytes) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const index = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1)
  return `${parseFloat((bytes / Math.pow(1024, index)).toFixed(1))} ${units[index]}`
}

const documentExportCell = (document, column) => {
  if (column.key === 'name') return document?.name || ''
  if (column.key === 'createTime') return formatTime(document?.createTime)
  if (column.key === 'type') return documentTypeLabel(document)
  if (column.key === 'size') return isFolder(document) ? '--' : formatBytes(document?.fileSize)
  return ''
}

const retryDocuments = () => fetchDocuments(currentParentId.value, { recordHistory: false })

function resolveDocumentLoadError(error) {
  const status = Number(error?.response?.status || error?.status || error?.code || 0)
  if (status === 401) return { icon: 'warning', title: '登录状态已失效', message: '请重新登录后再加载文档目录。' }
  if (status === 403) return { icon: 'warning', title: '暂无目录访问权限', message: '当前账号缺少文档列表或面包屑权限，请联系管理员。' }
  if (status >= 500) return { icon: 'error', title: '文档服务暂时不可用', message: '服务器处理失败，请稍后重试。' }
  return { icon: 'error', title: '文档目录加载失败', message: '网络连接异常，请检查网络后重试。' }
}

function permissionDisabledClass(disabled) {
  return disabled ? 'cursor-not-allowed grayscale' : ''
}

const getFileIconColor = (ext) => {
  const map = {
    pdf: 'is-pdf', docx: 'is-word', doc: 'is-word', xlsx: 'is-excel', xls: 'is-excel', csv: 'is-excel',
    pptx: 'is-powerpoint', ppt: 'is-powerpoint', png: 'is-image', jpg: 'is-image', jpeg: 'is-image', webp: 'is-image',
    zip: 'is-archive', rar: 'is-archive', '7z': 'is-archive', txt: 'is-text'
  }
  return map[String(ext || '').toLowerCase()] || 'is-generic'
}

onMounted(() => {
  const storedViewMode = window.localStorage?.getItem('hive.document.viewMode')
  if (storedViewMode === 'grid') viewMode.value = 'grid'
  window.addEventListener('resize', closeContextMenu)
  window.addEventListener('scroll', closeContextMenu, true)
  Promise.all([fetchDocuments(0, { recordHistory: false }), loadFolderTree()])
})

onBeforeUnmount(() => {
  documentRequestId += 1
  folderTreeRequestId += 1
  window.removeEventListener('resize', closeContextMenu)
  window.removeEventListener('scroll', closeContextMenu, true)
})
</script>

<style scoped>
.document-explorer {
  --document-border: #d8dee8;
  --document-muted: #667085;
  --document-hover: #f0f6ff;
  --document-selected: #dcecff;
  position: relative;
  display: flex;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  padding: 0;
  color: #172033;
  background: #fff;
  border: 1px solid var(--document-border);
  border-radius: 16px;
  box-shadow: 0 14px 38px rgb(15 23 42 / 8%);
}

.document-navigation-pane {
  display: flex;
  width: 250px;
  min-width: 250px;
  flex-direction: column;
  overflow: hidden;
  background: #f7f9fc;
  border-right: 1px solid var(--document-border);
}

.document-navigation-brand {
  display: flex;
  min-height: 72px;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: #fff;
  border-bottom: 1px solid var(--document-border);
}

.document-navigation-logo {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  place-items: center;
  color: #fff;
  background: var(--ys-primary, #087f73);
  border-radius: 10px;
}

.document-navigation-brand h1 {
  overflow: hidden;
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-navigation-brand p,
.document-content-heading p {
  margin: 2px 0 0;
  color: var(--document-muted);
  font-size: 12px;
}

.document-quick-access {
  padding: 14px 10px 8px;
}

.document-pane-title {
  margin: 0 8px 6px;
  color: #7a8497;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.document-nav-entry {
  display: flex;
  width: 100%;
  height: 38px;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  color: #344054;
  background: transparent;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
}

.document-nav-entry:hover,
.document-nav-entry.is-active {
  color: #075e56;
  background: #e3f3f0;
}

.document-nav-entry:disabled {
  cursor: not-allowed;
  opacity: 1;
}

.document-nav-entry .material-symbols-outlined,
.document-tree-node .material-symbols-outlined {
  font-size: 20px;
}

.document-tree-section {
  min-height: 0;
  flex: 1;
  overflow: auto;
  padding: 4px 10px 12px;
}

.document-tree-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.document-folder-tree {
  color: #344054;
  background: transparent;
  --el-tree-node-hover-bg-color: var(--document-hover);
}

.document-folder-tree :deep(.el-tree-node__content) {
  height: 36px;
  border-radius: 7px;
}

.document-folder-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  color: #075e56;
  background: #d9efeb;
}

.document-tree-node {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 7px;
}

.document-tree-node > span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-tree-node .material-symbols-outlined {
  flex: 0 0 auto;
  color: #e7ad32;
  font-variation-settings: 'FILL' 1;
}

.document-tree-empty {
  display: flex;
  min-height: 110px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #98a2b3;
  font-size: 12px;
}

.document-pane-footer {
  display: flex;
  height: 48px;
  align-items: center;
  gap: 8px;
  padding: 0 18px;
  color: #667085;
  font-size: 12px;
  border-top: 1px solid var(--document-border);
}

.document-workspace {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  background: #fff;
}

.document-topbar {
  display: grid;
  grid-template-columns: auto minmax(260px, 1fr) minmax(210px, 300px);
  min-height: 64px;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--document-border);
}

.document-navigation-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.document-navigation-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.document-address-bar {
  display: flex;
  min-width: 0;
  height: 38px;
  align-items: center;
  overflow-x: auto;
  padding: 3px 7px;
  background: #fff;
  border: 1px solid #cfd6e2;
  border-radius: 8px;
}

.document-address-bar:focus-within {
  border-color: var(--ys-primary, #087f73);
  box-shadow: 0 0 0 2px rgb(8 127 115 / 12%);
}

.document-address-bar button {
  display: inline-flex;
  height: 30px;
  flex: 0 0 auto;
  align-items: center;
  gap: 5px;
  padding: 0 8px;
  color: #344054;
  background: transparent;
  border: 0;
  border-radius: 5px;
  cursor: pointer;
}

.document-address-bar button:hover {
  background: var(--document-hover);
}

.document-address-bar button:disabled {
  cursor: not-allowed;
  opacity: 1;
}

.document-address-bar .material-symbols-outlined {
  font-size: 18px;
}

.document-address-separator {
  flex: 0 0 auto;
  color: #98a2b3;
  font-size: 18px;
}

.document-search {
  width: 100%;
}

.document-command-bar {
  display: flex;
  min-height: 58px;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  overflow-x: auto;
  background: #fbfcfe;
  border-bottom: 1px solid var(--document-border);
}

.document-command-bar :deep(.el-button + .el-button) {
  margin-left: 0;
}

.document-command-bar .material-symbols-outlined {
  margin-right: 4px;
  font-size: 19px;
}

.document-delete-command:not(.is-disabled) {
  color: #b42318;
}

.document-command-divider {
  width: 1px;
  height: 28px;
  flex: 0 0 auto;
  margin: 0 2px;
  background: var(--document-border);
}

.document-command-spacer {
  min-width: 14px;
  flex: 1;
}

.document-type-filter {
  width: 126px;
  flex: 0 0 126px;
}

.document-view-switch {
  display: flex;
  flex: 0 0 auto;
  padding: 3px;
  background: #eef1f6;
  border-radius: 8px;
}

.document-view-switch button {
  display: grid;
  width: 32px;
  height: 30px;
  place-items: center;
  color: #667085;
  background: transparent;
  border: 0;
  border-radius: 6px;
  cursor: pointer;
}

.document-view-switch button.is-active {
  color: #075e56;
  background: #fff;
  box-shadow: 0 1px 3px rgb(15 23 42 / 14%);
}

.document-view-switch .material-symbols-outlined {
  font-size: 19px;
}

.document-file-input {
  display: none;
}

.document-content {
  position: relative;
  min-height: 0;
  flex: 1;
  overflow: auto;
  padding: 14px;
  background: #fff;
}

.document-content.is-dragging {
  overflow: hidden;
}

.document-drop-overlay {
  position: absolute;
  z-index: 20;
  inset: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8px;
  color: #075e56;
  background: rgb(239 250 248 / 96%);
  border: 2px dashed #45aa9d;
  border-radius: 14px;
  pointer-events: none;
}

.document-drop-overlay .material-symbols-outlined {
  font-size: 44px;
}

.document-drop-overlay span:last-child {
  color: var(--document-muted);
  font-size: 12px;
}

.document-content-heading {
  display: flex;
  min-height: 48px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 4px 12px;
}

.document-content-heading h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 750;
}

.document-filter-summary {
  color: var(--document-muted);
  font-size: 12px;
}

.document-list-view {
  overflow: hidden;
  border: 1px solid var(--document-border);
  border-radius: 10px;
}

.document-list-view :deep(.el-table__row) {
  cursor: default;
}

.document-list-view :deep(.el-table__row.current-row > td.el-table__cell) {
  background: var(--document-selected) !important;
}

.document-list-view :deep(.el-button.is-link) {
  min-height: 26px;
  padding: 2px 4px;
  background: transparent !important;
  border: 0 !important;
  border-radius: 4px;
  box-shadow: none !important;
}

.document-list-view :deep(.el-button.is-link:hover) {
  background: var(--document-hover) !important;
}

.document-list-view :deep(.el-button.is-link.el-button--primary) {
  color: var(--ys-primary, #087f73) !important;
}

.document-list-view :deep(.el-button.is-link.el-button--danger) {
  color: #c2413b !important;
}

.document-name-cell {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.document-folder-icon {
  flex: 0 0 auto;
  color: #e8ad32;
  font-size: 30px;
  font-variation-settings: 'FILL' 1;
}

.document-file-icon {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  place-items: center;
  color: #fff;
  font-size: 9px;
  font-weight: 800;
  border-radius: 5px;
}

.document-name-text {
  overflow: hidden;
  color: #1d2939;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-name-text.is-disabled {
  color: #98a2b3;
  cursor: not-allowed;
}

.document-grid-view {
  display: grid;
  min-height: 180px;
  grid-template-columns: repeat(auto-fill, minmax(138px, 1fr));
  align-content: start;
  gap: 10px;
}

.document-grid-view > :deep(.el-empty) {
  grid-column: 1 / -1;
}

.document-grid-item {
  display: flex;
  min-width: 0;
  min-height: 142px;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  padding: 12px 8px;
  color: #344054;
  background: #fff;
  border: 1px solid transparent;
  border-radius: 10px;
  cursor: default;
}

.document-grid-item:hover {
  background: var(--document-hover);
  border-color: #d1e4fb;
}

.document-grid-item.is-selected {
  background: var(--document-selected);
  border-color: #91bdeb;
}

.document-grid-folder {
  color: #e8ad32;
  font-size: 68px;
  line-height: 1;
  font-variation-settings: 'FILL' 1;
}

.document-grid-file {
  position: relative;
  display: grid;
  width: 58px;
  height: 68px;
  place-items: center;
  color: #fff;
  border-radius: 7px;
}

.document-grid-file .material-symbols-outlined {
  font-size: 34px;
}

.document-grid-file small {
  position: absolute;
  right: 5px;
  bottom: 4px;
  font-size: 8px;
  font-weight: 800;
}

.document-grid-name {
  display: -webkit-box;
  max-width: 100%;
  overflow: hidden;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.35;
  text-align: center;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.document-grid-meta {
  color: #98a2b3;
  font-size: 10px;
}

.is-pdf { background: #e5484d; }
.is-word { background: #3478d4; }
.is-excel { background: #268b60; }
.is-powerpoint { background: #d96c2f; }
.is-image { background: #7c5ac7; }
.is-archive { background: #667085; }
.is-text { background: #7d8898; }
.is-generic { background: #5d6b7e; }

.document-statusbar {
  display: flex;
  min-height: 32px;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 5px 14px;
  color: #667085;
  font-size: 11px;
  background: #fbfcfe;
  border-top: 1px solid var(--document-border);
}

.document-context-menu {
  position: fixed;
  z-index: 4000;
  display: flex;
  width: 188px;
  flex-direction: column;
  gap: 2px;
  padding: 6px;
  background: rgb(255 255 255 / 98%);
  border: 1px solid #d8dee8;
  border-radius: 10px;
  box-shadow: 0 14px 34px rgb(15 23 42 / 22%);
  backdrop-filter: blur(10px);
}

.document-context-menu button {
  display: flex;
  width: 100%;
  height: 34px;
  align-items: center;
  gap: 10px;
  padding: 0 10px;
  color: #344054;
  background: transparent;
  border: 0;
  border-radius: 6px;
  cursor: pointer;
}

.document-context-menu button:hover:not(:disabled) {
  background: var(--document-hover);
}

.document-context-menu button:disabled {
  color: var(--ys-disabled-text);
  cursor: not-allowed;
}

.document-context-menu button.is-danger:not(:disabled) {
  color: #b42318;
}

.document-context-menu .material-symbols-outlined {
  font-size: 19px;
}

.document-context-divider {
  height: 1px;
  margin: 3px 4px;
  background: var(--document-border);
}

.document-dialog-location {
  margin: -4px 0 0;
  color: #667085;
  font-size: 12px;
}

@container (max-width: 72rem) {
  .document-topbar {
    grid-template-columns: auto minmax(220px, 1fr);
  }

  .document-search {
    grid-column: 1 / -1;
  }
}

@container (max-width: 88rem) {
  .document-selection-command {
    width: 38px;
    padding-inline: 8px;
  }

  .document-selection-command .material-symbols-outlined {
    margin-right: 0;
  }

  .document-selection-command-label {
    display: none;
  }
}

@container (max-width: 58rem) {
  .document-navigation-pane {
    width: 220px;
    min-width: 220px;
  }

  .document-command-bar {
    flex-wrap: wrap;
  }

  .document-command-spacer {
    display: none;
  }
}

@container (max-width: 44rem) {
  .document-explorer {
    min-height: 680px;
    border-radius: 10px;
  }

  .document-navigation-pane {
    display: none;
  }

  .document-topbar {
    grid-template-columns: 1fr;
  }

  .document-navigation-actions {
    order: 2;
  }

  .document-address-bar {
    order: 1;
  }

  .document-search {
    order: 3;
    grid-column: auto;
  }

  .document-command-divider {
    display: none;
  }

  .document-list-view :deep(.document-operation-column) {
    display: none;
  }

  .document-type-filter {
    width: 112px;
    flex-basis: 112px;
  }

  .document-statusbar span:last-child {
    display: none;
  }
}
</style>
