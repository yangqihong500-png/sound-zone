let formActive = false

export function beginCreateFromTab() {
  if (formActive) return false
  formActive = true
  return true
}

export function finishCreateFromTab() {
  formActive = false
}

export function isCreateFromTabActive() {
  return formActive
}
