import request from '@/utils/request.js'

const base = (ticketId) => `/after-sales/tickets/${encodeURIComponent(ticketId)}/treatments`
export const getTreatments = ticketId => request({ url: base(ticketId), method: 'get' })
export const createTreatment = (ticketId, data) => request({ url: base(ticketId), method: 'post', data })
export const updateTreatment = (ticketId, id, data) => request({ url: `${base(ticketId)}/${id}`, method: 'put', data })
export const advanceTreatment = (ticketId, id, action, data) => request({ url: `${base(ticketId)}/${id}/${action}`, method: 'post', data })
export const getTreatmentTodos = (params, options = {}) => request({ url: '/after-sales/treatment-todos', method: 'get', params, ...options })
