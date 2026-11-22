[CmdletBinding()]
param(
    [string]$EnvironmentFile = (Join-Path $PSScriptRoot '.env.acceptance.local')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http

function Import-AcceptanceEnvironment {
    param([Parameter(Mandatory = $true)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw 'Acceptance environment file not found. Copy .env.acceptance.example to .env.acceptance.local and fill its placeholders privately.'
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmedLine = $line.Trim()
        if ($trimmedLine.Length -eq 0 -or $trimmedLine.StartsWith('#')) {
            continue
        }

        $separatorIndex = $trimmedLine.IndexOf('=')
        if ($separatorIndex -le 0) {
            throw 'The acceptance environment file contains an invalid line. Use NAME=value syntax.'
        }

        $name = $trimmedLine.Substring(0, $separatorIndex).Trim()
        $value = $trimmedLine.Substring($separatorIndex + 1)
        if ($name -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
            throw 'The acceptance environment file contains an invalid variable name.'
        }
        Set-Item -Path "Env:$name" -Value $value
    }
}

function Assert-AcceptanceValue {
    param(
        [Parameter(Mandatory = $true)][bool]$Condition,
        [Parameter(Mandatory = $true)][string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

Import-AcceptanceEnvironment -Path $EnvironmentFile

$requiredNames = @(
    'ACCEPTANCE_DB_URL',
    'ACCEPTANCE_OWNER_USERNAME',
    'ACCEPTANCE_OWNER_PASSWORD'
)
foreach ($name in $requiredNames) {
    $variable = Get-Item -Path "Env:$name" -ErrorAction SilentlyContinue
    Assert-AcceptanceValue -Condition ($null -ne $variable -and -not [string]::IsNullOrWhiteSpace($variable.Value)) -Message "Required acceptance variable is missing: $name. Its value was not displayed."
}

Assert-AcceptanceValue -Condition ($env:ACCEPTANCE_DB_URL -match '(?i)acceptance') -Message 'The configured database is not clearly an isolated acceptance database. Verification stopped before any API call.'

$serverAddress = if ($env:ACCEPTANCE_SERVER_ADDRESS) { $env:ACCEPTANCE_SERVER_ADDRESS } else { '127.0.0.1' }
Assert-AcceptanceValue -Condition ($serverAddress -in @('127.0.0.1', 'localhost', '::1')) -Message 'Acceptance verification only permits a loopback backend address.'
$serverPort = if ($env:ACCEPTANCE_SERVER_PORT) { $env:ACCEPTANCE_SERVER_PORT } else { '8081' }
$baseUri = "http://${serverAddress}:$serverPort"
$script:httpClient = [System.Net.Http.HttpClient]::new()

function Invoke-AcceptanceApi {
    param(
        [Parameter(Mandatory = $true)][ValidateSet('GET', 'POST', 'PUT')][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [object]$Body,
        [int]$ExpectedStatus = 200
    )

    $request = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::new($Method),
        "$baseUri$Path")
    if (-not [string]::IsNullOrWhiteSpace($script:accessToken)) {
        $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new(
            'Bearer', $script:accessToken)
    }
    if ($null -ne $Body) {
        $request.Content = [System.Net.Http.StringContent]::new(
            ($Body | ConvertTo-Json -Depth 8 -Compress),
            [System.Text.Encoding]::UTF8,
            'application/json')
    }

    $response = $script:httpClient.SendAsync($request).GetAwaiter().GetResult()
    try {
        Assert-AcceptanceValue -Condition ([int]$response.StatusCode -eq $ExpectedStatus) -Message "Unexpected HTTP status for $Path."
        return ($response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json)
    } finally {
        $response.Dispose()
        $request.Dispose()
    }
}

function Invoke-AcceptanceParallelPosts {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][object[]]$Bodies
    )

    $requests = @()
    try {
        $tasks = foreach ($body in $Bodies) {
            $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Post, "$baseUri$Path")
            $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $script:accessToken)
            $request.Content = [System.Net.Http.StringContent]::new(
                ($body | ConvertTo-Json -Depth 8 -Compress),
                [System.Text.Encoding]::UTF8,
                'application/json')
            $requests += $request
            $script:httpClient.SendAsync($request)
        }
        return @($tasks | ForEach-Object {
            $response = $_.GetAwaiter().GetResult()
            try {
                [pscustomobject]@{
                    StatusCode = [int]$response.StatusCode
                    Body = ($response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json)
                }
            } finally {
                $response.Dispose()
            }
        })
    } finally {
        foreach ($request in $requests) {
            $request.Dispose()
        }
    }
}

try {
    $loginResponse = Invoke-AcceptanceApi -Method POST -Path '/api/v1/auth/login' -Body @{
        username = $env:ACCEPTANCE_OWNER_USERNAME
        password = $env:ACCEPTANCE_OWNER_PASSWORD
    }
    $script:accessToken = $loginResponse.accessToken
    Assert-AcceptanceValue -Condition (-not [string]::IsNullOrWhiteSpace($script:accessToken)) -Message 'Acceptance login returned no access token.'

    $verificationId = "PGREG$([Guid]::NewGuid().ToString('N').Substring(0, 12).ToUpperInvariant())"
    $businessDate = (Get-Date).ToString('yyyy-MM-dd')
    $paymentMethods = Invoke-AcceptanceApi -Method GET -Path '/api/v1/payment-methods?page=0&size=100'
    $cashMethod = @($paymentMethods.content | Where-Object { $_.code -eq 'CASH' -and $_.active }) | Select-Object -First 1
    Assert-AcceptanceValue -Condition ($null -ne $cashMethod) -Message 'An active CASH payment method is required for isolated acceptance verification.'

    $manualDescription = "PostgreSQL regression manual $verificationId"
    $manualOrder = Invoke-AcceptanceApi -Method POST -Path '/api/v1/invoices/simple-orders' -ExpectedStatus 201 -Body @{
        businessDate = $businessDate
        customerName = 'PostgreSQL verification customer'
        customerPhone = '01000000000'
        description = $manualDescription
        totalAmount = 12.34
        paymentMethodId = [long]$cashMethod.id
        notes = "Acceptance verification $verificationId"
    }
    $manualDetails = Invoke-AcceptanceApi -Method GET -Path "/api/v1/invoices/$($manualOrder.id)"
    Assert-AcceptanceValue -Condition ($manualDetails.items.Count -eq 1 -and $manualDetails.items[0].manualItem -and $manualDetails.items[0].itemNameArabic -eq $manualDescription) -Message 'The manual simple order snapshot is not returned correctly.'

    $catalogCategory = Invoke-AcceptanceApi -Method POST -Path '/api/v1/catalog/categories' -ExpectedStatus 201 -Body @{
        nameArabic = "PostgreSQL category $verificationId"
        nameEnglish = "PostgreSQL category $verificationId"
        active = $true
        description = 'Isolated acceptance regression data; retained intentionally.'
    }
    $catalogItem = Invoke-AcceptanceApi -Method POST -Path '/api/v1/catalog/items' -ExpectedStatus 201 -Body @{
        code = $verificationId
        nameArabic = "PostgreSQL catalog item $verificationId"
        nameEnglish = "PostgreSQL catalog item $verificationId"
        itemType = 'SERVICE'
        categoryId = [long]$catalogCategory.id
        currentSellingPrice = 23.45
        currentCost = 5.00
        active = $true
        description = 'Isolated acceptance regression data; retained intentionally.'
    }
    $hairItem = Invoke-AcceptanceApi -Method POST -Path '/api/v1/catalog/items' -ExpectedStatus 201 -Body @{
        code = "HAIR$verificationId"
        nameArabic = "PostgreSQL hair item $verificationId"
        nameEnglish = "PostgreSQL hair item $verificationId"
        itemType = 'HAIR_PRODUCT'
        categoryId = [long]$catalogCategory.id
        currentSellingPrice = 30.00
        currentCost = 10.00
        active = $true
        description = 'Isolated acceptance regression data; retained intentionally.'
    }
    $hairConfiguration = Invoke-AcceptanceApi -Method POST -Path '/api/v1/hair-configurations' -ExpectedStatus 201 -Body @{
        hairItemId = [long]$hairItem.id
        brand = "PostgreSQL brand $verificationId"
        length = '50 cm'
        unitType = 'gram'
        unitSellingPrice = 2.00
        unitCost = 1.00
        installationFee = 5.00
        active = $true
        effectiveDate = $businessDate
    }
    Invoke-AcceptanceApi -Method POST -Path "/api/v1/hair-stock/$($hairItem.id)/initialize" -ExpectedStatus 201 -Body @{
        quantityGrams = 5.000
        reason = "PostgreSQL opening stock $verificationId"
    } | Out-Null
    $parallelHairOrders = Invoke-AcceptanceParallelPosts -Path '/api/v1/invoices/quick-orders' -Bodies @(
        @{
            businessDate = $businessDate
            clientRequestId = "pg-hair-a-$verificationId"
            customerName = "PostgreSQL hair A $verificationId"
            lines = @(@{ catalogItemId = [long]$hairItem.id; quantity = 3.000; hairConfigurationId = [long]$hairConfiguration.id; hairGrams = 3.000 })
            payments = @(@{ paymentMethodId = [long]$cashMethod.id; amount = 11.00 })
        },
        @{
            businessDate = $businessDate
            clientRequestId = "pg-hair-b-$verificationId"
            customerName = "PostgreSQL hair B $verificationId"
            lines = @(@{ catalogItemId = [long]$hairItem.id; quantity = 3.000; hairConfigurationId = [long]$hairConfiguration.id; hairGrams = 3.000 })
            payments = @(@{ paymentMethodId = [long]$cashMethod.id; amount = 11.00 })
        }
    )
    $parallelStatuses = @($parallelHairOrders | ForEach-Object { $_.StatusCode } | Sort-Object)
    Assert-AcceptanceValue -Condition ($parallelStatuses.Count -eq 2 -and $parallelStatuses[0] -eq 201 -and $parallelStatuses[1] -eq 409) -Message 'Concurrent PostgreSQL hair orders did not produce exactly one completed sale and one insufficient-stock rejection.'
    $completedHairOrder = @($parallelHairOrders | Where-Object { $_.StatusCode -eq 201 }) | Select-Object -First 1
    $hairStockAfterConcurrentSale = Invoke-AcceptanceApi -Method GET -Path "/api/v1/hair-stock/$($hairItem.id)"
    Assert-AcceptanceValue -Condition ($hairStockAfterConcurrentSale.stockConfigured -and [decimal]$hairStockAfterConcurrentSale.availableGrams -eq [decimal]2.000) -Message 'Concurrent PostgreSQL hair sales produced an incorrect available balance.'
    $hairMovements = Invoke-AcceptanceApi -Method GET -Path "/api/v1/hair-stock/$($hairItem.id)/movements?page=0&size=100"
    Assert-AcceptanceValue -Condition (@($hairMovements.content | Where-Object { $_.movementType -eq 'SALE' }).Count -eq 1) -Message 'Concurrent PostgreSQL hair orders wrote an incorrect number of sale movements.'
    $completedHairDetails = Invoke-AcceptanceApi -Method GET -Path "/api/v1/invoices/$($completedHairOrder.Body.invoiceId)"
    $completedHairItem = @($completedHairDetails.items | Where-Object { $_.catalogItemId -eq $hairItem.id }) | Select-Object -First 1
    Assert-AcceptanceValue -Condition ($null -ne $completedHairItem -and [decimal]$completedHairItem.quantity -eq [decimal]3.000 -and [decimal]$completedHairItem.unitOriginalSellingPrice -eq [decimal]2.00 -and [decimal]$completedHairItem.installationFeeSnapshot -eq [decimal]5.00) -Message 'Hair invoice snapshots are incomplete or incorrect.'
    Invoke-AcceptanceApi -Method POST -Path "/api/v1/invoices/$($completedHairOrder.Body.invoiceId)/cancel" -Body @{ reason = "PostgreSQL hair cancellation $verificationId" } | Out-Null
    $hairStockAfterCancellation = Invoke-AcceptanceApi -Method GET -Path "/api/v1/hair-stock/$($hairItem.id)"
    Assert-AcceptanceValue -Condition ([decimal]$hairStockAfterCancellation.availableGrams -eq [decimal]5.000) -Message 'Cancelling a PostgreSQL hair invoice did not restore its sold grams.'
    $refundableHairOrder = Invoke-AcceptanceApi -Method POST -Path '/api/v1/invoices/quick-orders' -ExpectedStatus 201 -Body @{
        businessDate = $businessDate
        clientRequestId = "pg-hair-refund-$verificationId"
        customerName = "PostgreSQL hair refund $verificationId"
        lines = @(@{ catalogItemId = [long]$hairItem.id; quantity = 3.000; hairConfigurationId = [long]$hairConfiguration.id; hairGrams = 3.000 })
        payments = @(@{ paymentMethodId = [long]$cashMethod.id; amount = 11.00 })
    }
    $refundableHairDetails = Invoke-AcceptanceApi -Method GET -Path "/api/v1/invoices/$($refundableHairOrder.invoiceId)"
    $refundableHairItem = @($refundableHairDetails.items | Where-Object { $_.catalogItemId -eq $hairItem.id }) | Select-Object -First 1
    Invoke-AcceptanceApi -Method POST -Path "/api/v1/invoices/$($refundableHairOrder.invoiceId)/refunds" -ExpectedStatus 201 -Body @{
        businessDate = $businessDate
        amount = 1.00
        reason = "PostgreSQL partial hair return $verificationId"
        payments = @(@{ paymentMethodId = [long]$cashMethod.id; amount = 1.00 })
        hairReturns = @(@{ invoiceItemId = [long]$refundableHairItem.id; grams = 1.000 })
    } | Out-Null
    $hairStockAfterRefund = Invoke-AcceptanceApi -Method GET -Path "/api/v1/hair-stock/$($hairItem.id)"
    Assert-AcceptanceValue -Condition ([decimal]$hairStockAfterRefund.availableGrams -eq [decimal]3.000) -Message 'A PostgreSQL hair refund did not restore only the declared returned grams.'
    $globalDiscount = Invoke-AcceptanceApi -Method POST -Path '/api/v1/discounts' -ExpectedStatus 201 -Body @{
        name = "PostgreSQL global discount $verificationId"
        type = 'PERCENTAGE'
        value = 5.00
        startDate = $businessDate
        endDate = $businessDate
        active = $true
    }
    $itemDiscount = Invoke-AcceptanceApi -Method POST -Path '/api/v1/discounts' -ExpectedStatus 201 -Body @{
        name = "PostgreSQL item discount $verificationId"
        type = 'PERCENTAGE'
        value = 5.00
        startDate = $businessDate
        endDate = $businessDate
        active = $true
        catalogItemId = [long]$catalogItem.id
    }
    $categoryDiscount = Invoke-AcceptanceApi -Method POST -Path '/api/v1/discounts' -ExpectedStatus 201 -Body @{
        name = "PostgreSQL category discount $verificationId"
        type = 'PERCENTAGE'
        value = 5.00
        startDate = $businessDate
        endDate = $businessDate
        active = $true
        categoryId = [long]$catalogCategory.id
    }
    $discountSearch = Invoke-AcceptanceApi -Method GET -Path "/api/v1/discounts?active=true&applicableDate=$businessDate&page=0&size=100"
    $discountIds = @($discountSearch.content | ForEach-Object { [long]$_.id })
    Assert-AcceptanceValue -Condition (
        ($discountIds -contains [long]$globalDiscount.id) -and
        ($discountIds -contains [long]$itemDiscount.id) -and
        ($discountIds -contains [long]$categoryDiscount.id)) -Message 'The PostgreSQL discount search did not return global, item-scoped, and category-scoped records.'
    $discountPage = Invoke-AcceptanceApi -Method GET -Path "/api/v1/discounts?active=true&applicableDate=$businessDate&query=&page=0&size=1"
    Assert-AcceptanceValue -Condition ($discountPage.totalElements -ge 3 -and $discountPage.content.Count -eq 1) -Message 'The PostgreSQL discount search pagination is incorrect.'
    $discountEmpty = Invoke-AcceptanceApi -Method GET -Path "/api/v1/discounts?active=true&applicableDate=$businessDate&query=NO_MATCH_$verificationId&page=0&size=100"
    Assert-AcceptanceValue -Condition ($discountEmpty.totalElements -eq 0) -Message 'The PostgreSQL discount search did not return an empty result for an unmatched query.'
    $hairSearch = Invoke-AcceptanceApi -Method GET -Path "/api/v1/hair-configurations?active=true&effectiveOn=$businessDate&page=0&size=100"
    $verifiedHairConfiguration = @($hairSearch.content | Where-Object { [long]$_.id -eq [long]$hairConfiguration.id }) | Select-Object -First 1
    Assert-AcceptanceValue -Condition ($null -ne $verifiedHairConfiguration -and $null -eq $verifiedHairConfiguration.color) -Message 'The PostgreSQL hair configuration search did not return the nullable-color configuration.'
    $hairPage = Invoke-AcceptanceApi -Method GET -Path "/api/v1/hair-configurations?active=true&effectiveOn=$businessDate&query=&page=0&size=1"
    Assert-AcceptanceValue -Condition ($hairPage.totalElements -ge 1 -and $hairPage.content.Count -eq 1) -Message 'The PostgreSQL hair configuration search pagination is incorrect.'
    $hairEmpty = Invoke-AcceptanceApi -Method GET -Path "/api/v1/hair-configurations?active=true&effectiveOn=$businessDate&query=NO_MATCH_$verificationId&page=0&size=100"
    Assert-AcceptanceValue -Condition ($hairEmpty.totalElements -eq 0) -Message 'The PostgreSQL hair configuration search did not return an empty result for an unmatched query.'
    $catalogDraft = Invoke-AcceptanceApi -Method POST -Path '/api/v1/invoices' -ExpectedStatus 201 -Body @{
        businessDate = $businessDate
        customerName = 'PostgreSQL catalog verification customer'
        notes = "Acceptance verification $verificationId"
    }
    Invoke-AcceptanceApi -Method POST -Path "/api/v1/invoices/$($catalogDraft.id)/items" -ExpectedStatus 201 -Body @{
        catalogItemId = [long]$catalogItem.id
        quantity = 1
    } | Out-Null
    Invoke-AcceptanceApi -Method PUT -Path "/api/v1/invoices/$($catalogDraft.id)/payments" -Body @{
        payments = @(@{
            paymentMethodId = [long]$cashMethod.id
            amount = 23.45
        })
    } | Out-Null
    $catalogInvoice = Invoke-AcceptanceApi -Method POST -Path "/api/v1/invoices/$($catalogDraft.id)/complete"
    $catalogDetails = Invoke-AcceptanceApi -Method GET -Path "/api/v1/invoices/$($catalogInvoice.id)"
    Assert-AcceptanceValue -Condition ($catalogDetails.items.Count -eq 1 -and -not $catalogDetails.items[0].manualItem -and $catalogDetails.items[0].catalogItemId -eq $catalogItem.id) -Message 'The catalog invoice snapshot is not returned correctly.'

    $invoiceList = Invoke-AcceptanceApi -Method GET -Path "/api/v1/invoices?fromDate=$businessDate&toDate=$businessDate&page=0&size=100"
    Assert-AcceptanceValue -Condition (($invoiceList.content.id -contains $manualOrder.id) -and ($invoiceList.content.id -contains $catalogInvoice.id)) -Message 'The PostgreSQL invoice list did not include both verification invoices.'
    for ($index = 1; $index -lt $invoiceList.content.Count; $index++) {
        $previous = $invoiceList.content[$index - 1]
        $current = $invoiceList.content[$index]
        Assert-AcceptanceValue -Condition (($previous.businessDate -gt $current.businessDate) -or ($previous.businessDate -eq $current.businessDate -and [long]$previous.id -ge [long]$current.id)) -Message 'Invoice default sorting is not business date descending then id descending.'
    }
    Invoke-AcceptanceApi -Method GET -Path '/api/v1/invoices?page=0&size=20' | Out-Null
    Invoke-AcceptanceApi -Method GET -Path '/api/v1/invoices?query=&page=0&size=20' | Out-Null

    $expenseCategories = Invoke-AcceptanceApi -Method GET -Path '/api/v1/expense-categories?active=true&page=0&size=100'
    $expenseCategory = @($expenseCategories.content) | Select-Object -First 1
    Assert-AcceptanceValue -Condition ($null -ne $expenseCategory) -Message 'An active expense category is required for isolated acceptance verification.'
    $expenseDescription = "PostgreSQL regression expense $verificationId"
    Invoke-AcceptanceApi -Method POST -Path '/api/v1/expenses' -ExpectedStatus 201 -Body @{
        categoryId = [long]$expenseCategory.id
        amount = 4.56
        businessDate = $businessDate
        paymentMethodId = [long]$cashMethod.id
        description = $expenseDescription
        notes = "Acceptance verification $verificationId"
    } | Out-Null
    $expenseList = Invoke-AcceptanceApi -Method GET -Path "/api/v1/expenses?fromDate=$businessDate&toDate=$businessDate&query=$verificationId&page=0&size=20"
    Assert-AcceptanceValue -Condition ($expenseList.content.description -contains $expenseDescription) -Message 'The PostgreSQL expense list did not include the newly created verification expense.'

    $dashboard = Invoke-AcceptanceApi -Method GET -Path '/api/v1/dashboard?filter=TODAY'
    $financialReport = Invoke-AcceptanceApi -Method GET -Path "/api/v1/reports/financial?fromDate=$businessDate&toDate=$businessDate"
    Assert-AcceptanceValue -Condition ([decimal]$dashboard.grossSales -eq [decimal]$financialReport.grossSales -and [decimal]$dashboard.expenses -eq [decimal]$financialReport.expenses -and [decimal]$dashboard.netSalesAfterExpenses -eq [decimal]$financialReport.netSalesAfterExpenses) -Message 'Dashboard and financial report totals disagree for the same date.'
    Assert-AcceptanceValue -Condition ([int]$dashboard.completedInvoiceCount -ge 2 -and [decimal]$dashboard.grossSales -ge [decimal]35.79 -and [decimal]$dashboard.expenses -ge [decimal]4.56) -Message 'Dashboard does not include the acceptance verification activity.'

    Write-Host 'PostgreSQL acceptance regression verification passed.'
    Write-Host 'Verified PostgreSQL discount and hair-configuration search filters, empty results, nullable hair color, pagination, invoice list/filter/default sorting, snapshots, expenses, and dashboard/report agreement.'
    Write-Host 'The verification added only clearly marked records to the isolated acceptance database; it did not delete or modify existing records.'
} catch {
    throw "PostgreSQL acceptance regression verification failed at script line $($_.InvocationInfo.ScriptLineNumber): $($_.Exception.Message)"
} finally {
    $script:httpClient.Dispose()
}
