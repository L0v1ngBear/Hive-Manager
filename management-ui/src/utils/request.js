import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'
import { decryptPayload } from '@/utils/secure'

const service = axios.create({
  baseURL: '/web',
  timeout: 10000
})

function isEncryptedPayload(data) {
  return Boolean(data && typeof data === 'object' && data.iv && data.ciphertext && data.mac)
}

service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    config.headers = config.headers || {}
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
      if (userStore.responseKey && config.responseType !== 'blob' && config.url !== '/auth/login') {
        config.headers['X-Response-Encrypt'] = '1'
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  async (response) => {
    if (response.config?.responseType === 'blob') {
      return response.data
    }
    const userStore = useUserStore()
    const res = response.data
    if (isEncryptedPayload(res?.data) && userStore.responseKey) {
      try {
        res.data = await decryptPayload(userStore.responseKey, res.data)
      } catch (error) {
        ElMessage.error('响应解密失败，请重新登录后再试')
        userStore.logout()
        if (router.currentRoute.value.fullPath !== '/login') {
          router.push('/login')
        }
        return Promise.reject(error)
      }
    }
    if (res.code !== 200) {
      ElMessage.error(res.msg || '系统异常')
      return Promise.reject(res)
    }
    return res.data
  },
  (error) => {
    const userStore = useUserStore()
    const status = error.response?.status
    const currentPath = router.currentRoute.value.fullPath

    if (status === 401) {
      userStore.logout()
      if (currentPath !== '/login') {
        ElMessage.error(error.response?.data?.msg || '登录状态已失效，请重新登录')
        router.push({ path: '/login', query: currentPath ? { redirect: currentPath } : {} })
      }
    } else if (status === 403) {
      ElMessage.warning(error.response?.data?.msg || '您暂无权限访问当前功能，如需开通请联系管理员')
    } else if (status === 400) {
      ElMessage.error(error.response?.data?.msg || '非法请求')
    } else {
      ElMessage.error(error.response?.data?.msg || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default service
