import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const service = axios.create({
  baseURL: '/web',
  timeout: 10000
})

service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    config.headers = config.headers || {}

    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    if (userStore.tenantCode) {
      config.headers['Tenant-Code'] = userStore.tenantCode
    }
    if (userStore.userId) {
      config.headers['User-Id'] = userStore.userId
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '系统异常')
      return Promise.reject(new Error(res.msg || 'Error'))
    }
    return res.data
  },
  (error) => {
    if (error.response?.status === 403) {
      ElMessage.error('暂无权限')
    } else if (error.response?.status === 400) {
      ElMessage.error(error.response?.data?.msg || '非法请求')
    } else {
      ElMessage.error('网络错误')
    }
    return Promise.reject(error)
  }
)

export default service
