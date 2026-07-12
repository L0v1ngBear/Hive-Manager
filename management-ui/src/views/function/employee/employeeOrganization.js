const BOSS_KEYWORDS = ['老板', '董事长', '总经理', '首席执行官', 'CEO']

const normalizedEmployeeText = (employee) =>
  `${employee?.name || ''} ${employee?.positionName || ''}`.trim().toLocaleUpperCase()

const hasBossKeyword = (employee) => {
  const text = normalizedEmployeeText(employee)
  return BOSS_KEYWORDS.some((keyword) => text.includes(keyword.toLocaleUpperCase()))
}

const descendantCount = (employee) => (employee?.children || []).reduce(
  (total, child) => total + 1 + descendantCount(child),
  0
)

const selectOrganizationRoot = (roots) => roots
  .map((employee, index) => ({
    employee,
    index,
    bossKeyword: hasBossKeyword(employee) ? 1 : 0,
    descendants: descendantCount(employee)
  }))
  .sort((left, right) =>
    right.bossKeyword - left.bossKeyword ||
    right.descendants - left.descendants ||
    left.index - right.index
  )[0]?.employee

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

  const organizationRoot = selectOrganizationRoot(roots)
  const otherRoots = roots.filter((item) => item !== organizationRoot)
  const displayRoot = {
    ...organizationRoot,
    children: [...(organizationRoot.children || []), ...otherRoots]
  }

  return {
    data: toOrganizationNode(displayRoot, null, true),
    topLevelCount: 1,
    unassignedCount: otherRoots.length
  }
}
