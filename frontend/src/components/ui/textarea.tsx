import { forwardRef, type TextareaHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(
  ({ className, ...props }, ref) => (
    <textarea
      ref={ref}
      className={cn(
        "flex min-h-28 w-full resize-y rounded-2xl border border-soft-border bg-white px-3.5 py-3 text-base text-ink shadow-sm outline-none placeholder:text-muted/60 focus:border-primary focus:ring-2 focus:ring-primary-soft disabled:cursor-not-allowed disabled:bg-page",
        className,
      )}
      {...props}
    />
  ),
);

Textarea.displayName = "Textarea";
