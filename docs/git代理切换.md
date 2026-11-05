# Git 代理切换（宿舍直连 / 教室代理）

> 适用场景：教室网络连不上 GitHub 时要走代理；宿舍可以直连。
> 核心原则：**代理软件没开，就不要在 git 里配代理**，否则所有 GitHub 操作都会报
> `Failed to connect to 127.0.0.1 port 7890`。

## 一、先判断当前是哪种模式

```powershell
git config --global --get http.proxy
```

| 输出 | 含义 | 该做什么 |
| --- | --- | --- |
| `http://127.0.0.1:7890` | 走代理 | 确认代理软件开着 |
| 没有输出 | 直连 | 宿舍就用这个 |

## 二、宿舍：切成直连

```powershell
git config --global --unset http.proxy
git config --global --unset https.proxy
git config --global --get http.proxy
```

最后一条没有任何输出 = 已切成功。

## 三、教室：切回代理

**第一步：先打开代理软件**，确认它在运行（任务栏有图标）。
**第二步：验证端口在监听**

```powershell
Test-NetConnection 127.0.0.1 -Port 7890 -InformationLevel Quiet
```

返回 `True` 才继续。返回 `False` 说明代理软件没开或端口不对。

**第三步：配置 git 走代理**

```powershell
git config --global http.proxy http://127.0.0.1:7890
git config --global https.proxy http://127.0.0.1:7890
```

## 四、验证能不能连上仓库

```powershell
git ls-remote https://github.com/hbuas-se-2026fall/cs2411-team04-campus-overflow
```

- 看到 `refs/heads/main` → 通了
- `Failed to connect to 127.0.0.1 port 7890` → 代理软件没开，或该用直连
- `Could not resolve host` / 超时 → 当前网络需要代理
- `Authentication failed` → 网络通了，只是没登录（配置凭据即可）

## 五、常见坑

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 昨天还能推，今天全报 7890 连不上 | 代理软件没启动 | 开软件，或按第二节切直连 |
| 教室配了代理还是连不上 | 代理软件端口不是 7890 | 在代理软件里看"局域网/http 端口" |
| 浏览器能开 GitHub，git 不行 | 浏览器走系统代理，git 走自己的配置 | 按第一节检查 git 自己的配置 |

> 注意：本机 Windows 系统代理（`ProxyEnable`）是关闭状态，所以浏览器直连可用，
> 但 git 的配置是**独立**的，两者互不影响。
