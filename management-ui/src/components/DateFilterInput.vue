<template>
  <label class="date-filter-shell" :class="$attrs.class">
    <span v-if="!modelValue" class="date-filter-placeholder">{{ placeholder }}</span>
    <input
      class="date-filter-native"
      :class="{ 'is-empty': !modelValue }"
      type="date"
      :value="modelValue || ''"
      :title="title || placeholder"
      @input="handleInput"
      @change="handleChange"
    />
  </label>
</template>

<script setup>
defineOptions({ name: 'DateFilterInput', inheritAttrs: false })

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  placeholder: {
    type: String,
    default: '选择日期'
  },
  title: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

function handleInput(event) {
  emit('update:modelValue', event.target.value)
}

function handleChange(event) {
  emit('update:modelValue', event.target.value)
  emit('change', event)
}
</script>

<style scoped>
.date-filter-shell {
  position: relative;
  display: inline-flex;
  min-height: 2.5rem;
  min-width: 8.75rem;
  align-items: center;
  cursor: pointer;
}

.date-filter-placeholder {
  pointer-events: none;
  color: rgba(100, 116, 139, 0.78);
  white-space: nowrap;
}

.date-filter-native {
  position: absolute;
  inset: 0;
  box-sizing: border-box;
  height: 100%;
  width: 100%;
  border-radius: inherit;
  cursor: pointer;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  outline: none;
  padding: inherit;
}

.date-filter-native.is-empty {
  color: transparent;
}

.date-filter-native.is-empty::-webkit-datetime-edit {
  color: transparent;
}

.date-filter-native:focus {
  color: inherit;
}

.date-filter-shell:focus-within .date-filter-placeholder {
  opacity: 0;
}
</style>
