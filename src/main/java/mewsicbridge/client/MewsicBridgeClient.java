package mewsicbridge.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import mewsicbridge.MewsicBridgeMod;
import mewsicbridge.MusicState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public class MewsicBridgeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MewsicHudOverlay.register();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("mewsic")
                .then(ClientCommands.literal("play").executes(context -> {
                    if (MewsicBridgeMod.wsServer != null) {
                        MewsicBridgeMod.wsServer.broadcastAction("togglePlay");
                    }
                    context.getSource().sendFeedback(Component.literal("Toggled Play/Pause!"));
                    return 1;
                }))
                .then(ClientCommands.literal("pause").executes(context -> {
                    if (MewsicBridgeMod.wsServer != null) {
                        MewsicBridgeMod.wsServer.broadcastAction("togglePlay");
                    }
                    context.getSource().sendFeedback(Component.literal("Toggled Play/Pause!"));
                    return 1;
                }))
                .then(ClientCommands.literal("next").executes(context -> {
                    if (MewsicBridgeMod.wsServer != null) {
                        MewsicBridgeMod.wsServer.broadcastAction("playNext");
                    }
                    context.getSource().sendFeedback(Component.literal("Playing next track..."));
                    return 1;
                }))
                .then(ClientCommands.literal("previous").executes(context -> {
                    if (MewsicBridgeMod.wsServer != null) {
                        MewsicBridgeMod.wsServer.broadcastAction("playPrev");
                    }
                    context.getSource().sendFeedback(Component.literal("Playing previous track..."));
                    return 1;
                }))
                .then(ClientCommands.literal("shuffle").executes(context -> {
                    if (MewsicBridgeMod.wsServer != null) {
                        MewsicBridgeMod.wsServer.broadcastAction("toggleShuffle");
                    }
                    context.getSource().sendFeedback(Component.literal("Toggled Shuffle!"));
                    return 1;
                }))
                .then(ClientCommands.literal("repeat").then(ClientCommands.argument("mode", StringArgumentType.word()).executes(context -> {
                    String mode = StringArgumentType.getString(context, "mode");
                    if (MewsicBridgeMod.wsServer != null) {
                        MewsicBridgeMod.wsServer.broadcastActionWithPayload("setRepeatMode", "repeatMode", mode);
                    }
                    context.getSource().sendFeedback(Component.literal("Set repeat mode to " + mode));
                    return 1;
                })))
                .then(ClientCommands.literal("track").then(ClientCommands.argument("title", StringArgumentType.greedyString())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(MusicState.getInstance().getLibraryTracks().keySet(), builder))
                    .executes(context -> {
                        String title = StringArgumentType.getString(context, "title");
                        String trackId = MusicState.getInstance().getLibraryTracks().get(title);
                        if (trackId != null) {
                            if (MewsicBridgeMod.wsServer != null) {
                                MewsicBridgeMod.wsServer.broadcastActionWithPayload("playTrack", "trackId", trackId);
                            }
                            context.getSource().sendFeedback(Component.literal("Playing track: " + title.replace("_", " ")));
                        } else {
                            context.getSource().sendError(Component.literal("Track not found: " + title));
                        }
                        return 1;
                    })))
                .then(ClientCommands.literal("playlist").then(ClientCommands.argument("name", StringArgumentType.greedyString())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(MusicState.getInstance().getPlaylists().keySet(), builder))
                    .executes(context -> {
                        String name = StringArgumentType.getString(context, "name");
                        String playlistId = MusicState.getInstance().getPlaylists().get(name);
                        if (playlistId != null) {
                            if (MewsicBridgeMod.wsServer != null) {
                                MewsicBridgeMod.wsServer.broadcastActionWithPayload("playPlaylist", "playlistId", playlistId);
                            }
                            context.getSource().sendFeedback(Component.literal("Playing playlist: " + name.replace("_", " ")));
                        } else {
                            context.getSource().sendError(Component.literal("Playlist not found: " + name));
                        }
                        return 1;
                    })))
                .then(ClientCommands.literal("reconnect").executes(context -> {
                    context.getSource().sendFeedback(Component.literal("Rebooting Mewsic Bridge on port " + mewsicbridge.config.ModConfig.get().port + "..."));
                    MewsicBridgeMod.restartServer();
                    return 1;
                }))
                .then(ClientCommands.literal("port")
                    .executes(context -> {
                        context.getSource().sendFeedback(Component.literal("Current Mewsic port: " + mewsicbridge.config.ModConfig.get().port));
                        return 1;
                    })
                    .then(ClientCommands.argument("number", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1024, 65535)).executes(context -> {
                        int newPort = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "number");
                        mewsicbridge.config.ModConfig.get().port = newPort;
                        mewsicbridge.config.ModConfig.save();
                        context.getSource().sendFeedback(Component.literal("Mewsic port set to " + newPort + ". Reconnecting..."));
                        MewsicBridgeMod.restartServer();
                        return 1;
                    })))
            );
        });

        MewsicBridgeMod.LOGGER.info("[MewsicBridge] Client initialised \u2014 HUD overlay and commands registered.");
    }
}
