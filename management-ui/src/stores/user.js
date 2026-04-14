import { defineStore } from 'pinia'
import { ref } from 'vue'
import { hasAnyPermission as matchAnyPermission, hasPermission as matchPermission } from '@/utils/permission'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const permissions = ref(JSON.parse(localStorage.getItem('permissions') || '[]'))
  const isDeveloper = ref(JSON.parse(localStorage.getItem('isDeveloper') || 'false'))
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
    isDeveloper.value = Boolean(loginData?.developer)
    permissions.value = loginData?.permissions || []
    responseKey.value = loginData?.responseKey || ''

    localStorage.setItem('token', token.value)
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    localStorage.setItem('permissions', JSON.stringify(permissions.value))
    localStorage.setItem('isDeveloper', JSON.stringify(isDeveloper.value))
    localStorage.setItem('responseKey', responseKey.value)
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    isDeveloper.value = false
    responseKey.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('permissions')
    localStorage.removeItem('isDeveloper')
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
