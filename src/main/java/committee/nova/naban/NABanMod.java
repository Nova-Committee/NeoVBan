package committee.nova.naban;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.arguments.EntityArgument;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

@Mod(NABanMod.MOD_ID)
public class NABanMod {

    public static final String MOD_ID = "naban";
    private static NABanMod instance;
    private MinecraftServer server;
    private final Map<UUID, String> playerHardwareMap = new HashMap<>();
    private final Map<String, UUID> hardwarePlayerMap = new HashMap<>();

    public NABanMod() {
        instance = this;
        NeoForge.EVENT_BUS.register(this);
    }

    public static NABanMod getInstance() {
        return instance;
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        String hardwareId = getHardwareId();
        UUID playerUUID = player.getUUID();

        if (playerHardwareMap.containsKey(playerUUID)) {
            String storedHardwareId = playerHardwareMap.get(playerUUID);
            if (!storedHardwareId.equals(hardwareId)) {
                player.connection.disconnect(Component.literal("Detected account login from different device, connection refused"));
                return;
            }
        } else {
            if (hardwarePlayerMap.containsKey(hardwareId)) {
                UUID boundUUID = hardwarePlayerMap.get(hardwareId);
                if (!boundUUID.equals(playerUUID)) {
                    player.connection.disconnect(Component.literal("This device is already bound to another account, connection refused"));
                    return;
                }
            }
            playerHardwareMap.put(playerUUID, hardwareId);
            hardwarePlayerMap.put(hardwareId, playerUUID);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.server = event.getServer();
        CommandDispatcher<CommandSourceStack> dispatcher = this.server.getCommands().getDispatcher();
        registerCommands(dispatcher);
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("naban")
                        .requires(source -> source.hasPermission(3))
                        .then(Commands.literal("unbind")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(this::unbindPlayer)))
                        .then(Commands.literal("list")
                                .executes(this::listBindings))
                        .then(Commands.literal("info")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(this::showPlayerInfo)))

                        .then(Commands.literal("ban")
                                .then(Commands.argument("hardwareId",  StringArgumentType.string())
                                        .executes(this::banByHardwareId)))
        );
    }

    private int unbindPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "player");
        UUID playerUUID = player.getUUID();
        String hardwareId = playerHardwareMap.get(playerUUID);

        if (hardwareId != null) {
            playerHardwareMap.remove(playerUUID);
            hardwarePlayerMap.remove(hardwareId);
            context.getSource().sendSuccess(() -> Component.literal("Unbound device binding for player " + player.getName().getString()), true);
        } else {
            context.getSource().sendFailure(Component.literal("Player " + player.getName().getString() + " has no device binding record"));
        }

        return 1;
    }

    private int listBindings(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("Current device binding information:"), false);
        for (Map.Entry<UUID, String> entry : playerHardwareMap.entrySet()) {
            String playerName = getPlayerNameByUUID(entry.getKey());
            context.getSource().sendSuccess(() -> Component.literal(playerName + " -> " + entry.getValue()), false);
        }
        return playerHardwareMap.size();
    }

    private int showPlayerInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = net.minecraft.commands.arguments.EntityArgument.getPlayer(context, "player");
        UUID playerUUID = player.getUUID();
        String hardwareId = playerHardwareMap.get(playerUUID);

        if (hardwareId != null) {
            context.getSource().sendSuccess(() -> Component.literal("Player " + player.getName().getString() + " bound device ID: " + hardwareId), false);
        } else {
            context.getSource().sendFailure(Component.literal("Player " + player.getName().getString() + " has no device binding record"));
        }

        return 1;
    }

    private String getPlayerNameByUUID(UUID uuid) {
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                return player.getName().getString();
            }
        }
        return uuid.toString();
    }

    private String getHardwareId() {
        try {
            StringBuilder sb = new StringBuilder();
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                NetworkInterface network = networks.nextElement();
                byte[] mac = network.getHardwareAddress();
                if (mac != null) {
                    for (byte b : mac) {
                        sb.append(String.format("%02X", b));
                    }
                    break;
                }
            }
            if (sb.length() == 0) {
                sb.append(System.getProperty("os.name"));
                sb.append(System.getProperty("os.arch"));
                sb.append(System.getProperty("user.name"));
            }
            return sb.toString();
        } catch (SocketException e) {
            return System.getProperty("os.name") + System.getProperty("os.arch") + System.getProperty("user.name");
        }
    }
    private int banByHardwareId(CommandContext<CommandSourceStack> context) {
        String hardwareId = StringArgumentType.getString(context, "hardwareId");

        if (hardwarePlayerMap.containsKey(hardwareId)) {
            UUID playerUUID = hardwarePlayerMap.get(hardwareId);
            String playerName = getPlayerNameByUUID(playerUUID);

            playerHardwareMap.remove(playerUUID);
            hardwarePlayerMap.remove(hardwareId);

            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
                if (player != null) {
                    player.connection.disconnect(Component.literal("Banned by hardware ID"));
                }
            }

            context.getSource().sendSuccess(() -> Component.literal("Banned hardware ID: " + hardwareId + " (previously bound to " + playerName + ")"), true);
        } else {
            context.getSource().sendFailure(Component.literal("No player bound to hardware ID: " + hardwareId));
        }

        return 1;
    }
}
