package org.gms.soloMapling.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SoloMapling 虚拟角色与生态系统的国际化 (i18n) 管理中心。
 * 默认启用中文 (zh-CN) 版本，并提供本地化资源路径解析与语言判定。
 */
public final class SoloMaplingI18n {

    private static final Logger log = LoggerFactory.getLogger(SoloMaplingI18n.class);

    public static final String LANG_ZH_CN = "zh-CN";
    public static final String LANG_EN_US = "en-US";

    // 默认启用中文版本
    private static volatile String currentLanguage = LANG_ZH_CN;

    private SoloMaplingI18n() {
    }

    /**
     * 判断当前是否处于中文环境。
     */
    public static boolean isChinese() {
        return currentLanguage != null && (currentLanguage.equalsIgnoreCase(LANG_ZH_CN)
                || currentLanguage.toLowerCase().startsWith("zh"));
    }

    /**
     * 获取当前语言标识符（如 zh-CN, en-US）。
     */
    public static String getLanguage() {
        return currentLanguage;
    }

    /**
     * 动态设置当前系统语言。
     */
    public static void setLanguage(String lang) {
        if (lang != null && !lang.trim().isEmpty()) {
            currentLanguage = lang.trim();
            log.info("[SoloMaplingI18n] Language switched to: {}", currentLanguage);
        }
    }

    /**
     * 根据语言环境解析本地化资源文件路径。
     * 例如 basePath="BotDialoguePack/", filename="FollowerBotDialogue.yaml"：
     * 若处于中文模式且存在 "BotDialoguePack/zh-CN/FollowerBotDialogue.yaml"，则返回该路径；
     * 否则无缝回退至默认 "BotDialoguePack/FollowerBotDialogue.yaml"。
     */
    public static String resolveLocalizedResource(String basePath, String filename) {
        if (!basePath.endsWith("/")) {
            basePath = basePath + "/";
        }

        if (isChinese()) {
            String localizedCandidate = basePath + "zh-CN/" + filename;
            if (SoloMaplingResourceLoader.hasResource(localizedCandidate)) {
                return localizedCandidate;
            }
        }

        return basePath + filename;
    }

    /**
     * 确保 Bot 角色名符合中文规范（统一“仙”开头）。
     */
    public static String formatBotName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return "仙游侠";
        }
        String name = rawName.trim();
        if (isChinese()) {
            if (!name.startsWith("仙")) {
                name = "仙" + name;
            }
            // 冒险岛 v83 客户端对角色名通常有 12 字节限制（约 6 个汉字）
            if (name.length() > 6) {
                name = name.substring(0, 6);
            }
        }
        return name;
    }
}
