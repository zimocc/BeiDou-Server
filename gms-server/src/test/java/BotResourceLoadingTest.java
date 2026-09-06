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
    public void testChinesePriceFormatting() {
        Assertions.assertEquals("39万", org.gms.soloMapling.FreeMarket.FMEconomyManager.formatPriceToShorthand(390_000));
        Assertions.assertEquals("76万", org.gms.soloMapling.FreeMarket.FMEconomyManager.formatPriceToShorthand(760_000));
        Assertions.assertEquals("210万", org.gms.soloMapling.FreeMarket.FMEconomyManager.formatPriceToShorthand(2_100_000));
        Assertions.assertEquals("5000万", org.gms.soloMapling.FreeMarket.FMEconomyManager.formatPriceToShorthand(50_000_000));
        Assertions.assertEquals("1亿", org.gms.soloMapling.FreeMarket.FMEconomyManager.formatPriceToShorthand(100_000_000));
        Assertions.assertEquals("500", org.gms.soloMapling.FreeMarket.FMEconomyManager.formatPriceToShorthand(500));
    }

    @Test
    public void testMerchantBotChineseMessages() {
        for (int i = 0; i < 20; i++) {
            String buyMsg = org.gms.soloMapling.ArtificialPlayer.BotTypes.BuyingMerchantBot.buildBuyingMessage("拳套攻击卷轴", 390_000);
            Assertions.assertTrue(buyMsg.contains("拳套攻击卷轴"), "Must contain item name: " + buyMsg);
            Assertions.assertTrue(buyMsg.contains("39万"), "Must contain Chinese price: " + buyMsg);
            Assertions.assertFalse(buyMsg.contains("Buying"), "Must not contain English 'Buying': " + buyMsg);
            Assertions.assertFalse(buyMsg.contains("hmu"), "Must not contain English 'hmu': " + buyMsg);
            Assertions.assertFalse(buyMsg.contains("scammers"), "Must not contain English 'scammers': " + buyMsg);

            String sellMsg = org.gms.soloMapling.ArtificialPlayer.BotTypes.SellingMerchantBot.buildSellingMessage("齿轮镖");
            Assertions.assertTrue(sellMsg.contains("齿轮镖"), "Must contain item name: " + sellMsg);
            Assertions.assertFalse(sellMsg.contains("Selling"), "Must not contain English 'Selling': " + sellMsg);
            Assertions.assertFalse(sellMsg.contains("Spanish"), "Must not contain English 'Spanish': " + sellMsg);
            Assertions.assertFalse(sellMsg.contains("Offer"), "Must not contain English 'Offer': " + sellMsg);
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

    @Test
    public void testFMShopOfferableDescriptions() {
        for (int i = 0; i < 20; i++) {
            String offerDesc = org.gms.soloMapling.FreeMarket.FMShopDescGen.getOfferableDescription();
            Assertions.assertNotNull(offerDesc);
            Assertions.assertFalse(offerDesc.equalsIgnoreCase("leave offer"), "Must not contain 'leave offer': " + offerDesc);
            Assertions.assertFalse(offerDesc.equalsIgnoreCase("l/o"), "Must not contain 'l/o': " + offerDesc);
            Assertions.assertTrue(offerDesc.matches(".*[\\u4e00-\\u9fa5]+.*"), "Must contain Chinese characters: " + offerDesc);
        }
    }
}
