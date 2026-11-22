const money = new Intl.NumberFormat('zh-TW', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

export const price = (amount: number) => money.format(amount)
export const dateTime = (value: string) =>
  new Intl.DateTimeFormat('zh-TW', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))

export function routeId(value: string | undefined) {
  const id = Number(value)
  return Number.isSafeInteger(id) && id > 0 ? id : null
}

export function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : '操作失敗，請稍後再試。'
}
