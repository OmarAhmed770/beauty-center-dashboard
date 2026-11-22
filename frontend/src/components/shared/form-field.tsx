import type { ReactNode } from "react";

export function FormField({
  label,
  htmlFor,
  required = false,
  error,
  hint,
  children,
}: {
  label: string;
  htmlFor: string;
  required?: boolean;
  error?: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <div>
      <label className="mb-2 block text-sm font-bold text-stone-700" htmlFor={htmlFor}>{label}{required && <span className="ms-1 text-red-700" aria-hidden="true">*</span>}</label>
      {children}
      {error ? <p className="mt-1.5 text-xs font-bold text-red-700" role="alert">{error}</p> : hint ? <p className="mt-1.5 text-xs leading-5 text-stone-500">{hint}</p> : null}
    </div>
  );
}

export function FormServerError({ message }: { message?: string }) {
  return message ? <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-bold text-red-800" role="alert">{message}</div> : null;
}
