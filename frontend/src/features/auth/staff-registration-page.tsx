import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery } from "@tanstack/react-query";
import { CheckCircle2, Eye, EyeOff, LockKeyhole, Phone, Sparkles, UserPlus, UserRound } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link } from "react-router-dom";
import { z } from "zod";
import { FormField, FormServerError } from "@/components/shared/form-field";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { requestPublicBusinessIdentity, requestStaffRegistration } from "@/lib/api/auth";
import { getArabicApiErrorMessage } from "@/lib/api/errors";

const passwordStrength = z.string()
  .min(12, "كلمة المرور يجب ألا تقل عن 12 حرفًا")
  .max(72, "كلمة المرور طويلة جدًا")
  .refine((value) => /[\p{L}]/u.test(value) && /[\p{N}]/u.test(value), "استخدم حرفًا ورقمًا واحدًا على الأقل");

const registrationSchema = z.object({
  displayName: z.string().trim().min(1, "الاسم الكامل مطلوب").max(150, "الاسم طويل جدًا"),
  username: z.string().trim().regex(/^[a-z0-9._-]{3,100}$/, "استخدم 3 أحرف صغيرة أو أرقام أو . أو _ أو -"),
  password: passwordStrength,
  passwordConfirmation: z.string(),
  phone: z.string().trim().max(50, "رقم الهاتف طويل جدًا"),
}).refine((values) => values.password === values.passwordConfirmation, {
  message: "تأكيد كلمة المرور غير مطابق",
  path: ["passwordConfirmation"],
});

type RegistrationValues = z.infer<typeof registrationSchema>;

export function StaffRegistrationPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [submittedUsername, setSubmittedUsername] = useState<string | null>(null);
  const identity = useQuery({ queryKey: ["public-business-identity"], queryFn: requestPublicBusinessIdentity, retry: false, staleTime: 5 * 60_000 });
  const { register, handleSubmit, setError, formState: { errors } } = useForm<RegistrationValues>({
    resolver: zodResolver(registrationSchema),
    defaultValues: { displayName: "", username: "", password: "", passwordConfirmation: "", phone: "" },
  });
  const registration = useMutation({
    mutationFn: requestStaffRegistration,
    onSuccess: (response) => setSubmittedUsername(response.username),
    onError: (error) => setError("root.server", { message: getArabicApiErrorMessage(error) }),
  });
  const businessName = identity.data?.businessName || "نظام إدارة المركز";

  return (
    <main className="login-background flex min-h-screen items-center justify-center p-5" dir="rtl">
      <Card className="w-full max-w-xl p-6 sm:p-9">
        <header className="text-center">
          {identity.data?.logoDataUrl ? <img className="mx-auto h-12 w-12 rounded-2xl border border-[#eee7ea] object-contain" src={identity.data.logoDataUrl} alt={`شعار ${businessName}`} /> : <span className="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-[#FCEAF2] text-[#A83868]"><Sparkles className="h-5 w-5" /></span>}
          <p className="mt-4 truncate text-sm font-black text-[#A83868]">{businessName}</p>
          <h1 className="mt-2 text-2xl font-black text-[#29252A]">إنشاء حساب موظف</h1>
          <p className="mt-2 text-sm leading-6 text-[#736873]">سيُراجع المالك الطلب ويحدد الصلاحية قبل إتاحة الدخول.</p>
        </header>
        {submittedUsername ? (
          <div className="mt-7 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-bold text-emerald-900" role="status"><CheckCircle2 className="mb-2 h-5 w-5" />تم إرسال طلب الحساب <span dir="ltr">{submittedUsername}</span>. انتظر موافقة المالك قبل تسجيل الدخول.<Link to="/login" className="mt-3 block text-[#356e50] underline">العودة لتسجيل الدخول</Link></div>
        ) : (
          <form className="mt-7 grid gap-5 sm:grid-cols-2" aria-label="نموذج إنشاء حساب موظف" onSubmit={handleSubmit((values) => registration.mutate({ ...values, displayName: values.displayName.trim(), username: values.username.trim(), phone: values.phone || undefined }))} noValidate>
            <FormField label="الاسم الكامل" htmlFor="registration-name" required error={errors.displayName?.message}><Input id="registration-name" autoComplete="name" {...register("displayName")} /></FormField>
            <FormField label="رقم الهاتف" htmlFor="registration-phone" error={errors.phone?.message}><div className="relative"><Phone className="pointer-events-none absolute right-3.5 top-3.5 h-5 w-5 text-[#a69aa2]" /><Input id="registration-phone" className="pr-11" type="tel" inputMode="tel" autoComplete="tel" dir="ltr" {...register("phone")} /></div></FormField>
            <FormField label="اسم المستخدم" htmlFor="registration-username" required error={errors.username?.message} hint="أحرف صغيرة وأرقام و . أو _ أو -"><div className="relative"><UserRound className="pointer-events-none absolute right-3.5 top-3.5 h-5 w-5 text-[#a69aa2]" /><Input id="registration-username" className="pr-11" autoComplete="username" dir="ltr" {...register("username")} /></div></FormField>
            <FormField label="كلمة المرور" htmlFor="registration-password" required error={errors.password?.message} hint="12 حرفًا على الأقل، وتتضمن حرفًا ورقمًا."><div className="relative"><LockKeyhole className="pointer-events-none absolute right-3.5 top-3.5 h-5 w-5 text-[#a69aa2]" /><Input id="registration-password" className="px-11" type={showPassword ? "text" : "password"} autoComplete="new-password" {...register("password")} /><button type="button" className="absolute left-2 top-2 grid h-8 w-8 place-items-center rounded-lg text-[#736873] hover:bg-[#FCEAF2]" aria-label={showPassword ? "إخفاء كلمة المرور" : "إظهار كلمة المرور"} onClick={() => setShowPassword((value) => !value)}>{showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}</button></div></FormField>
            <div className="sm:col-span-2"><FormField label="تأكيد كلمة المرور" htmlFor="registration-password-confirmation" required error={errors.passwordConfirmation?.message}><Input id="registration-password-confirmation" type={showPassword ? "text" : "password"} autoComplete="new-password" {...register("passwordConfirmation")} /></FormField></div>
            <div className="sm:col-span-2"><FormServerError message={errors.root?.server?.message} /><Button type="submit" size="lg" className="mt-4 w-full" disabled={registration.isPending}><UserPlus className="h-5 w-5" />{registration.isPending ? "جارٍ إرسال الطلب..." : "إرسال طلب الحساب"}</Button><Link to="/login" className="mt-4 flex min-h-11 items-center justify-center text-sm font-bold text-[#A83868] hover:underline">لديك حساب بالفعل؟ تسجيل الدخول</Link></div>
          </form>
        )}
      </Card>
    </main>
  );
}
