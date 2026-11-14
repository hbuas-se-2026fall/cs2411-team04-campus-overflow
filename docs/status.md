# 项目进度总览

> **维护人：只有 PO 修改这一页**，其他人只读。
> 最后更新：2026-11-14
> 各成员的细节在 `docs/progress/<GitHub用户名>.md`，本页只做汇总。如果用户还没告诉你他的 github 用户名，让他告诉你（强制他保证正确）。
> 规则见 `docs/progress/README.md`（你作为 AI 必须认真读）。

## 当前阶段

**第 2 批 · Sprint 2 · 🟡 收尾中（9 张卡已完成 7 张）**

- 批次 1 已关闭（2026-11-09），`co-qa-domain` **53/53 全绿**
- 批次 2 的 **9 张任务卡（#24–#32）已完成 7 张**，`#31`（PR #41）待合并、`#32` 未开工
- 批次 2 门禁达成：`co-qa-application` **35/35 全绿**，且已纳入 CI 的 `SPRINT_MODULES`
- 剩余待实现方法：**48 个**（Reputation 34 / Discovery 14 / Q&A **0**）

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
| 2 | Q&A 领域测试全绿 | 🟡 收尾中（2026-11-14）：7/9 张卡已合并，剩 #31（PR #41）、#32 |
| 3 | 应用层测试绿 | 🟡 门禁已达成（`co-qa-application` 35/35 + CI 已纳入），待排卡 |
| 4 | Q&A 闭环可演示 | ⬜ 未开始 |
| 5 | 端到端可跑 | ⬜ 未开始 |

## 成员进度

| 成员 | 当前任务 | 状态 | 详见 |
| --- | --- | --- | --- |
| PO（Bugmageddon） | `#30` CI 纳入 `co-qa-application` | ✅ 已完成（批次 2 收尾） | `docs/progress/bugmageddon.md` |
| 代一鸣（qwerbnmv） | `#24` / `#25` / `#26` | ✅ 三张全部合并 | `docs/progress/qwerbnmv.md` |
| 谢守航（kl1h） | `#27` / `#28` / `#29` | ✅ 三张全部合并 | `docs/progress/kl1h.md` |
| 王子昂（Tourbillion-ANG） | `#31` 已提 PR #41，`#32` 未开工 | 🟡 PR #41 待合并 | `docs/progress/tourbillion-ang.md` |

> **每人同时只做 1 张**，做完自己从 `Ready` 拉下一张。开工前必须把卡片拖到 `进行中`。

### Sprint 2 的 9 张卡

| Issue | 标题 | 负责人 | 状态 |
| --- | --- | --- | --- |
| #24 | [T2-00] 三个下游监听器改空实现（23 个方法） | qwerbnmv | ✅ PR #40 |
| #25 | [T2-01] QuestionCommandService：4 个方法 + 新增测试 | qwerbnmv | ✅ PR #36 |
| #26 | [T2-02] VoteService：投票 / 改投 / 撤销 | qwerbnmv | ✅ PR #37 |
| #27 | [T2-03] AnswerCommandService：7 个方法 | kl1h | ✅ PR #33 |
| #28 | [T2-04] QuestionQueryService：3 个查询方法 + 新增测试 | kl1h | ✅ PR #38 |
| #29 | [T2-05] CommentService + QaQueryApiImpl：2 个方法 + 新增测试 | kl1h | ✅ PR #39 |
| #30 | [T2-06] CI 纳入 co-qa-application 门禁 + 更新进度 | Bugmageddon | ✅ 本卡 |
| #31 | [T2-07] 补写 4 篇缺失 ADR（001/003/005/006） | Tourbillion-ANG | 🟡 PR #41 待合并 |
| #32 | [T2-08] 错误码中文对照表（45 条） | Tourbillion-ANG | ⬜ 未开工 |

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

### 批次 2（2026-11-14 主体完成）

- **Q&A 应用层 18 个方法全部实现**：`QuestionCommandService`(4) / `VoteService`(2) / `AnswerCommandService`(7) / `QuestionQueryService`(3) / `CommentService`(1) / `QaQueryApiImpl`(1)
- **`co-qa-application` 测试从 4 个涨到 35 个，全绿**：`AnswerAndVoteServiceTest`(5) / `CommentServiceTest`(5) / `QuestionCommandServiceTest`(8) / `VoteServiceTest`(5) / `QaQueryApiImplTest`(3) / `QuestionQueryServiceTest`(9)
- **三个下游监听器改空实现**（`ReputationEventHandlers` / `NotificationProjector` / `SearchIndexProjector`，23 处），解除事件链阻塞
- **CI 门禁扩大到 `co-qa-application`**
- **4 篇缺失 ADR 补齐**（001 模块化单体 / 003 服务端会话 / 005 中文全文检索 / 006 软删除），`docs/adr/` 9 条索引不再有死链
- **统一错误码**：`AnswerCommandService` 的 `QA-1010`/`QA-1011` 撞号改为 `QA-4001`/`QA-4002`（PR #43）

### 仓库与流程

- 看板 3 个视图：`Sprint 1`（已归档）、`Sprint 2`、`全部任务`
- 看板自动化已启用：新建 Issue 自动进项目 → `Ready`；Issue 关闭 → `完成`；PR 关联 → `待评审`
- 团队约定（DoR / DoD / 评审归属）已就位：`docs/team-agreement.md`

## 进行中

- 批次 3 待排卡。已满足解锁条件（应用层测试绿 + CI 已纳入 `co-qa-application`）

## 卡住的

（暂无）

## 下一步

1. **评审并合并 PR #41**（`#31`，王子昂）——纯文档，零风险，合并后 `docs/adr/` 9 条索引不再有死链
2. **王子昂开工 `#32`**（错误码中文对照表，45 条）——他手上最后一张，纯文档、不依赖任何人
3. **PO 关闭 `Sprint 2` 里程碑**，建批次 3 的任务卡
4. **代一鸣与谢守航进入批次 3**：`AGENTS.md` 第 9 节定义的批次 3 是「查询服务、前端接线、冒烟脚本」，批次 4 才是「声誉 / 徽章 / 悬赏 / 排行榜」（Reputation 34 个 TODO）

> ⚠️ **开工前须知**：`AnswerCommandService` 的错误码撞号已在 PR #43 修复；`#32` 的错误码表应以此为准，不要再引用 `QA-1010`/`QA-1011` 表示「未找到」。
