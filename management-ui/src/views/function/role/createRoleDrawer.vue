<template>
  <el-drawer v-model="visible" :title="null" size="500px" direction="rtl" :with-header="false" class="atelier-drawer" append-to-body>
    <div class="h-1 bg-primary w-full sticky top-0 z-10"></div>
    <div class="flex flex-col h-full bg-surface-container-lowest font-body">
      <div class="px-8 py-8 border-b border-surface-variant/30">
        <div class="flex justify-between items-start mb-6">
          <div class="p-3 bg-primary-container text-white rounded-xl shadow-lg shadow-primary/10">
            <span class="material-symbols-outlined text-3xl">person_add</span>
          </div>
          <button @click="close" class="p-2 hover:bg-surface-container-high rounded-full transition-colors group">
            <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary">close</span>
          </button>
        </div>
        <h2 class="text-2xl font-black text-primary tracking-tight">新建角色</h2>
        <p class="text-sm text-on-surface-variant mt-1">定义职能岗位并关联对应的系统操作权限</p>
      </div>

      <div class="flex-1 overflow-y-auto p-8 space-y-8 no-scrollbar">
        <div class="space-y-2">
          <label class="text-xs font-black uppercase tracking-widest text-on-surface-variant">角色名称</label>
          <input v-model="form.roleName" type="text" placeholder="例如：高级裁切师" class="w-full px-4 py-3.5 rounded bg-surface-container-low border-none focus:ring-2 focus:ring-primary/20 text-on-surface transition-all placeholder:text-on-surface-variant/40" />
        </div>

        <div class="space-y-2 relative">
          <label class="text-xs font-black uppercase tracking-widest text-on-surface-variant flex justify-between">
            分配权限
            <span class="text-[10px] font-normal lowercase opacity-60">勾选下方列表授予功能访问权</span>
          </label>

          <div class="atelier-tree-select-wrapper">
            <el-tree-select
              v-model="form.permissionIds"
              :data="allPermissions"
              node-key="id"
              multiple
              collapse-tags
              collapse-tags-tooltip
              :props="{ value: 'id', label: 'permName', children: 'children' }"
              :placeholder="isLoadingPerms ? '正在加载权限列表...' : '点击选择系统权限'"
              class="atelier-tree-select w-full"
              filterable
              check-strictly
              :loading="isLoadingPerms"
            >
              <template #default="{ data }">
                <div class="flex items-center gap-2">
                  <span class="material-symbols-outlined text-[18px] opacity-40">{{ data.children?.length ? 'folder' : 'description' }}</span>
                  <span class="text-sm font-medium text-on-surface">{{ data.permName }}</span>
                </div>
              </template>
            </el-tree-select>
          </div>
        </div>
      </div>

      <div class="p-8 bg-surface-container-low border-t border-surface-variant/30 grid grid-cols-2 gap-4">
        <button @click="close" class="py-3 px-6 rounded-lg text-sm font-bold text-on-surface-variant hover:bg-surface-container-high transition-all">取消返回</button>
        <button @click="submit" :disabled="isSubmitting" class="py-3 px-6 rounded-lg text-sm font-bold text-white bg-primary shadow-lg shadow-primary/30 active:scale-95 hover:opacity-90 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2">
          <span v-if="isSubmitting" class="material-symbols-outlined text-[18px] animate-spin">progress_activity</span>
          {{ isSubmitting ? '提交中...' : '确认创建' }}
        </button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createRole, getAllPermissions } from './api/role.js'

const emit = defineEmits(['success'])
const visible = ref(false)
const isSubmitting = ref(false)
const isLoadingPerms = ref(false)
const form = ref({ roleName: '', permissionIds: [] })
const allPermissions = ref([])

function buildTree(list = []) {
  const nodeMap = new Map()
  const roots = []
  list.forEach((item) => nodeMap.set(item.id, { ...item, children: [] }))
  nodeMap.forEach((node) => {
    if (node.parentId && nodeMap.has(node.parentId)) {
      nodeMap.get(node.parentId).children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}

async function fetchPermissions() {
  isLoadingPerms.value = true
  try {
    allPermissions.value = buildTree(await getAllPermissions())
  } catch (error) {
    console.error('获取权限异常:', error)
  } finally {
    isLoadingPerms.value = false
  }
}

async function open() {
  visible.value = true
  form.value = { roleName: '', permissionIds: [] }
  await fetchPermissions()
}

function close() {
  visible.value = false
}

async function submit() {
  if (!form.value.roleName.trim()) {
    ElMessage.warning('角色名称是必填项')
    return
  }
  if (!form.value.permissionIds.length) {
    ElMessage.warning('请至少分配一个权限项')
    return
  }
  isSubmitting.value = true
  try {
    await createRole({ roleName: form.value.roleName.trim(), permissionIds: form.value.permissionIds })
    ElMessage.success(`角色【${form.value.roleName}】已成功创建`)
    emit('success')
    close()
  } catch (error) {
    console.error('提交异常:', error)
  } finally {
    isSubmitting.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
:deep(.atelier-drawer) { box-shadow: 0px 20px 40px rgba(0, 32, 69, 0.06) !important; background-color: #ffffff !important; }
:deep(.atelier-tree-select .el-input__wrapper) { background-color: #f1f4f6 !important; box-shadow: none !important; border-radius: 4px; padding: 10px 12px; }
:deep(.el-tree-node__expand-icon.is-leaf) { color: transparent; }
.no-scrollbar::-webkit-scrollbar { display: none; }
</style>