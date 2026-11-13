# 谢守航（kl1h）的进度

最后更新：2026-11-13
当前任务：`#28` T2-04 QuestionQueryService + `#29` T2-05 CommentService / QaQueryApiImpl（批次 2）
状态：待评审
卡在哪：无
下一步：开 PR（`Closes #28`、`Closes #29`），等 PO Approve 后合并；合并后从 Ready 拉 `#30` 之前先看 PO 的 CI 卡

## 2026-11-13

- `#28` 实现 `QuestionQueryService` 三个查询方法：
  - `list`：Cache-Aside（命中即返回；空结果**不写缓存**），作者信息用 `identity.findUsers` 一次批量查避免 N+1，摘要走 `renderer.toPlainText(body, 160)`
  - `detail`：浏览量 +1、回答/评论/课程/权限一次性装配；`acceptedAnswerId` 与 `answer.id()` 用 `Objects.equals` 比较（避免 Long 拆箱 NPE）；匿名访问 `PermissionsView.NONE`
  - `popularTags`：`limit <= 0` 直接返回空列表，不透传 SQL
- `#29` 实现 `CommentService.post`（解析问题 id → 父评论 → 领域校验 → 落库 → 发 `CommentPostedEvent`）与 `QaQueryApiImpl.findQuestion`（6 字段映射，不存在返回 `empty` 而非抛异常）
  - 新增错误码 **`QA-1018`（父评论不存在，404）**，已 grep 确认未重号
- 新增测试 **17 个**：`QuestionQueryServiceTest` 9、`CommentServiceTest` 5、`QaQueryApiImplTest` 3
  - `QaTestFixtures` 增补 `InMemoryComments`；`CountingCache` 从"永远 miss"改成真缓存并记录 `gets` / `puts`（原有用例只断言 `invalidations`，行为不变）
- 验证：
  - `mvn -B test -pl co-qa/co-qa-application -am` → **29/29 绿**（需先合并 PR #37；在 develop 上跑还有 2 个红，都是 `VoteService.cast` 的 TODO，属 #26）
  - `co-qa-domain` 53/53、`ArchitectureTest` 通过
- 未修改任何已有测试的断言，未改 `Comment.java` / `VoteAndCommentTest.java` / 其他人的服务

## 2026-11-12

- 完成 #27 `AnswerCommandService` 全部 7 个方法（`submit`/`edit`/`delete`/`accept`/`endorse` + `loadQuestion`/`loadAnswer`）：计数器原子增量、缓存失效恰好 1 次、采纳与认证写审计、按任务卡发布 4 类集成事件（`ContentDeletedEvent.targetType` 实为 `String`，传 `TargetType.ANSWER.name()`）
- 验证：`AnswerAndVoteServiceTest` 中 `submitAnswerPublishesEventAndBumpsCounter` 与 `onlyAskerOrCourseManagerCanAccept` 变绿（另 2 个失败属 `VoteService` 卡，不在本卡范围）；领域测试 53/53 绿；`ArchitectureTest` 通过
- PR #33 已合并（`cdb2b2c`）✅

## 2026-11-09

- 完成 #7 `Answer` 聚合 5 个方法：`submit`（已删除问题拒绝回答 QA-1020）、`edit`（非作者/非课程管理者 403 QA-3002）、`endorse`（非课程管理者 403 QA-3004，重复认证 409 QA-2003）、`softDelete`（被采纳回答本人删除 422 QA-1012）、`ensureNotDeleted`
- 完成 #8 `Vote`（`cast` 禁止自投 QA-1015、`checkValue` 仅 ±1 QA-1014、`change` 返回旧值并更新 `updatedAt`）与 `Comment.post`（正文 2–600 字符 QA-1003、仅一层回复 QA-1016、回复须与父评论同目标 QA-1017）
- 验证：`AnswerTest` 3/3 绿、`VoteAndCommentTest` 3/3 绿、`ArchitectureTest` 通过；未修改任何已有断言
- 说明：本机无 `mvn` 命令，用临时下载的 Maven 3.9.9 + JDK 21 跑的测试
- 按 PO 评审修改：`endorse` 去掉多余的 `isTeacherOrAdmin()` 判断（课程内角色由 `actorManagesCourse` 表达，避免误伤全局 role 为 STUDENT 的课程助教）；`checkValue` 错误消息改为"撤销由应用层删除记录实现"以统一口径
- PR #18（#7、#8）已于 2026-11-09 合并 ✅
