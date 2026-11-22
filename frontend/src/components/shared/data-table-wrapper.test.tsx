import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { DataTableWrapper } from "@/components/shared/data-table-wrapper";

describe("DataTableWrapper", () => {
  it("adds the desktop column heading to each cell for the mobile card layout", async () => {
    render(
      <DataTableWrapper label="قائمة تجريبية">
        <thead><tr><th>الاسم</th><th>الحالة</th></tr></thead>
        <tbody><tr><td>خدمة تجريبية</td><td>نشط</td></tr></tbody>
      </DataTableWrapper>,
    );

    await waitFor(() => {
      expect(screen.getByText("خدمة تجريبية")).toHaveAttribute("data-mobile-label", "الاسم");
      expect(screen.getByText("نشط")).toHaveAttribute("data-mobile-label", "الحالة");
    });
    expect(screen.getByRole("table", { name: "قائمة تجريبية" }).parentElement?.parentElement).toHaveClass("responsive-data-table");
  });
});
