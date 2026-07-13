<template>
  <div
    class="drag-attachment-upload"
    :class="{ 'is-disabled': disabled, 'is-dragging': dragging, 'is-uploading': uploading }"
    role="button"
    :tabindex="disabled || uploading ? -1 : 0"
    :aria-disabled="disabled || uploading"
    :title="disabled ? disabledReason : undefined"
    @click="openPicker"
    @keydown.enter.prevent="openPicker"
    @keydown.space.prevent="openPicker"
    @dragenter.prevent="onDragEnter"
    @dragover.prevent="onDragOver"
    @dragleave.prevent="onDragLeave"
    @drop.prevent="onDrop"
  >
    <input
      ref="inputRef"
      class="hidden"
      type="file"
      :accept="accept"
      :disabled="disabled || uploading"
      @change="onFileChange"
    >

    <div class="drag-upload-main">
      <span class="material-symbols-outlined drag-upload-icon" :class="{ 'animate-spin': uploading }">
        {{ uploading ? 'progress_activity' : icon }}
      </span>
      <div class="min-w-0 flex-1">
        <p class="drag-upload-title">
          {{ fileName || title }}
        </p>
        <p class="drag-upload-helper">
          {{ helperText }}
          <template v-if="fileSize"> · {{ formattedSize }}</template>
        </p>
      </div>
    </div>

    <div v-if="fileUrl" class="drag-upload-actions" @click.stop>
      <el-button v-if="downloadable" size="small" type="primary" plain :disabled="downloadDisabled" :title="downloadDisabled ? disabledReason : ''" @click="$emit('download')">
        查看附件
      </el-button>
      <el-button size="small" type="danger" plain :disabled="removeDisabled" :title="removeDisabled ? disabledReason : ''" @click="$emit('remove')">
        移除
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElButton, ElMessage } from 'element-plus'

const props = defineProps({
  title: {
    type: String,
    default: '点击或拖拽上传附件'
  },
  helperText: {
    type: String,
    default: '支持图片、PDF、Word、Excel、文本或压缩包'
  },
  accept: {
    type: String,
    default: '.pdf,.png,.jpg,.jpeg,.webp,.doc,.docx,.xls,.xlsx,.csv,.txt,.zip,.rar,.7z'
  },
  icon: {
    type: String,
    default: 'upload_file'
  },
  uploading: {
    type: Boolean,
    default: false
  },
  fileName: {
    type: String,
    default: ''
  },
  fileUrl: {
    type: String,
    default: ''
  },
  fileSize: {
    type: [Number, String],
    default: null
  },
  downloadable: {
    type: Boolean,
    default: true
  },
  disabled: {
    type: Boolean,
    default: false
  },
  disabledReason: {
    type: String,
    default: ''
  },
  downloadDisabled: { type: Boolean, default: false },
  removeDisabled: { type: Boolean, default: false }
})

const emit = defineEmits(['select', 'download', 'remove'])
const inputRef = ref(null)
const dragging = ref(false)

const formattedSize = computed(() => {
  const bytes = Number(props.fileSize || 0)
  if (!bytes) return ''
  const units = ['B', 'KB', 'MB', 'GB']
  let value = bytes
  let index = 0
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index += 1
  }
  return `${value.toFixed(value >= 10 || index === 0 ? 0 : 1)} ${units[index]}`
})

function openPicker() {
  if (props.disabled || props.uploading) return
  inputRef.value?.click()
}

function onFileChange(event) {
  const file = event.target.files?.[0]
  if (!props.disabled && !props.uploading) emitFile(file)
  event.target.value = ''
}

function onDragEnter() {
  if (!props.disabled && !props.uploading) {
    dragging.value = true
  }
}

function onDragOver() {
  if (!props.disabled && !props.uploading) {
    dragging.value = true
  }
}

function onDragLeave(event) {
  const relatedTarget = event.relatedTarget
  if (!relatedTarget || !event.currentTarget.contains(relatedTarget)) {
    dragging.value = false
  }
}

function onDrop(event) {
  dragging.value = false
  if (props.disabled || props.uploading) return
  emitFile(event.dataTransfer?.files?.[0])
}

function emitFile(file) {
  if (!file) return
  if (!fileMatchesAccept(file)) {
    ElMessage.warning('所选文件类型不受支持')
    return
  }
  emit('select', file)
}

function fileMatchesAccept(file) {
  const acceptedTypes = props.accept
    .split(',')
    .map((type) => type.trim().toLowerCase())
    .filter(Boolean)

  if (acceptedTypes.length === 0 || acceptedTypes.includes('*/*')) {
    return true
  }

  const fileName = String(file.name || '').toLowerCase()
  const mimeType = String(file.type || '').toLowerCase()
  return acceptedTypes.some((acceptedType) => {
    if (acceptedType.startsWith('.')) {
      return fileName.endsWith(acceptedType)
    }
    if (acceptedType.endsWith('/*')) {
      return mimeType.startsWith(acceptedType.slice(0, -1))
    }
    return mimeType === acceptedType
  })
}
</script>

<style scoped>
.drag-attachment-upload {
  cursor: pointer;
  border-radius: 18px;
  border: 1px dashed rgba(31, 111, 255, 0.32);
  background: linear-gradient(135deg, rgba(31, 111, 255, 0.06), rgba(255, 255, 255, 0.9));
  padding: 16px;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease, background 0.18s ease;
}

.drag-attachment-upload:hover,
.drag-attachment-upload.is-dragging {
  border-color: rgba(31, 111, 255, 0.72);
  background: linear-gradient(135deg, rgba(31, 111, 255, 0.12), rgba(255, 255, 255, 0.96));
  box-shadow: 0 18px 40px rgba(31, 111, 255, 0.12);
  transform: translateY(-1px);
}

.drag-attachment-upload.is-uploading {
  cursor: wait;
  opacity: 0.78;
}

.drag-attachment-upload.is-disabled {
  cursor: not-allowed;
  border-color: rgba(107, 122, 144, 0.28);
  background: rgba(245, 247, 250, 0.9);
  opacity: 0.7;
}

.drag-attachment-upload.is-disabled:hover {
  border-color: rgba(107, 122, 144, 0.28);
  background: rgba(245, 247, 250, 0.9);
  box-shadow: none;
  transform: none;
}

.drag-upload-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.drag-upload-icon {
  flex: 0 0 auto;
  font-size: 30px;
  color: #1f6fff;
}

.drag-upload-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 900;
  color: #12345d;
}

.drag-upload-helper {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: #6b7a90;
}

.drag-upload-actions {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

</style>
