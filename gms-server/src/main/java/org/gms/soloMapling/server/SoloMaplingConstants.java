package org.gms.soloMapling.server;

import org.gms.net.server.Server;
import org.gms.net.server.channel.Channel;

public class SoloMaplingConstants {

    public static Channel getMainChannel() {
        return Server.getInstance().getChannel(GameConstants.WORLD_SCANIA, GameConstants.CHANNEL_1);
    }

    public static class GameConstants {
        public static final int WORLD_SCANIA = 0;
        public static final int CHANNEL_1 = 1;
        public static final int BOT_BASE_ID = 20000;
    }

}
