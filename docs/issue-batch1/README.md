# 批次 1 任务卡（10 张）

> 对应 V3 Sprint 1 的领域层子集 + 3 张支持性任务。
> 卡片正文在 `*.md`，元数据在 `issues.json`，用 `tools/create-issues.ps1` 批量创建。

## 一、先建标签（只做一次）

```powershell
gh label create "type:story"   --color "0E8A16" --description "用户故事/任务卡" --force
gh label create "type:task"    --color "1D76DB" --description "支持性任务" --force
gh label create "type:bug"     --color "D73A4A" --description "缺陷" --force
gh label create "ctx:qa"       --color "5319E7" --description "Q&A 上下文" --force
gh label create "ctx:reputation" --color "5319E7" --description "声誉上下文" --force
gh label create "ctx:discovery"  --color "5319E7" --description "发现上下文" --force
gh label create "ctx:frontend"   --color "5319E7" --description "前端" --force
gh label create "ctx:infra"      --color "5319E7" --description "CI/部署/仓库配置" --force
gh label create "ctx:docs"       --color "5319E7" --description "文档" --force
gh label create "layer:domain"      --color "FBCA04" --description "领域层" --force
gh label create "layer:application" --color "FBCA04" --description "应用层" --force
gh label create "status:blocked" --color "B60205" --description "被阻塞" --force
```

## 二、创建 Issue

```powershell
powershell -ExecutionPolicy Bypass -File tools/create-issues.ps1
```

> 如果你的机器上有 `pwsh`（PowerShell 7），也可以用 `pwsh -File tools/create-issues.ps1`。
> 脚本本身是纯英文的，所以 Windows PowerShell 5.1 直接跑不会乱码；中文标题与正文从 UTF-8 的 JSON / md 文件读取。

预期输出：

```
准备创建 10 个 Issue...
创建：[T1-01] 值对象：MarkdownBody 与 Tags
  OK  https://github.com/.../issues/12
...
完成：成功 10，失败 0
```

## 三、加进看板与里程碑

1. **里程碑**：Issues 页 → 勾选这 10 个 → 右侧 `Milestone` → `Sprint 1`
   （如果还没有里程碑：Issues → Milestones → New milestone → 名字 `Sprint 1`）
2. **看板**：Projects → 打开看板 → `Add items` → 搜索并添加这 10 个 Issue → 拖到 `Ready` 列

## 四、分配一览

| # | 卡片 | 负责人 | 预估 |
| --- | --- | --- | --- |
| 01 | T1-01 值对象 | Bugmageddon | 2h |
| 02 | T1-02 Question 聚合 | qwerbnmv | 4h |
| 03 | T1-03 Question.accept | qwerbnmv | 3h |
| 04 | T1-04 Answer 聚合 | kl1h | 4h |
| 05 | T1-05 Vote 与 Comment | kl1h | 4h |
| 06 | T1-06 边界测试 | Bugmageddon | 4h |
| 07 | T0-01 看板与里程碑 | Bugmageddon | 1h |
| 08 | W-01 http 冒烟脚本 | Tourbillion-ANG | 2h |
| 09 | W-02 统一语言词汇表 | Tourbillion-ANG | 2h |
| 10 | W-03 错误码表 + README | Tourbillion-ANG | 4h |

**门禁**：01–06 全部合并后，`mvn test -pl co-qa/co-qa-domain` 应全绿，此时解锁批次 2。
