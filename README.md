# Carpet PLS Addition

[![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](LICENSE)
[![Releases](https://img.shields.io/github/v/release/RisingZenByte/carpet-pls-addition?label=release)](https://github.com/RisingZenByte/carpet-pls-addition/releases)

PLS 服务器定制的 [Carpet mod](https://github.com/gnembon/fabric-carpet) **服务端**扩展，提供 **PCA 同步协议**，供客户端 [pca-client](https://github.com/RisingZenByte/pca-client) 实现 Tweakeroo 多人容器预览。

> **注意：** 本 mod 为 **纯服务端**（`environment: server`）。**不要**在客户端安装；客户端请使用 [pca-client](https://github.com/RisingZenByte/pca-client)。

---

## 支持版本

本项目采用 **多版本 preprocess 架构**，会随 Minecraft 更新持续维护。请从 [Releases](https://github.com/RisingZenByte/carpet-pls-addition/releases) 下载与服务器 **相同 MC 版本** 的 jar。

| Minecraft | Mod 版本 | 状态 |
|-----------|----------|------|
| **26.2** | 1.0.0 | 当前支持 |

Release 标签格式：`v{mod版本}-mc{mc版本}`（例如 `v1.0.0-mc26.2`）。

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

1. 安装对应 MC 版本的 **Fabric Loader** 与 **[fabric-carpet](https://github.com/gnembon/fabric-carpet)**（版本需匹配）
2. 将 Release 中的 `carpet-pls-addition-*.jar` 放入服务端 `mods/`
3. 启动后执行：

```
/pls pcaSyncProtocol true
/pls pcaSyncPlayerEntity ops
```

配置可写入世界目录 `pls.conf` 持久保存。

**客户端**请安装同 MC 版本的 [pca-client](https://github.com/RisingZenByte/pca-client) + MaLiLib + Tweakeroo，**不要**安装本 mod。

---

## 编译

```powershell
git clone https://github.com/RisingZenByte/carpet-pls-addition.git
cd carpet-pls-addition
.\gradlew.bat :26.2:build
```

产物位于 `versions/{mc版本}/build/libs/`。发布构建：

```powershell
$env:BUILD_RELEASE = "true"
.\gradlew.bat :26.2:build
```

新增 MC 版本：在 `settings.json` 与 `versions/` 下添加对应子项目，并更新 `build.gradle` 中的 preprocess 节点链接。

---

## 依赖

| 依赖 | 说明 |
|------|------|
| Minecraft（与 jar 匹配） | 见 Releases |
| Fabric Loader ≥ 0.19.3 | |
| [fabric-carpet](https://github.com/gnembon/fabric-carpet) | 必须，版本需匹配 MC |
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
