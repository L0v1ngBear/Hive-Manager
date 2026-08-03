function normalizeDocumentId(value) {
  const id = Number(value || 0)
  return Number.isFinite(id) && id > 0 ? id : 0
}

function createsFolderCycle(node, parent, nodesById) {
  const visited = new Set([node.id])
  let current = parent
  while (current) {
    if (visited.has(current.id)) return true
    visited.add(current.id)
    current = nodesById.get(current.parentId)
  }
  return false
}

export function buildDocumentFolderTree(folders = []) {
  const nodesById = new Map()
  for (const folder of folders) {
    const id = normalizeDocumentId(folder?.id)
    if (!id || nodesById.has(id)) continue
    nodesById.set(id, {
      id,
      parentId: normalizeDocumentId(folder?.parentId),
      label: String(folder?.name || '未命名文件夹'),
      children: []
    })
  }

  const roots = []
  for (const node of nodesById.values()) {
    const parent = nodesById.get(node.parentId)
    if (!parent || parent.id === node.id || createsFolderCycle(node, parent, nodesById)) {
      roots.push(node)
    } else {
      parent.children.push(node)
    }
  }

  const sortNodes = (nodes) => {
    nodes.sort((left, right) => left.label.localeCompare(right.label, 'zh-CN'))
    nodes.forEach((node) => sortNodes(node.children))
    return nodes
  }

  return sortNodes(roots)
}

export function canMoveDocumentToFolder(document, targetParentId, folders = []) {
  const documentId = normalizeDocumentId(document?.id)
  const currentParentId = normalizeDocumentId(document?.parentId)
  const targetId = normalizeDocumentId(targetParentId)
  if (!documentId || currentParentId === targetId) return false
  if (Number(document?.type) !== 0) return true

  const parentById = new Map()
  folders.forEach((folder) => {
    const id = normalizeDocumentId(folder?.id)
    if (id) parentById.set(id, normalizeDocumentId(folder?.parentId))
  })

  const visited = new Set()
  let cursor = targetId
  while (cursor > 0 && !visited.has(cursor)) {
    if (cursor === documentId) return false
    visited.add(cursor)
    cursor = parentById.get(cursor) || 0
  }
  return true
}

export function pushExplorerLocation(history = [0], currentIndex = 0, targetId = 0) {
  const normalizedTarget = normalizeDocumentId(targetId)
  const normalizedHistory = history.length ? history.map(normalizeDocumentId) : [0]
  const safeIndex = Math.min(Math.max(Number(currentIndex) || 0, 0), normalizedHistory.length - 1)
  if (normalizedHistory[safeIndex] === normalizedTarget) {
    return { history: normalizedHistory, index: safeIndex }
  }
  const nextHistory = normalizedHistory.slice(0, safeIndex + 1)
  nextHistory.push(normalizedTarget)
  return { history: nextHistory, index: nextHistory.length - 1 }
}

export function stepExplorerLocation(history = [0], currentIndex = 0, direction = -1) {
  const normalizedHistory = history.length ? history.map(normalizeDocumentId) : [0]
  const safeIndex = Math.min(Math.max(Number(currentIndex) || 0, 0), normalizedHistory.length - 1)
  const nextIndex = Math.min(Math.max(safeIndex + Math.sign(Number(direction) || -1), 0), normalizedHistory.length - 1)
  return {
    targetId: normalizedHistory[nextIndex],
    index: nextIndex,
    changed: nextIndex !== safeIndex
  }
}
