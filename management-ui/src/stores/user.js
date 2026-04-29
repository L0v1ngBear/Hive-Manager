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
  ;['token', 'userInfo', 'permissions', 'responseKey'].forEach((key) => {
    authStorage.removeItem(key)
    legacyStorage.removeItem(key)
  })
}

export const useUserStore = defineStore('user', () => {
  const token = ref(readStorageItem('token'))
  const userInfo = ref(readJsonStorageItem('userInfo', null))
  const permissions = ref(readJsonStorageItem('permissions', []))
  const isDeveloper = computed(() => {
    const tenantCode = userInfo.value?.tenantCode
    return typeof tenantCode === 'string' && tenantCode.toLowerCase() === 'super'
  })
  const responseKey = ref(readStorageItem('responseKey'))

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
    responseKey.value = loginData?.responseKey || ''

    legacyStorage.removeItem('token')
    legacyStorage.removeItem('userInfo')
    legacyStorage.removeItem('permissions')
    legacyStorage.removeItem('responseKey')
    authStorage.setItem('token', token.value)
    authStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    authStorage.setItem('permissions', JSON.stringify(permissions.value))
    authStorage.setItem('responseKey', responseKey.value)
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    responseKey.value = ''
    removeLoginStorage()
  }

  const hasPermission = (permCode) => matchPermission(permissions.value, permCode)
  const hasAnyPermission = (permCodes) => matchAnyPermission(permissions.value, permCodes)

  return {
    token,
    userInfo,
    permissions,
    isDeveloper,
    responseKey,
    hasPermission,
    hasAnyPermission,
    setLoginInfo,
    logout
  }
})
