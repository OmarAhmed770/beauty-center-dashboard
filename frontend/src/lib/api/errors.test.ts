import { describe, expect, it } from "vitest";
import { getArabicApiErrorMessage, isClosedDateError, parseApiError } from "@/lib/api/errors";

describe("parseApiError", () => {
  it("preserves the standard backend message and field validation details", () => {
    const error = {
      isAxiosError: true,
      response: {
        data: {
          timestamp: "2026-07-26T08:00:00",
          status: 400,
          code: "VALIDATION_FAILED",
          message: "Request validation failed.",
          path: "/api/v1/auth/login",
          validationErrors: [{ field: "username", message: "Username is required." }],
        },
      },
    };

    const parsed = parseApiError(error);
    expect(parsed.status).toBe(400);
    expect(parsed.code).toBe("VALIDATION_FAILED");
    expect(parsed.validationErrors).toEqual([{ field: "username", message: "Username is required." }]);
  });
});

describe("تعريب أخطاء الوحدات المالية", () => {
  it("يعرض رسالة عربية واضحة للتاريخ المغلق", () => {
    const error = { isAxiosError: true, response: { data: { status: 409, message: "Financial records cannot be changed for a closed business date." } } };
    expect(getArabicApiErrorMessage(error)).toContain("تاريخ عمل مُقفل");
    expect(isClosedDateError(error)).toBe(true);
  });

  it("لا يمرر أي نص خطأ إنجليزي غير معروف إلى المستخدم", () => {
    const error = { isAxiosError: true, response: { data: { status: 500, message: "An unexpected error occurred." } } };
    expect(getArabicApiErrorMessage(error)).toBe("حصلت مشكلة، جرّب تاني");
  });
});
