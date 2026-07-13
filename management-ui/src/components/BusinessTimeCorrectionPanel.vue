<template>
  <section v-if="active" class="business-time-correction-panel">
    <div>
      <div class="business-time-correction-title">
        <span class="material-symbols-outlined text-[18px]">edit_calendar</span>
        {{ title }}
      </div>
      <p>{{ description }}</p>
    </div>
    <label>
      <span>{{ label }}</span>
      <input
        :value="modelValue"
        :data-field="dataField"
        class="business-time-correction-input"
        :type="inputType"
        :step="step"
        @input="$emit('update:modelValue', $event.target.value)"
      />
    </label>
  </section>
</template>

<script setup>
defineProps({
  active: {
    type: Boolean,
    default: false
  },
  modelValue: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    default: '业务时间修正'
  },
  description: {
    type: String,
    default: '用于修正当前单据的业务时间。'
  },
  label: {
    type: String,
    default: '业务时间'
  },
  dataField: {
    type: String,
    default: 'business.createTime'
  },
  inputType: {
    type: String,
    default: 'datetime-local'
  },
  step: {
    type: [String, Number],
    default: 1
  }
})

defineEmits(['update:modelValue'])
</script>

<style scoped>
.business-time-correction-panel {
  display: grid;
  gap: 16px;
  padding: 18px;
  border: 1px solid rgba(31, 63, 95, 0.22);
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(238, 244, 251, 0.95), rgba(255, 255, 255, 0.92));
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.08);
}

.business-time-correction-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 900;
  color: #1f3f5f;
}

.business-time-correction-panel p {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.8;
  color: rgba(71, 85, 105, 0.78);
}

.business-time-correction-panel label {
  display: grid;
  gap: 8px;
}

.business-time-correction-panel label > span {
  font-size: 12px;
  font-weight: 800;
  color: rgba(71, 85, 105, 0.86);
}

.business-time-correction-input {
  width: 100%;
  border-radius: 14px;
  border: 1px solid rgba(200, 211, 223, 0.86);
  background: rgba(255, 255, 255, 0.9);
  padding: 12px 14px;
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
  outline: none;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.business-time-correction-input:focus {
  border-color: rgba(31, 63, 95, 0.72);
  box-shadow: 0 0 0 4px rgba(31, 63, 95, 0.12);
}
</style>
