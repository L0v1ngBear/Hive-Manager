import { reactive } from 'vue'

function unwrapArray(response) {
  const value = response?.data?.data ?? response?.data ?? response ?? []
  return Array.isArray(value) ? value : []
}

function isForbidden(error) {
  const status = Number(error?.response?.status ?? error?.code)
  return status === 401 || status === 403
}

export function permissionTreeCanSubmit(loadState) {
  return loadState === 'ready' || loadState === 'empty'
}

export function createRolePermissionLoader({ getAllPermissions, getRolePermissionIds, afterTreeReady = async () => {} }) {
  const state = reactive({
    loading: false,
    loadState: 'idle',
    treeData: [],
    checkedPermissionIds: []
  })
  let latestRequestId = 0
  let latestRoleId = null

  async function load(roleId) {
    const requestId = ++latestRequestId
    latestRoleId = roleId
    state.treeData = []
    state.checkedPermissionIds = []
    state.loadState = 'loading'
    state.loading = true

    const ownsState = () => requestId === latestRequestId && roleId === latestRoleId
    try {
      const [permissionsRes, ownedIdsRes] = await Promise.all([
        getAllPermissions(),
        getRolePermissionIds(roleId)
      ])
      if (!ownsState()) return
      const treeData = unwrapArray(permissionsRes)
      state.treeData = treeData
      await afterTreeReady()
      if (!ownsState()) return
      state.checkedPermissionIds = unwrapArray(ownedIdsRes).map((id) => Number(id))
      state.loadState = treeData.length ? 'ready' : 'empty'
    } catch (error) {
      if (!ownsState()) return
      state.treeData = []
      state.checkedPermissionIds = []
      state.loadState = isForbidden(error) ? 'forbidden' : 'failed'
    } finally {
      if (ownsState()) state.loading = false
    }
  }

  return { state, load }
}

export function createPermissionTreeLoader({ getAllPermissions }) {
  const state = reactive({ loading: false, loadState: 'idle', treeData: [] })
  let latestRequestId = 0

  async function load() {
    const requestId = ++latestRequestId
    state.loading = true
    state.loadState = 'loading'
    state.treeData = []
    try {
      const response = await getAllPermissions()
      if (requestId !== latestRequestId) return
      state.treeData = unwrapArray(response)
      state.loadState = state.treeData.length ? 'ready' : 'empty'
    } catch (error) {
      if (requestId !== latestRequestId) return
      state.treeData = []
      state.loadState = isForbidden(error) ? 'forbidden' : 'failed'
    } finally {
      if (requestId === latestRequestId) state.loading = false
    }
  }

  return { state, load }
}
