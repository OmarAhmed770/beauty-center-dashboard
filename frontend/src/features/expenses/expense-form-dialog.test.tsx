import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ExpenseFormDialog } from "@/features/expenses/expense-form-dialog";

const { createExpenseMock, searchCategoriesMock, searchPaymentMethodsMock } = vi.hoisted(() => ({
  createExpenseMock: vi.fn(),
  searchCategoriesMock: vi.fn(),
  searchPaymentMethodsMock: vi.fn(),
}));

vi.mock("@/features/expenses/api", () => ({
  createExpense: createExpenseMock,
  expenseKeys: { all: ["expenses"] },
}));
vi.mock("@/features/expense-categories/api", () => ({
  expenseCategoryKeys: { all: ["expense-categories"] },
  searchExpenseCategories: searchCategoriesMock,
}));
vi.mock("@/features/payment-methods/api", () => ({
  paymentMethodKeys: { all: ["payment-methods"] },
  searchPaymentMethods: searchPaymentMethodsMock,
}));

function renderDialog() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}><ExpenseFormDialog open onOpenChange={vi.fn()} /></QueryClientProvider>);
}

describe("نموذج المصروف المبسط", () => {
  beforeEach(() => {
    createExpenseMock.mockReset();
    createExpenseMock.mockResolvedValue({ id: 1 });
    searchCategoriesMock.mockResolvedValue({ content: [{ id: 3, nameArabic: "ضيافة" }] });
    searchPaymentMethodsMock.mockResolvedValue({ content: [{ id: 7, displayNameArabic: "إنستاباي", requiresReferenceNumber: true }] });
  });

  it("يرسل الحقول المبسطة فقط حتى مع طريقة كانت تتطلب مرجعًا", async () => {
    const user = userEvent.setup();
    renderDialog();
    await screen.findByRole("option", { name: "ضيافة" });

    expect(screen.queryByLabelText("مرجع الدفع")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("مسار الإيصال")).not.toBeInTheDocument();
    await user.selectOptions(screen.getByRole("combobox", { name: /التصنيف/ }), "3");
    await user.type(screen.getByLabelText(/المبلغ/), "85");
    await user.selectOptions(screen.getByRole("combobox", { name: /طريقة الدفع/ }), "7");
    await user.type(screen.getByLabelText(/وصف المصروف/), "ضيافة العملاء");
    await user.type(screen.getByLabelText("ملاحظات"), "قهوة ومياه");
    await user.click(screen.getByRole("button", { name: "حفظ المصروف" }));

    await waitFor(() => expect(createExpenseMock).toHaveBeenCalledWith({
      categoryId: 3,
      amount: "85",
      businessDate: expect.any(String),
      paymentMethodId: 7,
      description: "ضيافة العملاء",
      notes: "قهوة ومياه",
    }));
    const payload = createExpenseMock.mock.calls[0][0] as Record<string, unknown>;
    expect(payload).not.toHaveProperty("paymentReference");
    expect(payload).not.toHaveProperty("receiptPath");
  });
});
