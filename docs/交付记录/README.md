# 交付记录

> 一个人一个文件夹，**一张看板卡一个子文件夹**。截图、录屏、导出的报告都往对应卡片的文件夹里放。

## 为什么要有这个目录

| 原因 | 说明 |
|---|---|
| 答辩要抽查 | 老师会指着某条验收标准问「你怎么证明做到了」，有图当场能翻 |
| 评审要看证据 | 评审人不用翻 CI 日志，打开对应文件夹就知道你跑了什么 |
| 最终交付物要用 | 指导书 §9.4 要求用户手册配真实截图、演示视频、测试报告 —— 素材都从这里取 |

## 目录结构

```
docs/交付记录/
├── Bugmageddon/
│   ├── T4-09-quality-gates/          # Issue #68
│   ├── T4-10-release-v1/          # Issue #69
│   ├── T4-11-grafana/          # Issue #70
│   ├── T4-12-presentation/          # Issue #71
│   ├── T4-ENV-docker-env/          # Issue #79
├── qwerbnmv/
│   ├── T4-01-discovery-domain/          # Issue #60
│   ├── T4-02-discovery-query/          # Issue #61
│   ├── T4-03-admin-disable-user/          # Issue #62
│   ├── T4-04-frontend-ux/          # Issue #63
│   ├── T4-05-frontend-admin-profile/          # Issue #64
│   ├── T4-06-sse-and-loadtest/          # Issue #65
│   ├── T4-ENV-docker-env/          # Issue #76
├── kl1h/
│   ├── T4-07-search-index-projector/          # Issue #66
│   ├── T4-08-notification-projector/          # Issue #67
│   ├── T4-ENV-docker-env/          # Issue #77
├── Tourbillion-ANG/
│   ├── T4-13-scrum-records/          # Issue #72
│   ├── T4-14-ddd-docs/          # Issue #73
│   ├── T4-15-readme-manual/          # Issue #74
│   ├── T4-16-adr-011-error-codes/          # Issue #75
│   ├── T4-ENV-docker-env/          # Issue #78
│   ├── T4-FIX-deploy-pipeline/          # Issue #80
└── README.md                  ← 本文件
```

## 往文件夹里放什么

| 类型 | 命名建议 | 例子 |
|---|---|---|
| 截图 | `序号-说明.png` | `01-hello-world.png`、`03-prometheus-targets-up.png` |
| 终端输出 | `序号-说明.txt` | `02-mvn-test.txt`、`05-flyway-history.txt` |
| 录屏 | `说明.mp4` / `.gif` | `grafana-dashboard.mp4` |
| 导出的报告 | 原名即可 | `coverage-report.html`、`load-test-result.txt` |

**命名统一用英文 + 数字前缀**，中文文件名在 Windows / Linux 之间来回传容易乱码。

## 四条规则

| 规则 | 说明 |
|---|---|
| 只放自己的 | 不要动别人的文件夹，冲突了不好解 |
| 一个文件夹只放这张卡的东西 | 不要把别的卡的截图混进来 |
| 验收标准里点名的截图一张不能少 | 缺哪张就是哪条没做到 |
| 图别太大 | 单张控制在 500 KB 以内（截图工具一般能压），仓库是 public 的 |

## 卡片清单

### Bugmageddon（5 张，进行中 4 张）

| 卡片 | 标题 | 文件夹 | 状态 |
|---|---|---|---|
| [#68](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/68) | 质量门禁收口：恢复全量 / 覆盖率 0.70 / 集成测试硬门禁 | [T4-09-quality-gates/](Bugmageddon/T4-09-quality-gates/) | ✅ 已完成 |
| [#69](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/69) | 一键部署与 v1.0.0 发布（含测试报告） | [T4-10-release-v1/](Bugmageddon/T4-10-release-v1/) | ⏳ 进行中 |
| [#70](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/70) | Grafana 可观测面板（PB-44） | [T4-11-grafana/](Bugmageddon/T4-11-grafana/) | ⏳ 进行中 |
| [#71](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/71) | 答辩物料：15 分钟 PPT + ≤5 分钟演示视频 | [T4-12-presentation/](Bugmageddon/T4-12-presentation/) | ⏳ 进行中 |
| [#79](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/79) | Docker 环境统一：Bugmageddon（含可观测栈验证） | [T4-ENV-docker-env/](Bugmageddon/T4-ENV-docker-env/) | ⏳ 进行中 |

### qwerbnmv（7 张，进行中 7 张）

| 卡片 | 标题 | 文件夹 | 状态 |
|---|---|---|---|
| [#60](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/60) | Discovery 领域层：SearchQuery / RelatedQuestionPolicy / MentionParser / Notification（7 个方法） | [T4-01-discovery-domain/](qwerbnmv/T4-01-discovery-domain/) | ⏳ 进行中 |
| [#61](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/61) | Discovery 查询用例与应用层测试体系（search / related + PB-39） | [T4-02-discovery-query/](qwerbnmv/T4-02-discovery-query/) | ⏳ 进行中 |
| [#62](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/62) | 管理员禁用账号与会话清理（PB-40） | [T4-03-admin-disable-user/](qwerbnmv/T4-03-admin-disable-user/) | ⏳ 进行中 |
| [#63](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/63) | 前端体验升级：分页 / 骨架屏 / 错误重试 / Markdown 预览（PB-42） | [T4-04-frontend-ux/](qwerbnmv/T4-04-frontend-ux/) | ⏳ 进行中 |
| [#64](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/64) | 前端页面补齐：管理页 + 个人资料页（PB-47 / PB-41） | [T4-05-frontend-admin-profile/](qwerbnmv/T4-05-frontend-admin-profile/) | ⏳ 进行中 |
| [#65](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/65) | 可选增强：通知红点 SSE + 问题列表压测（PB-43 / PB-45） | [T4-06-sse-and-loadtest/](qwerbnmv/T4-06-sse-and-loadtest/) | ⏳ 进行中 |
| [#76](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/76) | Docker 环境统一：qwerbnmv（含基线报告与自检脚本） | [T4-ENV-docker-env/](qwerbnmv/T4-ENV-docker-env/) | ⏳ 进行中 |

### kl1h（3 张，进行中 3 张）

| 卡片 | 标题 | 文件夹 | 状态 |
|---|---|---|---|
| [#66](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/66) | 搜索投影：SearchIndexProjector 全部 10 个方法 | [T4-07-search-index-projector/](kl1h/T4-07-search-index-projector/) | ⏳ 进行中 |
| [#67](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/67) | 通知投影与通知用例：NotificationProjector + NotificationService（13 个方法） | [T4-08-notification-projector/](kl1h/T4-08-notification-projector/) | ⏳ 进行中 |
| [#77](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/77) | Docker 环境统一：kl1h（含中文全文检索地基验证） | [T4-ENV-docker-env/](kl1h/T4-ENV-docker-env/) | ⏳ 进行中 |

### Tourbillion-ANG（6 张，进行中 5 张）

| 卡片 | 标题 | 文件夹 | 状态 |
|---|---|---|---|
| [#72](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/72) | Scrum 记录：sprint-0 至 sprint-4 五份 | [T4-13-scrum-records/](Tourbillion-ANG/T4-13-scrum-records/) | ⏳ 进行中 |
| [#73](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/73) | DDD 文档补全：上下文映射 / 事件目录 / 领域模型 | [T4-14-ddd-docs/](Tourbillion-ANG/T4-14-ddd-docs/) | ⏳ 进行中 |
| [#74](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/74) | README 重写 + 用户手册 + docs/demo.md | [T4-15-readme-manual/](Tourbillion-ANG/T4-15-readme-manual/) | ⏳ 进行中 |
| [#75](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/75) | ADR-011 禁用账号会话清理 + 错误码对照表维护 | [T4-16-adr-011-error-codes/](Tourbillion-ANG/T4-16-adr-011-error-codes/) | ⏳ 进行中 |
| [#78](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/78) | Docker 环境统一：Tourbillion-ANG（含用户手册截图素材） | [T4-ENV-docker-env/](Tourbillion-ANG/T4-ENV-docker-env/) | ⏳ 进行中 |
| [#80](https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow/issues/80) | 容器部署链路修复：jar 名 + Cookie Secure + dockerignore + nginx 解析 + npm ci | [T4-FIX-deploy-pipeline/](Tourbillion-ANG/T4-FIX-deploy-pipeline/) | ✅ 已完成 |

## 哪些卡需要截图

不是每张卡都有图，纯代码的卡（#60–#67）主要是终端输出和测试报告：

| 卡 | 要什么 |
|---|---|
| #60–#67 | 终端测试输出 `.txt`（`mvn test` 的结果） |
| #69 | `docker compose up -d --build` 的终端输出 + Release 页面截图 + 覆盖率报告 |
| #70 | 6 个 Grafana 面板截图 + 面板 JSON |
| #71 | PPT 与视频直接放这里（或放 `docs/final/`） |
| #72–#75 | 主要是文档本身，图可选 |
| #76–#79 | **验收标准点名了截图，一张不能少** |
| #80 | 8 项验证的终端输出 + 登录前后 cookie 截图 |

## 其它交付物放在哪

| 类型 | 位置 | 归属卡 |
|---|---|---|
| 用户手册要用的图 | 本目录 `#78/T4-ENV-docker-env/` | #78 |
| 测试报告 / 覆盖率 / 压测 | `docs/reports/` | #69 / #65 |
| 答辩 PPT 与视频 | `docs/final/` | #71 |
| Scrum 记录 | `docs/scrum/` | #72 |
| DDD 文档 | `docs/ddd/` | #73 |
| ADR | `docs/adr/` | #75 |
| 各人进度（旧机制，保留） | `docs/progress/` | — |

> `docs/progress/<用户名>.md` 是 Sprint 1–3 的机制（CI 会检查代码类 PR 是否更新）。Sprint 4 起**新增**本目录，两者并存：`progress` 记「我在做什么」，`交付记录` 记「这张卡我怎么证明做完了」。


