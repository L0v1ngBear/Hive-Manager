<template>
  <el-drawer v-model="visible" :title="null" size="500px" direction="rtl" :with-header="false" class="atelier-drawer" append-to-body destroy-on-close>
    <div class="h-1 bg-primary w-full sticky top-0 z-10"></div>
    <div class="flex flex-col h-full bg-surface-container-lowest font-body">
      <div class="px-8 py-8 border-b border-surface-variant/30">
        <div class="flex justify-between items-start mb-6">
          <div class="p-3 bg-primary-container text-white rounded-xl shadow-lg shadow-primary/10">
            <span class="material-symbols-outlined text-3xl">person_add</span>
          </div>
          <el-button circle text class="p-2 hover:bg-surface-container-high rounded-full transition-colors group" @click="close">
            <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary">close</span>
          </el-button>
        </div>
        <h2 class="text-2xl font-black text-primary tracking-tight">新建角色</h2>
        <p class="text-sm text-on-surface-variant mt-1">定义职能岗位并关联对应的系统操作权限</p>
      </div>

      <el-form :model="form" class="flex-1 overflow-y-auto p-8 space-y-8 no-scrollbar" @submit.prevent="submit">
        <el-form-item label="角色名称" class="space-y-2">
          <el-input v-model="form.roleName" placeholder="例如：高级裁剪师" />
        </el-form-item>

        <el-form-item label="分配权限" class="space-y-2 relative">
          <p class="text-[10px] font-normal lowercase text-on-surface-variant opacity-60">勾选下方列表授予功能访问权</p>

          <el-alert
            v-if="permissionLoader.state.loadState === 'forbidden'"
            title="无权查看权限树"
            description="当前账号缺少角色权限查看能力，请联系管理员分配 role:permission:list 权限。"
            type="warning"
            :closable="false"
            show-icon
          />
          <div v-else-if="permissionLoader.state.loadState === 'failed'" class="space-y-3">
            <el-alert title="权限树加载失败" description="无法连接权限服务，请检查网络或稍后重试。" type="error" :closable="false" show-icon />
            <el-button type="primary" plain @click="fetchPermissions">重新加载</el-button>
          </div>
          <el-empty v-else-if="permissionLoader.state.loadState === 'empty'" description="暂无可分配权限" />
          <div v-else-if="permissionLoader.state.loadState === 'ready' || permissionLoader.state.loading" class="atelier-tree-select-wrapper">
            <el-tree-select
              v-model="form.permissionIds"
              :data="permissionLoader.state.treeData"
              node-key="id"
              value-key="id"
              multiple
              show-checkbox
              collapse-tags
              collapse-tags-tooltip
              check-on-click-node
              :render-after-expand="false"
              :props="{ value: 'id', label: 'permName', children: 'children' }"
              :placeholder="permissionLoader.state.loading ? '正在加载权限列表...' : '点击选择系统权限'"
              class="atelier-tree-select w-full"
              filterable
              :loading="permissionLoader.state.loading"
            >
              <template #default="scope">
                <div v-if="scope?.data" class="flex items-center gap-2">
                  <span class="material-symbols-outlined text-[18px] opacity-40">
                    {{ scope.data.children?.length ? 'folder' : 'description' }}
                  </span>
                  <span class="text-sm font-medium text-on-surface">{{ scope.data.permName }}</span>
                </div>
              </template>
            </el-tree-select>
          </div>
        </el-form-item>
      </el-form>

      <div class="p-8 bg-surface-container-low border-t border-surface-variant/30 grid grid-cols-2 gap-4">
        <el-button @click="close">取消返回</el-button>
        <el-button type="primary" :loading="isSubmitting" :disabled="!permissionTreeCanSubmit(permissionLoader.state.loadState)" @click="submit">
          {{ isSubmitting ? '提交中...' : '确认创建' }}
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref } from 'vue'
import { ElAlert, ElButton, ElDrawer, ElEmpty, ElForm, ElFormItem, ElInput, ElMessage, ElTreeSelect } from 'element-plus'
import { createRole, getAllPermissions } from './api/role.js'
import { createPermissionTreeLoader, permissionTreeCanSubmit } from './permissionLoaders.js'

const emit = defineEmits(['success', 'closed'])
const visible = ref(false)
const isSubmitting = ref(false)
const form = ref({ roleName: '', permissionIds: [] })
const permissionLoader = createPermissionTreeLoader({ getAllPermissions })

async function fetchPermissions() {
  await permissionLoader.load()
}

async function open() {
  visible.value = true
  form.value = { roleName: '', permissionIds: [] }
  await fetchPermissions()
}

function close() {
  visible.value = false
  emit('closed')
}

async function submit() {
  if (!form.value.roleName.trim()) {
    ElMessage.warning('角色名称是必填项')
    return
  }
  if (!permissionTreeCanSubmit(permissionLoader.state.loadState)) {
    ElMessage.warning('权限树尚未准备完成')
    return
  }
  isSubmitting.value = true
  try {
    await createRole({
      roleName: form.value.roleName.trim(),
      permissionIds: [...new Set(form.value.permissionIds || [])]
    })
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
:deep(.atelier-drawer) { box-shadow: 0px 20px 40px rgba(31, 111, 255, 0.06) !important; background-color: #ffffff !important; }

:deep(.atelier-tree-select .el-select__wrapper) {
  background-color: #f1f4f6 !important;
  box-shadow: none !important;
  border-radius: 4px;
  min-height: 48px;
  padding: 4px 12px;
}

:deep(.el-tree-node__expand-icon.is-leaf) { color: transparent; }

.no-scrollbar::-webkit-scrollbar { display: none; }
</style>
