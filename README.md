# Project CrossingLines (b1.2)

一个基于 **Minecraft Java 1.21.4 + Fabric** 的铁路规划与施工 Mod。

## b1.2 已实现功能

- 可视化规划入口：客户端按 `P` 打开规划器界面。
- 两点铁路规划：支持 `SURFACE`（经典地面）、`EMBANKMENT`（堤式地面）与 `UNDERGROUND`（地下线）三种类型。
- 地形感知路径搜索：基于 A* 的简化实现，考虑高度变化并自动贴地生成。
- 自动施工队列：服务端分 Tick 放置铁轨与基础方块，避免一次性大量方块更新。
- 配套设施：
  - 经典地面线：石砖地基 + 周期性路灯。
  - 堤式地面线：安山岩/圆石双层路基 + 侧向魂灯立柱。
  - 地下线：自动挖设隧道截面并添加顶部照明。
- 线路持久化：使用 `PersistentState` 保存线路及最近一次规划。
- 施工队列控制：支持查询施工队列长度与一键取消施工队列任务（可中断任务）。

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
- `/cl queue_status`
  - 查看当前施工队列长度。
- `/cl cancel_build`
  - 取消当前及排队中的施工任务。

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

## b1.2 边界说明

- 当前为可运行的基础版本，重点在“规划 -> 保存 -> 施工”闭环。
- 预览渲染、撤销施工、多线路并发优先级、更多设施模板等属于后续版本。

## Changelog

### b1.2 (2026-04-24)

- 回滚地下铁路相关更新，恢复为稳定的地面/堤式线路双类型。
- 根据 `MC_RailMod_设计方案.md` 的“性能与稳定性建议 - 可中断任务”方向，新增施工队列可中断能力：
  - 新增 `/cl cancel_build` 用于清空在建与排队任务。
  - 新增 `/cl queue_status` 用于快速查看队列状态。
- 更新全局版本号为 `b1.2` 并同步文档与界面版本标识。


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
