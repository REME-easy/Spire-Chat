package SpireChat;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import com.evacipated.cardcrawl.modthespire.lib.SpireEnum;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;

import chronoMods.network.NetworkHelper;
import chronoMods.network.NetworkHelper.dataType;
import chronoMods.network.RemotePlayer;

public class Patches {
    public static class DataEnums {
        @SpireEnum
        public static NetworkHelper.dataType SendMessage;
    }

    @SpirePatch(clz = NetworkHelper.class, method = "sendData")
    public static class SendDataPatch {
        public static SpireReturn<Void> Prefix(dataType type) {
            if (type == DataEnums.SendMessage) {
                try {
                    String msg = ModCore.chatScreen.TypingMsg;
                    ByteBuffer data = ByteBuffer.allocateDirect(4 + (msg.getBytes()).length);
                    ((Buffer) data).position(4);
                    data.put(msg.getBytes());
                    ((Buffer) data).rewind();
                    data.putInt(0, type.ordinal());
                    Hpr.info(data.toString());
                    // for (dataType i : dataType.values()) {
                    // Hpr.info(String.valueOf(i));
                    // }
                    // SteamUser steamUser = (SteamUser)
                    // ReflectionHacks.getPrivate(CardCrawlGame.publisherIntegration,
                    // com.megacrit.cardcrawl.integrations.steam.SteamIntegration.class,
                    // "steamUser");
                    // NetworkHelper.parseData(data, new SteamPlayer(steamUser.getSteamID()));
                    if (NetworkHelper.service() != null) {
                        NetworkHelper.service().sendPacket(data);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = NetworkHelper.class, method = "parseData")
    public static class ReceiveDataPatch {
        @SpireInsertPatch(rloc = 99999, localvars = { "type" })
        public static void Insert(ByteBuffer data, RemotePlayer playerInfo, dataType type) {
            if (type == DataEnums.SendMessage) {
                try {
                    byte[] msgBytes = new byte[data.remaining()];
                    data.get(msgBytes);
                    String msg = new String(msgBytes);
                    ModCore.chatScreen.addMsg(playerInfo.userName, msg, playerInfo.colour);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
