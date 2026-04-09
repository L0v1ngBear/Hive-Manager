import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const tenantCode = ref(localStorage.getItem('tenantCode') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const permissions = ref(JSON.parse(localStorage.getItem('permissions') || '[]'))

  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setTenantCode = (newTenantCode) => {
    tenantCode.value = newTenantCode
    localStorage.setItem('tenantCode', newTenantCode)
  }

  const setUserId = (newUserId) => {
    userId.value = newUserId
    localStorage.setItem('userId', newUserId)
  }

  const setPermissions = (newPermissions) => {
    permissions.value = newPermissions
    localStorage.setItem('permissions', JSON.stringify(newPermissions))
  }

  const logout = () => {
    token.value = ''
    tenantCode.value = ''
    userId.value = ''
    permissions.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('tenantCode')
    localStorage.removeItem('userId')
    localStorage.removeItem('permissions')
  }

  return {
    token,
    tenantCode,
    userId,
    permissions,
    setToken,
    setTenantCode,
    setUserId,
    setPermissions,
    logout
  }
})
