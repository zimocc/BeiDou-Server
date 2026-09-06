/**
 * @description Bot 控制管理与召唤交互中心 (9000055)
 * 提供 Bot 队友招募、一键指挥打怪/跟随、本地图 Bot 召唤与清理功能。
 */

const BotGeneration = Java.type('org.gms.soloMapling.ArtificialPlayer.BotGeneration');
const BotTypeManager = Java.type('org.gms.soloMapling.ArtificialPlayer.BotTypeManager');
const BotRecruitManager = Java.type('org.gms.soloMapling.ArtificialPlayer.BotPartySystem.BotRecruitManager');
const BotPartyCommands = Java.type('org.gms.soloMapling.ArtificialPlayer.BotPartySystem.BotPartyCommands');
const BotHelpers = Java.type('org.gms.soloMapling.ArtificialPlayer.BotHelpers');
const CharacterStorage = Java.type('org.gms.soloMapling.ArtificialPlayer.BotMessagingSystem.CharacterStorage');
const GCMovement = Java.type('org.gms.soloMapling.ArtificialPlayer.GCMoveSystem.GCMovement');
const Party = Java.type('org.gms.net.server.world.Party');
const MapMobIndex = Java.type('org.gms.soloMapling.ArtificialPlayer.BotGrindSystem.MapMobIndex');

var status = -1;
var selectedOption = -1;

function toJsArray(javaCollection) {
    var arr = [];
    if (javaCollection != null) {
        var it = javaCollection.iterator();
        while (it.hasNext()) {
            arr.push(it.next());
        }
    }
    return arr;
}

function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode === 1) {
        status++;
    } else if (mode === -1) {
        status--;
    } else {
        cm.dispose();
        return;
    }

    var player = cm.getPlayer();
    if (player == null) {
        cm.dispose();
        return;
    }

    if (status === 0) {
        var party = player.getParty();
        var partyBotCount = 0;
        if (party != null) {
            var members = toJsArray(party.getMembers());
            for (var i = 0; i < members.length; i++) {
                var pc = members[i];
                if (pc != null) {
                    var p = pc.getPlayer();
                    if (p == null) {
                        p = BotHelpers.getCharFromChannelStorage(pc.getId());
                    }
                    if (p != null && BotHelpers.isBot(p)) {
                        partyBotCount++;
                    }
                }
            }
        }

        var map = player.getMap();
        var mapBotCount = 0;
        if (map != null) {
            var chrs = toJsArray(map.getCharacters());
            for (var i = 0; i < chrs.length; i++) {
                if (BotHelpers.isBot(chrs[i])) {
                    mapBotCount++;
                }
            }
        }

        var text = "\t\t\t#e#b[ BeiDou 虚拟伴侣控制中心 ]#k#n\r\n\r\n";
        text += "您好，#r#e" + player.getName() + "#k#n！在这里您可以召唤虚拟同伴或指挥队伍内的 Bot。\r\n";
        text += "当前队伍 Bot 伴侣数量：#r" + partyBotCount + "#k 人 (队伍上限 6 人)\r\n";
        text += "当前地图 Bot 总数量：#b" + mapBotCount + "#k 人\r\n\r\n";

        text += "#L1##b[招募伴侣]#k 召唤 Bot 队友加入队伍 (等级差10级以内, 自动跟随)#l\r\n";
        text += "#L2##r[战斗指令]#k 命令队伍内所有 Bot 开始打怪 (就地战斗)#l\r\n";
        text += "#L3##g[行军指令]#k 命令队伍内所有 Bot 停止打怪并跟随队长#l\r\n";
        text += "#L4##d[离队管理]#k 将队伍内的所有 Bot 请离队伍#l\r\n";
        text += "#L5##b[野外召唤]#k 在本地图召唤自主刷怪 Bot (不入队)#l\r\n";
        text += "#L6##r[清理地图]#k 清理本地图上的虚拟 Bot#l\r\n";
        text += "#L7##k[帮助说明]#k 查看 Bot 玩法与等级限制说明#l\r\n";

        cm.sendSimple(text);
    } else if (status === 1) {
        selectedOption = selection;
        switch (selection) {
            case 1: // 召唤 Bot 入队
                var pLevel = player.getLevel();
                var minL = Math.max(10, pLevel - 10);
                var maxL = Math.min(200, Math.max(minL, pLevel + 10));
                var text = "#e#b请选择您希望招募的 Bot 职业伴侣：#k#n\r\n\r\n";
                text += "伴侣等级将根据您的当前等级（#r" + pLevel + " 级#k）自动生成在 #b" + minL + " ~ " + maxL + " 级#k 之间（±10级以内）。\r\n";
                text += "召唤后将直接加入您的队伍并跟随您！\r\n\r\n";
                text += "#L11##b[战士]#k 强力近战与防御 (Warrior)#l\r\n";
                text += "#L12##r[法师]#k 华丽范围法术 (Magician)#l\r\n";
                text += "#L13##g[弓手]#k 远程精准射击 (Bowman)#l\r\n";
                text += "#L14##d[飞侠]#k 迅捷暴击刺杀 (Thief)#l\r\n";
                text += "#L15##k[随机]#k 随机职业伴侣#l\r\n";
                cm.sendSimple(text);
                break;

            case 2: // 全队打怪
                handlePartyAttack(player);
                break;

            case 3: // 全队跟随
                handlePartyFollow(player);
                break;

            case 4: // 移出队伍
                handlePartyDismissBots(player);
                break;

            case 5: // 野外召唤
                var range = getMapRecommendedLevelRange(player.getMap(), player);
                var text = "#e#b请选择召唤野外打怪 Bot 的职业类型：#k#n\r\n\r\n";
                text += "生成的 Bot 将在当前地图自动寻找怪物打怪，不占用队伍名额。\r\n";
                text += "当前地图怪物推荐等级：#r" + (range.mapLevel > 0 ? range.mapLevel + " 级" : "无怪物(自适应)") + "#k\r\n";
                text += "生成的野外 Bot 等级将适配为：#b" + range.minLevel + " ~ " + range.maxLevel + " 级#k。\r\n\r\n";
                text += "#L51##b[战士]#k 战士 (Warrior)#l\r\n";
                text += "#L52##r[法师]#k 魔法师 (Magician)#l\r\n";
                text += "#L53##g[弓手]#k 弓箭手 (Bowman)#l\r\n";
                text += "#L54##d[飞侠]#k 飞侠 (Thief)#l\r\n";
                text += "#L55##k[随机]#k 随机职业#l\r\n";
                cm.sendSimple(text);
                break;

            case 6: // 清理地图
                var text = "#e#b请选择您要执行的地图清理类型：#k#n\r\n\r\n";
                text += "#L61##b[清理野外Bot]#k 仅清理野外无队伍Bot (保留所有队伍伴侣)#l\r\n";
                text += "#L62##r[清理本地图与我的伴侣]#k 清理野外无队伍Bot及我的伴侣 (绝不影响他人伴侣)#l\r\n";
                cm.sendSimple(text);
                break;

            case 7: // 帮助
                handleShowHelp();
                break;

            default:
                cm.dispose();
                break;
        }
    } else if (status === 2) {
        if (selectedOption === 1) {
            // 招募伴侣入队
            var baseClass = 0;
            if (selection === 11) baseClass = 1;
            else if (selection === 12) baseClass = 2;
            else if (selection === 13) baseClass = 3;
            else if (selection === 14) baseClass = 4;
            else if (selection === 15) baseClass = (Math.floor(Math.random() * 4) + 1);
            handleSpawnPartyBot(player, baseClass);
        } else if (selectedOption === 5) {
            // 召唤野外打怪 Bot
            var baseClass = 0;
            if (selection === 51) baseClass = 1;
            else if (selection === 52) baseClass = 2;
            else if (selection === 53) baseClass = 3;
            else if (selection === 54) baseClass = 4;
            else if (selection === 55) baseClass = (Math.floor(Math.random() * 4) + 1);
            handleSpawnWildBot(player, baseClass);
        } else if (selectedOption === 6) {
            // 清理地图
            if (selection === 61) {
                handleClearWildBots(player);
            } else if (selection === 62) {
                handleClearAllMapBots(player);
            } else {
                cm.dispose();
            }
        } else {
            cm.dispose();
        }
    } else {
        cm.dispose();
    }
}

// 召唤伴侣并加入队伍 (限制在玩家等级差值10级以内)
function handleSpawnPartyBot(player, baseClass) {
    var party = player.getParty();
    if (party == null) {
        if (!Party.createParty(player, false)) {
            cm.sendOk("创建队伍失败，无法为伴侣分配队伍！");
            cm.dispose();
            return;
        }
        party = player.getParty();
    }

    if (toJsArray(party.getMembers()).length >= 6) {
        cm.sendOk("您的队伍人数已满（最多 6 人），无法再招募新的 Bot 伴侣！");
        cm.dispose();
        return;
    }

    var map = player.getMap();
    var pos = player.getPosition();
    var pLevel = player.getLevel();
    var minLevel = Math.max(10, pLevel - 10);
    var maxLevel = Math.min(200, Math.max(minLevel + 1, pLevel + 10));

    var botId = BotGeneration.createBot(pos, map, baseClass, minLevel, maxLevel);
    var botChr = BotHelpers.getCharFromChannelStorage(botId);

    if (botChr != null) {
        Party.joinParty(botChr, party.getId(), false);
        BotRecruitManager.setPendingLeader(botChr.getId(), player.getId());
        BotTypeManager.convertBotType(botChr, BotTypeManager.BotType.FOLLOWER_BOT);

        cm.sendOk("#b#e" + botChr.getName() + "#k#n (等级 #r" + botChr.getLevel() + "#k) 已成功招募并加入您的队伍！\r\n\r\n伴侣等级在您（" + pLevel + " 级）的 #r±10 级#k 范围内。\r\n当前处于#r[跟随状态]#k。进入战斗地图后，您可以通过本 NPC 发送【战斗指令】让其就地打怪！");
    } else {
        cm.sendOk("Bot 伴侣召唤成功（角色 ID：" + botId + "），正在进入世界...");
    }
    cm.dispose();
}

// 命令队伍 Bot 开始打怪
function handlePartyAttack(player) {
    var party = player.getParty();
    if (party == null) {
        cm.sendOk("您当前未加入任何队伍！");
        cm.dispose();
        return;
    }

    var map = player.getMap();
    if (map == null || MapMobIndex.level(map.getId()) < 0) {
        cm.sendOk("当前地图没有怪物（属于城镇或安全区），Bot 伴侣无法在此打怪！\r\n请先带领队伍前往野外狩猎地图。");
        cm.dispose();
        return;
    }

    var count = 0;
    var members = toJsArray(party.getMembers());
    for (var i = 0; i < members.length; i++) {
        var pc = members[i];
        if (pc != null) {
            var p = pc.getPlayer();
            if (p == null) {
                p = BotHelpers.getCharFromChannelStorage(pc.getId());
            }
            if (p != null && BotHelpers.isBot(p)) {
                // 防御校验：Bot 必须真实存在于当前地图中且未处于断线/清理状态
                if (p.getMap() == null || p.getMap().getId() != map.getId() || map.getCharacterById(p.getId()) == null || p.isAwayFromWorld()) {
                    continue;
                }
                BotRecruitManager.markStationHere(p.getId());
                GCMovement.stop(p);
                BotTypeManager.convertBotType(p, BotTypeManager.BotType.TRAINING_BOT);
                count++;
            }
        }
    }

    if (count > 0) {
        cm.sendOk("已成功指挥队伍内 #r#e" + count + "#k#n 位 Bot 伴侣在当前地图开始打怪！\r\n\r\n打怪经验和掉落将与队伍共享。需要移动时请选择【行军指令】使其继续跟随。");
    } else {
        cm.sendOk("当前队伍中没有在本地图的有效 Bot 伴侣。");
    }
    cm.dispose();
}

// 命令队伍 Bot 停止打怪并跟随
function handlePartyFollow(player) {
    var party = player.getParty();
    if (party == null) {
        cm.sendOk("您当前未加入任何队伍！");
        cm.dispose();
        return;
    }

    var map = player.getMap();
    var count = 0;
    var members = toJsArray(party.getMembers());
    for (var i = 0; i < members.length; i++) {
        var pc = members[i];
        if (pc != null) {
            var p = pc.getPlayer();
            if (p == null) {
                p = BotHelpers.getCharFromChannelStorage(pc.getId());
            }
            if (p != null && BotHelpers.isBot(p)) {
                // 防御校验：Bot 必须真实存在于当前地图中且未处于断线/清理状态
                if (p.getMap() == null || p.getMap().getId() != map.getId() || map.getCharacterById(p.getId()) == null || p.isAwayFromWorld()) {
                    continue;
                }
                BotRecruitManager.setPendingLeader(p.getId(), player.getId());
                BotTypeManager.convertBotType(p, BotTypeManager.BotType.FOLLOWER_BOT);
                count++;
            }
        }
    }

    if (count > 0) {
        cm.sendOk("已成功指挥队伍内 #b#e" + count + "#k#n 位 Bot 伴侣停止打怪并切换为#b[跟随模式]#k！\r\n它们将寸步不离跟随您跑图与穿过传送门。");
    } else {
        cm.sendOk("当前队伍中没有在本地图的有效 Bot 伴侣。");
    }
    cm.dispose();
}

// 请离队伍中的 Bot
function handlePartyDismissBots(player) {
    var party = player.getParty();
    if (party == null) {
        cm.sendOk("您当前未加入任何队伍！");
        cm.dispose();
        return;
    }

    var count = 0;
    var botList = [];
    var members = toJsArray(party.getMembers());
    for (var i = 0; i < members.length; i++) {
        var pc = members[i];
        if (pc != null) {
            var p = pc.getPlayer();
            if (p == null) {
                p = BotHelpers.getCharFromChannelStorage(pc.getId());
            }
            if (p != null && BotHelpers.isBot(p)) {
                botList.push(p);
            }
        }
    }

    for (var j = 0; j < botList.length; j++) {
        var botChr = botList[j];
        Party.expelFromParty(party, player.getClient(), botChr.getId());
        count++;
    }

    if (count > 0) {
        cm.sendOk("已将队伍内的 #r" + count + "#k 位 Bot 请离队伍。");
    } else {
        cm.sendOk("当前队伍内没有任何 Bot 伴侣。");
    }
    cm.dispose();
}

// 计算当前地图适合生成的野外 Bot 等级区间（以地图怪物等级为准）
function getMapRecommendedLevelRange(map, player) {
    if (map == null) {
        return { minLevel: 10, maxLevel: 20, mapLevel: -1 };
    }

    var mapId = map.getId();
    var mapLevel = -1;
    try {
        mapLevel = MapMobIndex.level(mapId);
    } catch (e) {}

    // 若 MapMobIndex 中未收录，尝试从当前地图已刷新的怪物中统计
    if (mapLevel <= 0) {
        try {
            var monsters = toJsArray(map.getAllMonsters());
            if (monsters.length > 0) {
                var mobLevels = [];
                for (var m = 0; m < monsters.length; m++) {
                    if (monsters[m] != null) {
                        mobLevels.push(monsters[m].getLevel());
                    }
                }
                if (mobLevels.length > 0) {
                    mobLevels.sort(function(a, b) { return a - b; });
                    mapLevel = mobLevels[Math.floor(mobLevels.length / 2)];
                }
            }
        } catch (e) {}
    }

    var minLevel = 1;
    var maxLevel = 10;
    if (mapLevel > 0) {
        // 符合地图等级：低级怪区(<=10级)生成低级新手/一转Bot，避免高级碾压
        if (mapLevel <= 10) {
            minLevel = Math.max(1, mapLevel - 2);
            maxLevel = Math.max(minLevel + 1, Math.min(15, mapLevel + 3));
        } else {
            minLevel = Math.max(10, mapLevel - 5);
            maxLevel = Math.min(200, Math.max(minLevel + 1, mapLevel + 5));
        }
    } else {
        // 地图完全没有怪物（如主城、自由市场等），回退为安全合理等级
        var pLevel = (player != null) ? player.getLevel() : 20;
        minLevel = Math.max(10, Math.min(30, pLevel - 5));
        maxLevel = Math.min(200, minLevel + 10);
    }
    return { minLevel: minLevel, maxLevel: maxLevel, mapLevel: mapLevel };
}

// 召唤野外自主打怪 Bot (符合地图等级，并具有单图数量上限保护)
function handleSpawnWildBot(player, baseClass) {
    var map = player.getMap();
    if (map == null) {
        cm.dispose();
        return;
    }

    // 检查地图 Bot 数量上限，防止 Bot 泛滥堆积
    var MAX_MAP_WILD_BOTS = 5;
    var chrs = toJsArray(map.getCharacters());
    var currentWildBots = 0;
    for (var i = 0; i < chrs.length; i++) {
        var c = chrs[i];
        if (c != null && BotHelpers.isBot(c) && c.getParty() == null) {
            currentWildBots++;
        }
    }
    if (currentWildBots >= MAX_MAP_WILD_BOTS) {
        cm.sendOk("当前地图已有 #r" + currentWildBots + "#k 位野外打怪 Bot（已达单张地图上限 " + MAX_MAP_WILD_BOTS + " 人）。\r\n为了服务器流畅度与生态平衡，请勿在同一地图生成过多 Bot！\r\n\r\n如需重新召唤，可先使用【清理地图】功能。");
        cm.dispose();
        return;
    }

    var pos = player.getPosition();
    var range = getMapRecommendedLevelRange(map, player);
    var minLevel = range.minLevel;
    var maxLevel = range.maxLevel;

    var botId = BotGeneration.createBot(pos, map, baseClass, minLevel, maxLevel);
    var botChr = BotHelpers.getCharFromChannelStorage(botId);

    if (botChr != null) {
        BotRecruitManager.markStationHere(botChr.getId());
        BotTypeManager.convertBotType(botChr, BotTypeManager.BotType.TRAINING_BOT);
        cm.sendOk("野外打怪 Bot #b#e" + botChr.getName() + "#k#n (等级 #r" + botChr.getLevel() + "#k) 已在当前地图生成并开始打怪！\r\n(等级已根据当前地图等级 " + (range.mapLevel > 0 ? range.mapLevel + " 级" : "自适应") + " 生成)");
    } else {
        cm.sendOk("野外打怪 Bot 生成成功（角色 ID：" + botId + "）。");
    }
    cm.dispose();
}

// 仅清理本地图野外无队伍的 Bot（绝不清理他人或自己的队伍伴侣）
function handleClearWildBots(player) {
    var map = player.getMap();
    if (map == null) {
        cm.dispose();
        return;
    }

    var botList = [];
    var chrs = toJsArray(map.getCharacters());
    for (var i = 0; i < chrs.length; i++) {
        var c = chrs[i];
        if (c != null && BotHelpers.isBot(c)) {
            // 只要 Bot 在任何队伍中（不论自己的还是别人的），绝对保留！
            if (c.getParty() != null) {
                continue;
            }
            botList.push(c);
        }
    }

    for (var j = 0; j < botList.length; j++) {
        BotGeneration.removeBotFromServer(botList[j]);
    }

    cm.sendOk("已成功清理当前地图上的 #r" + botList.length + "#k 位野外无队伍 Bot！\r\n所有队伍伴侣（包括您和他人队伍）均已为您完整保留。");
    cm.dispose();
}

// 清理本地图野外无队伍 Bot 与自己队伍的伴侣（绝不清理别人队伍里的 Bot）
function handleClearAllMapBots(player) {
    var map = player.getMap();
    if (map == null) {
        cm.dispose();
        return;
    }

    var myParty = player.getParty();
    var wildBots = [];
    var myPartyBots = [];

    var chrs = toJsArray(map.getCharacters());
    for (var i = 0; i < chrs.length; i++) {
        var c = chrs[i];
        if (c != null && BotHelpers.isBot(c)) {
            var botParty = c.getParty();
            if (botParty == null) {
                // 1. 无队伍的野外 Bot：清理
                wildBots.push(c);
            } else if (myParty != null && myParty.getId() == botParty.getId()) {
                // 2. 自己队伍里的 Bot 伴侣：清理并退队
                myPartyBots.push(c);
            } else {
                // 3. 别人队伍里的 Bot：绝对保护，不清理！
                continue;
            }
        }
    }

    // 清理自己队伍里的伴侣
    for (var k = 0; k < myPartyBots.length; k++) {
        var pBot = myPartyBots[k];
        try {
            Party.expelFromParty(myParty, player.getClient(), pBot.getId());
        } catch (e) {}
        BotGeneration.removeBotFromServer(pBot);
    }

    // 清理野外无队伍 Bot
    for (var j = 0; j < wildBots.length; j++) {
        BotGeneration.removeBotFromServer(wildBots[j]);
    }

    cm.sendOk("清理完成！已清理野外无队伍 Bot #r" + wildBots.length + "#k 位，退队并移除您的队伍伴侣 #r" + myPartyBots.length + "#k 位。\r\n#b注意：其他玩家队伍中的 Bot 伴侣不受任何影响，已完整保留。#k");
    cm.dispose();
}

// 玩法与帮助说明
function handleShowHelp() {
    var text = "\t\t\t#e#b[ Bot 虚拟伴侣交互指南 ]#k#n\r\n\r\n";
    text += "#e1. 组队与等级限制规则：#n\r\n";
    text += "   - 手动邀请 Bot 组队时，要求双方等级差距在 #r10 级以内#k；\r\n";
    text += "   - 右键点击等级差距 10 级以内的 Bot 选择【组队邀请】，Bot 将 100% 直接同意入队；\r\n";
    text += "   - 真实玩家之间的组队完全不受 10 级差距限制；\r\n";
    text += "   - 在本 NPC 选择【招募伴侣】，系统将自动生成与您等级差在 #r10 级以内#k 的 Bot 并加入队伍。\r\n\r\n";
    text += "#e2. 打怪与跟随：#n\r\n";
    text += "   - 队伍内的 Bot 默认处于【跟随模式】，紧跟队长跑图穿门。\r\n";
    text += "   - 队长可以在野外狩猎地图打开本 NPC，点击【战斗指令】，一键命令全队在场 Bot 展开攻击！\r\n";
    text += "   - 同样可在普通白字聊天输入 Bot 名字，在弹出的互动气泡中选择【Train here with me!】或【Follow me!】进行单体指挥。\r\n\r\n";
    text += "#e3. 地图生成与上限保护：#n\r\n";
    text += "   - 地图不会在玩家进入时自动生成 Bot，避免数量泛滥；\r\n";
    text += "   - 使用【野外召唤】时，Bot 等级严格根据当前地图的怪物等级生成；\r\n";
    text += "   - 单张地图设有野外 Bot 数量上限（最大 5 人），保障游戏流畅度。\r\n\r\n";
    text += "#e4. 清理安全保护规则：#n\r\n";
    text += "   - 本控制中心的所有清理功能，均受到队伍归属保护；\r\n";
    text += "   - 无论是清理野外还是全图清理，绝不会清理或影响其他玩家队伍中的 Bot 伴侣！\r\n";

    cm.sendOk(text);
    cm.dispose();
}
