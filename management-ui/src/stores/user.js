import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { hasAnyPermission as matchAnyPermission, hasPermission as matchPermission } from '@/utils/permission'

const authStorage = window.sessionStorage
const legacyStorage = window.localStorage

const readStorageItem = (key, fallback = '') => authStorage.getItem(key) || legacyStorage.getItem(key) || fallback

const readJsonStorageItem = (key, fallback) => {
  try {
    return JSON.parse(readStorageItem(key, JSON.stringify(fallback)))
  } catch (error) {
    return fallback
  }
}

const removeLoginStorage = () => {
  ;['token', 'userInfo', 'permissions', 'features', 'responseKey', 'expireAt'].forEach((key) => {
    authStorage.removeItem(key)
    legacyStorage.removeItem(key)
  })
}

export const useUserStore = defineStore('user', () => {
  const token = ref(readStorageItem('token'))
  const userInfo = ref(readJsonStorageItem('userInfo', null))
  const permissions = ref(readJsonStorageItem('permissions', []))
  const features = ref(readJsonStorageItem('features', []))
  const isDeveloper = computed(() => {
    const tenantCode = userInfo.value?.tenantCode
    return typeof tenantCode === 'string' && tenantCode.toLowerCase() === 'super'
  })
  const responseKey = ref(readStorageItem('responseKey'))
  const expireAt = ref(readStorageItem('expireAt'))

  const setLoginInfo = (loginData) => {
    token.value = loginData?.token || ''
    userInfo.value = loginData
      ? {
          userId: loginData.userId,
          userName: loginData.userName,
          tenantCode: loginData.tenantCode
        }
      : null
    permissions.value = loginData?.permissions || []
    features.value = Array.isArray(loginData?.features) ? loginData.features : []
    responseKey.value = loginData?.responseKey || ''
    expireAt.value = loginData?.expireAt ? String(loginData.expireAt) : ''

    legacyStorage.removeItem('token')
    legacyStorage.removeItem('userInfo')
    legacyStorage.removeItem('permissions')
    legacyStorage.removeItem('features')
    legacyStorage.removeItem('responseKey')
    legacyStorage.removeItem('expireAt')
    authStorage.setItem('token', token.value)
    authStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    authStorage.setItem('permissions', JSON.stringify(permissions.value))
    authStorage.setItem('features', JSON.stringify(features.value))
    authStorage.setItem('responseKey', responseKey.value)
    authStorage.setItem('expireAt', expireAt.value)
  }

  const renewSession = ({ token: renewedToken, responseKey: renewedResponseKey, expireAt: renewedExpireAt } = {}) => {
    if (!renewedToken || !renewedResponseKey) {
      return
    }
    token.value = renewedToken
    responseKey.value = renewedResponseKey
    expireAt.value = renewedExpireAt ? String(renewedExpireAt) : expireAt.value

    legacyStorage.removeItem('token')
    legacyStorage.removeItem('responseKey')
    legacyStorage.removeItem('expireAt')
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
    removeLoginStorage()
  }

  const hasPermission = (permCode) => matchPermission(permissions.value, permCode)
  const hasAnyPermission = (permCodes) => matchAnyPermission(permissions.value, permCodes)
  const hasFeature = (featureCode) => {
    if (!featureCode) {
      return true
    }
    if (isDeveloper.value) {
      return true
    }
    // Old sessions do not have feature data yet. Keep base modules usable, but never expose custom modules without an explicit grant.
    if ((!features.value || features.value.length === 0) && String(featureCode).startsWith('module.')) {
      return true
    }
    return features.value.includes(featureCode)
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
    isDeveloper,
    responseKey,
    expireAt,
    hasPermission,
    hasAnyPermission,
    hasFeature,
    hasAnyFeature,
    setLoginInfo,
    renewSession,
    logout
  }
})
