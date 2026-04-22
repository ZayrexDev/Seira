# Seira

Seira 是一个提供 osu! 成绩查询的 QQ 机器人。
支持生成最好成绩图、最近成绩图、排行榜等，持续更新中...

Seira 依赖 [oStella](https://github.com/ZayrexDev/oStella) 作为上游数据服务。

## 演示

### 最好成绩
<img width="800" alt="image" src="https://github.com/user-attachments/assets/a10742b8-148d-4fab-90b3-3ecd6882e2ae" />

### 铺面信息
<img width="400" alt="image" src="https://github.com/user-attachments/assets/41f36596-94c7-4407-af82-490efccf6704" />

### 排行榜
<img width="400" alt="image" src="https://github.com/user-attachments/assets/cbc361a4-61e0-440d-908e-f1d52009373e" />

### 分数
<img width="400" alt="image" src="https://github.com/user-attachments/assets/c3ac7fde-adea-427b-be0a-9871a45ca8ad" />

### 铺面集信息
<img width="400" alt="image" src="https://github.com/user-attachments/assets/2bb887d6-5fff-429b-b39e-963235ec463e" />

## 快速开始

### 1) 准备环境

- JDK 25（`pom.xml` 配置为 `source/target=25`）
- Maven
- QQ 机器人应用凭据
- 一个可访问的 `SEIRA_OSTELLA_ENDPOINT`

### 2) 配置 `.env`

通用必需项：

- `SEIRA_PLATFORM`（`qq` 或 `napcat`，默认 `qq`）
- `SEIRA_OSTELLA_ENDPOINT`

`qq` 平台额外必需项：

- `SEIRA_QQ_APPID`
- `SEIRA_QQ_APPSECRET`

可选项：

- `SEIRA_QQ_INTENTS`（默认 `(1 << 25)`）
- `SEIRA_SQLITE_PATH`（默认 `data/seira.db`，用于存储 `/bind` 绑定关系）
- `SEIRA_NAPCAT_WS_ENDPOINT`（仅 `napcat` 平台必需）
- `SEIRA_NAPCAT_HTTP_ENDPOINT`（仅 `napcat` 平台必需）
- `SEIRA_NAPCAT_TOKEN`（Napcat 开启鉴权时填写）

QQ 示例：

```env
SEIRA_PLATFORM=qq
SEIRA_QQ_APPID=your_app_id
SEIRA_QQ_APPSECRET=your_app_secret
SEIRA_OSTELLA_ENDPOINT=http://localhost:8721
SEIRA_QQ_INTENTS=33554432
SEIRA_SQLITE_PATH=./data/seira.db
```

Napcat 示例：

```env
SEIRA_PLATFORM=napcat
SEIRA_OSTELLA_ENDPOINT=http://localhost:8721
SEIRA_NAPCAT_WS_ENDPOINT=ws://127.0.0.1:3001
SEIRA_NAPCAT_HTTP_ENDPOINT=http://127.0.0.1:3000
SEIRA_NAPCAT_TOKEN=
SEIRA_SQLITE_PATH=./data/seira.db
```

### 3) 启动

```shell
mvn -U clean compile exec:java
```

## 常用命令

> 所有命令都以 `/` 开头。

| 命令        | 用法                            | 结果                    |
|-----------|-------------------------------|-----------------------|
| `/bind`   | `/bind <uid>`                 | 绑定当前用户到 osu uid       |
| `/unbind` | `/unbind`                     | 解除当前用户的 uid 绑定        |
| `/bo`     | `/bo [n] [uid]`               | 最好n个成绩图，无参时获取最佳成绩详情   |
| `/rs`     | `/rs [n] [uid]`               | 最近n个成绩图，无参时获取最近一个成绩详情 |
| `/m`      | `/m <id/rsN/boN> [Mod]`       | 获取指定铺面信息              |
| `/s`      | `/s <id/rsN/boN>`             | 获取指定成绩图               |
| `/r`      | `/r <id/rsN/boN>`             | 生成并发送指定成绩回放视频         |
| `/ms`     | `/ms <id/rsN/boN>`            | 获取指定铺面集信息             |
| `/sms`    | `/sms <query>`                | 搜索铺面集                 |
| `/lb`     | `/lb [id] [<uid1>,<uid2>...]` | 列出指定铺面排行或表现分排行        |
| `/daily`  | `/daily`                      | 每日挑战信息                |
| `/mp`     | `/mp`                         | 多人房间列表                |
| `/status` | `/status`                     | 服务状态文本                |
| `/help`   | `/help`                       | 显示帮助信息                |

绑定后可省略 uid：`/bo`、`/rs`。
在群聊中，`/lb <bm>` 会默认使用该群里已绑定过的所有玩家 uid；私聊中 `/lb <bm>` 使用你自己的绑定 uid。
另外：`/lb` 不带参数时，会基于默认绑定 uid 生成总表现分排行榜（群聊=本群绑定用户，私聊=你自己的绑定 uid）。

其中会调用 `APIHelper` 的指令（如 `/bo`、`/rs`、`/m`、`/s`、`/ms`、`/sms`、`/lb`、`/daily`、`/mp`）会先回复“请求已加入队列，预计等待时间 X 秒”，待异步请求完成后再额外发送结果消息。

`/r`（回放渲染）会先返回“生成请求正在等待中，队列位置：N”，随后在渲染完成后再发送回放视频。

### 快捷查询

`/m`、`/s`、`/ms` 支持快捷查询写法，格式是 `rs5`、`bo3` 这样的形式。

- `rs5`：使用你已绑定的玩家ID，查询“最近成绩第 5 条”
- `bo3`：使用你已绑定的玩家ID，查询“最好成绩第 3 条”

使用快捷查询前需要先执行 `/bind <玩家ID>`，否则会提示无法使用快捷查询。
