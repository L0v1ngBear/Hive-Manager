function leaves(nodes = []) {
  return nodes.flatMap((node) => node.children?.length ? leaves(node.children) : [{
    id: Number(node.id),
    name: node.permName || node.label || node.permCode || String(node.id),
    code: node.permCode || ''
  }])
}

export function permissionGroups(tree = [], keyword = '', selectedOnly = false, selectedIds = []) {
  const needle = String(keyword).trim().toLowerCase()
  const selected = new Set((selectedIds || []).map(Number))
  return tree.map((node) => {
    const permissions = leaves(node.children?.length ? node.children : [node]).filter((item) => {
      if (selectedOnly && !selected.has(item.id)) return false
      return !needle || `${item.name} ${item.code}`.toLowerCase().includes(needle)
    })
    return { id: Number(node.id), name: node.permName || node.label || '未分组', permissions }
  }).filter((group) => group.permissions.length)
}

export const groupLeafIds = (group) => [...new Set((group?.permissions || []).map((item) => Number(item.id)))]
