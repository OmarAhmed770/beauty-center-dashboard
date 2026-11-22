$ErrorActionPreference = 'Stop'
$safeConnection = $false
try {
    if ($env:DEMO_DB_URL.StartsWith('jdbc:postgresql://')) {
        $databaseUri = [uri]$env:DEMO_DB_URL.Substring(5)
        $local = $databaseUri.Host -in @('127.0.0.1', 'localhost')
        $approvedRemote = -not [string]::IsNullOrWhiteSpace($env:DEMO_ALLOWED_DB_HOST) -and $databaseUri.Host -eq $env:DEMO_ALLOWED_DB_HOST
        $safePort = if ($local) { $databaseUri.Port -in @(-1, 5432, 5433) } else { $databaseUri.Port -eq 5432 }
        $safeQuery = if ($local) { $databaseUri.Query -in @('', '?sslmode=require') } else { $databaseUri.Query -eq '?sslmode=require' }
        $databaseUser = if ($env:DEMO_DB_USERNAME) { $env:DEMO_DB_USERNAME } else { 'beauty_demo_app' }
        $safeConnection = $databaseUri.AbsolutePath -eq '/beauty_center_demo' -and -not $databaseUri.UserInfo -and -not $databaseUri.Fragment -and ($local -or $approvedRemote) -and $safePort -and $safeQuery -and $databaseUser -eq 'beauty_demo_app'
    }
} catch { $safeConnection = $false }
if (-not $safeConnection -or $env:DEMO_READ_ONLY -ne 'false') {
    throw 'Seed requires explicitly writable isolated demo mode with an approved database host and role.'
}
foreach ($variable in @('DEMO_OWNER_USERNAME', 'DEMO_OWNER_PASSWORD', 'DEMO_STAFF_PASSWORD')) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($variable, 'Process'))) {
        throw "Required Demo account variable is missing: $variable. No seed performed."
    }
}
$base = 'http://127.0.0.1:8082/api/v1'
$probe = Invoke-WebRequest "$base/setup/status" -UseBasicParsing
if ($probe.Headers['X-Beauty-Demo'] -ne 'local-seeding') { throw 'Server is not the verified demo instance. No seed performed.' }
if (-not ($probe.Content | ConvertFrom-Json).initialOwnerSetupRequired) { throw 'Demo already contains users. Refusing to seed twice or overwrite data.' }
$headers = @{}
function Api([string]$Method, [string]$Path, $Body = $null) {
    $args = @{ Method=$Method; Uri="$base$Path"; Headers=$headers; TimeoutSec=30; ContentType='application/json; charset=utf-8' }
    if ($null -ne $Body) { $args.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 12 -Compress)) }
    try { Invoke-RestMethod @args } catch {
        $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 'network' }
        # Never echo request bodies, authorization headers, or passwords.
        throw "Demo API failed: $Method $Path (HTTP $status). Existing seed data retained; no reset performed."
    }
}
$zone = [TimeZoneInfo]::FindSystemTimeZoneById('Egypt Standard Time')
$today = [TimeZoneInfo]::ConvertTimeFromUtc([DateTime]::UtcNow, $zone).Date
$from = $today.AddDays(-4).ToString('yyyy-MM-dd')
$null = Api POST '/setup/owner' @{ username=$env:DEMO_OWNER_USERNAME; displayName='مالك المركز التجريبي'; password=$env:DEMO_OWNER_PASSWORD; businessName='مركز إشراقة — نسخة تجريبية'; currency='EGP'; starterServices=@(); addDefaultExpenseCategories=$true; addDefaultPaymentMethods=$true }
$login = Api POST '/auth/login' @{ username=$env:DEMO_OWNER_USERNAME; password=$env:DEMO_OWNER_PASSWORD }
$headers.Authorization = "Bearer $($login.accessToken)"
$null = Api PUT '/business-profile' @{ businessName='مركز إشراقة — نسخة تجريبية'; currency='EGP'; phone='00000000000'; address='عنوان وهمي للعرض — لا يمثل أي فرع حقيقي'; receiptInformation='نسخة عرض توضيحي — بيانات وأسعار ومعاملات وهمية، ليست إيصالًا حقيقيًا.' }
$employees = @()
foreach ($employee in @(@{ name='سارة — تجريبية'; position='تصفيف الشعر'; defaultDailyWage=250 }, @{ name='نور — تجريبية'; position='مكياج وعناية'; defaultDailyWage=220 }, @{ name='ليلى — تجريبية'; position='أظافر واستقبال'; defaultDailyWage=200 })) {
    $employee.notes = 'شخصية وهمية للعرض فقط'
    $employees += Api POST '/employees' $employee
}
$null = Api POST '/users' @{ username='portfolio_staff'; displayName='موظفة العرض التجريبي'; password=$env:DEMO_STAFF_PASSWORD; roles=@('RECEPTIONIST'); permissions=@('ADD_ORDERS'); employeeLinkMode='EXISTING'; employeeId=$employees[2].id }
$categories = @{}
foreach ($category in @(@('hair','الشعر','Hair'), @('beauty','مكياج وعناية','Beauty'), @('nails','الأظافر','Nails'), @('extensions','تركيب الشعر','Extensions'), @('products','منتجات العناية','Products'))) {
    $categories[$category[0]] = Api POST '/catalog/categories' @{ nameArabic=$category[1]; nameEnglish=$category[2]; active=$true; description='تصنيف تجريبي' }
}
$items = @()
$definitions = @(
    @('DEMO_CUT','قص وتصفيف شعر','Haircut','hair','SERVICE',300,0),
    @('DEMO_BLOWDRY','سيشوار وتصفيف','Blow dry','hair','SERVICE',200,0),
    @('DEMO_MAKEUP','مكياج سهرة','Evening makeup','beauty','SERVICE',800,0),
    @('DEMO_FACIAL','جلسة عناية بالبشرة','Facial','beauty','SERVICE',450,0),
    @('DEMO_NAILS','مانيكير وعناية بالأظافر','Manicure','nails','SERVICE',180,0),
    @('DEMO_SHAMPOO','شامبو عناية تجريبي','Care shampoo','products','PRODUCT',250,120),
    @('DEMO_HAIR_BROWN','شعر بني طبيعي — تجريبي','Demo brown hair','extensions','HAIR_PRODUCT',20,9),
    @('DEMO_HAIR_BLACK','شعر أسود طبيعي — تجريبي','Demo black hair','extensions','HAIR_PRODUCT',24,11)
)
foreach ($item in $definitions) {
    $items += Api POST '/catalog/items' @{ code=$item[0]; nameArabic=$item[1]; nameEnglish=$item[2]; categoryId=$categories[$item[3]].id; itemType=$item[4]; currentSellingPrice=$item[5]; currentCost=$item[6]; active=$true; description='أسعار وبيانات افتراضية للعرض فقط' }
}
$hairConfigs = @()
for ($i=6; $i -lt 8; $i++) {
    $hairConfigs += Api POST '/hair-configurations' @{ hairItemId=$items[$i].id; brand='Demo Collection'; length='60 cm'; color=$(if ($i -eq 6) { 'بني' } else { 'أسود' }); unitType='gram'; unitSellingPrice=$definitions[$i][5]; unitCost=$definitions[$i][6]; installationFee=200; minimumQuantity=50; active=$true; effectiveDate=$from }
    $null = Api POST "/hair-stock/$($items[$i].id)/initialize" @{ quantityGrams=1500; reason='رصيد افتتاحي وهمي للعرض فقط' }
}
$discount = Api POST '/discounts' @{ name='عرض العناية التجريبي 10%'; type='PERCENTAGE'; value=10; startDate=$from; endDate=$today.AddMonths(2).ToString('yyyy-MM-dd'); active=$true; catalogItemId=$items[3].id }
$methods = (Api GET '/payment-methods?active=true&size=100').content
$cash = $methods | Where-Object code -EQ 'CASH' | Select-Object -First 1
$card = $methods | Where-Object type -EQ 'CARD' | Select-Object -First 1
$wallet = $methods | Where-Object type -EQ 'WALLET' | Select-Object -First 1
if (-not ($cash -and $card -and $wallet)) { throw 'Required demo payment methods missing.' }
$expenseCategories = (Api GET '/expense-categories?active=true&size=100').content
$paymentOptions = @($cash, $card, $wallet)
$orderCount = 0
for ($day = -4; $day -le 0; $day++) {
    $date = $today.AddDays($day).ToString('yyyy-MM-dd')
    $null = Api POST '/cash-adjustments' @{ businessDate=$date; type='CASH_IN'; amount=500; reason='عهدة افتتاحية تجريبية' }
    foreach ($employee in $employees) { $null = Api PUT "/employees/$($employee.id)/attendance?businessDate=$date" @{ status='PRESENT' } }
    for ($n=0; $n -lt 6; $n++) {
        $method = $paymentOptions[$n % 3]
        $lines = @(@{ catalogItemId=$items[$n].id; quantity=1; employeeId=$employees[$n % 3].id })
        if ($n -eq 0) { $lines += @{ catalogItemId=$items[4].id; quantity=1; employeeId=$employees[2].id } }
        $null = Api POST '/invoices/quick-orders' @{ businessDate=$date; lines=$lines; payments=@(@{paymentMethodId=$method.id; referenceNumber=$(if ($method.requiresReferenceNumber) { "DEMO-$date-$n" } else { $null })}); clientRequestId="portfolio-$date-$n"; customerName="عميلة تجريبية $($n+1)"; customerPhone='00000000000'; notes='عملية وهمية للعرض — ليست معاملة عميل حقيقي' }
        $orderCount++
    }
    $hairIndex = [Math]::Abs($day) % 2
    $null = Api POST '/invoices/quick-orders' @{ businessDate=$date; lines=@(@{ catalogItemId=$items[6+$hairIndex].id; quantity=1; hairConfigurationId=$hairConfigs[$hairIndex].id; hairGrams=100; employeeId=$employees[0].id }); payments=@(@{paymentMethodId=$cash.id}); clientRequestId="portfolio-$date-hair"; customerName='عميلة تركيب تجريبية'; customerPhone='00000000000'; notes='مثال خصم مخزون الشعر عبر دورة الأوردر الأصلية' }
    $orderCount++
    $null = Api POST '/expenses' @{ categoryId=$expenseCategories[0].id; amount=120; businessDate=$date; paymentMethodId=$cash.id; description='مستلزمات تشغيل تجريبية'; notes='بيانات وهمية' }
    $null = Api POST '/expenses' @{ categoryId=$expenseCategories[1].id; amount=80; businessDate=$date; paymentMethodId=$card.id; description='خدمة تشغيل تجريبية'; notes='بيانات وهمية' }
    $null = Api POST '/employee-transactions' @{ employeeId=$employees[0].id; type='COMMISSION'; amount=100; businessDate=$date; notes='عمولة خدمة تجريبية مستحقة وغير مدفوعة' }
    $null = Api POST '/employee-transactions' @{ employeeId=$employees[1].id; type='BONUS'; amount=50; businessDate=$date; notes='مكافأة تجريبية مستحقة وغير مدفوعة' }
    $null = Api POST '/employee-transactions' @{ employeeId=$employees[2].id; type='ADVANCE'; amount=50; businessDate=$date; paymentMethod=$cash.code; notes='سلفة تجريبية نقدية' }
    $null = Api POST '/employee-transactions' @{ employeeId=$employees[0].id; type='PAYMENT'; amount=150; businessDate=$date; paymentMethod=$cash.code; notes='سداد جزئي تجريبي للمستحقات' }
    if ($day -lt 0) {
        $preview = Api GET "/daily-closings/$date/preview"
        $null = Api POST "/daily-closings/$date/close" @{ actualCash=$preview.liveFinancials.expectedCashBalance; notes='إقفال تجريبي مطابق للنقدية المحسوبة' }
    }
}
# Demonstrate preserved historical price snapshots via the existing price-history API.
$null = Api POST "/catalog/items/$($items[0].id)/prices" @{ sellingPrice=320; reason='تحديث سعر تجريبي — الأوردرات السابقة لا تتغير' }
$yesterday = $today.AddDays(-1).ToString('yyyy-MM-dd')
$null = Api POST "/daily-closings/$yesterday/reopen" @{ reason='مثال تجريبي لمراجعة الإقفال مع الاحتفاظ بالإصدار السابق' }
$preview = Api GET "/daily-closings/$yesterday/preview"
$null = Api POST "/daily-closings/$yesterday/close" @{ actualCash=$preview.liveFinancials.expectedCashBalance; notes='إصدار ثانٍ تجريبي محفوظ بجوار الإصدار الأول' }
$dashboard = Api GET '/dashboard?filter=TODAY'
$stock = Api GET "/hair-stock/$($items[6].id)"
Write-Output "Seed complete through application APIs: 3 employees, 8 catalog items, 2 hair configurations, $orderCount completed orders, 10 expenses, 15 attendance records, 35 employee transactions, 5 preserved closing versions. Business date $($today.ToString('yyyy-MM-dd'))."
Write-Output "Demo stock verified: $($stock.availableGrams) grams for the first hair item. Today remains open; launcher will restart in read-only mode."
