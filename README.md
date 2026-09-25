# 敌对生物生成控制（Hostile Spawn Control）v1.0.0

为 Minecraft Java 版中**每一种敌对生物**提供独立的「自然生成」开关。
关闭某种生物后，只是不再自然刷新该生物；游戏难度、其他生物、已有生物都不受影响。

## 版本记录

```text
Minecraft: 26.3
Loader: Fabric Loader 0.19.5
Java major: 25
Fabric API: 0.161.0+26.3（必需，已放在 mods/ 中）
Other dependencies: 无
Mod: hostile_spawn_control 1.0.0，hostile_spawn_control-1.0.0.jar
Side: 两端通用（单人游戏装在客户端即可；专用服务器装在服务端即可，玩家客户端不强制安装）
Build: Loom 1.17-SNAPSHOT，Gradle 9.6.0，Temurin JDK 25.0.4.1（macOS arm64）
Tested on: Mac 开发服务端（runServer）自测通过；Windows 客户端界面待实机测试
```

## 安装（Windows 本机）

1. 安装适用于 **Minecraft 26.3** 的 **Fabric Loader 0.19.5**（https://fabricmc.net/use/installer/ ）。安装后启动器里会出现 `fabric-loader-0.19.5-26.3` 配置。
2. 建议为该配置设置独立的游戏目录，例如 `F:\MinecraftInstances\Fabric-26.3-Dev`，然后先启动一次再退出。
3. 把压缩包里 `mods\` 下的**两个** JAR 都复制到该游戏目录的 `mods\` 文件夹：
   - `hostile_spawn_control-1.0.0.jar`（本 Mod）
   - `fabric-api-0.161.0+26.3.jar`（依赖）
4. 用 Fabric 配置启动游戏。

## 使用方法

### 游戏内界面（单人游戏）
- 进入世界后按 **K** 打开「敌对生物生成」界面（可在 选项 → 控制 → 按键绑定 →「敌对生物生成控制」中修改按键）。
- 每行一种敌对生物：`开` = 保持原版自然生成，`关` = 不再自然生成。
- 顶部搜索框支持中文名、英文名或实体 ID（如 `苦`、`creeper`、`minecraft:creeper`）。
- 底部「全部开启 / 全部关闭」可批量切换；点「完成」时保存。
- 如果连接的是别人的服务器，界面为只读，需要服务器管理员使用命令修改。

### 命令（单人需开启作弊；服务器需 OP 2 级）
| 命令 | 作用 |
|---|---|
| `/spawncontrol list` | 列出所有敌对生物及其开关状态 |
| `/spawncontrol set <生物ID> natural <true\|false>` | 开/关某种生物的自然生成，例如 `/spawncontrol set minecraft:creeper natural false` |
| `/spawncontrol enableall` / `disableall` | 全部开启 / 全部关闭 |
| `/spawncontrol reload` | 手动编辑配置文件后重新加载 |
| `/spawncontrol status` | 统计：已关闭数量、本次启动以来拦截次数 |

### 配置文件
位置：`<游戏目录>\config\hostile_spawn_control.json`（全局生效，对所有存档有效）。只记录与默认值不同的项：

```json
{
  "version": 1,
  "mobs": {
    "minecraft:creeper": { "natural": false },
    "minecraft:phantom": { "natural": false }
  }
}
```

删除该文件 = 恢复原版行为。

## V1 功能边界

受控制（关闭后不再出现）：
- 夜晚 / 洞穴 / 下界 / 末地 / 特定生物群系的**自然刷怪**
- 区块生成时的自然生成
- 幻翼（失眠机制生成，也属于自然生成）

**不**受控制（保持原版）：
- `/summon`、刷怪蛋、发射器
- 刷怪笼、试炼刷怪笼
- 已经存在的生物、区块重新加载的生物
- 结构生成（如海底神殿守卫者、女巫小屋等）、袭击 / 巡逻队等事件生成、转化（如村民→女巫）

## 工作原理

- **生物识别**：启动后从实体注册表中自动筛选分类为 `MONSTER` 的实体，不维护固定名单；新版本或其他 Mod 加入的敌对生物会自动出现（26.3 原版共识别到 45 种）。
- **规则模型**：底层为「生物 × 生成来源」（自然、刷怪笼、结构、事件、转化、刷怪蛋、命令），V1 只开放并执行「自然生成」。
- **拦截方式**：在生成阶段拦截，而不是生成后删除。
  - `SpawnPlacements.checkSpawnRules`：自然刷怪在创建实体之前的规则检查，直接返回“不可生成”；
  - `EntityType.create`：对自然 / 区块生成原因返回空，覆盖不经过上面检查的自然生成（如幻翼）。

## Windows 实机测试清单

1. 新建一个**创造 + 作弊开启**的临时世界，难度设为困难。
2. 按 K 打开界面，确认列表显示中文名称，搜索「苦」能过滤出苦力怕。
3. 把**僵尸**、**骷髅**以外的全部关闭（先点「全部关闭」，再单独打开僵尸、骷髅），点「完成」。
4. `/time set night`，切换生存模式，在平原等待几分钟：新刷出的敌对生物应只有僵尸和骷髅，不再出现苦力怕、蜘蛛、女巫等。
5. `/summon minecraft:creeper` 与苦力怕刷怪蛋仍然可以生成苦力怕。
6. `/spawncontrol status` 能看到拦截次数在增长。
7. 退出重进游戏，按 K 确认设置仍保留；检查 `config\hostile_spawn_control.json`。
8. 点「全部开启」后，刷怪恢复原版行为。

出现问题时，请收集该实例的 `logs\latest.log` 和 `crash-reports\` 最新文件。

## 从源码构建（Mac）

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./gradlew build
```

产物：`build/libs/hostile_spawn_control-1.0.0.jar`（不要使用 `-sources.jar`）。
