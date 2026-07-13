export function buildStructuredExportData(columns, sourceRows, exportCell) {
  return {
    headers: columns.map((column) => column.label),
    rows: sourceRows.map((row) => columns.map((column) => exportCell(row, column)))
  }
}
