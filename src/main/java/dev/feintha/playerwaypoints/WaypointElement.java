package dev.feintha.playerwaypoints;

import com.google.common.collect.ImmutableSet;
import eu.pb4.polymer.virtualentity.api.elements.*;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.WaypointS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.*;
import net.minecraft.world.waypoint.*;
import org.jetbrains.annotations.NotNull;

import java.sql.Array;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;


public class WaypointElement extends AbstractElement implements StringIdentifiable {

    public WaypointElement(@NotNull ServerWorld world, Identifier id) {
        super();
        this.serverWorld = world;
        this.identifier = id;
        lastSyncedPos = Vec3d.ZERO;
    }
    UUID uuid = UUID.randomUUID();
    Identifier identifier;
    Set<ServerPlayerEntity> observers = new HashSet<>();

    public Set<ServerPlayerEntity> getObservers() {
        return ImmutableSet.copyOf(observers);
    }

    double maxDistance = 6e7;
    public Identifier getWaypointId() {
        return identifier;
    }
    ServerWorld serverWorld;

    public ServerWorld getWorld() {
        return serverWorld;
    }
    RegistryKey<WaypointStyle> waypointStyle=WaypointStyles.BOWTIE;
    int color = 0xffdb7991;
    Vec3d waypointPos = Vec3d.ZERO;

    ArrayList<HologramData<?, ?>> hologramDataList = new ArrayList<>();
    ArrayList<HologramData<?, ?>> hologram_titles_last = new ArrayList<>();
    public void setHologram(List<Text> hologram) {
        stopWatchingHolograms();
        this.hologramDataList = new ArrayList<>(hologram.stream().map(HologramData.MarkerHologramData::new).toList());
        startWatchingHolograms();
    }
    public void setHologram(HologramData<?, ?>... hologramData) {
        stopWatchingHolograms();
        this.hologramDataList = new ArrayList<>(List.of(hologramData));
        startWatchingHolograms();
    }
    public Vec3d getHologramElementPosition(Vec3d origin, int line, HologramData<?,?> data) {
        return data.getPosition(origin, line);
    }
    public void startWatchingHolograms() {
        for (HologramData<?, ?> d : hologramDataList) {
            observers.forEach(player -> {
                d.element.startWatching(player, player.networkHandler::sendPacket);
            });
        }
    }
    public void stopWatchingHolograms() {
        for (HologramData<?, ?> d : hologramDataList) {
            observers.forEach(player -> {
                d.element.stopWatching(player, player.networkHandler::sendPacket);
            });
        }
    }
    public void updateHologram() {
        for (HologramData<?, ?> hologramData : hologramDataList) {
            hologramData.updateElement();
        }
        for (int i = hologramDataList.size() - 1; i >= 0; i--) {
            var hologram = hologramDataList.get(i);
            hologram.element.setOffset(getHologramElementPosition(getCurrentPos(), i, hologram));
        }
    }

    public void removeHologram(HologramData<?, ?> hologramData) {
        stopWatchingHolograms();
        hologramDataList.remove(hologramData);
        startWatchingHolograms();
    }
    public void removeHolograms() {
        stopWatchingHolograms();
        hologramDataList = new ArrayList<>();
    }
    @Override
    public Vec3d getCurrentPos() {
        return waypointPos;
    }
    void sendUpdate() {
        assert getHolder() != null;
        observers.forEach(spe -> {
            spe.networkHandler.sendPacket(WaypointS2CPacket.untrack(uuid));
            if (shouldTransmit(spe)) {
                spe.networkHandler.sendPacket(WaypointS2CPacket.trackAzimuth(this.uuid, getConfig(spe), getAzimuth(spe)));
            }
        });

        updateHologram();
    }
    public void setPosition(Vec3d waypointPos) {
        this.waypointPos = waypointPos;
    }

    @Override
    public IntList getEntityIds() {
        return IntList.of();
    }

    @Override
    public void setOffset(Vec3d offset) {
        setPosition(offset);
    }
    public Vec3d getPosition() {
        return waypointPos;
    }

    public RegistryKey<WaypointStyle> getWaypointStyle() { return waypointStyle; }
    public void setWaypointStyle(RegistryKey<WaypointStyle> waypointStyle) { this.waypointStyle = waypointStyle; }

    public void setWaypointColor(int color) { this.color = color; }
    public int getWaypointColor() { return color; }

    public Waypoint.Config getConfig(ServerPlayerEntity player) {
        var c = new Waypoint.Config();
        c.color = Optional.of(this.color);
        c.style = waypointStyle == null ? WaypointStyles.BOWTIE : waypointStyle;
        return c;
    }
    public void setWaypointTransmitRange(double range) {
        this.maxDistance = range;
    }
    public boolean isPlayerInRange(ServerPlayerEntity player) {
        return player.squaredDistanceTo(getCurrentPos()) <= maxDistance * maxDistance;
    }
    public boolean shouldTransmit(ServerPlayerEntity player) {
        return true;
    }
    @Override
    public void startWatching(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {
        observers.add(player);
        if (shouldTransmit(player)) {
            packetConsumer.accept(WaypointS2CPacket.trackAzimuth(this.uuid, getConfig(player), getAzimuth(player)));
        }

        for (HologramData<?, ?> hologramElement : hologramDataList) {
            hologramElement.element.startWatching(player,packetConsumer);
        }
    }
    public float getAzimuth(ServerPlayerEntity player) {
        Vec3d vec3d = player.getPos().subtract(getPosition()).rotateYClockwise();
        return (float) MathHelper.atan2(vec3d.getZ(), vec3d.getX());
    }
    @Override
    public void stopWatching(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {
        packetConsumer.accept(WaypointS2CPacket.untrack(this.getUuid()));
        observers.remove(player);

        for (HologramData<?, ?> hologramElement : hologramDataList) {
            hologramElement.element.stopWatching(player,packetConsumer);
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public void notifyMove(Vec3d oldPos, Vec3d currentPos, Vec3d delta) {

    }

    @Override
    public void tick() {
        if (getHolder() == null) {
            var h = SVPlayerWaypoints.ensureHolder(getWorld());
            setHolder(h);
            h.addElement(this);
        }
        assert lastSyncedPos != null;
        assert getHolder() != null;
        updateLastSyncedPos();
        sendUpdate();

    }

    @Override
    public String asString() {
        return this.getUuid().toString();
    }
    public double getDistanceTo(ServerPlayerEntity player) {
        return player.getPos().distanceTo(getCurrentPos());
    }
    public double getSquaredDistanceTo(ServerPlayerEntity player) {
        return player.getPos().squaredDistanceTo(getCurrentPos());
    }
}
