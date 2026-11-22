import { zodResolver } from "@hookform/resolvers/zod";
import { useQuery } from "@tanstack/react-query";
import { Eye, EyeOff, LoaderCircle, LockKeyhole, LogIn, Sparkles, UserRound, UsersRound } from "lucide-react";
import { useState, type FormEvent } from "react";
import { useForm } from "react-hook-form";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/features/auth/auth-context";
import { requestPublicBusinessIdentity } from "@/lib/api/auth";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import type { UserResponse } from "@/types/auth";

const loginSchema = z.object({
  username: z.string().trim().min(1, "اسم المستخدم مطلوب").max(100, "اسم المستخدم طويل جدًا"),
  password: z.string().min(1, "كلمة المرور مطلوبة").max(200, "كلمة المرور طويلة جدًا"),
});

type LoginValues = z.infer<typeof loginSchema>;

function startPathFor(user: UserResponse): string {
  return user.roles.includes("OWNER") || user.permissions.includes("DASHBOARD") ? "/dashboard" : "/orders/new";
}

export function LoginPage() {
  const { token, user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [showPassword, setShowPassword] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const identity = useQuery({ queryKey: ["public-business-identity"], queryFn: requestPublicBusinessIdentity, retry: false, staleTime: 5 * 60_000 });
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: "", password: "" },
  });

  if (token && user) return <Navigate to={startPathFor(user)} replace />;

  const submit = handleSubmit((values) => {
    setSubmitError(null);
    return login(values).then(
      (loggedInUser) => {
        const requestedPath = (location.state as { from?: string } | null)?.from;
        navigate(requestedPath || startPathFor(loggedInUser), { replace: true });
      },
      (error: unknown) => setSubmitError(getArabicApiErrorMessage(error)),
    );
  });
  const submitForm = (event: FormEvent<HTMLFormElement>) => {
    void submit(event).catch((error: unknown) => setSubmitError(getArabicApiErrorMessage(error)));
  };
  const businessName = identity.data?.businessName || "نظام إدارة المركز";

  return (
    <main className="login-background flex min-h-screen items-center justify-center p-5" dir="rtl">
      <Card className="w-full max-w-md p-6 sm:p-9">
        <header className="text-center">
          {identity.data?.logoDataUrl ? <img className="mx-auto h-12 w-12 rounded-2xl border border-[#eee7ea] object-contain" src={identity.data.logoDataUrl} alt={`شعار ${businessName}`} /> : <span className="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-[#FCEAF2] text-[#A83868]"><Sparkles className="h-5 w-5" aria-hidden="true" /></span>}
          <p className="mt-4 truncate text-sm font-black text-[#A83868]">{businessName}</p>
          <h1 className="mt-2 text-2xl font-black text-[#29252A]">تسجيل الدخول</h1>
        </header>
        <form className="mt-7 space-y-5" onSubmit={submitForm} aria-label="نموذج تسجيل الدخول" noValidate>
          <div>
            <label className="mb-2 block text-sm font-bold text-[#4b454b]" htmlFor="username">اسم المستخدم</label>
            <div className="relative"><UserRound className="pointer-events-none absolute right-3.5 top-3.5 h-5 w-5 text-[#a69aa2]" aria-hidden="true" /><Input id="username" autoComplete="username" className="pr-11" aria-invalid={Boolean(errors.username)} {...register("username")} /></div>
            {errors.username && <p className="mt-2 text-sm font-semibold text-red-700" role="alert">{errors.username.message}</p>}
          </div>
          <div>
            <label className="mb-2 block text-sm font-bold text-[#4b454b]" htmlFor="password">كلمة المرور</label>
            <div className="relative"><LockKeyhole className="pointer-events-none absolute right-3.5 top-3.5 h-5 w-5 text-[#a69aa2]" aria-hidden="true" /><Input id="password" type={showPassword ? "text" : "password"} autoComplete="current-password" className="px-11" aria-invalid={Boolean(errors.password)} {...register("password")} /><button type="button" onClick={() => setShowPassword((value) => !value)} className="absolute left-2 top-2 grid h-8 w-8 place-items-center rounded-lg text-[#736873] hover:bg-[#FCEAF2]" aria-label={showPassword ? "إخفاء كلمة المرور" : "إظهار كلمة المرور"}>{showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}</button></div>
            {errors.password && <p className="mt-2 text-sm font-semibold text-red-700" role="alert">{errors.password.message}</p>}
          </div>
          {submitError && <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-800" role="alert">{submitError}</div>}
          <Button type="submit" size="lg" className="w-full" disabled={isSubmitting}>{isSubmitting && <LoaderCircle className="h-5 w-5 animate-spin" aria-hidden="true" />}<LogIn className="h-5 w-5" aria-hidden="true" />{isSubmitting ? "جارٍ تسجيل الدخول..." : "دخول"}</Button>
        </form>
        <Link to="/register" className="mt-5 flex min-h-11 items-center justify-center gap-2 rounded-2xl text-sm font-bold text-[#A83868] hover:bg-[#FCEAF2]"><UsersRound className="h-4 w-4" aria-hidden="true" />إنشاء حساب موظف</Link>
      </Card>
    </main>
  );
}
