# 谢守航（kl1h）的进度

最后更新：2026-11-12
当前任务：[T2-03] AnswerCommandService：7 个方法（批次 2，Issue #27）
状态：待评审
卡在哪：无
下一步：等待 PR 合并，然后从 Ready 认领 #29（T2-05）或 #28（T2-04）

## 2026-11-12

- 完成 #27 `AnswerCommandService` 全部 7 个方法（`submit`/`edit`/`delete`/`accept`/`endorse` + `loadQuestion`/`loadAnswer`）：计数器原子增量、缓存失效恰好 1 次、采纳与认证写审计、按任务卡发布 4 类集成事件（`ContentDeletedEvent.targetType` 实为 `String`，传 `TargetType.ANSWER.name()`）
- 验证：`AnswerAndVoteServiceTest` 中 `submitAnswerPublishesEventAndBumpsCounter` 与 `onlyAskerOrCourseManagerCanAccept` 变绿（另 2 个失败属 `VoteService` 卡，不在本卡范围）；领域测试 53/53 绿；`ArchitectureTest` 通过
- PR #18（#7、#8）已于 2026-11-09 合并 ✅

## 2026-11-09

- 完成 #7 `Answer` 聚合 5 个方法：`submit`（已删除问题拒绝回答 QA-1020）、`edit`（非作者/非课程管理者 403 QA-3002）、`endorse`（非课程管理者 403 QA-3004，重复认证 409 QA-2003）、`softDelete`（被采纳回答本人删除 422 QA-1012）、`ensureNotDeleted`
- 完成 #8 `Vote`（`cast` 禁止自投 QA-1015、`checkValue` 仅 ±1 QA-1014、`change` 返回旧值并更新 `updatedAt`）与 `Comment.post`（正文 2–600 字符 QA-1003、仅一层回复 QA-1016、回复须与父评论同目标 QA-1017）
- 验证：`AnswerTest` 3/3 绿、`VoteAndCommentTest` 3/3 绿、`ArchitectureTest` 通过；未修改任何已有断言
- 说明：本机无 `mvn` 命令，用临时下载的 Maven 3.9.9 + JDK 21 跑的测试
- 按 PO 评审修改：`endorse` 去掉多余的 `isTeacherOrAdmin()` 判断（课程内角色由 `actorManagesCourse` 表达，避免误伤全局 role 为 STUDENT 的课程助教）；`checkValue` 错误消息改为"撤销由应用层删除记录实现"以统一口径
