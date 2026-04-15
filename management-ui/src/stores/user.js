import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { hasAnyPermission as matchAnyPermission, hasPermission as matchPermission } from '@/utils/permission'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const permissions = ref(JSON.parse(localStorage.getItem('permissions') || '[]'))
  const isDeveloper = computed(() => {
    const tenantCode = userInfo.value?.tenantCode
    return typeof tenantCode === 'string' && tenantCode.toLowerCase() === 'super'
  })
  const responseKey = ref(localStorage.getItem('responseKey') || '')

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

    localStorage.setItem('token', token.value)
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    localStorage.setItem('permissions', JSON.stringify(permissions.value))
    localStorage.setItem('responseKey', responseKey.value)
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    responseKey.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('permissions')
    localStorage.removeItem('responseKey')
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
