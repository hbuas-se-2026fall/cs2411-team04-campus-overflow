# PO（Bugmageddon）的进度

最后更新：2026-11-18
当前任务：`#50` 前端联调验收 + 补齐声誉页（已提 PR）
状态：待评审合并
卡在哪：无
下一步：
- 等 `#50` 的 PR 评审通过后合并，Sprint 3 的 8 张卡即全部关闭
- 给代一鸣、谢守航开 Discovery 的 4 张卡（14 个 `UnsupportedOperationException` + 15 个空监听器）
- Discovery 前置卡（7 个纯领域方法）要在 11-21 前合并

## 已完成

- **2026-11-18**：`#50` 前端联调验收 + 补齐声誉页。新增 `frontend/src/pages/ReputationPage.tsx`（`/users/:id` 路由，展示声誉、徽章、流水；`availablePoints` 为 null 时不显示积分，不渲染成 0）；`LeaderboardPage` 用户名改为链到声誉页；`QuestionDetailPage` 按 `permissions.canDelete` 增加删除按钮（`deleteQuestion` 从「定义了没人调」变为「有入口」）；`api.ts` 补 `ledger()` 与 `LedgerEntry` 类型；`http/auth-smoke.ps1` 从 85 行扩到 190 行，新增 reputation → ledger → leaderboard → bounty 四段，其中悬赏用「预期 403 REP-3001」验证权限门禁，末尾自动软删除本次创建的问题。`npm run typecheck` 与 `npm run build` 均 exit=0
- **2026-11-18**：`#49` CI 纳入 `co-reputation` 门禁——`SPRINT_MODULES` 追加 `co-reputation/co-reputation-domain` 与 `co-reputation/co-reputation-application`。此前这 42 个测试在 CI 里一次都没跑过。覆盖率试点（0.70）本卡不做：`domain.coverage.minimum` 是全局属性，改动会同时影响 `co-qa-domain` / `co-identity-domain`，而本地 `verify` 阶段 jacoco 在沙箱里会崩，无法自检
- **2026-11-15**：关闭 Sprint 2 里程碑（8/9，`#32` 挪到 Sprint 3）；建 Sprint 3 里程碑与 8 张任务卡（#45–#51 + #32 改号）
- **2026-11-14**：`#30` CI 纳入 `co-qa-application`（PR #44），含反向验证（改坏断言 → CI 变红 → 改回 → 变绿）
- **2026-11-06**：仓库初始化、分支保护、默认分支改 develop
- **2026-11-06**：进度看板体系（`docs/status.md` + `docs/progress/`）
- **2026-11-06**：CI 渐进式质量门禁（ADR-009）+ 进度文件检查
- **2026-11-06**：修复 5 个乱码文件名
- **2026-11-06**：全工具 AI 约定（AGENTS.md / CLAUDE.md / 6 个工具入口）
- **2026-11-06**：Issue 模板、团队约定、批次 1 的 10 张任务卡、项目看板
- **2026-11-07**：`#4` 实现 `MarkdownBody` 与 `Tags.names`，新增 `MarkdownBodyTest`（PR #15）
- **2026-11-09**：评审 PR #16（`#5`）、#18（`#7`/`#8`）、#19（`#6`），提出 endorse 双重判断、NPE 拆箱、checkValue 口径三处修改
- **2026-11-09**：**批次 1 门禁达成** —— `co-qa-domain` 21/21 全绿
- **2026-11-09**：CI 覆盖范围加入 `co-qa/co-qa-domain`
- **2026-11-09**：`#9` 领域边界测试完成 —— 新增 `QuestionBoundaryTest`(13) / `AnswerBoundaryTest`(11) / `VoteBoundaryTest`(8) 共 32 个用例，`co-qa-domain` 从 21 个涨到 53 个，全绿；原有 4 个测试类零改动
- **2026-11-09**：对新增测试做变异验证（3 处：标题下界 10→11、去掉 `id == null` 保护、投票校验换序），三处都被捕获，确认测试有真实约束力
- **2026-11-11**：建批次 2 的 9 张任务卡（#24–#32）并分配到人，全部挂 `Sprint 2` 里程碑
- **2026-11-12**：**修复 CI 全面停摆**。现象：11-12 起所有 job `steps=0`、3 秒内失败，注释为 `The job was not started because recent account payments have failed or your spending limit needs to be increased`。定位：不是代码/配置问题，是私有仓库 + 组织额度/账单（同组织 public 仓库 team08 的 CI 正常）。核对用量：team04 从 11-01 至今 154 个 job、累计 63.49 分钟，远未触及 3000 分钟额度；组织下 20 个私有仓库、97 个席位共享额度。处理：仓库改为 public 后 CI 恢复，`run 43141289402` attempt 3 全绿（后端 58s / 前端 20s / 进度检查 6s）
- **2026-11-12**：核实两位成员提出的问题。谢守航的 CI 诊断成立，但他有 3 处误判（① 误以为 PO 是组织 Owner——实为 member，Owner 只有 `BostonMIT`；② 误以为 3000 分钟被本队跑光——实际 63.49 分钟；③ 不必等 CI 恢复才能合并——`protect-develop` 规则集没有 `required_status_checks`）。王子昂对 #31 的 3 条核查全部成立
- **2026-11-14**：**批次 2 主体完成，Q&A 应用层 TODO 归零**。`co-qa-application` 测试从 4 个涨到 **35 个全绿**（`AnswerAndVoteServiceTest` 5 / `CommentServiceTest` 5 / `QuestionCommandServiceTest` 8 / `VoteServiceTest` 5 / `QaQueryApiImplTest` 3 / `QuestionQueryServiceTest` 9）。9 张卡已合并 7 张（#24–#30），剩 #31（PR #41 待合并）、#32（未开工）。全仓库剩余 `UnsupportedOperationException` **48 个**：Reputation 34 / Discovery 14 / Q&A 0
- **2026-11-14**：**`#30` CI 纳入 `co-qa-application`**。`.github/workflows/ci.yml` 的 `SPRINT_MODULES` 追加 `co-qa/co-qa-application`，同步更新第 23–26 行注释（批次编号与 `AGENTS.md` 第 9 节对齐）。此前后端 job 的「单元测试（Sprint 范围）」与「覆盖率门禁」两步根本不跑 application 模块的 35 个测试。已核对 `co-qa-application/pom.xml` 未声明 jacoco `check`，加入后不会因覆盖率阈值变红

