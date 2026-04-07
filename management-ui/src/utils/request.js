import axios, { type InternalAxiosRequestConfig, type AxiosResponse } from 'axios';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/stores/user'; // Pinia store

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api', // 后端接口前缀
  timeout: 10000,
});

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore();
    // 自动携带 Token
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`;
    }
    // ⚠️ 核心多租户与用户标识拦截 (完美对应你后端的 TenantInterceptor)
    if (userStore.tenantCode) {
      config.headers['Tenant-Code'] = userStore.tenantCode;
    }
    if (userStore.userId) {
      config.headers['User-Id'] = userStore.userId;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data;
    // 对应你后端的 ResultDTO
    if (res.code !== 200) {
      ElMessage.error(res.msg || '系统错误');
      return Promise.reject(new Error(res.msg || 'Error'));
    }
    return res.data; // 直接剥离出 data 层
  },
  (error) => {
    if (error.response?.status === 403) {
      ElMessage.error('权限不足，禁止访问');
    } else if (error.response?.status === 400) {
      ElMessage.error(error.response.data?.msg || '参数错误');
    } else {
      ElMessage.error('网络连接异常');
    }
    return Promise.reject(error);
  }
);

export default service;
