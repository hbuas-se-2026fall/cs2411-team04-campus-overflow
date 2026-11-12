# PO（Bugmageddon）的进度

最后更新：2026-11-12
当前任务：`#30` CI 纳入 `co-qa-application`（等待 #25–#29 全合并）
状态：等待
卡在哪：无
下一步：
- 评审并合并 PR #33（`#27`，谢守航）
- 催三位开发把 `Ready` 卡的拖到 `进行中`
- 等 `#25–#29` 合并后做 `#30`

## 已完成

- **2026-11-06**：仓库初始化、分支保护、默认分支改 develop
- **2026-11-06**：进度看板体系（`docs/status.md` + `docs/progress/`）
- **2026-11-06**：CI 渐进式质量门禁（ADR-009）+ 进度文件检查
- **2026-11-06**：修复 5 个乱码文件名
- **2026-11-06**：全工具 AI 约定（AGENTS.md / CLAUDE.md / 6 个工具入口）
- **2026-11-06**：Issue 模板、团队约定、批次 1 的 10 张任务卡、项目看板
- **2026-11-07**：`#4` 实现 `MarkdownBody` 与 `Tags.names`，新增 `MarkdownBodyTest`（PR #15）
- **2026-11-09**：评审 PR #16（`#5`）、#18（`#7`/`#8`）、#19（`#6`），提出 endorse 双重判断、NPE 拆箱、checkValue 口径三处修改
- **2026-11-09**：**批次 1 门禁达成** —— `co-qa-domain` 21/21 全绿
- **2026-11-09**：CI 覆盖范围加入 `co-qa/co-qa-domain`
- **2026-11-09**：`#9` 领域边界测试完成 —— 新增 `QuestionBoundaryTest`(13) / `AnswerBoundaryTest`(11) / `VoteBoundaryTest`(8) 共 32 个用例，`co-qa-domain` 从 21 个涨到 53 个，全绿；原有 4 个测试类零改动
- **2026-11-09**：对新增测试做变异验证（3 处：标题下界 10→11、去掉 `id == null` 保护、投票校验换序），三处都被捕获，确认测试有真实约束力
- **2026-11-11**：建批次 2 的 9 张任务卡（#24–#32）并分配到人，全部挂 `Sprint 2` 里程碑
- **2026-11-12**：**修复 CI 全面停摆**。现象：11-12 起所有 job `steps=0`、3 秒内失败，注释为 `The job was not started because recent account payments have failed or your spending limit needs to be increased`。定位：不是代码/配置问题，是私有仓库 + 组织额度/账单（同组织 public 仓库 team08 的 CI 正常）。核对用量：team04 从 11-01 至今 154 个 job、累计 63.49 分钟，远未触及 3000 分钟额度；组织下 20 个私有仓库、97 个席位共享额度。处理：仓库改为 public 后 CI 恢复，`run 43141289402` attempt 3 全绿（后端 58s / 前端 20s / 进度检查 6s）
- **2026-11-12**：核实两位成员提出的问题。谢守航的 CI 诊断成立，但他有 3 处误判（① 误以为 PO 是组织 Owner——实为 member，Owner 只有 `BostonMIT`；② 误以为 3000 分钟被本队跑光——实际 63.49 分钟；③ 不必等 CI 恢复才能合并——`protect-develop` 规则集没有 `required_status_checks`）。王子昂对 #31 的 3 条核查全部成立
- **2026-11-12**：修正 3 张 Issue 的事实性错误 —— #24 补充方法数口径（23 处 = 20 个 `@EventListener` + 3 个私有辅助，与卡面「23 个方法」吻合，原先误记为 25 处）；#28 修正 `JdbcQaReadModel` 路径（`infrastructure/query/` 而非 `persistence/`）；#31 修正 discovery SQL 路径、`deleted_at` 字段数（2 个而非「5 处」，第 17/18/48 行是索引）、`@ArchTest` 个数（8 个而非 10 个），并把 ADR-006 标题改为「问题与回答软删除，投票撤销即删除」（评论无删除路径、投票撤销即物理删除）
