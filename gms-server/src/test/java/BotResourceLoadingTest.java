import org.gms.soloMapling.FreeMarket.FMShopDescGen;
import org.gms.soloMapling.itemPool.DesirableEquipList;
import org.gms.soloMapling.itemPool.EquipOmitList;
import org.gms.soloMapling.itemPool.GachaFillerSystem;
import org.gms.soloMapling.server.SoloMaplingResourceLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class BotResourceLoadingTest {

    @Test
    public void testFMShopDescGenIGN() {
        String ign = FMShopDescGen.getRandomCharacterIGN();
        System.out.println("Loaded IGN: " + ign);
        Assertions.assertNotNull(ign);
        Assertions.assertFalse(ign.isEmpty());
    }

    @Test
    public void testLegacyPathResolution() {
        List<String> lines = SoloMaplingResourceLoader.readAllLines("src/main/resources/soloMapling/FreeMarket/FMNameDesc/randomRealMaplestoryIGNs.txt");
        Assertions.assertFalse(lines.isEmpty());
        System.out.println("Loaded lines count from legacy path: " + lines.size());
    }

    @Test
    public void testRelativePathResolution() {
        List<String> lines = SoloMaplingResourceLoader.readAllLines("FMNameDesc/randomRealMaplestoryIGNs.txt");
        Assertions.assertFalse(lines.isEmpty());
        System.out.println("Loaded lines count from relative path: " + lines.size());
    }

    @Test
    public void testYamlConfigs() {
        DesirableEquipList.load();
        EquipOmitList.load();
        Assertions.assertDoesNotThrow(() -> GachaFillerSystem.createGachaListWithPrize(1002000));
    }

    @Test
    public void testChineseBotNames() throws Exception {
        Assertions.assertTrue(org.gms.soloMapling.server.SoloMaplingI18n.isChinese());
        for (int i = 0; i < 50; i++) {
            String ign = FMShopDescGen.getRandomCharacterIGN();
            Assertions.assertNotNull(ign);
            Assertions.assertTrue(ign.startsWith("仙"), "Bot name must start with '仙': " + ign);
            byte[] bytes = ign.getBytes("GBK");
            Assertions.assertTrue(bytes.length <= 12, "Bot name exceeds 12 bytes in GBK: " + ign + " (" + bytes.length + " bytes)");
        }
    }

    @Test
    public void testChineseDialogueLoading() {
        String resolved = org.gms.soloMapling.server.SoloMaplingI18n.resolveLocalizedResource("BotDialoguePack/", "FollowerBotDialogue.yaml");
        Assertions.assertTrue(resolved.contains("zh-CN"), "Resolved path should contain zh-CN: " + resolved);
        
        java.util.Map<String, Object> node = org.gms.soloMapling.ArtificialPlayer.BotDialogueHandler.readDialogueYaml("FollowerBotDialogue.yaml", "FollowerBot", "FollowStart");
        Assertions.assertNotNull(node, "FollowStart node should not be null");
        Assertions.assertNotNull(node.get("text"), "text property should not be null");
    }
}
