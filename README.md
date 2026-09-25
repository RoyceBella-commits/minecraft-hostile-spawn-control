# Hostile Spawn Control · 敌对生物生成控制

[![Minecraft](https://img.shields.io/badge/Minecraft-26.3-62B47A)](https://www.minecraft.net/)
[![Loader](https://img.shields.io/badge/Loader-Fabric-DBD0B4)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25-ED8B00)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)
[![Release](https://img.shields.io/github/v/release/RoyceBella-commits/minecraft-hostile-spawn-control)](https://github.com/RoyceBella-commits/minecraft-hostile-spawn-control/releases/latest)

**讨厌苦力怕炸家，但又不想开和平模式？**
这个 Fabric Mod 让你为**每一种敌对生物**单独决定它能不能出现，还能细分到**它是怎么出现的**：自然刷新、刷怪笼、刷怪蛋、命令……各自独立开关。

> *English:* A Fabric mod that lets you toggle spawning for each hostile mob individually, per spawn source (natural, spawner, structure, event, conversion, spawn egg, command). Defaults to vanilla behaviour.

---

## ✨ 功能亮点

- **逐个生物控制**：关掉苦力怕，僵尸和骷髅照常出现；难度设置不受影响，困难模式下也能只关某几种。
- **按生成方式细分**：同一种生物，可以禁止它夜晚自然刷新，却保留刷怪笼和 `/summon`。
- **只拦新生成，不删已有生物**：在生物“即将生成”时拒绝，而不是生成后再删除。已有的生物、存档里的生物都不会被动到。
- **默认 = 原版**：装上后什么都不改，游戏行为与原版完全一致。
- **自动识别敌对生物**：直接从游戏注册表读取，其他 Mod 添加的敌对生物也会自动出现在列表里。
- **游戏内图形界面 + 命令**：单人按一个键就能改；服务器管理员可以用命令。

## 📥 安装

需要：

| 组件 | 版本 |
|---|---|
| Minecraft Java 版 | 26.3 |
| [Fabric Loader](https://fabricmc.net/use/installer/) | 0.19.5 或更高 |
| [Fabric API](https://modrinth.com/mod/fabric-api) | 适用于 26.3 的版本 |
| Java | 25（官方启动器会自动提供） |

步骤：

1. 用 [Fabric 安装器](https://fabricmc.net/use/installer/)为 Minecraft 26.3 安装 Fabric Loader。
2. 从 [Releases](https://github.com/RoyceBella-commits/minecraft-hostile-spawn-control/releases/latest) 下载 `hostile_spawn_control-x.y.z.jar`。
3. 把它和 Fabric API 一起放进游戏目录的 `mods` 文件夹（Windows 默认是 `%APPDATA%\.minecraft\mods`）。
4. 在启动器里选择 Fabric 配置启动游戏。

**单人游戏**：装在自己电脑上即可。
**多人服务器**：只需要装在服务器上，玩家不用安装；想在本地看到中文界面和按键的玩家也可以装。

## 🎮 使用

### 图形界面

进入单人世界后按 **K**（可在 *选项 → 控制 → 按键绑定* 中修改）。

```
┌───────────────────────┬──────────────────────────────────────────┐
│ [ 搜索名称或 ID…    ] │ 苦力怕               [本生物全开][本生物全关] │
│ 🥚 烈焰人             │ minecraft:creeper                          │
│ 🥚 洞穴蜘蛛           │ 自然生成                          [ 关 ]   │
│ 🥚 苦力怕       受限  │ 刷怪笼                            [ 开 ]   │
│ 🥚 溺尸               │ 结构生成                          [ 开 ]   │
│ 🥚 末影人             │ 事件生成                          [ 开 ]   │
│ …                     │ 转换生成                          [ 开 ]   │
│                       │ 刷怪蛋                            [ 关 ]   │
│                       │ 命令召唤                          [ 开 ]   │
├───────────────────────┴──────────────────────────────────────────┤
│               [ 全部开启 ]   [ 全部关闭 ]   [ 完成 ]               │
└──────────────────────────────────────────────────────────────────┘
```

- **左边选生物**：支持搜中文名、英文名或 ID（`苦`、`creeper`、`minecraft:creeper` 都行）。有任何限制的生物会标出「受限」。
- **右边改开关**：鼠标悬停在开关上可以看到这一项具体包括哪些情况。
- **完成** 时自动保存。

### 命令

需要作弊权限（单人）或 OP 2 级（服务器）。

```mcfunction
# 禁止苦力怕自然刷新
/spawncontrol set minecraft:creeper natural false

# 所有敌对生物都不能用刷怪蛋刷出
/spawncontrol disableall spawn_egg

# 恢复一切为原版
/spawncontrol enableall

# 查看当前设置 / 拦截统计
/spawncontrol list
/spawncontrol status
```

| 子命令 | 说明 |
|---|---|
| `set <生物> <生成方式> <true\|false>` | 设置某种生物的某个生成方式 |
| `enableall [生成方式]` / `disableall [生成方式]` | 对所有敌对生物批量开 / 关；不写生成方式则作用于全部 |
| `list` | 列出每种生物被关闭的生成方式 |
| `status` | 各生成方式的禁用数量与拦截次数 |
| `reload` | 手动修改配置文件后重新读取 |

生成方式参数：`natural` `spawner` `structure` `event` `conversion` `spawn_egg` `command`

## 📖 各开关到底管什么

| 开关 | 包括 |
|---|---|
| **自然生成** `natural` | 夜晚、洞穴、下界、末地、各生物群系的自然刷怪；新区块生成时的刷怪；失眠引来的幻翼；女巫小屋、下界要塞、掠夺者前哨站等结构范围内**持续刷新**的怪 |
| **刷怪笼** `spawner` | 刷怪笼、试炼刷怪笼 |
| **结构生成** `structure` | 结构**生成时一次性放置**的生物：女巫小屋的女巫、海底神殿的远古守卫者、林地府邸的卫道士与唤魔者、末地城的潜影贝、海底废墟的溺尸、下界传送门冒出的僵尸猪灵 |
| **事件生成** `event` | 袭击、灾厄巡逻队、僵尸围城、末影龙复活、搭建凋灵、幽匿尖啸体召唤监守者、寄生 / 渗浆效果、末影珍珠带出的末影螨、骷髅陷阱 |
| **转换生成** `conversion` | 村民被雷劈成女巫、僵尸溺水成溺尸、骷髅冻成流髑、猪灵在主世界僵尸化等 |
| **刷怪蛋** `spawn_egg` | 玩家使用刷怪蛋、发射器发射刷怪蛋 |
| **命令召唤** `command` | `/summon` |

被拦截时会发生什么：

- 刷怪蛋**不会被消耗**；
- 转换被拦截时，**原来的生物保持原样**（村民还是村民）；
- 凋灵被拦截时，搭好的灵魂沙和头颅**不会消失**；
- `/summon` 会提示“无法召唤实体”。

**永远不受影响**：已经存在的生物、区块重新加载的生物、穿越传送门的生物，以及繁殖、骑乘组合、僵尸增援、唤魔者召唤恼鬼等生物自身机制。

## ⚙️ 配置文件

`config/hostile_spawn_control.json`，对所有存档生效，只记录被关掉的项：

```json
{
  "version": 2,
  "mobs": {
    "minecraft:creeper": { "natural": false, "spawn_egg": false },
    "minecraft:phantom": { "natural": false }
  }
}
```

删除这个文件就会回到原版行为。游戏运行中手动修改后，执行 `/spawncontrol reload`。

## ❓ 常见问题

**关掉之后，已经在附近的苦力怕会消失吗？**
不会。这个 Mod 只阻止新的生成，不删除任何已有生物。

**和难度设置冲突吗？**
不冲突，两者独立。和平模式下原版本来就不刷敌对生物；在其他难度下，这个 Mod 在此基础上进一步限制。

**我在别人的服务器上按 K 为什么改不了？**
规则由服务器执行。连接远程服务器时界面只读，需要管理员用 `/spawncontrol` 修改。

**列表里有末影龙、凋灵、巨人，它们平时也不会刷出来啊？**
列表是从游戏里自动识别的，凡是被游戏归为“怪物”分类的都会出现。关掉它们对应的开关同样有效，例如关掉凋灵的「事件生成」就无法搭建凋灵。

**支持其他 Mod 的怪物吗？**
支持。只要那个 Mod 把生物注册为怪物分类，就会自动出现在列表中。

**从 1.0.0 升级需要注意什么？**
删掉旧的 JAR，放入新版本即可，配置文件自动兼容。

## 🛠️ 从源码构建

需要 JDK 25。

```bash
git clone https://github.com/RoyceBella-commits/minecraft-hostile-spawn-control.git
cd minecraft-hostile-spawn-control
./gradlew build
```

产物在 `build/libs/`，使用不带 `-sources` 后缀的那个 JAR。

<details>
<summary>实现原理</summary>

- **生物识别**：遍历 `BuiltInRegistries.ENTITY_TYPE`，筛选 `MobCategory.MONSTER`。
- **规则模型**：`生物 × SpawnSource`，每格一个布尔值，默认允许。
- **拦截点**（Mixin）：
  - `SpawnPlacements.checkSpawnRules`：自然刷怪与刷怪笼在创建实体前的规则检查；
  - `EntityType.create(Level, EntitySpawnRequest)`：所有实体创建的统一入口，按 `EntitySpawnReason` 映射到生成方式，禁止时直接返回 `null`，原版调用方均能正确处理。
- `LOAD`、`DIMENSION_TRAVEL` 等原因不映射到任何开关，因此已有生物永远不会被拦截。

</details>

## 📄 许可证

[MIT](LICENSE)
