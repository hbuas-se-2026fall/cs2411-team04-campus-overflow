# #78 交付记录：Docker 环境 + 用户手册截图素材

> 采集时间：2026-11-26
> 环境：Windows 11 + Docker Desktop 29.9.3（WSL2 后端）+ Chrome
> 访问地址：`http://localhost`（容器版，非 5173 开发服务器）
> 演示数据：`docker compose down -v` 后重建，`demo` profile 自动播种

## 一、环境验收

| 项 | 结果 | 证据 |
| --- | --- | --- |
| `docker run --rm hello-world` | `Hello from Docker!` | `01-hello-world.png` |
| `docker compose ps` | co-mysql / co-redis / co-backend 均 healthy，co-web Up | `02-docker-ps.png` |
| 后端健康 | `GET /actuator/health/readiness` → `{"status":"UP"}` | 见 PR |
| 前端 | `http://localhost` → 200，含 `<div id="root">` | 见 PR |

## 二、截图清单

| 文件 | 操作 | 账号 | 说明 |
| --- | --- | --- | --- |
| `01-hello-world.png` | `docker run --rm hello-world` | — | 引擎可用性 |
| `02-docker-ps.png` | `docker compose ps` | — | 四容器状态 |
| `login-1.png` | 打开登录页 | 未登录 | `localhost/login`，登录/注册两个 tab |
| `login-2.png` | 登录后首页 | `2023001` 小明 | 三条种子问题 + 热门标签 |
| `ask-1.png` | 填写提问表单 | `2023001` 小明 | 标题/正文/标签/课程 CS2001 已填，未提交 |
| `ask-2.png` | 提交后详情页 | `2023001` 小明 | `/questions/4`，0 个回答 |
| `answer-1.png` | 写回答 | `2023002` 小红 | 回答框已填，未提交（「提交回答」按钮在视口下方） |
| `vote-1.png` | 给回答点赞 | `2023003` 小刚 | 分数 0 → 1 |
| `bounty-1.png` | 发起悬赏 | `T2001` 王老师 | 原生 prompt，`悬赏积分（10–500）`，填 50 |
| `reputation-1.png` | 声誉页下半部分 | `2023002` 小红 | 4 个徽章 + 6 条流水（+50 赢得悬赏 / +15 被采纳 / +10 被投票） |
| `reputation-2.png` | 声誉页上半部分 | `2023002` 小红 | 声誉值 110，可用/冻结积分 0/0 |
| `leaderboard-1.png` | 排行榜「本周」 | `2023002` 小红 | 小红 110 / 小明 10 |
| `leaderboard-2.png` | 排行榜「本月」 | `2023002` 小红 | 同上 |
| `leaderboard-3.png` | 排行榜「本学期」 | `2023002` 小红 | 同上 |
| `leaderboard-4.png` | 排行榜「总榜」 | `2023002` 小红 | 6 人：小红 110 / 小明 10 / 其余 0 |
| `notification-1.png` | 通知列表 | — | **待补**：等 `#67`（NotificationService）合并 |

统计：**15 张 PNG，合计 2.44 MB**（单张 ≤ 331 KB，宽 1600px）。

## 三、与现有文档的差异（重要）

跑真实环境时发现以下与 `docs/demo-声誉.md` / README 描述不一致的地方，写用户手册（#74）时必须按**实际**写：

| # | 文档说法 | 实际行为 |
| --- | --- | --- |
| 1 | 课程显示 `CS2001 数据结构（2026-秋）` | 课程下拉只显示 **`CS2001 数据结构`**，没有学期 |
| 2 | 详情页右侧有「相关问题」推荐 | 恒为 **「暂无」** —— `/questions/{id}/related` 仍是 TODO（`#66`/`#61` 未完成） |
| 3 | 悬赏对话框有「积分 + 天数」两个输入 | 实际是浏览器原生 `prompt`，**只能填积分**，天数固定 7 天（`QuestionDetailPage.tsx:54`） |
| 4 | ——（文档未提及） | 「**发起悬赏**」与「**删除问题**」两个链接**紧挨着渲染**（`发起悬赏删除问题`），本次误点触发过一次删除确认框，易误操作 |
| 5 | 导航有「通知」入口 | 页面存在但**后端未实现**，点击会报错（`#67` 未合并） |
| 6 | 声誉页只展示「声誉 + 徽章 + 流水」 | 实际还展示「可用悬赏积分 / 冻结中」表格（仅本人与管理员可见） |
| 7 | 悬赏以积分入账 | 实际以**声誉**入账：小红声誉 110，可用悬赏积分仍为 0（与 ADR-010 一致，文档需写清） |
| 8 | 排行榜按周期区分 | 「本周 / 本月 / 本学期 / 总榜」在当前演示数据下**结果相同**（数据都在今天产生，无法体现差异） |

### 补充说明

- `answer-1.png` 未包含「提交回答」按钮：按钮在视口下方，滚动后可见；截图保留回答框内容与地址栏。
- 排行榜四个周期结果相同是**数据时间集中**导致的正常表现，不是缺陷。
- 声誉流水里 `+10 / +15` 各有两条：一条来自种子数据（问题 #1 的回答），一条来自本次演示（问题 #4 的回答）。

## 四、复现步骤

```powershell
cd D:\dev\cs2411-team04-campus-overflow
docker compose -f deploy/docker-compose.yml down -v
docker compose -f deploy/docker-compose.yml up -d
# 等 co-backend 变 healthy（约 40s）
docker compose -f deploy/docker-compose.yml ps
# 浏览器打开 http://localhost，按上表账号操作
```

> 本机 Windows 的 6379 落在系统保留端口段（6366–6465）时，`up` 会报 `bind: ... access permissions`，
> 需管理员执行 `net stop winnat` → `up -d` → `net start winnat`。
