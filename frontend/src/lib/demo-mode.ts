export const demoReadOnly = import.meta.env.VITE_DEMO_READ_ONLY === "true";
export const demoReadOnlyMessage = "دي نسخة للعرض فقط؛ الحفظ والتعديل غير متاحين.";

export function isDemoWrite(method = "get", url = "", data?: unknown) {
  if (method.toLowerCase() === "post" && url === "/api/v1/hair-configurations/preview"
    && data && typeof data === "object" && !Array.isArray(data)
    && Object.entries(data).every(([key, value]) => value === undefined || ["configurationId", "quantity", "discountId"].includes(key))) {
    return false;
  }
  return !["get", "head", "options"].includes(method.toLowerCase())
    && !(method.toLowerCase() === "post" && url === "/api/v1/auth/login");
}
