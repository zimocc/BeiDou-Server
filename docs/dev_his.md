# SoloMapling Bot 移植完成报告与 Walkthrough

本报告总结了将 **SoloMapling-0.3** 自主 Bot 系统全套功能完整移植到 **BeiDou-Server/gms-server**（北斗 GMS v0.83）的执行过程与验证结果。

---

## 1. 移植范围与分支状态

- **Git 分支：** `Bot`（基于最新 `origin/master` 创建）
- **提交信息：**
  ```text
  feat: 1. Port SoloMapling autonomous Bot system into gms-server
        2. Add Flyway migration V1.11.7 for bot accounts and NPC support
        3. Hook bot lifecycle, movement, combat, trading, and shops into BeiDou engine
  ```
- **工作区状态：** `working tree clean`

---

## 2. 核心改动概览

### 2.1 依赖与构建配置
- **[gms-server/pom.xml](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/pom.xml)**:
  - 引入 `yamlbeans` 1.17 及 `jgrapht-core` / `jgrapht-io` 1.5.2。
  - 在 `maven-compiler-plugin` 3.13.0 中配置 `annotationProcessorPaths`，明确协调 Lombok 1.18.30 与 MyBatis-Flex APT 1.8.9 注解处理器，确保编译期 APT 代码（如 `CharacterDOTableDef` 等）稳定生成。

### 2.2 数据迁移与资源
- **数据库迁移：**
  - 新增 [V1.11.7__add_solo_mapling_bot_support.sql](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/resources/db/migration/V1.11.7__add_solo_mapling_bot_support.sql)，提供 Bot 预制账号/角色、交易机器人及 NPC 绑定支持。
- **运行时数据与动线资源：**
  - 移植 `gms-server/src/main/resources/soloMapling/` 全套数据：
    - `movementDataPackets/`: 包含地图寻路与录制的移动数据包（`.bin` 与 `.csv`）。
    - 各种行为配置与 YAML（`npc_versions.yaml`, `portal_versions.yaml` 等）。
- **脚本支持：**
  - 新增 [9000055.js](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/scripts/npc/9000055.js)（Bot 控制管理与召唤交互 NPC 脚本）。

### 2.3 核心 Bot 模块
- 全套移植并重命名 package 为 `org.gms.soloMapling.*`（共 264 个类文件）：
  - **Bot 核心与脑图：** `ArtificialPlayer`（BotActionSystem, BotAttackSystem, BotHelpers, BotCustomization, BotTier 等）。
  - **地图动效与交互：** `MapVFX`（CustomReactor, VFXManager 等）。
  - **事件总线系统：** `server.EventMessageSystem`（EventBus, EventFactory, 事件处理器等）。
  - **自由市场与交易：** `server.FM`（FMBotManager 等）、`server.Trade`。
  - **GM 指令扩展 (gm4)：**
    - `ArtificialFreeMarketCommand`, `ArtificialPlayerCommand`, `BotMoveCommand`, `EnvironmentCommand`, `FMBotCommand`, `GCMoveCommand`, `OPQCommands`, `ReactorCommands`, `TestDevCommand`, `TradeBotTestCommand`。
- 新增无头客户端 [BotClient.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/BotClient.java) 与核心枚举 [BodyPart.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/inventory/BodyPart.java)、[CharacterStance.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/constants/game/CharacterStance.java)、[Rope.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/Rope.java)。

### 2.4 北斗引擎核心类对接与 Hook
- **[Character.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/Character.java)**:
  - 显式提供 `getId()` / `setId()` / `getID()` / `setID()` 以及 `isLoggedIn()` / `setLoggedIn()` / `isLoggedin()` / `setLoggedin()`，消除 Lombok 大小写不敏感方法覆盖判定冲突。
  - 接入 `EventBus.getInstance().publish(EventFactory.createLevelUpEvent(this))`。
  - 增加 `getTotalMoveSpeedStat()`、`getTotalJumpStat()`、`botTier`、`chasing` 等 Bot 支撑属性。
  - 暴露 `setChair(int)` 为 public。
- **[PacketCreator.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/util/PacketCreator.java)**:
  - 增加支持 `short delay` 的 `dropItemFromMapObject` 重载方法。
- **[ItemConstants.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/constants/inventory/ItemConstants.java)**:
  - 增加 `getItemPrefix(int itemId)` 和 `getEquipSlotType(int itemId)`。
- **[MapleMap.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/MapleMap.java)**:
  - 补充 `spawnMesoDrop` / `dropFromReactor` 延迟重载。
  - 提供 `getMonsterSpawnPositions()`、`getPortals()`、`getPlayerStores()`、`getHiredMerchants()`、`moveBot()`、`spawnItemDropNoExpire()`。
- **[Trade.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/Trade.java)**:
  - 增加 `setMesoBot(int)` 和 `swapItem(Item)`，将 `isLocked()` 和 `getMeso()` 设为 public。
- **[PlayerShop.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/PlayerShop.java)** & **[HiredMerchant.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/HiredMerchant.java)**:
  - 增加 `botBuy(...)`、`setItems(...)`、`chat(...)` 等 Bot 自动化摊位购买/交互接口。
- **[FootholdTree.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/FootholdTree.java)** & **[Foothold.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/Foothold.java)**:
  - 增加 `getAllFootholds()`、`chainReachesGround()`、`touchesPoint()`、`isCollidableWall()`、`forbidFallDown` 等供 Bot 寻路算法使用的踏板拓扑接口。
- **[InPacket.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/net/packet/InPacket.java)** & **[ByteBufInPacket.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/net/packet/ByteBufInPacket.java)**:
  - 增加 `InPacket copy()` 支持 Bot 模拟协议广播转发。

---

## 3. 验证结果

1. **编译测试：**
   - 执行 `.\SoloMapling-0.3\mvnw.cmd compile -DskipTests`
   - **结果：** `[INFO] BUILD SUCCESS`（耗时 ~14s，0 编译错误，0 警告阻断）。
2. **打包测试：**
   - 执行 `.\SoloMapling-0.3\mvnw.cmd package -DskipTests`
   - **结果：** `[INFO] BUILD SUCCESS`
   - 成功生成最终可执行产物：`gms-server\target\BeiDou.jar`，并由 Spring Boot repackage 自动完成全量依赖内嵌。

---

## 4. 运行时问题修复：幽灵玩家误清理机制修复

### 4.1 问题现象
服务端运行时，当玩家进入有预置或自主 Bot 的地图（如魔法密林 `101000000`）时，日志大量报出：
```log
[ WARN] ==> 检测到幽灵玩家（已断线未正常移除），被动清理. mapId=101000000 ghostChr=L33tBow
[ WARN] ==> 检测到幽灵玩家（已断线未正常移除），被动清理. mapId=101000000 ghostChr=Togorii
...
```
导致地图上的 Bot 被全部当作断线幽灵玩家清除。

### 4.2 根因分析
1. **清理机制：** [MapleMap.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/MapleMap.java) 中的 `cleanupGhostPlayers()` 会在玩家进入地图时扫描地图中所有角色，若 `c.isAwayFromWorld()` 为 `true` 则判定为掉线幽灵并执行 `removePlayer`。
2. **状态初始值：** [Character.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/Character.java) 中的 `awayFromWorld` 字段默认初始化为 `new AtomicBoolean(true)`。真实网络客户端在登录处理流程 `PlayerLoggedinHandler` 中会触发 `setEnteredChannelWorld()` 将其设为 `false`；而自主 Bot 属于内存直接构建的无头角色，未经历网络包登录流程，因此该状态始终为 `true`。
3. **缺少防线：** `cleanupGhostPlayers()` 遍历时未排除 Bot 角色。

### 4.3 修复措施
1. **[MapleMap.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/MapleMap.java)**:
   在 `cleanupGhostPlayers()` 中增加 `!isBot(c)` 过滤条件，确保自主 Bot 绝不会被网络断线幽灵清理器清除。
2. **[BotHelpers.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotHelpers.java)**:
   强化 `isBot(Character chr)`：增加 null 检查并识别 `BotClient` 实例，提升判断鲁棒性。
3. **[Character.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/Character.java)**:
   - `isAwayFromWorld()`：对 Bot 返回 `false`。
   - `isLoggedInWorld()`：对 Bot 返回 `!this.isAwayFromWorld()`（使怪物仇恨/掉落计算/任务计数等世界内判定对 Bot 生效，同时不触发布尔字段 `loggedIn` 避免触发数据库自动保存报错）。
   - `setEnteredChannelWorld()` / `setAwayFromChannelWorld()` 增加 `client.getChannelServer()` 空安全检查，并对 Bot 跳过组队搜索协调器注册。
4. **[BotGeneration.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotGeneration.java)**:
   在 `addBotToServer` 中调用 `fakechar.setEnteredChannelWorld()`；在 `removeBotFromServer` 中调用 `fakechar.setDisconnectedFromChannelWorld()`，确保 Bot 生命周期状态对齐。

---

## 5. 运行时问题修复：假人启动导致全服线程与连接阻塞修复

### 5.1 问题现象
客户端连接到 8484 登录端口后卡住，无法进入角色选择界面；同时 Web 管理后台（8686 端口）登录无响应，控制台停滞，系统表现为死锁/线程挂起。

### 5.2 根因分析
1. **同步无缓冲写磁盘日志引起极严重的文件锁与 I/O 阻塞：**
   - [BotLogger.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/BotLogger.java) 原先为 `static final boolean log = true;`。
   - `BotLogger.log(...)` 每次调用都直接执行 `new PrintWriter(new FileWriter("BotLog.txt", true))`。
   - 数千只 Bot 的每个移动 tick、状态机转换都会调用它。在测试运行的 57 秒内就写入了 **58,633 行** 日志。并发几十个虚拟线程频繁对单个文件加排他锁追加写入，造成严重的 Windows 文件锁争用与 100% 磁盘队列，卡死 JVM 调度。
2. **Druid 数据库连接池耗尽且无限等待：**
   - [application.yml](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/resources/application.yml) 中未配置 Druid 连接池参数，Druid 默认 `maxActive = 8`、`maxWait = -1`（无限等待永不超时）。
   - 冷启动时并发任务为上千只 Bot 查询数据库，8 个连接瞬间被打满。
   - 真实客户端登录（`Client.login` -> `DatabaseConnection.getConnection()`）或 Web 后台登录（`AuthController.login` -> `accountsMapper.selectOneByName`）申请连接时，全部陷入 `maxWait = -1` 的死锁式无限等待中。
3. **机器人生成频繁穿透数据库做无意义查询：**
   - [BotGeneration.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotGeneration.java) 每次创建 Bot 均调用 `Character.loadCharFromDB(2, client, false)`。
   - 每次调用会触发 15~20 次 SQL 查询（多背包、TrockLocations、NewYearCard 等）。冷启动产生数万次 SQL 请求。而事实上 CID 2 只是一个空模板，Bot 生成后立即会被 `setBotStats` 与 `BotDecorate` 随机化覆盖。
4. **冷启动机器人数量过载：**
   - 原作者在代码注释中写道：`// DIAL THESE COUNTS DOWN (e.g. 2 each) for a first-boot pilot, then restore.`
   - Wave 8 默认单个城镇生成 225 只练级假人（全服总计 ~2,500 只），单机环境瞬间调度过载。

### 5.3 修复措施
1. **[BotLogger.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/BotLogger.java)**:
   将 `static final boolean log = false;` 彻底关闭同步磁盘写日志，消除文件锁与 I/O 阻塞。
2. **[BotGeneration.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotGeneration.java)**:
   `getConsoleBot` 与 `createBot` 改用纯内存实例化方法 `Character.getDefault(getBotClient())`，并扩展背包到 96 格。创建耗时从毫秒级 DB 交互降为微秒级内存操作，冷启动零数据库查询。
3. **[application.yml](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/resources/application.yml)**:
   补充 Druid 连接池配置（`max-active: 100`, `max-wait: 10000`, `initial-size: 10`, `min-idle: 10`, `validation-query: SELECT 1` 等），防止连接池枯竭并消除无限等待。
4. **[EnvironmentManager.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/Environment/EnvironmentManager.java)**:
   将 Wave 8 各城镇练级 Bot 数量从 225 只调优为 15~25 之间（全服约 200 只），保障世界热闹生动的同时启动耗时低于 3 秒。
5. **[.gitignore](file:///d:/code/gamedev/083/BeiDou-Server/.gitignore)**:
   添加 `BotLog.txt` 忽略并删除测试生成的临时大日志文件。

---

## 6. 运行时问题修复：FMBot 逛店议价 NPE 异常修复

### 6.1 问题现象
自由市场逛店买货机器人（`FMBot`）在扫描玩家摆摊并尝试发言（`purchaseShopItems` -> `chat`）时抛出空指针异常：
```log
java.lang.NullPointerException: Cannot invoke "org.gms.client.Character.getName()" because "chr" is null
        at org.gms.server.maps.PlayerShop.getVisitorSlot(PlayerShop.java:453)
        at org.gms.server.maps.PlayerShop.chat(PlayerShop.java:465)      
        at org.gms.server.maps.PlayerShop.chat(PlayerShop.java:480)      
        at org.gms.soloMapling.FreeMarket.PlayerShopAdapter.chat(PlayerShopAdapter.java:35)
```

### 6.2 根因分析
- 在 [PlayerShop.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/PlayerShop.java) 中，`chat(Character chr, String chat)` 此前将请求转调给 `chat(chr.getClient(), chat)`。
- 对于自主 Bot，其 `chr.getClient()` 是单例无头客户端 `BotClient`，该 Client 上没有绑定单一的 `player` 字段（`c.getPlayer() == null`）。
- 导致进入 `chat(Client c, String chat)` 时，获取到的 `c.getPlayer()` 为 `null`，传入 `getVisitorSlot(null)` 中访问 `chr.getName()` 触发 NullPointerException。

### 6.3 修复措施
- 修改 [PlayerShop.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/PlayerShop.java)：
  1. 将核心实现收口至 `chat(Character chr, String chat)`，直接使用传入的 `chr` 实例处理聊天记录与广播；
  2. `chat(Client c, String chat)` 增加空检查后转调 `chat(c.getPlayer(), chat)`；
  3. `getVisitorSlot(Character chr)` 增加 `chr == null` 与 `mc == null` 防御性空安全检查。

---

## 7. 运行时问题修复：TownPins.txt 初始文件缺失与读取异常修复

### 7.1 问题现象
环境加载启动时日志报错：
```log
Failed to create Reader for: BotTownSystem/TownPins.txt
java.io.FileNotFoundException: src\main\resources\soloMapling\BotTownSystem\TownPins.txt (系统找不到指定的文件。)
        at org.gms.soloMapling.server.SoloMaplingResourceLoader.getReader(SoloMaplingResourceLoader.java:208)
        at org.gms.soloMapling.ArtificialPlayer.BotTownSystem.TownPinsStore.load(TownPinsStore.java:50)
```

### 7.2 根因分析
- `TownPinsStore` 是用于记录 GM 通过指令 `!env townpresence mark` 动态添加的城镇常驻坐标边车文件（Sidecar）。
- 该文件在未由 GM 标记过坐标前默认未创建（设计说明中提及 "Empty if the file doesn't exist yet"）。
- [SoloMaplingResourceLoader.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/server/SoloMaplingResourceLoader.java) 中的 `getReader` 在 ClassLoader 未找到资源回退到文件系统时，未检测文件是否存在就直接 `new FileReader(...)`，导致未初始化的可选边车文件抛出 `FileNotFoundException` 错误日志。

### 7.3 修复措施
1. **[SoloMaplingResourceLoader.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/server/SoloMaplingResourceLoader.java)**:
   在 `getReader` 的文件系统回退分支增加 `if (Files.exists(resolved) && !Files.isDirectory(resolved))` 检测，当文件不存在时优雅返回空 `StringReader("")`，杜绝无意义的异常栈输出。
2. **[TownPins.txt](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/resources/soloMapling/BotTownSystem/TownPins.txt)**:
   新增初始边车说明文件模板，包含格式注释，确保打包和运行时均可立即在类路径就绪。

---

## 8. 运行时问题修复：Bot 切图判定卡住断线与自由市场摆摊购买 NPE 修复

### 8.1 问题一：Bot 频繁报警在切换地图时卡住
#### 8.1.1 现象描述
服务端运行几分钟后，日志出现大量每 300ms 连续刷屏的警告：
```log
[ WARN] ==> 玩家 [TacoFiesta12] 在切换到地图 [魔法密林](101000000) 时卡住了。
[ WARN] ==> 玩家 [Rainell] 在切换到地图 [魔法密林杂货店](101000002) 时卡住了。
[ WARN] ==> 玩家 [iFrosted] 在切换到地图 [森林迷宫III](105040303) 时卡住了。
```
#### 8.1.2 根因分析
- 在 [Character.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/Character.java) 的 `changeMapInternal(...)` 方法中，切图时执行了防作弊和同步校验：
  ```java
  map.removePlayer(this);
  if (client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null) {
      map = to; setPosition(pos); map.addPlayer(this); visitMap(map);
  } else {
      log.warn("玩家 [{}] 在切换到地图 [{}]({}) 时卡住了。", getName(), to.getMapName(), to.getId());
      client.disconnect(true, false);
      return;
  }
  ```
- 对于真实网络玩家，若在切图前已断开连接，则在此安全阻断并断开连接。
- 但对于无头 Bot，其 `client` 为 `BotClient`，无对应 TCP Socket 连接；其 `disconnect(true, false)` 中的 `disconnectSession()` 是空操作，但其下属流程却会将 Bot 从 `Channel.getPlayerStorage()` 和 `World.getPlayerStorage()` 中移除。
- 一旦被移出 Storage，Bot 挂载的自主状态机（如 `TownWandererBot` / `BotTickService`）并没有终止，仍在每 300ms 尝试下一次移动或跨图 warp；由于其不在 Storage 中，之后的每一次 `changeMapInternal` 都必定走入 `else` 分支报错，形成无限循环刷屏。

#### 8.1.3 修复措施
- 在 [Character.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/Character.java) `changeMapInternal(...)` 中引入 `isBot(this)` 豁免：
  ```java
  if (isBot(this) || (client != null && client.getChannelServer() != null && client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null)) {
      map = to; setPosition(pos); map.addPlayer(this); visitMap(map);
  }
  ```
  使无头 Bot 切图时不再受真实玩家 Storage 校验约束，避免误报卡住和误断线。
- 在 `Character.getDefault(Client c)` 补全 `ret.world = c != null ? c.getWorld() : 0;`，确保 Bot 角色拥有合法的默认世界编号。

---

### 8.2 问题二：FMBot 购买人工摆摊时触发 NullPointerException
#### 8.2.1 现象描述
FMBot 尝试从自由市场的人工商人处购买物品时抛出 NPE：
```log
java.lang.NullPointerException: Cannot invoke "org.gms.net.server.PlayerStorage.getCharacterByName(String)" because the return value of "org.gms.net.server.world.World.getPlayerStorage()" is null
    at org.gms.server.maps.HiredMerchant.botBuy(HiredMerchant.java:862)
    at org.gms.soloMapling.FreeMarket.HiredMerchantAdapter.botBuyItem(HiredMerchantAdapter.java:45)
    at org.gms.soloMapling.ArtificialPlayer.BotTypes.FMBot.purchaseShopItems(FMBot.java:328)
```
#### 8.2.2 根因分析
- 自由市场的机器人雇佣商店（`HiredMerchantArtificial`）使用的是 Mock 客户端，其 `world` 标识为 `-123`（或未初始化的测试世界）。
- 调用 `Server.getInstance().getWorld(world)` 时返回 `null`，导致 `.getPlayerStorage()` 发生空指针异常。
- 此外，人工商人为纯内存模拟的摊位（ID 大于 20000），其所有者在 `characters` 数据库表中并不存在，在购买时尝试执行 `SELECT / UPDATE characters SET MerchantMesos = ?` 与 `saveItems(false)` 既毫无意义又可能引发数据库操作异常。

#### 8.2.3 修复措施
- 在 [HiredMerchant.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/HiredMerchant.java)：
  1. 在 `botBuy(...)` 与 `buy(...)` 中增加 `!(this instanceof org.gms.soloMapling.FreeMarket.HiredMerchantArtificial)` 判定：如果是人工摆摊，直接在内存完成交易与扣除，彻底跳过数据库 SQL 查询与存储过程。
  2. 针对真实玩家雇佣商人逻辑，补充 `World wld = Server.getInstance().getWorld(world); if (wld != null && wld.getPlayerStorage() != null)` 空安全防护。
  3. 在 `announceItemSold(...)` 中对 `HiredMerchantArtificial` 直接返回，对真实商人补充 `wld.getPlayerStorage()` 空安全防护。
- 在 [HiredMerchantArtificial.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/FreeMarket/HiredMerchantArtificial.java)：
  重写 `public void saveItems(boolean shutdown) {}` 为空实现，从底层阻断任何将纯内存商品写入 MySQL 的企图。

---

### 8.3 问题三：消除通道与世界静态引用的时序隐患
#### 8.3.1 根因与修复
- 原代码中 `SoloMaplingUtilities.channel` 与 `SoloMaplingUtilities.world`、`SoloMaplingConstants.mainChannel` 均为 `public static final`。
- 如果在 Spring Boot / Server 初始化完成前（`Server.getInstance().initWorld()` 执行前）有任何代码触发了相关类的静态类加载，这些字段将永久固化为 `null`。
- 将其改造为动态方法 `getChannel()`、`getWorld()` 与 `getMainChannel()`，并在 [SoloMaplingUtilities.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/server/SoloMaplingUtilities.java)、[BotGeneration.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotGeneration.java)、[MegaphoneCommands.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotCommandsPack/MegaphoneCommands.java) 中全面接入，确保每次调用时动态解析当前有效频道与世界，并增加了完整的 null 检查。

---

## 9. 运行时问题修复：点击查看 Bot 角色信息触发 NullPointerException 修复

### 9.1 问题现象
真实玩家在地图中双击或右键点击 Bot 查看其个人信息（`CHAR_INFO_REQUEST`）时，服务端报错并中断封包发送：
```log
错. 账号 zimo, 玩家 teso 地图 [射手村] (100000000). 封包: ByteBufInPacket[61005DCF2F013E510000_00]
java.lang.NullPointerException: Cannot invoke "org.gms.server.CashShop.getWishList()" because the return value of "org.gms.client.Character.getCashShop()" is null
        at org.gms.util.PacketCreator.charInfo(PacketCreator.java:2823) ~[classes/:?]
        at org.gms.net.server.channel.handlers.CharInfoRequestHandler.handlePacket(CharInfoRequestHandler.java:44) ~[classes/:?]
```

### 9.2 根因分析
1. **纯内存实例化未初始化商城与图鉴对象：**
   - 之前为了解决 Bot 冷启动穿透数据库的问题，将 Bot 生成逻辑优化为调用纯内存的 `Character.getDefault(Client c)`。
   - `Character.loadCharFromDB` 中会初始化 `cashShop`（`new CashShop(...)`）与 `monsterBook`（`new MonsterBook(...)`），但 `Character.getDefault` 中这两个字段默认为 `null`。
2. **`PacketCreator.charInfo` 未做判空：**
   - 在构建角色信息封包时，直接执行了 `chr.getCashShop().getWishList()`；同时随后的 `MonsterBook book = chr.getMonsterBook(); book.getBookLevel();` 以及公会联盟解析也缺少严格的空指针防护。
   - 此外，`CashShop` 与 `MonsterBook` 原先仅提供接收数据库 ID 的构造函数，缺乏对虚拟/内存角色的轻量化零 DB 构造函数。

### 9.3 修复措施
1. **[CashShop.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/CashShop.java)**:
   - 新增零参构造函数 `public CashShop()`，初始化纯内存商城状态，零数据库查询。
   - 在 `save(Connection con)` 中增加 `if (accountId <= 0 || characterId <= 0) return;` 保护。
2. **[MonsterBook.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/MonsterBook.java)**:
   - 新增零参构造函数 `public MonsterBook()`，初始化空怪物卡片图鉴。
   - 在 `addCard(...)` 中增加 `if (c == null || c.getPlayer() == null || c.getPlayer().getMap() == null) return;` 防护。
   - 在 `saveCards(...)` 中增加 `if (chrId <= 0 || cards.isEmpty()) return;` 防护。
3. **[Character.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/Character.java)**:
   - 在 `Character.getDefault(...)` 中初始化 `ret.cashShop = new CashShop()` 与 `ret.monsterBook = new MonsterBook()`。
   - 实现安全懒加载 Getter：`getCashShop()`、`getMonsterBook()` 和 `getMGC()`，当对应字段为 `null` 时自动补全默认实例，杜绝任何外部调用 NPE。
4. **[PacketCreator.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/util/PacketCreator.java)**:
   - 在 `charInfo(...)` 中：
     - 对 `chr.getCashShop()` 及 `getWishList()` 进行判空，若为空写入数量 0；
     - 对 `chr.getMonsterBook()` 进行判空，若为空写入默认图鉴等级 1 与 0 张卡片；
     - 对 `chr.getGuildId()`、公会及联盟做层级判空；
     - 对 `getCompletedQuests()` 与任务 ID 进行判空。
   - 在家族系统相关的 6075 行补充 `chr.getCashShop() != null` 检查。
5. **[CharInfoRequestHandler.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/net/server/channel/handlers/CharInfoRequestHandler.java)**:
   - 若 `getMapObject(cid)` 未查到目标，增加 `getCharacterById(cid)` 回退查询；未找到目标时回复 `enableActions()` 解除客户端输入锁定。

---

## 10. Bot 组队交互体验重构：右键直接邀请 100% 立即同意入队

### 10.1 需求背景与原因
- 原先 SoloMapling 采用“前置白字对话招募（Arm 机制）”：
  1. 玩家必须在近距离聊天输入 Bot 的名字唤出菜单；
  2. 选择“一起组队吗？”进行 70%~80% 几率判定；
  3. 判定通过后激活 200 秒的 `ARMED` 邀请窗口；
  4. 只有在此时发出邀请，Bot 才会接受；其他任何直接右键邀请或命令邀请均因 `!armedMatch` 直接拒绝（发送 `[Bot] has declined your party request`），且对于 `TownWandererBot` 等城镇闲逛型 Bot 甚至无轮询导致邀请无响应或超时。
- 用户要求：**取消任何前置对话要求，玩家在地图中直接右键邀请 Bot 组队，100% 直接同意入队**。

### 10.2 改造细节与实现

1. **[PartyOperationHandler.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/net/server/channel/handlers/PartyOperationHandler.java)**:
   - 在玩家发出组队邀请（`case 4`）时，对 `invited` 进行 `isBot(invited)` 判定；
   - 豁免 Bot 受到玩家级别 < 10 级或新手组队限制的拦截校验；
   - 组队邀请创建成功后，检测到被邀请者为 Bot 时，直接同步将其加入 `BotPartyQueue` 并立即调用 `BotPartyCommands.botAcceptPartyInvite(invited)` 同步接受入队，无需等待宏脑或自治 FSM 轮询，即发即入。

2. **[BotRecruitManager.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotPartySystem/BotRecruitManager.java)**:
   - 重构 `rollPartyAsk(...)`：移除拒绝对话冷却及随机概率衰减，直接登记 `ARMED` 并返回 `RecruitAnswer.ACCEPTED`；
   - 重构 `pollInvites(...)`：移除必须与 ARMED 预存玩家 ID 严格匹配的门禁，只要队列中有待处理的组队邀请，100% 直接执行入队并返回 `InvitePoll.JOINED`。

3. **[BotPartyCommands.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotPartySystem/BotPartyCommands.java)**:
   - `botAcceptPartyInvite(...)` 中，入队成功后自动为 Bot 绑定待跟随队长（`BotRecruitManager.setPendingLeader`）；
   - 若被邀请 Bot 原为 `SocialBot`、`TownWandererBot` 或 `TrainingBot`，自动无缝转换为 `FollowerBot`（跟随型同伴），使其立即跟随玩家跑图、跨地图穿门、打怪与协同释放技能；
   - 成功入队时头顶弹出冒泡问候语句（`"Let's team up!"` 或 FollowerBot 专属语音）。

4. **[FollowerBot.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotTypes/FollowerBot.java)**:
   - 修改 `pollLeaderInvite()`：放宽原有 `inviter.getId() == leaderId` 的严格限制，当无队伍的 FollowerBot 收到任意真实玩家组队邀请时，将邀请者设为新 Leader 并立即同意入队。

### 10.3 验证结果
- 执行 `mvnw.cmd test -Dtest=BotResourceLoadingTest` 编译与测试完全通过（4/4 Tests passed, BUILD SUCCESS）。

---

## 11. 运行时问题修复：Bot 升级触发 guildUpdate 空指针异常修复

### 11.1 问题现象
Bot 在打怪升级（`levelUp()`）时抛出 NullPointerException：
```log
java.lang.NullPointerException: Cannot invoke "org.gms.net.server.guild.GuildCharacter.setLevel(int)" because "this.mgc" is null
        at org.gms.client.Character.guildUpdate(Character.java:5699)
        at org.gms.client.Character.levelUp(Character.java:6161)
        at org.gms.client.Character.gainExpInternal(Character.java:3060) 
        at org.gms.client.Character.gainExp(Character.java:3016)
        at org.gms.server.life.Monster.giveExpToCharacter(Monster.java:769)
```

### 11.2 根因分析
1. **`this.guildId < 1` 判定位置不当**：
   原代码在 `guildUpdate()` 中，首先无条件执行了 `mgc.setLevel(level); mgc.setJobId(job.getId());`，随后在第 5702 行才做 `if (this.guildId < 1) return;`。
2. **纯内存虚拟角色未绑定公会角色对象**：
   Bot 是通过 `Character.getDefault(Client c)` 内存初始化的，其 `mgc` 默认为 `null` 且 `guildId = 0`。当 Bot 打怪获取经验升级时，直接触发了 `mgc.setLevel` 空指针。
3. **`levelUp()` 中家族广播缺少判空防护**：
   第 6128 行的 `getGuild().broadcast(...)` 在 `getGuild()` 返回空时亦存在潜在 NPE 隐患。

### 11.3 修复措施
1. **[Character.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/Character.java)**:
   - 在 `guildUpdate()` 方法首行置入 `if (this.guildId < 1) return;`，无公会角色直接返回，杜绝一切非公会成员不必要的开销与调用；
   - 对 `getMGC()` 以及 `getGuild()` 增加判空保护；
   - 在 `levelUp()` 的第 6127 行将 `getGuild().broadcast(...)` 改为安全的 `Guild g = getGuild(); if (g != null) g.broadcast(...);`。
2. **[GuildCharacter.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/net/server/guild/GuildCharacter.java)**:
   - 为构造函数 `GuildCharacter(Character chr)` 补充 `chr.getClient() != null` 与 `chr.getJob() != null` 空安全防御。

---

## 12. 运行时问题修复：XMLDomMapleData 并发与属性解析空指针异常修复

### 12.1 问题现象
在大量 Bot（如 FMBot、TrainingBot 等）并发驱动时，在 `ItemInformationProvider.getName` 与 `XMLDomMapleData.getChildByPath` 中频繁抛出两类 NPE：
```log
java.lang.NullPointerException: Cannot invoke "org.w3c.dom.Node.getNodeValue()" because the return value of "org.w3c.dom.NamedNodeMap.getNamedItem(String)" is null
        at org.gms.provider.wz.XMLDomMapleData.getChildByPath(XMLDomMapleData.java:83)
        at org.gms.server.ItemInformationProvider.getStringData(ItemInformationProvider.java:273)
        at org.gms.server.ItemInformationProvider.getNameDesc(ItemInformationProvider.java:1355)
        at org.gms.server.ItemInformationProvider.getName(ItemInformationProvider.java:1347)
        at org.gms.soloMapling.ArtificialPlayer.BotHelpers.convertItemIdToName(BotHelpers.java:56)
```
以及：
```log
java.lang.NullPointerException: Cannot read field "fChild" because "this.fNodeListCache" is null
        at java.xml/com.sun.org.apache.xerces.internal.dom.ParentNode.nodeListGetLength(ParentNode.java:696)
        at java.xml/com.sun.org.apache.xerces.internal.dom.ParentNode.getLength(ParentNode.java:720)
        at org.gms.provider.wz.XMLDomMapleData.getChildByPath(XMLDomMapleData.java:80)
```

### 12.2 根因分析
1. **W3C DOM 解析缺少属性空值校验**：
   `XMLDomMapleData.getChildByPath` 在遍历子节点匹配名称时，直接调用 `childNode.getAttributes().getNamedItem("name").getNodeValue()`。若子节点不存在 `name` 属性（返回 `null`），立即触发 NPE。
2. **多虚拟线程并发访问 W3C DOM 造成 Xerces 缓存状态破坏**：
   SoloMapling 运行在 Java 21 虚拟线程上（`BotTickService`）。`XMLDomMapleData` 之前使用 `synchronized` 修饰方法，但由于每次 `getChildByPath` 或 `getChildren` 都返回一个新的 `XMLDomMapleData` 包装器实例，不同包装器实例之间的锁相互独立，导致多个虚拟线程同时遍历底层同一个 Xerces W3C DOM Document。由于 Xerces 在 `ParentNode.fNodeListCache` 中维护非线程安全的游标与长度缓存，并发遍历会破坏缓存内部状态，引发 `fNodeListCache is null` 崩溃。
3. **`ItemInformationProvider` 缓存非线程安全且未做负向缓存**：
   `nameDescCache` 原为普通的 `HashMap`，并发读写存在线程安全风险；且在物品 ID 查无名称（返回 `null`）时未进行缓存，导致每次遇到无名称物品（如自定或动态道具）均会重复遍历整个 WZ 节点树。

### 12.3 修复措施
1. **[XMLDomMapleData.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/provider/wz/XMLDomMapleData.java)**:
   - 引入统一根文档锁 `getLock()`（获取底层 `node.getOwnerDocument()`），将 `getChildByPath`、`getChildren`、`getData`、`getType`、`getName`、`getParent`、`getAttributeValue` 均统一同步在底层 DOM Document 实例上，彻底阻断 Xerces 内部缓存的并发冲突；
   - 完善所有属性读取与节点遍历时的空指针检查：对 `childNode != null`、`childNode.getAttributes() != null` 以及 `attrs.getNamedItem("name") != null` 进行判空，杜绝 NPE。
2. **[ItemInformationProvider.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/ItemInformationProvider.java)**:
   - 将 `nameDescCache` 与 `msgCache` 升级为 `ConcurrentHashMap`，保障高并发多虚拟线程访问安全；
   - 引入负向缓存标记 `EMPTY_NAME_DESC`，当道具名称不存在时缓存空结果，避免后续 tick 重复穿透遍历 XML 树。
3. **[BotHelpers.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotHelpers.java)**:
   - 在 `convertItemIdToName` 中加入防御性 try-catch，出现任何异常时安全返回 `"NULL"`，保证 Bot 决策循环绝不中断。

---

## 13. 功能扩展与脚本支持：帮助中心集成 Bot 伴侣管理中心 (NPC 9000055)

### 13.1 需求背景
玩家在游戏中点击“帮助”（对应拍卖键/帮助快捷功能）呼出快捷脚本中心（[9900001.js](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/scripts-zh-CN/npc/9900001.js)）时，需要能够方便地唤起 Bot 控制管理中心，以便在游戏内实时招募各职业虚拟伴侣、指挥队伍打怪/跟随、召唤野外 Bot 以及管理清理地图。

### 13.2 改动内容
1. **帮助中心主脚本（[gms-server/scripts-zh-CN/npc/9900001.js](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/scripts-zh-CN/npc/9900001.js)）**：
   - 在主菜单第 53 行新增菜单项 `#L90#Bot伴侣管理#l`（位于“野外BOSS刷新”旁）；
   - 在 `doSelect` 分支处理中增加 `case 90: cm.dispose(); cm.openNpc(9000055); break;`，顺畅调起 NPC 9000055。
2. **Bot 管理与召唤 NPC 脚本（[9000055.js](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/scripts-zh-CN/npc/9000055.js)）**：
   - 同步更新于 `scripts-zh-CN/npc/9000055.js` 与 `scripts/npc/9000055.js`；
   - **功能 1：【招募伴侣】**：支持玩家自主选择战士、魔法师、弓箭手、飞侠或随机职业伴侣。Bot 生成后根据玩家等级动态适配，自动创建队伍（如未组队）并将 Bot 加入玩家队伍，默认设为 FOLLOWER 状态，实现实时跟随；
   - **功能 2：【战斗指令】**：一键将队伍内所有跟随状态的 Bot 转换为 TRAINING_BOT 并在当前地图驻扎（markStationHere），Bot 立即在本地图搜寻怪物开打，全队共享经验与掉落；
   - **功能 3：【行军指令】**：一键将队伍内处于战斗状态的 Bot 切回 FOLLOWER 模式，重新开启寸步不离跟随队长，方便跑图过图与穿门；
   - **功能 4：【离队管理】**：一键安全请离队伍中的所有 Bot；
   - **功能 5：【野外召唤】**：支持在当前打怪地图单独生成自主刷怪 Bot，不占用玩家队伍名额；
   - **功能 6：【清理地图】**：一键清理当前地图上的所有虚拟 Bot；
   - **功能 7：【帮助说明】**：提供清晰直观的组队、指令气泡互动及经验共享机制指引。

### 13.3 运行时异常修复：MapMobIndex 包名修正
- **问题**：客户端启动脚本时 GraalVM Polyglot 抛出 `TypeError: Access to host class org.gms.soloMapling.Field.MapMobIndex is not allowed or does not exist`。
- **根因**：`MapMobIndex` 实际类包名为 `org.gms.soloMapling.ArtificialPlayer.BotGrindSystem.MapMobIndex`，脚本导入包路径误写为 `org.gms.soloMapling.Field.MapMobIndex`。
- **解决**：在 [scripts-zh-CN/npc/9000055.js](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/scripts-zh-CN/npc/9000055.js) 和 [scripts/npc/9000055.js](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/scripts/npc/9000055.js) 中将 `Java.type` 修正为完整正确的包名路径。

### 13.4 运行时异常修复：Java Collection 无 get 方法问题
- **问题**：启动脚本时报错 `TypeError: invokeMember (get) on java.util.Collections$UnmodifiableCollection failed due to: Unknown identifier: get in <eval> at line number 57`。
- **根因**：`map.getCharacters()` 与 `party.getMembers()` 返回的均为 Java 原生 `Collection<Character>` / `Collection<PartyCharacter>`，并非 `List`，因此未实现 `get(int index)` 方法；脚本使用 `for (var i = 0; i < chrs.size(); i++) chrs.get(i)` 导致 GraalJS 反射调用失败。
- **解决**：在脚本中加入通用的 `toJsArray(javaCollection)` 转换函数，通过原生 `iterator()` 将 Java 集合遍历转换为纯 JavaScript 数组，支持安全的 `length` 和 `[i]` 索引访问；同时对队伍角色的 `getPlayer()` 补充了 `BotHelpers.getCharFromChannelStorage(pc.getId())` 空回退防护。

### 13.5 交互显示与状态残留修复：问号乱码与清理地图后幽灵打怪问题
- **问题 1（问号乱码）**：
  - **现象**：招募伴侣等菜单选项前显示 `? 战士 (Warrior)` 等问号乱码。
  - **根因**：MapleStory 083 客户端字库不支持 Unicode Emoji（如 `⚔`, `🔮`, `🏹`, `🗡`, `🎲` 等），客户端渲染非 GBK/ASCII 字符时回退为 `?`。
  - **解决**：移除全部 Emoji，替换为冒险岛原生文本标签与颜色代码（如 `#b[战士]#k`、`#r[法师]#k`、`#g[弓手]#k`、`#d[飞侠]#k`、`#k[随机]#k`），界面清爽且 100% 兼容。
- **问题 2（清理地图后 Bot 变成看不见的幽灵继续打怪/加Buff）**：
  - **现象**：使用“清理地图”后，再点击“行军指令”或“战斗指令”，Bot 会像回到地图一样，空气打死怪物，且隐身给玩家加 Buff。
  - **根因**：
    1. 原 `BotGeneration.removeBotFromServer` 仅从地图移除玩家，未注销 `BotSM` 定时器任务（`BotTickService` 仍在后台周期执行）；未停用 `GCMovement`；更未将 Bot 移出队伍（`fakechar.getParty()` 仍残留该成员）。
    2. “清理地图”清理了伴侣 Bot 后，玩家队伍成员依然保留了该 Bot。当玩家随后点击“战斗指令”或“行军指令”时，脚本遍历队伍成员重新调用 `convertBotType`，在已离开地图的角色上重新拉起了 `TrainingBot` 行为树，导致其作为“幽灵实体”对怪物造成伤害和释放 Buff。
    3. `handlePartyAttack` 和 `handlePartyFollow` 缺少对 Bot 是否真正存活并处于本地图的有效性校验。
  - **解决**：
    1. **服务端底层修复**：在 [BotGeneration.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotGeneration.java) 的 `removeBotFromServer` 中，加入彻底注销逻辑：关闭 `BotSM` 任务并在 `BotTickService` 注销、停用 `GCMovement`、清理 Buff 驱动，并在检测到 Bot 处于队伍时自动向世界广播脱离队伍（`PartyOperation.LEAVE`），彻底断绝状态残留。
    2. **脚本安全校验**：在 [9000055.js](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/scripts-zh-CN/npc/9000055.js) 中，为“行军指令”和“战斗指令”增加有效性校验：跳过未处于当前地图或已掉线的失效 Bot。
    3. **清理模式细分**：将“清理地图”拆分为：
       - `[仅清理野外Bot]`：保留玩家招募的队伍伴侣，只清理本地图野外自主 Bot；
       - `[全图彻底清理]`：清理本地图所有 Bot，并自动将伴侣请离队伍。

### 13.6 等级差规则优化：招募与手动邀请 10 级限制 (真实玩家不受限)
- **需求背景**：
  1. 招募伴侣限制召唤跟玩家等级差值 10 级以内的 Bot；
  2. 手动组 Bot 的等级限制恢复为 10 级限制（真实玩家组队不受限制）。
- **根因与机制调整**：
  1. **伴侣招募等级范围限制**：
     - 在 [9000055.js](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/scripts-zh-CN/npc/9000055.js) 和 [scripts/npc/9000055.js](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/scripts/npc/9000055.js) 中，根据玩家等级 `pLevel`，计算 `minLevel = Math.max(10, pLevel - 10)` 和 `maxLevel = Math.min(200, Math.max(minLevel + 1, pLevel + 10))`，传入 `BotGeneration.createBot`；
     - 修复 [BotGeneration.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotGeneration.java) 中当 `baseClass <= 0`（随机职业）时无条件覆盖使用默认等级范围 `10~80` 的缺陷，确保随机职业伴侣同样严格遵循 `[minLevel, maxLevel]` 等级区间；
     - 无论是自选职业还是随机职业，NPC 9000055 生成并在当前地图放置的打怪 Bot 或队伍伴侣均在 `[playerLevel - 10, playerLevel + 10]` 范围内。
  2. **手动邀请组队等级限制**：
     - 在 [PartyOperationHandler.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/net/server/channel/handlers/PartyOperationHandler.java) 的组队邀请包处理中区分 `isBot(invited)`：若被邀请者为 Bot 且 `Math.abs(player.getLevel() - invited.getLevel()) > 10`，拦截并向玩家发送服务器黄字提示：`"无法邀请与您等级相差超过10级的Bot伴侣加入队伍。"`；
     - 真实玩家（`!isBot(invited)`）不受 10 级限制，维持原先逻辑不变；
     - 在 [BotPartyCommands.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotPartySystem/BotPartyCommands.java) 的 `botAcceptPartyInvite` 与 [BotRecruitManager.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotPartySystem/BotRecruitManager.java) 的 `rollPartyAsk` 中增加双重防护：若等级差距超过 10 级，Bot 拒绝入队。

### 13.7 地图 Bot 生成与清理安全隔离优化
- **需求背景**：
  1. 清理 Bot 脚本绝不应清理别人队伍里的 Bot，两个选项只能清理野外无队伍的 Bot 和自己队伍的 Bot；
  2. 在地图生成的野外 Bot 应当符合地图怪物等级，而非符合玩家等级（例如射手训练场几级怪物应生成个位数等级 Bot，杜绝高级碾压）；
  3. 优化地图 Bot 数量与生成机制，避免进图堆积与 Bot 数量泛滥。
- **机制优化与改动**：
  1. **队伍归属隔离保护清理机制**：
     - 在 [9000055.js](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/scripts-zh-CN/npc/9000055.js) 中，重构两种清理逻辑：
       - `[清理野外Bot]`：严格仅清理 `c.getParty() == null` 的野外游荡 Bot；凡处于队伍中的 Bot（无论是自己的队伍还是其他玩家的队伍），**一律跳过保留**；
       - `[清理本地图与我的伴侣]`：严格仅清理 `c.getParty() == null` 的野外游荡 Bot 以及归属于当前玩家队伍的伴侣（`myParty.getId() == botParty.getId()`）；对其他玩家队伍中的 Bot 伴侣实施强制豁免保护，**绝对不会误删他人队伍伴侣**。
  2. **野外召唤 Bot 等级符合地图怪物等级**：
     - 在 [9000055.js](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/scripts-zh-CN/npc/9000055.js) 中实现 `getMapRecommendedLevelRange(map, player)` 函数：优先通过 `MapMobIndex.level(mapId)` 获取地图怪物中位数等级，若无则统计现场活怪平均等级；
     - 若为新手/低级猎场（怪物等级 $\le 10$ 级，如射手训练场），生成等级设为 $[ \max(1, \text{mapLevel} - 2), \min(15, \text{mapLevel} + 3) ]$，生成的为个位数新手剑士/初学者，与本地图怪物等级精准匹配；
     - 若为中高级猎场，设为 $[ \text{mapLevel} - 5, \text{mapLevel} + 5 ]$；无怪和平地图回退至安全初始等级；
     - 菜单中实时提示当前地图怪物评级与生成预测。
  3. **地图容量上限与开机群落配比优化**：
     - 在 [9000055.js](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/scripts-zh-CN/npc/9000055.js) 的 `handleSpawnWildBot` 中增加单图野外 Bot 数量上限保护（`MAX_MAP_WILD_BOTS = 5`），若本地图野外 Bot 已达上限则拦截生成并提示清理，彻底防止玩家重复点击造成地图 Bot 堆积；
     - 在 [EnvironmentManager.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/Environment/EnvironmentManager.java) 中优化 Wave 8 开机预设 TrainingBot 群落：将射手村（Henesys）预设生成等级从 `10~95` 修正为与金银岛新手区相匹配的 `10~30`（消除初始高等级 Bot 闲逛至低级射手训练场的问题），废都、勇士、魔法密林统一调优为 `10~35`，并适度削减了初始群落生成基数（总数由 230+ 精简至 130 左右），避免开机即大面积占图拥堵。

---

## 14. 提交规范
遵守仓库 Git Commit Message 规范，提交记录：
```text
feat: 1. 在帮助中心脚本(9900001.js)中集成Bot伴侣管理入口
      2. 新增Bot控制管理与召唤交互NPC脚本(9000055.js)

fix:  1. 修复NPC脚本9000055.js中MapMobIndex类包名路径错误

fix:  1. 修复NPC脚本9000055.js中Java集合无get方法导致的Unknown identifier错误

fix:  1. 修复NPC脚本9000055.js中Emoji导致选项显示问号乱码的问题
      2. 修复清理地图后Bot残留队伍与后台调度引发幽灵打怪的缺陷

feat: 1. 限制招募Bot伴侣等级在玩家等级±10级以内
fix:  1. 恢复手动邀请Bot入队的10级等级差限制(真实玩家不受限制)

feat: 1. 地图生成的野外Bot等级严格适配地图怪物等级(消除低级怪区生成高级Bot)
      2. 增加单地图野外Bot数量上限保护(最大5人)防止泛滥堆积
      3. 优化开机新手村Bot群落等级区间与初始密度
fix:  1. 清理Bot功能增加队伍隔离保护(严禁清理他人队伍中的Bot伴侣)

feat: 1. 实现 Bot i18n 本地化管理类与资源路由，默认启用中文支持并提供无缝回退
      2. 规范 Bot 角色名称库以"仙"开头并适配 GBK 字节限制（<=12字节），新增中文热梗与小说角色名库
      3. 提供 Bot 对话包全部 20 个 YAML 的 1:1 中文翻译，完整保留占位符、表情代码与等待时间
      4. 汉化 FollowerBot 与 TrainingBot 头顶气泡操作菜单，支持中英双语关键词与组队交互提示
      5. 补充 BotResourceLoadingTest 本地化资源与角色名规则单元测试

fix:  1. 修复自由市场买卖/点券机器人发言英文硬编码，支持经典中文喊话与万/亿金币表达
      2. 修复摆摊商店店名描述（Cheap/Leave Offer/装备属性简称/极品天然）汉化缺失
      3. 修复Bot交易窗口聊天内容及需求提示（I want/交易确认/取消/超时）为中文
```

---

## 15. Bot i18n 国际化与中文本地化完整实现

### 15.1 架构与设计
1. **[SoloMaplingI18n.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/server/SoloMaplingI18n.java)**：
   - 核心本地化管理单例，默认启用中文模式（`isChinese() == true`），可通过 JVM 参数 `-DsoloMapling.language=en` 随时切换回原版英文；
   - 实现了双向安全路径解析：`resolveLocalizedResource(basePath, filename)`，在中文模式下优先探测 `basePath/zh-CN/filename`，若存在直接定位，若缺失平滑回退至原版文件，完全消除缺失资源引发的崩溃风险；
   - 提供了 `formatBotName(String rawName)` 规则强制器：
     - 所有 Bot 角色名统一以汉字 `"仙"` 开头；
     - 严格遵守冒险岛客户端角色名 12 字节（GBK）限制（中文每个字符 2 字节，最大 6 个汉字），自动截断至 12 字节以内，彻底避免客户端截断显示乱码或越界。
2. **[SoloMaplingResourceLoader.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/server/SoloMaplingResourceLoader.java)**：
   - 扩展了 `hasResource(String rawPath)` 接口，支持 classpath 与多层文件系统资源探测，与 `SoloMaplingI18n` 无缝联动；
   - `getReader(String rawPath)` 全面采用 `StandardCharsets.UTF_8` 解码，杜绝 Windows 默认编码带来的 YAML 解析中文乱码。

### 15.2 中文名称库与自由市场描述
1. **中文名称库**：
   - 在 `FMNameDesc/zh-CN/randomRealMaplestoryIGNs.txt`（以及 `FreeMarket/FMNameDesc/zh-CN/`）中生成了 430+ 个网络热梗名、小说角色名、游戏角色名，全部以 `"仙"` 字冠首（如 `"仙尊韩立"`、`"仙秦始皇"`、`"仙剑逍遥"`、`"仙道求魔"`、`"仙之巅"` 等），字长严格控制在 3~5 汉字（6~10 字节）。
2. **职业与分类中文描述库**：
   - 完整生成了 `zh-CN/` 目录下的 `warriorDesc.txt`, `mageDesc.txt`, `bowmanDesc.txt`, `thiefDesc.txt`, `commonDesc.txt`, `scrollsDesc.txt`, `useableDesc.txt`, `etcDesc.txt`, `chairDesc.txt`, `shortWordDesc.txt`, `FMClans.txt`, `emojiFaces.txt`。
3. **[FMShopDescGen.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/FreeMarket/FMShopDescGen.java)**：
   - `resolveFilePath` 优先重定向至 `zh-CN/`；
   - 角色名生成管道全量接入 `SoloMaplingI18n.formatBotName(...)` 过滤，确保生成的 Bot 无论是从文本库读取还是动态拼接，100% 格式合法。

### 15.3 Bot 对话包全部 20 个 YAML 的 1:1 中文翻译
在 `gms-server/src/main/resources/soloMapling/BotDialoguePack/zh-CN/` 下创建并翻译全部 20 个对话包：
- **完整保留结构与占位符**：严格对齐原版 YAML 键名、表情 Emote 数组、等待延时 wait 时间；保留全部上下文占位符（`{PLAYER_NAME}`, `{PLAYER_LEVEL}`, `{PLAYER_JOB}`, `{PLAYER_FAME}`, `{PLAYER_WEAPON}`, `{PLAYER_GEAR}`, `{PLAYER_PET}`, `{PLAYER_GUILD}`, `{PLAYER_NX}`, `{MAP}`, `{REGION}`, `{MOB}`, `{DROP}`, `{item}`, `{price}`, `{counter_price}` 等）；
- **包含 20 个模块**：
  1. `FollowerBotDialogue.yaml`：跟班伴侣对话（带路、安营扎寨、跟丢、解散等）
  2. `TownChatterDialogue.yaml`：城镇双人闲聊对话
  3. `TownWandererDialogue.yaml`：城镇闲逛 Bot 问候与升级恭喜
  4. `DropGameBotDialogue.yaml`：丢物抢宝主机主持台词
  5. `DropGameSpectatorDialogue.yaml`：丢物抢宝观众喝彩与抢夺
  6. `DropGameLootPool.yaml`：奖池配置
  7. `JQBotDialogue.yaml`：跳跳任务挑战与抓狂
  8. `TutorialBotDialogue.yaml`：新手指引向导台词
  9. `ShopOfferDialogue.yaml`：自由市场砍价与还价
  10. `MerchantBotDialogue.yaml`：摆摊商贩经典热梗与叫卖
  11. `ScrollingBotDialogue.yaml`：砸卷 Bot 成功狂喜与炸装崩溃
  12. `FMBotDialogue.yaml`：自由市场淘货与采购
  13. `BlackjackDealerBotDialogue.yaml`：21点荷官发牌与台风
  14. `GameZoneHostBotDialogue.yaml`：娱乐中心招待台词
  15. `HenesysBotDialogue.yaml`：射手村专属 NPC/Bot 对话
  16. `SocialHotPotatoDialogue.yaml`：日常游戏吐槽与闲扯
  17. `MegaphoneDialogue.yaml`：全服喇叭喊话（生日祝福、公会招人、卖卷收金、装逼等）
  18. `SocialBotDialogue.yaml`：社交 Bot 对话与好友交互
  19. `TrainingBotDialogue.yaml`：打怪练级、残血求生、合图与组队
  20. `ConversationDialogue.yaml`：城镇多角色剧本式对话（303个剧本）

### 15.4 代码层本地化集成
1. **[BotDialogueHandler.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotDialogueHandler.java)**：`readDialogueYaml` 路由至本地化路径；
2. **[ConversationManager.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/ConversationManager.java)**：`loadScripts` 路由至本地化路径；
3. **[TownChatterLines.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotChatterSystem/TownChatterLines.java)**：`load` 路由至本地化路径；
4. **[DropGameLootPool.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotTypes/DropGameLootPool.java)**：路由至本地化路径；
5. **[FollowerBot.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotTypes/FollowerBot.java)**：汉化头顶气泡操作选项（`"陪我在这里打怪！"` / `"取消"`），关键词支持中英文双语识别（`打怪`, `练级`, `刷怪`, `停在这`, `取消`, `算了`, `再见` 等）；
6. **[TrainingBot.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotTypes/TrainingBot.java)**：汉化单人/队伍头顶气泡操作选项（`"练级还顺利吗？"` / `"想一起组队吗？"` / `"跟我来！"` / `"再见"`），关键词支持中英文双语识别（`怎么样`, `顺利吗`, `打怪`, `组队`, `一起`, `跟我来`, `跟我走`, `拜拜` 等）；
7. **[BotPartyCommands.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotPartySystem/BotPartyCommands.java)**：汉化入队邀请反馈、拒绝提示以及发言（`"咱们一起组队吧！"`）。

### 15.5 编译与自动化测试验证
- **Maven 构建**：`.\SoloMapling-0.3\mvnw.cmd test-compile -f gms-server\pom.xml` -> **BUILD SUCCESS**（1403 个源码文件编译通过）；

- **单元测试**：`.\SoloMapling-0.3\mvnw.cmd test -Dtest=BotResourceLoadingTest -f gms-server\pom.xml`：
  - `testChineseBotNames`: 50 次随机角色名抽取均以 `"仙"` 开头，GBK 编码下长度严格 $\le 12$ 字节；
  
  - `testChineseDialogueLoading`: 验证 `FollowerBotDialogue.yaml` 等本地化资源成功定位并解析，对话节点非空；
  
  - 6 个测试全部通过：**Tests run: 6, Failures: 0, Errors: 0, Skipped: 0**。
  
    

### 15.6 自由市场与交易系统汉化修复
全面解决自由市场交易机器人喊话、摆摊商店描述及 Bot 交易窗口聊天残留英文问题：

1. **自由市场交易机器人喊话汉化**：
   - **[BuyingMerchantBot.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotTypes/BuyingMerchantBot.java)**:
     - 买入前缀：从 `Buying`, `B>`, `BUY>` 等修改为中文 `收>`, `收`, `收>>`, `高价收>`, `收收收>`, `求购>`；
     - 买入后缀：从 `Trade Me`, `PM me`, `hmu`, `no scammers`, `no lowball`, `no weebs` 等修改为中文 `点我交易`, `带价密`, `直接点我交易`, `速点交易`, `骗子绕道`, `黑人绕道`, `妹子优先`, `不墨迹的来` 等；
     - 英文全大写逻辑在中文环境下自动旁路，保持中文标点与字形自然。
   - **[SellingMerchantBot.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotTypes/SellingMerchantBot.java)**:
     - 售卖前缀：从 `Selling`, `S>`, `SELL>` 等修改为中文 `出>`, `出`, `出>>`, `急出>`, `甩卖>`, `急甩>`；
     - 售卖后缀：从 `You Offer`, `Offer`, `Trade Me`, `PM me`, `no time wasters`, `No Spanish` 等修改为中文 `代价密`, `带价来`, `诚心带价`, `点我交易`, `诚信交易`, `在线等`, `不墨迹的来` 等。
   - **[NXMerchantBot.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotTypes/NXMerchantBot.java)**:
     - 喊话：汉化为 `"出10k点券卡密，5000万金币，点我交易！"`、`"出> 10k点卷卡密 5000w，不议价"`、`"点券卡密 10k >> 5000w 点我交易！！诚信第一"` 等；
     - 私聊与卡密发放：私聊提示 `"私聊你发卡密了。"`、卡密说明 `"这是10k点券卡密... 请记好，注意不要输入横杠："`、`"祝游戏愉快！"`。
   - **[FMEconomyManager.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/FreeMarket/FMEconomyManager.java)**:
     - 价格缩写 `formatPriceToShorthand`：适配中国玩家习惯的 `万` 与 `亿`（如 `390k` $\rightarrow$ `39万`，`760k` $\rightarrow$ `76万`，`2.1m` $\rightarrow$ `210万`，`50m` $\rightarrow$ `5000万`，`1.5b` $\rightarrow$ `1.5亿`）。

2. **摆摊商店内容描述汉化**：
   - **[ArtificialFreeMarket.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/FreeMarket/ArtificialFreeMarket.java)**:
     - 特价商店追加词：从 `" Cheap"` 汉化为 `" 特价"`；
     - 清仓商店追加词：从 `" QUITTING SALE"` 汉化为 `" 退坑甩卖"`；
     - 1金币店标题：从 `" 1 MESO SHOP!!!"` 汉化为 `" 1金币店!!!"`。
   - **[FMShopDescGen.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/FreeMarket/FMShopDescGen.java)**:
     - 出价提示（`getOfferableDescription`）：从 `"Leave Offer"`, `"Offer"`, `"L/O"` 汉化为 `"带价来"`, `"代价密"`, `"诚心留言"`, `"自带价"`, `"留言出价"`, `"带诚意代价"`, `"不墨迹代价"`, `"诚心带价"`；
     - 装备属性强化招牌（`advertiseBestEquip`）：
       - 属性简写由 `dex`, `str`, `int`, `luk`, `watt`, `matt` 翻译为 `"敏捷"`, `"力量"`, `"智力"`, `"运气"`, `"物攻"`, `"魔攻"`（如 `23 dex 黑飘云之衣` $\rightarrow$ `23敏捷 黑飘云之衣`）；
       - 非白板未写属性：`"Godly "` $\rightarrow$ `"极品 "`；
       - 白板装备：`"clean "` $\rightarrow$ `"天然 "`；
     - 交易货币缩写（`advertiseRWTCurrencies`）：支持 `"点卷"`, `"金币"`, `"白卷"`, `"枫叶"`, `"混沌"`。
   - **[FMShopDescriptionManager.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/FreeMarket/FMShopDescriptionManager.java)**:
     - 店铺欢迎语：从 `"Welcome to " + owner + "'s Shop!"` 汉化为 `"欢迎光临 " + owner + " 的小店！"`。
   - **[ShopOfferWelcome.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/FreeMarket/ShopOfferSystem/ShopOfferWelcome.java)** & **[ShopOfferResponse.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/FreeMarket/ShopOfferSystem/ShopOfferResponse.java)**:
     - 进店出价提示与私聊通知价格调整全部提供中文支持。

3. **跟 Bot 交易窗口聊天汉化**：
   - **[BotTradeSM.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotTradeSystem/BotTradeSM.java)**:
     - 诉求信息 `generateWantsMessageString()`：
       - 从 `"I want "` 汉化为 `"想要 "`；
       - 金币数量汉化：如 `"想要 5000万金币"`、`"想要 5000万金币 和 拳套攻击卷轴"`、`"想要 2个 拳套攻击卷轴"`；
       - 无特定需求汉化：从 `"nothing specific"` 汉化为 `"随便给点什么都行"`；
     - 交易过程聊天：
       - `"我现在没有什么想要交易的"`（原 `"I don't have anything at the moment"`）；
       - `"交易没问题，确认了！"`（原 `"trade looks good to go!"`）；
       - `"这是给你道具的 39万 金币！"`（原 `"Here's ... mesos for your item!"`）；
       - `"这是我要出售的，看看吧！"`（原 `"Here is what I've got. check it out!"`）；
       - `"多谢，合作愉快！"`（原 `"Thank you!"`）；
       - `"为什么取消交易了？"`（原 `"Why did you decline?"`）；
       - `"交易超时！"`（原 `"Timed Out!"`）；
       - `"算了，不换了。再见。"`（原 `"Nah I'm good. Good bye."`）。
   - **[TutorialBot.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotTypes/TutorialBot.java)**:
     - 新手礼包交易发言汉化为 `"给你一些新手启动物资！"`，`"10亿金币、药水、卷轴、飞镖和装备，全都是你的！"`。
   - **[DropGameBot.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotTypes/DropGameBot.java)**:
     - 丢丢乐交易窗口规则与提示全量汉化（`"丢丢乐游戏！中级: 1000万 / 高级: 5000万"`、`"放入金币并点击确定！"`、`"太慢了！交易超时。"` 等）。

4. **测试与验证结果**：
   - **Maven 编译**：`.\SoloMapling-0.3\mvnw.cmd test-compile -f gms-server\pom.xml` $\rightarrow$ **BUILD SUCCESS**；
   - **单元测试**：`.\SoloMapling-0.3\mvnw.cmd test -Dtest=BotResourceLoadingTest -f gms-server\pom.xml`：
     - `testChinesePriceFormatting`: 验证 390k $\rightarrow$ 39万, 760k $\rightarrow$ 76万, 2.1m $\rightarrow$ 210万, 50m $\rightarrow$ 5000万, 100m $\rightarrow$ 1亿；
     - `testMerchantBotChineseMessages`: 验证买入与卖出喊话无英文前缀/后缀残留，包含正宗中文口吻；
     - `testFMShopOfferableDescriptions`: 验证摆摊出价描述均为中文；
     - `testChineseBotNames`: 验证 50 个生成的 Bot 名称均以 "仙" 开头且 $\le 12$ 字节；
     - `testChineseDialogueLoading`: 验证 zh-CN 对话资源正常解析；
     - **测试结果**：9 个测试用例全部通过（**Tests run: 9, Failures: 0, Errors: 0, Skipped: 0**）。

---

## 16. master 远程更新同步合并至 Bot 分支

### 16.1 背景与目标
拉取远程 `origin/master` 最新更新（提交 `6c5a1c1f5`：修复封禁账号后雇佣商店继续营业的异常问题），并将其平滑合并到 `Bot` 分支。严格保障合并过程不影响、不破坏 `Bot` 分支的所有新增功能、自主逻辑与汉化体系。

### 16.2 合并过程与冲突检测
1. **获取最新主干**：通过 `git fetch origin master:master` 快进更新本地 `master` 至 `origin/master`（`6c5a1c1f5`）。
2. **合并至 Bot 分支**：在 `Bot` 分支执行 `git merge --no-commit master`。
3. **冲突判定**：Git 3-way 自动合并顺利完成（`Automatic merge went well`），**0 冲突**，无需人工裁决介入。

### 16.3 变更审查与 Bot 代码防护确认
本次合并引入来自 `master` 的 7 个文件改动（共 +216 行 / -66 行）：
- **[HiredMerchant.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/HiredMerchant.java)**:
  - `master` 改动：增加了封禁账号时的自动闭店与游离状态防护（`ownerBanned`、`detached`、`closeForBan()`）；
  - `Bot` 防护审查：Bot 新增的自由市场购买逻辑（`botBuy`）以及纯内存人工商店（`HiredMerchantArtificial`）跳过数据库存储与世界查询的优化逻辑完整保留，与封禁检测互不干扰。
- **[Character.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/Character.java)**:
  - `master` 改动：在角色关闭雇佣商店交互时增加了 `merchant.isClosedForBan()` 检查；
  - `Bot` 防护审查：Bot 新增的 `isBot` 判定、`isLoggedInWorld`、`getCashShop()`、`getMonsterBook()` 懒加载等完全位于独立代码块，未受任何改动影响。
- **[AccountService.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/service/AccountService.java)**、**[PlayerInteractionHandler.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/net/server/channel/handlers/PlayerInteractionHandler.java)**、**[World.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/net/server/world/World.java)** 以及国际化日志配置：
  - 均为账号封号处理时级联关闭玩家已开雇佣商店的业务闭环，未涉及 Bot 交互与行为决策树。

### 16.4 构建与测试验证
- **Maven 全量编译**：1403 个源码文件均通过编译，无任何警告与语法错误。
- **单元测试**：运行 `.\SoloMapling-0.3\mvnw.cmd test -Dtest=BotResourceLoadingTest -f gms-server\pom.xml`，所有 9 个单元测试 100% 通过（**Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS**）。