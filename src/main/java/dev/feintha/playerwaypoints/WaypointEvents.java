package dev.feintha.playerwaypoints;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.concurrent.atomic.AtomicBoolean;

public class WaypointEvents {
    public interface Added {
        void onAdded(WaypointElement waypoint, ServerWorld world);
        static void invoke(WaypointElement waypointElement, ServerWorld world) {
            ADDED.invoker().onAdded(waypointElement, world);
        }
    }
    public static final Event<Added> ADDED = EventFactory.createArrayBacked(Added.class, i -> ((waypoint, world) -> {
        for (var listener : i) {
            listener.onAdded(waypoint, world);
        }
    }));
}
