package SpireChat;

import java.util.ArrayList;
import java.util.Scanner;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

import SpireChat.TextEffects.NullEffect;
import SpireChat.TextEffects.TextEffect;
import basemod.BaseMod;
import basemod.interfaces.PostRenderSubscriber;
import basemod.interfaces.PostUpdateSubscriber;
import chronoMods.network.NetworkHelper;

public class ChatScreen implements PostUpdateSubscriber, PostRenderSubscriber {

    public boolean isOpen = false;

    public ArrayList<String> Messages;
    public ArrayList<ArrayList<ChatText>> Texts;
    public int ShowIndex = 0;
    public String TypingMsg = "";
    public int TypingCursor = 0;
    public float showTimer = 0.0F;
    public Color CloseColor = new Color(0.0F, 0.0F, 0.0F, 0.0F);

    public GlyphLayout GL;

    public static int MAX_MSG_CAP = 200;

    public static final int OpenKey = Keys.F2;
    public static final int SendKey = Keys.ENTER;
    public static final int RemoveKey = Keys.BACKSPACE;

    public static final Color BG_OPEN_COLOR = new Color(0.0F, 0.0F, 0.0F, 0.75F);

    public static final BitmapFont FONT = FontHelper.turnNumFont;

    public static final int MAX_MSG_SIZE = 8;

    public static final float LINE_HEIGHT = 30.0F * Settings.scale;
    public static final float CHAT_X = 200.0F * Settings.scale;
    public static final float CHAT_Y = 200.0F * Settings.scale;
    public static final float CHAT_W = 500.0F * Settings.scale;
    public static final float CHAT_H = MAX_MSG_SIZE * LINE_HEIGHT + 10.0F * Settings.scale;

    public ChatScreen() {
        InputMultiplexer iMultiplexer = new InputMultiplexer();
        iMultiplexer.addProcessor(Gdx.input.getInputProcessor());
        iMultiplexer.addProcessor(new ChatTextProcessor(this));
        Gdx.input.setInputProcessor(iMultiplexer);

        BaseMod.subscribe(this);
        this.Messages = new ArrayList<>();
        this.Texts = new ArrayList<>();
        GL = new GlyphLayout();
    }

    public void addMsg(String msg, Color color) {
        String str = String.format("[#%s]%s[]", color.toString(), msg);
        Messages.add(0, str);
        makeText(str);
        if (Messages.size() > MAX_MSG_CAP) {
            Messages.remove(Messages.size() - 1);
            ArrayList<ChatText> text = Texts.remove(Texts.size() - 1);
            for (ChatText t : text) {
                t.dispose();
            }
        }
        this.showTimer = 3.0F;
    }

    public void addMsg(String user, String msg, Color color) {
        String str = String.format("[#%s]%s[]: %s", color.toString(), user, msg);
        Messages.add(0, str);
        makeText(str);
        if (Messages.size() > MAX_MSG_CAP) {
            Messages.remove(Messages.size() - 1);
            ArrayList<ChatText> text = Texts.remove(Texts.size() - 1);
            for (ChatText t : text) {
                t.dispose();
            }
        }
        this.showTimer = 3.0F;
    }

    public void makeText(String msg) {
        try {
            ArrayList<ChatText> texts = new ArrayList<>();
            Scanner s = new Scanner(msg);
            float x = 0.0F;
            while (s.hasNext()) {
                ArrayList<TextEffect> es = new ArrayList<>();
                String w = s.next();
                Color c = ChatText.IdentifyWordColor(w);
                if (c != null) {
                    w = w.substring(2);
                }
                TextEffect e = ChatText.IdentifyWordEffect(w);
                if (!(e instanceof NullEffect)) {
                    w = w.substring(1, w.length() - 1);
                    es.add(e);
                }
                GL.setText(FONT, w);

                texts.add(new ChatText(FONT, w, es, c, x));
                x += GL.width;
                if (!Settings.lineBreakViaCharacter) {
                    x += 8.0F * Settings.scale;
                }
            }
            s.close();
            Texts.add(0, texts);
        } catch (Exception ex) {
            ex.printStackTrace();
            ArrayList<ChatText> texts = new ArrayList<>();
            texts.add(new ChatText(FONT, msg, new ArrayList<>(), Color.WHITE.cpy(), 0.0F));
            Texts.add(0, texts);
        }

    }

    public void clear() {
        this.Messages.clear();
        this.Texts.clear();
        this.ShowIndex = 0;
    }

    public boolean isMouseInScreen() {
        float x = InputHelper.mX;
        float y = InputHelper.mY;

        return x > CHAT_X && x < CHAT_X + CHAT_W && y > CHAT_Y && y < CHAT_Y + CHAT_H;
    }

    @Override
    public void receivePostRender(SpriteBatch sb) {
        int size;
        if (Messages.size() < MAX_MSG_SIZE) {
            size = Messages.size() + 1;
        } else {
            size = MAX_MSG_SIZE;
        }

        sb.setColor(isOpen ? BG_OPEN_COLOR : CloseColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, CHAT_X, CHAT_Y, CHAT_W, CHAT_H);

        float x = CHAT_X + 5.0F * Settings.scale;
        float y = CHAT_Y + (float) Math.floor(LINE_HEIGHT);
        if (isOpen) {
            FONT.draw(sb, ">:" + TypingMsg, x, y);
            float w;
            if (TypingCursor > 0) {
                GL.setText(FONT, ">:" + TypingMsg.substring(0, TypingCursor));
            } else {
                GL.setText(FONT, ">:");
            }
            w = GL.width - 10.0F * Settings.scale;
            if (!Settings.lineBreakViaCharacter) {
                w += 2.0F * Settings.scale;
            }
            FONT.draw(sb, "|", x + w, y);
        }

        if (size == 1 || (!isOpen && showTimer <= 0.0F))
            return;
        for (int i = ShowIndex; i < ShowIndex + size - 1; i++) {
            y += (float) Math.floor(LINE_HEIGHT);
            // FONT.draw(sb, Messages.get(i), x, y);
            ArrayList<ChatText> texts = Texts.get(i);
            for (ChatText text : texts) {
                text.render(sb, x, y);
            }
        }
    }

    @Override
    public void receivePostUpdate() {
        if (Gdx.input.isKeyJustPressed(OpenKey)) {
            isOpen = !isOpen;
        }

        if (this.showTimer > 0.0F) {
            this.showTimer -= Gdx.graphics.getDeltaTime();
            if (this.showTimer < 0.0F) {
                this.showTimer = 0.0F;
            }
            if (this.showTimer > 0.1) {
                CloseColor.a = 0.45F;
            } else {
                CloseColor.a = this.showTimer * 4.5F;
            }
        }

        if (isOpen) {
            if (TypingMsg != "") {
                if (Gdx.input.isKeyJustPressed(SendKey)) {
                    NetworkHelper.sendData(Patches.DataEnums.SendMessage);
                    TypingMsg = "";
                    TypingCursor = 0;
                }
                if (Gdx.input.isKeyJustPressed(RemoveKey)) {
                    if (TypingCursor > 0) {
                        if (TypingCursor == 1) {
                            TypingMsg = TypingMsg.substring(1, TypingMsg.length());
                        } else if (TypingCursor == TypingMsg.length()) {
                            TypingMsg = TypingMsg.substring(0, TypingMsg.length() - 1);
                        } else {
                            String pre = TypingMsg.substring(0, TypingCursor - 1);
                            String post = TypingMsg.substring(TypingCursor, TypingMsg.length());
                            TypingMsg = pre + post;
                        }
                        TypingCursor--;
                    }
                }
                if (Gdx.input.isKeyJustPressed(Keys.LEFT)) {
                    TypingCursor -= 1;
                    if (TypingCursor < 0)
                        TypingCursor = 0;
                }
                if (Gdx.input.isKeyJustPressed(Keys.RIGHT)) {
                    TypingCursor += 1;
                    if (TypingCursor > TypingMsg.length())
                        TypingCursor = TypingMsg.length();
                }
            }

            int size;
            if (Messages.size() < MAX_MSG_SIZE) {
                size = Messages.size() + 1;
            } else {
                size = MAX_MSG_SIZE;
            }
            for (int i = ShowIndex; i < ShowIndex + size - 1; i++) {
                // FONT.draw(sb, Messages.get(i), x, y);
                ArrayList<ChatText> texts = Texts.get(i);
                for (ChatText text : texts) {
                    text.update();
                }
            }
        }
    }

    public class ChatTextProcessor implements InputProcessor {
        public ChatScreen parent;

        public ChatTextProcessor(ChatScreen parent) {
            this.parent = parent;
        }

        public boolean keyDown(int keycode) {
            return false;
        }

        public boolean keyUp(int keycode) {
            return false;
        }

        public boolean keyTyped(char character) {
            if (!parent.isOpen)
                return false;
            String charStr = String.valueOf(character);
            if (Character.isLetterOrDigit(character) || ChatText.WHITE_LIST.contains(charStr)) {
                if (parent.TypingMsg == "") {
                    parent.TypingMsg = charStr;
                } else {
                    StringBuilder str = new StringBuilder(parent.TypingMsg);
                    str.insert(parent.TypingCursor, charStr);
                    parent.TypingMsg = str.toString();
                }

                // parent.TypingMsg += charStr;
                parent.TypingCursor += charStr.length();
                return false;
            }
            return false;
        }

        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            return false;
        }

        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            return false;
        }

        public boolean touchDragged(int screenX, int screenY, int pointer) {
            return false;
        }

        public boolean mouseMoved(int screenX, int screenY) {
            return false;
        }

        public boolean scrolled(int amount) {
            if (!parent.isOpen || !isMouseInScreen())
                return false;
            if (amount < 0) {
                ShowIndex++;
            } else if (amount > 0) {
                ShowIndex--;
            }
            if (Messages.size() <= MAX_MSG_SIZE) {
                ShowIndex = 0;
            } else {
                ShowIndex = MathUtils.clamp(ShowIndex, 0, Messages.size() - MAX_MSG_SIZE);
            }
            return false;
        }
    }
}
