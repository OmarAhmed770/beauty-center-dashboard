import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { HairPreviewPanel } from "@/features/hair/hair-preview-panel";

const mocks = vi.hoisted(() => ({
  getCatalogItem: vi.fn(),
  hasAnyRole: vi.fn(),
  hasPermission: vi.fn(),
  previewHairCalculation: vi.fn(),
  searchDiscounts: vi.fn(),
  searchHairConfigurations: vi.fn(),
}));

vi.mock("@/features/auth/auth-context", () => ({ useAuth: () => ({ hasAnyRole: mocks.hasAnyRole, hasPermission: mocks.hasPermission }) }));
vi.mock("@/features/catalog/api", () => ({ catalogKeys: { item: (id: number) => ["catalog", id] }, getCatalogItem: mocks.getCatalogItem }));
vi.mock("@/features/discounts/api", () => ({ searchDiscounts: mocks.searchDiscounts }));
vi.mock("@/features/hair/api", () => ({ hairKeys: { all: ["hair"], list: (params: object) => ["hair", params] }, previewHairCalculation: mocks.previewHairCalculation, searchHairConfigurations: mocks.searchHairConfigurations }));

function renderPanel(onAddConfiguration = vi.fn()) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return { onAddConfiguration, ...render(<QueryClientProvider client={client}><HairPreviewPanel onAddConfiguration={onAddConfiguration} /></QueryClientProvider>) };
}

describe("معاينة تركيب الشعر", () => {
  beforeEach(() => {
    mocks.hasAnyRole.mockReturnValue(true);
    mocks.hasPermission.mockReturnValue(true);
    mocks.searchDiscounts.mockResolvedValue({ content: [] });
    mocks.getCatalogItem.mockResolvedValue({ id: 22, categoryId: 4 });
    mocks.previewHairCalculation.mockResolvedValue({ baseProductAmount: 50, installationFee: 20, subtotal: 70, discountAmount: 0, calculatedFinalAmount: 70, overridden: false, finalAmount: 70, costEstimate: 25 });
  });

  it("يعرض حالة واضحة وزر إضافة عند عدم وجود إعدادات", async () => {
    mocks.searchHairConfigurations.mockResolvedValue({ content: [] });
    const { onAddConfiguration } = renderPanel();
    expect(await screen.findByText("مفيش إعدادات شعر لسه")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "ضيف أول إعداد" }));
    expect(onAddConfiguration).toHaveBeenCalledOnce();
    expect(screen.queryByRole("button", { name: "احسب المعاينة" })).not.toBeInTheDocument();
  });

  it("لا يسمح بالحساب قبل اكتمال البيانات ويخفي التعديل اليدوي حتى يطلبه المالك", async () => {
    mocks.searchHairConfigurations.mockResolvedValue({ content: [{ id: 12, hairItemId: 22, hairItemNameArabic: "شعر طبيعي", brand: "A", length: "20", color: null }] });
    renderPanel();
    const calculate = await screen.findByRole("button", { name: "احسب المعاينة" });
    expect(calculate).toBeDisabled();
    expect(screen.queryByLabelText("السعر اليدوي")).not.toBeInTheDocument();
    await userEvent.selectOptions(screen.getByLabelText(/نوع الشعر/), "12");
    await waitFor(() => expect(screen.getByRole("button", { name: "احسب المعاينة" })).toBeEnabled());
    await userEvent.click(screen.getByRole("button", { name: "تعديل السعر يدويًا" }));
    expect(screen.getByLabelText("السعر اليدوي")).toBeInTheDocument();
  });
});
