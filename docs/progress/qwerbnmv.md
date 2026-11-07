# 代一鸣（qwerbnmv）的进度

最后更新：2026-11-07
当前任务：`#5` T1-02 Question 聚合 post/edit/softDelete（批次 1）
状态：待评审
卡在哪：无
下一步：
- 开 PR（`Closes #5`），等 PO Approve 后合并
- 合并后开 `#6` T1-03 Question.accept

## 已完成

- **2026-11-07**：`#5` 实现 `Question.post` / `edit` / `softDelete` / `ensureOpenForAnswers` / `ensureNotDeleted`
  - 错误码：`QA-3002`（无权编辑）、`QA-3003`（无权删除）、`QA-1013`（有回答不能删）、`QA-1020`（已删除）
  - 未回写 `score` / `answerCount` / `viewCount`（final，由仓储 `adjustXxx` 原子维护）
  - `mvn test -pl co-qa/co-qa-domain -am`：`QuestionTest$Posting` 4/4 绿，`EditingAndDeleting` 2/3 绿
  - `ArchitectureTest` 通过
  - 剩下 1 个红 `deletedQuestionCannotBeAnswered` 依赖 `Answer.submit`（`#7`，kl1h 的卡）
