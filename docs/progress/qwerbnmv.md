# 代一鸣（qwerbnmv）的进度

最后更新：2026-11-26
当前任务：`#60` T4-01 Discovery 领域层 7 个方法（批次 4）
状态：待评审
卡在哪：无
下一步：
- `#60` 的 PR 等 PO Approve 后合并
- `#60` 合并后转 `#62` T4-03 管理员禁用账号与会话清理（本 Sprint 唯一技术难点，PR 里要说明会话清理选方案 A `FindByIndexNameSessionRepository` 还是方案 B `SessionRegistry`）
- Sprint 4 用户名下剩余：`#61`（等 kl1h 的 `#66`/`#67`）、`#63`、`#64`、`#65`（卡面写明时间紧可整卡不做）
- 当前测试基线：`co-discovery-domain` 由 5 红转为 **62 个测试全绿**；`ArchitectureTest` 1/1

## 已完成

- **2026-11-26**：`#60` Discovery 领域层 7 个方法
  - `SearchQuery.terms()` / `terms(String)`：按 `\s+` 切分、去空串、**先去重再 `limit(MAX_TERMS)`**（若先限流再去重，`@张三 @张三 @李四 @王五 @赵六 @钱七` 会把第 5 个人挤掉）；`keyword == null || isBlank()` 返回 `List.of()`
  - `RelatedQuestionPolicy.jaccard`：`shared / (sizeA + sizeB - shared)`，分母 `<= 0` 返回 `0.0`
  - `RelatedQuestionPolicy.rank`：**四级排序** —— Jaccard 降序 → 得分降序 → 已采纳优先 → `questionId` 升序。第四级是稳定性兜底：前三项全并列时 `List.sort` 不保证稳定，分页会重复返回同一条。`candidates == null || isEmpty() || limit <= 0` 返回空
  - `MentionParser.extract`：`LinkedHashSet` 去重保序，**「满 5 个就停」写在 `while (matcher.find() && mentions.size() < MAX_MENTIONS)` 里**，长评论不必扫到底；`text == null` 返回 `Set.of()`
  - `Notification.create`：标题 `strip()` 后为空抛 `IllegalArgumentException`（**不是** `BusinessRuleException`）—— 标题由系统拼装、不来自用户输入，为空说明代码缺陷；用业务异常会被 `OutboxRelay` 当可重试错误退避十次进死信，把编码错误伪装成环境问题。超 200 码点**截断而非拒绝**（`dis_notification.title` 是 `VARCHAR(200)`，抛异常会让整条通知丢失、用户收不到提醒）
  - `Notification.markRead`：**先判权限再判是否已读**。若反过来（已读直接 return），非接收者标记已读通知会「成功」返回，等于泄露「这条通知已读」
  - 新增 4 个边界测试文件共 **57 个用例**（`SearchQueryBoundaryTest` 17 / `RelatedQuestionPolicyBoundaryTest` 13 / `MentionParserBoundaryTest` 13 / `NotificationBoundaryTest` 14），**原有 `DiscoveryDomainTest` 的 5 个用例一个字没改**
  - 验证：`co-discovery-domain` `Tests run: 62, Failures: 0, Errors: 0`；`ArchitectureTest` 1/1；本卡范围 TODO 归零（application 层 7 处不归本卡）
  - `docs/错误码对照表.md`：`DIS-3001` 从「尚未定义的码」移入主表（`DIS-4001` 仍属 application 层的 `NotificationService`，留在未定义表）；唯一码总数 60 → **61**


- **2026-11-24**：`#76` Docker 环境统一与基线报告
  - 装 **Docker Desktop 4.100.0** + **WSL2 3.0.1.0**（内核 6.18.40.1-1）：`wsl --install --no-distribution`，重启后 `winget install Docker.DockerDesktop`
  - **磁盘镜像搬出 C 盘**：Settings → VM resources → Disk image location → `D:\DockerData\DockerDesktopWSL`（`disk\docker_data.vhdx` 1.46 GB + `main\ext4.vhdx` 0.09 GB）；程序本体仍在 `C:\Program Files\Docker`（3.91 GB，搬不了）
  - **本机 MySQL 改手动启动**（原 `StartType=Automatic` 会抢 3306）：cmd 里 `sc config MySQL start= demand`，验证 `START_TYPE : 3 DEMAND_START`
  - 新增 `docs/reports/docker-baseline.md`（368 行）：环境版本、143 个测试结果、15 项扩展检测逐项结论、容器 vs 本地差异、复现步骤，引用 3 张截图
  - 新增 `docs/reports/screenshots/`（3 张，共 0.94 MB）：`01-docker-version.png`、`02-hello-world.png`、`03-compose-ps.png`
  - 新增 `http/docker-check.ps1`（UTF-8 with BOM，**17/17 全过 exit 0**）：12 个检查项，含 docker 命令、daemon、WSL2、端口、compose 可解析、三容器 healthy、MySQL 参数与四上下文表、Redis、全文索引、readiness 与 5 个业务指标、前端 200、时区一致性
  - `docs/开发者须知.md` 新增第 6 节「Docker 环境」（8 个小节：装什么、安装步骤、磁盘迁移、本机 MySQL 处理、常用命令、一键自检、清库重建、8 条报错对照）
  - `docs/tech-debt.md`：A5（安装 Docker）标 ✅ 已完成；B1（Flyway 兼容性）标 ✅ 已验证——容器 MySQL **8.0.39** 上 5 个迁移全部 `Successfully applied`（2.629 秒），建出 20 张表
  - **关键结论**：`mvn verify` 的失败原因是**测试断言**而非连接失败——`co-discovery-domain` 的 5 个红全是 `AssertionError: Expecting ... BusinessRuleException but was UnsupportedOperationException: TODO(S3)：SearchQuery.terms 尚未实现`；对全部 surefire 报告扫 `Connection refused|Communications link failure|Unable to acquire JDBC|CannotGetJdbcConnection|ConnectException` **零命中**
  - **发件箱投递验证**：26 条事件全部 `published`、`pending=0`、`attempts=0`、`last_error=NULL`，覆盖 10 种事件类型（含下游产生的 `BadgeAwardedEvent` 4 条与 `ReputationChangedEvent` 4 条，证明「Q&A 发事件 → 消费 → 记账 → 发新事件」链条通）；端到端实测发问题后 **29.7 毫秒**内投递
  - **时区一致性**：宿主 UTC / 容器 `date -u` / 容器 `date` / MySQL `NOW()` / MySQL `UTC_TIMESTAMP()` 全部 `2026-11-24 08:06:27~28`，`@@time_zone=+00:00`、`@@system_time_zone=UTC`，`outbox_event.occurred_at` 存 UTC，后端日志带 `Z` → **全链路统一 UTC，无时区错乱**
  - 踩坑：Docker Hub 的 IPv6 不可达导致拉镜像随机超时（`dial tcp [2a03:...]:443`），必须**先 `docker pull` 预拉再 `build`**，backend 首次 4.1 分钟、web 0.6 分钟
  - 踩坑：`mysql -p密码` 的 stderr 警告会混进 PowerShell 输出把行解析全打乱（曾误判「共 4 张表」），改用 `docker exec -e MYSQL_PWD=xxx co-mysql mysql -uxxx`
  - 踩坑：PS 5.1 的 `Invoke-WebRequest.Content` 是 `Byte[]`，`.Trim()` 报 `[System.Byte] does not contain a method named 'Trim'`；已在脚本里加 `Get-ResponseText` 统一解码（同时修好了 prometheus 指标那处的同类隐患）

- **2026-11-17**：`#46` 实现徽章与学期键 5 个方法 + 5 个事件处理器真正记账
  - `AcademicTerm`：`termKey`（2–7 月春季、8–12 月秋季，**次年 1 月仍属上一年秋季**，所以 `2027-01` → `2026-FALL`）、`weekKey`（用 `IsoFields.WEEK_BASED_YEAR` 而非日历年，否则跨年那一周会出错；补零到两位）、`monthKey`（补零）
  - `BadgePolicy`：`forAcceptedCount`（≥1 授 `FIRST_ACCEPTED`、≥10 授 `HELPER_10`）、`forReputation`（≥100 授 `REPUTATION_100`、≥1000 授 `REPUTATION_1000`）；返回的是「此刻应持有的全部徽章」而非增量，配合 `grant` 幂等
  - `BadgeService.grant`：`badges.has(userId, type)` 为真直接返回（不重复发事件）→ `badges.award(new BadgeAward(...))` → 发 `BadgeAwardedEvent(type.name(), type.displayName())`
  - `ReputationEventHandlers` 5 个处理器：**开户**（账户已存在则忽略；初始积分是悬赏积分不进账本，否则破坏「声誉 = 流水之和」FF-8）、**采纳**（`acceptedDelta` → `record` → 用 `countByUserAndReason` 算采纳数 → `BadgePolicy.forAcceptedCount` 授徽章）、**投票**（`voteDelta(old, new)` → `delta > 0` 时用 `sumPositiveVoteDeltaSince(作者, UTC 当日零点)` 套每日上限 → 记账；reason 按 `targetType` 取 `QUESTION_VOTE` / `ANSWER_VOTE`）、**首个回答**、**教师认证**
  - `VoteCastEvent.targetType` 用字面量 `"QUESTION"` / `"ANSWER"` 判断而非 import qa 的 `TargetType` 枚举——跨上下文只能走 `*-api`，而 `TargetType` 在 `co-qa-domain` 里
  - 每日上限按 **UTC** 自然日（`ReputationRulebook` 类注释明确写「每人每个自然日（UTC）」），用 `LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC).atStartOfDay()`
  - **采纳结算的最后一步「悬赏结算」故意未调用**：`BountyService.settleOnAccept` 仍是 `TODO(S3)`（属 `#47`），现在调用会让每个 `AnswerAccepted` 事件抛异常、被 OutboxRelay 指数退避、十次后进死信，把整条声誉链堵死。已在方法内留 `// TODO(#47)` 标记，`bounties` 字段保持注入
  - 新增 `ReputationEventHandlersTest`（14 个用例，全手写替身不启 Spring）：开户积分按角色、开户幂等不重置余额、采纳 +15、采纳幂等、自问自答 0 分不写流水、首个采纳授徽章、被赞 +10、问题维度用 `QUESTION_VOTE`、赞改踩 −12、每日上限削到 0、上限不影响踩、投票幂等、`FIRST_ANSWER` 不重复、`TEACHER_ENDORSED` 不重复
  - 新增 `ReputationTestFixtures`（`InMemoryAccounts` / `InMemoryLedger` / `InMemoryBadges` / `RecordingEvents`）
  - 测试：`co-reputation-application` **14 tests, 0F, 0E**（该模块此前一个测试都没有）；`BountyAndAccountTest` 的 `badgePolicyThresholds` 与 `academicTermKeysFollowCampusCalendar` 转绿；`ArchitectureTest` 1/1
  - `co-reputation-domain` 从 5 红降到 **3 红**，剩余的 `onlyCourseManagerCanOpenBountyWithinLimits` / `bountyCanBeSettledOnlyOnce` / `sponsorCannotWinOwnBountyAndDueDetectionWorks` 全部卡在 `Bounty.open`，属 `#47`

- **2026-11-17**：`#45` 实现 `ReputationAccount` 7 个方法 + `ReputationRulebook` 4 个方法
  - `ReputationAccount`：`open`（声誉 0、可用 = 开户积分、冻结 0、version 0，负数抛 `REP-1006`）、`applyReputation`（直接 `reputation += delta`，**故意不设下限截断**，否则破坏「余额 = 流水之和」的对账不变量 FF-8）、`freezePoints`（`requirePositive` → 余额不足抛 `REP-1002` → `available -= p; frozen += p`）、`spendFrozen`（`ensureFrozen` 后 `frozen -= p`）、`refundFrozen`（`ensureFrozen` 后 `frozen -= p; available += p`）、`ensureFrozen`、`requirePositive`
  - `ReputationRulebook`：`voteEffect`（1→+10、-1→-2、其余 0）、`voteDelta`（= `voteEffect(new) - voteEffect(old)`，改投撤销统一按差值算，避免历史票值被重复累加）、`applyDailyCap`（`delta <= 0` 原样返回，否则 `max(0, min(delta, 200 - gainedToday))`）、`initialPoints`（教师/管理员 1000、助教 500、学生 0）
  - 两个钱包彼此独立：声誉只能通过账本流水变化，悬赏积分是另一套货币；不变量 `availablePoints >= 0` 且 `frozenPoints >= 0`
  - `initialPoints(TA)` = 500 是**自选值**：指导书把 TA / ADMIN 的额度留给 PO 决定（建议 TA 500、ADMIN 1000），此处取建议值，待 PO 确认后可能需要落到 ADR
  - 测试：`ReputationRulebookTest` **9 tests, 0F, 0E**（含 6 组参数化 `voteDelta`），`BountyAndAccountTest.freezeSpendAndRefundKeepPointsConsistent` 通过
  - `co-reputation-domain` 整体从 14 红降到 **1F / 4E**，剩余 5 个全部属于 #46（徽章 / 学期键）与 #47（悬赏），不是本卡范围
  - `ArchitectureTest` 通过（1/1）

- **2026-11-13**：`#26` 按 PO 评审意见补强（已合并，PR #37）
  - 修正 `VoteService.java:95` 与本文档中的红线编号：「第 4 条」→「第 6 条」（计数器只用 `adjustXxx` 原子更新）
  - 新增 `VoteServiceTest`（5 个用例）：撤销空操作、撤销赞、撤销踩、撤销后再投、问题维度撤销
    - 踩让分值 0 → -1，撤销增量 `-(-1) = +1`，所以用例名是 `revokingDownvoteRevertsScoreByPlusOne`（一开始误写 ByPlusTwo，PO 指出后改正）
    - 问题维度用例必须由另一人投票，否则撞 `Vote.cast` 的自投校验 `QA-1015`
  - `docs/tech-debt.md` 新增 A8（已删除内容仍可投票），建议错误码用 `QA-1021`（`QA-1020` 已被 `Answer.java:89` / `Question.java:134` 占用）

- **2026-11-13**：`#24` 把三个下游监听器改为空实现（23 个方法）
  - `ReputationEventHandlers`（5 个 `on(...)`）、`NotificationProjector`（6 个 `on(...)` + `notify` + `name`）、`SearchIndexProjector`（9 个 `on(...)` + `firstTime`）
  - 每个方法体由 `throw new UnsupportedOperationException("TODO(S3)")` 改为一行 `log.debug(...)`，`@EventListener` / `@Transactional` / 方法签名全部保留，`// TODO(S3)` 注释改成具体实现要点方便 Sprint 3 找回
  - 动机：`OutboxRelay.dispatch()` 捕获异常后 `failed.increment()` 并按指数退避推后 `locked_until`，超 10 次进死信。`#25` 的 `QuestionCommandService.post` 已经在发 `QuestionPostedEvent`，任何一次提问都会撞上这三个监听器然后阻塞整条事件链
  - 两个模块的 `pom.xml` 各补一条 `slf4j-api`（版本由 Spring Boot 父 POM 管理），否则编译报「程序包 org.slf4j 不存在」
  - 三个文件 `UnsupportedOperationException` 归零；`mvn -B -o -f backend/pom.xml compile -DskipTests` BUILD SUCCESS（27 模块）
  - `ArchitectureTest` 通过（1/1）
  - 说明：Issue 验收标准写的「`grep backend/co-reputation backend/co-discovery` 输出为空」在当前 develop 上不可达成，除本卡三个文件外还有 26 处其他类的 Sprint 3 TODO，需 Sprint 3 全部实现后才清零

- **2026-11-12**：`#26` 实现 `VoteService.cast` / `resolve`（已合并，PR #37）
  - `cast` 五分支：撤销（`value == 0`，无旧票则空操作、有则 `votes.delete` + `adjustScore(-oldValue)`）、新增投票、幂等（`oldValue == value` 时不写库不发事件）、改投（`change` + `adjustScore(value - oldValue)`）、自投由 `Vote.cast` 抛 `QA-1015`
  - `resolve`：`QUESTION` → 查 `questions`，`ANSWER` → 查 `answers`；不存在分别抛 `QA-4001` / `QA-4002`
  - `applyScore` 私有方法：`delta == 0` 直接返回，其余走 `questions.adjustScore` / `answers.adjustScore` 原子增量（AGENTS.md 技术红线第 6 条）
  - `publish` 私有方法：统一发 `VoteCastEvent`（携带 `oldValue` / `newValue`，下游据此算声誉增量）

- **2026-11-12**：`#25` 实现 `QuestionCommandService` 的 `post` / `edit` / `delete` / `load`（已合并，PR #36）
  - `post`：`Question.post` → `questions.save`（仓储内部 assignId）→ 发 `QuestionPostedEvent` → `tags.adjustUsage(+1)` → `listCache.invalidateAll()` → 审计 `QUESTION_POSTED`
  - `edit`：先记旧标签集合，`question.edit` 后按差集调整（移除 -1、新增 +1、两边都有不动），避免未变标签重复计数
  - `delete`：`question.softDelete` → 发 `ContentDeletedEvent`（`targetType = QUESTION`）→ `tags.adjustUsage(-1)` → 审计 `QUESTION_DELETED`
  - `load`：`questions.findById().orElseThrow(new NotFoundException("QA-4001", ...))`
  - 新增 `QuestionCommandServiceTest`（8 个用例）：事件字段、缓存失效次数、标签增量（含编辑差集）、审计动作、`QA-3002` 陌生人编辑、`QA-1013` 有回答删除、`QA-4001` 不存在、摘要截断长度
  - 错误码说明：`QA-4001`（问题不存在，NotFound 404）而非 `QA-1010`——后者已被领域层 `Question.java:102` 占用为「答案不属于该问题」（BusinessRule 422），同一个码不能有两种语义

- **2026-11-09**：`#6` 实现 `Question.accept`
  - 错误码：`QA-3001`（非提问者且非课程管理者采纳）、`QA-2002`（重复采纳）、`QA-1010`（答案不属于本题）、`QA-1011`（已删除的回答）
  - 按 PO 评审意见，`edit()` 里的 3 处 `Objects.requireNonNull` 改为 `Guard.requireNonNull`，映射 `QA-1001` / `QA-1002` / `QA-1005`；构造函数中的 3 处属起始代码，本 PR 未改动
  - `mvn test -pl co-qa/co-qa-domain -am`：**21 tests, 0 Failures, 0 Errors**（改动前 3F/2E）
  - `ArchitectureTest` 通过

- **2026-11-07**：`#5` 实现 `Question.post` / `edit` / `softDelete` / `ensureOpenForAnswers` / `ensureNotDeleted`（已合并，PR #16）
  - 错误码：`QA-3002`（无权编辑）、`QA-3003`（无权删除）、`QA-1013`（有回答不能删）、`QA-1020`（已删除）
  - 未回写 `score` / `answerCount` / `viewCount`（final，由仓储 `adjustXxx` 原子维护）
