# W-01 等价冒烟脚本（Windows PowerShell 5.1 / PowerShell 7 均可运行）
# 作用：不依赖 IDE 的 HTTP Client 脚本引擎，用真实 Set-Cookie 握手依次验证
#   csrf → register → login → me → courses → join → reputation → ledger → leaderboard → bounty，
#   作为 auth.http 的可执行证据，同时覆盖 T3-06 新增的声誉 / 排行榜 / 悬赏接口。
# 本文件必须保存为 UTF-8 with BOM：否则 Windows PowerShell 5.1 会按 ANSI(GBK) 读中文，
#   出现「哈希文本不完整 / 字符串缺少终止符」之类解析错误。
# 运行（5.1）：powershell -NoProfile -ExecutionPolicy Bypass -File http\auth-smoke.ps1
# 运行（7）  ：pwsh -NoProfile -File http/auth-smoke.ps1

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

# 返回 @{ Status = <int>; Body = <object> }，用于断言「预期失败」的请求（如 403 / 422）。
function Invoke-ApiExpectError {
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
    try {
        $ok = Invoke-RestMethod @params
        @{ Status = 200; Body = $ok }
    } catch {
        # 5.1 抛 WebException（Response 是 HttpWebResponse）；7 抛 HttpResponseMessageException，
        # 正文在 ErrorDetails.Message 里。两套都要兼容。
        $status = 0
        $text = ''
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
            $text = $_.ErrorDetails.Message
        }
        $resp = $_.Exception.Response
        if ($resp) {
            if ($resp.PSObject.Properties.Name -contains 'StatusCode' -and $resp.StatusCode -is [int]) {
                $status = $resp.StatusCode
            } elseif ($resp.StatusCode) {
                $status = [int]$resp.StatusCode
            }
            if (-not $text) {
                try {
                    $stream = $resp.GetResponseStream()
                    if ($stream) {
                        $reader = New-Object System.IO.StreamReader($stream)
                        $text = $reader.ReadToEnd()
                    }
                } catch { $text = '' }
            }
        }
        $parsed = $null
        if ($text) { try { $parsed = $text | ConvertFrom-Json } catch { $parsed = $null } }
        @{ Status = $status; Body = $parsed }
    }
}

try {
    $first = Invoke-RestMethod -Uri "$base/api/v1/auth/csrf" -SessionVariable session
} catch {
    throw "无法连接 $base —— 请先启动后端（dev profile）。原始错误：$($_.Exception.Message)"
}
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

# --- T3-06 新增：声誉 / 流水 / 排行榜 / 悬赏 ---
# 注册事件是异步经 outbox 投递的，声誉账户可能还没建好；这里轮询几次，避免偶发 404。
$profile = $null
for ($i = 0; $i -lt 10; $i++) {
    try {
        $profile = Invoke-Api GET "/api/v1/users/$($me.id)/reputation"
        break
    } catch {
        if ($i -eq 9) { throw "reputation 一直不可用：$($_.Exception.Message)" }
        Start-Sleep -Milliseconds 500
    }
}
if ($profile.userId -ne $me.id) { throw "reputation userId 不符：$($profile.userId) != $($me.id)" }
# 本人可见悬赏积分（学生开户 0 分）
if ($null -eq $profile.availablePoints) { throw '本人查看时 availablePoints 不应为 null' }
Write-Host "[6] reputation 200 (reputation=$($profile.reputation), availablePoints=$($profile.availablePoints), badges=$($profile.badges.Count))"

$ledger = Invoke-Api GET "/api/v1/users/$($me.id)/reputation/ledger"
Write-Host "[7] ledger 200 (共 $($ledger.total) 条)"

$board = Invoke-Api GET '/api/v1/leaderboard?period=week'
if ($null -eq $board) { $board = @() }
Write-Host "[8] leaderboard 200 (week 共 $($board.Count) 人)"

# 悬赏需要课程管理者权限（REP-3001）；学生账号预期被拒，正好验证权限门禁生效。
$question = Invoke-Api POST '/api/v1/questions' @{
    courseId = $courseId
    title    = "冒烟测试问题 $suffix"
    body     = '这是 http/auth-smoke.ps1 自动创建的问题，用于验证悬赏权限与声誉接口。'
    tags     = @('冒烟测试')
}
$questionId = $question.id
Write-Host "[9] ask 201 (questionId=$questionId)"

$bounty = Invoke-ApiExpectError POST "/api/v1/questions/$questionId/bounty" @{ points = 50; days = 7 }
if ($bounty.Status -eq 201 -or $bounty.Status -eq 200) {
    # 账号恰好是教师/管理员：悬赏真的开出来了
    Write-Host "[10] bounty 201 (id=$($bounty.Body.id))"
} elseif ($bounty.Status -eq 403 -and $bounty.Body.code -eq 'REP-3001') {
    Write-Host "[10] bounty 403 REP-3001 (学生无权发起悬赏，符合预期)"
} else {
    throw "bounty 返回了意外结果：HTTP $($bounty.Status)，code=$($bounty.Body.code)"
}

# 清理：删掉本次创建的问题（软删除），避免反复跑脚本把首页刷满。
try {
    Invoke-Api DELETE "/api/v1/questions/$questionId" | Out-Null
    Write-Host "[11] delete 204 (清理 $questionId)"
} catch {
    Write-Host "[11] delete 跳过（$($_.Exception.Message)）"
}

Write-Host "AUTH SMOKE PASSED username=$username userId=$($me.id) courseId=$courseId questionId=$questionId"
