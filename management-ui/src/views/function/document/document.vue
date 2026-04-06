<template>
  <div class="h-screen bg-surface text-on-surface flex overflow-hidden font-sans">

    <aside class="w-64 bg-surface-container-lowest border-r border-outline-variant/20 flex flex-col shrink-0">
      <div class="h-16 flex items-center px-6 border-b border-outline-variant/20 shrink-0">
        <span class="material-symbols-outlined text-primary text-2xl mr-2">corporate_fare</span>
        <h1 class="text-lg font-black text-primary tracking-tight">企业知识库</h1>
      </div>

      <div class="flex-1 overflow-y-auto p-4 space-y-6">
        <div class="space-y-1">
          <h3 class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest px-2 mb-2">快速访问</h3>
          <button class="w-full flex items-center gap-3 px-3 py-2 bg-primary/10 text-primary rounded-lg font-bold text-sm transition-colors">
            <span class="material-symbols-outlined text-[18px]">home</span>
            公司文档 (根目录)
          </button>
          <button class="w-full flex items-center gap-3 px-3 py-2 text-on-surface-variant hover:bg-surface-container hover:text-primary rounded-lg font-medium text-sm transition-colors">
            <span class="material-symbols-outlined text-[18px]">schedule</span>
            最近使用
          </button>
          <button class="w-full flex items-center gap-3 px-3 py-2 text-on-surface-variant hover:bg-surface-container hover:text-primary rounded-lg font-medium text-sm transition-colors">
            <span class="material-symbols-outlined text-[18px]">star</span>
            已加星标
          </button>
        </div>

        <div class="space-y-1">
          <h3 class="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest px-2 mb-2">部门目录</h3>
          <button class="w-full flex items-center gap-3 px-3 py-2 text-on-surface-variant hover:bg-surface-container rounded-lg font-medium text-sm transition-colors">
            <span class="material-symbols-outlined text-[18px] text-amber-500" style="font-variation-settings: 'FILL' 1;">folder</span>
            研发中心
          </button>
          <button class="w-full flex items-center gap-3 px-3 py-2 text-on-surface-variant hover:bg-surface-container rounded-lg font-medium text-sm transition-colors">
            <span class="material-symbols-outlined text-[18px] text-amber-500" style="font-variation-settings: 'FILL' 1;">folder</span>
            人力资源部
          </button>
          <button class="w-full flex items-center gap-3 px-3 py-2 text-on-surface-variant hover:bg-surface-container rounded-lg font-medium text-sm transition-colors">
            <span class="material-symbols-outlined text-[18px] text-amber-500" style="font-variation-settings: 'FILL' 1;">folder</span>
            财务与法务
          </button>
        </div>
      </div>

      <div class="p-6 border-t border-outline-variant/20 bg-surface-container-low/50 shrink-0">
        <div class="flex justify-between items-center text-xs font-bold text-on-surface-variant mb-2">
          <span>存储空间</span>
          <span>45%</span>
        </div>
        <div class="w-full bg-outline-variant/30 h-1.5 rounded-full overflow-hidden">
          <div class="bg-primary h-full rounded-full w-[45%]"></div>
        </div>
        <div class="text-[10px] text-on-surface-variant/70 mt-2">450 GB / 1 TB 已用</div>
      </div>
    </aside>

    <main class="flex-1 flex flex-col min-w-0 bg-surface">

      <header class="h-16 flex items-center justify-between px-4 sm:px-6 border-b border-outline-variant/20 bg-surface-container-lowest shrink-0">

        <div class="flex items-center gap-2">
          <button @click="navigateUp" :disabled="breadcrumbs.length <= 1" class="p-2 rounded-lg text-on-surface-variant hover:bg-surface-container hover:text-primary transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
            <span class="material-symbols-outlined">arrow_upward</span>
          </button>
          <div class="h-6 w-px bg-outline-variant/30 mx-1"></div>
          <button class="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg text-sm font-bold shadow-sm hover:bg-primary/90 transition-all active:scale-95">
            <span class="material-symbols-outlined text-[18px]">upload</span>
            上传文件
          </button>
          <button class="flex items-center gap-2 px-4 py-2 bg-surface-container border border-outline-variant/30 text-primary rounded-lg text-sm font-bold shadow-sm hover:bg-surface-container-high transition-all active:scale-95">
            <span class="material-symbols-outlined text-[18px]">create_new_folder</span>
            新建文件夹
          </button>
        </div>

        <div class="relative w-64 hidden sm:block">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant/50 text-[18px]">search</span>
          <input type="text" placeholder="在当前目录中搜索..." class="w-full pl-9 pr-4 py-2 bg-surface-container-low border-none rounded-lg text-sm focus:ring-2 focus:ring-primary transition-shadow font-medium" />
        </div>
      </header>

      <div class="px-6 py-3 bg-surface-container-low/30 border-b border-outline-variant/10 flex items-center gap-1 text-sm shrink-0 overflow-x-auto">
        <template v-for="(crumb, index) in breadcrumbs" :key="crumb.id">
          <button @click="navigateTo(crumb.id)" class="px-2 py-1 hover:bg-surface-container rounded transition-colors text-on-surface-variant hover:text-primary font-medium whitespace-nowrap flex items-center gap-1" :class="{ 'font-bold text-primary': index === breadcrumbs.length - 1 }">
            <span v-if="index === 0" class="material-symbols-outlined text-[16px]">home</span>
            {{ crumb.name }}
          </button>
          <span v-if="index < breadcrumbs.length - 1" class="material-symbols-outlined text-on-surface-variant/40 text-[16px]">chevron_right</span>
        </template>
      </div>

      <div class="flex-1 overflow-y-auto p-4 sm:p-6">
        <div class="bg-surface-container-lowest rounded-xl border border-outline-variant/20 shadow-sm overflow-hidden min-h-full">
          <table class="w-full text-left text-sm whitespace-nowrap">
            <thead class="bg-surface-container-low text-on-surface-variant font-bold text-xs sticky top-0 z-10 shadow-sm">
            <tr>
              <th class="w-10 px-4 py-3 text-center">
                <input type="checkbox" class="rounded border-outline-variant text-primary focus:ring-primary" />
              </th>
              <th class="px-4 py-3 hover:bg-surface-container cursor-pointer transition-colors w-1/2">名称</th>
              <th class="px-4 py-3 hover:bg-surface-container cursor-pointer transition-colors w-1/6">修改日期</th>
              <th class="px-4 py-3 hover:bg-surface-container cursor-pointer transition-colors w-1/6">类型</th>
              <th class="px-4 py-3 hover:bg-surface-container cursor-pointer transition-colors w-1/6 text-right">大小</th>
              <th class="px-4 py-3 text-right">操作</th>
            </tr>
            </thead>
            <tbody class="divide-y divide-outline-variant/10">

            <tr v-for="doc in documentList" :key="doc.id"
                @dblclick="handleDoubleClick(doc)"
                class="hover:bg-primary/5 transition-colors group select-none cursor-default">

              <td class="px-4 py-3 text-center" @click.stop>
                <input type="checkbox" class="rounded border-outline-variant/50 text-primary focus:ring-primary" />
              </td>

              <td class="px-4 py-3 flex items-center gap-3">
                <span v-if="doc.type === 1" class="material-symbols-outlined text-3xl text-amber-400" style="font-variation-settings: 'FILL' 1;">folder</span>
                <div v-else class="w-8 h-8 rounded flex items-center justify-center font-black text-[10px] text-white" :class="getFileIconColor(doc.fileExt)">
                  {{ doc.fileExt.toUpperCase() }}
                </div>

                <span class="font-bold text-primary group-hover:text-primary-fixed cursor-pointer truncate max-w-[300px]">{{ doc.name }}</span>
              </td>

              <td class="px-4 py-3 text-on-surface-variant">{{ doc.updateTime }}</td>

              <td class="px-4 py-3 text-on-surface-variant">{{ doc.type === 1 ? '文件夹' : `${doc.fileExt.toUpperCase()} 文件` }}</td>

              <td class="px-4 py-3 text-right text-on-surface-variant font-mono">
                {{ doc.type === 1 ? '--' : formatBytes(doc.fileSize) }}
              </td>

              <td class="px-4 py-3 text-right">
                <div class="flex justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity" @click.stop>
                  <button v-if="doc.type === 2" class="p-1.5 text-on-surface-variant hover:text-primary hover:bg-surface-container rounded transition-colors" title="下载">
                    <span class="material-symbols-outlined text-[18px]">download</span>
                  </button>
                  <button class="p-1.5 text-on-surface-variant hover:text-primary hover:bg-surface-container rounded transition-colors" title="重命名">
                    <span class="material-symbols-outlined text-[18px]">edit</span>
                  </button>
                  <button class="p-1.5 text-on-surface-variant hover:text-error hover:bg-error-container rounded transition-colors" title="删除">
                    <span class="material-symbols-outlined text-[18px]">delete</span>
                  </button>
                </div>
              </td>
            </tr>

            <tr v-if="documentList.length === 0">
              <td colspan="6" class="px-4 py-16 text-center text-on-surface-variant/50">
                <span class="material-symbols-outlined text-4xl mb-2 opacity-50">folder_open</span>
                <p class="font-medium text-sm">此文件夹为空</p>
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
import { ref, computed } from 'vue'

// ==========================================
// 1. 状态与模拟数据 (严格映射你的 Document 实体)
// ==========================================

// 面包屑导航 (模拟地址栏记录)
const breadcrumbs = ref([
  { id: 0, name: '公司文档' }
])

// 模拟数据库中拿到的 Document 列表
const documentList = ref([
  {
    id: 101,
    parentId: 0,
    name: '2026年研发中心制度规范',
    type: 1, // 1-文件夹
    fileUrl: null,
    fileSize: null,
    fileExt: null,
    mimeType: null,
    createTime: '2026-04-01 10:00:00',
    updateTime: '2026-04-05 14:30:00'
  },
  {
    id: 102,
    parentId: 0,
    name: '产品设计资产 (UI/UX)',
    type: 1,
    fileUrl: null,
    fileSize: null,
    fileExt: null,
    mimeType: null,
    createTime: '2026-04-02 09:15:00',
    updateTime: '2026-04-02 09:15:00'
  },
  {
    id: 103,
    parentId: 0,
    name: 'Q1_财务报表_最终版.xlsx',
    type: 2, // 2-文件
    fileUrl: 'https://oss.example.com/Q1.xlsx',
    fileSize: 2450000, // 字节
    fileExt: 'xlsx',
    mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    createTime: '2026-04-06 16:20:00',
    updateTime: '2026-04-06 16:20:00'
  },
  {
    id: 104,
    parentId: 0,
    name: '员工入职手册2026.pdf',
    type: 2,
    fileUrl: 'https://oss.example.com/handbook.pdf',
    fileSize: 8540000,
    fileExt: 'pdf',
    mimeType: 'application/pdf',
    createTime: '2026-03-15 11:00:00',
    updateTime: '2026-03-15 11:00:00'
  },
  {
    id: 105,
    parentId: 0,
    name: '系统架构演进图.png',
    type: 2,
    fileUrl: 'https://oss.example.com/arch.png',
    fileSize: 1250000,
    fileExt: 'png',
    mimeType: 'image/png',
    createTime: '2026-04-05 18:00:00',
    updateTime: '2026-04-05 18:00:00'
  }
])

// ==========================================
// 2. 交互逻辑
// ==========================================

// 双击事件：进入文件夹 或 预览文件
const handleDoubleClick = (doc) => {
  if (doc.type === 1) {
    // 文件夹：进入下一级
    enterFolder(doc)
  } else {
    // 文件：执行预览或下载
    console.log('预览/下载文件:', doc.fileUrl)
    // TODO: window.open(doc.fileUrl)
  }
}

// 进入文件夹
const enterFolder = (folder) => {
  breadcrumbs.value.push({ id: folder.id, name: folder.name })

  // TODO: 这里应向后端发起请求获取子节点数据
  // axios.get(`/api/document/list?parentId=${folder.id}`)

  // 模拟清空进入空文件夹
  documentList.value = []
}

// 向上返回一级 (向上箭头)
const navigateUp = () => {
  if (breadcrumbs.value.length > 1) {
    breadcrumbs.value.pop()
    const currentFolderId = breadcrumbs.value[breadcrumbs.value.length - 1].id
    refreshData(currentFolderId)
  }
}

// 点击面包屑跳转到指定层级
const navigateTo = (folderId) => {
  const index = breadcrumbs.value.findIndex(c => c.id === folderId)
  if (index !== -1) {
    breadcrumbs.value = breadcrumbs.value.slice(0, index + 1)
    refreshData(folderId)
  }
}

// 模拟刷新数据
const refreshData = (parentId) => {
  // TODO: 发起真实 API 请求
  console.log(`刷新目录 ID: ${parentId} 的数据...`)
  // 仅作恢复初始数据的演示
  if (parentId === 0) {
    window.location.reload()
  }
}

// ==========================================
// 3. 格式化工具函数
// ==========================================

// 格式化文件大小 (字节转换为 KB/MB)
const formatBytes = (bytes) => {
  if (bytes === 0 || !bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

// 根据文件扩展名返回不同的 Tailwind 背景色类名
const getFileIconColor = (ext) => {
  const extLower = ext?.toLowerCase()
  const map = {
    'pdf': 'bg-red-500',
    'docx': 'bg-blue-500',
    'doc': 'bg-blue-500',
    'xlsx': 'bg-emerald-500',
    'xls': 'bg-emerald-500',
    'pptx': 'bg-orange-500',
    'png': 'bg-purple-500',
    'jpg': 'bg-purple-500',
    'zip': 'bg-slate-500',
    'rar': 'bg-slate-500',
    'txt': 'bg-gray-400'
  }
  return map[extLower] || 'bg-slate-400'
}
</script>

<style scoped>
/* 隐藏复选框的默认样式，使其更符合系统主题 */
input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
}
</style>
