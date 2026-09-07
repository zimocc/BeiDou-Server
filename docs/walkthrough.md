# SoloMapling Bot 系统完整演进与本地化实施记录

本文档为 **BeiDou-Server (gms-server)** 引入并完善 **SoloMapling-0.3** 自主虚拟玩家（Bot）系统的全流程演进与优化技术记录，涵盖底层架构移植、引擎核心 Hook、高并发稳定性与死锁调优、运行时关键异常修复、伴侣管理与生态治理、全量 i18n 国际化中文化，到自由市场交易系统深度汉化的所有技术细节。

---

## 目录
- [一、移植范围与分支状态](#一移植范围与分支状态)
- [二、Bot 核心架构移植与北斗引擎 Hook](#二bot-核心架构移植与北斗引擎-hook)
- [三、高并发稳定性保障与关键运行时异常修复](#三高并发稳定性保障与关键运行时异常修复)
  - [1. 虚拟线程 XML 并发安全与 DOM 树根文档锁](#1-虚拟线程-xml-并发安全与-dom-树根文档锁)
  - [2. 假人启动死锁、Druid 连接池耗尽与日志 IO 阻塞治理](#2-假人启动死锁druid-连接池耗尽与日志-io-阻塞治理)
  - [3. 零 DB 纯内存角色构建与背包初始化](#3-零-db-纯内存角色构建与背包初始化)
  - [4. 地图幽灵玩家误清理机制修复与 Bot 生命周期对齐](#4-地图幽灵玩家误清理机制修复与-bot-生命周期对齐)
  - [5. Bot 跨图传送卡住掉线误判修复](#5-bot-跨图传送卡住掉线误判修复)
  - [6. FMBot 逛店议价与购买人工摆摊 NPE 修复](#6-fmbot-逛店议价与购买人工摆摊-npe-修复)
  - [7. TownPins 边车文件缺失与优雅回退](#7-townpins-边车文件缺失与优雅回退)
  - [8. 通道与世界静态引用时序隐患消除](#8-通道与世界静态引用时序隐患消除)
  - [9. 查看 Bot 角色信息与升级公会 NPE 修复](#9-查看-bot-角色信息与升级公会-npe-修复)
- [四、Bot 伴侣管理、招募等级限制与野外生态治理](#四bot-伴侣管理招募等级限制与野外生态治理)
  - [1. 帮助中心集成与 Bot 管理脚本 (9900001.js & 9000055.js)](#1-帮助中心集成与-bot-管理脚本-9900001js--9000055js)
  - [2. GraalVM 脚本引擎兼容性修复](#2-graalvm-脚本引擎兼容性修复)
  - [3. v083 客户端 Emoji 乱码与原生样式改造](#3-v083-客户端-emoji-乱码与原生样式改造)
  - [4. 清理地图后幽灵打怪修复与生命周期彻底注销](#4-清理地图后幽灵打怪修复与生命周期彻底注销)
  - [5. 清理权限隔离与队伍保护机制](#5-清理权限隔离与队伍保护机制)
  - [6. 伴侣招募与手动邀请 10 级限制](#6-伴侣招募与手动邀请-10-级限制)
  - [7. 野外 Bot 怪物等级自适应与单图容量保护](#7-野外-bot-怪物等级自适应与单图容量保护)
  - [8. 开机预设群落配比与密度优化](#8-开机预设群落配比与密度优化)
  - [9. 组队交互体验重构：右键直接邀请 100% 同意入队](#9-组队交互体验重构右键直接邀请-100-同意入队)
- [五、Bot 国际化 (i18n) 与全量中文本地化](#五bot-国际化-i18n-与全量中文本地化)
  - [1. i18n 核心管理架构与资源路由](#1-i18n-核心管理架构与资源路由)
  - [2. Bot 角色名称规范与“仙”字库 (GBK <= 12字节)](#2-bot-角色名称规范与仙字库-gbk--12字节)
  - [3. 职业与分类中文描述库](#3-职业与分类中文描述库)
  - [4. 全部 20 个 YAML 对话包 1:1 中文翻译](#4-全部-20-个-yaml-对话包-11-中文翻译)
  - [5. 代码层本地化集成与中英双语指令兼容](#5-代码层本地化集成与中英双语指令兼容)
- [六、自由市场与交易系统深度汉化](#六自由市场与交易系统深度汉化)
  - [1. 买卖与点券喊话机器人汉化](#1-买卖与点券喊话机器人汉化)
  - [2. 摆摊店名、出价提示与装备属性招牌汉化](#2-摆摊店名出价提示与装备属性招牌汉化)
  - [3. 跟 Bot 交易窗口聊天与互动流程汉化](#3-跟-bot-交易窗口聊天与互动流程汉化)
  - [4. 新手向导与丢丢乐小游戏交易汉化](#4-新手向导与丢丢乐小游戏交易汉化)
- [七、验证与测试体系汇总](#七验证与测试体系汇总)
- [八、Git 提交记录与 Author 规范](#八git-提交记录与-author-规范)

---

## 一、移植范围与分支状态

- **Git 分支：** `Bot`（基于最新 `origin/master` 创建并维护）
- **初始化提交信息：**
  ```text
  feat: 1. Port SoloMapling autonomous Bot system into gms-server
        2. Add Flyway migration V1.11.7 for bot accounts and NPC support
        3. Hook bot lifecycle, movement, combat, trading, and shops into BeiDou engine
  ```
- **工作区状态：** `working tree clean`，所有改动均已通过自动化单元测试与全量编译打包验证。

---

## 二、Bot 核心架构移植与北斗引擎 Hook

为使 SoloMapling-0.3 能够在北斗 GMS v0.83 服务端稳定运作，实施了全套模块移植、依赖协调及底层引擎方法 Hook：

### 1. 依赖与构建配置
- **[gms-server/pom.xml](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/pom.xml)**:
  - 引入 `yamlbeans:1.17`（支持行为树对话与配置 YAML 解析）以及 `org.jgrapht:jgrapht-core:1.5.2` / `jgrapht-io:1.5.2`（支持高级图算法拓扑与寻路）；
  - 在 `maven-compiler-plugin:3.13.0` 中配置 `annotationProcessorPaths`，明确协调 `lombok:1.18.30` 与 `mybatis-flex-processor:1.8.9`，确保编译期 APT 代码（如 `CharacterDOTableDef` 等）稳定生成，杜绝注解处理器冲突。

### 2. 数据迁移与资源支持
- **Flyway 数据库迁移：**
  - 新增 [V1.11.7__add_solo_mapling_bot_support.sql](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/resources/db/migration/V1.11.7__add_solo_mapling_bot_support.sql)，提供 Bot 预制系统账号、虚拟角色数据模版、交易机器人支持及专属管理 NPC 绑定；
- **运行时动线与配置资源：**
  - 移植 `gms-server/src/main/resources/soloMapling/` 全套数据：
    - `movementDataPackets/`：包含全地图拓扑寻路与录制的移动数据包（`.bin` 与 `.csv` 动线记录）；
    - 各种行为配置与版本元数据（`npc_versions.yaml`, `portal_versions.yaml` 等）。

### 3. 核心 Bot 模块
- 全量移植并重命名 package 为 `org.gms.soloMapling.*`（共 264 个类文件）：
  - **核心行为与状态机：** `ArtificialPlayer`（BotActionSystem, BotAttackSystem, BotHelpers, BotCustomization, BotTier, BotTickService 等）；
  - **地图动效与交互：** `MapVFX`（CustomReactor, VFXManager 等）；
  - **事件总线系统：** `server.EventMessageSystem`（EventBus, EventFactory, 各种领域事件处理器等）；
  - **自由市场与交易：** `server.FM`（FMBotManager 等）、`server.Trade`；
  - **GM 指令扩展 (gm4 权限)：**
    - `ArtificialFreeMarketCommand`, `ArtificialPlayerCommand`, `BotMoveCommand`, `EnvironmentCommand`, `FMBotCommand`, `GCMoveCommand`, `OPQCommands`, `ReactorCommands`, `TestDevCommand`, `TradeBotTestCommand`；
  - **无头客户端与协议枚举：**
    - 新增无头客户端单例 [BotClient.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/BotClient.java)；
    - 补充核心枚举 [BodyPart.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/inventory/BodyPart.java)、[CharacterStance.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/constants/game/CharacterStance.java)、[Rope.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/Rope.java)。

### 4. 北斗引擎核心类对接与 Hook
- **[Character.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/Character.java)**:
  - 显式提供 `getId()` / `setId()` / `getID()` / `setID()` 以及 `isLoggedIn()` / `setLoggedIn()` / `isLoggedin()` / `setLoggedin()`，消除 Lombok 大小写不敏感方法覆盖判定冲突；
  - 在升级逻辑中接入 `EventBus.getInstance().publish(EventFactory.createLevelUpEvent(this))`；
  - 增加 `getTotalMoveSpeedStat()`、`getTotalJumpStat()`、`botTier`、`chasing` 等 Bot 支撑属性；
  - 暴露 `setChair(int)` 为 public 供 Bot 休憩坐椅调用。
- **[PacketCreator.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/util/PacketCreator.java)**:
  - 增加支持 `short delay` 的 `dropItemFromMapObject` 重载方法，支持掉落动效延时播放。
- **[ItemConstants.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/constants/inventory/ItemConstants.java)**:
  - 增加 `getItemPrefix(int itemId)` 和 `getEquipSlotType(int itemId)` 辅助分类方法。
- **[MapleMap.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/MapleMap.java)**:
  - 补充 `spawnMesoDrop` / `dropFromReactor` 延迟重载；
  - 提供 `getMonsterSpawnPositions()`、`getPortals()`、`getPlayerStores()`、`getHiredMerchants()`、`moveBot()`、`spawnItemDropNoExpire()` 接口。
- **[Trade.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/Trade.java)**:
  - 增加 `setMesoBot(int)` 和 `swapItem(Item)`，将 `isLocked()` 和 `getMeso()` 设为 public 供 Bot 模拟玩家交易。
- **[PlayerShop.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/PlayerShop.java)** & **[HiredMerchant.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/HiredMerchant.java)**:
  - 增加 `botBuy(...)`、`setItems(...)`、`chat(...)` 等 Bot 自动化摆摊购买与议价交互接口。
- **[FootholdTree.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/FootholdTree.java)** & **[Foothold.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/Foothold.java)**:
  - 增加 `getAllFootholds()`、`chainReachesGround()`、`touchesPoint()`、`isCollidableWall()`、`forbidFallDown` 等踏板拓扑接口，供 Bot 路径规划与平台跳跃判定使用。
- **[InPacket.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/net/packet/InPacket.java)** & **[ByteBufInPacket.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/net/packet/ByteBufInPacket.java)**:
  - 增加 `InPacket copy()` 支持 Bot 模拟网络协议广播转发。

---

## 三、高并发稳定性保障与关键运行时异常修复

在将 Bot 接入服务端后，服务端在多虚拟线程（Java 21 Virtual Threads）并发场景下暴露出多处严重的死锁、崩溃及性能瓶颈，均已进行深度根因剖析与根治：

### 1. 虚拟线程 XML 并发安全与 DOM 树根文档锁
- **现象**：高并发驱动（如数十只 Bot 练级和自由市场淘货）时，频繁抛出 `fNodeListCache is null` 与 `NamedNodeMap.getNamedItem("name")` 的 `NullPointerException`。
- **根因分析**：
  1. `XMLDomMapleData` 之前使用 `synchronized` 关键字修饰方法，但由于每次调用 `getChildByPath` 或 `getChildren` 均返回一个新的包装器实例，不同包装器实例之间的锁彼此独立；
  2. 底层采用的 Xerces W3C DOM Document 在 `ParentNode.fNodeListCache` 中维护非线程安全的游标与长度缓存，多个虚拟线程同时遍历底层同一个 DOM Document 导致缓存状态破坏；
  3. `ItemInformationProvider` 缓存为普通 `HashMap`，并发读写存在竞态；且道具未命中时没有负向缓存，导致未命中道具频繁穿透重复遍历 DOM 树。
- **修复方案**：
  - 在 [XMLDomMapleData.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/provider/wz/XMLDomMapleData.java) 引入统一根文档锁 `getLock()`（绑定底层 `node.getOwnerDocument()`），将所有节点访问同步在底层 Document 实例上；补充所有属性与子节点读取的判空检查；
  - 在 [ItemInformationProvider.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/ItemInformationProvider.java) 将 `nameDescCache` 升级为 `ConcurrentHashMap`，并引入 `EMPTY_NAME_DESC` 负向缓存标记；
  - 在 [BotHelpers.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotHelpers.java) 的 `convertItemIdToName` 增加防御性 try-catch，未命中或异常时安全回退 `"NULL"`。

### 2. 假人启动死锁、Druid 连接池耗尽与日志 IO 阻塞治理
- **现象**：客户端连接到 8484 登录端口卡死，Web 管理后台（8686 端口）登录无响应，控制台完全挂起。
- **根因分析**：
  1. **同步写文件锁争用**：[BotLogger.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/BotLogger.java) 原先为 `log = true`，每次移动 tick 都 `new PrintWriter(new FileWriter("BotLog.txt", true))`，测试 57 秒写入 **58,633 行**，多个虚拟线程排他文件锁争用卡死 JVM；
  2. **连接池无限等待**：`application.yml` 未配置 Druid 参数，默认 `maxActive = 8`、`maxWait = -1`（永不超时）；冷启动时并发为上千只 Bot 查询数据库打满 8 个连接，导致真实玩家登录和后台接口陷入死锁；
  3. **冷启动数量过载**：Wave 8 单城镇默认生成 225 只假人（全服 ~2500 只），单机调度瞬间超载。
- **修复方案**：
  - 将 [BotLogger.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/BotLogger.java) 改为 `static final boolean log = false;` 彻底关闭同步写盘，并在 `.gitignore` 中加入 `BotLog.txt`；
  - 在 [application.yml](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/resources/application.yml) 补充 Druid 参数：`max-active: 100`, `max-wait: 10000`, `initial-size: 10`, `min-idle: 10`, `validation-query: SELECT 1`，彻底消除无限等待；
  - 在 [EnvironmentManager.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/Environment/EnvironmentManager.java) 将 Wave 8 单城镇 Bot 调整为 15~25 只（全服约 200 只），启动耗时降至 3 秒内。

### 3. 零 DB 纯内存角色构建与背包初始化
- **现象**：每次生成 Bot 触发 15~20 次无意义的 SQL 查询（`Character.loadCharFromDB(2, ...)`），冷启动产生数万次 SQL 请求。
- **修复方案**：
  - 在 [BotGeneration.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotGeneration.java) 中改用纯内存构建 `Character.getDefault(getBotClient())`，并扩展背包到 96 格；
  - 创建耗时由数十毫秒降至微秒级，冷启动实现零数据库穿透。

### 4. 地图幽灵玩家误清理机制修复与 Bot 生命周期对齐
- **现象**：玩家进图时日志大量告警 `检测到幽灵玩家（已断线未正常移除），被动清理`，将本地图 Bot 全部移除。
- **根因分析**：
  - `Character.java` 中 `awayFromWorld` 默认为 `true`，真实客户端由登录包处理流程置为 `false`；无头 Bot 未走登录网络包流程，始终为 `true`；
  - [MapleMap.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/MapleMap.java) 的 `cleanupGhostPlayers()` 扫描时未排除 Bot。
- **修复方案**：
  - 在 `MapleMap.cleanupGhostPlayers()` 中增加 `!isBot(c)` 过滤；
  - 在 `Character.isAwayFromWorld()` 对 Bot 返回 `false`；`isLoggedInWorld()` 对 Bot 返回 `!this.isAwayFromWorld()`（使怪物仇恨与掉落判定对 Bot 正常生效，同时不触发布尔字段 `loggedIn` 避免触发自动存档异常）；
  - 在 `BotGeneration.addBotFromServer` 中补充 `fakechar.setEnteredChannelWorld()`，移除时触发 `setDisconnectedFromChannelWorld()` 对齐生命周期。

### 5. Bot 跨图传送卡住掉线误判修复
- **现象**：Bot 切换地图时每 300ms 刷屏告警 `玩家 [xxx] 在切换到地图 [xxx] 时卡住了`，随后断线移出 Storage，但后台状态机仍在每 300ms 尝试移动，导致死循环刷屏。
- **根因分析**：
  - `Character.changeMapInternal` 对玩家执行了 `client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null` 防作弊校验；Bot 未注册进普通网络玩家 Storage，导致走入 `else` 分支。
- **修复方案**：
  - 在 [Character.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/Character.java) 的 `changeMapInternal` 校验条件中加入 `isBot(this)` 豁免；
  - 在 `Character.getDefault(Client c)` 中补全 `ret.world = c != null ? c.getWorld() : 0;`。

### 6. FMBot 逛店议价与购买人工摆摊 NPE 修复
- **现象 1 (议价 NPE)**：自由市场逛店买货机器人尝试发言时，抛出 `PlayerShop.getVisitorSlot: Cannot invoke getName because chr is null`。
  - **根因**：原代码将请求转调给 `chat(chr.getClient(), chat)`，无头客户端 `BotClient` 无单例 `player`（返回 null）。
  - **修复**：在 [PlayerShop.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/PlayerShop.java) 中将核心实现收口至 `chat(Character chr, String chat)`，并补充严格判空。
- **现象 2 (购买 NPE 与数据库报错)**：FMBot 购买人工商人商品时抛出 `World.getPlayerStorage() is null`。
  - **根因**：人工商人（`HiredMerchantArtificial`）为纯内存模拟（ID > 20000，world 为 -123），数据库中无对应角色，执行 `saveItems` 或更新 MerchantMesos 会报错。
  - **修复**：在 [HiredMerchant.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/maps/HiredMerchant.java) 增加 `!(this instanceof HiredMerchantArtificial)` 判定，对人工摆摊在内存直接扣除结算，跳过所有数据库操作；在 [HiredMerchantArtificial.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/FreeMarket/HiredMerchantArtificial.java) 将 `saveItems` 重写为空实现；对真实商人补充空安全防护。

### 7. TownPins 边车文件缺失与优雅回退
- **现象**：服务端启动时报 `FileNotFoundException: TownPins.txt (系统找不到指定的文件)`。
- **根因**：`TownPinsStore` 用于记录 GM 指令 `!env townpresence mark` 动态添加的城镇坐标边车，未标记前文件默认不存在；`SoloMaplingResourceLoader` 未检查文件存在性即创建 `FileReader`。
- **修复**：在 [SoloMaplingResourceLoader.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/server/SoloMaplingResourceLoader.java) 的 `getReader` 文件系统分支增加 `Files.exists` 检测，缺失时优雅返回空 `StringReader("")`；并在 resources 中补充 `TownPins.txt` 模板注释文件。

### 8. 通道与世界静态引用时序隐患消除
- **根因与修复**：原代码中 `SoloMaplingUtilities.channel`、`SoloMaplingUtilities.world`、`SoloMaplingConstants.mainChannel` 均为 `public static final`，若在 Server 初始化前触发类加载将永久固化为 `null`。将其全量重构为动态方法 `getChannel()`、`getWorld()` 与 `getMainChannel()`，并在调用点接入。

### 9. 查看 Bot 角色信息与升级公会 NPE 修复
- **现象 1 (查看角色信息 NPE)**：右键查看 Bot 信息时报 `chr.getCashShop().getWishList()` 产生 NullPointerException。
  - **修复**：在 [CashShop.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/server/CashShop.java) 与 [MonsterBook.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/MonsterBook.java) 新增纯内存零参构造函数；在 [Character.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/Character.java) 实现安全懒加载 Getter（`getCashShop()`, `getMonsterBook()`, `getMGC()`）；在 [PacketCreator.charInfo](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/util/PacketCreator.java) 对商城、图鉴、公会联盟进行判空防护。
- **现象 2 (升级公会 NPE)**：Bot 打怪升级时报 `this.mgc is null`。
  - **修复**：在 [Character.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/client/Character.java) 的 `guildUpdate()` 首行加入 `if (this.guildId < 1) return;`，并对 `getGuild()` 广播与 `GuildCharacter` 构造函数补充空指针防御。

---

## 四、Bot 伴侣管理、招募等级限制与野外生态治理

为了提供良好的游戏内操作体验，对 Bot 伴侣管理、组队交互以及野外生成平衡性进行了系统化重构：

### 1. 帮助中心集成与 Bot 管理脚本 (9900001.js & 9000055.js)
- **帮助中心入口**：在快捷脚本中心 [9900001.js](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/scripts-zh-CN/npc/9900001.js) 菜单新增 `#L90#Bot伴侣管理#l`，点击触发 `cm.openNpc(9000055)`；
- **核心控制脚本 (9000055.js)**：
  - **【招募伴侣】**：支持玩家自选战士、法师、弓手、飞侠或随机职业伴侣，生成后自动建队/入队并设为 `FOLLOWER` 实时跟随；
  - **【战斗指令】**：一键将队伍内所有伴侣切换为 `TRAINING_BOT` 并本地图驻守（`markStationHere`），自动索敌打怪，全队共享经验与掉落；
  - **【行军指令】**：一键将伴侣切回 `FOLLOWER` 模式，寸步不离跟随队长跑图与穿门；
  - **【离队管理】**：安全请离队伍中的所有伴侣 Bot；
  - **【野外召唤】**：在当前地图生成独立的自主打怪 Bot（不占用玩家队伍）；
  - **【清理地图】**：提供区分权限的清理逻辑；
  - **【使用指引】**：提供气泡关键词指令与机制说明。

### 2. GraalVM 脚本引擎兼容性修复
- **类包路径修正**：修正 `MapMobIndex` 包路径为 `org.gms.soloMapling.ArtificialPlayer.BotGrindSystem.MapMobIndex`（排除了错误的 `Field` 包路径）；
- **Java 集合无 get 方法错误**：针对 GraalJS 对 Java 原生 `Collection`（如 `map.getCharacters()`）无 `get(index)` 索引的问题，编写通用的 `toJsArray(javaCollection)` 转换函数，通过原生 `iterator()` 转换为 JS 数组访问；
- **离线角色回退**：对队伍角色的 `getPlayer()` 补充 `BotHelpers.getCharFromChannelStorage(pc.getId())` 空回退防护。

### 3. v083 客户端 Emoji 乱码与原生样式改造
- **现象**：客户端选项前显示 `? 战士 (Warrior)` 等问号乱码；
- **根因**：冒险岛 v083 客户端字库不兼容 Unicode Emoji；
- **修复**：移除所有 Emoji 字符，替换为冒险岛原生文本标签与颜色代码（如 `#b[战士]#k`、`#r[法师]#k`、`#g[弓手]#k`、`#d[飞侠]#k`、`#k[随机]#k`）。

### 4. 清理地图后幽灵打怪修复与生命周期彻底注销
- **现象**：清理地图后，若再次点击“战斗指令”，Bot 会作为看不见的“幽灵”在后台继续秒怪并给玩家加 Buff；
- **根因**：原 `removeBotFromServer` 仅从地图移除玩家，未关闭 `BotSM` 状态机任务（`BotTickService` 仍在后台调度），未停用 `GCMovement`，未移出队伍；随后点击指令再次触发了行为树；
- **修复方案**：
  - **服务端注销**：在 [BotGeneration.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotGeneration.java) 的 `removeBotFromServer` 中彻底注销 `BotSM`、在 `BotTickService` 注销定时器、停用 `GCMovement`、清除 Buff 驱动，并在 Bot 在队伍时向世界广播 `PartyOperation.LEAVE` 强制退队；
  - **脚本端校验**：在 [9000055.js](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/scripts-zh-CN/npc/9000055.js) 的指令处理中跳过未在当前地图或已失效的 Bot。

### 5. 清理权限隔离与队伍保护机制
- 重构地图清理逻辑，严格保护联机多玩家体验：
  - **`[仅清理野外Bot]`**：严格限制为仅清理 `c.getParty() == null` 的野外游荡 Bot，处于队伍中的伴侣一律跳过保留；
  - **`[清理本地图与我的伴侣]`**：严格仅清理野外无队伍 Bot 以及归属于当前玩家队伍的伴侣（`myParty.getId() == botParty.getId()`）；
  - **核心防线**：**绝对禁止清理其他玩家队伍中的伴侣 Bot**。

### 6. 伴侣招募与手动邀请 10 级限制
- **伴侣招募限制**：
  - 动态计算 `minLevel = Math.max(10, pLevel - 10)` 和 `maxLevel = Math.min(200, Math.max(minLevel + 1, pLevel + 10))`；
  - 修复 [BotGeneration.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotGeneration.java) 中随机职业（`baseClass <= 0`）被硬编码覆盖为默认 `10~80` 级的缺陷，确保随机职业同样严格遵循玩家等级 $\pm 10$ 级范围；
- **手动邀请组队等级限制**：
  - 在 [PartyOperationHandler.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/net/server/channel/handlers/PartyOperationHandler.java) 中判定 `isBot(invited)`：若等级差超过 10 级，拦截并弹出服务器黄字提示：`"无法邀请与您等级相差超过10级的Bot伴侣加入队伍。"`；
  - 真实玩家（`!isBot(invited)`）不受 10 级限制，维持原版自由组队机制；
  - 在 `BotPartyCommands.botAcceptPartyInvite` 与 `BotRecruitManager.rollPartyAsk` 增加双重防御。

### 7. 野外 Bot 怪物等级自适应与单图容量保护
- **怪物等级自适应**：
  - 在 [9000055.js](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/scripts-zh-CN/npc/9000055.js) 实现 `getMapRecommendedLevelRange`：通过 `MapMobIndex.level(mapId)` 获取怪物等级中位数，若无则统计现场活怪平均等级；
  - 低级/新手猎场（怪物 $\le 10$ 级，如射手训练场）：生成等级为 $[ \max(1, \text{mapLevel} - 2), \min(15, \text{mapLevel} + 3) ]$，生成个位数新手初学者或剑士，彻底解决低级怪区生成高级 Bot 的违和感；
  - 中高级猎场：生成等级为 $[ \text{mapLevel} - 5, \text{mapLevel} + 5 ]$；
- **单图防泛滥上限**：
  - 设置单图野外 Bot 最大数量上限 `MAX_MAP_WILD_BOTS = 5`，超过则拦截生成并提示清理。

### 8. 开机预设群落配比与密度优化
- 在 [EnvironmentManager.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/Environment/EnvironmentManager.java) 中治理 Wave 8 开机预设群落：
  - 射手村（Henesys）初始等级从 `10~95` 修正为与金银岛新手区相符的 `10~30`；
  - 废都、勇士、魔法密林统一调优为 `10~35`；
  - 初始群落总数由 230+ 精简至 130 左右，消除开机拥堵与大面积占图。

### 9. 组队交互体验重构：右键直接邀请 100% 同意入队
- **改造背景**：原 SoloMapling 采用繁琐的白字对话招募（白字喊话 $\rightarrow$ 弹出菜单 $\rightarrow$ 几率判定 $\rightarrow$ 200秒 ARMED 窗口），直接右键会判定 `!armedMatch` 拒绝入队；
- **改造实现**：
  - [PartyOperationHandler.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/net/server/channel/handlers/PartyOperationHandler.java)：豁免 Bot 受新手/10级限制校验，邀请成功创建后检测为 Bot 则直接加入 `BotPartyQueue` 并同步调用 `BotPartyCommands.botAcceptPartyInvite` 即时同意；
  - [BotRecruitManager.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotPartySystem/BotRecruitManager.java)：移除拒绝对话冷却与概率衰减，直接登记并入队；
  - [BotPartyCommands.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotPartySystem/BotPartyCommands.java)：入队后自动将原 `SocialBot`、`TownWandererBot`、`TrainingBot` 无缝转换为 `FollowerBot` 伴侣并绑定队长；
  - [FollowerBot.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotTypes/FollowerBot.java)：放宽原有 `leaderId` 严格限制，无队伍伴侣收到真实玩家邀请即刻接受。

---

## 五、Bot 国际化 (i18n) 与全量中文本地化

构建了原生的多语言框架，并完成全部文本与对话包的高质量中文化：

### 1. i18n 核心管理架构与资源路由
- **[SoloMaplingI18n.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/server/SoloMaplingI18n.java)**:
  - 核心单例管理类，默认启用中文支持（`isChinese() == true`），可通过 JVM 启动参数 `-DsoloMapling.language=en` 随时平滑切换回原版英文；
  - 提供双向路径解析 `resolveLocalizedResource(basePath, filename)`：中文模式下优先解析 `basePath/zh-CN/filename`，未命中时平滑回退至原版路径，杜绝资源缺失引发的系统中断；
- **[SoloMaplingResourceLoader.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/server/SoloMaplingResourceLoader.java)**:
  - 扩展 `hasResource` 接口，支持 Classpath 与多层文件系统资源探测；
  - `getReader` 全面强制采用 `StandardCharsets.UTF_8` 解码，杜绝 Windows 平台默认 GBK 环境读取 YAML 引发的中文乱码。

### 2. Bot 角色名称规范与“仙”字库 (GBK <= 12字节)
- **“仙”字开头规范**：由 `SoloMaplingI18n.formatBotName(String rawName)` 强制格式化，所有中文环境下的 Bot 名称统一以汉字 **“仙”** 开头；
- **GBK 长度合规**：根据冒险岛 v083 客户端限制，角色名最大长度为 12 字节（GBK 编码下每个汉字 2 字节，最大 6 个汉字）；`formatBotName` 会在 GBK 字节层面自动安全截断，杜绝客户端截断问号乱码；
- **500+ 中文个性化名称库**（位于 `FMNameDesc/zh-CN/randomRealMaplestoryIGNs.txt`）：
  - **网络热梗**：仙坤坤、仙保国、仙纯一郎、仙泰裤辣、仙华强买瓜、仙原神启动等；
  - **修真玄幻**：仙尊韩立、仙尊萧炎、仙尊石昊、仙尊叶凡、仙尊林动、仙徐凤年、仙陈平安等；
  - **经典游戏/仙侠**：仙剑逍遥、仙剑灵儿、仙剑月如、仙剑景天、仙剑雪见、仙剑重楼等；
  - **国漫神话**：仙悟空、仙哪吒、仙二郎、仙索隆、仙鸣人等。

### 3. 职业与分类中文描述库
完整构建了 `src/main/resources/soloMapling/FMNameDesc/zh-CN/`（及自由市场子目录）下的 12 种描述文本库：
- `warriorDesc.txt`（战士店名描述）
- `mageDesc.txt`（法师店名描述）
- `bowmanDesc.txt`（弓手店名描述）
- `thiefDesc.txt`（飞侠店名描述）
- `commonDesc.txt`（通用装备店名描述）
- `scrollsDesc.txt`（卷轴专卖描述）
- `useableDesc.txt`（消耗药水描述）
- `etcDesc.txt`（其他收集品描述）
- `chairDesc.txt`（休闲椅子描述）
- `shortWordDesc.txt`（简短短语）
- `FMClans.txt`（摆摊家族名号）
- `emojiFaces.txt`（颜文字表情）

### 4. 全部 20 个 YAML 对话包 1:1 中文翻译
位于 `src/main/resources/soloMapling/BotDialoguePack/zh-CN/`，100% 完整保留原版 YAML 键名、表情 Emote 数组、等待延时（wait）以及全部上下文占位符（`{PLAYER_NAME}`, `{PLAYER_LEVEL}`, `{PLAYER_JOB}`, `{PLAYER_GUILD}`, `{MAP}`, `{MOB}`, `{DROP}`, `{item}`, `{price}` 等）：

| 对话包文件名 | 覆盖业务功能与场景 |
| :--- | :--- |
| `FollowerBotDialogue.yaml` | 跟班伴侣对话（带路、驻守打怪、跟丢、解散队伍） |
| `TownChatterDialogue.yaml` | 城镇双人闲聊对话（日常趣事、八卦交流） |
| `TownWandererDialogue.yaml` | 城镇闲逛 Bot 问候、天气吐槽与升级道贺 |
| `ConversationDialogue.yaml` | 城镇多角色剧本式长对话（包含全部 303 个经典互动剧本） |
| `TrainingBotDialogue.yaml` | 野外打怪练级、残血求救、抢怪吐槽、合图与组队 |
| `SocialBotDialogue.yaml` | 社交 Bot 好友交互、搭讪闲聊 |
| `MegaphoneDialogue.yaml` | 全服喇叭喊话（生日祝福、公会招人、卖卷收金、炫耀装逼） |
| `SocialHotPotatoDialogue.yaml` | 传话闲扯与日常游戏机制吐槽 |
| `FMBotDialogue.yaml` | 自由市场淘货、逛摊与采购反馈 |
| `MerchantBotDialogue.yaml` | 摆摊商贩招揽生意、经典热梗叫卖 |
| `ShopOfferDialogue.yaml` | 自由市场进店出价、还价与议价协商 |
| `ScrollingBotDialogue.yaml` | 砸卷 Bot 成功狂喜、连成庆祝与炸装崩溃 |
| `DropGameBotDialogue.yaml` | 丢物抢宝小游戏主持台词与流程把控 |
| `DropGameSpectatorDialogue.yaml` | 丢物抢宝观众喝彩、哄抢与起哄吐槽 |
| `DropGameLootPool.yaml` | 丢物抢宝奖池配置本地化 |
| `BlackjackDealerBotDialogue.yaml` | 21点荷官发牌、要牌、停牌与结算台词 |
| `GameZoneHostBotDialogue.yaml` | 娱乐中心向导招待台词 |
| `TutorialBotDialogue.yaml` | 新手营定向指引向导台词与启动物资赠送 |
| `JQBotDialogue.yaml` | 忍耐跳跳乐任务挑战、跳关与抓狂吐槽 |
| `HenesysBotDialogue.yaml` | 射手村专属经典 NPC/Bot 对话 |

### 5. 代码层本地化集成与中英双语指令兼容
- **对话加载器路由**：在 `BotDialogueHandler`、`ConversationManager`、`TownChatterLines`、`DropGameLootPool` 中全面接入 `SoloMaplingI18n.resolveLocalizedResource` 路径解析；
- **头顶气泡操作选项与双语关键词**：
  - **`FollowerBot`**：气泡选项显示 `"陪我在这里打怪！"`、`"取消"`；关键词支持输入 `1`、`打怪`、`练级`、`刷怪`、`停在这` 或 `train`；
  - **`TrainingBot`**：气泡选项显示 `"练级还顺利吗？"`、`"想一起组队吗？"`、`"跟我来！"`、`"再见"`；关键词支持输入 `1~4`、`顺利`、`打怪`、`组队`、`一起`、`跟我来` 或 `party` / `follow` 等双语指令；
- **组队提示**：[BotPartyCommands.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotPartySystem/BotPartyCommands.java) 入队欢迎与拒绝提示全面汉化。

---

## 六、自由市场与交易系统深度汉化

自由市场（FM）作为 SoloMapling 最核心的经济拟真模块，对所有英文硬编码进行了地毯式清洗与本土化：

### 1. 买卖与点券喊话机器人汉化
- **[BuyingMerchantBot.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotTypes/BuyingMerchantBot.java)**:
  - 买入前缀：从 `Buying>`, `B>`, `BUY>` 汉化为 `收>`, `收`, `收>>`, `高价收>`, `收收收>`, `求购>`；
  - 买入后缀：从 `Trade Me`, `PM me`, `hmu`, `no scammers`, `no lowball`, `no weebs` 汉化为 `点我交易`, `带价密`, `直接点我交易`, `速点交易`, `骗子绕道`, `黑人绕道`, `妹子优先`, `不墨迹的来`；
  - 英文全大写逻辑（`toUpperCase`）在中文环境下自动旁路，保持中文标点与语感自然。
- **[SellingMerchantBot.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotTypes/SellingMerchantBot.java)**:
  - 售卖前缀：从 `Selling>`, `S>`, `SELL>` 汉化为 `出>`, `出`, `出>>`, `急出>`, `甩卖>`, `急甩>`；
  - 售卖后缀：从 `You Offer`, `Offer`, `Trade Me`, `PM me`, `no time wasters`, `No Spanish` 汉化为 `代价密`, `带价来`, `诚心带价`, `点我交易`, `诚信交易`, `在线等`, `不墨迹的来`。
- **[NXMerchantBot.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotTypes/NXMerchantBot.java)**:
  - 喊话汉化：`"出10k点券卡密，5000万金币，点我交易！"`、`"出> 10k点卷卡密 5000w，不议价"`、`"点券卡密 10k >> 5000w 点我交易！！诚信第一"`；
  - 私聊与卡密发放：提示 `"私聊你发卡密了。"`、卡密格式 `"这是10k点券卡密... 请记好，注意不要输入横杠："`、`"祝游戏愉快！"`。
- **[FMEconomyManager.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/FreeMarket/FMEconomyManager.java)**:
  - 价格缩写 `formatPriceToShorthand` 全面适配中国玩家习惯：
    - `390k` $\rightarrow$ `39万`；
    - `760k` $\rightarrow$ `76万`；
    - `2.1m` $\rightarrow$ `210万`；
    - `50m` $\rightarrow$ `5000万`；
    - `1.5b` $\rightarrow$ `1.5亿`。

### 2. 摆摊店名、出价提示与装备属性招牌汉化
- **[ArtificialFreeMarket.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/FreeMarket/ArtificialFreeMarket.java)**:
  - 特价店追加词：`" Cheap"` $\rightarrow$ `" 特价"`；
  - 清仓店追加词：`" QUITTING SALE"` $\rightarrow$ `" 退坑甩卖"`；
  - 1金币店招牌：`" 1 MESO SHOP!!!"` $\rightarrow$ `" 1金币店!!!"`。
- **[FMShopDescGen.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/FreeMarket/FMShopDescGen.java)**:
  - 出价提示（`getOfferableDescription`）：从 `"Leave Offer"`, `"Offer"`, `"L/O"` 汉化为 `"带价来"`, `"代价密"`, `"诚心留言"`, `"自带价"`, `"留言出价"`, `"带诚意代价"`, `"不墨迹代价"`, `"诚心带价"`；
  - 装备属性强化招牌（`advertiseBestEquip`）：
    - 属性缩写 `dex`, `str`, `int`, `luk`, `watt`, `matt` 翻译为 `"敏捷"`, `"力量"`, `"智力"`, `"运气"`, `"物攻"`, `"魔攻"`（如 `23 dex 黑飘云之衣` $\rightarrow$ `23敏捷 黑飘云之衣`）；
    - 极品未指定属性：`"Godly "` $\rightarrow$ `"极品 "`；白板未砸卷装备：`"clean "` $\rightarrow$ `"天然 "`；
  - RWT 货币缩写（`advertiseRWTCurrencies`）：支持 `"点卷"`, `"金币"`, `"白卷"`, `"枫叶"`, `"混沌"`；
  - 角色名生成管道接入 `SoloMaplingI18n.formatBotName(...)`，保证招牌名字 100% 合法。
- **[FMShopDescriptionManager.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/FreeMarket/FMShopDescriptionManager.java)**:
  - 店铺欢迎语：`"Welcome to " + owner + "'s Shop!"` $\rightarrow$ `"欢迎光临 " + owner + " 的小店！"`。
- **[ShopOfferWelcome.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/FreeMarket/ShopOfferSystem/ShopOfferWelcome.java)** & **[ShopOfferResponse.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/FreeMarket/ShopOfferSystem/ShopOfferResponse.java)**:
  - 进店出价格式指引与改价私聊通知全面中文化。

### 3. 跟 Bot 交易窗口聊天与互动流程汉化
- **[BotTradeSM.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotTradeSystem/BotTradeSM.java)**:
  - 诉求提示 `generateWantsMessageString()` 汉化：
    - `"想要 5000万金币"`
    - `"想要 拳套攻击卷轴"`
    - `"想要 5000万金币 和 拳套攻击卷轴"`
    - `"想要 2个 拳套攻击卷轴"`
    - 无特定诉求：从 `"nothing specific"` 汉化为 `"随便给点什么都行"`；
  - 交易过程交互台词全量中文化：
    - `"我现在没有什么想要交易的"`
    - `"这是我要出售的，看看吧！"`
    - `"这是给你道具的 39万 金币！"`
    - `"交易没问题，确认了！"`
    - `"多谢，合作愉快！"`
    - `"为什么取消交易了？"`
    - `"交易超时！"`
    - `"算了，不换了。再见。"`

### 4. 新手向导与丢丢乐小游戏交易汉化
- **[TutorialBot.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotTypes/TutorialBot.java)**:
  - 交易发言汉化：`"给你一些新手启动物资！"`，`"10亿金币、药水、卷轴、飞镖和装备，全都是你的！"`；
- **[DropGameBot.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/main/java/org/gms/soloMapling/ArtificialPlayer/BotTypes/DropGameBot.java)**:
  - 交易窗口规则与判定全量汉化：`"丢丢乐游戏！中级: 1000万 / 高级: 5000万"`、`"放入金币并点击确定！"`、`"太慢了！交易超时。"` 等。

---

## 七、验证与测试体系汇总

所有阶段的工作均通过严格的编译构建与单元测试验证：

1. **工程编译与打包测试**：
   - 执行 `.\SoloMapling-0.3\mvnw.cmd test-compile -f gms-server\pom.xml`：
     - **编译结果**：1403 个 Java 源码文件全量编译成功（**BUILD SUCCESS**）；
   - 执行 `.\SoloMapling-0.3\mvnw.cmd package -DskipTests`：
     - **打包结果**：成功输出内嵌依赖的可执行产物 `gms-server\target\BeiDou.jar`（**BUILD SUCCESS**）。

2. **单元测试矩阵 ([BotResourceLoadingTest.java](file:///d:/code/gamedev/083/BeiDou-Server/gms-server/src/test/java/BotResourceLoadingTest.java))**：
   - 执行 `.\SoloMapling-0.3\mvnw.cmd test -Dtest=BotResourceLoadingTest -f gms-server\pom.xml`：
     - `testChineseBotNames`：抽样 50 次 Bot 随机名称，100% 以“仙”开头且 GBK 编码长度严格 $\le 12$ 字节；
     - `testChinesePriceFormatting`：测试 `390k` $\rightarrow$ `39万`、`760k` $\rightarrow$ `76万`、`2.1m` $\rightarrow$ `210万`、`50m` $\rightarrow$ `5000万`、`100m` $\rightarrow$ `1亿`、`500` $\rightarrow$ `500` 格式化准确无误；
     - `testMerchantBotChineseMessages`：多轮检验买入与卖出喊话，确认零英文残留、前缀/后缀中文语法地道、道具名解析正确；
     - `testFMShopOfferableDescriptions`：验证摆摊出价描述均为经典国服中文表达；
     - `testChineseDialogueLoading`：验证中文资源目录路由正常，YAML 节点解析成功；
     - `testYamlConfigs` / `testLegacyPathResolution` / `testRelativePathResolution`：验证各级路径探测与跨平台回退兼容性；
   - **测试结果**：**Tests run: 9, Failures: 0, Errors: 0, Skipped: 0**（全部通过）。

---

## 八、Git 提交记录与 Author 规范

### 1. Git Author 配置与历史重写
- **配置固定**：
  ```bash
  git config user.name "紫陌"
  git config user.email "dev@zimo.cc"
  ```
- **历史提交重写**：通过 `git filter-branch` 将 `Bot` 分支上的全部历史提交（共 19 次 Commit）作者与提交者信息统一重写为 `紫陌 <dev@zimo.cc>`，后续开发提交自动继承该身份。

### 2. 规范化提交记录汇总
严格遵守仓库 Git Commit Message 规范：

```text
feat: 1. Port SoloMapling autonomous Bot system into gms-server
      2. Add Flyway migration V1.11.7 for bot accounts and NPC support
      3. Hook bot lifecycle, movement, combat, trading, and shops into BeiDou engine

fix:  1. 修复 MapleMap 被动清理幽灵玩家误清理活跃 Bot 的问题
      2. 完善 Bot 在 Character 中的 isAwayFromWorld 和 isLoggedInWorld 状态
      3. 确保 BotClient 正确对齐世界与频道生命周期

fix:  1. 关闭 BotLogger 频繁同步写磁盘日志，消除文件锁与 IO 阻塞
      2. 机器人生成改用纯内存 Character.getDefault 实例，消除冷启动海量无意义数据库查询
      3. 优化 Druid 数据库连接池配置，增加借出超时与连接上限，杜绝假死
      4. 调优 Wave 8 城镇练级机器人生成基数，缓解单机 CPU 调度压力

fix:  1. 修复 PlayerShop 中 BotClient 无单例玩家导致的 NullPointerException
      2. 优化自由市场商店 chat 接口空指针防护

fix:  1. SoloMaplingResourceLoader 回退文件系统时增加存在性检查，缺失时优雅返回空字符流
      2. 新增初始 TownPins.txt 边车文件模板

fix:  1. 修复 Character.changeMapInternal 误将无头 Bot 判定为卡住掉线的问题
      2. 修复 FMBot 购买人工摆摊商品时触发的 World.getPlayerStorage 空指针异常
      3. 消除 SoloMaplingUtilities 中 channel 与 world 静态常量引用的时序隐患

fix:  1. 为 CashShop 和 MonsterBook 提供零 DB 纯内存默认构造函数
      2. 在 Character.getDefault 中初始化商城与图鉴实例，并提供安全懒加载 Getter
      3. 完善 PacketCreator.charInfo 中针对虚拟角色的全套空指针防御

feat: 1. 组队邀请 Bot 改为 100% 直接同意入队，无需前置白字对话招募
      2. 自动将入队的社交/练级 Bot 转换为跟随型同伴 (FollowerBot)
      3. 豁免 Bot 受到玩家等级 10 级限制的入队拦截校验

fix:  1. 修复 Character.guildUpdate 未判断公会成员资格导致 Bot 升级触发的空指针异常
      2. 补充 levelUp 中家族广播和 GuildCharacter 构造函数的空安全防护

fix:  1. 为 XMLDomMapleData 引入统一根文档锁，彻底解决虚拟线程并发解析 Xerces DOM 树的缓存破坏异常
      2. 全面完善 XML 节点属性与子节点读取的判空检查
      3. 将 ItemInformationProvider 名称缓存升级为 ConcurrentHashMap 并引入负向缓存标记

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
