# Batch-create issues for Batch 1
#
# Usage (run in project root):
#   powershell -ExecutionPolicy Bypass -File tools/create-issues.ps1
#   powershell -ExecutionPolicy Bypass -File tools/create-issues.ps1 -StartAt 02-t1-02.md
#
# -StartAt <file>: skip cards until this file name is reached (use after a partial run).
#
# Note: this script is ASCII-only on purpose, so Windows PowerShell 5.1 does not
#       garble it. Titles/bodies come from UTF-8 files read with -Encoding UTF8.

param(
    [string]$StartAt = ""
)

# Do NOT use "Stop" here: gh writes warnings to stderr, and with Stop
# PowerShell 5.1 turns them into terminating errors mid-loop.
$ErrorActionPreference = "Continue"

$repo = "hbuas-se-2026fall/cs2411-team04-campus-overflow"
$root = Split-Path -Parent $PSScriptRoot
$jsonPath = Join-Path $root "docs/issue-batch1/issues.json"

if (-not (Test-Path $jsonPath)) {
    Write-Host "ERROR: cannot find $jsonPath" -ForegroundColor Red
    exit 1
}

$cards = Get-Content $jsonPath -Raw -Encoding UTF8 | ConvertFrom-Json
Write-Host ("Found {0} cards." -f $cards.Count) -ForegroundColor Cyan

$skipping = ($StartAt -ne "")
if ($skipping) {
    Write-Host ("StartAt = {0} (earlier cards will be skipped)" -f $StartAt) -ForegroundColor Yellow
}
Write-Host ""

$created = 0
$failed = 0
$skipped = 0

foreach ($card in $cards) {
    if ($skipping) {
        if ($card.file -eq $StartAt) {
            $skipping = $false
        }
        else {
            $skipped++
            continue
        }
    }

    $bodyFile = Join-Path $root ("docs/issue-batch1/" + $card.file)
    if (-not (Test-Path $bodyFile)) {
        Write-Host ("SKIP (body missing): " + $card.file) -ForegroundColor Yellow
        $failed++
        continue
    }

    $labelArg = ($card.labels -join ",")

    # Redirect stderr to a temp file so PS 5.1 never treats it as an error record.
    $tmp = [System.IO.Path]::GetTempFileName()
    gh issue create --repo $repo --title $card.title --body-file $bodyFile --label $labelArg --assignee $card.assignee 1> $tmp 2> $tmp
    $code = $LASTEXITCODE
    $out = (Get-Content $tmp -Raw -Encoding UTF8)
    Remove-Item $tmp -Force -ErrorAction SilentlyContinue

    if ($code -eq 0) {
        $created++
        Write-Host ("OK   " + $card.file + " -> " + $out) -ForegroundColor Green
    }
    else {
        $failed++
        Write-Host ("FAIL " + $card.file) -ForegroundColor Red
        Write-Host ("     " + $out) -ForegroundColor Red
    }
}

Write-Host ""
Write-Host ("Done. created={0} failed={1} skipped={2}" -f $created, $failed, $skipped) -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Issues page: select the new issues, set Milestone = Sprint 1"
Write-Host "  2. Projects board: Add items, move them to the Ready column"
