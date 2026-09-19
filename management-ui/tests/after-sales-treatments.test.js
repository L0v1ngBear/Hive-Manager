import test from 'node:test'
import assert from 'node:assert/strict'
import { treatmentTodoRoute, validateTreatment, blankTreatment } from '../src/views/function/afterSales/treatmentHelpers.js'

test('each processing record has a distinct target and intent', () => {
  assert.notEqual(blankTreatment().requestKey, blankTreatment().requestKey)
  assert.deepEqual(treatmentTodoRoute({ ticketId: 1, id: 9 }).query, { tab: 'my-tasks', ticketId: '1', treatmentId: '9' })
})
test('parts and motor can be processed together without changing warranty', () => {
  const form = { ...blankTreatment(), description: '第二次处理', treatmentType: 'parts_and_motor', newMotorModel: 'M1', parts: [{ partId: 1, quantity: 2 }] }
  assert.equal(validateTreatment(form), '')
  assert.equal('openingDate' in form, false)
  assert.notEqual(validateTreatment({ ...form, parts: [] }), '')
  assert.notEqual(validateTreatment({ ...form, newMotorModel: '' }), '')
  assert.notEqual(validateTreatment({ ...form, parts: [{ partId: 1, quantity: 2 }, { partId: 1, quantity: 1 }] }), '')
})
