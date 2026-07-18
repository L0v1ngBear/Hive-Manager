let filterPanelSequence = 0

const mountedPanels = new WeakMap()

function createElement(tagName, className, text = '') {
  const element = document.createElement(tagName)
  element.className = className
  element.textContent = text
  return element
}

function mountFilterCollapse(element, binding) {
  if (!element?.parentElement || mountedPanels.has(element)) return

  const options = binding?.value && typeof binding.value === 'object' ? binding.value : {}
  const panelId = element.id || `function-filter-panel-${++filterPanelSequence}`
  const assignedId = !element.id
  element.id = panelId

  const bar = createElement('div', 'function-filter-collapse-bar')
  const copy = createElement('div', 'function-filter-collapse-copy')
  const leadingIcon = createElement('span', 'material-symbols-outlined', 'filter_alt')
  leadingIcon.setAttribute('aria-hidden', 'true')

  const labels = createElement('span', 'function-filter-collapse-labels')
  const title = createElement('strong', '', options.title || '筛选条件')
  const description = createElement('small', '', options.description || '按条件快速缩小列表范围')
  labels.append(title, description)
  copy.append(leadingIcon, labels)

  const toggle = createElement('button', 'function-filter-collapse-toggle')
  toggle.type = 'button'
  toggle.setAttribute('aria-controls', panelId)
  const toggleIcon = createElement('span', 'material-symbols-outlined')
  toggleIcon.setAttribute('aria-hidden', 'true')
  const toggleText = createElement('span', '')
  toggle.append(toggleIcon, toggleText)
  bar.append(copy, toggle)

  let expanded = options.defaultExpanded !== false
  const render = () => {
    element.hidden = !expanded
    bar.classList.toggle('function-filter-collapse-bar--collapsed', !expanded)
    toggle.setAttribute('aria-expanded', String(expanded))
    toggleIcon.textContent = expanded ? 'expand_less' : 'expand_more'
    toggleText.textContent = expanded ? '收起筛选' : '展开筛选'
  }
  const handleToggle = () => {
    expanded = !expanded
    render()
  }

  toggle.addEventListener('click', handleToggle)
  element.parentElement.insertBefore(bar, element)
  mountedPanels.set(element, { assignedId, bar, handleToggle })
  render()
}

function unmountFilterCollapse(element) {
  const state = mountedPanels.get(element)
  if (!state) return
  state.bar.querySelector('button')?.removeEventListener('click', state.handleToggle)
  state.bar.remove()
  element.hidden = false
  if (state.assignedId) element.removeAttribute('id')
  mountedPanels.delete(element)
}

export default {
  mounted: mountFilterCollapse,
  unmounted: unmountFilterCollapse,
}
