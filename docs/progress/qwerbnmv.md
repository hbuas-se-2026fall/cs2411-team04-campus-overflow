# 代一鸣（qwerbnmv）的进度

最后更新：2026-11-12
当前任务：`#25` T2-01 QuestionCommandService 四个方法 + 新增测试（批次 2）
状态：待评审
卡在哪：无
下一步：
- 开 PR（`Closes #25`），等 PO Approve 后合并
- 合并后接 `#26` T2-02 VoteService，做完 `co-qa-application` 门禁即达成

## 已完成

- **2026-11-12**：`#25` 实现 `QuestionCommandService` 的 `post` / `edit` / `delete` / `load`
  - `post`：`Question.post` → `questions.save`（仓储内部 assignId）→ 发 `QuestionPostedEvent` → `tags.adjustUsage(+1)` → `listCache.invalidateAll()` → 审计 `QUESTION_POSTED`
  - `edit`：先记旧标签集合，`question.edit` 后按差集调整（移除 -1、新增 +1、两边都有不动），避免未变标签重复计数
  - `delete`：`question.softDelete` → 发 `ContentDeletedEvent`（`targetType = QUESTION`）→ `tags.adjustUsage(-1)` → 审计 `QUESTION_DELETED`
  - `load`：`questions.findById().orElseThrow(new NotFoundException("QA-1010", ...))`
  - 新增 `QuestionCommandServiceTest`（8 个用例）：事件字段、缓存失效次数、标签增量（含编辑差集）、审计动作、`QA-3002` 陌生人编辑、`QA-1013` 有回答删除、`QA-1010` 不存在、摘要截断长度
  - `mvn test -pl co-qa/co-qa-application -am`：`QuestionCommandServiceTest` **8 tests, 0F, 0E**；该模块整体仍 1F/1E，均来自 `AnswerAndVoteServiceTest`（等 `#26` VoteService）
  - `ArchitectureTest` 通过（1/1）

- **2026-11-09**：`#6` 实现 `Question.accept`
  - 错误码：`QA-3001`（非提问者且非课程管理者采纳）、`QA-2002`（重复采纳）、`QA-1010`（答案不属于本题）、`QA-1011`（已删除的回答）
  - 按 PO 评审意见，`edit()` 里的 3 处 `Objects.requireNonNull` 改为 `Guard.requireNonNull`，映射 `QA-1001` / `QA-1002` / `QA-1005`；构造函数中的 3 处属起始代码，本 PR 未改动
  - `mvn test -pl co-qa/co-qa-domain -am`：**21 tests, 0 Failures, 0 Errors**（改动前 3F/2E）
  - `ArchitectureTest` 通过

- **2026-11-07**：`#5` 实现 `Question.post` / `edit` / `softDelete` / `ensureOpenForAnswers` / `ensureNotDeleted`（已合并，PR #16）
  - 错误码：`QA-3002`（无权编辑）、`QA-3003`（无权删除）、`QA-1013`（有回答不能删）、`QA-1020`（已删除）
  - 未回写 `score` / `answerCount` / `viewCount`（final，由仓储 `adjustXxx` 原子维护）
