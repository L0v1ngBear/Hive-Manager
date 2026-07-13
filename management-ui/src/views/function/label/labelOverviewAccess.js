export async function loadEquipmentOverviewCount({ canListEquipment, getEquipmentPage }) {
  if (!canListEquipment) return null
  const page = await getEquipmentPage({ pageNum: 1, pageSize: 1 })
  return Number(page?.total || page?.totalCount || page?.records?.length || page?.data?.length || 0)
}
