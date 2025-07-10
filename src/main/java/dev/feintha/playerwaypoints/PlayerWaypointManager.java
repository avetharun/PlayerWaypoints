package dev.feintha.playerwaypoints;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
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
    public static void consumePlayerWaypoints(ServerPlayerEntity player, World world, Consumer<Set<WaypointElement>> consumer) {
        if (getHolderForWorld(world) == null) {
            consumer.accept(Set.of());
            return;
        }
        var m = PlayerWaypoints.getOrDefault(player.networkHandler, new HashMap<>());
        var s = m.getOrDefault(world, new Pair<>(getHolderForWorld(world), new HashSet<>()));
        consumer.accept(s.getRight());
        m.put(world, s);
        PlayerWaypoints.put(player.networkHandler, m);
    }
    /// Get all waypoints in a given world, and do something with them. Defaults to an empty set
    public static void consumeGlobalWaypoints(World world, Consumer<Set<WaypointElement>> consumer) {
        if (getHolderForWorld(world) == null) {
            consumer.accept(Set.of());
            return;
        }
        var s = GlobalWaypoints.getOrDefault(world, new HashSet<>());
        consumer.accept(s);
        GlobalWaypoints.put(world, s);
    }
    /// Removes a global waypoint
    public static void removeWaypoint(ServerWorld world, WaypointElement element) {
        consumeGlobalWaypoints(world, s -> {
            s.remove(element);
            if (getHolderForWorld(world) != null) {
                Objects.requireNonNull(getHolderForWorld(world)).removeElement(element);
                return;
            }
        });
    }
    /// Adds a waypoint to all players in a world.
    public static WaypointElement addGlobalWaypoint(ServerWorld world, BlockPos pos, Identifier id) {
        var e = new WaypointElement(world, id);
        e.setPosition(pos.toBottomCenterPos());
        return addGlobalWaypoint(world, e);
    }
    /// Adds a waypoint to all players in a world
    public static WaypointElement addGlobalWaypoint(ServerWorld world, WaypointElement waypointElement) {
        consumeGlobalWaypoints(world, s -> {
            s.add(waypointElement);
            if (getHolderForWorld(world) != null) {
                Objects.requireNonNull(getHolderForWorld(world)).addElement(waypointElement);
                return;
            }
            WaypointEvents.Added.invoke(waypointElement, world);
        });

        return waypointElement;
    }
    public static void addPlayerWaypoint(ServerPlayerEntity player, ServerWorld world, Vec3d waypointPos){

    }
}
