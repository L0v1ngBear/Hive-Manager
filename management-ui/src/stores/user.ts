import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useUserStore = defineStore('user', () => {
  // 1. 定义状态 (State)
  // 优先从 localStorage 读取，防止刷新页面后状态丢失
  const token = ref<string>(localStorage.getItem('token') || '');
  const tenantCode = ref<string>(localStorage.getItem('tenantCode') || '');
  const userId = ref<string>(localStorage.getItem('userId') || '');
  const permissions = ref<string[]>(JSON.parse(localStorage.getItem('permissions') || '[]'));

  // 2. 定义动作 (Actions)
  const setToken = (newToken: string) => {
    token.value = newToken;
    localStorage.setItem('token', newToken);
  };

  const setTenantCode = (newTenantCode: string) => {
    tenantCode.value = newTenantCode;
    localStorage.setItem('tenantCode', newTenantCode);
  };

  const setUserId = (newUserId: string) => {
    userId.value = newUserId;
    localStorage.setItem('userId', newUserId);
  };

  const setPermissions = (newPermissions: string[]) => {
    permissions.value = newPermissions;
    localStorage.setItem('permissions', JSON.stringify(newPermissions));
  };

  // 退出登录时清除所有状态
  const logout = () => {
    token.value = '';
    tenantCode.value = '';
    userId.value = '';
    permissions.value = [];
    localStorage.removeItem('token');
    localStorage.removeItem('tenantCode');
    localStorage.removeItem('userId');
    localStorage.removeItem('permissions');
  };

  // 3. 暴露给外部使用
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
  };
});
