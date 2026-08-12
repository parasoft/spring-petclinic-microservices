<#
.SYNOPSIS
    Extracts Parasoft coverage agent jars and configures agent.properties for all
    four instrumented microservices. Safe to run multiple times (idempotent).
    Used by both Jenkinsfile.deploy and local developers on Windows.

.EXAMPLE
    # Jars only - no CTP/DTP API calls
    .\scripts\setup-coverage.ps1

.EXAMPLE
    # Full setup with CTP subscription queues and DTP filter ID resolved automatically
    $env:PARASOFT_USER = "user"; $env:PARASOFT_PASS = "pass"
    .\scripts\setup-coverage.ps1 -CtpUrl http://ctp:8080 -EnvId 4 -DtpUrl http://dtp:8083 -BuildId baseline

.NOTES
    Requires: Docker (unless -SkipJars), PowerShell 5.1+
    Credentials: set PARASOFT_USER and PARASOFT_PASS environment variables before running.
#>
[CmdletBinding()]
param(
    [string] $CtpUrl   = "",
    [string] $EnvId    = "",
    [string] $DtpUrl   = "",
    [string] $BuildId  = "baseline",
    [string] $AppName  = "spring-petclinic-microservices",
    [switch] $SkipJars,
    [switch] $UseCTPWsUrl,
    [switch] $CiDebug
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ─── Constants ────────────────────────────────────────────────────────────────

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot  = Split-Path -Parent $ScriptDir
$Template  = Join-Path $RepoRoot "jtest\coverage\agent.properties"
$CtpImage  = "parasoft/ctp:latest"
$CtpJarPath = "/usr/local/parasoft/ctp/webapps/em/coverage/Java/jtest_agent"

# Maps service directory to CTP component name — must match CTP environment configuration
$Services = [ordered]@{
    "spring-petclinic-api-gateway"      = "UI + API Gateway"
    "spring-petclinic-customers-service" = "customers-service REST API"
    "spring-petclinic-vets-service"     = "vets-service REST API"
    "spring-petclinic-visits-service"   = "visits-service REST API"
}

# ─── Validation ───────────────────────────────────────────────────────────────

if (($CtpUrl -and -not $EnvId) -or (-not $CtpUrl -and $EnvId)) {
    Write-Error "ERROR: -CtpUrl and -EnvId must be provided together."
}

if ($CtpUrl -or $DtpUrl) {
    $user = $env:PARASOFT_USER
    $pass = $env:PARASOFT_PASS
    if (-not $user -or -not $pass) {
        Write-Error "ERROR: PARASOFT_USER and PARASOFT_PASS env vars must be set when using CTP/DTP APIs."
    }
}

if (-not $SkipJars -and -not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "ERROR: docker is required for jar extraction. Use -SkipJars to skip."
}

if (-not (Test-Path $Template)) {
    Write-Error "ERROR: Template not found at $Template"
}

# Strip trailing slashes from URLs
$CtpUrl = $CtpUrl.TrimEnd('/')
$DtpUrl = $DtpUrl.TrimEnd('/')

# ─── Helpers ──────────────────────────────────────────────────────────────────

function Log   { param([string]$Msg) Write-Host "[setup-coverage] $Msg" }
function Debug { param([string]$Msg) if ($CiDebug) { Write-Host "[debug] $Msg" } }

function Invoke-Api {
    param([string]$Url)
    $user = $env:PARASOFT_USER
    $pass = $env:PARASOFT_PASS
    # Use curl.exe (-sk = silent + allow self-signed certs), mirroring the bash script.
    # Invoke-RestMethod has unreliable TLS behaviour with self-signed certs on Windows PS 5.1.
    if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
        $json = & curl.exe -sk -u "${user}:${pass}" -H 'Accept: application/json' $Url
        return $json | ConvertFrom-Json
    }
    # Fallback for environments without curl.exe (PS 7+ SkipCertificateCheck path)
    $base64 = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${user}:${pass}"))
    $headers = @{ Authorization = "Basic $base64"; Accept = "application/json" }
    return Invoke-RestMethod -Uri $Url -Headers $headers -SkipCertificateCheck
}

# Replaces a property line in a file using multiline regex (equivalent to sed -i)
function Set-Property {
    param([string]$File, [string]$Key, [string]$Value)
    $escaped = [Regex]::Escape($Key)
    $content = Get-Content -Path $File -Raw
    $content = $content -replace "(?m)^${escaped}=.*", "${Key}=${Value}"
    Set-Content -Path $File -Value $content -NoNewline
}

Set-Location $RepoRoot

# ─── Step 1: Extract jars from CTP Docker image ───────────────────────────────

$Container = ""
try {
    if (-not $SkipJars) {
        Log "Pulling coverage agent jars from ${CtpImage}..."
        $Container = (docker create $CtpImage).Trim()
        foreach ($service in $Services.Keys) {
            $coverageDir = Join-Path $RepoRoot "${service}\src\test\resources\coverage"
            New-Item -ItemType Directory -Force -Path $coverageDir | Out-Null
            docker cp "${Container}:${CtpJarPath}/." $coverageDir
            Log "  Jars -> $coverageDir"
        }
        docker rm $Container | Out-Null
        $Container = ""
    }

    # ─── Step 2: Resolve CTP component settings ───────────────────────────────

    $ComponentsData = $null
    if ($CtpUrl) {
        Log "Fetching CTP environment ${EnvId} components..."
        $ComponentsData = Invoke-Api "${CtpUrl}/em/api/v3/environments/${EnvId}/components"
        Debug "CTP components: $($ComponentsData | ConvertTo-Json -Depth 5)"
    }

    # ─── Step 3: Resolve DTP filter ID ────────────────────────────────────────

    $FilterId = ""
    if ($DtpUrl) {
        Log "Resolving DTP filter ID for '${AppName}'..."
        $filterData = Invoke-Api "${DtpUrl}/grs/api/v1.12/filters?managedOnly=false&name=${AppName}"
        Debug "DTP filter response: $($filterData | ConvertTo-Json)"
        if (-not $filterData -or $filterData.Count -eq 0) {
            Write-Error "ERROR: Could not resolve DTP filter ID for '${AppName}'."
        }
        $FilterId = $filterData[0].id.ToString()
        Log "  Resolved DTP filter ID: $FilterId"
    }

    # ─── Step 4: Generate per-service agent.properties ────────────────────────

    foreach ($entry in $Services.GetEnumerator()) {
        $service  = $entry.Key
        $ctpName  = $entry.Value
        $coverageDir = Join-Path $RepoRoot "${service}\src\test\resources\coverage"
        $propsFile   = Join-Path $coverageDir "agent.properties"

        Log "Configuring ${service}..."
        New-Item -ItemType Directory -Force -Path $coverageDir | Out-Null
        Copy-Item -Path $Template -Destination $propsFile -Force

        # Patch ctp.subscription.queue from CTP API.
        # ctp.websocket.url is only patched when -UseCTPWsUrl is set (e.g. Jenkins, where CTP
        # is on a remote host). For local Docker deployments the template value is correct as-is.
        if ($ComponentsData) {
            $component = $ComponentsData.components | Where-Object { $_.name -eq $ctpName }
            if (-not $component) {
                Write-Error "ERROR: CTP component '${ctpName}' not found in environment ${EnvId}."
            }
            if ($UseCTPWsUrl) {
                Set-Property $propsFile "ctp.websocket.url" $component.ctpWebsocketUrl
            }
            Set-Property $propsFile "ctp.subscription.queue" $component.ctpSubscriptionQueue
        }

        # Patch common DTP properties
        Set-Property $propsFile "dtp.project"         $AppName
        Set-Property $propsFile "dtp.buildID"         "${AppName}-${BuildId}"
        Set-Property $propsFile "dtp.coverageImages"  "${AppName};${AppName}-FT"
        if ($FilterId) {
            Set-Property $propsFile "dtp.filterID" $FilterId
        }

        if ($CiDebug) {
            Write-Host "--- $propsFile ---"
            Get-Content $propsFile | Write-Host
            Write-Host "---"
        }
        Log "  $propsFile written."
    }

    # ─── Step 5: Create runtime data directories for compose bind mounts ──────

    Log "Creating runtime data directories..."
    $runtimeDirs = @(
        "spring-petclinic-api-gateway\src\test\resources\coverage\runtime_coverage_api_gateway1",
        "spring-petclinic-api-gateway\src\test\resources\coverage\runtime_coverage_api_gateway2",
        "spring-petclinic-customers-service\src\test\resources\coverage\runtime_coverage_customer1",
        "spring-petclinic-customers-service\src\test\resources\coverage\runtime_coverage_customer2",
        "spring-petclinic-vets-service\src\test\resources\coverage\runtime_coverage_vets1",
        "spring-petclinic-vets-service\src\test\resources\coverage\runtime_coverage_vets2",
        "spring-petclinic-visits-service\src\test\resources\coverage\runtime_coverage_visit1",
        "spring-petclinic-visits-service\src\test\resources\coverage\runtime_coverage_visit2"
    )
    foreach ($dir in $runtimeDirs) {
        New-Item -ItemType Directory -Force -Path (Join-Path $RepoRoot $dir) | Out-Null
    }

    Log "Coverage setup complete."

} finally {
    # Ensure the temporary Docker container is always removed on error
    if ($Container) {
        docker rm $Container | Out-Null
    }
}
