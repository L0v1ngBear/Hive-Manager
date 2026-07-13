import { onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

export const TIME_CORRECTION_HOTKEY_LABEL = 'Ctrl + Alt + T'

export function useTimeCorrectionMode(options = {}) {
  const timeCorrectionMode = ref(false)
  const isAvailable = typeof options.isAvailable === 'function' ? options.isAvailable : () => true

  function toggleTimeCorrectionMode() {
    timeCorrectionMode.value = !timeCorrectionMode.value
    ElMessage.info(timeCorrectionMode.value ? '已开启业务时间修正' : '已关闭业务时间修正')
  }

  function closeTimeCorrectionMode() {
    timeCorrectionMode.value = false
  }

  function isTimeCorrectionHotkey(event) {
    if (!event || event.repeat || event.isComposing) {
      return false
    }
    const key = String(event.key || '').toLowerCase()
    const code = String(event.code || '')
    return event.ctrlKey && event.altKey && (key === 't' || code === 'KeyT')
  }

  function handleTimeCorrectionHotkey(event) {
    if (!isTimeCorrectionHotkey(event)) {
      return
    }
    event.preventDefault()
    event.stopPropagation()
    toggleTimeCorrectionMode()
  }

  onMounted(() => {
    document.addEventListener('keydown', handleTimeCorrectionHotkey, true)
  })

  onUnmounted(() => {
    document.removeEventListener('keydown', handleTimeCorrectionHotkey, true)
  })

  return {
    timeCorrectionMode,
    isTimeCorrectionAvailable: isAvailable,
    toggleTimeCorrectionMode,
    closeTimeCorrectionMode
  }
}
