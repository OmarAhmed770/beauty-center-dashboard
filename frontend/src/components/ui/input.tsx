import { forwardRef, type InputHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <input
      ref={ref}
      className={cn(
        "flex h-12 w-full rounded-2xl border border-soft-border bg-white px-3.5 text-base text-ink shadow-sm outline-none placeholder:text-muted/60 focus:border-primary focus:ring-2 focus:ring-primary-soft disabled:cursor-not-allowed disabled:bg-page disabled:text-muted",
        className,
      )}
      {...props}
    />
  ),
);

Input.displayName = "Input";
