import { computed, ref, unref, watch } from 'vue'

const STORAGE_PREFIX = 'hive.table.columns.'

function normalizeColumns(defaultColumns, savedKeys = []) {
  const defaultList = Array.isArray(defaultColumns) ? defaultColumns : []
  const columnMap = new Map(defaultList.map((column) => [column.key, column]))
  const orderedKeys = Array.isArray(savedKeys)
    ? savedKeys.filter((key) => columnMap.has(key))
    : []
  const missingKeys = defaultList
    .map((column) => column.key)
    .filter((key) => !orderedKeys.includes(key))

  return [...orderedKeys, ...missingKeys]
    .map((key) => columnMap.get(key))
    .filter(Boolean)
}

function readSavedKeys(storageKey) {
  if (!storageKey) return []
  try {
    const raw = window.localStorage.getItem(`${STORAGE_PREFIX}${storageKey}`)
    const parsed = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function saveKeys(storageKey, columns) {
  if (!storageKey) return
  try {
    window.localStorage.setItem(
      `${STORAGE_PREFIX}${storageKey}`,
      JSON.stringify(columns.map((column) => column.key))
    )
  } catch {
    // Local storage may be unavailable in private mode. Keep runtime order only.
  }
}

export function useLocalTableColumns(storageKey, defaultColumns) {
  const defaultColumnList = computed(() => {
    const value = unref(defaultColumns)
    return Array.isArray(value) ? value : []
  })

  const columns = ref(normalizeColumns(defaultColumnList.value, readSavedKeys(storageKey)))

  const orderedColumns = computed(() => columns.value)
  const columnCount = computed(() => columns.value.length)

  watch(
    defaultColumnList,
    (nextDefaultColumns) => {
      columns.value = normalizeColumns(
        nextDefaultColumns,
        columns.value.map((column) => column.key)
      )
    },
    { deep: true }
  )

  watch(
    columns,
    (nextColumns) => saveKeys(storageKey, nextColumns),
    { deep: true }
  )

  const moveColumn = (key, direction) => {
    const index = columns.value.findIndex((column) => column.key === key)
    const nextIndex = index + direction
    if (index < 0 || nextIndex < 0 || nextIndex >= columns.value.length) return
    const nextColumns = [...columns.value]
    const current = nextColumns[index]
    nextColumns[index] = nextColumns[nextIndex]
    nextColumns[nextIndex] = current
    columns.value = nextColumns
  }

  const resetColumns = () => {
    columns.value = normalizeColumns(defaultColumnList.value, [])
  }

  return {
    orderedColumns,
    columnCount,
    moveColumn,
    resetColumns
  }
}
