package dev.feintha.playerwaypoints;

import dev.feintha.playerwaypoints.commands.AbstractCommandRegistrar;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.ManualAttachment;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.waypoint.WaypointStyle;
import net.minecraft.world.waypoint.WaypointStyles;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



/// All classes in this are internal, don't use unless you know what you're doing (i don't)
public class SVPlayerWaypoints implements ModInitializer {
    public static HashMap<World, Set<WaypointElement>> GlobalWaypoints = new HashMap<>();
    public static HashMap<ServerPlayNetworkHandler, HashMap<World, Pair<ElementHolder, Set<WaypointElement>>>> PlayerWaypoints = new HashMap<>();

    public static final GameRules.Key<GameRules.BooleanRule> SEND_LOCATOR_BAR_UPDATES_FROM_PLAYERS_GAMERULE =
            GameRuleRegistry.register("emitLocatorBarLocationAtPlayer", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(false));
    public static ElementHolder _sEH = new ElementHolder();
    public static HashMap<World, ManualAttachment> _waMap = new HashMap<>();
    public static Vec3d ORIGIN_GETTER() {return Vec3d.ZERO;}
    public static Set<ManualAttachment> attachmentsToTick = new HashSet<>();
    public static @Nullable ElementHolder getHolderForWorld(ServerWorld world) {
        return ensureHolder(world);
    }
    public static class Config {
        boolean addNorthWaypoint = false;
        public boolean addNorthWaypointEnabled() {
            return addNorthWaypoint;
        }
    }
    public static ElementHolder ensureHolder(ServerWorld world) {
        var h = new ElementHolder();
        ManualAttachment a = null;
        if (!_waMap.containsKey(world)) {
            a = new ManualAttachment(h, world, SVPlayerWaypoints::ORIGIN_GETTER);
            _waMap.put(world, a);
            attachmentsToTick.add(a);
        } else {
            a = _waMap.get(world);
            h = a.holder();
            if (a.holder() == null) {
                h = new ElementHolder();
                a = new ManualAttachment(h, world, SVPlayerWaypoints::ORIGIN_GETTER);
                _waMap.put(world, a);
                attachmentsToTick.add(a);
            }
        }
        return h;
    }
    public static Config CONFIG = new Config();
    @Override
    public void onInitialize() {
        try {
            AbstractCommandRegistrar.getImplsForPackage("dev.feintha");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        WaypointEvents.ADDED_GLOBAL.register((waypoint, world) -> world.getPlayers().forEach(playerEntity ->
                waypoint.startWatching(playerEntity, playerEntity.networkHandler::sendPacket))
        );
        WaypointEvents.ADDED_PLAYER.register((waypoint, playerEntity, world) ->
                waypoint.startWatching(playerEntity, playerEntity.networkHandler::sendPacket)
        );

        // TODO: ensure this is removed once the wwp command is finished!
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) CommandRegistrationCallback.EVENT.register(AbstractCommandRegistrar::registerAll);
        ServerWorldEvents.LOAD.register((minecraftServer, serverWorld) -> {
            if (CONFIG.addNorthWaypointEnabled()) {
                var e = PlayerWaypointManager.addGlobalWaypoint(serverWorld, new WaypointElement(serverWorld, Identifier.of("north")) {
                    @Override
                    public float getAzimuth(ServerPlayerEntity player) {
                        return 180 * MathHelper.RADIANS_PER_DEGREE;
                    }
                });
                e.setWaypointColor(0xff0000);
                e.setWaypointStyle(WaypointStyles.of("north"));
            }
        });
        ServerWorldEvents.UNLOAD.register((minecraftServer, serverWorld) -> {
        });
        ServerPlayerEvents.JOIN.register(serverPlayerEntity -> {
            _sEH.startWatching(serverPlayerEntity.networkHandler);
            for (VirtualElement element : _sEH.getElements()) {
                element.startWatching(serverPlayerEntity, serverPlayerEntity.networkHandler::sendPacket);
            }
            PlayerWaypointManager.consumeGlobalWaypoints(serverPlayerEntity.getWorld(), waypointElements -> waypointElements.forEach(waypointElement -> waypointElement.startWatching(serverPlayerEntity, serverPlayerEntity.networkHandler::sendPacket)));
            PlayerWaypointManager.consumePlayerWaypoints(serverPlayerEntity, serverPlayerEntity.getWorld(), waypointElements -> waypointElements.forEach(waypointElement -> waypointElement.startWatching(serverPlayerEntity, serverPlayerEntity.networkHandler::sendPacket)));
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((serverPlayerEntity, old_world, new_world) -> {
            // remove old waypoints
            PlayerWaypointManager.consumeGlobalWaypoints(old_world, waypointElements -> waypointElements.forEach(waypointElement -> waypointElement.stopWatching(serverPlayerEntity, serverPlayerEntity.networkHandler::sendPacket)));
            PlayerWaypointManager.consumePlayerWaypoints(serverPlayerEntity, old_world, waypointElements -> waypointElements.forEach(waypointElement -> waypointElement.stopWatching(serverPlayerEntity, serverPlayerEntity.networkHandler::sendPacket)));
            // add new waypoints
            PlayerWaypointManager.consumeGlobalWaypoints(new_world, waypointElements -> waypointElements.forEach(waypointElement -> waypointElement.startWatching(serverPlayerEntity, serverPlayerEntity.networkHandler::sendPacket)));
            PlayerWaypointManager.consumePlayerWaypoints(serverPlayerEntity, old_world, waypointElements -> waypointElements.forEach(waypointElement -> waypointElement.stopWatching(serverPlayerEntity, serverPlayerEntity.networkHandler::sendPacket)));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((serverPlayNetworkHandler, minecraftServer) -> {
//            _sEH.stopWatching(serverPlayNetworkHandler);
            PlayerWaypoints.remove(serverPlayNetworkHandler);
            var wm = _waMap.get(serverPlayNetworkHandler.getPlayer().getWorld());
            if (wm != null) {
//                wm.startWatching(serverPlayNetworkHandler);
            }
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {

            for (ServerWorld serverWorld : server.getWorlds()) {
                var a = new ManualAttachment(new ElementHolder(), serverWorld, SVPlayerWaypoints::ORIGIN_GETTER);

                _waMap.put(serverWorld, a);
                attachmentsToTick.add(a);
            }
        });
        ServerTickEvents.START_SERVER_TICK.register(minecraftServer -> {
            _sEH.tick();
            _waMap.values().forEach(HolderAttachment::tick);
            attachmentsToTick.forEach(HolderAttachment::tick);
        });
    }
}
