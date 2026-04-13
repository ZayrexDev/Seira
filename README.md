# Seira

Seira 是一个提供 osu! 成绩查询的 QQ 机器人。
支持生成最好成绩图、最近成绩图、排行榜等，持续更新中...

目前 Seira 依赖 [oStella](https://github.com/ZayrexDev/oStella) 作为上游数据服务。

## 演示

### 最好成绩
<img width="900" alt="image" src="https://github.com/user-attachments/assets/f4ff6ac7-b241-4c6b-956e-9c258c27faa7" />

### 铺面信息
<img width="400" alt="image" src="https://github.com/user-attachments/assets/0b8c51aa-ecf7-4e9e-9f67-327ce53ad9e3" />

### 排行榜
<img width="400" alt="image" src="https://github.com/user-attachments/assets/4a229779-c674-4172-8d6b-6accdabb5a5e" />

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

| 命令        | 用法                           | 结果             |
|-----------|------------------------------|----------------|
| `/status` | `/status`                    | 服务状态文本         |
| `/daily`  | `/daily`                     | 每日挑战信息         |
| `/mp`     | `/mp`                        | 多人房间列表         |
| `/bo`     | `/bo <n> <uid>`              | Best 成绩图片      |
| `/top`    | `/top <n> <uid>`             | 同 `/bo`        |
| `/rs`     | `/rs <n> <uid>`              | Recent 成绩图片    |
| `/c`      | `/c <bm> <uid1>[,<uid2>]...` | 生成指定用户的特定铺面排行榜 |

