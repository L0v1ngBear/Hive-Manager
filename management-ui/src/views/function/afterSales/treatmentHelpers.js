export const treatmentTypes = { resend_parts: '补发配件', motor_replacement: '更换电机', parts_and_motor: '补发配件并更换电机' }
export const treatmentStates = { waiting_outbound: '待配件出库', processing: '处理中', waiting_follow_up: '待回访', resolved: '回访已解决', unresolved: '回访未解决，需继续处理' }
export const treatmentTodoRoute = record => ({ path: '/function/after-sales', query: { tab: 'my-tasks', ticketId: String(record.ticketId), treatmentId: String(record.id) } })
export function blankTreatment() {
  return { requestKey: crypto.randomUUID(), treatmentType: 'resend_parts', description: '', logisticsCompany: '', waybillNo: '', manufacturerReturnLogisticsCompany: '', manufacturerReturnWaybillNo: '', oldMotorInfo: '', newMotorModel: '', motorQuantity: 1, returnOldMotorQuantity: 0, parts: [], repairImages: [] }
}
export function validateTreatment(form) {
  if (!form.description?.trim()) return '请填写本次处理说明'
  if (form.treatmentType !== 'resend_parts' && (!form.newMotorModel?.trim() || !(form.motorQuantity > 0))) return '请填写新电机型号和数量'
  if (form.treatmentType !== 'motor_replacement' && !form.parts?.length) return '请添加补发配件'
  const seen = new Set()
  for (const part of form.parts || []) {
    if (!part.partId || !(part.quantity > 0) || !Number.isInteger(part.quantity)) return '请选择配件并填写正整数数量'
    if (seen.has(part.partId)) return '同一配件请合并为一行'
    seen.add(part.partId)
  }
  return ''
}
