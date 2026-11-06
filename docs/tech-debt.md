# 技术债与待办

> 记录"知道要做、但现在还不该做"的事，避免忘掉或提前做导致阻塞。
> 每完成一项，把状态改成 ✅ 并注明日期。
> 维护人：PO

## A. 有明确时机的

| # | 事项 | 为什么现在不能做 | 什么时候做 | 状态 |
| --- | --- | --- | --- | --- |
| A1 | **把 `co-qa/co-qa-domain` 加进 CI 的 `SPRINT_MODULES`** | 该模块还有大量未实现的测试（`AnswerTest`、`VoteAndCommentTest` 等），一加 CI 立刻全红 | 批次 1 的 `#5`~`#8` 全部合并、`mvn test -pl co-qa/co-qa-domain` 全绿之后 | 待办 |
| A2 | 把 CI 覆盖扩到 `co-reputation-*`、`co-discovery-*` | 同上，依赖 S2/S3 实现进度 | 批次 4 / 批次 5 | 待办 |
| A3 | 开启「必须通过状态检查」的分支保护 | 现在后端只跑 S0 范围，等 A1 完成后再开才不会被误拦 | A1 之后 | 待办 |
| A4 | main 的分支保护从 1 个 Approve 改成 2 个 | 现在 4 个人，2 个会卡住发布 | S4 发布阶段前 | 待办 |
| A5 | 安装 Docker（WSL2 也未装） | 本机 MySQL/Redis 够用 | S2 集成测试前（Testcontainers 需要）| 待办 |
| A6 | 删除 `legacy` 与 `docs/team-charter` 旧分支 | 保留着可以回溯旧代码，无成本 | 项目结束时清理 | 待办 |

## B. 待验证的未知项

| # | 事项 | 怎么验证 | 风险 |
| --- | --- | --- | --- |
| B1 | Flyway 是否兼容本机 MySQL 8.4.8 | 跑一次 `mvn spring-boot:run -pl co-bootstrap -Dspring-boot.run.profiles=dev` | 不兼容则后端起不来，必须换 Docker 的 MySQL 8.0.39 |
| B2 | Node 24 与 Vite 5 是否兼容 | `cd frontend && npm run build` | 构建失败会导致 CI 前端任务红 |
| B3 | 组织 Actions 分钟余额 | 教师查 Billing and plans | 余量不足会导致 CI 停跑 |

## C. 文档补齐

| # | 事项 | 状态 |
| --- | --- | --- |
| C1 | 补写 ADR 001 / 003 / 005 / 006（索引里有链接但文件缺失，点了 404） | 待办 |
| C2 | 7 个 AI 入口文件里的"完整约定见 CLAUDE.md"统一改为指向 `AGENTS.md` | 待办 |
| C3 | `docs/notes/architecture.md`（架构理解笔记，S0 遗留） | 待办 |
| C4 | `docs/demo.md`（演示账号与演示脚本，答辩用） | 待办（S4） |

## D. 已决策不做

| 事项 | 原因 | 记录于 |
| --- | --- | --- |
| CODEOWNERS | 批准集中由 PO 做，4 人团队自动指派收益低 | `docs/sprint0-收尾清单.md` E 节 |
| 事件风暴工作坊 / `docs/ddd/` 全套 | 30 天工期下收益低 | 同上 |
| 前端课程页 | 不影响问答闭环演示 | 同上 |

## E. 已关闭

| # | 事项 | 关闭日期 | 说明 |
| --- | --- | --- | --- |
| — | — | — | — |
