# 项目进度总览

> **维护人：只有 PO 修改这一页**，其他人只读。
> 最后更新：2026-11-18
> 各成员的细节在 `docs/progress/<GitHub用户名>.md`，本页只做汇总。如果用户还没告诉你他的 github 用户名，让他告诉你（强制他保证正确）。
> 规则见 `docs/progress/README.md`（你作为 AI 必须认真读）。

## 当前阶段

**第 3 批 · Sprint 3 · 🟡 主体完成（8 张卡已完成 4 张）**

- 批次 1 已关闭（2026-11-09），`co-qa-domain` **53/53 全绿**
- 批次 2 已关闭（2026-11-15），9 张卡全部完成
- 批次 3 的 **8 张任务卡（#32、#45–#51）已完成 4 张**（#45–#48 全部合并），`#49`/`#50`/`#51`/`#32` 待做
- **Reputation 上下文 TODO 已归零**：`co-reputation-domain` 15/15、`co-reputation-application` 27/27，共 **42 个测试全绿**
- 剩余待实现方法：**14 个，全部在 Discovery**（`SearchQuery` 2 / `RelatedQuestionPolicy` 2 / `MentionParser` 1 / `Notification` 2 / `DiscoveryQueryService` 2 / `NotificationService` 5）

### 2026-11-18 事件记录

- **Reputation 上下文 4 张卡全部合并**：#45（`ReputationAccount` + `ReputationRulebook`，PR #52）、#46（徽章 + 5 个事件处理器真正记账，PR #53）、#47（`Bounty` + `BountyService` 悬赏闭环，PR #54）、#48（声誉查询 + 排行榜 + 对账，PR #55）
- **`REP-*` 错误码对齐指导书 §3.7**：`Bounty` 6 处改号（`REP-3002`→`REP-3001`、自造的 `REP-1007/1008/1009/1010`→`REP-1003/1004/1001/1005`、`REP-2001`→`REP-2002`），`ReputationQueryService` 的 `REP-3001`→`REP-3002`；`BountyService.open` 补「该问题已有进行中的悬赏」检查（`REP-2001`）
- **`co-reputation` 纳入 CI 门禁**（本卡）：`.github/workflows/ci.yml` 的 `SPRINT_MODULES` 追加 `co-reputation/co-reputation-domain` 与 `co-reputation/co-reputation-application`。此前这 42 个测试在 CI 里**一次都没跑过**
- **A8 技术债已关闭**：`VoteService.resolve` 补 `isDeleted()` 判断并抛 `QA-1021`，放行撤销减分路径（PR #53）
- **`co-qa-application` 测试从 35 涨到 37**：新增 `cannotVoteOnDeletedQuestion` / `canRevokeVoteOnDeletedQuestion`

### 2026-11-14 事件记录

- **Q&A 应用层 TODO 归零**：`QaQueryApiImpl.findQuestion` 与 `CommentService.post` 由 PR #39 落地（谢守航），合并提交 `650dd25`，Issue #29 自动关闭
- **批次 2 七张卡已合并**：#24（监听器空实现，PR #40）、#25（`QuestionCommandService`，PR #36）、#26（`VoteService`，PR #37）、#27（`AnswerCommandService`，PR #33）、#28（`QuestionQueryService`，PR #38）、#29（`CommentService` + `QaQueryApiImpl`，PR #39）、#30（CI 门禁，本卡）；`#31` 的 PR #41 待合并、`#32` 未开工
- **`co-qa-application` 纳入 CI 门禁**：`.github/workflows/ci.yml` 的 `SPRINT_MODULES` 追加 `co-qa/co-qa-application`，此前该模块 35 个测试 CI 根本不跑
- **谢守航自行合并 PR #43**（`QA-1010`/`QA-1011` 撞号修复）：技术上不违规（已有 1 名非作者批准），但惯例应由评审人合并；他在后续 PR #39 中主动注明「本人不自行合并」

### 2026-11-12 事件记录

- **CI 停摆已修复**：11-12 起所有 job 秒挂（`steps=0`，注释 `The job was not started because recent account payments have failed...`），原因是组织额度/账单，不是代码问题。仓库从 private 改为 **public** 后 CI 恢复（`run 43141289402` attempt 3：后端 58 秒 / 前端 20 秒 / 进度检查 6 秒，全部 success）
- **#27 已合并**：PR #33（谢守航）评审通过并合并，合并提交 `cdb2b2c`，Issue #27 由 PR 自动关闭。`AnswerCommandService` 7 个方法全部落地，Q&A 应用层剩余 11 个
- **Issue 事实性错误已修正**：#24（补充 23 处 = 20 个 `@EventListener` + 3 个私有辅助）、#28（`JdbcQaReadModel` 真实路径在 `infrastructure/query/` 而非 `persistence/`）、#31（discovery SQL 真实路径、`deleted_at` 只有 2 个字段而非 5 处、`@ArchTest` 是 8 个而非 10 个；ADR-006 标题改为「问题与回答软删除，投票撤销即删除」）

## 批次与门禁

| 批次 | 解锁条件 | 状态 |
| --- | --- | --- |
| 1 | 无（现在就能开） | ✅ 已完成（2026-11-09） |
| 2 | Q&A 领域测试全绿 | ✅ 已完成（2026-11-15，9/9 张卡） |
| 3 | 应用层测试绿 | 🟡 主体完成（2026-11-18）：#45–#48 已合并，#49 CI 门禁（本卡）、#50 前端联调、#51 演示脚本、#32 错误码表待做 |
| 4 | Q&A 闭环可演示 | 🟡 门禁已达成（Reputation 42 个测试全绿 + CI 已纳入），待排卡 |
| 5 | 端到端可跑 | ⬜ 未开始（Discovery 14 个 TODO 全在这里） |

## 成员进度

| 成员 | 当前任务 | 状态 | 详见 |
| --- | --- | --- | --- |
| PO（Bugmageddon） | `#49` CI 纳入 `co-reputation` | 🟡 已提 PR | `docs/progress/bugmageddon.md` |
| 代一鸣（qwerbnmv） | `#45` / `#46` | ✅ 两张全部合并（PR #52 / #53） | `docs/progress/qwerbnmv.md` |
| 谢守航（kl1h） | `#47` / `#48` | ✅ 两张全部合并（PR #54 / #55） | `docs/progress/kl1h.md` |
| 王子昂（Tourbillion-ANG） | `#32` 错误码对照表 / `#51` 演示脚本 + ADR-010 | ⬜ 未开工 | `docs/progress/tourbillion-ang.md` |

> **每人同时只做 1 张**，做完自己从 `Ready` 拉下一张。开工前必须把卡片拖到 `进行中`。

### Sprint 3 的 8 张卡

| Issue | 标题 | 负责人 | 状态 |
| --- | --- | --- | --- |
| #45 | [T3-01] `ReputationAccount` 7 个方法 + `ReputationRulebook` 4 个方法 | qwerbnmv | ✅ PR #52 |
| #46 | [T3-02] 徽章与学期键 5 个方法 + 5 个事件处理器真正记账 | qwerbnmv | ✅ PR #53 |
| #47 | [T3-03] `Bounty` 5 个方法 + `BountyService` 5 个方法（悬赏闭环） | kl1h | ✅ PR #54 |
| #48 | [T3-04] 声誉查询 4 个 + 排行榜 2 个 + 每日对账 1 个 | kl1h | ✅ PR #55 |
| #49 | [T3-05] CI 纳入 `co-reputation` 门禁 + 领域覆盖率试点 | Bugmageddon | 🟡 本卡 |
| #50 | [T3-06] 前端联调验收：排行榜 / 悬赏对话框 + 补齐声誉页 | Bugmageddon | ⬜ 未开工 |
| #51 | [T3-08] 声誉演示脚本 + ADR-010（悬赏积分与声誉分离） | Tourbillion-ANG | ⬜ 未开工 |
| #32 | [T3-07] 错误码中文对照表（60 条） | Tourbillion-ANG | ⬜ 未开工 |

### Sprint 2 的 9 张卡（已归档）

| Issue | 标题 | 负责人 | 状态 |
| --- | --- | --- | --- |
| #24 | [T2-00] 三个下游监听器改空实现（23 个方法） | qwerbnmv | ✅ PR #40 |
| #25 | [T2-01] QuestionCommandService：4 个方法 + 新增测试 | qwerbnmv | ✅ PR #36 |
| #26 | [T2-02] VoteService：投票 / 改投 / 撤销 | qwerbnmv | ✅ PR #37 |
| #27 | [T2-03] AnswerCommandService：7 个方法 | kl1h | ✅ PR #33 |
| #28 | [T2-04] QuestionQueryService：3 个查询方法 + 新增测试 | kl1h | ✅ PR #38 |
| #29 | [T2-05] CommentService + QaQueryApiImpl：2 个方法 + 新增测试 | kl1h | ✅ PR #39 |
| #30 | [T2-06] CI 纳入 co-qa-application 门禁 + 更新进度 | Bugmageddon | ✅ PR #44 |
| #31 | [T2-07] 补写 4 篇缺失 ADR（001/003/005/006） | Tourbillion-ANG | ✅ PR #41 |
| #32 | [T2-08] 错误码中文对照表 → 已挪到 Sprint 3 | Tourbillion-ANG | ➡️ #T3-07 |

## 已完成

### 批次 1（2026-11-09 关闭）

- 仓库初始化，`main` / `develop` 就位，历史清理完毕
- 默认分支改为 `develop`；`main` / `develop` 分支保护已开（禁直推、禁强推）
- 进度看板建立（本页 + 各成员页）
- CI 改为渐进式质量门禁（ADR-009）
- 项目看板：https://github.com/orgs/hbuas-se-2026fall/projects/23
- 批次 1 的 10 张任务卡已建（#4–#13）
- **Q&A 领域层 6 个类全部实现**，`co-qa-domain` **53/53** 测试全绿（#4–#8 已合并）
- **CI 覆盖范围加入 `co-qa/co-qa-domain`**：领域层改坏会立刻红灯
- 统一语言词汇表（23 个术语，#22）
- 领域层边界测试 7 类边界钉死（#23）

### 批次 2（2026-11-15 关闭）

- **Q&A 应用层 18 个方法全部实现**：`QuestionCommandService`(4) / `VoteService`(2) / `AnswerCommandService`(7) / `QuestionQueryService`(3) / `CommentService`(1) / `QaQueryApiImpl`(1)
- **`co-qa-application` 测试从 4 个涨到 35 个，全绿**：`AnswerAndVoteServiceTest`(5) / `CommentServiceTest`(5) / `QuestionCommandServiceTest`(8) / `VoteServiceTest`(5) / `QaQueryApiImplTest`(3) / `QuestionQueryServiceTest`(9)
- **三个下游监听器改空实现**（`ReputationEventHandlers` / `NotificationProjector` / `SearchIndexProjector`，23 处），解除事件链阻塞
- **CI 门禁扩大到 `co-qa-application`**
- **4 篇缺失 ADR 补齐**（001 模块化单体 / 003 服务端会话 / 005 中文全文检索 / 006 软删除），`docs/adr/` 9 条索引不再有死链
- **统一错误码**：`AnswerCommandService` 的 `QA-1010`/`QA-1011` 撞号改为 `QA-4001`/`QA-4002`（PR #43）

### 批次 3（2026-11-18 主体完成）

- **Reputation 上下文 34 个 TODO 全部清零**：`ReputationAccount`(7) / `ReputationRulebook`(4) / `BadgePolicy`(2) / `AcademicTerm`(3) / `Bounty`(5) / `BadgeService`(1) / `BountyService`(5) / `LeaderboardService`(2) / `ReconciliationService`(1) / `ReputationApiImpl`(2) / `ReputationQueryService`(2) / `ReputationEventHandlers`(5 个监听器从空实现改为真正记账)
- **`co-reputation` 测试从 0 涨到 42 个，全绿**：`ReputationRulebookTest`(9) / `BountyAndAccountTest`(6) / `BountyServiceTest`(5) / `ReputationEventHandlersTest`(14) / `ReputationReadServicesTest`(8)
- **悬赏闭环打通**：发起冻结积分 → 采纳结算 → 到期退款，事件与账本唯一键共同保证幂等
- **`REP-*` 错误码 11 个全部对齐指导书 §3.7**
- **CI 门禁扩大到 `co-reputation-domain` + `co-reputation-application`**
- **A8 技术债关闭**：已删除内容拒绝新投票（`QA-1021`），放行撤销减分路径

### 仓库与流程

- 看板 3 个视图：`Sprint 1`（已归档）、`Sprint 2`（已归档）、`全部任务`。**Sprint 3 视图尚未创建**（API 不支持设置视图过滤器，需在网页 UI 手动新建并填 `milestone:"Sprint 3"`）
- 看板自动化已启用：新建 Issue 自动进项目 → `Ready`；Issue 关闭 → `完成`；PR 关联 → `待评审`
- 团队约定（DoR / DoD / 评审归属）已就位：`docs/team-agreement.md`

## 进行中

- **批次 3 主体完成**（2026-11-18）：Reputation 上下文 34 个 TODO 全部清零，42 个测试全绿并已纳入 CI
- 剩 `#49`（CI 门禁，本卡）、`#50`（前端联调验收）、`#51`（演示脚本 + ADR-010）、`#32`（错误码对照表 60 条）
- 批次 4 解锁条件已满足，待排卡（但 Discovery 14 个 TODO 属批次 5，需先决定是否砍）

## 卡住的

（暂无）

## 下一步

1. **合并 `#49` 的 PR**（CI 纳入 `co-reputation`）——合并后 Reputation 的 42 个测试才真正受门禁保护
2. **王子昂开工 `#32`**（错误码对照表，**60 条**：QA 24 / ID 23 / REP 11 / DIS 2）——他手上还有 `#51`，按 `AGENTS.local.md` 设 48 小时检查点
3. **PO 做 `#50`**（前端联调验收：排行榜 / 悬赏对话框 + 补齐声誉页）
4. **排批次 4/5 的卡**：Discovery 14 个 TODO（`SearchQuery` 2 / `RelatedQuestionPolicy` 2 / `MentionParser` 1 / `Notification` 2 / `DiscoveryQueryService` 2 / `NotificationService` 5），加 `mvn verify` + `CampusOverflowFlowIT` 全绿

> ⚠️ **开工前须知**：`AnswerCommandService` 的错误码撞号已在 PR #43 修复；`Bounty` 的 `REP-*` 已在 PR #54 对齐指导书 §3.7。`#32` 的错误码表应以此为准。
