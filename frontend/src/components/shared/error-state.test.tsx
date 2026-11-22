// TEST ONLY - credential literals in this file are synthetic test fixtures.
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ErrorState } from "@/components/shared/error-state";

describe("ErrorState", () => {
  it("displays a standard API response message in Arabic UI", () => {
    const error = {
      isAxiosError: true,
      response: {
        data: {
          status: 401,
          code: "AUTHENTICATION_FAILED",
          message: "اسم المستخدم أو كلمة المرور غير صحيحة",
        },
      },
    };

    render(<ErrorState error={error} />);

    expect(screen.getByRole("alert")).toHaveTextContent("اسم المستخدم أو كلمة المرور غير صحيحة");
  });
});
