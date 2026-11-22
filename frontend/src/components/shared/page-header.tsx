import type { ReactNode } from "react";

export function PageHeader({
  title: _title,
  description: _description,
  actions,
}: {
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  if (!actions) return null;
  return <div className="flex w-full justify-end [&>a]:w-full [&>button]:w-full sm:[&>a]:w-auto sm:[&>button]:w-auto">{actions}</div>;
}
