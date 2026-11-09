# 谢守航（kl1h）的进度

最后更新：2026-11-09
当前任务：[T1-04] Answer 聚合 + [T1-05] Vote 与 Comment 领域规则（批次 1，Issue #7、#8）
状态：待评审（PR #18，已按 PO 评审意见修改一轮）
卡在哪：无
下一步：等待 PR #18 合并，合并后认领批次 2 的命令服务任务

## 2026-11-09

- 完成 #7 `Answer` 聚合 5 个方法：`submit`（已删除问题拒绝回答 QA-1020）、`edit`（非作者/非课程管理者 403 QA-3002）、`endorse`（非课程管理者 403 QA-3004，重复认证 409 QA-2003）、`softDelete`（被采纳回答本人删除 422 QA-1012）、`ensureNotDeleted`
- 完成 #8 `Vote`（`cast` 禁止自投 QA-1015、`checkValue` 仅 ±1 QA-1014、`change` 返回旧值并更新 `updatedAt`）与 `Comment.post`（正文 2–600 字符 QA-1003、仅一层回复 QA-1016、回复须与父评论同目标 QA-1017）
- 验证：`AnswerTest` 3/3 绿、`VoteAndCommentTest` 3/3 绿、`ArchitectureTest` 通过；未修改任何已有断言
- 说明：本机无 `mvn` 命令，用临时下载的 Maven 3.9.9 + JDK 21 跑的测试
- 按 PO 评审修改：`endorse` 去掉多余的 `isTeacherOrAdmin()` 判断（课程内角色由 `actorManagesCourse` 表达，避免误伤全局 role 为 STUDENT 的课程助教）；`checkValue` 错误消息改为"撤销由应用层删除记录实现"以统一口径
