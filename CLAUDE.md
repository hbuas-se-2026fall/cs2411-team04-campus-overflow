# CampusOverflow · 团队公开约定

> 本文件**提交到仓库**，任何人和任何 AI 助手都能读到；PO 本地的 `AGENTS.md` 是它的超集（含不公开的成员安排）。
> **修改本文件必须走 PR。**

## 1. 开工前必做（三步，缺一不可）

1. 读 **`docs/status.md`**：当前批次、门禁是否通过、谁在做什么。
2. 读 **`docs/progress/<你的 GitHub 用户名>.md`**：你自己的当前任务与卡点。
3. 同步代码：`git switch develop && git pull`。

## 2. 完工后必做

1. **更新你自己**的 `docs/progress/<用户名>.md`（只改自己的；**不要改别人的，也不要改 `docs/status.md`**，那只有 PO 能改）。
2. 在你开的 PR 里勾选「已更新我的 progress 文件」（PR 模板里有这一栏）。
3. 若你完成的是某个**门禁项**（让某一批可以解锁），在群里说一句，由 PO 更新总览。

> **这条不是靠自觉**：CI 有一项「进度文件检查」，代码类 PR 若没有更新本人的 progress 文件，会**直接失败**。
> 纯文档 PR（只改 `docs/` 或 `.md`）会自动跳过。

> 为什么这样分：4 个人改同一个进度文件必然合并冲突；一人一个文件 = 零冲突。

## 3. 硬性要求

- **动手前先说明**要做什么、为什么、影响哪些文件；得到确认后再改。
- **不直接推送** `main` / `develop`；一律「分支 → PR → 评审 → 合并」。
- **不修改已有测试断言、不删除测试、不加 `@Disabled`** —— 等同于篡改需求。
- 领域层不得依赖 Spring / JPA / Jackson（ArchUnit 会拦）。
- 跨上下文只经 `*-api` 或集成事件。
- 不提交 `target/`、`node_modules/`、`.idea/`、`.env`、真实密钥。

## 4. 分支与提交

- 分支：`feat/xxx`、`fix/xxx`、`docs/xxx`、`ci/xxx`；一个任务一个分支，生命周期 ≤3 天。
- 提交：`<type>(<scope>): 中文简述`，`type` ∈ `feat|fix|test|docs|ci|chore`。
- PR：≤400 行，关联 Issue，附测试证据。

## 5. 文档索引

| 文件 | 内容 |
| --- | --- |
| `docs/status.md` | 进度总览（**只有 PO 修改**） |
| `docs/progress/` | 每人一份进度，**本人修改** |
| `docs/团队分工与Sprint任务分配.md` | 分工、任务表、依赖 |
| `docs/adr/` | 架构决策记录 |
| `CampusOverflow-实验指导书V3.pdf` | 权威需求（README 的 15 周计划已废弃） |
