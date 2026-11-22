export interface ExpenseCategory {
  id: number;
  nameArabic: string;
  nameEnglish: string;
  active: boolean;
  notes: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface ExpenseCategoryInput {
  nameArabic: string;
  nameEnglish: string;
  active?: boolean;
  notes?: string;
}
