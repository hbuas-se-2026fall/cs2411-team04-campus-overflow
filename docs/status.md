# 项目进度总览

> **维护人：只有 PO 修改这一页**，其他人只读。
> 最后更新：2026-11-12
> 各成员的细节在 `docs/progress/<GitHub用户名>.md`，本页只做汇总。如果用户还没告诉你他的 github 用户名，让他告诉你（强制他保证正确）。
> 规则见 `docs/progress/README.md`（你作为 AI 必须认真读）。

## 当前阶段

**第 2 批 · Sprint 2 · 已开工（第 1 张卡已提 PR）**

- 批次 1 已关闭（2026-11-09），`co-qa-domain` **53/53 全绿**
- 批次 2 的 **9 张任务卡（#24–#32）已全部建好**，挂在 `Sprint 2` 里程碑
- **#27 已合并**（谢守航，PR #33，2026-11-12）——`AnswerCommandService` 7 个方法全部落地，Q&A 剩余 11 个方法
- 剩余待实现方法：**82 个**（Discovery 32 / Reputation 39 / Q&A 11）

### 2026-11-12 事件记录

- **CI 停摆已修复**：11-12 起所有 job 秒挂（`steps=0`，注释 `The job was not started because recent account payments have failed...`），原因是组织额度/账单，不是代码问题。仓库从 private 改为 **public** 后 CI 恢复（`run 43141289402` attempt 3：后端 58 秒 / 前端 20 秒 / 进度检查 6 秒，全部 success）
- **#27 已合并**：PR #33（谢守航）评审通过并合并，合并提交 `cdb2b2c`，Issue #27 由 PR 自动关闭。`AnswerCommandService` 7 个方法全部落地，Q&A 应用层剩余 11 个
- **Issue 事实性错误已修正**：#24（补充 23 处 = 20 个 `@EventListener` + 3 个私有辅助）、#28（`JdbcQaReadModel` 真实路径在 `infrastructure/query/` 而非 `persistence/`）、#31（discovery SQL 真实路径、`deleted_at` 只有 2 个字段而非 5 处、`@ArchTest` 是 8 个而非 10 个；ADR-006 标题改为「问题与回答软删除，投票撤销即删除」）

## 批次与门禁

| 批次 | 解锁条件 | 状态 |
| --- | --- | --- |
| 1 | 无（现在就能开） | ✅ 已完成（2026-11-09） |
| 2 | Q&A 领域测试全绿 | 🟡 已排期，9 张卡待认领（#24–#32） |
| 3 | 应用层测试绿 | ⬜ 未开始 |
| 4 | Q&A 闭环可演示 | ⬜ 未开始 |
| 5 | 端到端可跑 | ⬜ 未开始 |

## 成员进度

| 成员 | 当前任务 | 状态 | 详见 |
| --- | --- | --- | --- |
| PO（Bugmageddon） | `#30` CI 纳入 `co-qa-application` | ⬜ 等待（依赖 #25–#29 全合并） | `docs/progress/bugmageddon.md` |
| 代一鸣（qwerbnmv） | `#24` 监听器空实现（23 个方法） | ⬜ 待开工（队列：#24 → #25 → #26） | `docs/progress/qwerbnmv.md` |
| 谢守航（kl1h） | `#27` AnswerCommandService（7 个方法） | 🟡 已提 PR #33，待评审合并（队列：#27 → #28 → #29） | `docs/progress/kl1h.md` |
| 王子昂（Tourbillion-ANG） | `#31` 补写 4 篇 ADR（001/003/005/006） | ⬜ 待开工（队列：#31 → #32） | `docs/progress/tourbillion-ang.md` |

> **每人同时只做 1 张**，做完自己从 `Ready` 拉下一张。开工前必须把卡片拖到 `进行中`。

### Sprint 2 的 9 张卡

| Issue | 标题 | 负责人 |
| --- | --- | --- |
| #24 | [T2-00] 三个下游监听器改空实现（23 个方法） | qwerbnmv |
| #25 | [T2-01] QuestionCommandService：4 个方法 + 新增测试 | qwerbnmv |
| #26 | [T2-02] VoteService：投票 / 改投 / 撤销 | qwerbnmv |
| #27 | [T2-03] AnswerCommandService：7 个方法 | kl1h |
| #28 | [T2-04] QuestionQueryService：3 个查询方法 + 新增测试 | kl1h |
| #29 | [T2-05] CommentService + QaQueryApiImpl：2 个方法 + 新增测试 | kl1h |
| #30 | [T2-06] CI 纳入 co-qa-application 门禁 + 更新进度 | Bugmageddon |
| #31 | [T2-07] 补写 4 篇缺失 ADR（001/003/005/006） | Tourbillion-ANG |
| #32 | [T2-08] 错误码中文对照表（45 条） | Tourbillion-ANG |

**关键路径**：`#25`（`QuestionCommandService.post`）必须先合并——它是"发一个问题"的唯一入口，其余所有服务与冒烟脚本都依赖它。

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

### 仓库与流程

- 看板 3 个视图：`Sprint 1`（已归档）、`Sprint 2`、`全部任务`
- 看板自动化已启用：新建 Issue 自动进项目 → `Ready`；Issue 关闭 → `完成`；PR 关联 → `待评审`
- 团队约定（DoR / DoD / 评审归属）已就位：`docs/team-agreement.md`

## 进行中

- **批次 2：Q&A 应用层**（18 个方法：三个命令服务 + 查询侧 + 行走骨架）
  - 门禁：`co-qa-application` 的 `AnswerAndVoteServiceTest` 全绿
  - 状态：9 张卡已建，**#27 已提 PR #33（待评审）**，其余 8 张待开工

## 卡住的

（暂无）

## 下一步

1. **评审并合并 PR #33**（`#27`）：规则集 `protect-develop` 要求 1 名非作者审批，CI 已全绿
2. 三个开发各自把 `Ready` 列的卡片拖到 `进行中`（**AI 拖不了，必须人手动拖**）
3. 代一鸣先做 `#24`（监听器空实现）——不做完，后面所有事件都会被堵死
4. 王子昂的 `#31` / `#32` 与代码无关，可以立刻并行开始
5. PO 等 `#25–#29` 全部合并后再做 `#30`（CI 门禁）
