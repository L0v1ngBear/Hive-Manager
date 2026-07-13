export function createDocumentNavigator({ canNavigate, fetchDocuments }) {
  const navigate = async (targetId) => {
    if (!canNavigate()) return false
    await fetchDocuments(targetId)
    return true
  }

  return {
    navigateUp: navigate,
    navigateTo: navigate,
    openFolder: navigate,
    goRoot: () => navigate(0)
  }
}
