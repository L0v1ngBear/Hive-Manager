import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import postcss from 'postcss'
import { pageGuides } from '../src/config/pageGuides.js'

const read = path => readFileSync(new URL(`../src/${path}`, import.meta.url), 'utf8')

test('every workspace route has concise operation guidance', () => {
  const router = read('router/index.js')
  const routes = [...router.matchAll(/name: '([^']+)',\s*component: \(\) => import\('([^']+)'\),\s*meta: \{([^}]+)\}/g)]
    .filter(([, name, , meta]) => !meta.includes('public: true') && name !== 'ForcePasswordChange')
  assert.equal(routes.length, 23)
  for (const [, name] of routes) {
    assert.ok(pageGuides[name]?.group, `${name} needs a location group`)
    assert.equal(pageGuides[name].steps.length, 3, `${name} needs three short steps`)
  }
  assert.match(read('layout/index.vue'), /<PageOperationGuide :route="route"/)
})

test('office layout rules are scoped, screen-only, and cannot change the palette', () => {
  const sheet = postcss.parse(read('styles/office-layout.css'))
  sheet.walkRules(rule => {
    assert.ok(rule.selectors.every(selector => /^\.hive-office-ui(?: |\.)/.test(selector)), rule.selector)
    let parent = rule.parent
    while (parent && !(parent.type === 'atrule' && parent.name === 'media' && parent.params === 'screen')) parent = parent.parent
    assert.ok(parent, `${rule.selector} must not affect print`)
  })
  sheet.walkDecls(declaration => {
    assert.doesNotMatch(declaration.prop, /color|background|shadow|opacity|filter|^border(?:$|-color)/i)
    assert.doesNotMatch(declaration.value, /#[\da-f]{3,8}\b|rgba?\(|hsla?\(/i)
  })
})

test('guidance uses a keyboard-operable disclosure and contains no business mutations', () => {
  const component = read('components/PageOperationGuide.vue')
  assert.match(component, /<details :key="route.name"/)
  assert.match(component, /<summary>操作指引<\/summary>/)
  assert.match(component, /aria-current="page"/)
  assert.doesNotMatch(component, /@click|fetch\(|request\(|router\.push/)
})
