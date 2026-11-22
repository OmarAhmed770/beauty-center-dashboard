export type UserRole = "OWNER" | "RECEPTIONIST";
export type SystemPermission =
  | "DASHBOARD"
  | "ADD_ORDERS"
  | "EXPENSES"
  | "SERVICES_AND_PRICES"
  | "EMPLOYEES"
  | "DAILY_CLOSING"
  | "REPORTS"
  | "INVOICES_AND_RETURNS"
  | "SETTINGS"
  | "BACKUP_MANAGEMENT";
export type RegistrationStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface UserResponse {
  id: number;
  username: string;
  displayName: string;
  phone: string | null;
  employeeId: number | null;
  employeeName: string | null;
  active: boolean;
  registrationStatus: RegistrationStatus;
  roles: UserRole[];
  permissions: SystemPermission[];
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AccessTokenResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  expiresAt: string;
  user: UserResponse;
}

export interface InitialSetupStatus {
  initialOwnerSetupRequired: boolean;
}

export interface CreateInitialOwnerRequest {
  username: string;
  displayName: string;
  password: string;
  businessName?: string;
  currency?: "EGP" | "SAR" | "AED" | "USD" | "EUR";
  starterServices?: Array<{ nameArabic: string; category: "HAIR" | "MAKEUP" | "SKIN_CARE" | "NAILS" | "HAIR_INSTALLATION" }>;
  addDefaultExpenseCategories?: boolean;
  addDefaultPaymentMethods?: boolean;
}

export interface StaffRegistrationRequest {
  displayName: string;
  username: string;
  password: string;
  passwordConfirmation: string;
  phone?: string;
}

export interface StaffRegistrationResponse {
  username: string;
  registrationStatus: RegistrationStatus;
}

export interface PublicBusinessIdentity {
  businessName: string;
  logoDataUrl: string | null;
}
