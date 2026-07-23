const toOrganizationNode = (employee, parentId = null, isOrganizationRoot = false) => {
  const id = String(employee.id)
  return {
    ...employee,
    id,
    pid: parentId,
    label: employee.name || '--',
    expand: true,
    isOrganizationRoot,
    children: (employee.children || []).map((child) => toOrganizationNode(child, id, false))
  }
}

export const buildEmployeeHierarchy = (source = []) => {
  const nodes = source.map((item) => ({ ...item, children: [] }))
  const byId = new Map(nodes.map((item) => [String(item.id), item]))
  const byName = new Map(nodes.filter((item) => item.name).map((item) => [item.name, item]))
  const roots = []

  nodes.forEach((node) => {
    const leaderId = node.leaderId == null ? '' : String(node.leaderId)
    const leader = leaderId ? byId.get(leaderId) : (node.leaderName ? byName.get(node.leaderName) : null)

    if (leader && String(leader.id) !== String(node.id)) {
      leader.children.push(node)
    } else {
      roots.push(node)
    }
  })

  return roots
}

export const buildOrganizationChart = (roots = []) => {
  if (!roots.length) {
    return {
      data: { id: 'empty', pid: null, label: '暂无数据', expand: true, children: [] },
      topLevelCount: 0,
      unassignedCount: 0
    }
  }

  if (roots.length === 1) {
    return {
      data: toOrganizationNode(roots[0], null, true),
      topLevelCount: 1,
      unassignedCount: 0
    }
  }

  return {
    data: {
      id: 'organization-root',
      pid: null,
      label: '组织架构',
      expand: true,
      isVirtualRoot: true,
      children: roots.map((employee) => toOrganizationNode(employee, 'organization-root', false))
    },
    topLevelCount: roots.length,
    unassignedCount: roots.length
  }
}
