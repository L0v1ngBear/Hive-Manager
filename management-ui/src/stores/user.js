import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { hasAnyPermission as matchAnyPermission, hasPermission as matchPermission } from '@/utils/permission'

const authStorage = window.sessionStorage
const persistentStorage = window.localStorage
const AUTH_KEYS = ['token', 'userInfo', 'permissions', 'features', 'responseKey', 'expireAt', 'mustChangePassword']
const ORDER_FEATURE_CODE = 'module.order'

const clearPersistentAuthStorage = () => {
  AUTH_KEYS.forEach((key) => persistentStorage.removeItem(key))
}

clearPersistentAuthStorage()

const readStorageItem = (key, fallback = '') => authStorage.getItem(key) || fallback

const readJsonStorageItem = (key, fallback) => {
  try {
    return JSON.parse(readStorageItem(key, JSON.stringify(fallback)))
  } catch {
    return fallback
  }
}

const removeLoginStorage = () => {
  AUTH_KEYS.forEach((key) => {
    authStorage.removeItem(key)
    persistentStorage.removeItem(key)
  })
}

export const useUserStore = defineStore('user', () => {
  const token = ref(readStorageItem('token'))
  const userInfo = ref(readJsonStorageItem('userInfo', null))
  const permissions = ref(readJsonStorageItem('permissions', []))
  const features = ref(readJsonStorageItem('features', []))
  const currentTenantCode = computed(() => String(userInfo.value?.tenantCode || '').trim())
  const currentTenantName = computed(() => {
    const tenantName = String(userInfo.value?.tenantName || '').trim()
    if (tenantName) {
      return tenantName
    }
    return currentTenantCode.value || '当前组织'
  })
  const currentTenantLabel = computed(() => {
    return currentTenantName.value
  })
  const currentTenantLogoUrl = computed(() => String(userInfo.value?.tenantLogoUrl || '').trim())
  const isDeveloper = computed(() => Boolean(userInfo.value?.developer))
  const isPlatformTenant = computed(() => currentTenantCode.value.toLowerCase() === 'super')
  const responseKey = ref(readStorageItem('responseKey'))
  const expireAt = ref(readStorageItem('expireAt'))
  const mustChangePassword = ref(readStorageItem('mustChangePassword') === '1')

  const setLoginInfo = (loginData) => {
    token.value = loginData?.token || ''
    userInfo.value = loginData
      ? {
          userId: loginData.userId,
          userName: loginData.userName,
          tenantCode: loginData.tenantCode,
          tenantName: loginData.tenantName,
          tenantLogoUrl: loginData.tenantLogoUrl,
          developer: Boolean(loginData.developer)
        }
      : null
    permissions.value = loginData?.permissions || []
    features.value = Array.isArray(loginData?.features) ? loginData.features : []
    responseKey.value = loginData?.responseKey || ''
    expireAt.value = loginData?.expireAt ? String(loginData.expireAt) : ''
    mustChangePassword.value = Boolean(loginData?.mustChangePassword)

    clearPersistentAuthStorage()
    authStorage.setItem('token', token.value)
    authStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    authStorage.setItem('permissions', JSON.stringify(permissions.value))
    authStorage.setItem('features', JSON.stringify(features.value))
    authStorage.setItem('responseKey', responseKey.value)
    authStorage.setItem('expireAt', expireAt.value)
    authStorage.setItem('mustChangePassword', mustChangePassword.value ? '1' : '0')
  }

  const renewSession = ({ token: renewedToken, responseKey: renewedResponseKey, expireAt: renewedExpireAt } = {}) => {
    if (!renewedToken || !renewedResponseKey) {
      return
    }
    token.value = renewedToken
    responseKey.value = renewedResponseKey
    expireAt.value = renewedExpireAt ? String(renewedExpireAt) : expireAt.value

    clearPersistentAuthStorage()
    authStorage.setItem('token', token.value)
    authStorage.setItem('responseKey', responseKey.value)
    authStorage.setItem('expireAt', expireAt.value)
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    features.value = []
    responseKey.value = ''
    expireAt.value = ''
    mustChangePassword.value = false
    removeLoginStorage()
  }

  const markPasswordChanged = () => {
    mustChangePassword.value = false
    clearPersistentAuthStorage()
    authStorage.setItem('mustChangePassword', '0')
  }

  const updateTenantBrand = ({ tenantName, tenantLogoUrl } = {}) => {
    if (!userInfo.value) {
      return
    }
    userInfo.value = {
      ...userInfo.value,
      tenantName: tenantName ?? userInfo.value.tenantName,
      tenantLogoUrl: tenantLogoUrl ?? userInfo.value.tenantLogoUrl
    }
    clearPersistentAuthStorage()
    authStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  const hasPermission = (permCode) => matchPermission(permissions.value, permCode)
  const hasAnyPermission = (permCodes) => matchAnyPermission(permissions.value, permCodes)
  const hasFeature = (featureCode) => {
    const normalizedFeatureCode = String(featureCode || '').trim()
    if (!normalizedFeatureCode) {
      return true
    }
    if (Array.isArray(features.value) && features.value.includes(normalizedFeatureCode)) {
      return true
    }
    if (normalizedFeatureCode === ORDER_FEATURE_CODE && matchPermission(permissions.value, 'order:list')) {
      return true
    }
    // Old sessions do not have feature data yet. Keep base modules usable, but never expose custom modules without an explicit grant.
    if ((!features.value || features.value.length === 0) && normalizedFeatureCode.startsWith('module.')) {
      return true
    }
    return false
  }
  const hasAnyFeature = (featureCodes) => {
    if (!Array.isArray(featureCodes) || featureCodes.length === 0) {
      return true
    }
    return featureCodes.some((featureCode) => hasFeature(featureCode))
  }

  return {
    token,
    userInfo,
    permissions,
    features,
    currentTenantCode,
    currentTenantName,
    currentTenantLabel,
    currentTenantLogoUrl,
    isDeveloper,
    isPlatformTenant,
    responseKey,
    expireAt,
    mustChangePassword,
    hasPermission,
    hasAnyPermission,
    hasFeature,
    hasAnyFeature,
    setLoginInfo,
    renewSession,
    markPasswordChanged,
    updateTenantBrand,
    logout
  }
})
