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
        const userStore = useUserStore()
        if (response.config?.responseType === 'blob') {
            await assertBlobResponseOk(response, userStore)
            applyRenewedSession(response, userStore)
            return response.data
        }

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
            handleBusinessError(res, response.config, userStore)
            return Promise.reject(res)
        }
        return res.data
    },
    (error) => {
        finishGlobalLoading(error.config)
        const userStore = useUserStore()
        const currentPath = router.currentRoute.value.fullPath

        if (error.config?.silent) {
            return Promise.reject(error)
        }

        handleHttpError(error, userStore, currentPath)
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

function normalizeMessage(message) {
    const text = String(message || '').trim()
    return text && text !== 'success' ? text : ''
}

function resolveBusinessTip({ code, status, message }) {
    const bizCode = Number(code || status || 0)
    const baseMessage = normalizeMessage(message)
    if (bizCode === 401) {
        return {
            level: 'error',
            message: baseMessage || '登录状态已失效，请重新登录后继续操作'
        }
    }
    if (bizCode === 403) {
        if (/平台账号|租户管理/.test(baseMessage)) {
            return {
                level: 'warning',
                message: `访问范围受限：${baseMessage || '当前账号不能访问该功能'}`
            }
        }
        if (/组织|租户|套餐|授权|到期|停用/.test(baseMessage) && !/权限|无权/.test(baseMessage)) {
            return {
                level: 'warning',
                message: `组织状态受限：${baseMessage || '当前组织暂不能使用该功能'}`
            }
        }
        return {
            level: 'warning',
            message: `权限不足：${baseMessage || '当前账号没有该功能权限'}。如需使用，请联系管理员在角色管理中分配权限。`
        }
    }
    if (bizCode === 400) {
        return {
            level: 'error',
            message: baseMessage || '提交内容不完整或格式不正确，请检查后再试'
        }
    }
    if (bizCode >= 500) {
        return {
            level: 'error',
            message: baseMessage || '系统处理失败，请稍后重试；如持续出现，请联系管理员处理'
        }
    }
    return {
        level: 'error',
        message: baseMessage || '操作失败，请稍后重试'
    }
}

function showBusinessTip(tip) {
    const message = tip?.message || '操作失败，请稍后重试'
    const level = tip?.level || 'error'
    ElMessage[level]({
        message,
        duration: level === 'warning' ? 5000 : 4000,
        showClose: true
    })
}

function handleBusinessError(res, config, userStore) {
    if (config?.silent) {
        return
    }
    const tip = resolveBusinessTip({ code: res?.code, message: res?.msg || res?.message })
    if (Number(res?.code) === 401) {
        const currentPath = router.currentRoute.value.fullPath
        userStore.logout()
        if (!isLoginPath(currentPath)) {
            showBusinessTip(tip)
            router.push({ path: '/login', query: buildLoginQuery(currentPath) })
        }
        return
    }
    showBusinessTip(tip)
}

function handleHttpError(error, userStore, currentPath) {
    const status = error.response?.status
    const payload = error.response?.data || {}
    const tip = resolveBusinessTip({
        code: payload.code,
        status,
        message: payload.msg || payload.message || error.message
    })
    if (status === 401) {
        userStore.logout()
        if (!isLoginPath(currentPath)) {
            showBusinessTip(tip)
            router.push({ path: '/login', query: buildLoginQuery(currentPath) })
        }
        return
    }
    showBusinessTip(tip)
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

async function assertBlobResponseOk(response, userStore) {
    const blob = response.data
    if (typeof Blob === 'undefined' || !(blob instanceof Blob)) {
        return
    }
    if (blob.size === 0) {
        const msg = '文件下载失败，系统返回了空文件'
        ElMessage.error(msg)
        throw createDisplayedError(msg)
    }
    const contentType = getBlobContentType(response, blob)
    const isErrorPayload = contentType.includes('application/json')
        || contentType.startsWith('text/')
        || contentType.includes('html')
    if (!isErrorPayload) {
        return
    }
    const msg = await resolveBlobErrorMessage(blob, userStore)
    ElMessage.error(msg)
    throw createDisplayedError(msg)
}

function createDisplayedError(message) {
    const error = new Error(message)
    error.__shown = true
    return error
}

function getBlobContentType(response, blob) {
    return String(getResponseHeader(response.headers, 'Content-Type') || blob.type || '').toLowerCase()
}

async function resolveBlobErrorMessage(blob, userStore) {
    const text = await blob.text()
    if (!text) {
        return '文件下载失败，系统未返回有效文件'
    }
    try {
        const payload = JSON.parse(text)
        if (payload?.data) {
            try {
                payload.data = await decryptPayload(userStore.responseKey, payload.data)
            } catch {
                // Blob errors may already be plain JSON; keep the original message if decrypt is not needed.
            }
        }
        return payload?.msg
            || payload?.message
            || payload?.data?.msg
            || payload?.data?.message
            || '文件下载失败，请稍后重试'
    } catch {
        return text.replace(/\s+/g, ' ').trim().slice(0, 160) || '文件下载失败，请稍后重试'
    }
}

export default request
