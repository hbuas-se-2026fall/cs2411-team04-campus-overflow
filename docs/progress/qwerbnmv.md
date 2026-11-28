# 代一鸣（qwerbnmv）的进度

最后更新：2026-11-28
当前任务：`#64` T4-05 前端页面补齐：管理页 + 个人资料页（批次 4）
状态：待评审
卡在哪：无
下一步：
- `#64` 的 PR 等 PO Approve 后合并
- 合并后转 `#63` T4-04 前端体验升级（分页 / 骨架屏 / 重试 / Markdown）
- Sprint 4 用户名下剩余：`#61`（等 kl1h 的 `#66`/`#67` 解除阻塞）、`#65`（卡面写明时间紧可整卡不做）
- 当前测试基线：`co-identity-domain` **17 个测试全绿**、`co-identity-application` **22 个测试全绿**（`AdminUserServiceDisableTest` 9 + `UserCommandServiceUpdateProfileTest` 13）；`ArchitectureTest` 1/1

## 已完成

- **2026-11-28**：`#64` 前端页面补齐：管理页 + 个人资料页（PB-47 / PB-41）
  - **后端 `PUT /api/v1/auth/me` 的路径选 `/auth/me` 而非 `/users/{id}`**：URL 里不带用户 id，方法签名也就没有「改谁」这个参数，从接口层面杜绝越权改他人资料，不需要再写一遍「id 是不是你自己」的校验
  - **`PUT /auth/me` 采用部分更新语义**：`null` = 本次不改这一项，空串 = 清空。最初的实现把 `null` 同时当成「清空」（学院/班级）和「不能为空」（昵称），导致「只传学院」既会 422 又会把学院/班级清空——评审指出文档与实现矛盾后改为部分更新，`UserCommandService.keepIfAbsent` 负责把 `null` 换成库里现值。选部分更新而非整表替换的理由：调用方（移动端/脚本/第三方集成）很难保证每次都带全三个字段，「缺省即清空」下少传一个字段就等于静默删掉用户资料
  - `AuthController.updateProfile` 返 **204 而不是新的 `MeView`**：前端提交成功后本来就要重新拉 `/auth/me` 刷新 `auth.tsx` 里的全局状态（否则顶栏还显示旧昵称），返回体用不上；少一处数据契约也少一处与 `MeView` 同步的负担
  - `User.updateProfile` 只改昵称 / 学院 / 班级三个字段，`username` / `role` / `verified` / `email` 仍是 `final` 或不在方法内——**改不了的字段在领域对象上就没有赋值路径**，比在应用层做白名单更可靠
  - **昵称唯一性放应用层而不是领域层**：领域对象看不到仓储。`UserCommandService.updateProfile` 用 `findByDisplayName` 再比 id，而**不是** `existsByDisplayName`——后者分不清「被别人占了」和「就是我自己」，用户只改学院、昵称原样提交也会被判成重名（`ID-2002`）
  - `User.updateProfile` 复用 `checkDisplayName`，所以长度（2–20）与字符（不含 `@`/空格）规则、错误码 `ID-1002` 与注册时**完全一致**；学院/班级复用 `optional(value, 50)`，空白字符串归一成 `null`，否则「未填写」会有 `''` 和 `NULL` 两种表示
  - **`ProfileUpdateRequest.displayName` 故意不加 `@Size`**：加了会让「昵称太短」出现两种失败方式——空串被 Bean Validation 拦成 400 `SYS-400-VALIDATION`、过短值走领域层得到 422 `ID-1002`，前端得为同一个问题处理两套码。去掉后领域层是唯一出口。`college`/`className` 的 `@Size(max = 50)` 反而保留：领域层 `User.optional` 对超长值是**静默截断**而非报错，没有错误码可统一，留在这里报 400 比悄悄截掉用户输入要好
  - **只写审计 `PROFILE_UPDATED`、不发领域事件**：昵称变更不影响任何聚合的一致性，检索与通知都在读取时实时联表取 `displayName`，没有需要同步的物化副本
  - 管理页 `frontend/src/pages/AdminPage.tsx`：三个区块对应 `AdminUserController` 的端点（待认证列表 / 认证 / 创建教职工）。认证成功后**重新拉一次列表**而不是本地删行——本地删行会掩盖「其实没保存成功」这类问题
  - **前端的 `me.role === 'ADMIN'` 判断不是安全边界**，真正的授权在 `SecurityConfig.java:73` 的 `hasRole("ADMIN")`，后端每次都会再校验；前端这层只为不把必然 403 的页面摆到学生面前。非管理员访问 `/admin` 显示「无权访问」而非白屏
  - 个人资料页 `frontend/src/pages/ProfilePage.tsx`：学号/角色/认证状态/邮箱只展示不可编辑；保存成功后 `await refresh()` 刷新全局 `me`，否则顶栏昵称不更新、用户会以为没保存成功；`dirty` 判断让「没有任何改动」时保存按钮禁用。表单 `useEffect` 依赖故意只写 `[me]` 而非三个字段——`AuthProvider` 每次 refresh 都换新对象，只在「拉到新数据」时触发；若拆成三个字段，用户正在输入时恰好有一次 refresh 把值改回旧值，依赖判定「没变」不重跑，输入框会停在旧值上
  - `api.ts` 新增 `describeError(e)`：`ApiError` 自带中文 `detail` 直接用；网络中断拿到的是 `TypeError("Failed to fetch")`，英文且面向开发者，统一换成中文兜底
  - 导航：`/profile` 所有登录用户可见，`/admin` 仅 `me.role === 'ADMIN'` 可见
  - `api.ts` 的管理端方法只保留本页实际调用的三个（`adminUsers` / `adminVerify` / `adminCreateStaff`）：`#62` 的禁用端点暂无 UI 入口，对应方法不提前定义，等做禁用按钮那张卡时再加，避免出现定义了却没人调用的死代码
  - 验证：`tsc --noEmit` exit=0、`tsc -b && vite build` exit=0（45 modules，dist 188.07 kB / gzip 60.85 kB）；`co-identity-application` **Tests run: 22, Failures: 0, Errors: 0**；`co-identity-domain` 17 全绿；`ArchitectureTest` 1/1
  - 新增 `UserCommandServiceUpdateProfileTest` **13 个用例**（成功更新三字段 / 审计内容 / 昵称原样提交不算重名 / **昵称传 null 保留现值** / **学院班级传 null 保留现值** / **三字段全 null 不改值但仍写审计** / **同请求内 null 保留与空串清空并存** / **昵称空串走 `ID-1002` 而非 400 且整笔不落地** / 昵称被他人占用被拒 / 昵称过短走 `ID-1002` / 空白学院班级变 null / 陌生 actor 被拒 / 不动 username·role·verified），手写 Fake 未引入 Mockito，**既有测试一个断言都没改**
  - `docs/错误码对照表.md`：`User.java` 加方法后行号位移，`ID-1001` `:127`→`:145`、`ID-1002` `:133`/`:135`→`:151`/`:153`、`ID-1003` `:142`→`:160`、`ID-3001` `:121`→`:139`；`ID-2002` 与 `ID-4001` 各增一处抛出位置（`UserCommandService.java:58` / `:52`），相关批次列改为「批次 1、批次 4」并在备注里写清哪处属哪批次

- **2026-11-27**：`#62` 管理员禁用账号与会话清理（PB-40）
  - **会话清理选方案 A（`FindByIndexNameSessionRepository`）**，理由是重启后仍然有效：`SessionRegistryImpl` 是纯内存注册表，应用一重启就空，重启前登录的会话不在表里，禁用时一个都清不掉；而 `findByPrincipalName` 直接查 Redis 索引，重启后照样查得到
  - 只需在 `application.yml` 加 **一行** `spring.session.redis.repository-type: indexed`，不用改 `SecurityConfig`、不用加 `@EnableRedisIndexedHttpSession`。反编译 `spring-boot-autoconfigure-3.4.5.jar` 的 `RedisSessionConfiguration$IndexedRedisSessionConfiguration` 确认它带 `@ConditionalOnProperty(prefix="spring.session.redis", name="repository-type", havingValue="indexed")` + `@Import(RedisIndexedHttpSessionConfiguration)`，配了这个属性就会装配
  - **索引的 principal 是学号/工号不是昵称**：`PrincipalNameIndexResolver` 取 `authentication.getName()`，即 `SecurityUser.getUsername()` 返回的 `username` 字段（`2023001`），而 `toActor()` 用的是 `displayName`。写成 `findByPrincipalName(displayName())` 不会报错，只会永远返回空集合、禁用「看起来成功」但会话没清掉
  - **不绕道 `SpringSessionBackedSessionRegistry`**：它的 `getAllPrincipals()` 直接 `throw new UnsupportedOperationException`（反编译 `spring-session-core-3.4.3.jar` 确认），拿不到全量 principal
  - `AdminUserService.disable(Actor, long)`：`load`（不存在抛 `ID-4001`）→ `user.disable(actor)`（领域层已校验管理员身份 `ID-3001` 与「不能禁用自己」`ID-1007`，应用层**不重复校验**）→ `users.save` → `sessions.revokeAll(user.username())` → `audit.record("USER_DISABLED", ...)`
  - **顺序是「先落库、后清会话」，且清会话失败要抛出去让事务回滚**：会话在 Redis、事务在 MySQL，两者无法真正原子。反过来（先清后落库）一旦落库失败，用户明明还是正常状态却被踢下线；而落库成功、清会话静默失败，则用户已被停用却还能拿旧 Cookie 继续操作，正是本卡要堵的漏洞。选择「宁可禁用失败，也不留下未清理的会话」
  - `POST /api/v1/admin/users/{userId}/disable` 返 204；`SecurityConfig.java:73` 的 `.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")` 已覆盖，无需改安全配置
  - 新增领域层 `UserDisableTest` **8 个用例**（权限 4 / 状态迁移 4），覆盖「自禁失败后状态不变」「重复禁用幂等」「非管理员对已禁用用户仍被拒」；新增应用层 `AdminUserServiceDisableTest` **9 个用例**（含「先 save 后清会话」的调用顺序断言、「按 username 不按 displayName 清」、清会话异常必须冒泡），全部手写 Fake、未引入 Mockito，**既有 `UserTest` 6 个用例一个字没改**
  - 验证：`co-identity-domain` `Tests run: 17, Failures: 0, Errors: 0`；`co-identity-application` `Tests run: 9, Failures: 0, Errors: 0`；`ArchitectureTest` 1/1
  - 端到端实测（容器环境）：学生 2023002 登录 → 管理员 `POST /admin/users/5/disable` 返回 204 → 学生用原 Cookie 调 `/api/v1/auth/me` 返回 **401** → 学生重新登录也返回 **401**；Redis 里 `co:session:index:...PRINCIPAL_NAME_INDEX_NAME:2023002` 及对应会话键全部消失
  - `docs/错误码对照表.md`：`AdminUserService` 加方法后行号位移，`ID-3001` 的 `:88` → `:111`、`ID-4001` 的 `:83` → `:106`


- **2026-11-26**：`#60` Discovery 领域层 7 个方法
  - `SearchQuery.terms()` / `terms(String)`：按 `\s+` 切分、去空串、**先去重再 `limit(MAX_TERMS)`**（若先限流再去重，`@张三 @张三 @李四 @王五 @赵六 @钱七` 会把第 5 个人挤掉）；`keyword == null || isBlank()` 返回 `List.of()`
  - `RelatedQuestionPolicy.jaccard`：`shared / (sizeA + sizeB - shared)`，分母 `<= 0` 返回 `0.0`
  - `RelatedQuestionPolicy.rank`：**四级排序** —— Jaccard 降序 → 得分降序 → 已采纳优先 → `questionId` 升序。第四级是稳定性兜底：前三项全并列时 `List.sort` 不保证稳定，分页会重复返回同一条。`candidates == null || isEmpty() || limit <= 0` 返回空
  - `MentionParser.extract`：`LinkedHashSet` 去重保序，**「满 5 个就停」写在 `while (matcher.find() && mentions.size() < MAX_MENTIONS)` 里**，长评论不必扫到底；`text == null` 返回 `Set.of()`
  - `Notification.create`：标题 `strip()` 后为空抛 `IllegalArgumentException`（**不是** `BusinessRuleException`）—— 标题由系统拼装、不来自用户输入，为空说明代码缺陷；用业务异常会被 `OutboxRelay` 当可重试错误退避十次进死信，把编码错误伪装成环境问题。超 200 码点**截断而非拒绝**（`dis_notification.title` 是 `VARCHAR(200)`，抛异常会让整条通知丢失、用户收不到提醒）
  - `Notification.markRead`：**先判权限再判是否已读**。若反过来（已读直接 return），非接收者标记已读通知会「成功」返回，等于泄露「这条通知已读」
  - 新增 4 个边界测试文件共 **58 个用例**（`SearchQueryBoundaryTest` 17 / `RelatedQuestionPolicyBoundaryTest` 13 / `MentionParserBoundaryTest` 13 / `NotificationBoundaryTest` 15），**原有 `DiscoveryDomainTest` 的 5 个用例一个字没改**
  - 验证：`co-discovery-domain` `Tests run: 63, Failures: 0, Errors: 0`（58 + 5）；`ArchitectureTest` 1/1；本卡范围 TODO 归零（application 层 7 处不归本卡）
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
