package mewsicbridge.client;

import java.util.Map;
import java.util.Random;
import mewsicbridge.MewsicBridgeMod;
import mewsicbridge.MusicState;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.sdl.SDLMouse;

public class MewsicHudOverlay {
    private static final int MIN_WINDOW_WIDTH = 150;
    private static final int MAX_WINDOW_WIDTH = 200;
    private static final int WINDOW_HEIGHT = 72;
    private static final int COLOR_TITLE = -1;
    private static final int COLOR_ARTIST = -5592406;
    private static final int COLOR_PAUSED = -7829368;
    private static final String NOTE_PLAYING = "\u266a ";
    private static final String NOTE_PAUSED = "\u23f8 ";

    private static final Item[] DISCS = new Item[]{
        Items.MUSIC_DISC_13, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_BLOCKS, Items.MUSIC_DISC_CHIRP,
        Items.MUSIC_DISC_FAR, Items.MUSIC_DISC_MALL, Items.MUSIC_DISC_MELLOHI, Items.MUSIC_DISC_STAL,
        Items.MUSIC_DISC_STRAD, Items.MUSIC_DISC_WARD, Items.MUSIC_DISC_11, Items.MUSIC_DISC_WAIT,
        Items.MUSIC_DISC_OTHERSIDE, Items.MUSIC_DISC_5, Items.MUSIC_DISC_PIGSTEP, Items.MUSIC_DISC_RELIC,
        Items.MUSIC_DISC_CREATOR, Items.MUSIC_DISC_CREATOR_MUSIC_BOX, Items.MUSIC_DISC_PRECIPICE,
        Items.MUSIC_DISC_BOUNCE, Items.MUSIC_DISC_LAVA_CHICKEN, Items.MUSIC_DISC_TEARS
    };

    private static final Random RANDOM = new Random();
    private static String lastTrackKey = "";
    private static Item currentDisc = Items.MUSIC_DISC_CAT;
    private static Item cachedDiscItem = null;
    private static ItemStack cachedDiscStack = null;

    // Metadata & text measurement caching to avoid per-frame allocations
    private static int lastMetadataVersion = -1;
    private static Font lastFont = null;
    private static boolean lastPlaying = false;
    private static String cachedTitleText = "";
    private static int cachedTitleWidth = 0;
    private static String cachedArtistText = "";
    private static int cachedArtistWidth = 0;
    private static String cachedContextText = "";
    private static int cachedContextWidth = 0;
    private static int cachedTextWidth = 0;

    // Buttons
    private static Button prevBtn;
    private static Button playPauseBtn;
    private static Button nextBtn;

    // Layout caching
    private static int lastLayoutLeft = -1;
    private static int lastLayoutTop = -1;
    private static int lastLayoutWidth = -1;

    // Window dragging & position state
    private static boolean hasCustomPosition = false;
    private static int customX = 0;
    private static int customY = 0;
    private static boolean dragging = false;
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;

    private static int windowLeft = 0;
    private static int windowTop = 0;
    private static int windowRight = 0;
    private static int windowBottom = 0;

    private static void updateDiscIfNeeded(String title, String artist) {
        String key = title + " - " + artist;
        if (!key.equals(lastTrackKey)) {
            lastTrackKey = key;
            currentDisc = DISCS[RANDOM.nextInt(DISCS.length)];
        }
    }

    private static ItemStack getDiscStack(Item disc) {
        if (cachedDiscStack == null || cachedDiscItem != disc) {
            cachedDiscItem = disc;
            cachedDiscStack = new ItemStack(disc);
        }
        return cachedDiscStack;
    }

    private static boolean isLeftMouseButtonDown() {
        try {
            return (SDLMouse.nSDL_GetMouseState(0L, 0L) & SDLMouse.SDL_BUTTON_LMASK) != 0;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isSupportedScreen(Screen screen) {
        return screen instanceof PauseScreen || screen instanceof TitleScreen;
    }

    public static void register() {
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (isSupportedScreen(screen)) {
                // Initialize playback buttons once for this screen session
                prevBtn = Button.builder(Component.literal("\u23ee"), button -> {
                    if (MewsicBridgeMod.wsServer != null) {
                        MewsicBridgeMod.wsServer.broadcastAction("playPrev");
                    }
                }).bounds(0, 0, 10, 20).build();

                playPauseBtn = Button.builder(Component.literal("\u23ef"), button -> {
                    if (MewsicBridgeMod.wsServer != null) {
                        MewsicBridgeMod.wsServer.broadcastAction("togglePlay");
                    }
                }).bounds(0, 0, 10, 20).build();

                nextBtn = Button.builder(Component.literal("\u23ed"), button -> {
                    if (MewsicBridgeMod.wsServer != null) {
                        MewsicBridgeMod.wsServer.broadcastAction("playNext");
                    }
                }).bounds(0, 0, 10, 20).build();

                // Invalidate button layout cache so buttons reposition to current screen scale
                lastLayoutLeft = -1;
                lastLayoutTop = -1;
                lastLayoutWidth = -1;

                ScreenEvents.afterExtract(screen).register((screenInstance, drawContext, mouseX, mouseY, tickDelta) -> {
                    MewsicHudOverlay.render(drawContext, mouseX, mouseY, tickDelta);
                });

                ScreenMouseEvents.beforeMouseClick(screen).register((screenInstance, event) -> {
                    int btn = event.button();
                    // In SDL3 left click is button 1; handle button 0 for GLFW fallback
                    if (btn == 1 || btn == 0) {
                        double mouseX = event.x();
                        double mouseY = event.y();
                        MusicState state = MusicState.getInstance();
                        boolean hasTrack = !state.getTitle().isEmpty() || !state.getArtist().isEmpty();
                        if (!hasTrack) {
                            return;
                        }

                        // Check button clicks first
                        if (prevBtn != null && prevBtn.isMouseOver(mouseX, mouseY)) {
                            prevBtn.playDownSound(client.getSoundManager());
                            if (MewsicBridgeMod.wsServer != null) {
                                MewsicBridgeMod.wsServer.broadcastAction("playPrev");
                            }
                            return;
                        }
                        if (playPauseBtn != null && playPauseBtn.isMouseOver(mouseX, mouseY)) {
                            playPauseBtn.playDownSound(client.getSoundManager());
                            if (MewsicBridgeMod.wsServer != null) {
                                MewsicBridgeMod.wsServer.broadcastAction("togglePlay");
                            }
                            return;
                        }
                        if (nextBtn != null && nextBtn.isMouseOver(mouseX, mouseY)) {
                            nextBtn.playDownSound(client.getSoundManager());
                            if (MewsicBridgeMod.wsServer != null) {
                                MewsicBridgeMod.wsServer.broadcastAction("playNext");
                            }
                            return;
                        }

                        // Check panel dragging
                        if (mouseX >= windowLeft && mouseX <= windowRight && mouseY >= windowTop && mouseY <= windowBottom) {
                            dragging = true;
                            if (!hasCustomPosition) {
                                hasCustomPosition = true;
                                customX = windowLeft;
                                customY = windowTop;
                            }
                            dragOffsetX = (int)mouseX - customX;
                            dragOffsetY = (int)mouseY - customY;
                        }
                    }
                });

                ScreenMouseEvents.beforeMouseRelease(screen).register((screenInstance, event) -> {
                    int btn = event.button();
                    if (btn == 1 || btn == 0) {
                        dragging = false;
                    }
                });

                ScreenEvents.remove(screen).register(screenInstance -> {
                    dragging = false;
                });
            }
        });
    }

    private static void renderMarqueeText(GuiGraphicsExtractor context, Font tr, String text, int textWidth,
                                          int clipLeft, int clipRight, int y, int color) {
        int clipWidth = clipRight - clipLeft;
        if (textWidth <= clipWidth) {
            context.text(tr, text, clipLeft, y, color, true);
            return;
        }

        int overflow = textWidth - clipWidth;
        double speed = 25.0; // 25 pixels per second for readable pacing
        double scrollTime = (double)overflow / speed;
        double pauseTime = 1.5; // 1.5s pause at start and end
        double resetPause = 0.8; // 0.8s pause before loop restart
        double totalCycle = pauseTime + scrollTime + pauseTime + resetPause;

        double timeSeconds = (double)(System.currentTimeMillis() % 86400000L) / 1000.0;
        double cycleTime = timeSeconds % totalCycle;

        int offset;
        if (cycleTime < pauseTime) {
            offset = 0;
        } else if (cycleTime < pauseTime + scrollTime) {
            double progress = (cycleTime - pauseTime) / scrollTime;
            offset = (int)Math.round(progress * overflow);
        } else {
            offset = overflow;
        }

        context.enableScissor(clipLeft, y - 1, clipRight, y + 10);
        context.text(tr, text, clipLeft - offset, y, color, true);
        context.disableScissor();
    }

    private static void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }

        MusicState state = MusicState.getInstance();
        String title = state.getTitle();
        String artist = state.getArtist();
        boolean hasTrack = !title.isEmpty() || !artist.isEmpty();

        if (!hasTrack) {
            return;
        }

        updateDiscIfNeeded(title, artist);
        Font tr = client.font;
        int screenWidth = context.guiWidth();
        int screenHeight = context.guiHeight();

        // Check if cached text measurements need refreshing
        int metaVer = state.getMetadataVersion();
        boolean isPlaying = state.isPlaying();
        if (metaVer != lastMetadataVersion || tr != lastFont || isPlaying != lastPlaying) {
            lastMetadataVersion = metaVer;
            lastFont = tr;
            lastPlaying = isPlaying;

            String prefix = isPlaying ? NOTE_PLAYING : NOTE_PAUSED;
            cachedTitleText = prefix + title;
            cachedTitleWidth = tr.width(cachedTitleText);

            cachedArtistText = artist;
            cachedArtistWidth = tr.width(cachedArtistText);

            String playlistId = state.getCurrentPlaylist();
            if (playlistId != null && !playlistId.equalsIgnoreCase("library") && !playlistId.isEmpty()) {
                String resolvedName = playlistId;
                for (Map.Entry<String, String> entry : state.getPlaylists().entrySet()) {
                    if (entry.getValue().equals(playlistId)) {
                        resolvedName = entry.getKey();
                        break;
                    }
                }
                resolvedName = resolvedName.replace("_", " ");
                cachedContextText = "Playlist : " + resolvedName;
            } else {
                cachedContextText = "From Library";
            }
            cachedContextWidth = tr.width(cachedContextText);

            cachedTextWidth = Math.max(Math.max(cachedTitleWidth, cachedArtistWidth), cachedContextWidth);
        }

        int titleColor = isPlaying ? COLOR_TITLE : COLOR_PAUSED;

        // Native mouse polling occurs ONLY when dragging is active, eliminating per-frame JNI calls otherwise
        if (dragging) {
            if (!isLeftMouseButtonDown()) {
                dragging = false;
            } else {
                customX = mouseX - dragOffsetX;
                customY = mouseY - dragOffsetY;
            }
        }

        // Apply width limit and clamp within screen dimensions
        int maxAllowedWidth = Math.min(MAX_WINDOW_WIDTH, screenWidth - 12);
        int windowWidth = Math.min(maxAllowedWidth, Math.max(MIN_WINDOW_WIDTH, 28 + cachedTextWidth + 6));
        int windowHeight = WINDOW_HEIGHT;

        if (hasCustomPosition) {
            if (customX < 0) {
                customX = 0;
            }
            if (customY < 0) {
                customY = 0;
            }
            if (customX + windowWidth > screenWidth) {
                customX = screenWidth - windowWidth;
            }
            if (customY + windowHeight > screenHeight) {
                customY = screenHeight - windowHeight;
            }
            windowLeft = customX;
            windowTop = customY;
        } else {
            windowRight = screenWidth - 6;
            windowLeft = windowRight - windowWidth;
            windowTop = screenHeight - windowHeight - 6;
        }
        windowRight = windowLeft + windowWidth;
        windowBottom = windowTop + windowHeight;

        // Draw overlay panel background using GuiGraphicsExtractor
        context.fill(windowLeft, windowTop, windowRight, windowBottom, -12303292);
        context.fill(windowLeft + 1, windowTop + 1, windowRight - 1, windowBottom - 1, -535818224);

        // Draw disc item (reusing cached ItemStack instance to avoid per-frame allocations)
        context.item(getDiscStack(currentDisc), windowLeft + 6, windowTop + 8);

        // Render text lines with marquee scrolling if they exceed available width
        int clipLeft = windowLeft + 28;
        int clipRight = windowRight - 6;

        renderMarqueeText(context, tr, cachedTitleText, cachedTitleWidth, clipLeft, clipRight, windowTop + 6, titleColor);
        if (!cachedArtistText.isEmpty()) {
            renderMarqueeText(context, tr, cachedArtistText, cachedArtistWidth, clipLeft, clipRight, windowTop + 17, COLOR_ARTIST);
        }
        renderMarqueeText(context, tr, cachedContextText, cachedContextWidth, clipLeft, clipRight, windowTop + 28, COLOR_ARTIST);

        // Progress bar
        float progress = state.getDuration() > 0 ? state.getCurrentTime() / (float)state.getDuration() : 0.0f;
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        int barWidth = windowWidth - 12;
        int barY = windowTop + 40;
        context.fill(windowLeft + 6, barY, windowLeft + 6 + barWidth, barY + 2, -11184811);
        if (progress > 0.0f) {
            int filledWidth = (int)((float)barWidth * progress);
            context.fill(windowLeft + 6, barY, windowLeft + 6 + filledWidth, barY + 2, -1);
        }

        // Layout playback buttons only when window dimensions or position change
        int availableWidth = windowWidth - 12;
        int spacing = 2;
        int buttonAreaWidth = availableWidth - spacing * 2;
        int playPauseWidth = buttonAreaWidth * 40 / 100;
        int prevNextWidth = (buttonAreaWidth - playPauseWidth) / 2;
        playPauseWidth = buttonAreaWidth - prevNextWidth * 2;
        int buttonsY = windowTop + 46;
        int buttonsStartX = windowLeft + 6;

        if (windowLeft != lastLayoutLeft || windowTop != lastLayoutTop || windowWidth != lastLayoutWidth) {
            lastLayoutLeft = windowLeft;
            lastLayoutTop = windowTop;
            lastLayoutWidth = windowWidth;

            if (prevBtn != null) {
                prevBtn.setWidth(prevNextWidth);
                prevBtn.setX(buttonsStartX);
                prevBtn.setY(buttonsY);
            }
            if (playPauseBtn != null) {
                playPauseBtn.setWidth(playPauseWidth);
                playPauseBtn.setX(buttonsStartX + prevNextWidth + spacing);
                playPauseBtn.setY(buttonsY);
            }
            if (nextBtn != null) {
                nextBtn.setWidth(prevNextWidth);
                nextBtn.setX(buttonsStartX + prevNextWidth + playPauseWidth + spacing * 2);
                nextBtn.setY(buttonsY);
            }
        }

        // Render playback buttons exactly once on top of HUD panel
        if (prevBtn != null) {
            prevBtn.extractRenderState(context, mouseX, mouseY, tickDelta);
        }
        if (playPauseBtn != null) {
            playPauseBtn.extractRenderState(context, mouseX, mouseY, tickDelta);
        }
        if (nextBtn != null) {
            nextBtn.extractRenderState(context, mouseX, mouseY, tickDelta);
        }
    }
}
