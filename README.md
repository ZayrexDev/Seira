# Seira

Seira 是一个面向私聊场景的 Java QQ 机器人客户端。
你给它发斜杠命令，它会调用后端接口并回你文本或图片结果。

目前 Seira 依赖 [oStella](https://github.com/ZayrexDev/oStella) 作为上游数据服务（例如成绩图和日常数据）。

## Seira 能做什么

- 连接 QQ Gateway，并自动维持心跳
- 自动获取并续期 access token
- 监听私聊事件 `C2C_MESSAGE_CREATE`
- 识别 `/` 命令并返回文本或图片

## 快速开始

### 1) 准备环境

- JDK 25（`pom.xml` 配置为 `source/target=25`）
- Maven
- QQ 机器人应用凭据
- 一个可访问的 `SEIRA_OSTELLA_ENDPOINT`

### 2) 配置 `.env`

必需项：

- `SEIRA_APPID`
- `SEIRA_APPSECRET`
- `SEIRA_OSTELLA_ENDPOINT`

可选项：

- `SEIRA_INTENTS`（默认 `(1 << 25)`）

示例：

```env
SEIRA_APPID=your_app_id
SEIRA_APPSECRET=your_app_secret
SEIRA_OSTELLA_ENDPOINT=http://localhost:8721
SEIRA_INTENTS=33554432
```

### 3) 构建并启动

```shell
mvn clean package
java -jar target/Seira-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## 常用命令

> 所有命令都以 `/` 开头。

| 命令        | 用法                    | 结果          |
|-----------|-----------------------|-------------|
| `/status` | `/status`             | 服务状态文本      |
| `/daily`  | `/daily`              | 每日挑战信息      |
| `/mp`     | `/mp`                 | 多人房间列表      |
| `/bo`     | `/bo <n> <playerId>`  | Best 成绩图片   |
| `/top`    | `/top <n> <playerId>` | 同 `/bo`     |
| `/rs`     | `/rs <n> <playerId>`  | Recent 成绩图片 |
| `/c`      | `/c`                  | 占位回复（功能未完成） |

已知限制：

- `/bo`、`/rs` 的单参数模式还没实现
- `/c` 目前是占位命令
- `/bo`、`/top`、`/rs` 参数不是整数时，可能抛出 `NumberFormatException`

## 日志与产物

- 运行日志：`logs/latest.log`（以及滚动归档 `*.log.gz`）
- 打包产物：
  - `target/Seira-1.0-SNAPSHOT.jar`
  - `target/Seira-1.0-SNAPSHOT-jar-with-dependencies.jar`

