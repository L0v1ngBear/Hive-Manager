<template>
  <div>
    <div v-for="(image, index) in modelValue" :key="image.fileUrl" class="image-row">
      <el-button link @click="$emit('preview', image)">{{ image.fileName }}</el-button>
      <el-button v-if="!readonly" link type="danger" :disabled="busy" @click="remove(index)">移除</el-button>
    </div>
    <el-upload v-if="!readonly" :show-file-list="false" :before-upload="upload" accept="image/png,image/jpeg,image/webp">
      <el-button :loading="busy" :disabled="modelValue.length >= 9">上传故障反馈图片 / 回访图片</el-button>
      <template #tip><small>JPG、PNG、WebP；单张不超过5MB，最多9张。</small></template>
    </el-upload>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { ElButton, ElUpload, ElMessage } from 'element-plus'
import { uploadAfterSalesRepairImage } from './api/afterSales'
const props = defineProps({ modelValue: { type: Array, default: () => [] }, readonly: Boolean })
const emit = defineEmits(['update:modelValue', 'preview', 'busy'])
const busy = ref(false)
function remove(index) { emit('update:modelValue', props.modelValue.filter((_, i) => i !== index)) }
async function upload(file) {
  if (busy.value || props.modelValue.length >= 9) return false
  if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024) {
    ElMessage.warning('请选择5MB以内的 JPG、PNG、WebP 图片'); return false
  }
  busy.value = true; emit('busy', true)
  try {
    const body = new FormData(); body.append('file', file)
    const result = await uploadAfterSalesRepairImage(body)
    emit('update:modelValue', [...props.modelValue, { fileName: result.fileName || file.name, fileUrl: result.fileUrl, fileSize: result.fileSize ?? file.size }])
  } catch { ElMessage.error('图片上传失败，请重试') }
  finally { busy.value = false; emit('busy', false) }
  return false
}
</script>
<style scoped>
.image-row { display:flex; align-items:center; gap:.5rem; flex-wrap:wrap; }
</style>
