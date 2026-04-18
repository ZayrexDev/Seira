# Seira

Seira 是一个提供 osu! 成绩查询的 QQ 机器人。
支持生成最好成绩图、最近成绩图、排行榜等，持续更新中...

目前 Seira 依赖 [oStella](https://github.com/ZayrexDev/oStella) 作为上游数据服务。

## 演示

### 最好成绩
<img width="800" alt="5b3c818dc2ee31c34006912b94a964ec" src="https://github.com/user-attachments/assets/e327de4d-3816-401b-ba0e-366ac743f412" />

### 铺面信息
<img width="400" alt="92e9c2fbfe193288f1d548a995907224" src="https://github.com/user-attachments/assets/879d6d89-8dbf-4d49-925f-088db7d48eeb" />

### 排行榜
<img width="400" alt="4152ccfa0c05510f5271368832d150d3" src="https://github.com/user-attachments/assets/faf2e03a-3fc4-4f32-8980-b7f54f1be0ab" />

### 铺面集信息
<img width="400" alt="2fe68add23352fb2dd7822747ad0a8a0" src="https://github.com/user-attachments/assets/50b0395b-a3be-48e6-9c1e-024f7de7e59a" />

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

| 命令        | 用法                           | 结果               |
|-----------|------------------------------|------------------|
| `/status` | `/status`                    | 服务状态文本           |
| `/daily`  | `/daily`                     | 每日挑战信息           |
| `/mp`     | `/mp`                        | 多人房间列表           |
| `/bind`   | `/bind <uid>`                | 绑定当前用户到 osu uid  |
| `/bo`     | `/bo <n> <uid>`              | Best 成绩图片        |
| `/top`    | `/top <n> <uid>`             | 同 `/bo`          |
| `/rs`     | `/rs <n> <uid>`              | Recent 成绩图片      |
| `/c`      | `/c <bm> <uid1>[,<uid2>]...` | 生成指定用户的特定铺面排行榜   |
| `/lb`     | `/lb [bm]`                   | `/c` 的别名（默认使用绑定） |

绑定后可省略 uid：`/bo <n>`、`/rs <n>`。
在群聊中，`/c <bm>` 会默认使用该群里已绑定过的所有玩家 uid；私聊中 `/c <bm>` 使用你自己的绑定 uid。
另外：`/c` 或 `/lb` 不带参数时，会基于默认绑定 uid 生成综合排行榜（群聊=本群绑定用户，私聊=你自己的绑定 uid）。

