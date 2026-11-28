# #64 交付记录：管理页 + 个人资料页

> 采集时间：2026-11-28
> 环境：Windows 11 + Docker Desktop 29.9.3（WSL2 后端）+ JDK 21.0.10 + headless Edge 158
> 访问地址：`http://localhost:5173`（Vite 开发服务器，`/api` 代理到 `http://localhost:8080`）
> 演示数据：`dev,demo` profile 启动后由 `DemoDataSeeder` 自动播种，所有种子账号密码 `Campus2026`

## 一、环境与启动

```powershell
# 1. 起 MySQL + Redis
docker compose -f deploy/docker-compose.yml up -d mysql redis

# 2. 打包并启动后端（demo profile 会写演示数据）
mvn -B -f backend/pom.xml -pl co-bootstrap -am -DskipTests package
$env:SPRING_PROFILES_ACTIVE = "dev,demo"
$env:DB_HOST = "localhost"; $env:REDIS_HOST = "localhost"
java -jar backend/co-bootstrap/target/campus-overflow.jar
# 等 /actuator/health/readiness 返回 {"status":"UP"}

# 3. 起前端
cd frontend
npm install
npm run dev            # http://localhost:5173
```

> **待认证列表默认是空的**：`DemoDataSeeder.java:92` 把三名种子学生全部 `verify` 了。
> 截图前通过 `POST /api/v1/auth/register` 新注册了 `2023101 陈子昂`（id=7）与
> `2023102 林晓雨`（id=8），让管理页的「待认证」区块有真实数据可展示 —— 注册流程本来
> 就会产出待认证学生，这是真实业务路径而不是改库造数据。

## 二、截图清单

| 文件 | 操作 | 账号 | 说明 |
| --- | --- | --- | --- |
| `01-admin-pending-list.png` | 打开 `/admin` | `admin01` 系统管理员 | 待认证学生 2 行 + 创建教职工表单；顶栏有「我的」「管理」两个新入口 |
| `02-profile-edit-form.png` | 打开 `/profile` | `2023001` 小明 | 只读区（学号/角色/认证状态/邮箱）+ 可编辑三字段 + 所属课程 |
| `03-profile-saved.png` | 改昵称/学院/班级后点保存 | `2023001` 小明 | 绿色「已保存」，**顶栏昵称同步变「小明同学」** |
| `04-profile-after-reload.png` | 刷新页面 | `2023001` 小明 | 仍是新值 —— 证明落库，而非只改了 React 状态 |
| `05-admin-forbidden.png` | 学生访问 `/admin` | `2023001` 小明 | 「无权访问」+ 当前角色说明，不是白屏也不是报错页 |
| `06-admin-actions-done.png` | 点「认证」+ 填表创建助教 | `admin01` 系统管理员 | 林晓雨认证后从待认证列表消失（2→1）；创建出 TA3002 |

统计：**6 张 PNG，合计 344 KB**（单张 ≤ 62 KB，宽 1600px）。

## 三、验收标准逐条对照

| # | 验收标准 | 结果 | 证据 |
| --- | --- | --- | --- |
| 1 | `npm run typecheck` / `npm run build` 均 exit=0 | ✅ | `tsc --noEmit` exit=0；`tsc -b && vite build` exit=0，45 modules，dist 188.13 kB / gzip 60.86 kB |
| 2 | 管理页：待认证列表 + 认证 + 创建教职工 | ✅ | `01-admin-pending-list.png` + `06-admin-actions-done.png` |
| 3 | 非管理员访问 `/admin` 显示「无权访问」 | ✅ | `05-admin-forbidden.png` |
| 4 | 资料页：查看 + 改昵称/学院/班级 + 刷新仍生效 | ✅ | `02` → `03` → `04` |
| 5 | 后端应用层至少 2 个测试 | ✅ | `UserCommandServiceUpdateProfileTest` **8 个用例**（要求 2 个） |
| 6 | `mvn -B -f backend/pom.xml test -pl co-identity/co-identity-application -am` | ✅ | 见下方输出 |
| 7 | 不改已有断言 / 不删测试 / 不加 `@Disabled` | ✅ | 本卡只新增测试文件，`git diff` 未触及任何既有测试 |
| 8 | `ArchitectureTest` FF-1~FF-6 | ✅ | `Tests run: 1, Failures: 0, Errors: 0` |

### 测试输出

```
mvn -B -o -f backend/pom.xml test -pl co-identity/co-identity-application -am

co-shared-kernel          Tests run: 2,  Failures: 0, Errors: 0
co-identity-domain        Tests run: 17, Failures: 0, Errors: 0
co-identity-application   Tests run: 17, Failures: 0, Errors: 0
  AdminUserServiceDisableTest          9
  UserCommandServiceUpdateProfileTest  8
BUILD SUCCESS
```

```
mvn -B -o -f backend/pom.xml test -pl co-bootstrap -am -Dtest=ArchitectureTest
Tests run: 1, Failures: 0, Errors: 0, Time elapsed: 4.082 s
BUILD SUCCESS
```

## 四、实现说明

### 后端 `PUT /api/v1/auth/me`

| 项 | 取值 | 理由 |
| --- | --- | --- |
| 路径 | `/api/v1/auth/me`，不新建 Controller | URL 里不带用户 id，方法签名也就没有「改谁」这个参数，从接口层面杜绝越权改他人资料 |
| 返回 | 204 空响应 | 前端提交成功后本来就要重拉 `/auth/me` 刷新全局状态，返回体用不上；少一处数据契约也少一处与 `MeView` 同步的负担 |
| 可改字段 | 仅 `displayName` / `college` / `className` | `username` / `email` / `passwordHash` / `createdAt` / `version` 仍是 `final`，`role` / `verified` 不在 `User.updateProfile` 内 —— 改不了的字段在领域对象上就没有赋值路径 |
| 昵称校验 | 复用 `checkDisplayName`，错误码 `ID-1002` | 与注册时的 2–20 字、不含 `@`/空格规则完全一致 |
| 昵称唯一 | 应用层 `UserCommandService.java:52`，错误码 `ID-2002` | 用 `findByDisplayName` 再比 id，而不是 `existsByDisplayName` —— 后者分不清「被别人占了」和「就是我自己」，只改学院、昵称原样提交也会被判成重名 |
| 空白处理 | `optional(value, 50)` 归一成 `null` | 否则「未填写」会有 `''` 和 `NULL` 两种表示 |
| 审计 | `audit.record("PROFILE_UPDATED", actor.userId(), "USER", ..., user.displayName())` | 只写审计、不发领域事件：昵称变更不影响任何聚合的一致性，检索与通知都在读取时实时联表取 `displayName` |

### 前端

- 管理页 `frontend/src/pages/AdminPage.tsx`：三个区块一一对应 `AdminUserController` 的端点。认证成功后**重新拉一次列表**而不是本地删行 —— 本地删行会掩盖「其实没保存成功」这类问题。
- **`me.role === 'ADMIN'` 判断不是安全边界**，真正的授权在 `SecurityConfig.java:73` 的 `hasRole("ADMIN")`，后端每次都会再校验；前端这层只为不把必然 403 的页面摆到学生面前。
- 资料页 `frontend/src/pages/ProfilePage.tsx`：学号/角色/认证状态/邮箱只展示不可编辑；保存成功后 `await refresh()` 刷新全局 `me`，否则顶栏昵称不更新、用户会以为没保存成功；`dirty` 判断让「没有任何改动」时保存按钮禁用。
- `api.ts` 的 `describeError(e)`：`ApiError` 自带中文 `detail` 直接用；网络中断拿到的是英文 `TypeError("Failed to fetch")`，统一换成中文兜底。
- 导航：`/profile` 所有登录用户可见，`/admin` 仅 `me.role === 'ADMIN'` 可见。

## 五、复现步骤

```powershell
# 后端起来后，浏览器打开 http://localhost:5173
# 1) admin01 / Campus2026 登录 → 顶栏「管理」→ /admin
#    待认证列表为空时，先在 /login 用「注册」tab 注册一个新学号（如 2023103）
# 2) 点某行的「认证」→ 绿色提示「已认证「xxx」」，该行从列表消失
# 3) 填「创建教职工账号」表单 → 选角色助教 TA → 点「创建账号」
# 4) 退出，用 2023001 / Campus2026 登录 → 顶栏「我的」→ /profile
# 5) 改昵称/学院/班级 → 点「保存」→ 绿色「已保存」，顶栏昵称同步变化
# 6) F5 刷新 → 三个输入框仍是新值
# 7) 学生状态下直接访问 /admin → 显示「无权访问」
```
