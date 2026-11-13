# 代一鸣（qwerbnmv）的进度

最后更新：2026-11-13
当前任务：`#24` T2-00 三个下游监听器改空实现（批次 2）
状态：待评审
卡在哪：无
下一步：
- 开 PR（`Closes #24`），等 PO Approve 后合并
- `#26` T2-02 VoteService 的 PR #37 也在等评审，两张卡都合并后 Sprint 2 门禁达成

## 已完成

- **2026-11-13**：`#24` 把三个下游监听器改为空实现（23 个方法）
  - `ReputationEventHandlers`（5 个 `on(...)`）、`NotificationProjector`（6 个 `on(...)` + `notify` + `name`）、`SearchIndexProjector`（9 个 `on(...)` + `firstTime`）
  - 每个方法体由 `throw new UnsupportedOperationException("TODO(S3)")` 改为一行 `log.debug(...)`，`@EventListener` / `@Transactional` / 方法签名全部保留，`// TODO(S3)` 注释改成具体实现要点方便 Sprint 3 找回
  - 动机：`OutboxRelay.dispatch()` 捕获异常后 `failed.increment()` 并按指数退避推后 `locked_until`，超 10 次进死信。`#25` 的 `QuestionCommandService.post` 已经在发 `QuestionPostedEvent`，任何一次提问都会撞上这三个监听器然后阻塞整条事件链
  - 两个模块的 `pom.xml` 各补一条 `slf4j-api`（版本由 Spring Boot 父 POM 管理），否则编译报「程序包 org.slf4j 不存在」
  - 三个文件 `UnsupportedOperationException` 归零；`mvn -B -o -f backend/pom.xml compile -DskipTests` BUILD SUCCESS（27 模块）
  - `mvn test -pl co-qa/co-qa-application -am`：12 tests，1F/1E。该 1F/1E 与本卡无关——本分支尚未合并 `#26`，`VoteService.cast` 仍是 TODO；已用 `git stash` 在干净 develop 上复现同样的 1F/1E
  - `ArchitectureTest` 通过（1/1）
  - 说明：Issue 验收标准写的「`grep backend/co-reputation backend/co-discovery` 输出为空」在当前 develop 上不可达成，除本卡三个文件外还有 26 处其他类的 Sprint 3 TODO，需 Sprint 3 全部实现后才清零

- **2026-11-12**：`#25` 实现 `QuestionCommandService` 的 `post` / `edit` / `delete` / `load`
  - `post`：`Question.post` → `questions.save`（仓储内部 assignId）→ 发 `QuestionPostedEvent` → `tags.adjustUsage(+1)` → `listCache.invalidateAll()` → 审计 `QUESTION_POSTED`
  - `edit`：先记旧标签集合，`question.edit` 后按差集调整（移除 -1、新增 +1、两边都有不动），避免未变标签重复计数
  - `delete`：`question.softDelete` → 发 `ContentDeletedEvent`（`targetType = QUESTION`）→ `tags.adjustUsage(-1)` → 审计 `QUESTION_DELETED`
  - `load`：`questions.findById().orElseThrow(new NotFoundException("QA-4001", ...))`
  - 新增 `QuestionCommandServiceTest`（8 个用例）：事件字段、缓存失效次数、标签增量（含编辑差集）、审计动作、`QA-3002` 陌生人编辑、`QA-1013` 有回答删除、`QA-4001` 不存在、摘要截断长度
  - 错误码说明：`QA-4001`（问题不存在，NotFound 404）而非 `QA-1010`——后者已被领域层 `Question.java:102` 占用为「答案不属于该问题」（BusinessRule 422），同一个码不能有两种语义
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
