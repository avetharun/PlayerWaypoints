package dev.feintha.playerwaypoints;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static dev.feintha.playerwaypoints.SVPlayerWaypoints.*;

/// @author Lillium (Feintha)
/// @version 0.0.1
///
/// Creates and manages waypoints for the server.
///
/// Waypoints are per-world, and are handled automatically.
///
/// <br>
/// Waypoints are also compatible with Polymer's {@link ElementHolder}
///
@SuppressWarnings("UnusedReturnValue")
public class PlayerWaypointManager {


    public static @Nullable WaypointElement getWaypoint(World world, Identifier waypoint_id) {
        return GlobalWaypoints.get(world).stream().filter(waypointElement -> waypointElement.getWaypointId() == waypoint_id).findFirst().orElse(null);
    }

    /// Get all waypoints in a given world, for a given player, and do something with them. Defaults to an empty set
    public static void consumePlayerWaypoints(ServerPlayerEntity player, ServerWorld world, Consumer<Set<WaypointElement>> consumer) {
        if (ensureHolder(world) == null) {
            consumer.accept(Set.of());
            return;
        }
        var m = PlayerWaypoints.getOrDefault(player.networkHandler, new HashMap<>());
        var s = m.getOrDefault(world, new Pair<>(ensureHolder(world), new HashSet<>()));
        consumer.accept(s.getRight());
        m.put(world, s);
        PlayerWaypoints.put(player.networkHandler, m);
    }
    /// Get all waypoints in a given world, and do something with them. Defaults to an empty set
    public static void consumeGlobalWaypoints(ServerWorld world, Consumer<Set<WaypointElement>> consumer) {
        var s = GlobalWaypoints.getOrDefault(world, new HashSet<>());
        consumer.accept(s);
        GlobalWaypoints.put(world, s);
    }
    /// Adds a waypoint to all players in a world.
    public static WaypointElement addGlobalWaypoint(ServerWorld world, Vec3d pos, Identifier id) {
        var e = new WaypointElement(world, id);
        e.setPosition(pos);
        return addGlobalWaypoint(world, e);
    }
    /// Adds a waypoint to all players in a world
    public static WaypointElement addGlobalWaypoint(ServerWorld world, WaypointElement waypointElement) {
        consumeGlobalWaypoints(world, s -> {
            s.add(waypointElement);
            waypointElement.setHolder(ensureHolder(world));
            if (ensureHolder(world) != null) {
                Objects.requireNonNull(ensureHolder(world)).addElement(waypointElement);
                return;
            }
            WaypointEvents.ADDED_GLOBAL.invoker().onAdded(waypointElement, world);
        });

        return waypointElement;
    }
    public static WaypointElement addPlayerWaypoint(ServerPlayerEntity player, ServerWorld world, WaypointElement waypointElement) {
        consumePlayerWaypoints(player, world, s -> {
            s.add(waypointElement);
            if (ensureHolder(world) != null) {
                Objects.requireNonNull(ensureHolder(world)).addElement(waypointElement);
                return;
            }
            WaypointEvents.ADDED_PLAYER.invoker().onAdded(waypointElement, player, world);
        });
        return waypointElement;
    }
    public static WaypointElement addPlayerWaypoint(ServerPlayerEntity player, ServerWorld world, Vec3d waypointPos, Identifier id){
        var e = new WaypointElement(world, id);
        e.setPosition(waypointPos);
        return addPlayerWaypoint(player, world, e);
    }
    /// Removes a waypoint for a player
    public static void removePlayerWaypoint(ServerPlayerEntity player, ServerWorld world, WaypointElement element) {
        consumePlayerWaypoints(player, world, s -> {
            element.stopWatching(player, player.networkHandler::sendPacket);
            if (element.observers.isEmpty()) {
                s.remove(element);
                if (ensureHolder(world) != null) {
                    Objects.requireNonNull(ensureHolder(world)).removeElement(element);
                    return;
                }
            }
        });
    }
    /// Removes a global waypoint
    public static void removeGlobalWaypoint(ServerWorld world, WaypointElement element) {
        consumeGlobalWaypoints(world, s -> {
            element.observers.forEach(player -> element.stopWatching(player, player.networkHandler::sendPacket));
            s.remove(element);
            if (ensureHolder(world) != null) {
                Objects.requireNonNull(ensureHolder(world)).removeElement(element);
                return;
            }
        });
    }
}
