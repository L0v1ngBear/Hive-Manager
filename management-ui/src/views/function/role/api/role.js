import request from '@/utils/request.js'

/**
 * 获取角色分页列表
 * @param {Object} params - 查询参数 (如: page, size, keyword)
 */
export function getRolePage(params) {
  return request({
    url: '/sys/role/page', // 替换为你真实的后端Controller路径
    method: 'get',
    params
  })
}

/**
 * 获取近期操作动态
 */
export function getRecentActivities() {
  return request({
    url: '/sys/activity/recent',
    method: 'get'
  })
}

/**
 * 删除角色
 * @param {Number} id - 角色ID
 */
export function deleteRole(id) {
  return request({
    url: `/sys/role/${id}`,
    method: 'delete'
  })
}
