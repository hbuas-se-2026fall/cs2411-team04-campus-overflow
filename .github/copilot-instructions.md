# CampusOverflow · 给 AI 助手的项目约定

> 本文件提交到仓库，GitHub Copilot 会自动读取。
> 完整版见仓库根目录 `CLAUDE.md`；本文件是它的强制摘要。

## 每次动手前，必须先做

1. 读 `docs/status.md` —— 当前批次、门禁状态。
2. 读 `docs/progress/<当前用户 GitHub 用户名>.md` —— 本人当前任务。
3. 确认分支：不得直接改 `main` / `develop`，一律新建 `feat/`、`fix/`、`docs/`、`ci/` 分支。

## 完成任务后，必须做

1. **更新本人**的 `docs/progress/<用户名>.md`（改别人文件或改 `docs/status.md` 都是错的）。
2. 提醒用户：PR 里要勾选「已更新 progress」。
3. 更新内容包含：当前任务、状态（进行中/待评审/已完成/卡住）、卡在哪、下一步。

## 红线（违反会被 PR 拒绝）

- 不修改已有测试断言、不删除测试、不加 `@Disabled`。
- 领域层（`*.domain` 包）不得依赖 Spring / JPA / Jackson。
- 跨上下文只经 `*-api` 包或集成事件，不直接 import 其他上下文的 domain/application。
- 不提交 `target/`、`node_modules/`、`.idea/`、`.env`、密钥。

## 常用命令

```bash
cd backend && mvn -B test -pl <模块> -am     # 跑单个模块测试
mvn -B test -pl co-bootstrap -Dtest=ArchitectureTest   # 架构检查
cd frontend && npm run typecheck && npm run build      # 前端
```

## 开工第一件事（重要）

把看板上对应的卡片从 `Ready` 拖到 `进行中`。
AI 无法直接操作看板，**必须提醒用户手动拖**；不拖等于没人知道你开始了，可能被重复认领。