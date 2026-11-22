export const closingStatusLabels = { OPEN: "مفتوح", CLOSED: "مغلق", REOPENED: "أُعيد فتحه" } as const;

export function validateActualCash(value: string): string | null {
  if (!/^\d{1,17}(\.\d{1,2})?$/.test(value.trim())) return "أدخل النقدية الفعلية كرقم غير سالب بمنزلتين عشريتين كحد أقصى.";
  return null;
}

export function calculateCashDifference(actualCash: string, expectedCash: number | string): number {
  return Math.round((Number(actualCash || 0) - Number(expectedCash)) * 100) / 100;
}
