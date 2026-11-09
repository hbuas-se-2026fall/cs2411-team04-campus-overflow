## 关联 Issue

Closes #

## 改动说明

<!-- 做了什么、为什么这样做；涉及哪个上下文与哪一层 -->

## 测试证据

<!-- 粘贴 mvn test 输出摘要或截图；前端改动附截图 -->

## 右侧栏元数据（开 PR 后立刻设置，缺一项都不算完成）

| 字段 | 怎么填 | 不填的后果 |
| --- | --- | --- |
| **Labels** | 至少一个 `ctx:*`（qa / reputation / discovery / docs / infra / frontend）+ 一个 `layer:*`（domain / application / frontend） | 无法统计各上下文贡献分布，评审会「贡献者维度」查不出来 |
| **Milestone** | 当前 Sprint（Sprint 1 / 2 / 3 / 4） | 燃尽图漏掉这张卡，答辩时拿不出进度证据 |
| **Assignees** | 点 `assign yourself` | 看板上不知道是谁的活 |
| **Reviewers** | 至少指定 1 名非作者 | PR 躺着没人看，24h 内无人评审 |

> **Projects 一栏不要填 PR。** 看板只放 Issue（一张 Issue = 一张卡）。把 PR 挂进去会让看板被「代码提交」淹没，看不出还剩多少活。PR 与 Issue 的关联靠正文的 `Closes #N`，看板卡片会自动跟着 Issue 的状态走。

- [ ] 以上 4 项已设置（**Projects 保持 None**）
- [ ] 正文的 `Closes #` 已填写（合并后自动关闭 Issue，**不要手动关**）

## 自检清单

- [ ] 已更新**我自己**的 `docs/progress/<用户名>.md`（状态、卡点、下一步）
- [ ] 本地 `mvn test -pl <模块> -am` 通过
- [ ] `ArchitectureTest` 通过
- [ ] 新增业务规则有对应单元测试，且**未修改任何已有断言**
- [ ] 异常类型与错误码符合 `docs/` 约定
- [ ] 前端改动已通过 `npm run typecheck && npm run build`
- [ ] 未提交 `target/`、`node_modules/`、`.idea/`、密钥
- [ ] 使用了 AI 辅助：否 / 是（说明：）

<!-- 评审人请按 AGENTS.md 第 6、7 节（硬性要求与技术红线）逐条检查 -->
