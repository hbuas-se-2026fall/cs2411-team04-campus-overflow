# 技术债与待办

> 记录"知道要做、但现在还不该做"的事，避免忘掉或提前做导致阻塞。
> 每完成一项，把状态改成 ✅ 并注明日期。
> 维护人：PO

## A. 有明确时机的

| # | 事项 | 为什么现在不能做 | 什么时候做 | 状态 |
| --- | --- | --- | --- | --- |
| A1 | **把 `co-qa/co-qa-domain` 加进 CI 的 `SPRINT_MODULES`** | 该模块还有大量未实现的测试（`AnswerTest`、`VoteAndCommentTest` 等），一加 CI 立刻全红 | 批次 1 的 `#5`~`#8` 全部合并、`mvn test -pl co-qa/co-qa-domain` 全绿之后 | ✅ 2026-11-09（PR #21） |
| A2 | 把 CI 覆盖扩到 `co-reputation-*`、`co-discovery-*` | 同上，依赖 S2/S3 实现进度 | 批次 4 / 批次 5 | 🟡 Reputation 部分 ✅ 2026-11-18（#49）；`co-discovery-*` 待办——`#60` 合并后 `co-discovery-domain` 已 63 个测试全绿，可随时纳入 `SPRINT_MODULES`（`co-discovery-application` 见 A9） |
| A3 | 开启「必须通过状态检查」的分支保护 | 现在后端只跑 S0 范围，等 A1 完成后再开才不会被误拦 | A1 之后 | 待办 |
| A4 | main 的分支保护从 1 个 Approve 改成 2 个 | 现在 4 个人，2 个会卡住发布 | S4 发布阶段前 | 待办 |
| A5 | 安装 Docker（WSL2 也未装） | 本机 MySQL/Redis 够用；且 `CampusOverflowFlowIT` 依赖的声誉与搜索功能刚做完，现在装了也跑不绿 | **S4 集成测试前**（Testcontainers 需要，`mvn verify` + FlowIT） | ✅ 2026-11-24（`#76`）：Docker Desktop 4.100.0 + WSL2 3.0.1.0 已装，磁盘镜像迁到 `D:\DockerData`；基线数据见 `docs/reports/docker-baseline.md` |
| A6 | 删除 `legacy` 与 `docs/team-charter` 旧分支 | 保留着可以回溯旧代码，无成本 | 项目结束时清理 | 待办 |
| A7 | **提交 `frontend/package-lock.json`**（当前无锁文件，`package.json` 用 `^` 范围，每次 CI 都装当时最新版） | 生成锁文件要本地跑 `npm install`；现在不改也能跑 | Sprint 4 收尾 | 待办（见 B4） |
| A8 | **已软删除的问题/回答仍可被投票**（**已修**）：`VoteService.resolve` 原本只查存在性，不检查 `isDeleted()`；`QuestionRepositoryAdapter.findById` 也不过滤 `deleted_at` | 任何文档都没定义这条规则，`#26` 卡面也没要求，属既有缺口 | `#46` 已按方案 A 修完：`VoteService.resolve` 把 `isDeleted()` 带进 `Target.deleted`，`cast` 在 `value != 0` 时抛 `BusinessRuleException("QA-1021", "内容已删除，不能再投票")`。**撤销（`value == 0`）放行**——撤销走 `applyScore(-oldValue)` 的减分路径，拦住它会让用户删掉问题后那张票永远撤不回来，`qa_vote` 与 `ReputationAccount` 从此不一致。**没有**在 `on(VoteCastEvent)` 里过滤：那样投票照写进 `qa_vote` 表，只是声誉不变，脏数据留在库里 | 已完成 |
| A9 | **把 `co-discovery-domain` 与 `co-identity-application` 加进 CI 的 `SPRINT_MODULES`** | `#60` 实现完 Discovery 领域层后该模块 63 个测试全绿、`#62` 给 `co-identity-application` 新建了 9 个测试，但不加进 `SPRINT_MODULES` 就**只有本地会跑**，CI 上没人拦得住回归 | `#60`/`#62` 都已合并，随时可做，改动只有 `.github/workflows/ci.yml:27` 一行 | 待办（开卡做） |

## B. 待验证的未知项

| # | 事项 | 怎么验证 | 风险 |
| --- | --- | --- | --- |
| B1 | Flyway 是否兼容本机 MySQL 8.4.8 | 跑一次 `mvn spring-boot:run -pl co-bootstrap -Dspring-boot.run.profiles=dev` | 不兼容则后端起不来，必须换 Docker 的 MySQL 8.0.39。**✅ 2026-11-24 已验证（`#76`）**：容器 MySQL **8.0.39** 上 5 个迁移脚本全部 `Successfully applied`，耗时 2.629 秒，建出 20 张表，`Successfully validated 5 migrations`。原担心的 8.4.8 本地版本未继续验证——已统一改用 compose 的 8.0.39，详见 `docs/reports/docker-baseline.md` |
| B2 | Node 24 与 Vite 5 是否兼容 | `cd frontend && npm run build` | 构建失败会导致 CI 前端任务红 |
| B3 | 组织 Actions 分钟余额 | 教师查 Billing and plans | 余量不足会导致 CI 停跑 |
| B4 | **CI 前端构建会因 npm 上游 404 偶发失败** | 2026-11-09 22:21 实测：`GET https://registry.npmjs.org/rollup/-/rollup-4.68.2.tgz` 返回 404，`npm install` 直接失败；同一分支上一次运行（13:59）却通过。根因是**没有 `package-lock.json`**，`"vite": "^5.4.8"` 每次解析出当时最新的 5.x，连带其依赖的最新 rollup，撞上上游 CDN 临时缺文件的概率被放大。查过 npm 元数据接口，`rollup@4.68.2` 的 `dist.tarball` 就是这个地址，说明版本已发布、只是当时下载不到 | 偶发：不影响 PR 内容，重跑即可（`gh run rerun <id> --failed`，或 Actions 页面 `Re-run failed jobs`）。根治见 A7 |

## C. 文档补齐

| # | 事项 | 状态 |
| --- | --- | --- |
| C1 | 补写 ADR 001 / 003 / 005 / 006（索引里有链接但文件缺失，点了 404） | 待办 |
| C2 | ~~7 个 AI 入口文件里的"完整约定见 CLAUDE.md"统一改为指向 `AGENTS.md`~~ —— 已随 10 个冗余 AI 配置文件一并删除而失效 | ✅ 2026-11-10 关闭 |
| C3 | `docs/notes/architecture.md`（架构理解笔记，S0 遗留） | 待办 |
| C4 | `docs/demo.md`（演示账号与演示脚本，答辩用） | 待办（S4） |
| C5 | `docs/错误码对照表.md`（`QA-*`/`ID-*`/`DIS-*`/`AUTH-*`/`SYS-*`/`REP-*` 全量对照，供前端做中文提示） | ✅ 2026-11-18（60 个业务码 / 86 处，见 `docs/错误码对照表.md`；原 #13 已关闭） |
| C6 | `AGENTS.md` / `CLAUDE.md` / `README.md` 曾引用不存在的 `docs/错误码.md`，已于 2026-11-09 改为「以代码中的异常消息为准」 | ✅ 2026-11-09 |
| C7 | `docs/开发者须知.md`（环境版本、连接参数、测试基线、如何拿教师账号、常见报错）——对应 PB-02 的 Sprint 0 欠账。README 只留一行指针 | ✅ 2026-11-09 |

## D. 已决策不做

| 事项 | 原因 | 记录于 |
| --- | --- | --- |
| 事件风暴工作坊 / `docs/ddd/` 全套 | 30 天工期下收益低 | 同上 |
| 前端课程页 | 不影响问答闭环演示 | 同上 |

## E. 已关闭

| # | 事项 | 关闭日期 | 说明 |
| --- | --- | --- | --- |
| E1 | ~~CODEOWNERS~~（原 D 节「已决策不做」） | 2026-11-21 | **推翻，改为做**。原理由「4 人团队自动指派收益低」在 Sprint 0 成立；但 `#78` 发现容器部署链路自起始代码起从未跑通、共埋 5 个问题（见 `#80`），且 `main` 零提交导致镜像 job 永远 `skipped` —— 这类「沉默失败」的共同点就是「改了没人看见」。CODEOWNERS 覆盖 `/deploy/`、`/.github/`、`db/migration/`、`backend/pom.xml` 四类路径。**注意**：`protect-develop` ruleset 的 `require_code_owner_review` 为 `false`，所以它只发评审通知，不阻止合并、不增加批准人数、不影响 CI 耗时 |
