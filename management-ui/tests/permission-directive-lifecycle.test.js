import assert from 'node:assert/strict'
import test from 'node:test'

import { createPermissionDirectiveLifecycle } from '../src/directives/permissionState.js'

class FakeClassList {
  constructor(classes = []) {
    this.classes = new Set(classes)
  }

  add(className) {
    this.classes.add(className)
  }

  contains(className) {
    return this.classes.has(className)
  }

  remove(className) {
    this.classes.delete(className)
  }

  values() {
    return [...this.classes].sort()
  }
}

class FakeElement {
  constructor({ attributes = {}, classes = [], cursor = '', disabled = false } = {}) {
    this.attributes = new Map(Object.entries(attributes))
    this.classList = new FakeClassList(classes)
    this.disabled = disabled
    this.listeners = new Map()
    this.style = { cursor }
  }

  addEventListener(type, handler, capture) {
    const listeners = this.listeners.get(type) || []
    listeners.push({ capture, handler })
    this.listeners.set(type, listeners)
  }

  getAttribute(name) {
    return this.attributes.has(name) ? this.attributes.get(name) : null
  }

  listenerCount(type) {
    return (this.listeners.get(type) || []).length
  }

  removeAttribute(name) {
    this.attributes.delete(name)
  }

  removeEventListener(type, handler, capture) {
    const listeners = this.listeners.get(type) || []
    this.listeners.set(type, listeners.filter((listener) => (
      listener.handler !== handler || listener.capture !== capture
    )))
  }

  setAttribute(name, value) {
    this.attributes.set(name, String(value))
  }

  dispatch(type, { key } = {}) {
    const event = {
      defaultPrevented: false,
      immediatePropagationStopped: false,
      key,
      type,
      preventDefault() {
        this.defaultPrevented = true
      },
      stopImmediatePropagation() {
        this.immediatePropagationStopped = true
      }
    }
    for (const { handler } of [...(this.listeners.get(type) || [])]) {
      handler(event)
      if (event.immediatePropagationStopped) break
    }
    return event
  }
}

function elementState(element) {
  return {
    ariaDisabled: element.getAttribute('aria-disabled'),
    classes: element.classList.values(),
    cursor: element.style.cursor,
    disabled: element.disabled,
    title: element.getAttribute('title')
  }
}

test('allowed mount and updates leave business state untouched', () => {
  const directive = createPermissionDirectiveLifecycle({ isAllowed: Boolean })
  const element = new FakeElement({
    attributes: { 'aria-disabled': 'false', title: 'Business title' },
    classes: ['business-disabled'],
    cursor: 'wait',
    disabled: true
  })
  const before = elementState(element)

  directive.mounted(element, { value: true })
  directive.updated(element, { value: true })

  assert.deepEqual(elementState(element), before)
  assert.equal(element.listenerCount('click'), 0)
  assert.equal(element.listenerCount('keydown'), 0)
})

test('allowed to denied to allowed restores the immediate pre-denial state', () => {
  const directive = createPermissionDirectiveLifecycle({ isAllowed: Boolean })
  const element = new FakeElement({ classes: ['business-class'] })
  directive.mounted(element, { value: true })

  element.disabled = true
  element.setAttribute('aria-disabled', 'false')
  element.setAttribute('title', 'Reactive business title')
  element.style.cursor = 'wait'
  const beforeDenial = elementState(element)

  directive.updated(element, { value: false })
  assert.equal(element.disabled, true, 'permission denial must not mutate native disabled')
  assert.equal(element.getAttribute('aria-disabled'), 'true')
  assert.equal(element.getAttribute('title'), '当前账号暂无权限')
  assert.equal(element.style.cursor, 'not-allowed')
  assert.equal(element.classList.contains('is-permission-disabled'), true)

  directive.updated(element, { value: true })
  assert.deepEqual(elementState(element), beforeDenial)
})

test('repeated denied updates keep one pair of capture blockers active', () => {
  let blockedCount = 0
  const directive = createPermissionDirectiveLifecycle({
    isAllowed: Boolean,
    onBlocked: () => { blockedCount += 1 }
  })
  const element = new FakeElement()

  directive.mounted(element, { value: false })
  directive.updated(element, { value: false })

  assert.equal(element.listenerCount('click'), 1)
  assert.equal(element.listenerCount('keydown'), 1)
  const click = element.dispatch('click')
  const enter = element.dispatch('keydown', { key: 'Enter' })
  const space = element.dispatch('keydown', { key: ' ' })
  const escape = element.dispatch('keydown', { key: 'Escape' })
  for (const event of [click, enter, space]) {
    assert.equal(event.defaultPrevented, true)
    assert.equal(event.immediatePropagationStopped, true)
  }
  assert.equal(escape.defaultPrevented, false)
  assert.equal(escape.immediatePropagationStopped, false)
  assert.equal(blockedCount, 3)
})

test('pre-existing business disabled state and permission class survive denial', () => {
  const directive = createPermissionDirectiveLifecycle({ isAllowed: Boolean })
  const element = new FakeElement({
    attributes: { 'aria-disabled': 'mixed' },
    classes: ['is-permission-disabled'],
    disabled: true
  })

  directive.mounted(element, { value: false })
  directive.updated(element, { value: true })

  assert.equal(element.disabled, true)
  assert.equal(element.getAttribute('aria-disabled'), 'mixed')
  assert.equal(element.classList.contains('is-permission-disabled'), true)
})

test('business-owned attribute changes during denial are not overwritten on release', () => {
  const directive = createPermissionDirectiveLifecycle({ isAllowed: Boolean })
  const element = new FakeElement({ attributes: { title: 'Before' }, cursor: 'help' })
  directive.mounted(element, { value: false })

  element.disabled = true
  element.setAttribute('aria-disabled', 'mixed')
  element.setAttribute('title', 'Changed while denied')
  element.style.cursor = 'progress'
  directive.updated(element, { value: true })

  assert.equal(element.disabled, true)
  assert.equal(element.getAttribute('aria-disabled'), 'mixed')
  assert.equal(element.getAttribute('title'), 'Changed while denied')
  assert.equal(element.style.cursor, 'progress')
  assert.equal(element.classList.contains('is-permission-disabled'), false)
})

test('release and unmount detach blockers and restore permission-owned state', () => {
  let blockedCount = 0
  const directive = createPermissionDirectiveLifecycle({
    isAllowed: Boolean,
    onBlocked: () => { blockedCount += 1 }
  })
  const released = new FakeElement({ attributes: { title: 'Released' }, cursor: 'pointer' })
  directive.mounted(released, { value: false })
  directive.updated(released, { value: true })
  assert.equal(released.listenerCount('click'), 0)
  assert.equal(released.listenerCount('keydown'), 0)
  released.dispatch('click')

  const unmounted = new FakeElement({ attributes: { 'aria-disabled': 'false' }, cursor: 'crosshair' })
  const beforeDenial = elementState(unmounted)
  directive.mounted(unmounted, { value: false })
  directive.unmounted(unmounted)
  assert.deepEqual(elementState(unmounted), beforeDenial)
  assert.equal(unmounted.listenerCount('click'), 0)
  assert.equal(unmounted.listenerCount('keydown'), 0)
  unmounted.dispatch('keydown', { key: 'Enter' })
  assert.equal(blockedCount, 0)
})
