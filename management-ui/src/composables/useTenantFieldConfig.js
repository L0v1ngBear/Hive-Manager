import { computed, ref } from 'vue'
import { getCurrentTenantFieldConfig } from '@/api/tenantFieldConfig'
import {
  defaultTenantFieldConfig,
  mergeTenantFieldConfig,
  tenantFieldLabel,
  tenantFieldRequired,
  tenantFieldVisible,
  visibleTenantFields
} from '@/utils/tenantFieldConfig'

export function useTenantFieldConfig(moduleCode, options = {}) {
  const backendRequiredFields = new Set(options.backendRequiredFields || [])
  const config = ref(defaultTenantFieldConfig(moduleCode))

  const visibleFields = computed(() => visibleTenantFields(config.value, options.fallbackKey))

  async function loadFieldConfig() {
    try {
      const rows = await getCurrentTenantFieldConfig(moduleCode)
      config.value = mergeTenantFieldConfig(moduleCode, rows)
    } catch {
      config.value = defaultTenantFieldConfig(moduleCode)
    }
  }

  function fieldLabel(key, fallback) {
    return tenantFieldLabel(config.value, key, fallback)
  }

  function fieldRequired(key) {
    return backendRequiredFields.has(key) || tenantFieldRequired(config.value, key)
  }

  function fieldVisible(key, options = {}) {
    if (options.keepBackendRequired !== false && backendRequiredFields.has(key)) {
      return true
    }
    return tenantFieldVisible(config.value, key)
  }

  return {
    fieldConfig: config,
    visibleFields,
    loadFieldConfig,
    fieldLabel,
    fieldRequired,
    fieldVisible
  }
}
