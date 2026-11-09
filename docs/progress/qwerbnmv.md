# 代一鸣（qwerbnmv）的进度

最后更新：2026-11-09
当前任务：`#6` T1-03 Question.accept 采纳规则（批次 1）
状态：待评审
卡在哪：无
下一步：
- 开 PR（`Closes #6`），等 PO Approve 后合并
- 合并后批次 1 的 Q&A 领域全部完成，可进入批次 2

## 已完成

- **2026-11-07**：`#5` 实现 `Question.post` / `edit` / `softDelete` / `ensureOpenForAnswers` / `ensureNotDeleted`（已合并，PR #16）
  - 错误码：`QA-3002`（无权编辑）、`QA-3003`（无权删除）、`QA-1013`（有回答不能删）、`QA-1020`（已删除）
  - 未回写 `score` / `answerCount` / `viewCount`（final，由仓储 `adjustXxx` 原子维护）

- **2026-11-09**：`#6` 实现 `Question.accept`
  - 错误码：`QA-3001`（非提问者且非课程管理者采纳）、`QA-2002`（重复采纳）、`QA-1010`（答案不属于本题）、`QA-1011`（已删除的回答）
  - 按 PO 评审意见，`edit()` 里的 3 处 `Objects.requireNonNull` 改为 `Guard.requireNonNull`，映射 `QA-1001` / `QA-1002` / `QA-1005`；构造函数中的 3 处属起始代码，本 PR 未改动
  - `mvn test -pl co-qa/co-qa-domain -am`：**21 tests, 0 Failures, 0 Errors**（改动前 3F/2E）
  - `ArchitectureTest` 通过
