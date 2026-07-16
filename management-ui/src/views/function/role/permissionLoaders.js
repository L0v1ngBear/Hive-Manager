import { reactive } from 'vue'

function unwrapArray(response) {
  return Array.isArray(response) ? response : []
}

function isForbidden(error) {
  const status = Number(error?.code ?? error?.response?.data?.code ?? error?.response?.status ?? error?.status)
  return status === 401 || status === 403
}

export function permissionTreeCanSubmit(loadState) {
  return loadState === 'ready' || loadState === 'empty'
}

export function syncCommittedPermissionIds(currentIds, result) {
  return result?.committed ? [...result.checkedPermissionIds] : currentIds
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
      if (!ownsState()) return { committed: false }
      const treeData = unwrapArray(permissionsRes)
      state.treeData = treeData
      await afterTreeReady()
      if (!ownsState()) return { committed: false }
      state.checkedPermissionIds = unwrapArray(ownedIdsRes).map((id) => Number(id))
      state.loadState = treeData.length ? 'ready' : 'empty'
      return {
        committed: true,
        checkedPermissionIds: [...state.checkedPermissionIds]
      }
    } catch (error) {
      if (!ownsState()) return { committed: false }
      state.treeData = []
      state.checkedPermissionIds = []
      state.loadState = isForbidden(error) ? 'forbidden' : 'failed'
      return { committed: true, checkedPermissionIds: [] }
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
