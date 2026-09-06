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
}
