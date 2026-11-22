import { ImagePlus, Save, Trash2 } from "lucide-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { FormField, FormServerError } from "@/components/shared/form-field";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { businessProfileKeys, updateBusinessProfile } from "@/features/business/api";
import { useBusinessProfile } from "@/features/business/currency-context";
import type { BusinessProfile, UpdateBusinessProfileRequest } from "@/features/business/types";
import { getArabicApiErrorMessage } from "@/lib/api/errors";
import type { CurrencyCode } from "@/lib/format";

const currencies: Array<{ value: CurrencyCode; label: string }> = [
  { value: "EGP", label: "الجنيه المصري (ج.م)" },
  { value: "SAR", label: "الريال السعودي (ر.س)" },
  { value: "AED", label: "الدرهم الإماراتي (د.إ)" },
  { value: "USD", label: "الدولار الأمريكي (US$)" },
  { value: "EUR", label: "اليورو (€)" },
];

type FormValues = {
  businessName: string;
  logoDataUrl: string;
  phone: string;
  address: string;
  currency: CurrencyCode;
  receiptInformation: string;
};

export function BusinessProfileCard({ editable }: { editable: boolean }) {
  const queryClient = useQueryClient();
  const { profile, isLoading } = useBusinessProfile();
  const { register, handleSubmit, reset, setError, setValue, watch, formState: { errors } } = useForm<FormValues>({
    defaultValues: { businessName: "", logoDataUrl: "", phone: "", address: "", currency: "EGP", receiptInformation: "" },
  });
  const logoDataUrl = watch("logoDataUrl");

  useEffect(() => {
    if (!profile) return;
    reset(toFormValues(profile));
  }, [profile, reset]);

  const mutation = useMutation({
    mutationFn: updateBusinessProfile,
    onSuccess: (updated) => {
      queryClient.setQueryData(businessProfileKeys.current, updated);
      reset(toFormValues(updated));
    },
    onError: (error) => setError("root.server", { message: getArabicApiErrorMessage(error) }),
  });

  const selectLogo = async (file: File | undefined) => {
    if (!file) return;
    if (!/^image\/(png|jpeg|webp)$/.test(file.type)) {
      setError("logoDataUrl", { message: "اختر صورة PNG أو JPEG أو WebP." });
      return;
    }
    if (file.size > 256 * 1024) {
      setError("logoDataUrl", { message: "حجم الشعار يجب ألا يزيد عن 256 كيلوبايت." });
      return;
    }
    const dataUrl = await readFile(file);
    if (!dataUrl) {
      setError("logoDataUrl", { message: "تعذر قراءة الشعار." });
      return;
    }
    setValue("logoDataUrl", dataUrl, { shouldDirty: true });
  };

  const submit = (values: FormValues) => mutation.mutate({
    businessName: values.businessName.trim(),
    logoDataUrl: values.logoDataUrl || null,
    phone: values.phone.trim() || null,
    address: values.address.trim() || null,
    currency: values.currency,
    receiptInformation: values.receiptInformation.trim() || null,
  } satisfies UpdateBusinessProfileRequest);

  return (
    <Card className="p-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div><h2 className="text-lg font-black">بيانات مركز التجميل</h2><p className="mt-1 text-sm leading-6 text-stone-500">تظهر هذه البيانات والعملة على الإيصالات والتقارير دون تغيير أي مبلغ محفوظ.</p></div>
        {logoDataUrl ? <img className="h-14 w-14 rounded-xl border border-stone-200 object-contain" src={logoDataUrl} alt="شعار مركز التجميل" /> : null}
      </div>
      {isLoading && !profile ? <p className="mt-5 text-sm text-stone-500">جارٍ تحميل بيانات المركز...</p> : !editable ? <ProfileSummary profile={profile} /> : <form className="mt-6 grid gap-5 md:grid-cols-2" onSubmit={handleSubmit(submit)} noValidate>
        <FormField label="اسم مركز التجميل" htmlFor="business-name" required error={errors.businessName?.message}><Input id="business-name" maxLength={150} {...register("businessName", { required: "اسم المركز مطلوب." })} /></FormField>
        <FormField label="العملة" htmlFor="business-currency" required error={errors.currency?.message}><Select id="business-currency" {...register("currency")}>{currencies.map((currency) => <option key={currency.value} value={currency.value}>{currency.label}</option>)}</Select></FormField>
        <FormField label="رقم الهاتف" htmlFor="business-phone" error={errors.phone?.message}><Input id="business-phone" inputMode="tel" maxLength={50} {...register("phone")} /></FormField>
        <FormField label="العنوان" htmlFor="business-address" error={errors.address?.message}><Input id="business-address" maxLength={500} {...register("address")} /></FormField>
        <div className="md:col-span-2"><FormField label="معلومات الإيصال" htmlFor="receipt-information" error={errors.receiptInformation?.message} hint="مثال: رسالة شكر أو تعليمات مختصرة تظهر في الطباعة."><Textarea id="receipt-information" className="min-h-24" maxLength={1000} {...register("receiptInformation")} /></FormField></div>
        <div className="md:col-span-2"><FormField label="شعار المركز" htmlFor="business-logo" error={errors.logoDataUrl?.message} hint="اختياري — PNG أو JPEG أو WebP حتى 256 كيلوبايت."><div className="flex flex-wrap items-center gap-3"><label className="inline-flex h-12 cursor-pointer items-center gap-2 rounded-xl border border-stone-300 px-4 text-sm font-bold text-stone-700 hover:bg-stone-50"><ImagePlus className="h-4 w-4" />اختيار شعار<input id="business-logo" className="sr-only" type="file" accept="image/png,image/jpeg,image/webp" onChange={(event) => void selectLogo(event.target.files?.[0])} /></label>{logoDataUrl && <Button type="button" variant="ghost" className="text-red-700" onClick={() => setValue("logoDataUrl", "", { shouldDirty: true })}><Trash2 className="h-4 w-4" />إزالة الشعار</Button>}</div></FormField></div>
        <div className="md:col-span-2"><FormServerError message={errors.root?.server?.message} /><Button className="mt-2" type="submit" disabled={mutation.isPending}>{mutation.isPending ? "جارٍ الحفظ..." : <><Save className="h-4 w-4" />حفظ بيانات المركز</>}</Button></div>
      </form>}
    </Card>
  );
}

function toFormValues(profile: BusinessProfile): FormValues {
  return {
    businessName: profile.businessName,
    logoDataUrl: profile.logoDataUrl ?? "",
    phone: profile.phone ?? "",
    address: profile.address ?? "",
    currency: profile.currency,
    receiptInformation: profile.receiptInformation ?? "",
  };
}

async function readFile(file: File): Promise<string | null> {
  return new Promise((resolve) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => resolve(null);
    reader.readAsDataURL(file);
  });
}

function ProfileSummary({ profile }: { profile: BusinessProfile | undefined }) {
  if (!profile) return <p className="mt-5 text-sm text-stone-500">لا يمكن تحميل بيانات المركز الآن.</p>;
  return <dl className="mt-5 grid gap-4 sm:grid-cols-2"><Item label="اسم المركز" value={profile.businessName} /><Item label="العملة" value={currencies.find((currency) => currency.value === profile.currency)?.label ?? profile.currency} />{profile.phone && <Item label="الهاتف" value={profile.phone} />}{profile.address && <Item label="العنوان" value={profile.address} />}</dl>;
}

function Item({ label, value }: { label: string; value: string }) {
  return <div><dt className="text-xs font-bold text-stone-500">{label}</dt><dd className="mt-1 font-black text-stone-900">{value}</dd></div>;
}
