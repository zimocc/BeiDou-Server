package org.gms.soloMapling.ArtificialPlayer.BotCommandsPack;

import org.gms.client.Character;
import org.gms.client.inventory.Item;
import org.gms.net.server.Server;
import org.gms.server.maps.MapleTVEffect;
import org.gms.util.PacketCreator;

import org.gms.net.server.channel.Channel;
import org.gms.net.server.world.World;

import java.util.LinkedList;
import java.util.List;

import static org.gms.soloMapling.server.SoloMaplingUtilities.getChannel;
import static org.gms.soloMapling.server.SoloMaplingUtilities.getRandomElement;
import static org.gms.soloMapling.server.SoloMaplingUtilities.getWorld;

public class MegaphoneCommands {

    private final static List<Integer> AvatarMegaphoneItems = List.of(5390000, 5390001, 5390002, 5390005, 5390006);
    private final static List<Integer> MapleTVItems = List.of(5075000, 5075001, 5075002);
    private final static List<Integer> MapleTVPartnerItems = List.of(5075001, 5075002);

    public static void BotItemMegaphone(Character fakechar, String message, Item item) {
        String msg = fakechar.getName() + " : " + message; // medal + // optional medal
        boolean whisper = true;
        World w = getWorld();
        Channel ch = getChannel();
        if (w != null && ch != null) {
            Server.getInstance().broadcastMessage(w.getId(), PacketCreator.itemMegaphone(msg, whisper, ch.getId(), item));
        }
    }

    public static void BotGachaponMegaphone(Character fakechar, Item item) {
        String map = fakechar.getMap() != null ? fakechar.getMap().getMapName() : "";
        World w = getWorld();
        if (w != null) {
            Server.getInstance().broadcastMessage(w.getId(), PacketCreator.gachaponMessage(item, map, fakechar));
        }
    }

    private static void BotSuperMegaphone(Character fakechar, String msg, boolean ear) {
        World w = getWorld();
        Channel ch = getChannel();
        if (w != null && ch != null) {
            Server.getInstance().broadcastMessage(w.getId(), PacketCreator.serverNotice(3, ch.getId(), fakechar.getName() + " : " + msg, ear));
        }
    }

    public static void BotSuperMegaphone(Character fakechar, String msg) {
        BotSuperMegaphone(fakechar, msg, true);
    }

    private static void BotAvatarMegaphone(Character fakechar, String message, int itemId) {
        LinkedList list = stringTo4LineLinkedList(message);
        String medal = "";
        boolean ear = true;
        World w = getWorld();
        Channel ch = getChannel();
        if (w != null && ch != null) {
            Server.getInstance().broadcastMessage(w.getId(), PacketCreator.getAvatarMega(fakechar, medal, ch.getId(), itemId, list, ear));
        }
    }


    public static void BotAvatarMegaphone(Character fakechar, String msg) {
        int itemId = getRandomElement(AvatarMegaphoneItems);
        BotAvatarMegaphone(fakechar, msg, itemId);
    }

    private static void BotMapleTVFull(Character fakechar, String msg, int itemId, Character partner) {
        int tvType = itemId % 10;
        if (tvType != 4) {
            partner = partner;
        }
        LinkedList messages = stringTo5LineLinkedList(msg);
        MapleTVEffect.broadcastMapleTVIfNotActive(fakechar, partner, messages, tvType);
    }

    public static void BotMapleTV(Character fakechar, String msg) {
        int itemId = getRandomElement(MapleTVItems);
        BotMapleTVFull(fakechar, msg, itemId, null);
    }

    public static void BotMapleTVPartner(Character fakechar, String msg, Character partner) {
        int itemId = getRandomElement(MapleTVPartnerItems);
        BotMapleTVFull(fakechar, msg, itemId, partner);
    }

    /*
    Utility Code for Megaphone Commands
     */
    
    private static LinkedList<String> stringToLinkedList(String message, int lineCount, int charsPerLine) {
        LinkedList<String> list = new LinkedList<>();

        // Handle null input safely
        if (message == null) {
            message = "";
        }

        // Add each chunk of the string to the list
        for (int i = 0; i < lineCount; i++) {
            int startIndex = i * charsPerLine;

            // If we've reached the end of the string, add empty strings to fill remaining lines
            if (startIndex >= message.length()) {
                list.add("");
            } else {
                int endIndex = Math.min(startIndex + charsPerLine, message.length());
                list.add(message.substring(startIndex, endIndex));
            }
        }

        return list;
    }

    private static LinkedList<String> stringTo4LineLinkedList(String message) {
        return stringToLinkedList(message, 4, 13);
    }

    private static LinkedList<String> stringTo5LineLinkedList(String message) {
        return stringToLinkedList(message, 5, 20);
    }

}
