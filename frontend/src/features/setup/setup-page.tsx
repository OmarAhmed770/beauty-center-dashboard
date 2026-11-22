import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery } from "@tanstack/react-query";
import { ArrowRight, ArrowLeft, KeyRound, Plus, ShieldCheck, Trash2, UserRound } from "lucide-react";
import { useState } from "react";
import { useFieldArray, useForm } from "react-hook-form";
import { Navigate, useNavigate } from "react-router-dom";
import { z } from "zod";
import { ErrorState } from "@/components/shared/error-state";
import { FormField, FormServerError } from "@/components/shared/form-field";
import { LoadingState } from "@/components/shared/loading-state";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { createInitialOwner, requestInitialSetupStatus } from "@/lib/api/auth";
import { getArabicApiErrorMessage } from "@/lib/api/errors";

const categories = [
  ["HAIR", "شعر"], ["MAKEUP", "مكياج"], ["SKIN_CARE", "عناية بالبشرة"], ["NAILS", "أظافر"], ["HAIR_INSTALLATION", "تركيب شعر"],
] as const;
const currencies = [["EGP", "الجنيه المصري"], ["SAR", "الريال السعودي"], ["AED", "الدرهم الإماراتي"], ["USD", "الدولار الأمريكي"], ["EUR", "اليورو"]] as const;

const schema = z.object({
  username: z.string().trim().regex(/^[a-z0-9._-]{3,100}$/, "استخدم 3 أحرف صغيرة أو أرقام أو . أو _ أو -"),
  displayName: z.string().trim().min(1, "اسم العرض مطلوب").max(150, "اسم العرض طويل جدًا"),
  password: z.string().min(12, "كلمة المرور يجب ألا تقل عن 12 حرفًا").max(72, "كلمة المرور طويلة جدًا"),
  passwordConfirmation: z.string(),
  businessName: z.string().trim().min(1, "اسم المركز مطلوب").max(150, "اسم المركز طويل جدًا"),
  currency: z.enum(["EGP", "SAR", "AED", "USD", "EUR"]),
  starterServices: z.array(z.object({ nameArabic: z.string().trim().min(1, "اسم الخدمة مطلوب").max(150, "اسم الخدمة طويل جدًا"), category: z.enum(["HAIR", "MAKEUP", "SKIN_CARE", "NAILS", "HAIR_INSTALLATION"]) })),
  addDefaultExpenseCategories: z.boolean(),
  addDefaultPaymentMethods: z.boolean(),
}).refine((values) => values.password === values.passwordConfirmation, { message: "تأكيد كلمة المرور غير مطابق", path: ["passwordConfirmation"] });

type Values = z.infer<typeof schema>;
const stepNames = ["حساب المالك", "بيانات المركز", "خدمات مبدئية", "إعدادات اختيارية"];

export function SetupPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState(0);
  const status = useQuery({ queryKey: ["setup", "status"], queryFn: requestInitialSetupStatus, retry: false });
  const { register, control, handleSubmit, trigger, setError, formState: { errors } } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { username: "", displayName: "", password: "", passwordConfirmation: "", businessName: "مركز التجميل", currency: "EGP", starterServices: [], addDefaultExpenseCategories: false, addDefaultPaymentMethods: false },
  });
  const services = useFieldArray({ control, name: "starterServices" });
  const createOwner = useMutation({
    mutationFn: ({ passwordConfirmation: _passwordConfirmation, ...request }: Values) => createInitialOwner(request),
    onSuccess: () => navigate("/login", { replace: true, state: { setupCompleted: true } }),
    onError: (error) => setError("root.server", { message: getArabicApiErrorMessage(error) }),
  });
  const next = async () => {
    const fields: Array<Array<keyof Values>> = [["username", "displayName", "password", "passwordConfirmation"], ["businessName", "currency"], ["starterServices"], []];
    if (await trigger(fields[step])) setStep((current) => Math.min(current + 1, stepNames.length - 1));
  };

  if (status.isPending) return <LoadingState fullPage label="جارٍ التحقق من إعداد النظام..." />;
  if (status.isError) return <div className="flex min-h-screen items-center justify-center p-6"><ErrorState error={status.error} onRetry={() => void status.refetch()} /></div>;
  if (!status.data.initialOwnerSetupRequired) return <Navigate to="/login" replace />;

  return <main className="login-background flex min-h-screen items-center justify-center p-5" dir="rtl"><Card className="w-full max-w-2xl p-6 sm:p-10"><span className="grid h-14 w-14 place-items-center rounded-2xl bg-rose-100 text-rose-800"><ShieldCheck className="h-7 w-7" /></span><p className="mt-6 text-sm font-bold text-rose-700">إعداد آمن لمرة واحدة</p><h1 className="mt-2 text-3xl font-black text-stone-950">إعداد مركزك</h1><ol className="mt-6 grid gap-2 text-sm sm:grid-cols-4">{stepNames.map((name, index) => <li key={name} className={`rounded-xl px-3 py-2 font-bold ${index === step ? "bg-rose-100 text-rose-800" : index < step ? "bg-emerald-50 text-emerald-800" : "bg-stone-100 text-stone-500"}`}>{index + 1}. {name}</li>)}</ol><form className="mt-8 space-y-5" onSubmit={handleSubmit((values) => createOwner.mutate(values))} noValidate>{step === 0 && <><p className="text-sm leading-7 text-stone-600">أنشئ حساب المالك الأول. بعد الحفظ لن تظهر هذه الصفحة مرة أخرى.</p><div className="grid gap-5 sm:grid-cols-2"><FormField label="اسم المستخدم" htmlFor="setup-username" required error={errors.username?.message}><Input id="setup-username" autoComplete="username" dir="ltr" {...register("username")} /></FormField><FormField label="اسم العرض" htmlFor="setup-display-name" required error={errors.displayName?.message}><Input id="setup-display-name" autoComplete="name" {...register("displayName")} /></FormField><FormField label="كلمة المرور" htmlFor="setup-password" required error={errors.password?.message} hint="12 حرفًا على الأقل."><Input id="setup-password" type="password" autoComplete="new-password" {...register("password")} /></FormField><FormField label="تأكيد كلمة المرور" htmlFor="setup-password-confirmation" required error={errors.passwordConfirmation?.message}><Input id="setup-password-confirmation" type="password" autoComplete="new-password" {...register("passwordConfirmation")} /></FormField></div><div className="flex gap-3 rounded-xl bg-stone-50 p-4 text-sm leading-6 text-stone-600"><KeyRound className="mt-0.5 h-5 w-5 shrink-0 text-stone-500" />لا تُعرض كلمة المرور أو تُحفظ في المتصفح.</div></>}{step === 1 && <div className="grid gap-5 sm:grid-cols-2"><FormField label="اسم المركز" htmlFor="setup-business-name" required error={errors.businessName?.message}><Input id="setup-business-name" autoComplete="organization" {...register("businessName")} /></FormField><FormField label="العملة" htmlFor="setup-currency" required error={errors.currency?.message}><Select id="setup-currency" {...register("currency")}>{currencies.map(([code, label]) => <option key={code} value={code}>{label}</option>)}</Select></FormField></div>}{step === 2 && <><p className="text-sm leading-7 text-stone-600">أضف خدماتك الأساسية إن أردت. ستُحفظ بدون سعر وبحالة غير نشطة حتى تحدد السعر وتفعلها لاحقًا.</p><div className="space-y-3">{services.fields.map((field, index) => <div key={field.id} className="grid gap-3 rounded-2xl border border-stone-200 p-3 sm:grid-cols-[1fr_190px_auto]"><FormField label={`اسم الخدمة ${index + 1}`} htmlFor={`starter-name-${field.id}`} error={errors.starterServices?.[index]?.nameArabic?.message}><Input id={`starter-name-${field.id}`} {...register(`starterServices.${index}.nameArabic`)} /></FormField><FormField label="التصنيف" htmlFor={`starter-category-${field.id}`} error={errors.starterServices?.[index]?.category?.message}><Select id={`starter-category-${field.id}`} {...register(`starterServices.${index}.category`)}>{categories.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</Select></FormField><Button type="button" variant="ghost" className="self-end text-red-700" onClick={() => services.remove(index)} aria-label="حذف الخدمة"><Trash2 className="h-4 w-4" />حذف</Button></div>)}</div><Button type="button" variant="outline" onClick={() => services.append({ nameArabic: "", category: "HAIR" })}><Plus className="h-4 w-4" />إضافة خدمة</Button></>}{step === 3 && <div className="space-y-4"><p className="text-sm leading-7 text-stone-600">اختياري: أضف البيانات الجاهزة التي لا تتوفر أصلًا. يمكنك إدارتها لاحقًا من الإعدادات.</p><label className="flex min-h-12 items-center gap-3 rounded-2xl border border-stone-200 p-4 text-sm font-bold"><input type="checkbox" {...register("addDefaultExpenseCategories")} />إضافة تصنيفات المصروفات الافتراضية</label><label className="flex min-h-12 items-center gap-3 rounded-2xl border border-stone-200 p-4 text-sm font-bold"><input type="checkbox" {...register("addDefaultPaymentMethods")} />إضافة طرق الدفع الافتراضية</label></div>}<FormServerError message={errors.root?.server?.message} /><div className="flex flex-col-reverse gap-3 border-t border-stone-100 pt-5 sm:flex-row sm:justify-between">{step > 0 ? <Button type="button" variant="outline" onClick={() => setStep((current) => current - 1)} disabled={createOwner.isPending}><ArrowRight className="h-4 w-4" />السابق</Button> : <span />}{step < stepNames.length - 1 ? <Button type="button" onClick={() => void next()}>التالي<ArrowLeft className="h-4 w-4" /></Button> : <Button type="submit" size="lg" disabled={createOwner.isPending}><UserRound className="h-5 w-5" />{createOwner.isPending ? "جارٍ إنشاء الحساب..." : "إنهاء الإعداد"}</Button>}</div></form></Card></main>;
}
