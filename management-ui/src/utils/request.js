import axios from 'axios'
import {ElMessage} from 'element-plus'
import router from '@/router'
import {useUserStore} from '@/stores/user'
import { useRequestStatusStore } from '@/stores/requestStatus'
import {decryptPayload} from '@/utils/secure'
import { buildLoginQuery, isLoginPath } from '@/utils/redirect'

const service = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/web',
    timeout: 10000
})

const pendingGetRequests = new Map()
const responseCache = new Map()

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
        startGlobalLoading(config)
        return config
    },
    (error) => Promise.reject(error)
)

service.interceptors.response.use(
    async (response) => {
        finishGlobalLoading(response.config)
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

        applyRenewedSession(response, userStore)

        if (res.code !== 200) {
            if (!response.config?.silent) {
                ElMessage.error(res.msg || '系统异常')
            }
            return Promise.reject(res)
        }
        return res.data
    },
    (error) => {
        finishGlobalLoading(error.config)
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

function request(configOrUrl, extraConfig = {}) {
    const config = normalizeRequestConfig(configOrUrl, extraConfig)
    const method = normalizeMethod(config.method)
    const isGet = method === 'get'
    const shouldDedupe = isGet && config.dedupe !== false
    const shouldCache = isGet && Number(config.cacheTtl) > 0 && !isRefreshRequest(config)
    const requestKey = isGet ? buildRequestKey(config) : ''

    if (shouldCache) {
        const cached = responseCache.get(requestKey)
        if (cached && cached.expiresAt > Date.now()) {
            return Promise.resolve(cached.data)
        }
        responseCache.delete(requestKey)
    }

    if (shouldDedupe && pendingGetRequests.has(requestKey)) {
        return pendingGetRequests.get(requestKey)
    }

    const promise = service(config)
        .then((data) => {
            if (shouldCache) {
                responseCache.set(requestKey, {
                    data,
                    expiresAt: Date.now() + Number(config.cacheTtl)
                })
            }
            if (!isGet) {
                responseCache.clear()
            }
            return data
        })
        .finally(() => {
            if (shouldDedupe) {
                pendingGetRequests.delete(requestKey)
            }
        })

    if (shouldDedupe) {
        pendingGetRequests.set(requestKey, promise)
    }
    return promise
}

function normalizeRequestConfig(configOrUrl, extraConfig) {
    if (typeof configOrUrl === 'string') {
        return { ...extraConfig, url: configOrUrl }
    }
    return configOrUrl ? { ...configOrUrl } : {}
}

function normalizeMethod(method) {
    return String(method || 'get').toLowerCase()
}

function isRefreshRequest(config) {
    return Boolean(config?.params?.refresh || config?.refresh || config?.skipCache)
}

function buildRequestKey(config) {
    return [
        normalizeMethod(config.method),
        config.baseURL || service.defaults.baseURL || '',
        config.url || '',
        stableStringify(cleanValue(config.params) || {}),
        stableStringify(cleanValue(config.data) || {})
    ].join('|')
}

function stableStringify(value) {
    if (value === null || value === undefined) {
        return ''
    }
    if (typeof FormData !== 'undefined' && value instanceof FormData) {
        return '[form-data]'
    }
    if (typeof Blob !== 'undefined' && value instanceof Blob) {
        return '[blob]'
    }
    if (Array.isArray(value)) {
        return `[${value.map((item) => stableStringify(item)).join(',')}]`
    }
    if (typeof value === 'object') {
        return `{${Object.keys(value).sort().map((key) => `${key}:${stableStringify(value[key])}`).join(',')}}`
    }
    return String(value)
}

function startGlobalLoading(config) {
    if (!shouldShowGlobalLoading(config)) {
        return
    }
    const type = isQueryMethod(config.method) ? 'query' : 'mutation'
    config.__globalLoadingType = type
    useRequestStatusStore().start(type)
}

function finishGlobalLoading(config) {
    if (!config?.__globalLoadingType) {
        return
    }
    useRequestStatusStore().finish(config.__globalLoadingType)
    config.__globalLoadingType = undefined
}

function shouldShowGlobalLoading(config) {
    return !config?.silent && config?.showGlobalLoading !== false
}

function isQueryMethod(method) {
    const normalized = normalizeMethod(method)
    return normalized === 'get' || normalized === 'head' || normalized === 'options'
}

function applyRenewedSession(response, userStore) {
    const renewedToken = getResponseHeader(response.headers, 'X-Auth-Renewed-Token')
    const renewedResponseKey = getResponseHeader(response.headers, 'X-Auth-Renewed-Response-Key')
    const renewedExpireAt = getResponseHeader(response.headers, 'X-Auth-Renewed-Expire-At')
    userStore.renewSession({
        token: renewedToken,
        responseKey: renewedResponseKey,
        expireAt: renewedExpireAt
    })
}

function getResponseHeader(headers, name) {
    if (!headers || !name) {
        return ''
    }
    if (typeof headers.get === 'function') {
        return headers.get(name) || headers.get(name.toLowerCase()) || ''
    }
    const matchedKey = Object.keys(headers).find((key) => key.toLowerCase() === name.toLowerCase())
    return matchedKey ? headers[matchedKey] : ''
}

export default request
