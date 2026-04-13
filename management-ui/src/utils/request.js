import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
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
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    const res = response.data
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
        ElMessage.error(error.response?.data?.msg || '登录状态已失效')
        router.push({ path: '/login', query: currentPath ? { redirect: currentPath } : {} })
      }
    } else if (status === 403) {
      ElMessage.error(error.response?.data?.msg || '暂无权限')
    } else if (status === 400) {
      ElMessage.error(error.response?.data?.msg || '非法请求')
    } else {
      ElMessage.error(error.response?.data?.msg || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default service