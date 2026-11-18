# CampusOverflow · 项目约定（AGENTS.md）

> 本文件**提交到仓库**，任何人和任何 AI 助手都应先读它。
> **本文件是唯一的 AI 规则文件**（团队统一用 DeepSeek Harness 与 Kimi，不再维护其他工具的入口）。
> 修改本文件必须走 PR。

## 1. 项目是什么

- 《软件工程》课程贯穿项目：**CampusOverflow 校园问答平台**（校园版 Stack Overflow）。
- 后端 Java 21 + Spring Boot 3.4，模块化单体 + 六边形分层，4 个限界上下文 × 5 层 = 27 个 Maven 模块；前端 React 18 + TypeScript + Vite。
- 起始代码**只完整实现了 Identity**；Q&A / Reputation / Discovery 共 107 处待实现（起始状态，按 `throw` 行计），**现在剩 14 处，全部在 Discovery**——Identity 0、Q&A 0、Reputation 0（前两个上下文已全部实现并纳入 CI 门禁）。
- 查当前数量用这条，别凭记忆（Windows 上没有 `grep`，用 `git grep`，`git` 自带、跨平台）：
  `git grep -n 'throw new UnsupportedOperationException("TODO' -- 'backend/*.java'`
- **5 个测试当前为红**，全部在 `DiscoveryDomainTest`——**测试即需求**，先读测试再写实现。这 5 个覆盖 `SearchQuery.terms`、`RelatedQuestionPolicy.jaccard`、`MentionParser.extract`、`Notification.create` / `markRead`，即批次 4 的内容。其余 **144 个单元测试全绿**（含 `ArchitectureTest` 的 1 个 `@ArchTest`）；另有 `CampusOverflowFlowIT` 3 个集成测试，`mvn test` 不跑，要 `mvn verify` 且需要 Docker。
- 权威需求：`CampusOverflow-实验指导书V3.pdf`（README 里的 15 周计划已废弃）。
- **编号错位陷阱**：代码里的 `TODO(S2)` 对应 **V3 Sprint 1**；`TODO(S3)` 拆成 **Sprint 2（Reputation）+ Sprint 3（Discovery）**。建 Issue / Milestone 时按 V3 填。

## 2. 时间与目标

- 周期：**2026-11-05 → 2026-12-05（30 天，硬截止）**。
- 指导书按 8 周 / 5 Sprint / 5 人设计；本组约 4 周 / 4 人，容量约为原设计的 **43%**。
- 完成定义：**可演示的端到端闭环 + 质量门禁全绿**。范围按 Must → Should → Could 排序。
- 目标优先级：① 实验完整做完并能答辩；② 团队真正学会 GitHub。

## 3. 团队与分工

| 角色 | 姓名 | GitHub | 主责 |
| --- | --- | --- | --- |
| PO + SM | Bugmageddon | Bugmageddon | 看板、任务分配、评审、演示与最终答辩；CI / 文档 / 门禁 |
| 技术负责人 | 代一鸣 | qwerbnmv | Q&A 与 Reputation 核心领域，技术兜底 |
| 后端开发 | 谢守航 | kl1h | 应用层服务 + Discovery |
| 文档 / 物料 | 王子昂 | Tourbillion-ANG | **不写 Java、不做前端**：ADR、词汇表、错误码表、演示物料（边界清晰、量大、可独立完成） |

- 4 人对应 V3 的 A–E 需要合并：E（质量与 DevOps）由 PO 与技术负责人分担。
- **前端代码已基本完成**：6 个页面（首页 / 问题详情 / 提问 / 登录 / 排行榜 / 通知）全部真实调用 `api.ts`，零 TODO、零 mock。**唯一缺口**：`api.ts:78` 有 `reputation()` 方法、`types.ts` 有 `ReputationProfile`，但没有页面调用它，`App.tsx` 也没有 `/users/:id` 路由——这是批次 3 `#50` 的活。「前端开发」不是待办任务，最多只能做「联调验收 + 补这一页」。

## 4. 开工前必做（四步）

1. 读 **`docs/status.md`**：当前批次、门禁状态。
2. 读 **`docs/progress/<你的 GitHub 用户名>.md`**：本人的当前任务。
3. `git switch develop && git pull`，然后新建分支再动手。
4. **把看板上对应的卡片从 `Ready` 拖到 `进行中`**。
   AI 无法直接操作看板，**必须提醒用户手动拖**；不拖就等于没告诉任何人"我开始做了"，别人可能重复认领同一张卡。

## 5. 完工后必做（三步）

1. 更新**你自己**的 `docs/progress/<用户名>.md`（只改自己的）。
2. PR 描述写 `Closes #<Issue号>`。
3. 勾选 PR 模板里的「已更新 progress」。

> **不是靠自觉**：CI 有「进度文件检查」，代码类 PR 未更新本人 progress 会直接失败；纯文档 PR 自动跳过。
> `docs/status.md` 只有 PO 能改。

## 6. 硬性要求

- **动手前先说明**要做什么、为什么、影响哪些文件。
- **不直接推送** `main` / `develop`；一律「分支 → PR → 评审 → 合并」。
- **不修改已有测试断言、不删除测试、不加 `@Disabled`**。
- 每个 PR 必须关联至少 1 个 Issue（`Closes #号`）。
- 禁止手动关闭「已完成」任务的 Issue，只能由 PR 合并自动关闭（例外：范围裁剪 / 重复 / 超时，须写原因）。
- 不提交 `target/`、`node_modules/`、`.idea/`、`.env`、真实密钥。
- **提交前先本地跑过对应模块的测试**，不要用 CI 代替自检。
- 使用 AI 辅助须在 PR 描述中注明使用范围，并保证本人能解释每一行提交的代码。

## 7. 技术红线

1. **领域层零框架依赖**：`*.domain` 包内不得 import Spring / JPA / Jackson / Swagger；ArchUnit FF-1~FF-6 必须通过。
2. **跨上下文只经 `*-api` 或集成事件**：不得 import 其他上下文的 `domain` / `application` 类。
3. **写操作在 `@Transactional` 内**；事件在聚合保存**之后**、**同一事务**内发布（事务性发件箱）。
4. **消费端必须幂等**：同一事件重复投递不得重复记账（账本唯一键 `(event_id, user_id, reason)`、`processed_event` 表、通知唯一键 `(source_event_id, recipient_id)`）。
5. **错误码**：形如 `QA-1013`——前缀表示上下文（`QA-` / `REP-` / `DIS-` / `ID-` / `AUTH-` / `SYS-`），首位数字表示异常族：`1xxx`=业务规则 422、`2xxx`=冲突 409、`3xxx`=禁止 403、`4xxx`=未找到 404。新增错误码前先 `git grep -n '"QA-10' -- 'backend/*.java'` 查现有代码避免重号；已出现的码以代码为准（`docs/错误码对照表.md` 尚未建立，见 `docs/tech-debt.md` 的 C5 与 Issue #32）。
6. **计数器只用 `adjustXxx` 原子更新**，领域对象不回写 `score` / `answerCount` / `viewCount`。
7. **渐进门禁只能「缩小运行范围」**，不得用 `-Dmaven.test.failure.ignore`、`@Disabled`、删测试来变绿。

## 8. 分支与提交

- 分支：`feat/xxx`、`fix/xxx`、`docs/xxx`、`ci/xxx`；一个任务一个分支，≤3 天。
- 提交：`<type>(<scope>): 中文简述`，type ∈ `feat|fix|test|docs|ci|chore`。
- PR：≤400 行，关联 Issue，附测试证据。
- **PR 右侧栏 4 项必须设置**：Labels（`ctx:*` + `layer:*`）、Milestone（当前 Sprint）、Assignees（自己）、Reviewers（≥1 名非作者）。缺一项按「未完成」处理——不填 Milestone 会让燃尽图漏卡，不填 Labels 会让贡献分布统计不出来。
- **PR 的 Projects 一栏保持 None**：看板只放 Issue（一张 Issue = 一张卡），PR 挂进去会让看板被代码提交淹没。PR 与 Issue 的关联靠正文 `Closes #N`，卡片状态由自动化跟着 Issue 走。
- **Issue 只能由 PR 合并自动关闭**（正文写 `Closes #N`），**严禁手动关 Issue**——手动关会提前用掉看板的「Item closed → 完成」事件，PR 合并后卡片会永远卡在「待评审」。

## 9. 排期方式（队列 + 门禁，不按天）

- 看板四列：`Ready → 进行中 → 待评审 → 完成`；**每人同时只做 1 个**，做完自己拉下一个。
- 进度用门禁衡量：

| 批次 | 门禁 | 内容 |
| --- | --- | --- |
| 1 | 无 | 仓库设置、环境、Q&A 领域 6 个类、支持性文档 |
| 2 | Q&A 领域测试全绿 | **9 张卡（Issue #24–#32）**：监听器空实现、三个命令服务、两个查询服务、CI 纳入 `co-qa-application`、ADR、错误码表。**已完成 8/9**——`#24`~`#31` 全部合并，`#32` 错误码表因当时 `REP-*` 尚未实现而挪到批次 3 |
| 3 | Reputation 测试全绿（已达成：domain 15/15 + application 27/27，均已纳入 CI 门禁） | **8 张卡（Issue #45–#51 + #32）**：声誉领域与事件记账、悬赏闭环、查询/排行榜/对账、CI 纳入 `co-reputation`、前端联调验收、错误码对照表、演示脚本与 ADR-010 |
| 4 | 端到端可跑（`mvn verify` + `CampusOverflowFlowIT` 绿） | **Discovery（剩 14 个 TODO）**：搜索 / 相关推荐 / 通知 / @提醒；清库重建 + 全链路走查 |
| 5 | 可演示可交付 | 发布 v1.0.0、Grafana 面板、README 与收尾、答辩彩排 |

> 批次 3 与批次 4 的分界已按实际执行调整：原表把 Reputation 排在批次 4，2026-11-15 排 Sprint 3 时把 Reputation 提前到批次 3，Discovery 顺延到批次 4。

## 10. 文档索引

| 文件 | 内容 |
| --- | --- |
| `docs/status.md` | 进度总览（只有 PO 改） |
| `docs/progress/` | 每人一份进度（本人改） |
| `docs/team-agreement.md` | DoR / DoD / 评审归属 / 红线 |
| `docs/adr/` | 架构决策记录 |
| `docs/tech-debt.md` | **技术债与待办**：什么现在不能做、什么时候做 |
| `docs/开发者须知.md` | 环境版本、连接参数、哪些测试必须绿、教师账号、常见报错 |
| `docs/git代理切换.md` | 宿舍直连 / 教室代理切换 |
| `docs/archive/` | **历史参考，不再维护**：V2/V3 差异比对、逐 Sprint 分配方案 |

## 11. 为什么易变内容不放这里

进度、谁在做什么、当前卡点 → `docs/status.md` 与 `docs/progress/`。

原因：本文件是「会话基线快照」，外部修改不会立刻生效；而 `read` 永远读到最新磁盘内容。
**快照放规则，`read` 取实时。**

## 12. 常用命令（提交前自查用）

```bash
# 跑单个模块的测试（-am 会一起构建它依赖的模块）
mvn -B -f backend/pom.xml test -pl co-qa/co-qa-domain -am

# 架构约束检查（必须始终通过）
mvn -B -f backend/pom.xml test -pl co-bootstrap -am -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false

# 全量单元测试（当前只剩 Discovery 的 5 个红，作为基线参考）
mvn -B -f backend/pom.xml test

# 集成测试（需要 Docker；会跑 CampusOverflowFlowIT，Discovery 未完成前必红）
mvn -B -f backend/pom.xml verify

# 前端类型检查 + 构建（CI 会跑这两条）
cd frontend && npm run typecheck && npm run build

# 本地启动后端（需要本机 MySQL + Redis）
mvn -B -f backend/pom.xml spring-boot:run -pl co-bootstrap -Dspring-boot.run.profiles=dev
```

## 13. 事件与发件箱（最容易踩的坑）

应用服务调用 `publish()` 时，事件只是写进 `outbox_event` 表；由 `OutboxRelay` 每 500ms 取出、按 id 顺序投递给所有 `@EventListener`。

**关键约束**：任一监听器抛异常 → 本条事件记为失败、**并阻塞本批后续事件**（保序）。

因此：

1. **还没进入当前批次的上下文，其监听器必须先写成空实现**（只打 DEBUG 日志），否则整条事件链会被堵死，表现为"声誉永远不变化"。
2. 每个空实现都要能被后来的人一眼认出来：**现在只剩两个类**——`NotificationProjector` / `SearchIndexProjector`（都在 `co-discovery-application`），类注释里有「批次 2 临时状态」段落，并指向各方法上的 `TODO(S3)`。**这些注释里没有 Issue 编号**——想找对应的技术债，查 `docs/tech-debt.md`。`ReputationEventHandlers` 已在批次 3 实现完毕（`#46`），不再是空实现。
3. 进入实现阶段后需要重放事件：

```sql
UPDATE outbox_event SET attempts = 0, locked_until = NULL WHERE published_at IS NULL;
-- 或本地清库重建：docker compose down -v
```

4. 排查"声誉不变化"的顺序：先看 `outbox_event.published_at` 与 `last_error`，再看 `rep_ledger` 有没有流水。
