package mewsicbridge.client;

import java.util.Map;
import java.util.Random;
import mewsicbridge.MewsicBridgeMod;
import mewsicbridge.MusicState;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MewsicHudOverlay {
    private static final int PADDING_RIGHT = 6;
    private static final int PADDING_BOTTOM = 6;
    private static final int LINE_GAP = 2;
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

    private static Button prevBtn;
    private static Button playPauseBtn;
    private static Button nextBtn;

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

    public static void register() {
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof PauseScreen) {
                ScreenEvents.afterExtract(screen).register((screenInstance, drawContext, mouseX, mouseY, tickDelta) -> {
                    MewsicHudOverlay.render(drawContext, mouseX, mouseY, tickDelta);
                });

                ScreenMouseEvents.beforeMouseClick(screen).register((screenInstance, event) -> {
                    if (event.button() == 0) {
                        double mouseX = event.x();
                        double mouseY = event.y();
                        MusicState state = MusicState.getInstance();
                        boolean hasTrack = !state.getTitle().isEmpty() || !state.getArtist().isEmpty();
                        if (hasTrack && mouseX >= windowLeft && mouseX <= windowRight && mouseY >= windowTop && mouseY <= windowBottom) {
                            boolean overButton = (prevBtn != null && prevBtn.isMouseOver(mouseX, mouseY))
                                              || (playPauseBtn != null && playPauseBtn.isMouseOver(mouseX, mouseY))
                                              || (nextBtn != null && nextBtn.isMouseOver(mouseX, mouseY));
                            if (!overButton) {
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
                    }
                });

                ScreenMouseEvents.beforeMouseRelease(screen).register((screenInstance, event) -> {
                    if (event.button() == 0) {
                        dragging = false;
                    }
                });

                ScreenMouseEvents.beforeMouseDrag(screen).register((screenInstance, event, dragX, dragY) -> {
                    if (dragging) {
                        customX = (int)event.x() - dragOffsetX;
                        customY = (int)event.y() - dragOffsetY;
                    }
                });

                ScreenEvents.remove(screen).register(screenInstance -> {
                    dragging = false;
                });
            }
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof PauseScreen) {
                MusicState state = MusicState.getInstance();
                String title = state.getTitle();
                String artist = state.getArtist();
                boolean hasTrack = !title.isEmpty() || !artist.isEmpty();

                Font tr = client.font;
                String prefix = state.isPlaying() ? NOTE_PLAYING : NOTE_PAUSED;
                String titleText = prefix + title;
                int titleWidth = tr.width(titleText);
                int artistWidth = tr.width(artist);
                int textWidth = Math.max(titleWidth, artistWidth);
                int windowWidth = Math.max(28 + textWidth + 6, 96);
                int right = scaledWidth - 6;
                int left = right - windowWidth;
                int windowHeight = 60;
                int top = scaledHeight - windowHeight - 6;
                int buttonsY = top + 34;
                int buttonsStartX = left + (windowWidth - 84) / 2;

                prevBtn = Button.builder(Component.literal("\u23ee"), button -> {
                    if (MewsicBridgeMod.wsServer != null) {
                        MewsicBridgeMod.wsServer.broadcastAction("playPrev");
                    }
                    button.setFocused(false);
                }).bounds(buttonsStartX, buttonsY, 24, 20).build();
                prevBtn.visible = hasTrack;

                playPauseBtn = Button.builder(Component.literal("\u23ef"), button -> {
                    if (MewsicBridgeMod.wsServer != null) {
                        MewsicBridgeMod.wsServer.broadcastAction("togglePlay");
                    }
                    button.setFocused(false);
                }).bounds(buttonsStartX + 26, buttonsY, 32, 20).build();
                playPauseBtn.visible = hasTrack;

                nextBtn = Button.builder(Component.literal("\u23ed"), button -> {
                    if (MewsicBridgeMod.wsServer != null) {
                        MewsicBridgeMod.wsServer.broadcastAction("playNext");
                    }
                    button.setFocused(false);
                }).bounds(buttonsStartX + 60, buttonsY, 24, 20).build();
                nextBtn.visible = hasTrack;

                Screens.getWidgets(screen).add(prevBtn);
                Screens.getWidgets(screen).add(playPauseBtn);
                Screens.getWidgets(screen).add(nextBtn);
            }
        });
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

        if (prevBtn != null) {
            prevBtn.visible = hasTrack;
        }
        if (playPauseBtn != null) {
            playPauseBtn.visible = hasTrack;
        }
        if (nextBtn != null) {
            nextBtn.visible = hasTrack;
        }

        if (!hasTrack) {
            return;
        }

        updateDiscIfNeeded(title, artist);
        Font tr = client.font;
        int screenWidth = context.guiWidth();
        int screenHeight = context.guiHeight();

        String prefix = state.isPlaying() ? NOTE_PLAYING : NOTE_PAUSED;
        String titleText = prefix + title;
        int titleColor = state.isPlaying() ? COLOR_TITLE : COLOR_PAUSED;

        String playlistId = state.getCurrentPlaylist();
        String contextStr = "From Library";
        if (playlistId != null && !playlistId.equalsIgnoreCase("library") && !playlistId.isEmpty()) {
            String resolvedName = playlistId;
            for (Map.Entry<String, String> entry : state.getPlaylists().entrySet()) {
                if (!entry.getValue().equals(playlistId)) continue;
                resolvedName = entry.getKey();
                break;
            }
            resolvedName = resolvedName.replace("_", " ");
            contextStr = "Playlist : " + resolvedName;
        }

        int titleWidth = tr.width(titleText);
        int artistWidth = tr.width(artist);
        int contextWidth = tr.width(contextStr);
        int textWidth = Math.max(Math.max(titleWidth, artistWidth), contextWidth);

        if (dragging) {
            customX = mouseX - dragOffsetX;
            customY = mouseY - dragOffsetY;
        }

        int windowWidth = Math.max(28 + textWidth + 6, 96);
        int windowHeight = 72;

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
        context.item(new ItemStack(currentDisc), windowLeft + 6, windowTop + 8);
        context.text(tr, titleText, windowLeft + 28, windowTop + 6, titleColor, true);
        if (!artist.isEmpty()) {
            context.text(tr, artist, windowLeft + 28, windowTop + 17, COLOR_ARTIST, true);
        }
        context.text(tr, contextStr, windowLeft + 28, windowTop + 28, COLOR_ARTIST, true);

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

        // Layout playback buttons
        int availableWidth = windowWidth - 12;
        int spacing = 2;
        int buttonAreaWidth = availableWidth - spacing * 2;
        int playPauseWidth = buttonAreaWidth * 40 / 100;
        int prevNextWidth = (buttonAreaWidth - playPauseWidth) / 2;
        playPauseWidth = buttonAreaWidth - prevNextWidth * 2;
        int buttonsY = windowTop + 46;
        int buttonsStartX = windowLeft + 6;

        if (prevBtn != null) {
            prevBtn.setWidth(prevNextWidth);
            prevBtn.setX(buttonsStartX);
            prevBtn.setY(buttonsY);
            if (prevBtn.visible) {
                prevBtn.extractRenderState(context, mouseX, mouseY, tickDelta);
            }
        }
        if (playPauseBtn != null) {
            playPauseBtn.setWidth(playPauseWidth);
            playPauseBtn.setX(buttonsStartX + prevNextWidth + spacing);
            playPauseBtn.setY(buttonsY);
            if (playPauseBtn.visible) {
                playPauseBtn.extractRenderState(context, mouseX, mouseY, tickDelta);
            }
        }
        if (nextBtn != null) {
            nextBtn.setWidth(prevNextWidth);
            nextBtn.setX(buttonsStartX + prevNextWidth + playPauseWidth + spacing * 2);
            nextBtn.setY(buttonsY);
            if (nextBtn.visible) {
                nextBtn.extractRenderState(context, mouseX, mouseY, tickDelta);
            }
        }
    }
}
