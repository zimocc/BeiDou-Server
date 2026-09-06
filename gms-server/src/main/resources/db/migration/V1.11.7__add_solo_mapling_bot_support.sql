-- ======================================================================
-- SoloMapling-0.3 Bot Framework Database Support
-- ======================================================================

-- 1. Bot account 'fmbot'
INSERT IGNORE INTO accounts (`name`, password, pin, pic, birthday, nxcredit, maplepoint, nxprepaid, characterslots,
                             gender, tos)
VALUES ('fmbot', '$2y$12$xS3xZTX5hSU8v0SvC4h1FewFeK4Lx0q6kXoqv/bFJu6Hr3Wuimr9q', '0000', '000000',
        '2005-05-11', 0, 0, 0, 3, 0, 1);

-- 2. Base Bot Template Character (CID 2)
INSERT IGNORE INTO characters (id, accountid, world, `name`, level, exp,
                              str, dex, luk, `int`, hp, mp, maxhp, maxmp, meso, job, skincolor, gender,
                              hair, face, ap, map, spawnpoint, gm, equipslots, useslots,
                              setupslots, etcslots)
VALUES (2, (SELECT id FROM accounts WHERE `name` = 'fmbot'), 0, 'fmbot', 1, 0,
        12, 5, 4, 4, 50, 5, 50, 5, 0, 0, 0, 0,
        30030, 20000, 0, 10000, 0, 0, 96, 96,
        96, 96);

-- 3. Casino Stamp Exchange Shop (Shop 9999001, NPC 9000055)
INSERT INTO shops (shopid, npcid)
VALUES (9999001, 9000055)
ON DUPLICATE KEY UPDATE npcid = 9000055;

DELETE FROM shopitems WHERE shopid = 9999001;

INSERT INTO shopitems (shopid, itemid, price, pitch, position)
VALUES (9999001, 4002000, 10000, 0, 4),
       (9999001, 4002001, 50000, 0, 3),
       (9999001, 4002002, 250000, 0, 2),
       (9999001, 4002003, 1000000, 0, 1);

-- 4. Bot GM4 Commands in command_info
INSERT INTO `command_info` (`syntax`, `level`, `enabled`, `clazz`, `default_level`)
SELECT 'bot', 4, 1, 'ArtificialPlayerCommand', 4
WHERE NOT EXISTS (SELECT 1 FROM `command_info` WHERE `syntax` = 'bot');

INSERT INTO `command_info` (`syntax`, `level`, `enabled`, `clazz`, `default_level`)
SELECT 'move', 4, 1, 'BotMoveCommand', 4
WHERE NOT EXISTS (SELECT 1 FROM `command_info` WHERE `syntax` = 'move');

INSERT INTO `command_info` (`syntax`, `level`, `enabled`, `clazz`, `default_level`)
SELECT 'env', 4, 1, 'EnvironmentCommand', 4
WHERE NOT EXISTS (SELECT 1 FROM `command_info` WHERE `syntax` = 'env');

INSERT INTO `command_info` (`syntax`, `level`, `enabled`, `clazz`, `default_level`)
SELECT 'betafmshop', 4, 1, 'ArtificialFreeMarketCommand', 4
WHERE NOT EXISTS (SELECT 1 FROM `command_info` WHERE `syntax` = 'betafmshop');

INSERT INTO `command_info` (`syntax`, `level`, `enabled`, `clazz`, `default_level`)
SELECT 'fmbot', 4, 1, 'FMBotCommand', 4
WHERE NOT EXISTS (SELECT 1 FROM `command_info` WHERE `syntax` = 'fmbot');

INSERT INTO `command_info` (`syntax`, `level`, `enabled`, `clazz`, `default_level`)
SELECT 'tradebot', 4, 1, 'TradeBotTestCommand', 4
WHERE NOT EXISTS (SELECT 1 FROM `command_info` WHERE `syntax` = 'tradebot');

INSERT INTO `command_info` (`syntax`, `level`, `enabled`, `clazz`, `default_level`)
SELECT 'test', 4, 1, 'TestDevCommand', 4
WHERE NOT EXISTS (SELECT 1 FROM `command_info` WHERE `syntax` = 'test');

INSERT INTO `command_info` (`syntax`, `level`, `enabled`, `clazz`, `default_level`)
SELECT 'opq', 4, 1, 'OPQCommands', 4
WHERE NOT EXISTS (SELECT 1 FROM `command_info` WHERE `syntax` = 'opq');

INSERT INTO `command_info` (`syntax`, `level`, `enabled`, `clazz`, `default_level`)
SELECT 'reactor', 4, 1, 'ReactorCommands', 4
WHERE NOT EXISTS (SELECT 1 FROM `command_info` WHERE `syntax` = 'reactor');

INSERT INTO `command_info` (`syntax`, `level`, `enabled`, `clazz`, `default_level`)
SELECT 'gcmove', 4, 1, 'GCMoveCommand', 4
WHERE NOT EXISTS (SELECT 1 FROM `command_info` WHERE `syntax` = 'gcmove');

-- 5. Game Config: spawn_bots_on_startup
INSERT INTO `game_config`(`config_type`, `config_sub_type`, `config_clazz`, `config_code`, `config_value`, `config_desc`, `update_time`)
SELECT 'server', 'Game Mechanics', 'java.lang.Boolean', 'spawn_bots_on_startup', 'true', 'spawn_bots_on_startup', '2026-09-06 00:00:00'
WHERE NOT EXISTS (
    SELECT 1 FROM `game_config` WHERE `config_code` = 'spawn_bots_on_startup'
);

INSERT INTO `lang_resources`(`lang_type`, `lang_base`, `lang_code`, `lang_value`, `lang_extend`)
SELECT 'zh-CN', 'game_config', 'spawn_bots_on_startup', '是否开机自动加载假人机器人', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM `lang_resources` WHERE `lang_type` = 'zh-CN' AND `lang_code` = 'spawn_bots_on_startup'
);

INSERT INTO `lang_resources`(`lang_type`, `lang_base`, `lang_code`, `lang_value`, `lang_extend`)
SELECT 'en-US', 'game_config', 'spawn_bots_on_startup', 'Whether to spawn bots on startup', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM `lang_resources` WHERE `lang_type` = 'en-US' AND `lang_code` = 'spawn_bots_on_startup'
);
