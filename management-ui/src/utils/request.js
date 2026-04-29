import axios from 'axios'
import {ElMessage} from 'element-plus'
import router from '@/router'
import {useUserStore} from '@/stores/user'
import {decryptPayload} from '@/utils/secure'
import { buildLoginQuery, isLoginPath } from '@/utils/redirect'

const service = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/web',
    timeout: 10000
})

function cleanValue(value) {
    if (value === undefined || value === null) {
        return undefined
    }
    if (typeof value === 'string') {
        const normalized = value.trim().toLowerCase()
        return normalized === 'undefined' || normalized === 'null' ? undefined : value
    }
    if (Array.isArray(value)) {
        return value.map((item) => cleanValue(item)).filter((item) => item !== undefined)
    }
    if (value instanceof Date) {
        return value
    }
    if (typeof FormData !== 'undefined' && value instanceof FormData) {
        return value
    }
    if (typeof Blob !== 'undefined' && value instanceof Blob) {
        return value
    }
    if (typeof value !== 'object') {
        return value
    }
    return Object.fromEntries(
        Object.entries(value)
            .map(([key, item]) => [key, cleanValue(item)])
            .filter(([, item]) => item !== undefined)
    )
}

service.interceptors.request.use(
    (config) => {
        const userStore = useUserStore()
        config.headers = config.headers || {}
        config.params = cleanValue(config.params) || undefined
        config.data = cleanValue(config.data)
        if (userStore.token) {
            config.headers.Authorization = `Bearer ${userStore.token}`
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

        try {
            res.data = await decryptPayload(userStore.responseKey, res.data)
        } catch (error) {
            ElMessage.error('响应解密失败，请稍后重试')
            if (userStore.token) {
                userStore.logout()
                if (!isLoginPath(router.currentRoute.value.fullPath)) {
                    router.push('/login')
                }
            }
            return Promise.reject(error)
        }

        if (res.code !== 200) {
            if (!response.config?.silent) {
                ElMessage.error(res.msg || '系统异常')
            }
            return Promise.reject(res)
        }
        return res.data
    },
    (error) => {
        const userStore = useUserStore()
        const status = error.response?.status
        const currentPath = router.currentRoute.value.fullPath

        if (error.config?.silent) {
            return Promise.reject(error)
        }

        if (status === 401) {
            userStore.logout()
            if (!isLoginPath(currentPath)) {
                ElMessage.error(error.response?.data?.msg || '登录状态已失效，请重新登录')
                router.push({path: '/login', query: buildLoginQuery(currentPath)})
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
