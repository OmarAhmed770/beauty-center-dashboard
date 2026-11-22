import { ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";

export function ReportPageHeader({ title, description }: { title: string; description: string }) {
  return <PageHeader title={title} description={description} actions={<Button asChild variant="outline"><Link to="/reports"><ArrowRight className="h-4 w-4" />كل التقارير</Link></Button>} />;
}
