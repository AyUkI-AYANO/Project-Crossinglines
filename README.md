# Project CrossingLines (b1.1)

一个基于 **Minecraft Java 1.21.4 + Fabric** 的铁路规划与施工 Mod。

## b1.1 已实现功能

- 可视化规划入口：客户端按 `P` 打开规划器界面。
- 两点铁路规划：支持 `SURFACE`（经典地面）、`EMBANKMENT`（堤式地面）与 `UNDERGROUND`（地下线）三种类型。
- 地形感知路径搜索：基于 A* 的简化实现，考虑高度变化并自动贴地生成。
- 自动施工队列：服务端分 Tick 放置铁轨与基础方块，避免一次性大量方块更新。
- 配套设施：
  - 经典地面线：石砖地基 + 周期性路灯。
  - 堤式地面线：安山岩/圆石双层路基 + 侧向魂灯立柱。
  - 地下线：自动挖设隧道截面并添加顶部照明。
- 线路持久化：使用 `PersistentState` 保存线路及最近一次规划。

## 快速开始

### 1. 环境要求

- JDK 21
- Gradle（或使用 `./gradlew`）
- Minecraft 1.21.4
- Fabric Loader 0.16+

### 2. 构建

```bash
gradle build
```

### 3. 游戏内操作

1. 进入世界后按 `P` 打开规划器。
2. 在界面中设置起点和终点（取玩家当前方块坐标）。
3. 切换线路类型（经典地面/堤式地面/地下线）。
4. 点击“规划并预览”（当前版本以服务端规划结果文本反馈为主）。
5. 点击“开始施工”，将最近一次规划加入施工队列。

## 命令

- `/cl plan <start> <end> <type>`
  - 示例：`/cl plan 0 70 0 120 72 180 surface`
  - `type` 支持：`surface` / `embankment`（别名 `e`）/ `underground`（别名 `u`）。
- `/cl build_latest`
  - 施工最近一次成功规划的线路。

## 项目结构

```text
src/main/java/com/crossinglines
├─ CrossingLinesMod.java              # Mod 入口
├─ client/
│  ├─ CrossingLinesClientMod.java     # 客户端入口 & 按键
│  └─ RoutePlannerScreen.java         # 规划 UI
├─ command/CrossingLinesCommand.java  # /cl 命令
├─ path/TerrainPathFinder.java        # A* 路径搜索
├─ planner/                           # 线路设施策略
├─ build/BuildTaskQueue.java          # 分段施工
├─ state/RailLineState.java           # 持久化
└─ model/                             # 数据模型
```

## b1.1 边界说明

- 当前为可运行的基础版本，重点在“规划 -> 保存 -> 施工”闭环。
- 预览渲染、撤销施工、多线路并发优先级、更多设施模板等属于后续版本。

## Changelog

### b1.1 (2026-04-23)

- 根据 `MC_RailMod_设计方案.md` 的“增强（第二阶段）- 地下铁路自动处理”方向，新增地下线路类型的真实规划/施工支持。
- 路径规划器新增固定埋深逻辑：地下线会基于起终点地表高度计算目标埋深，生成稳定隧道路由。
- 设施策略路由新增 `UndergroundFacilityPolicy` 挂载，不再将 `underground` 兼容映射为堤式线路。
- 客户端规划 UI 的线路类型切换支持三态循环（Surface / Embankment / Underground）。


## 常见报错排查

### 1) `Plugin [id: 'fabric-loom'] was not found`

这是因为 Gradle 在插件解析阶段没有找到 Fabric Maven。你需要：

- 确保 `settings.gradle` 中有 `pluginManagement.repositories`，并包含：
  - `https://maven.fabricmc.net/`
  - `gradlePluginPortal()`
- 重新执行：

```bash
gradle --refresh-dependencies build
```

### 2) `Unsupported class file major version 69`

这是 **JDK 25** 导致的兼容性问题（Loom/Gradle 脚本链路通常建议 JDK 21）。

- 切换到 JDK 21 后再构建。
- Windows PowerShell 示例：

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
gradle build
```

## 许可证

MIT
