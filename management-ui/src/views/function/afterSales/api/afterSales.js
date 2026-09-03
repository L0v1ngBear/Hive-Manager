import request from '@/utils/request.js'

export const getAfterSalesTickets = (params) => request({ url: '/after-sales/tickets', method: 'get', params })
export const exportAfterSalesTickets = (params) => request({ url: '/after-sales/tickets/export', method: 'get', params, responseType: 'blob' })
export const getAfterSalesTicket = (id) => request({ url: `/after-sales/tickets/${id}`, method: 'get' })
export const getAfterSalesTicketLogisticsTracking = (id) => request({
  url: `/after-sales/tickets/${id}/logistics-tracking`,
  method: 'get',
  silent: true,
  cacheTtl: 30 * 60 * 1000
})
export const saveAfterSalesTicket = (data) => request({ url: '/after-sales/tickets', method: 'post', data })
export const uploadAfterSalesRepairImage = (data) => request({ url: '/after-sales/tickets/repair-image', method: 'post', data })
export const downloadAfterSalesRepairImage = (params) => request({ url: '/after-sales/tickets/repair-image', method: 'get', params, responseType: 'blob' })
export const uploadAfterSalesFollowUpImage = (data) => request({ url: '/after-sales/tickets/follow-up-image', method: 'post', data })
export const downloadAfterSalesFollowUpImage = (params) => request({ url: '/after-sales/tickets/follow-up-image', method: 'get', params, responseType: 'blob' })
export const updateAfterSalesTicketStatus = (data) => request({ url: '/after-sales/tickets/status', method: 'post', data })
export const followUpAfterSalesTicket = (id, data) => request({ url: `/after-sales/tickets/${id}/follow-up`, method: 'post', data })
export const getAfterSalesAssigneeOptions = (params) => request({ url: '/after-sales/assignee-options', method: 'get', params, silent: true })
export const assignAfterSalesTicket = (id, data) => request({ url: `/after-sales/tickets/${id}/assignee`, method: 'post', data })
export const getAfterSalesApprovals = () => request({ url: '/after-sales/approvals', method: 'get' })
export const auditAfterSalesTicket = (id, data) => request({ url: `/after-sales/tickets/${id}/approval`, method: 'post', data })
export const outboundAfterSalesTicket = (id) => request({ url: `/after-sales/tickets/${id}/outbound`, method: 'post' })
export const getAfterSalesParts = (params) => request({ url: '/after-sales/parts', method: 'get', params })
export const saveAfterSalesPart = (data) => request({ url: '/after-sales/parts', method: 'post', data })
export const uploadAfterSalesPartPhoto = (data) => request({ url: '/after-sales/parts/photo', method: 'post', data })
export const stockInAfterSalesPart = (data) => request({ url: '/after-sales/parts/stock-in', method: 'post', data })
export const getAfterSalesOrderOptions = (params) => request({ url: '/after-sales/order-options', method: 'get', params })
export const getAfterSalesCustomerOptions = (params) => request({ url: '/after-sales/customer-options', method: 'get', params })
