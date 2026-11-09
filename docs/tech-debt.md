# 技术债与待办

> 记录"知道要做、但现在还不该做"的事，避免忘掉或提前做导致阻塞。
> 每完成一项，把状态改成 ✅ 并注明日期。
> 维护人：PO

## A. 有明确时机的

| # | 事项 | 为什么现在不能做 | 什么时候做 | 状态 |
| --- | --- | --- | --- | --- |
| A1 | **把 `co-qa/co-qa-domain` 加进 CI 的 `SPRINT_MODULES`** | 该模块还有大量未实现的测试（`AnswerTest`、`VoteAndCommentTest` 等），一加 CI 立刻全红 | 批次 1 的 `#5`~`#8` 全部合并、`mvn test -pl co-qa/co-qa-domain` 全绿之后 | ✅ 2026-11-09（PR #21） |
| A2 | 把 CI 覆盖扩到 `co-reputation-*`、`co-discovery-*` | 同上，依赖 S2/S3 实现进度 | 批次 4 / 批次 5 | 待办 |
| A3 | 开启「必须通过状态检查」的分支保护 | 现在后端只跑 S0 范围，等 A1 完成后再开才不会被误拦 | A1 之后 | 待办 |
| A4 | main 的分支保护从 1 个 Approve 改成 2 个 | 现在 4 个人，2 个会卡住发布 | S4 发布阶段前 | 待办 |
| A5 | 安装 Docker（WSL2 也未装） | 本机 MySQL/Redis 够用 | S2 集成测试前（Testcontainers 需要）| 待办 |
| A6 | 删除 `legacy` 与 `docs/team-charter` 旧分支 | 保留着可以回溯旧代码，无成本 | 项目结束时清理 | 待办 |
| A7 | **提交 `frontend/package-lock.json`**（当前无锁文件，`package.json` 用 `^` 范围，每次 CI 都装当时最新版） | 生成锁文件要本地跑 `npm install`；现在不改也能跑 | Sprint 4 收尾 | 待办（见 B4） |

## B. 待验证的未知项

| # | 事项 | 怎么验证 | 风险 |
| --- | --- | --- | --- |
| B1 | Flyway 是否兼容本机 MySQL 8.4.8 | 跑一次 `mvn spring-boot:run -pl co-bootstrap -Dspring-boot.run.profiles=dev` | 不兼容则后端起不来，必须换 Docker 的 MySQL 8.0.39 |
| B2 | Node 24 与 Vite 5 是否兼容 | `cd frontend && npm run build` | 构建失败会导致 CI 前端任务红 |
| B3 | 组织 Actions 分钟余额 | 教师查 Billing and plans | 余量不足会导致 CI 停跑 |
| B4 | **CI 前端构建会因 npm 上游 404 偶发失败** | 2026-11-09 22:21 实测：`GET https://registry.npmjs.org/rollup/-/rollup-4.68.2.tgz` 返回 404，`npm install` 直接失败；同一分支上一次运行（13:59）却通过。根因是**没有 `package-lock.json`**，`"vite": "^5.4.8"` 每次解析出当时最新的 5.x，连带其依赖的最新 rollup，撞上上游 CDN 临时缺文件的概率被放大。查过 npm 元数据接口，`rollup@4.68.2` 的 `dist.tarball` 就是这个地址，说明版本已发布、只是当时下载不到 | 偶发：不影响 PR 内容，重跑即可（`gh run rerun <id> --failed`，或 Actions 页面 `Re-run failed jobs`）。根治见 A7 |

## C. 文档补齐

| # | 事项 | 状态 |
| --- | --- | --- |
| C1 | 补写 ADR 001 / 003 / 005 / 006（索引里有链接但文件缺失，点了 404） | 待办 |
| C2 | 7 个 AI 入口文件里的"完整约定见 CLAUDE.md"统一改为指向 `AGENTS.md` | 待办 |
| C3 | `docs/notes/architecture.md`（架构理解笔记，S0 遗留） | 待办 |
| C4 | `docs/demo.md`（演示账号与演示脚本，答辩用） | 待办（S4） |
| C5 | `docs/错误码对照表.md`（`QA-*`/`ID-*`/`DIS-*`/`AUTH-*`/`SYS-*`/`REP-*` 全量对照，供前端做中文提示） | 待办（Sprint 2 中期，与 `T2-13` 前端任务一起做——届时 `REP-*` 才存在。原 #13 已关闭） |
| C6 | `AGENTS.md` / `CLAUDE.md` / `README.md` 曾引用不存在的 `docs/错误码.md`，已于 2026-11-09 改为「以代码中的异常消息为准」 | ✅ 2026-11-09 |
| C7 | `docs/开发者须知.md`（环境版本、连接参数、测试基线、如何拿教师账号、常见报错）——对应 PB-02 的 Sprint 0 欠账。README 只留一行指针 | ✅ 2026-11-09 |

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
