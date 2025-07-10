package dev.feintha.playerwaypoints;

import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.elements.*;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
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

import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;


public class WaypointElement extends AbstractElement implements StringIdentifiable {
    UUID uuid = UUID.randomUUID();
    Identifier identifier;
    Set<ServerPlayerEntity> observers = new HashSet<>();
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
        this.hologramDataList = new ArrayList<>(hologram.stream().map(HologramData.MarkerHologramData::new).toList());
        updateHologram();
    }
    public void setHologram(HologramData<?, ?>... hologramData) {
        this.hologramDataList = new ArrayList<>(List.of(hologramData));
        updateHologram();
    }
    public void updateHologram() {
        var removedElements = new ArrayList<>(hologram_titles_last);
        removedElements.removeAll(hologramDataList);
        for (HologramData<?, ?> removedElement : removedElements) {
            observers.forEach(player -> removedElement.element.stopWatching(player, player.networkHandler::sendPacket));
        }
        for (HologramData<?, ?> hologramElement : hologramDataList) {
            getHolder().addElement(hologramElement.element);
            observers.forEach(player -> hologramElement.element.startWatching(player, player.networkHandler::sendPacket));
        }
        for (HologramData<?, ?> hologramData : hologramDataList) {
            hologramData.updateElement();
        }
        this.hologram_titles_last = this.hologramDataList;
    }

    public void removeTitle() {
        hologramDataList = new ArrayList<>();
        updateHologram();
    }
    Function<Integer, Vec3d> hologramPositionGetter;
    public Vec3d getHologramPosition(int line) {
        return getCurrentPos().offset(Direction.UP, (-line * 0.25f)).offset(Direction.EAST, Math.sin(Instant.now().toEpochMilli() * 0.001));
    }
    @Override
    public Vec3d getCurrentPos() {
        return waypointPos;
    }
    void sendUpdate() {
        assert getHolder() != null;
        observers.forEach(spe -> {

            Vec3d vec3d = spe.getPos().subtract(getPosition()).rotateYClockwise();
            float f = (float) MathHelper.atan2(vec3d.getZ(), vec3d.getX());
            spe.networkHandler.sendPacket(WaypointS2CPacket.updateAzimuth(uuid, getConfig(spe), f));
            for (int i = 0; i < hologramDataList.size(); i++) {
                var hologram = hologramDataList.get(i);
                hologram.element.setOffset(hologram.getPosition(getCurrentPos(), i));
            }
        });
    }
    public void setPosition(Vec3d waypointPos) {
        this.waypointPos = waypointPos;
        sendUpdate();
    }

    @Override
    public IntList getEntityIds() {
        return IntList.of();
    }

    @Override
    public void setOffset(Vec3d offset) {
        setPosition(offset);
        sendUpdate();
    }
    public Vec3d getPosition() {
        return waypointPos;
    }

    public RegistryKey<WaypointStyle> getWaypointStyle() { return waypointStyle; }
    public void setWaypointStyle(RegistryKey<WaypointStyle> waypointStyle) { this.waypointStyle = waypointStyle; sendUpdate();}

    public void setWaypointColor(int color) { this.color = color; sendUpdate();}
    public int getWaypointColor() { return color; }

    public Waypoint.Config getConfig(ServerPlayerEntity player) {
        var c = new Waypoint.Config();
        c.color = Optional.of(this.color);
        c.style = waypointStyle == null ? WaypointStyles.BOWTIE : waypointStyle;
        return c;
    }
    EntityAttributeInstance RANGE_ATTR;
    public WaypointElement(@NotNull ServerWorld world, Identifier id) {
        super();
        this.serverWorld = world;
        lastSyncedPos = Vec3d.ZERO;
    }
    public void setWaypointTransmitRange(double range) {
        this.maxDistance = range;
    }
    public boolean isPlayerInRange(ServerPlayerEntity player) {
        return player.squaredDistanceTo(getCurrentPos()) <= maxDistance * maxDistance;
    }
    @Override
    public void startWatching(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {
        observers.add(player);
        if (isPlayerInRange(player)) {
            Vec3d vec3d = player.getPos().subtract(getPosition()).rotateYClockwise();
            float f = (float) MathHelper.atan2(vec3d.getZ(), vec3d.getX());
            packetConsumer.accept(WaypointS2CPacket.trackAzimuth(this.uuid, new Waypoint.Config(), f));
        }
    }

    @Override
    public void stopWatching(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {
        packetConsumer.accept(WaypointS2CPacket.untrack(this.getUuid()));
        observers.remove(player);
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public void notifyMove(Vec3d oldPos, Vec3d currentPos, Vec3d delta) {

    }

    boolean isFirstTick = true;
    @Override
    public void tick() {
        isFirstTick = false;
        assert lastSyncedPos != null;
        assert getHolder() != null;
        updateLastSyncedPos();
        sendUpdate();
    }
    public BlockPos getBlockPos() {
        return new BlockPos((int) waypointPos.x, (int) waypointPos.y, (int) waypointPos.z);
    }

    @Override
    public String asString() {
        return this.getUuid().toString();
    }
}
