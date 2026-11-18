# 声誉演示脚本

> 用途：批次 4 验收 / 答辩现场演示。照着念即可 —— 每一步都写了「怎么操作 / 看到什么 / 数字应该变成多少」。
> 所有数字都能在代码里找到出处，见第 5 节「数字速查表」。
> 前置：`develop` 分支且批次 4 的声誉实现已合并（`#45`–`#48`）。

---

## 1. 准备

### 1.1 起环境

```powershell
# 1) MySQL + Redis
docker compose -f deploy/docker-compose.yml up -d mysql redis

# 2) 用 demo profile 起后端：自动写入 1 门课、6 个账号、3 个问题、1 个已有悬赏
mvn -B -f backend/pom.xml spring-boot:run -pl co-bootstrap -Dspring-boot.run.profiles=demo
```

浏览器打开 `http://localhost:5173`（前端 dev）或 `http://localhost:8080`（打包部署）。
API 文档：`http://localhost:8080/swagger-ui.html`。

> `demo` profile 启动失败时见第 6 节「排障」；实在起不来就手动注册账号再改角色（`docs/开发者须知.md` 第 4 节方式二）。

### 1.2 账号（密码统一 `Campus2026`）

| 用户名 | 昵称 | 角色 | 开户声誉 | 开户悬赏积分 |
| --- | --- | --- | --- | --- |
| `2023001` | 小明 | 学生 A | 0 | 0 |
| `2023002` | 小红 | 学生 B | 0 | 0 |
| `2023003` | 小刚 | 学生 C | 0 | 0 |
| `T2001` | 王老师 | 教师 | 0 | **1000** |
| `TA3001` | 李助教 | 助教 | 0 | **500** |
| `admin01` | 系统管理员 | 管理员 | 0 | 1000 |

开户积分的出处：`ReputationRulebook.initialPoints`（`ReputationRulebook.java:50-56`）。
**开户积分不是声誉，不进账本** —— 否则会破坏「声誉 = 流水之和」的对账不变量（`ReputationEventHandlers.java:73-75`）。

### 1.3 先记基线（关键，否则数字会对不上）

`demo` 种子数据本身就会产生投票、采纳、认证等动作，**账户里可能已经有流水**。演示前先记下基线：

```sql
-- 三个演示账号的 id
SELECT id, username, display_name FROM id_user WHERE username IN ('2023001','2023002','T2001');

-- 基线：声誉 / 可用积分 / 冻结积分
SELECT user_id, reputation, available_points, frozen_points FROM rep_account
WHERE user_id IN (SELECT id FROM id_user WHERE username IN ('2023001','2023002','T2001'));

-- 已有徽章
SELECT user_id, badge_code FROM rep_badge;
```

后面每一步都说「在基线上 +X」，评委问「为什么他现在是 75」时能对得上。

### 1.4 三个观察位

| 看什么 | 怎么看 |
| --- | --- |
| 声誉与徽章 | 前端暂无「个人声誉页」（见 `#50`），用 API：`GET /api/v1/users/{userId}/reputation` |
| 账本流水 | `GET /api/v1/users/{userId}/reputation/ledger?page=1&size=20` |
| 数据库 | 第 7 节 SQL |

`GET /api/v1/users/{userId}/reputation` 返回：

```json
{"userId":2,"displayName":"小红","reputation":75,"availablePoints":0,"frozenPoints":0,
 "badges":[{"code":"FIRST_ANSWER","name":"初来乍到","description":"提交第一个回答","awardedAt":"..."}]}
```

---

## 2. 演示步骤

> 全程用**新建的问题**演示，避免和种子数据里已有的悬赏（`q2`）冲突。

### 步骤 1：学生 A 提问

- **操作**：`2023001` 登录 → 首页「提问」→ 选课程「CS2001 数据结构」→ 标题填 10–150 字 → 提交
- **看到**：问题详情页，`0 个回答`
- **数字**：**提问不加声誉**（无流水）
- **校验**：`rep_account` 里 A 的 `reputation` 与基线一致

### 步骤 2：学生 B 回答 → 得「初来乍到」

- **操作**：`2023002` 登录 → 打开该问题 → 底部「写回答」→ 提交
- **看到**：回答出现在列表里
- **数字**：**回答本身不加声誉**；B 获得徽章 `FIRST_ANSWER`（初来乍到）
- **校验**：`SELECT badge_code FROM rep_badge WHERE user_id = <B的id>` → 出现 `FIRST_ANSWER`
- **答辩点**：徽章是「累计态」，`BadgeService.grant` 幂等，重复触发不会重复发

### 步骤 3：投票 —— 声誉变动的核心

用 A 和 C 给 B 的回答投票（**不能自投**，会报 `QA-1015`）。

| 动作 | B 的声誉增量 |
| --- | --- |
| A 点 ▲（新赞） | **+10** |
| A 再点 ▲（撤销赞） | **−10** |
| A 点 ▼（新踩） | **−2** |
| A 把 ▼ 改成 ▲（改投） | **+12** |

四步全做完，B 的声誉相对基线 **+10**（10 − 10 − 2 + 12）。

- **校验**：`GET /api/v1/users/<B的id>/reputation/ledger` 里应有 4 条 `ANSWER_VOTE`
  （只看 `ref_id` = **新建回答的 id** 的流水；种子数据里 `DemoDataSeeder:120` 也给 q1 的回答投过票，
  那是另一个 `ref_id`，别一起数）
- **答辩点**：改投**不是**「先扣再补两条流水」，而是**一条**按「新效果 − 旧效果」算的流水
  （`ReputationRulebook.voteDelta:33-35`；6 组用例见 `ReputationRulebookTest:13-20`）
- **反作弊**：每人每个自然日（UTC）因被投票获得的正向声誉最多 **+200**（`DAILY_VOTE_CAP`）；
  超出部分不再计分，负向投票不受限（`applyDailyCap:42-47`）

### 步骤 4：教师认证 —— 只有徽章，不加减声誉

- **操作**：`T2001` 登录 → 打开该问题 → B 的回答下方点「教师认证」
- **看到**：回答旁出现 `教师认证 · 王老师`
- **数字**：**B 的声誉不变**；获得徽章 `TEACHER_ENDORSED`（名师认可）
- **校验**：`rep_ledger` 里**没有**新增流水；`rep_badge` 多一条 `TEACHER_ENDORSED`
- **答辩点**：认证是「教师背书」，不是「加分」；加分只来自投票与采纳

### 步骤 5：教师发起悬赏 —— 两种货币分道扬镳（答辩必讲）

- **操作**：`T2001` 登录 → 打开刚才的问题 → 点「发起悬赏」→ 输入 `50`（范围 10–500）→ 默认 7 天
- **看到**：问题页出现「悬赏 50 分」，状态 OPEN
- **数字**：
  - 教师 `availablePoints` **−50**（1000 → 950）
  - 教师 `frozenPoints` **+50**（0 → 50）
  - 教师 **`reputation` 不变**
- **校验**：
  ```sql
  SELECT available_points, frozen_points, reputation FROM rep_account WHERE user_id = <T2001 的 id>;
  SELECT points, status FROM rep_bounty WHERE question_id = <问题 id>;
  ```
- **答辩点**：冻结 ≠ 扣款。到期没采纳要原路退回（`refundFrozen`），直接扣就没法退。
- **追问预案**：「为什么学生不能发悬赏？」—— 两道锁：学生开户 0 积分（`initialPoints`）、
  且 `Bounty.open` 只允许课程管理者（`REP-3001`，用例 `BountyAndAccountTest:22`）。
  悬赏是教师的运营功能，不是学生自助功能。

### 步骤 6：A 采纳 B 的回答 —— 三重效果

- **操作**：`2023001` 登录 → B 的回答 → 点「采纳此回答」
- **数字**（三件事同时发生）：
  1. B 的声誉 **+15**（采纳；自问自答为 0）
  2. B 的声誉再 **+50**（悬赏结算，以**声誉**形式入账，不是积分）
  3. 教师的 `frozenPoints` **−50**（50 → 0），`availablePoints` 仍是 950 —— 这 50 分**没有**转进 B 的钱包
- **徽章**：B 获得 `FIRST_ACCEPTED`（初露锋芒）；悬赏状态 `OPEN → AWARDED`
- **预期总账**：若步骤 3 全部做完，B 的声誉相对基线为 **+75**
  （步骤 3 的 +10 + 采纳 +15 + 悬赏 +50）
- **校验**：
  ```sql
  SELECT reason, delta, ref_type, ref_id FROM rep_ledger WHERE user_id = <B 的 id> ORDER BY id;
  -- 期望末两条：ANSWER_ACCEPTED(+15) 与 BOUNTY_AWARDED(+50)
  ```

### 步骤 7：排行榜

- **操作**：打开 `/leaderboard`，切换「周 / 月 / 学期 / 全部」
- **看到**：B 出现在列表里，`score` 等于其声誉
- **校验**：`GET /api/v1/leaderboard?period=all&limit=20`
- **答辩点**：排行榜读 Redis 缓存（`RedisLeaderboardStore`），最终一致，刷新可能延迟 1–2 秒

### 步骤 8（可选）：悬赏到期退回

- **做法**：发起一个 **1 天**到期的悬赏，到期不采纳
- **看到**：`rep_bounty.status` 从 `OPEN` 变 `EXPIRED`，教师 `frozenPoints` 回到 `availablePoints`
- **时机**：到期由定时任务处理，默认每 **5 分钟**扫一次
  （`ReputationJobs:29`，`co.reputation.bounty-expiry-interval-ms` 默认 `300000`）

---

## 3. 演示录音版（30 秒讲稿）

> 「声誉只有两个来源：投票和采纳。赞 +10、踩 −2，改投按差值补；回答被采纳 +15，自问自答不算。
> 教他认证只发徽章、不加分。悬赏是另一套货币：教师有 1000 分，发起悬赏时先冻结，采纳后这 50 分
> 以声誉形式给获奖者、发起人的冻结分消失；到期没采纳就原路退回。
> 所以一个人可以声誉很高但没积分，也可以花光积分而声誉不变 —— 这是两个独立的钱包。」

---

## 4. 排障（演示卡住时按顺序查）

**① 声誉没变**

```sql
-- 事件是否投递成功
SELECT id, event_type, published_at, attempts, last_error
FROM outbox_event WHERE published_at IS NULL ORDER BY id;
```

- `published_at` 为空且有 `last_error` → 监听器抛异常，按 `AGENTS.md` 第 13 节：**任一监听器失败会阻塞本批后续事件**
- 修复后重放：`UPDATE outbox_event SET attempts = 0, locked_until = NULL WHERE published_at IS NULL;`

**② 账本有流水但账户没变** → 对账（FF-8）：

```sql
SELECT (SELECT SUM(delta) FROM rep_ledger WHERE user_id = ?) AS ledger_sum,
       (SELECT reputation FROM rep_account WHERE user_id = ?) AS account;
```

两者必须相等；不等说明漏记或重复记。定时对账在每天 03:30（`Asia/Shanghai`，`ReputationJobs:37`）。

**③ 常见业务错误码**

| 码 | 文案 | 触发场景 |
| --- | --- | --- |
| `REP-1002` | 可用悬赏积分不足 / 冻结积分不足 | 学生（0 分）发起悬赏；或重复结算 |
| `REP-1003` | 悬赏积分必须在 10–500 之间 | 输入越界 |
| `REP-1004` | 悬赏期限必须在 1–14 天之间 | 天数越界 |
| `REP-1001` | 已删除或已采纳的问题不能发起悬赏 | 问题已被采纳 |
| `REP-2001` | 该问题已有进行中的悬赏 | 同一问题重复开悬赏 |
| `REP-3001` | 只有课程管理者可以发起悬赏 | 学生发起悬赏 |
| `QA-1015` | 不能给自己的内容投票 | 自投 |

**④ 徽章没出现** → 查 `rep_badge`；徽章是**幂等授予**，重复触发不会重复，也不会补发历史

---

## 5. 数字速查表（每个数字都有出处）

| 场景 | 数值 | 出处 |
| --- | --- | --- |
| 回答被采纳 | `+15`（自问自答 `0`） | `ReputationRulebook.java:15`、`acceptedDelta:37-39` |
| 被赞 | `+10` | `ReputationRulebook.java:16` |
| 被踩 | `−2` | `ReputationRulebook.java:17` |
| 撤销赞 / 撤销踩 | `−10` / `+2` | `voteDelta:33-35` |
| 赞改踩 / 踩改赞 | `−12` / `+12` | `voteDelta`；用例见 `ReputationRulebookTest:13-20` |
| 每日正向投票上限 | `+200`（UTC 自然日） | `ReputationRulebook.java:18`、`applyDailyCap:42-47` |
| 开户积分：教师/管理员 | `1000` | `initialPoints:50-56` |
| 开户积分：助教 | `500` | 同上 |
| 开户积分：学生 | `0` | 同上 |
| 悬赏范围 / 期限 | `10–500` 分 / `1–14` 天 | `Bounty.java:15-17` |
| 悬赏积分流转 | `freezePoints → spendFrozen / refundFrozen` | `ReputationAccount.java:47/57/64` |
| 悬赏结算记入 | 获奖者**声誉** `+悬赏分` | `BountyService.java:96-97` |
| 徽章阈值 | 首个回答 / 首个采纳 / 累计 10 采纳 / 声誉 100 / 1000 / 教师认证 | `BadgePolicy.java`、`BadgeType.java` |

---

## 6. 排障之外：起不来怎么办

1. `docker compose ps` 确认 `co-mysql` / `co-redis` 是 healthy
2. 看后端日志第一条 `ERROR`：
   - `Unsupported character encoding 'utf8mb4'` → 本机 MySQL 参数问题，见 `docs/开发者须知.md`
   - `Flyway validate failed` → 库是从旧结构来的，`docker compose down -v` 重建（会清空数据）
3. 仍然失败 → 改用「手动注册 + 改角色」：

```sql
UPDATE id_user SET role = 'TEACHER' WHERE username = '你的测试号';
```

---

## 7. 常用 SQL

```sql
-- 账户总览（按声誉倒序）
SELECT u.username, a.reputation, a.available_points, a.frozen_points
FROM rep_account a JOIN id_user u ON u.id = a.user_id
ORDER BY a.reputation DESC;

-- 某人最近 20 条流水
SELECT reason, delta, ref_type, ref_id, created_at
FROM rep_ledger WHERE user_id = ? ORDER BY id DESC LIMIT 20;

-- 悬赏状态
SELECT id, question_id, sponsor_id, points, status, winner_id, answer_id, expires_at FROM rep_bounty ORDER BY id DESC;

-- 徽章
SELECT u.username, b.badge_code, b.awarded_at FROM rep_badge b JOIN id_user u ON u.id = b.user_id ORDER BY b.id DESC;
```
