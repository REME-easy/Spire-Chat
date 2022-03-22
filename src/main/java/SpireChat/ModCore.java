package SpireChat;

import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.UIStrings;

import basemod.BaseMod;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.PostInitializeSubscriber;

@SpireInitializer
public class ModCore implements EditStringsSubscriber, PostInitializeSubscriber {
    public static ChatScreen chatScreen;

    public ModCore() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new ModCore();
    }

    public void receiveEditStrings() {
        String lang = "eng";
        if (Settings.language == Settings.GameLanguage.ZHS || Settings.language == Settings.GameLanguage.ZHT) {
            lang = "zhs";
        }
        BaseMod.loadCustomStringsFile(UIStrings.class, "SpireChatResources/SpireChatUI_" + lang +
                ".json");
    }

    @Override
    public void receivePostInitialize() {
        chatScreen = new ChatScreen();
    }
}
