---
applyTo: "**"
description: CampusOverflow 项目规则：开工先读进度，完工必须更新本人 progress
---

# 开工前（必做）

1. 读 `docs/status.md` —— 当前批次、门禁状态。
2. 读 `docs/progress/<当前用户 GitHub 用户名>.md` —— 本人任务。
3. 新建分支，不要直接改 `main` / `develop`。

# 完工后（必做）

1. 更新**本人**的 `docs/progress/<用户名>.md`（当前任务、状态、卡点、下一步）。
2. 不改别人的 progress 文件，不改 `docs/status.md`（只有 PO 能改）。
3. PR 描述写 `Closes #<Issue号>`。

# 红线

- 不修改已有测试断言、不删除测试、不加 `@Disabled`。
- 领域层不得依赖 Spring / JPA / Jackson。

完整约定见 `CLAUDE.md`。

## 开工第一件事（重要）

把看板上对应的卡片从 `Ready` 拖到 `进行中`。
AI 无法直接操作看板，**必须提醒用户手动拖**；不拖等于没人知道你开始了，可能被重复认领。