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
            var members = party.getMembers();
            for (var i = 0; i < members.size(); i++) {
                var pc = members.get(i);
                if (pc != null && pc.getPlayer() != null && BotHelpers.isBot(pc.getPlayer())) {
                    partyBotCount++;
                }
            }
        }

        var map = player.getMap();
        var mapBotCount = 0;
        if (map != null) {
            var chrs = map.getCharacters();
            for (var i = 0; i < chrs.size(); i++) {
                if (BotHelpers.isBot(chrs.get(i))) {
                    mapBotCount++;
                }
            }
        }

        var text = "\t\t\t#e#b★ BeiDou 虚拟伴侣控制中心 ★#k#n\r\n\r\n";
        text += "您好，#r#e" + player.getName() + "#k#n！在这里您可以召唤虚拟同伴或指挥队伍内的 Bot。\r\n";
        text += "当前队伍 Bot 伴侣数量：#r" + partyBotCount + "#k 人 (队伍上限 6 人)\r\n";
        text += "当前地图 Bot 总数量：#b" + mapBotCount + "#k 人\r\n\r\n";

        text += "#L1##b【招募伴侣】#k召唤 Bot 队友加入队伍 (自动跟随)#l\r\n";
        text += "#L2##r【战斗指令】#k命令队伍内所有 Bot 开始打怪 (就地战斗)#l\r\n";
        text += "#L3##g【行军指令】#k命令队伍内所有 Bot 停止打怪并跟随队长#l\r\n";
        text += "#L4##d【离队管理】#k将队伍内的所有 Bot 请离队伍#l\r\n";
        text += "#L5##b【野外召唤】#k在本地图召唤自主刷怪 Bot (不入队)#l\r\n";
        text += "#L6##r【清理地图】#k清除当前地图上的所有虚拟 Bot#l\r\n";
        text += "#L7##k【帮助说明】#k查看 Bot 玩法与快捷聊天指令说明#l\r\n";

        cm.sendSimple(text);
    } else if (status === 1) {
        selectedOption = selection;
        switch (selection) {
            case 1: // 召唤 Bot 入队
                var text = "#e#b请选择您希望招募的 Bot 职业伴侣：#k#n\r\n\r\n";
                text += "伴侣将自动适配您的等级，召唤后直接加入您的队伍并跟随您！\r\n\r\n";
                text += "#L11#⚔ 战士 (Warrior)#l\r\n";
                text += "#L12#🔮 魔法师 (Magician)#l\r\n";
                text += "#L13#🏹 弓箭手 (Bowman)#l\r\n";
                text += "#L14#🗡 飞侠 (Thief)#l\r\n";
                text += "#L15#🎲 随机职业同伴#l\r\n";
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
                var text = "#e#b请选择召唤野外打怪 Bot 的职业类型：#k#n\r\n\r\n";
                text += "生成的 Bot 将在当前地图自动寻找怪物打怪，不占用队伍名额。\r\n\r\n";
                text += "#L51#⚔ 战士 (Warrior)#l\r\n";
                text += "#L52#🔮 魔法师 (Magician)#l\r\n";
                text += "#L53#🏹 弓箭手 (Bowman)#l\r\n";
                text += "#L54#🗡 飞侠 (Thief)#l\r\n";
                text += "#L55#🎲 随机职业#l\r\n";
                cm.sendSimple(text);
                break;

            case 6: // 清理地图
                handleClearMapBots(player);
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
            handleSpawnPartyBot(player, baseClass);
        } else if (selectedOption === 5) {
            // 召唤野外打怪 Bot
            var baseClass = 0;
            if (selection === 51) baseClass = 1;
            else if (selection === 52) baseClass = 2;
            else if (selection === 53) baseClass = 3;
            else if (selection === 54) baseClass = 4;
            handleSpawnWildBot(player, baseClass);
        } else {
            cm.dispose();
        }
    } else {
        cm.dispose();
    }
}

// 召唤伴侣并加入队伍
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

    if (party.getMembers().size() >= 6) {
        cm.sendOk("您的队伍人数已满（最多 6 人），无法再招募新的 Bot 伴侣！");
        cm.dispose();
        return;
    }

    var map = player.getMap();
    var pos = player.getPosition();
    var level = Math.max(10, Math.min(200, player.getLevel()));

    var botId = BotGeneration.createBot(pos, map, baseClass, level, level);
    var botChr = BotHelpers.getCharFromChannelStorage(botId);

    if (botChr != null) {
        Party.joinParty(botChr, party.getId(), false);
        BotRecruitManager.setPendingLeader(botChr.getId(), player.getId());
        BotTypeManager.convertBotType(botChr, BotTypeManager.BotType.FOLLOWER_BOT);

        cm.sendOk("#b#e" + botChr.getName() + "#k#n (等级 " + botChr.getLevel() + ") 已成功召唤并加入您的队伍！\r\n\r\n当前处于#r跟随状态#k。进入战斗地图后，您可以通过本 NPC 发送【战斗指令】让其就地打怪！");
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
    var members = party.getMembers();
    for (var i = 0; i < members.size(); i++) {
        var pc = members.get(i);
        if (pc != null) {
            var p = pc.getPlayer();
            if (p != null && BotHelpers.isBot(p)) {
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
        cm.sendOk("队伍中未找到处于跟随状态的 Bot 伴侣。");
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

    var count = 0;
    var members = party.getMembers();
    for (var i = 0; i < members.size(); i++) {
        var pc = members.get(i);
        if (pc != null) {
            var p = pc.getPlayer();
            if (p != null && BotHelpers.isBot(p)) {
                BotRecruitManager.setPendingLeader(p.getId(), player.getId());
                BotTypeManager.convertBotType(p, BotTypeManager.BotType.FOLLOWER_BOT);
                count++;
            }
        }
    }

    if (count > 0) {
        cm.sendOk("已成功指挥队伍内 #b#e" + count + "#k#n 位 Bot 伴侣停止打怪并切换为#b跟随模式#k！\r\n它们将寸步不离跟随您跑图与穿过传送门。");
    } else {
        cm.sendOk("队伍中未找到处于战斗状态的 Bot 伴侣。");
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
    var members = party.getMembers();
    for (var i = 0; i < members.size(); i++) {
        var pc = members.get(i);
        if (pc != null) {
            var p = pc.getPlayer();
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

// 召唤野外自主打怪 Bot
function handleSpawnWildBot(player, baseClass) {
    var map = player.getMap();
    if (map == null || MapMobIndex.level(map.getId()) < 0) {
        cm.sendOk("当前地图没有怪物，无法召唤野外打怪 Bot！请在狩猎地图使用此功能。");
        cm.dispose();
        return;
    }

    var pos = player.getPosition();
    var level = Math.max(10, Math.min(200, player.getLevel()));

    var botId = BotGeneration.createBot(pos, map, baseClass, level, level);
    var botChr = BotHelpers.getCharFromChannelStorage(botId);

    if (botChr != null) {
        BotRecruitManager.markStationHere(botChr.getId());
        BotTypeManager.convertBotType(botChr, BotTypeManager.BotType.TRAINING_BOT);
        cm.sendOk("野外打怪 Bot #b#e" + botChr.getName() + "#k#n 已在当前地图生成并开始打怪！");
    } else {
        cm.sendOk("野外打怪 Bot 生成成功（角色 ID：" + botId + "）。");
    }
    cm.dispose();
}

// 清理本地图所有虚拟 Bot
function handleClearMapBots(player) {
    var map = player.getMap();
    if (map == null) {
        cm.dispose();
        return;
    }

    var botList = [];
    var chrs = map.getCharacters();
    for (var i = 0; i < chrs.size(); i++) {
        var c = chrs.get(i);
        if (BotHelpers.isBot(c)) {
            botList.push(c);
        }
    }

    for (var j = 0; j < botList.length; j++) {
        BotGeneration.removeBotFromServer(botList[j]);
    }

    cm.sendOk("已清理当前地图上的 #r" + botList.length + "#k 位虚拟 Bot。");
    cm.dispose();
}

// 玩法与帮助说明
function handleShowHelp() {
    var text = "\t\t\t#e#b★ Bot 虚拟伴侣交互指南 ★#k#n\r\n\r\n";
    text += "#e1. 组队与邀请：#n\r\n";
    text += "   - 在野外或城镇右键点击任意 Bot，选择【组队邀请】，Bot 将 100% 同意入队。\r\n";
    text += "   - 也可在本 NPC 直接选择【招募伴侣】，一键将 Bot 召唤并拉进队伍。\r\n\r\n";
    text += "#e2. 打怪与跟随：#n\r\n";
    text += "   - 队伍内的 Bot 默认处于【跟随模式】，紧跟队长跑图穿门。\r\n";
    text += "   - 队长可以在本地图打开本 NPC，点击【战斗指令】，一键命令全队 Bot 展开攻击！\r\n";
    text += "   - 同样可在普通白字聊天输入 Bot 名字，在弹出的互动气泡中选择【Train here with me!】或【Follow me!】进行单体指挥。\r\n\r\n";
    text += "#e3. 经验共享：#n\r\n";
    text += "   - 组队打怪期间，Bot 击杀怪物的经验值会自动与队伍成员共享！\r\n";

    cm.sendOk(text);
    cm.dispose();
}
