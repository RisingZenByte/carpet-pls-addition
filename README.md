# Carpet PLS Addition

[![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-green.svg)](https://minecraft.net)

PLS 服务器定制的 [Carpet mod](https://github.com/gnembon/fabric-carpet) **服务端**扩展，提供 **PCA 同步协议**，供客户端 [pca-client](https://github.com/RisingZenByte/pca-client) 实现 Tweakeroo 多人容器预览。

> **注意：** 本 mod 为 **纯服务端**（`environment: server`）。**不要**在客户端安装；客户端请使用 [pca-client](https://github.com/RisingZenByte/pca-client)。

---

## 功能

### PCA 同步协议 (`pcaSyncProtocol`)

在服务端与客户端之间同步 Entity、BlockEntity NBT，用于多人游戏中的容器预览等场景。

| 属性 | 值 |
|------|-----|
| 类型 | `boolean` |
| 默认值 | `false` |
| Carpet 分类 | `PCA`, `protocal`, `client` |

### PCA 同步玩家实体 (`pcaSyncPlayerEntity`)

控制哪些玩家实体数据可被同步（如预览玩家背包）。

| 属性 | 值 |
|------|-----|
| 类型 | `enum` |
| 默认值 | `ops` |
| 选项 | `nobody`, `bot`, `ops`, `ops_and_self`, `everyone` |

---

## 安装

### 服务端

1. Minecraft **26.2** + Fabric Loader **≥ 0.19.3**
2. 安装 [fabric-carpet 26.2](https://github.com/gnembon/fabric-carpet/releases/tag/v26.2)
3. 将编译好的 `carpet-pls-addition-*.jar` 放入 `mods/`
4. 启动后执行：

```
/pls pcaSyncProtocol true
/pls pcaSyncPlayerEntity ops
```

配置可写入世界目录 `pls.conf` 持久保存。

### 客户端

安装 **[pca-client](https://github.com/RisingZenByte/pca-client)** + MaLiLib + Tweakeroo。**不要**安装本 mod。

---

## 编译

```powershell
git clone https://github.com/RisingZenByte/carpet-pls-addition.git
cd carpet-pls-addition
.\gradlew.bat :26.2:build
```

产物：`versions/26.2/build/libs/carpet-pls-addition-*.jar`

---

## 依赖

| 依赖 | 说明 |
|------|------|
| Minecraft 26.2 | |
| Fabric Loader ≥ 0.19.3 | |
| [fabric-carpet](https://github.com/gnembon/fabric-carpet) ≥ 26.2 | 必须 |
| [fanetlib](https://github.com/Fallen-Breath/fanetlib) | 已内嵌于 jar |

---

## 致谢与版权

本项目主要基于 [Fallen-Breath/pca-protocol](https://github.com/Fallen-Breath/pca-protocol)（LGPL-3.0）移植，并集成 [fanetlib](https://github.com/Fallen-Breath/fanetlib) 网络层。

**完整第三方声明见 [NOTICES.md](NOTICES.md)。**

- **Copyright (C) 2026 [RisingZenByte](https://github.com/RisingZenByte)**
- **License:** [LGPL-3.0-or-later](LICENSE)

---

## 相关链接

- 客户端配套 mod：[pca-client](https://github.com/RisingZenByte/pca-client)
- 问题反馈：[Issues](https://github.com/RisingZenByte/carpet-pls-addition/issues)
