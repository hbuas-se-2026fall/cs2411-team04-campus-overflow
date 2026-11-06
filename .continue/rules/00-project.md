---
name: CampusOverflow 项目规则
description: 开工前读进度、完工后更新本人 progress
alwaysApply: true
---

# 开工前

1. 读 `docs/status.md`（批次与门禁）。
2. 读 `docs/progress/<当前用户 GitHub 用户名>.md`。
3. 新建分支，不直接改 `main` / `develop`。

# 完工后

1. 更新**本人**的 `docs/progress/<用户名>.md`。
2. 不改别人的 progress，不改 `docs/status.md`。
3. PR 描述写 `Closes #<Issue号>`。

# 红线

不改已有测试断言、不删测试、不加 `@Disabled`；领域层不依赖 Spring / JPA / Jackson。

完整约定见 `CLAUDE.md`。

## 开工第一件事（重要）

把看板上对应的卡片从 `Ready` 拖到 `进行中`。
AI 无法直接操作看板，**必须提醒用户手动拖**；不拖等于没人知道你开始了，可能被重复认领。