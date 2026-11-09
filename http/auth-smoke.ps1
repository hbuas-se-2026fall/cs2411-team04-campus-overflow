# W-01 等价冒烟脚本（PowerShell 7）
# IntelliJ 2025.2 的 HTTP Client 响应脚本存在 Object.defineProperty 引擎 bug，
# 此脚本用于提供可执行验证证据；auth.http 保留标准 IntelliJ 格式。
# 运行：pwsh -NoProfile -File http/auth-smoke.ps1

$ErrorActionPreference = 'Stop'

$base = 'http://127.0.0.1:8080'
$session = $null
$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds().ToString()
$suffix = $suffix.Substring($suffix.Length - 8)
$username = "auth$suffix"
$displayName = "smoke$suffix"
$password = 'SmokePass123'
$csrf = $null

function Get-Csrf {
    Invoke-RestMethod -Uri "$base/api/v1/auth/csrf" -WebSession $session | Out-Null
    $cookie = $session.Cookies.GetCookies($base) | Where-Object Name -eq 'XSRF-TOKEN'
    if (-not $cookie) { throw 'no XSRF-TOKEN cookie' }
    $cookie.Value
}

function Invoke-Api {
    param([string]$Method, [string]$Path, $Body = $null)

    $headers = @{}
    if ($csrf) { $headers['X-XSRF-TOKEN'] = $csrf }

    $params = @{
        Uri         = "$base$Path"
        Method      = $Method
        WebSession  = $session
        Headers     = $headers
        ErrorAction = 'Stop'
    }
    if ($null -ne $Body) {
        $params['ContentType'] = 'application/json'
        $params['Body'] = ($Body | ConvertTo-Json -Compress -Depth 10)
    }
    Invoke-RestMethod @params
}

$first = Invoke-RestMethod -Uri "$base/api/v1/auth/csrf" -SessionVariable session
$csrf = ($session.Cookies.GetCookies($base) | Where-Object Name -eq 'XSRF-TOKEN').Value
Write-Host "[0] CSRF 200"

Invoke-Api POST '/api/v1/auth/register' @{
    username    = $username
    displayName = $displayName
    email       = "$username@example.com"
    password    = $password
    college     = '计算机学院'
    className   = '软工2401'
} | Out-Null
Write-Host "[1] register 201 ($username)"

Invoke-Api POST '/api/v1/auth/login' @{
    username = $username
    password = $password
} | Out-Null
$csrf = Get-Csrf
Write-Host "[2] login 200"

$me = Invoke-Api GET '/api/v1/auth/me'
Write-Host "[3] me 200 ($($me.username))"

$courses = Invoke-Api GET '/api/v1/courses'
if (-not $courses -or $courses.Count -eq 0) {
    throw 'no course found; insert one first'
}
$courseId = $courses[0].id
Write-Host "[4] courses 200 (courseId=$courseId)"

Invoke-Api POST "/api/v1/courses/$courseId/join" | Out-Null
Write-Host "[5] join 204"

Write-Host "AUTH SMOKE PASSED username=$username courseId=$courseId"
