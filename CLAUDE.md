# CampusOverflow · 给 AI 助手的项目规则

> **完整约定见仓库根目录 `AGENTS.md`**（同一份规则的主文件）。
> 本文件是它的精简入口，供 Claude Code 等读取 `CLAUDE.md` 的工具使用。

## 开工前（必做）

1. 读 `docs/status.md` —— 当前批次、门禁状态。
2. 读 `docs/progress/<当前用户 GitHub 用户名>.md` —— 本人任务。
3. `git switch develop && git pull`，然后新建分支；不直接改 `main` / `develop`。

## 完工后（必做）

1. 更新**本人**的 `docs/progress/<用户名>.md`（只改自己的）。
2. PR 描述写 `Closes #<Issue号>`。
3. 勾选 PR 模板里的「已更新 progress」。

> 不是靠自觉：CI 有「进度文件检查」，代码类 PR 未更新本人 progress 会直接失败；纯文档 PR 自动跳过。
> `docs/status.md` 只有 PO 能改。

## 红线（违反会被拒绝合并）

- 不修改已有测试断言、不删除测试、不加 `@Disabled`。
- 领域层（`*.domain`）不得依赖 Spring / JPA / Jackson / Swagger。
- 跨上下文只经 `*-api` 或集成事件。
- 写操作在 `@Transactional` 内；事件在保存后、同一事务内发布；消费端必须幂等。
- 错误码严格遵循 `docs/错误码.md`（`BusinessRule`→422、`Conflict`→409、`Forbidden`→403、`NotFound`→404）。
- 不提交 `target/`、`node_modules/`、`.idea/`、`.env`、密钥。
- 提交前先本地跑对应模块的测试，不要用 CI 代替自检。

## 常用命令

```bash
mvn -B -f backend/pom.xml test -pl co-qa/co-qa-domain -am          # 单模块测试
mvn -B -f backend/pom.xml test -pl co-bootstrap -am -Dtest=ArchitectureTest  # 架构检查
cd frontend && npm run typecheck && npm run build                   # 前端
```

## 最容易踩的坑

**发件箱会被未实现的监听器堵死**：任一 `@EventListener` 抛异常，就会阻塞本批后续事件，表现为"声誉永远不变化"。
所以没进入当前批次的上下文，监听器必须先写成空实现（只打 DEBUG 日志），并建 tech-debt Issue。
