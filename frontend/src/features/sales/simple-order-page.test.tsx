import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SimpleOrderPage } from "@/features/sales/simple-order-page";

const mocks = vi.hoisted(() => ({
  authUser: { value: null as { employeeId: number | null } | null },
  createQuickOrder: vi.fn(),
  searchCatalogItems: vi.fn(),
  searchEmployees: vi.fn(),
  searchHairConfigurations: vi.fn(),
  previewHairCalculation: vi.fn(),
  searchPaymentMethods: vi.fn(),
}));

vi.mock("@/features/sales/api", () => ({ createQuickOrder: mocks.createQuickOrder, invoiceKeys: { all: ["invoices"] } }));
vi.mock("@/features/catalog/api", () => ({ catalogKeys: { items: (params: object) => ["catalog", params] }, searchCatalogItems: mocks.searchCatalogItems }));
vi.mock("@/features/employees/api", () => ({ employeeKeys: { all: ["employees"] }, searchEmployees: mocks.searchEmployees }));
vi.mock("@/features/hair/api", () => ({ hairKeys: { list: (params: object) => ["hair", params] }, searchHairConfigurations: mocks.searchHairConfigurations, previewHairCalculation: mocks.previewHairCalculation }));
vi.mock("@/features/payment-methods/api", () => ({ paymentMethodKeys: { all: ["payment-methods"] }, searchPaymentMethods: mocks.searchPaymentMethods }));
vi.mock("@/features/auth/auth-context", () => ({ useAuth: () => ({ user: mocks.authUser.value }) }));

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={client}><MemoryRouter><SimpleOrderPage /></MemoryRouter></QueryClientProvider>);
}

describe("شاشة أوردر الكتالوج", () => {
  beforeEach(() => {
    mocks.createQuickOrder.mockReset();
    mocks.authUser.value = null;
    mocks.searchCatalogItems.mockResolvedValue({ content: [{ id: 10, nameArabic: "قص شعر", nameEnglish: "Cut", itemType: "SERVICE", categoryNameArabic: "شعر", currentSellingPrice: 250, active: true }] });
    mocks.searchEmployees.mockResolvedValue({ content: [{ id: 9, name: "منى", active: true }] });
    mocks.searchHairConfigurations.mockResolvedValue({ content: [] });
    mocks.searchPaymentMethods.mockResolvedValue({ content: [
      { id: 1, code: "CASH", displayNameArabic: "نقدي", requiresReferenceNumber: false },
      { id: 2, code: "FAWRY", displayNameArabic: "فوري", requiresReferenceNumber: true },
    ] });
  });

  it("لا يحفظ قبل اختيار خدمة", async () => {
    renderPage();
    await screen.findByRole("button", { name: "إضافة قص شعر" });
    await userEvent.click(screen.getByRole("button", { name: "حفظ الأوردر" }));
    expect(await screen.findByText("اختر خدمة واحدة على الأقل")).toHaveAttribute("role", "alert");
    expect(mocks.createQuickOrder).not.toHaveBeenCalled();
  });

  it("لا يجعل تعذر إعدادات الشعر الاختيارية شاشة الأوردر العادي غير قابلة للاستخدام", async () => {
    mocks.searchHairConfigurations.mockRejectedValue(new Error("An unexpected error occurred."));
    renderPage();
    expect(await screen.findByRole("button", { name: "إضافة قص شعر" })).toBeEnabled();
    expect(screen.queryByText("حصلت مشكلة، جرّب تاني")).not.toBeInTheDocument();
  });

  it("يحفظ خدمة من الكتالوج بلا سعر مرسل من العميل", async () => {
    mocks.createQuickOrder.mockResolvedValue({ invoiceId: 7, invoiceNumber: "INV-20260801-ABC", total: 250, lines: [] });
    const user = userEvent.setup();
    renderPage();
    await user.click(await screen.findByRole("button", { name: "إضافة قص شعر" }));
    await user.type(screen.getByLabelText("اسم العميلة"), "سارة أحمد");
    await user.type(screen.getByLabelText("رقم الموبايل"), "+20 100 123 4567");
    await user.type(screen.getByLabelText("ملاحظات"), "طلب سريع");
    await user.selectOptions(screen.getByLabelText("الموظف أو المتخصص"), "9");
    await user.click(screen.getByRole("button", { name: "حفظ الأوردر" }));

    await waitFor(() => expect(mocks.createQuickOrder).toHaveBeenCalledWith(expect.objectContaining({
      lines: [expect.objectContaining({ catalogItemId: 10, quantity: "1", employeeId: 9 })],
      payments: [{ paymentMethodId: 1 }],
      clientRequestId: expect.any(String),
      customerName: "سارة أحمد",
      customerPhone: "+20 100 123 4567",
      notes: "طلب سريع",
    })));
    expect(mocks.createQuickOrder.mock.calls[0][0].lines[0]).not.toHaveProperty("price");
    expect(await screen.findByText(/تم حفظ الأوردر رقم/)).toHaveTextContent("INV-20260801-ABC");
  });

  it("يعرض مرجع الدفع فقط للطريقة التي تتطلبه ويمسحه عند تغيير الطريقة", async () => {
    mocks.createQuickOrder.mockResolvedValue({ invoiceId: 8, invoiceNumber: "INV-REF", total: 250, lines: [] });
    const user = userEvent.setup();
    renderPage();
    await user.click(await screen.findByRole("button", { name: "إضافة قص شعر" }));
    await user.click(screen.getByRole("button", { name: "فوري" }));
    await user.click(screen.getByRole("button", { name: "حفظ الأوردر" }));
    expect(await screen.findByText("رقم العملية أو مرجع الدفع مطلوب")).toHaveAttribute("role", "alert");
    await user.type(screen.getByLabelText(/رقم العملية/), "FW-123");
    await user.click(screen.getByRole("button", { name: "نقدي" }));
    expect(screen.queryByLabelText(/رقم العملية/)).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "حفظ الأوردر" }));
    await waitFor(() => expect(mocks.createQuickOrder).toHaveBeenCalled());
    expect(mocks.createQuickOrder.mock.calls[0][0].payments[0]).not.toHaveProperty("referenceNumber");
  });

  it("يعطل الحفظ أثناء الإرسال ويبقي البيانات عند خطأ يمكن إصلاحه", async () => {
    let rejectRequest: (error: Error) => void = () => undefined;
    mocks.createQuickOrder.mockImplementationOnce(() => new Promise((_, reject) => { rejectRequest = reject; }));
    const user = userEvent.setup();
    renderPage();
    await user.click(await screen.findByRole("button", { name: "إضافة قص شعر" }));
    await user.click(screen.getByRole("button", { name: "حفظ الأوردر" }));
    expect(await screen.findByRole("button", { name: "جارٍ حفظ الأوردر..." })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "جارٍ حفظ الأوردر..." }));
    expect(mocks.createQuickOrder).toHaveBeenCalledTimes(1);
    rejectRequest(new Error("تعذر حفظ الأوردر الآن"));
    expect(await screen.findByText("تعذر حفظ الأوردر الآن")).toHaveAttribute("role", "alert");
    expect(screen.getAllByText("قص شعر")).toHaveLength(2);
  });

  it("يضبط جرامات الشعر تلقائيًا على الحد الأدنى ويمنع النزول عنه عند تغيير النوع", async () => {
    mocks.searchCatalogItems.mockResolvedValue({ content: [{ id: 20, nameArabic: "شعر طبيعي", nameEnglish: "Hair", itemType: "HAIR_PRODUCT", categoryNameArabic: "تركيب شعر", currentSellingPrice: 0, active: true }] });
    mocks.searchHairConfigurations.mockResolvedValue({ content: [{
      id: 30,
      hairItemId: 20,
      hairItemNameArabic: "شعر طبيعي",
      hairItemNameEnglish: "Hair",
      hairItemCode: "HAIR-1",
      brand: "Brand",
      length: "50 سم",
      color: null,
      unitType: "جرام",
      unitSellingPrice: 20,
      unitCost: null,
      installationFee: 15,
      minimumQuantity: 2,
      stockConfigured: true,
      availableStockGrams: 5,
      active: true,
      effectiveDate: "2026-11-14",
      version: 0,
      createdAt: "2026-11-14T00:00:00",
      updatedAt: "2026-11-14T00:00:00",
      createdBy: "owner",
      updatedBy: "owner",
    }, {
      id: 31,
      hairItemId: 20,
      hairItemNameArabic: "شعر طبيعي",
      hairItemNameEnglish: "Hair",
      hairItemCode: "HAIR-1",
      brand: "Brand",
      length: "60 سم",
      color: null,
      unitType: "جرام",
      unitSellingPrice: 22,
      unitCost: null,
      installationFee: 15,
      minimumQuantity: 4,
      stockConfigured: true,
      availableStockGrams: 5,
      active: true,
      effectiveDate: "2026-11-14",
      version: 0,
      createdAt: "2026-11-14T00:00:00",
      updatedAt: "2026-11-14T00:00:00",
      createdBy: "owner",
      updatedBy: "owner",
    }] });
    mocks.previewHairCalculation.mockResolvedValue({ baseProductAmount: 20, installationFee: 15, subtotal: 35, discountAmount: 0, calculatedFinalAmount: 35, overridden: false, finalAmount: 35, costEstimate: null });
    const user = userEvent.setup();
    renderPage();
    await user.click(await screen.findByRole("button", { name: "إضافة شعر طبيعي" }));
    await user.selectOptions(await screen.findByLabelText(/نوع الشعر/), "30");
    const gramsInput = screen.getByLabelText(/الجرامات/);
    expect(gramsInput).toHaveValue(2);
    expect((await screen.findByText(/الحد الأدنى:/)).parentElement).toHaveTextContent("2 جرام");
    expect(screen.getByText(/المتاح:/).parentElement).toHaveTextContent("5 جرام");
    expect(screen.getByText(/سعر الجرام:/).parentElement).toHaveTextContent("20");
    fireEvent.change(gramsInput, { target: { value: "1" } });
    expect(gramsInput).toHaveValue(2);
    await user.selectOptions(screen.getByLabelText(/نوع الشعر/), "31");
    expect(gramsInput).toHaveValue(4);
  });
});
