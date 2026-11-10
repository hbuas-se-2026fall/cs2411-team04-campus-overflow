# CampusOverflow · PO 任务分配与 Sprint 作战手册

> 依据：《CampusOverflow 实验指导书 **V3**》（8 周 / 5 Sprint / 47 个 PBI / 107 个 TODO）与起始代码实测结果。
> 本文件面向 **Product Owner**：怎么带队、每个 Sprint 谁做什么、什么时候必须做决定。
> 文中「建议」= 本文件的补充判断，指导书原文没有；「文档规定」= 指导书硬性要求。

---

## 0. 一页速览

| 维度 | 结论 |
| --- | --- |
| 项目是什么 | 校园版 Stack Overflow：按课程提问/回答/投票/评论/采纳，声誉+徽章+悬赏+排行榜激励，中文全文搜索+相关推荐+站内通知 |
| 技术形态 | 模块化单体 + 六边形分层，4 个限界上下文 × 5 层 = 27 个 Maven 模块 + React 18 前端 |
| 起始代码 | Identity 上下文**已完整实现**（当范例）；Q&A / Reputation / Discovery 只剩骨架，**107 个 `UnsupportedOperationException` 待实现**（实测：36 个标 `TODO(S2)`、71 个标 `TODO(S3)`，分布在 32 个文件） |
| 你的交付节奏 | S0 启动(第1周) → S1 问答核心(2–3周) → S2 声誉激励(4–5周) → S3 内容发现(6–7周) → S4 发布(第8周) |
| 每个 Sprint 末 | `main` 打标签 v0.1.0 / v0.2.0 / v0.3.0 / v0.9.0 / v1.0.0，评审会现场从标签启动演示 |
| 考核权重 | Sprint 评审 40% + 最终产品答辩 25% + 工程质量 20% + 过程协作 15%，个人成绩 = 团队成绩 × 贡献系数(0.6–1.2) |

**⚠️ 第一个坑：编号错位。** 代码里的 `TODO(Sx)` 和 `docs/学生任务清单.md` 沿用 README 的 15 周旧编号，V3 把 8 周重排了：

| 代码/清单里的标签 | 实际属于 | 周次 | 方法数 | 你该建的 Milestone |
| --- | --- | --- | --- | --- |
| `TODO(S2)` + 清单「S2 · Q&A」 | **V3 Sprint 1** | 第 2–3 周 | 36 | `Sprint 1` |
| `TODO(S3)` + 清单「S3 · Reputation」 | **V3 Sprint 2** | 第 4–5 周 | 39 | `Sprint 2` |
| `TODO(S3)` + 清单「S3 · Discovery」 | **V3 Sprint 3** | 第 6–7 周 | 32 | `Sprint 3` |
| （无 TODO） | V3 Sprint 0 / 4 | 第 1 / 8 周 | 0 | `Sprint 0` / `Sprint 4` |

> 导入 Issue 时按**右列**的 Sprint 字段填，不要按代码标签填，否则 S1 的里程碑会漏掉一半任务。

---

## 0.5 V2 vs V3：只用 V3，但要知道它改了什么

我把两版做了逐行比对（去掉页眉页码后 V2 独有 204 行、V3 独有 304 行），结论是：

| 维度 | 是否变化 | 说明 |
| --- | --- | --- |
| Sprint 结构（8 周 / 5 个） | **没变** | 周次、Sprint Goal、版本标签 v0.1.0→v1.0.0 逐字相同 |
| Product Backlog（48 条 PBI） | **没变** | 编号、史诗、优先级、故事点、Sprint 归属零变化；SP 合计 S0=13/S1=43/S2=42/S3=31/S4=26 |
| 附录 A（107 个 TODO 映射） | **没变** | S1=36 / S2=39 / S3=32，32 个文件，逐类逐方法一致 |
| 5 人分工 A–E、T0–T4 的「负责」列 | **没变** | 可直接沿用，不用重新分工 |
| DoR/DoD、容量、质量门禁、评分 | **没变** | 40/25/20/15 与个人系数 0.6–1.2 一致 |
| **第 7 章协作模式** | **重写了** | V2：PO 建**个人** GitHub 仓库 + 加协作者，PO = 仓库所有者，免费账号私有仓库开不了分支保护→要申请 GitHub Pro 或**必要时改公开仓库**；V3：**教师**用 GitHub Education 认证账号建 **GitHub Team 组织**（如 `hbuas-se-2026fall`），每组一个 Secret 团队 + 空私有仓库 `teamNN-campus-overflow`，**PO = 本组仓库 Admin**，教师 = 组织 Owner |
| **第 8 章 Actions 用量** | **新增 8.5** | 组织共享 **每月 3000 分钟**，Sprint 0 的 T0-04 必须一并做：`concurrency: cancel-in-progress`、集成测试只在 push 跑、镜像只在 `main` 构建、Maven 缓存、`[skip ci]` |
| 术语 | 微调 | 第 1.4/2/5 章加"逆向工程"框定；PB-01 文本由"PO 创建的共享仓库"改为"教师组织中的本组私有仓库" |
| 篇幅 | 59 → 61 页 | 只增在第 7、8 章，第 1–6 章页码一致 |

**对你的三个直接影响：**

1. **以 V3 为准**。V2 那套"PO 个人仓库 + 申请 GitHub Pro / 改公开仓库"的做法已经不适用；如果你们老师还没建组织，见下面第 3 条。
2. **S0 的第一个动作不是推代码，是催老师**。V3 新增了一句前置条件：`Sprint 0 开始前由教师完成`创建组织、Secret 团队、空私有仓库并授予 PO Admin——教师认证"审核通常需要数天"。你今天就该确认这件事，否则 T0-01 全组卡住。
3. **T0-01 / T0-04 的范围变大了，工时还是 3**。T0-01 现在包含"接受组织邀请 + 导入 + 推送 + 建 develop + 分支保护（含不允许绕过）+ 建 Projects"；T0-04 现在包含 Actions 用量优化 + 写 ADR-009。重估时建议把 T0-04 的 CI 优化拆成独立 Task（E 3h → 2h + 2h），否则 S0 一定超期。

> **保底方案**（如果老师最终仍让你们用 PO 个人仓库）：私有仓库的分支保护在免费账号上不可用，只能走"申请 GitHub Pro（学生包里本来就送）"这一条，**不要**把仓库改公开——9.3 红线里"复制其他小组代码"和公开仓库是互相放大的风险。

---

## 1. 项目到底在做什么（用一句话讲给队友）

> 三个人同时在线：小明提问 → 小红回答 → 王老师认证并采纳 → 系统给小红加 15 分声誉、发徽章、结算悬赏 → 排行榜刷新 → 其他人能搜到这道题、收到通知。

这条链路拆成四个上下文，**谁也不能跳过谁**：

```
Identity（已实现）──UserSummary/UserRegisteredEvent──▶ Q&A ──事件──▶ Reputation ──事件──▶ Discovery
                                                          └──────事件──────────────┘
```

- **Identity**（支撑域，已给）：注册登录、课程与成员、角色、`canManageCourse` 授权判定。
- **Q&A**（核心域，36 个方法）：问题/回答/评论/投票/采纳/教师认证/软删除/列表详情查询。
- **Reputation**（核心域，39 个方法）：声誉账本、计分规则、每日上限、徽章、悬赏、排行榜、对账。
- **Discovery**（支撑域，32 个方法）：搜索投影、中文全文检索、相关问题推荐、@提及、站内通知。

**三个工程约束会直接决定你的排期**（务必在 S0 讲清楚）：

1. **跨上下文只能发事件**（事务性发件箱，ADR-002）→ 所以 Q&A 必须先于 Reputation，再先于 Discovery。
2. **发件箱会被"未实现的监听器"堵死**（指导书 2.5.1）→ 所以 S1 第一件事是「行走骨架」：把还没排到的监听器改成空实现 + 建 tech-debt Issue，否则整条事件链一行都跑不通。
3. **测试即需求**：38 个测试现在就是红的，**禁止改断言/删测试**（一票否决红线）。

---

## 2. 角色与人员（先定人，再分活）

文档给的是 5 人组 A–E 模型，"主责"= 技术把关 + Code Review 第一责任人，**不是独占编码**：

| 代号 | 主责方向 | 横向职责 | 建议填谁 |
| --- | --- | --- | --- |
| A | Q&A 问题与查询 | S1 的 SM | 后端最强、能把 `Question` 聚合讲清楚的人 |
| B | Q&A 回答/采纳 → Reputation 计分 | **PO + 仓库 Admin** | 你 |
| C | 投票评论 → 悬赏 → 搜索推荐 | S2 的 SM | 细心、擅长测试边界值的人 |
| D | 前端 + 排行榜 + 通知中心 | 前端负责人、S3 的 SM | 会 React 的人 |
| E | 质量与 DevOps（CI/ArchUnit/测试替身/集成测试） | S0、S4 的 SM，CI 流水线负责人 | 环境最熟、愿意帮别人排错的人 |

**SM 每个 Sprint 轮换**（A→C→D），S0 和 S4 由 E 兼。PO 不兼 SM，否则会议主持 + 需求排序 + 演示全压在一个人身上。

---

## 3. 逐 Sprint 的功能范围与分配表

> 工时单位＝理想小时；2 周 Sprint 每人容量 16–20h，**只承诺 80%**。表里「负责」是文档原方案，🟡 表示本文件建议改动（见第 4 节）。

### Sprint 0（第 1 周）· 目标：能跑起来 + 能协作，v0.1.0

| 任务 | PBI | 内容 | 负责 | 工时 |
| --- | --- | --- | --- | --- |
| T0-01 | PB-01 | PO 导入代码并推送本组仓库，建 `develop`、配分支保护（勾"不允许绕过"）、建 Projects 看板 | **B** | 3 |
| T0-02 | PB-01 | Issue/PR 模板、CODEOWNERS、标签集、Milestone（Sprint 0–4）、看板自定义字段 | A | 3 |
| T0-03 | PB-02 | 全员装 JDK21/Maven/Docker/Node20，起中间件，跑 `mvn clean test`（预期红）与前端，交截图 | 全员 | 2×5 |
| T0-04 | PB-04 | 把 CI 改成「渐进式质量门禁」+ 省 Actions 用量 + 写 ADR-009 | E | 3 |
| T0-05 | PB-03 | 精读 8 条 ADR + Identity 参考实现，每人讲 1–2 条，合成 `docs/notes/architecture.md` | 全员 | 2×5 |
| T0-06 | PB-03 | 90 分钟事件风暴 → `docs/ddd/`（上下文映射图 + 统一语言） | 全员 | 2×5 |
| T0-07 | PB-01 | **导入 48 个 PBI Issue**、Planning Poker 重估、确认 DoR/DoD 与团队约定 | **B** | 3 |
| T0-08 | PB-05 | IntelliJ HTTP Client 写 `http/auth.http`（csrf→register→login→me→courses→join） | C | 2 |
| T0-09 | PB-05 | 前端"课程"页：课程列表、我的课程、加入课程；提问页可选课程 | D | 5 |
| T0-10 | PB-01 | 破冰 PR：每人往 `docs/team.md` 写信息，走完整 PR + 至少 1 条有效评审 | 全员 | 0.5×5 |
| T0-11 | PB-02 | 教师/管理员测试账号获取方式（`UPDATE id_user SET role=...`），写进 README | E | 1 |

**S0 验收（评审会现场演示）**：任一成员的电脑启动系统 → 注册 → 登录 → 加入课程；Swagger 可访问；看板 48 个 PBI 已排序且 S1 的 PBI 满足 DoR；分支保护对 PO 本人也生效（现场展示）；每人至少 1 个已合并 PR + 1 次评审；`main` 打 v0.1.0。

> **PO 注意**：S0 是唯一"不写业务代码"的 Sprint，但它是**唯一会影响你整个学期效率的 Sprint**。分支保护、看板、CI 门禁三件事没做完，后面每周都会返工。

### Sprint 1（第 2–3 周）· 目标：提问→回答→投票→评论→采纳 闭环，v0.2.0

范围：PB-06 ~ PB-19（14 个 PBI，43 故事点，36 个 TODO 方法）

| 任务 | PBI | 内容 | 负责 | 工时 |
| --- | --- | --- | --- | --- |
| T1-01 | PB-06 | `MarkdownBody.ofQuestion/ofAnswer`、`Tags.names`（让 `QuestionTest.Posting` 全绿） | A 🟡 E | 2 |
| T1-02 | PB-06/12/13 | `Question.post/edit/ensureNotDeleted/ensureOpenForAnswers/softDelete` | A | 4 |
| T1-03 | PB-10 | `Question.accept`（`QuestionTest.Accepting` 全绿） | B | 3 |
| T1-04 | PB-09/12/13/14 | `Answer.submit/edit/endorse/softDelete/ensureNotDeleted`（`AnswerTest` 全绿） | B | 4 |
| T1-05 | PB-11/15 | `Vote.cast/change/checkValue`、`Comment.post`（`VoteAndCommentTest` 全绿） | C 🟡 C+D | 4 |
| T1-06 | PB-06~15 | 边界测试：标题 9/10/150/151 字、6 个标签、重复采纳、已删问题编辑、二次认证 | 全员 | 4 |
| T1-07 | PB-06/12/13 | `QuestionCommandService` 4 方法 + `QuestionCommandServiceTest`（复用 `QaTestFixtures`） | A | 8 |
| T1-08 | PB-09/10/13/14 | `AnswerCommandService` 7 方法（`AnswerAndVoteServiceTest` 全绿） | B | 7 |
| T1-09 | PB-11/15 | `VoteService.cast/resolve`、`CommentService.post` + `CommentServiceTest` | C | 7 |
| T1-10 | PB-07/08/16 | `QuestionQueryService.list/detail/popularTags` + 假读模型测试 | D 🟡 D+E | 10 |
| T1-11 | PB-17 | `QaQueryApiImpl.findQuestion` | E | 1 |
| T1-12 | PB-18 | **行走骨架**：Reputation/Discovery 所有 `@EventListener` 暂空实现 + tech-debt Issue | E | 1 |
| T1-13 | PB-19 | 前端：编辑/删除、评论与回复、按 `permissions` 显隐按钮、错误码提示 | D | 6 |
| T1-14 | PB-04 | CI 门禁纳入 Q&A 模块；`co-qa-domain` 覆盖率门槛 0.70 | E | 2 |
| T1-15 | — | `http/qa.http` 冒烟脚本 + 评审演示脚本 | E | 2 |

**S1 验收**：8 条 Given/When/Then 场景（见指导书 6.3.5）全过：提问 201 且标签小写去重、回答数 +1、非提问者采纳 403 `QA-3001`、重复采纳 409 `QA-2002`、点赞两次不变、给自己点赞 422 `QA-1015`、回复只能一层、教师认证与删除生效。

### Sprint 2（第 4–5 周）· 目标：问答行为 → 声誉/徽章/悬赏/排行榜（异步+幂等），v0.3.0

范围：PB-20 ~ PB-30（11 个 PBI，42 故事点，39 个 TODO 方法）

| 任务 | PBI | 内容 | 负责 | 工时 |
| --- | --- | --- | --- | --- |
| T2-01 | PB-20/22 | `ReputationRulebook`（voteEffect/voteDelta/applyDailyCap/initialPoints） | B 🟡 E | 2 |
| T2-02 | PB-20/24/25 | `ReputationAccount` 7 方法 | B | 3 |
| T2-03 | PB-24/25 | `Bounty.open/award/close/isDue/ensureOpen` | C | 3 |
| T2-04 | PB-23/26 | `BadgePolicy` 2 方法 + `AcademicTerm` 3 方法 | D 🟡 A | 3 |
| T2-05 | PB-20/23 | `BadgeService.grant` + `ReputationEventHandlers.on(UserRegistered/AnswerSubmitted/AnswerEndorsed)` | A | 4 |
| T2-06 | PB-21/22 | `ReputationEventHandlers.on(AnswerAccepted/VoteCast)`（含每日上限、采纳数徽章） | B | 5 |
| T2-07 | PB-24/25 | `BountyService.open/latest/settleOnAccept/expireDue/closeAndRefund` | C | 9 |
| T2-08 | PB-26 | `LeaderboardService.on/top` | D | 4 |
| T2-09 | PB-27 | `ReputationQueryService.profile/ledger`、`ReputationApiImpl.summary/reputations` | A | 3 |
| T2-10 | PB-28 | `ReconciliationService.reconcile` | E | 2 |
| T2-11 | PB-30 | **Reputation 应用层测试**：`ReputationTestFixtures` + 重复事件不重复记账、每日上限、悬赏结算/自答退款/到期退款、对账 | E + 各主责 | 10 |
| T2-12 | PB-18/04 | 移除 Reputation 侧行走骨架；CI 纳入 Reputation；`co-reputation-domain` ≥ 0.70 | E | 1 |
| T2-13 | PB-29 | 前端：悬赏对话框（替换 `prompt()`）+ 倒计时；个人声誉主页（徽章墙、流水分页）；排行榜周期切换 | D | 8 |
| T2-14 | PB-38 | 验证 `demo` profile 可启动（Discovery 监听器仍空实现） | E 🟡 B | 1 |

**S2 验收（教师最爱抽查）**：注册教师可用悬赏积分 1000、学生 0；采纳后数秒内 +15 且有"回答被采纳 +15"流水与"初露锋芒"徽章；自问自答被采纳**不加分**；被赞 +10 / 被踩 −2 / 赞改踩 −12；同日正向投票所得超 200 不计；50 分 7 天悬赏 → 可用 950、冻结 50，采纳后获奖者 +50；**把 `outbox_event.published_at` 置空重放，声誉/徽章/悬赏/排行榜都不能重复变化**。

### Sprint 3（第 6–7 周）· 目标：搜得到、推荐得出、通知到 → `mvn verify` 全绿，v0.9.0(RC)

范围：PB-31 ~ PB-39（9 个 PBI，31 故事点，32 个 TODO 方法）

| 任务 | PBI | 内容 | 负责 | 工时 |
| --- | --- | --- | --- | --- |
| T3-01 | PB-31 | `SearchQuery.terms()` / `terms(String)` | C | 1 |
| T3-02 | PB-33 | `RelatedQuestionPolicy.jaccard/rank` | C 🟡 A | 2 |
| T3-03 | PB-34/35/36 | `MentionParser.extract`、`Notification.create/markRead` | D 🟡 B+D | 2 |
| T3-04 | PB-32 | `SearchIndexProjector` 10 个方法（每个 `on` 先 `firstTime(e)`） | A | 6 |
| T3-05 | PB-31/33 | `DiscoveryQueryService.search/related` | C | 4 |
| T3-06 | PB-34/35 | `NotificationProjector` 8 个方法（自答不通知、"某同学"兜底） | B | 6 |
| T3-07 | PB-36 | `NotificationService` 5 个方法 | D | 3 |
| T3-08 | PB-39 | Discovery 应用层测试：同一事件投两次只生效一次、自己回自己不通知、@不存在昵称忽略、他人不能标记已读 | E | 6 |
| T3-09 | PB-37 | **移除全部行走骨架**，本地与 CI 跑通 `mvn verify`（Testcontainers），恢复完整门禁 | E | 5 |
| T3-10 | PB-37 | 根 POM `domain.coverage.minimum` 改 0.70 并保证通过 | E | 2 |
| T3-11 | PB-31/33/36 | 前端：搜索筛选与排序、相关问题侧栏、通知单条已读、@昵称高亮 | D | 8 |
| T3-12 | PB-38 | `demo` profile 走查演示数据；全员回归 + 缺陷修复 | 全员 | 2×5 |

**S3 验收**：搜"跳表""红黑树"中英文都命中；按标签/未回答/悬赏筛选 + 四种排序生效；单字关键词 422 `DIS-1001`；相关问题显示相似度；删掉的问题不再出现在搜索与推荐；六类通知（新回答/被采纳/被认证/悬赏获奖/徽章/被@）都到、未读数正确、只能标记自己的；CI 全绿 + 覆盖率 ≥70% + 镜像构建成功。

### Sprint 4（第 8 周）· 目标：稳定化 + v1.0.0 发布 + 答辩

范围：PB-40 ~ PB-47（按容量选做，**必选加粗**）

| 任务 | PBI | 内容 | 负责 | 工时 |
| --- | --- | --- | --- | --- |
| **T4-01** | **PB-46** | develop→main 发布 PR、打 v1.0.0、写 Release Notes、验证 `docker compose up -d --build` | E | 3 |
| **T4-02** | **PB-40** | 管理员禁用账号 + 删除该用户全部 Redis 会话 + 测试 + 审计（ADR-003） | A、B | 8 |
| T4-03 | PB-42 | 前端：分页/骨架屏/错误重试、Markdown 实时预览与代码高亮 | D | 6 |
| **T4-04** | **PB-44** | Grafana 面板：发件箱积压、死信、对账不一致、接口延迟 | E | 3 |
| T4-05 | PB-47 | 管理页：待认证学生列表与认证、创建教职工账号 | C | 4 |
| T4-06 | PB-41/43/45 | 个人资料、SSE 通知、列表压测（**仅容量允许时**） | — | — |
| T4-07 | — | 更新 README、补 ADR-010（禁用账号会话清理）、整理用户手册 | C | 3 |
| T4-08 | — | 最终演示彩排（≤15 分钟）+ 项目总回顾 | 全员 | 1×5 |

**PO 决策点**：PB-41（Could）、PB-43（Could，SSE 替换 30 秒轮询）、PB-45（Could，压测）、PB-47（Could）四选二即可。**PB-48 选课审批已明确 Won't（本期）**，不要接。

---

## 4. 分配时必须修正的 5 处（文档原方案的实际漏洞）

### 4.1 硬约束：每人每个开发 Sprint 至少 1 个领域层 PR + 1 个应用层 PR

这是文档 4.1 的硬性要求，也是 9.1 评审会"贡献者维度"的检查项。按原方案逐人核对，**有 8 处不满足**：

| Sprint | 谁缺什么 | 原任务 | 建议调整 |
| --- | --- | --- | --- |
| S1 | **D 无领域层 PR** | 只有 T1-10（应用）+ T1-13（前端） | 把 T1-05 拆开：`Comment.post`（`Comment.java` 单文件）给 D |
| S1 | **E 无领域层 PR** | 只有 T1-11/12/14/15（应用/CI/http） | 把 T1-01 拆开：`MarkdownBody` 给 E，`Tags.names` 留给 A |
| S2 | **A 无领域层 PR** | 只有 T2-05/09（应用） | 把 T2-04（`BadgePolicy`+`AcademicTerm`）给 A |
| S2 | **E 无领域层 PR** | 只有 T2-10/12/14 + 测试 | 把 T2-01（`ReputationRulebook`）给 E |
| S3 | **A 无领域层 PR** | 只有 T3-04（应用） | 把 T3-02（`RelatedQuestionPolicy`）给 A |
| S3 | **B 无领域层 PR** | 只有 T3-06（应用） | 把 T3-03 拆开：`MentionParser.extract` 给 B |
| S3 | **E 无领域层 PR** | 只有 T3-08/09/10 | 把 T3-03 拆开：`Notification.create/markRead` 给 E |
| S4 | 全员 | 1 周 Sprint，主要是发布与打磨 | S4 不算开发 Sprint，但文档要求「每人都有领域层与应用层 PR」是**整学期**口径，S1–S3 已满足即可 |

**拆分原则**：只拆**不同文件**，不要两个人改同一个 `.java`（会产生无谓冲突，也会让 Code Review 变糊）。上表每一处拆分都落在不同文件上。

### 4.2 负载不均（修正前 vs 修正后，单位：理想小时）

| Sprint | A | B(PO) | C | D | E | 合计 |
| --- | --- | --- | --- | --- | --- | --- |
| S0 原方案 | 9.5 | **12.5** | 8.5 | 11.5 | 10.5 | 52.5 |
| S1 原方案 | 18 | 18 | 15 | **20** | 10 | 81 |
| S1 修正后 | 17 | 18 | 14 | 15 | 17 | 81 |
| S2 原方案 | **7** | 10 | 12 | 15 | 14 | 58 |
| S2 修正后 | 10 | 9 | 12 | 12 | 15 | 58 |
| S3 原方案 | **6** | **6** | 7 | 13 | 13 | 45 |
| S3 修正后 | 8 | 7 | 5 | 12 | 14 | 46 |

- **S1 的 D 是瓶颈**（20h，等于上限，还兼前端负责人）→ 必须卸掉约 5h。
- **S2/S3 的 A、B 明显偏轻**（6–7h）→ 正好用来接上面那些领域层方法，同时给 PO 留出评审与演示的时间。
- **S0 的 B 偏重**（1 周 12.5h）→ `T0-07`（导入 48 个 Issue）可让 A 一起做，用 `gh issue create` 脚本批量建，2 人各 1.5h。

### 4.3 PO 的编码量要主动压到均值的 60–70%

你还要做：维护 Backlog 排序、写验收标准、主持计划会、演示、发布 PR、现场展示分支保护、处理教师的退回意见。**B 的编码建议 ≤ 12h/2周**；S2 的 T2-06（5h）+ T2-02（3h）已经够，别再抢新故事。

### 4.4 SM 轮换与实际挑人

| Sprint | SM | 说明 |
| --- | --- | --- |
| S0 | E | 启动会 + 环境排错，E 最合适 |
| S1 | A | 业务流程最重，需要 SM 盯依赖顺序 |
| S2 | C | 幂等与边界测试最需要纪律 |
| S3 | D | 集成联调跨模块最多，D 当前端负责人正好协调 |
| S4 | E | 发布流程 |

### 4.5 4 人组怎么办（不建议按文档"合并 C、D"）

文档说"4 人组将 C、D 合并"，但 C（11h）+ D（16h）= 27h/2周，**远超 20h 上限**，而且前端负责人这个角色会消失。建议改为**按层切**：

| 代号 | 主责 | 必须放弃的内容 |
| --- | --- | --- |
| A | Q&A 领域（Question/Answer/查询） | — |
| B | PO + Reputation 领域与应用 | 少做 1 个领域方法（转 A） |
| C | **前端全部** + Discovery 应用（两个 Projector） | 放弃 T3-05（`DiscoveryQueryService`）→ 并给 A |
| D | Q&A 应用（Command 服务）+ Discovery 领域 + DevOps | 放弃 T3-08 的一部分边界测试 |

并且 S1 范围从 43 点砍到约 34 点：**推迟 PB-14（教师认证）、PB-16（热门标签）、PB-19（前端编辑删除）**——三个都是 Should 级，砍掉不影响闭环演示。

---

## 5. 每个 Sprint 内的开工顺序（依赖驱动的关键路径）

不要平均发任务，按下面顺序发，否则后一个人会"等接口"：

**S1**
```
第 1 天  行走骨架 T1-12（E）──┐  ← 先干这个，否则事件全堵
         T1-01 值对象（A/E）──┴→ 领域方法并行：T1-02(A) T1-03/04(B) T1-05(C/D)
第 3–4 天 领域全绿 → 应用层：T1-07(A) T1-08(B) T1-09(C) T1-10(D/E)
第 5–7 天 前端 T1-13 + T1-06 全员边界测试（谁写完谁补）
第 8–10 天 联调、http 冒烟 T1-15、CI 门禁 T1-14、演示彩排
```
**关键依赖**：`T1-12` 必须先做；`T1-07/08/09/10` 必须在对应领域方法合并后才能开工；`T1-13` 只依赖后端接口，可提前并行。

**S2**：`T2-01/02`（规则+账户）→ `T2-03`（悬赏）→ `T2-05/06`（事件处理器）→ `T2-07/08`（服务）→ `T2-11`（幂等测试，最后 3 天全员一起补）。`T2-12` 移除骨架放在 T2-05/06 合并后立刻做。

**S3**：`T3-01/02/03`（领域，1 天内全部完成）→ `T3-04/05/06/07`（应用层并行）→ `T3-09`（移除骨架 + `mvn verify`，**留足 3 天**，Testcontainers 第一次跑一定会踩坑）→ `T3-11` 前端 → `T3-12` 全员回归。

---

## 6. PO 自己的 7 件事（按时间排）

| 时机 | 动作 | 产出 |
| --- | --- | --- |
| **现在** | **确认教师已建 GitHub 组织 / Secret 团队 / 空私有仓库，并给你 Admin（教师认证要等数天，越早催越好）** | 你能 push 的 `teamNN-campus-overflow` |
| 现在 | 定 5 人名单 → A–E 对应真人 → 建群、发本文档 | `docs/team.md` |
| S0 第 1 天 | 推代码到本组仓库、建 `develop`、配分支保护（含"不允许绕过"）、建 Projects | 仓库 + 看板 |
| S0 第 2 天 | 用附录脚本批量建 48 个 Issue，打 `type:story` `ctx:*` `priority:*` 标签，填 Story Points 与 Sprint | 看板里 48 张卡 |
| S0 第 3 天 | 写 `docs/team-agreement.md`：DoR/DoD、站会时间（周一/三/五）、PR ≤400 行、24h 内评审、分支生命周期 ≤3 天 | 团队约定 |
| 每个 Sprint 第 1 天 | 计划会：定 Sprint Goal（写进 Milestone 描述）→ 只承诺容量的 80% → 任务拆到 ≤1 天 | Sprint Backlog |
| 每周一/三/五 | 15 分钟站会：昨天做了啥/今天做啥/卡在哪；更新看板；有阻碍就建 `status:blocked` Issue | 看板实时一致 |
| 每个 Sprint 最后一天 | 评审会演示（15–20 min，从 `main` 标签启动）→ 回顾会（≤45 min，产出 1–3 条改进行动）→ 发布 PR + 打标签 | `docs/scrum/sprint-N.md` |

**评审会前 10 项自检**（照抄 9.1 节，教师逐项核查）：
1. 从 `main` 版本标签能启动并跑完 Sprint 演示脚本；2. 纳入的 Must 级 PBI 全部满足 DoD；3. `git log` 分布均匀（没有"最后一晚一次性提交"）；4. 每人都有领域层 + 应用层 PR；5. 没有人的贡献低于团队平均 50%；6. 抽 3–5 个 PR：有非作者 Approve、有实质评审意见、关联 Issue；7. 看板状态与实际一致、缺陷用模板登记；8. 分支命名规范、合并后删除、`main` 只接受发布 PR；9. `develop`/`main` 最新提交 CI 全绿、红灯恢复 <24h；10. 燃尽图可解释、回顾改进项已落实或有说明。

---

## 7. 风险台账（PO 要盯的 6 件事）

| 风险 | 信号 | 处置 |
| --- | --- | --- |
| **老师没建 GitHub 组织（还按 V2 的老办法）** | 你只有个人仓库、开不了分支保护 | 立刻找老师确认 V3 的 7.1.1/7.1.2；临时用学生包里的 GitHub Pro 开保护，**不要**把私有仓库改公开 |
| 发件箱被空监听器堵死 | 声誉永不变化，`outbox_event.published_at` 为 null、`last_error` 有 `TODO(S3)` | S1 第一件事做行走骨架；修复后 `UPDATE outbox_event SET attempts=0, locked_until=NULL WHERE published_at IS NULL` |
| Actions 3000 分钟被全组织用完 | 流水线突然不跑，Sprint 验收没 CI 证据 | S0 就做 `concurrency: cancel-in-progress`、集成测试只在 push 跑、镜像只在 main 构建；本地先自检再推。**注意**：指导书 6.1 的"每天至少一次推送"和 8.5 的省配额是冲突的——你该把它改成"每天至少一次有意义提交，推送前先本地跑对应模块 `mvn test`，纯文档提交加 `[skip ci]`"，并写进团队约定 |
| 领域层被 Spring 污染 / 跨上下文直连 | ArchUnit 红（FF-1~FF-6） | S0 全员跑一次 `ArchitectureTest`，把报错当教材；CODEOWNERS 让共享内核改动需两人评审 |
| 幂等没做透，重放导致重复加分 | S2 验收时教师把 `published_at` 置空 | S2 每个事件处理器都写"同一事件调两次"的测试 |
| 前端成为 S1–S3 的固定瓶颈 | D 每周 ≥16h、PR 堆积 | 前端按页面拆给 A/B/C 各 1 个页面，D 只做设计与评审 |
| 有人"只写文档/只写前端" | Insights 里某人 PR 数为 0 | 分配表按 4.1 节硬约束校验；每个 Sprint 计划会现场点一遍 |

**红线（一票否决）**：改已有测试断言、删测试、`@Disabled`；绕过 PR 或关分支保护直接推（包括 PO 用 Admin 权限）；冒用他人提交；提交密钥/真实学号手机号。AI 可以用，但必须在 PR 描述里注明，且能解释自己提交的每一行。

---

## 8. 附录：48 个 PBI 的 Issue 导入清单

字段建议：标题 `[PB-xx] 短标题`，标签 `type:story` + `ctx:*` + `priority:must|should|could`，Story Points 按下表，Milestone 按 **V3 Sprint**。

| Sprint | PBI | 点数 | 核心交付 |
| --- | --- | --- | --- |
| S0 | PB-01~05 | 13 | 仓库/看板/CI 门禁/环境/事件风暴/课程页 |
| S1 | PB-06~19 | 43 | 提问、列表、详情、回答、采纳、投票、编辑、删除、认证、评论、热门标签、QaQueryApi、行走骨架、前端编辑删除 |
| S2 | PB-20~30 | 42 | 账户、+15、投票计分、徽章、悬赏发起与结算、排行榜、档案流水、对账、前端悬赏与声誉页、应用层测试 |
| S3 | PB-31~39 | 31 | 搜索、搜索投影、相关推荐、通知、@提及、通知中心、端到端 + 覆盖率、demo 数据、投影测试 |
| S4 | PB-40~47 | 26 | 禁用账号、个人资料、前端体验、SSE、Grafana、压测、一键部署、管理页 |
| Won't | PB-48 | 8 | 选课审批（本期不做，留在 Backlog 里做"排除项"证据） |

```bash
# 批量导入（先准备 backlog.csv：id,title,labels,points,sprint）
while IFS=, read -r id title labels points sprint; do
  gh issue create --title "[$id] $title" --label "type:story,$labels" \
      --milestone "$sprint" --body-file .github/ISSUE_TEMPLATE/story.md
done < backlog.csv
# 再在 Projects 表格视图里按 Rank 拖排序、补 Story Points 字段
```

---

## 9. 一句话总结该怎么分

> **按上下文定主责，按层补 PR，按依赖排开工顺序，按容量砍 Should。**
> 每次发包前问三句：①这个人这个 Sprint 有领域层 PR 吗？②这个人这周是不是超过 20 小时了？③他等的接口今天有人在做吗？
