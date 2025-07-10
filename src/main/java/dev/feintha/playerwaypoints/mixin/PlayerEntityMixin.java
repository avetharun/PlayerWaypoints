package dev.feintha.playerwaypoints.mixin;

import com.mojang.authlib.GameProfile;
import dev.feintha.playerwaypoints.SVPlayerWaypoints;
import dev.feintha.playerwaypoints.WaypointElement;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.ManualAttachment;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ServerPlayerEntity.class)
public abstract class PlayerEntityMixin extends PlayerEntity {
    @Shadow public abstract ServerWorld getWorld();

    @Unique private ManualAttachment attachment;
    public PlayerEntityMixin(World world, GameProfile profile) {
        super(world, profile);
    }

    @Override
    public double getAttributeBaseValue(RegistryEntry<EntityAttribute> attribute) {
        if (attribute == EntityAttributes.WAYPOINT_TRANSMIT_RANGE) {
            if (!getWorld().getGameRules().getBoolean(SVPlayerWaypoints.SEND_LOCATOR_BAR_UPDATES_FROM_PLAYERS_GAMERULE)) {
                return -1;
            }
        }
        return super.getAttributeBaseValue(attribute);
    }
}
