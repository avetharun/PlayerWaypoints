package dev.feintha.playerwaypoints;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.concurrent.atomic.AtomicBoolean;

public class WaypointEvents {
    public interface AddedGlobal {
        void onAdded(WaypointElement waypoint, ServerWorld world);
    }
    public interface AddedPlayer {
        void onAdded(WaypointElement waypoint, ServerPlayerEntity player, ServerWorld world);
    }
    public static final Event<AddedGlobal> ADDED_GLOBAL = EventFactory.createArrayBacked(AddedGlobal.class, i -> ((waypoint, world) -> {
        for (var listener : i) {
            listener.onAdded(waypoint, world);
        }
    }));
    public static final Event<AddedPlayer> ADDED_PLAYER = EventFactory.createArrayBacked(AddedPlayer.class, i -> ((waypoint, player, world) -> {
        for (var listener : i) {
            listener.onAdded(waypoint, player, world);
        }
    }));
}
