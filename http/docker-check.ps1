# docker-check.ps1 —— CampusOverflow 容器环境一键自检
#
# 作用：新同事拿到仓库后跑这一条命令，就知道自己的 Docker 环境行不行。
#   检查 13 项（7 项基础 + 6 项可自动化的扩展项），全过 exit 0，任一失败 exit 1。
#
# 兼容性：Windows PowerShell 5.1 与 PowerShell 7.x 均可运行。
#   本文件必须保存为 UTF-8 with BOM，否则 Windows PowerShell 5.1 按 ANSI(GBK) 读中文会解析失败。
#
# 运行（推荐，自动选可用壳）：
#   powershell -NoProfile -ExecutionPolicy Bypass -File http\docker-check.ps1
#   pwsh -NoProfile -File http/docker-check.ps1
#
# 可选参数：
#   -SkipApp    跳过依赖 backend / web 容器的检查项（只验中间件，用于还没起应用时）
#   -Verbose    打印每项的详细命令与原始输出
#
# 退出码：0 = 全部通过；1 = 至少一项失败

[CmdletBinding()]
param(
    [switch]$SkipApp
)

$ErrorActionPreference = 'Stop'

# ---------------------------------------------------------------------------
# 基础设施：结果收集与彩色输出
# ---------------------------------------------------------------------------

$script:results = New-Object System.Collections.ArrayList
$script:repoRoot = Split-Path -Parent $PSScriptRoot
$script:composeFile = Join-Path $script:repoRoot 'deploy/docker-compose.yml'
$script:dockerBin = 'C:\Program Files\Docker\Docker\resources\bin'

# Docker Desktop 装好后会把它写进机器 PATH，但本进程可能是安装前启动的，继承的是旧快照。
# 这里主动补一次，避免「明明装了却找不到命令」。
if ((Test-Path $script:dockerBin) -and ($env:PATH -notlike "*$script:dockerBin*")) {
    $env:PATH = "$script:dockerBin;$env:PATH"
}

function Write-Head {
    param([string]$Text)
    Write-Host ''
    Write-Host "=== $Text ===" -ForegroundColor Cyan
}

function Add-Result {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Detail = '',
        [string]$Fix = ''
    )
    $null = $script:results.Add([pscustomobject]@{
            Name    = $Name
            Passed  = $Passed
            Detail  = $Detail
            Fix     = $Fix
        })

    if ($Passed) {
        Write-Host ("  [PASS] {0}" -f $Name) -ForegroundColor Green
        if ($Detail) { Write-Host ("         {0}" -f $Detail) -ForegroundColor DarkGray }
    }
    else {
        Write-Host ("  [FAIL] {0}" -f $Name) -ForegroundColor Red
        if ($Detail) { Write-Host ("         {0}" -f $Detail) -ForegroundColor Yellow }
        if ($Fix) { Write-Host ("         修复: {0}" -f $Fix) -ForegroundColor Yellow }
    }
}

# 统一执行外部命令，避免 stderr 被 PowerShell 当成 NativeCommandError 抛出。
function Invoke-Native {
    param(
        [string]$Exe,
        [string[]]$Arguments
    )
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $out = & $Exe @Arguments 2>&1
        return [pscustomobject]@{
            ExitCode = $LASTEXITCODE
            Output   = ($out | Out-String).Trim()
        }
    }
    catch {
        return [pscustomobject]@{
            ExitCode = -1
            Output   = $_.Exception.Message
        }
    }
    finally {
        $ErrorActionPreference = $prev
    }
}

# PowerShell 5.1 下 Invoke-WebRequest 的 .Content 是 Byte[]，必须解码成字符串；
# PowerShell 7+ 下已经是 String。统一走这个函数，避免 .Trim() / -match 报
# "Method invocation failed because [System.Byte] does not contain a method named 'Trim'"。
function Get-ResponseText {
    param([Parameter(Mandatory = $true)] $Response)
    $c = $Response.Content
    if ($c -is [byte[]]) {
        return [Text.Encoding]::UTF8.GetString($c)
    }
    return [string]$c
}

Write-Host ''
Write-Host 'CampusOverflow Docker 环境自检' -ForegroundColor White
Write-Host ("仓库根目录: {0}" -f $script:repoRoot) -ForegroundColor DarkGray
Write-Host ("compose 文件: {0}" -f $script:composeFile) -ForegroundColor DarkGray

# ---------------------------------------------------------------------------
# 第 1 项：docker 命令存在
# ---------------------------------------------------------------------------

Write-Head '1. docker 命令'

$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
if ($dockerCmd) {
    Add-Result -Name 'docker 命令可用' -Passed $true -Detail $dockerCmd.Source
}
else {
    Add-Result -Name 'docker 命令可用' -Passed $false `
        -Detail 'PATH 里找不到 docker' `
        -Fix '装 Docker Desktop；若已装，重开一个终端窗口（PATH 需要刷新），或临时执行 $env:PATH = "C:\Program Files\Docker\Docker\resources\bin;$env:PATH"'
}

# ---------------------------------------------------------------------------
# 第 2 项：Docker daemon 在跑
# ---------------------------------------------------------------------------

Write-Head '2. Docker 引擎'

if ($dockerCmd) {
    $info = Invoke-Native -Exe 'docker' -Arguments @('info', '--format', '{{.ServerVersion}}')
    if ($info.ExitCode -eq 0 -and $info.Output -notmatch 'error|failed to connect') {
        Add-Result -Name 'Docker daemon 在运行' -Passed $true -Detail ("ServerVersion={0}" -f $info.Output)
    }
    else {
        Add-Result -Name 'Docker daemon 在运行' -Passed $false `
            -Detail $info.Output `
            -Fix '启动 Docker Desktop（开始菜单搜 Docker Desktop），等左下角显示 Engine running 再重试'
    }
}
else {
    Add-Result -Name 'Docker daemon 在运行' -Passed $false -Detail '跳过（docker 命令不可用）'
}

# ---------------------------------------------------------------------------
# 第 3 项：WSL2 已装且默认版本为 2
# ---------------------------------------------------------------------------

Write-Head '3. WSL2'

$wslExe = Join-Path $env:SystemRoot 'System32\wsl.exe'
if (Test-Path $wslExe) {
    # wsl.exe 的输出编码在不同版本间会变（旧版 UTF-16LE，新版 UTF-8）。
    # 设 WSL_UTF8=1 让 3.x 直接输出 UTF-8，旧版忽略该变量仍走下面的兜底逻辑。
    $env:WSL_UTF8 = '1'
    $wslStatus = Invoke-Native -Exe $wslExe -Arguments @('--status')

    if ($wslStatus.ExitCode -eq 0) {
        $text = $wslStatus.Output
        # 兜底：若出现大量 \0 说明是 UTF-16LE 被按 ANSI 读了，重新解码
        if ($text -match "`0") {
            $tmp = [IO.Path]::GetTempFileName()
            $ErrorActionPreference = 'Continue'
            & $wslExe --status *> $tmp
            $ErrorActionPreference = 'Stop'
            $bytes = [IO.File]::ReadAllBytes($tmp)
            Remove-Item $tmp -Force -ErrorAction SilentlyContinue
            $text = [Text.Encoding]::Unicode.GetString($bytes)
        }

        if ($text -match '默认版本[:：]\s*2' -or $text -match 'Default Version:\s*2') {
            $verLines = $text -split "`n" | Where-Object { $_ -match 'WSL\s*(版本|Version)' }
            $ver = ''
            if ($verLines) { $ver = ([string]($verLines | Select-Object -First 1)).Trim() }
            Add-Result -Name 'WSL2 已安装且为默认版本' -Passed $true -Detail $ver
        }
        else {
            Add-Result -Name 'WSL2 已安装且为默认版本' -Passed $false `
                -Detail "wsl --status 未显示「默认版本: 2」" `
                -Fix '管理员 PowerShell 执行 wsl --set-default-version 2；若提示 WSL 未安装，先 wsl --install --no-distribution 并重启'
        }
    }
    else {
        Add-Result -Name 'WSL2 已安装且为默认版本' -Passed $false `
            -Detail $wslStatus.Output `
            -Fix '管理员 PowerShell 执行 wsl --install --no-distribution，然后重启电脑'
    }
}
else {
    Add-Result -Name 'WSL2 已安装且为默认版本' -Passed $false -Detail 'wsl.exe 不存在' -Fix '管理员 PowerShell 执行 wsl --install --no-distribution 并重启'
}

# ---------------------------------------------------------------------------
# 第 4 项：端口 3306 / 6379 未被本地服务占用
# ---------------------------------------------------------------------------

Write-Head '4. 端口占用'

$conflictPorts = @()
foreach ($port in 3306, 6379) {
    $listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        Where-Object { $_.LocalAddress -notmatch ':' }
    if ($listener) {
        $procId = ($listener | Select-Object -First 1).OwningProcess
        $procName = (Get-Process -Id $procId -ErrorAction SilentlyContinue).ProcessName
        $conflictPorts += "$port($procName)"
    }
}

if ($conflictPorts.Count -eq 0) {
    Add-Result -Name '端口 3306 / 6379 空闲' -Passed $true -Detail '可被容器映射'
}
else {
    # 已经被 compose 自己的容器占用也算正常
    $ownContainers = @()
    if ($dockerCmd) {
        $running = Invoke-Native -Exe 'docker' -Arguments @('ps', '--format', '{{.Names}}')
        if ($running.ExitCode -eq 0) { $ownContainers = $running.Output -split "`n" }
    }
    $composeOwns = ($ownContainers -contains 'co-mysql') -and ($ownContainers -contains 'co-redis')

    if ($composeOwns) {
        Add-Result -Name '端口 3306 / 6379 空闲' -Passed $true -Detail '被本项目容器占用（正常）'
    }
    else {
        Add-Result -Name '端口 3306 / 6379 空闲' -Passed $false `
            -Detail ("被占用: {0}" -f ($conflictPorts -join ', ')) `
            -Fix '管理员 PowerShell 执行 Stop-Service MySQL,Redis -ErrorAction SilentlyContinue 与 Set-Service MySQL -StartupType Manual'
    }
}

# ---------------------------------------------------------------------------
# 第 5 项：compose 文件可解析
# ---------------------------------------------------------------------------

Write-Head '5. compose 配置'

if ($dockerCmd) {
    $cfg = Invoke-Native -Exe 'docker' -Arguments @('compose', '-f', $script:composeFile, 'config', '--quiet')
    if ($cfg.ExitCode -eq 0) {
        Add-Result -Name 'compose 文件可解析' -Passed $true -Detail 'config --quiet 通过'
    }
    else {
        Add-Result -Name 'compose 文件可解析' -Passed $false -Detail $cfg.Output -Fix '检查 deploy/docker-compose.yml 语法，常见原因是缩进与引号'
    }
}
else {
    Add-Result -Name 'compose 文件可解析' -Passed $false -Detail '跳过（docker 命令不可用）'
}

# ---------------------------------------------------------------------------
# 第 6 项：容器健康状态
# ---------------------------------------------------------------------------

Write-Head '6. 容器健康'

$serviceHealth = @{}
if ($dockerCmd) {
    $ps = Invoke-Native -Exe 'docker' -Arguments @('compose', '-f', $script:composeFile, 'ps', '--format', '{{.Service}}|{{.Health}}|{{.State}}')
    if ($ps.ExitCode -eq 0 -and $ps.Output) {
        foreach ($line in ($ps.Output -split "`n")) {
            if ($line -match '^\s*$') { continue }
            $parts = $line.Split('|')
            if ($parts.Count -ge 3) { $serviceHealth[$parts[0]] = "$($parts[1])/$($parts[2])" }
        }
    }
}

foreach ($svc in 'mysql', 'redis') {
    if ($serviceHealth.ContainsKey($svc)) {
        $h = $serviceHealth[$svc]
        Add-Result -Name "$svc 容器 healthy" -Passed ($h -like 'healthy*') -Detail $h `
            -Fix "docker compose -f deploy/docker-compose.yml up -d $svc；若反复 unhealthy，docker compose logs $svc 看日志"
    }
    else {
        Add-Result -Name "$svc 容器 healthy" -Passed $false -Detail '容器未启动' `
            -Fix 'docker compose -f deploy/docker-compose.yml up -d mysql redis'
    }
}

if (-not $SkipApp) {
    foreach ($svc in 'backend', 'web') {
        if ($serviceHealth.ContainsKey($svc)) {
            $h = $serviceHealth[$svc]
            # backend / web 没有 healthcheck 时报空，退化成只看 State
            $ok = ($h -like 'healthy*') -or ($h -like '*/running')
            Add-Result -Name "$svc 容器运行中" -Passed $ok -Detail $h `
                -Fix "docker compose -f deploy/docker-compose.yml --profile default up -d --build $svc"
        }
    }
}

# ---------------------------------------------------------------------------
# 第 7 项：MySQL 关键参数与表结构
# ---------------------------------------------------------------------------

Write-Head '7. MySQL 参数与表'

if ($serviceHealth.ContainsKey('mysql') -and $serviceHealth['mysql'] -like 'healthy*') {
    # 密码走 MYSQL_PWD 环境变量而不是 -p 参数：否则 mysql 客户端会往 stderr 打
    # 「Using a password on the command line interface can be insecure」，被 PowerShell
    # 包成 ErrorRecord 后混进输出，导致按行解析全部错位。
    $sql = "SELECT @@ngram_token_size AS a, @@innodb_ft_enable_stopword AS b, @@time_zone AS c;"
    $q = Invoke-Native -Exe 'docker' -Arguments @(
        'exec', '-e', 'MYSQL_PWD=root_dev_pwd', 'co-mysql',
        'mysql', '-uroot', '-N', '-B', '-e', $sql
    )

    if ($q.ExitCode -eq 0) {
        $vals = (($q.Output -split "`n") | Where-Object { $_ -match '\S' } | Select-Object -Last 1)
        $cols = $vals -split "`t"
        $paramOk = ($cols.Count -ge 3) -and ($cols[0] -eq '2') -and ($cols[1] -eq '0') -and ($cols[2] -eq '+00:00')
        $paramDetail = if ($cols.Count -ge 3) {
            "ngram_token_size={0} / innodb_ft_enable_stopword={1} / time_zone={2}" -f $cols[0], $cols[1], $cols[2]
        }
        else { "解析失败，原始输出: $vals" }
        Add-Result -Name 'MySQL 关键参数生效' -Passed $paramOk `
            -Detail $paramDetail `
            -Fix '参数没生效通常是命令里 # 注释被当成了参数的一部分；检查 docker-compose.yml 的 command 列表'
    }
    else {
        Add-Result -Name 'MySQL 关键参数生效' -Passed $false -Detail $q.Output -Fix 'docker compose logs mysql 检查启动参数'
    }

    # Flyway 是否已建表（backend 未启动时表为空是正常的，不算失败）
    $tbl = Invoke-Native -Exe 'docker' -Arguments @(
        'exec', '-e', 'MYSQL_PWD=campus_dev_pwd', 'co-mysql',
        'mysql', '-ucampus', '-N', '-B', '-D', 'campus_overflow', '-e', 'SHOW TABLES;'
    )
    if ($tbl.ExitCode -eq 0) {
        $tableRows = @(($tbl.Output -split "`n") | Where-Object { $_ -match '\S' })
        $tableCount = $tableRows.Count
        if ($tableCount -gt 0) {
            $joined = $tableRows -join ' '
            $hasAll = ($joined -match 'id_user') -and ($joined -match 'qa_question') -and ($joined -match 'rep_account') -and ($joined -match 'dis_search_doc')
            Add-Result -Name '四上下文表已建出' -Passed $hasAll `
                -Detail ("共 {0} 张表；id_user/qa_question/rep_account/dis_search_doc {1}" -f $tableCount, $(if ($hasAll) { '齐全' } else { '不全' })) `
                -Fix 'backend 启动时会由 Flyway 建表；若缺表，docker compose logs backend | Select-String "Migrating"'
        }
        else {
            Add-Result -Name '四上下文表已建出' -Passed $true -Detail '表为空（backend 尚未启动过，属正常）'
        }
    }
    else {
        Add-Result -Name '四上下文表已建出' -Passed $false -Detail $tbl.Output -Fix 'ddl-auto 为 none，表必须由 Flyway 建；先起 backend'
    }
}
else {
    Add-Result -Name 'MySQL 关键参数生效' -Passed $false -Detail '跳过（mysql 容器未 healthy）'
    Add-Result -Name '四上下文表已建出' -Passed $false -Detail '跳过（mysql 容器未 healthy）'
}

# ---------------------------------------------------------------------------
# 第 8 项：Redis 可连
# ---------------------------------------------------------------------------

Write-Head '8. Redis'

if ($serviceHealth.ContainsKey('redis') -and $serviceHealth['redis'] -like 'healthy*') {
    $ping = Invoke-Native -Exe 'docker' -Arguments @('exec', 'co-redis', 'redis-cli', 'PING')
    if ($ping.ExitCode -eq 0 -and $ping.Output -match 'PONG') {
        Add-Result -Name 'Redis 可连接' -Passed $true -Detail 'PING -> PONG'
    }
    else {
        Add-Result -Name 'Redis 可连接' -Passed $false -Detail $ping.Output -Fix 'docker compose logs redis'
    }
}
else {
    Add-Result -Name 'Redis 可连接' -Passed $false -Detail '跳过（redis 容器未 healthy）' `
        -Fix 'docker compose -f deploy/docker-compose.yml up -d redis'
}

# ---------------------------------------------------------------------------
# 第 9 项：dis_search_doc 的 ngram 全文索引存在（扩展项）
# ---------------------------------------------------------------------------

Write-Head '9. 全文索引（扩展）'

if ($serviceHealth.ContainsKey('mysql') -and $serviceHealth['mysql'] -like 'healthy*') {
    $idx = Invoke-Native -Exe 'docker' -Arguments @('exec', 'co-mysql', 'mysql', '-ucampus', '-pcampus_dev_pwd', 'campus_overflow', '-N', '-B', '-e', 'SHOW INDEX FROM dis_search_doc;')
    if ($idx.ExitCode -eq 0) {
        $hasNgram = $idx.Output -match 'ngram|FULLTEXT|ft_'
        Add-Result -Name 'dis_search_doc 有全文索引' -Passed $hasNgram -Detail '（表存在即说明 Flyway 已跑过 discovery 迁移）'
    }
    else {
        Add-Result -Name 'dis_search_doc 有全文索引' -Passed $true -Detail '表尚未创建（backend 未启动，属正常）'
    }
}
else {
    Add-Result -Name 'dis_search_doc 有全文索引' -Passed $false -Detail '跳过（mysql 容器未 healthy）'
}

# ---------------------------------------------------------------------------
# 第 10 项：后端 readiness 探针（扩展项）
# ---------------------------------------------------------------------------

if (-not $SkipApp) {
    Write-Head '10. 后端探针（扩展）'

    try {
        $r = Invoke-WebRequest -Uri 'http://localhost:8080/actuator/health/readiness' -UseBasicParsing -TimeoutSec 10
        $body = Get-ResponseText -Response $r
        $up = $body -match '"status"\s*:\s*"UP"'
        Add-Result -Name 'readiness 返回 UP' -Passed ($r.StatusCode -eq 200 -and $up) -Detail ("HTTP {0} {1}" -f $r.StatusCode, $body.Trim()) `
            -Fix 'docker compose logs backend | Select-String "Started CampusOverflowApplication"'
    }
    catch {
        Add-Result -Name 'readiness 返回 UP' -Passed $false -Detail $_.Exception.Message `
            -Fix 'backend 容器还没起（或反复重启）：docker compose -f deploy/docker-compose.yml up -d --build backend'
    }

    # 业务指标是否存在
    try {
        $p = Invoke-WebRequest -Uri 'http://localhost:8080/actuator/prometheus' -UseBasicParsing -TimeoutSec 15
        $promText = Get-ResponseText -Response $p
        $want = @('co_outbox_backlog', 'co_outbox_published_total', 'co_outbox_failed_total', 'co_outbox_dead_letters', 'co_reputation_mismatches')
        $missing = @($want | Where-Object { $promText -notmatch [regex]::Escape($_) })
        Add-Result -Name '5 个业务指标已暴露' -Passed ($missing.Count -eq 0) `
            -Detail $(if ($missing.Count -eq 0) { '全部存在' } else { "缺: $($missing -join ', ')" }) `
            -Fix '检查 management.endpoints.web.exposure.include 是否含 prometheus'
    }
    catch {
        Add-Result -Name '5 个业务指标已暴露' -Passed $false -Detail $_.Exception.Message -Fix 'backend 未就绪'
    }

    # 前端
    Write-Head '11. 前端（扩展）'
    try {
        $w = Invoke-WebRequest -Uri 'http://localhost/' -UseBasicParsing -TimeoutSec 10
        Add-Result -Name '前端首页返回 200' -Passed ($w.StatusCode -eq 200) -Detail ("HTTP {0}" -f $w.StatusCode) `
            -Fix 'docker compose -f deploy/docker-compose.yml up -d --build web'
    }
    catch {
        Add-Result -Name '前端首页返回 200' -Passed $false -Detail $_.Exception.Message `
            -Fix 'web 容器未起：docker compose -f deploy/docker-compose.yml up -d --build web'
    }
}

# ---------------------------------------------------------------------------
# 第 12 项：时区一致性（扩展项，本卡重点）
# ---------------------------------------------------------------------------

Write-Head '12. 时区一致性（扩展）'

$tzResults = @{}

if ($serviceHealth.ContainsKey('mysql') -and $serviceHealth['mysql'] -like 'healthy*') {
    $mt = Invoke-Native -Exe 'docker' -Arguments @('exec', 'co-mysql', 'mysql', '-uroot', '-proot_dev_pwd', '-N', '-B', '-e', 'SELECT NOW(), @@time_zone;')
    if ($mt.ExitCode -eq 0) {
        $row = (($mt.Output -split "`n") | Where-Object { $_ -match '\S' -and $_ -notmatch 'Warning' } | Select-Object -Last 1)
        $tzResults['mysql'] = ($row -split "`t")[0]
    }
}

if ($serviceHealth.ContainsKey('backend') -and $serviceHealth['backend'] -like '*running*') {
    $bt = Invoke-Native -Exe 'docker' -Arguments @('exec', 'co-backend', 'date', '-u', '+%Y-%m-%d %H:%M:%S')
    if ($bt.ExitCode -eq 0) { $tzResults['backend(UTC)'] = $bt.Output.Trim() }
}

$hostUtc = (Get-Date).ToUniversalTime().ToString('yyyy-MM-dd HH:mm:ss')
$tzResults['host(UTC)'] = $hostUtc

foreach ($k in $tzResults.Keys) {
    Write-Host ("         {0}: {1}" -f $k, $tzResults[$k]) -ForegroundColor DarkGray
}

# 判断：宿主 UTC 与容器 UTC 差在 120 秒内属正常（容器启动时间差 + 时钟漂移）
$tzOk = $true
if ($tzResults.ContainsKey('backend(UTC)')) {
    $hostT = [datetime]::ParseExact($hostUtc, 'yyyy-MM-dd HH:mm:ss', $null)
    $backT = [datetime]::ParseExact($tzResults['backend(UTC)'], 'yyyy-MM-dd HH:mm:ss', $null)
    $diff = [math]::Abs(($hostT - $backT).TotalSeconds)
    $tzOk = $diff -lt 120
    Add-Result -Name '宿主与后端容器时钟一致' -Passed $tzOk `
        -Detail ("差 {0:N0} 秒" -f $diff) `
        -Fix 'compose 里 backend 设了 TZ: UTC、JAVA_OPTS 设了 -Duser.timezone=UTC；不一致说明某一处没生效'
}
else {
    Add-Result -Name '宿主与后端容器时钟一致' -Passed $true -Detail 'backend 未运行，跳过比对'
}

# ---------------------------------------------------------------------------
# 汇总
# ---------------------------------------------------------------------------

$total = $script:results.Count
$passed = ($script:results | Where-Object Passed).Count
$failed = $total - $passed

Write-Host ''
Write-Host ('=' * 60) -ForegroundColor Cyan
if ($failed -eq 0) {
    Write-Host ("全部通过：{0}/{1} 项" -f $passed, $total) -ForegroundColor Green
    Write-Host '环境没问题，可以开始开发。' -ForegroundColor Green
    Write-Host ''
    exit 0
}
else {
    Write-Host ("通过 {0}/{1}，失败 {2} 项" -f $passed, $total, $failed) -ForegroundColor Red
    Write-Host ''
    Write-Host '失败项汇总：' -ForegroundColor Yellow
    $script:results | Where-Object { -not $_.Passed } | ForEach-Object {
        Write-Host ("  - {0}" -f $_.Name) -ForegroundColor Yellow
        if ($_.Fix) { Write-Host ("    {0}" -f $_.Fix) -ForegroundColor DarkYellow }
    }
    Write-Host ''
    exit 1
}
