package dev.feintha.playerwaypoints.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.feintha.playerwaypoints.PlayerWaypointManager;
import dev.feintha.playerwaypoints.compat.polymer_rp.DefaultRPBuilderInstance;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.resourcepack.impl.PolymerResourcePackImpl;
import eu.pb4.polymer.resourcepack.impl.PolymerResourcePackMod;
import eu.pb4.polymer.resourcepack.impl.client.rendering.PolymerResourcePack;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.resource.server.ServerResourcePackLoader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ExecuteCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.waypoint.WaypointStyle;
import net.minecraft.world.waypoint.WaypointStyles;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class WorldWaypointCommand extends AbstractCommandRegistrar {
    static int globalWaypointExecutor(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        int color = 0xffdb7991;
        RegistryKey<WaypointStyle> style = WaypointStyles.DEFAULT;
        ServerWorld world = DimensionArgumentType.getDimensionArgument(context, "world");
        Vec3d pos = Vec3ArgumentType.getVec3(context, "pos");
        Identifier id = IdentifierArgumentType.getIdentifier(context, "id");
        double range = 6e7;
        try { color = HexColorArgumentType.getArgbColor(context, "color"); } catch (IllegalArgumentException ignored) {}
        try { style = WaypointStyles.of(StringArgumentType.getString(context, "style")); } catch (IllegalArgumentException ignored) {}
        try { range = DoubleArgumentType.getDouble(context, "range"); } catch (IllegalArgumentException ignored) {}
        var wp = PlayerWaypointManager.addGlobalWaypoint(world, BlockPos.ofFloored(pos), id);
        wp.setWaypointColor(color);
        wp.setWaypointStyle(style);
        wp.setPosition(pos);
        return 0;
    }
    public static class WaypointStyleArgumentType implements ArgumentType<Identifier> {


        public static IdentifierArgumentType identifier() {
            return new IdentifierArgumentType();
        }

        public static Identifier getIdentifier(CommandContext<ServerCommandSource> context, String name) {
            return (Identifier)context.getArgument(name, Identifier.class);
        }

        public Identifier parse(StringReader stringReader) throws CommandSyntaxException {
            return Identifier.fromCommandInput(stringReader);
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            builder.suggest("default");
            builder.suggest("bowtie");
            return builder.buildFuture();
        }
    }
    static LiteralArgumentBuilder<ServerCommandSource> cmd_Create() {
        return CommandManager.literal("create")
                .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                    .then(CommandManager.argument("world", DimensionArgumentType.dimension())
                        // global waypoint
                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                    .executes(WorldWaypointCommand::globalWaypointExecutor) // global, pos, id
                                        .then(CommandManager.argument("color", HexColorArgumentType.hexColor())
                                            .executes(WorldWaypointCommand::globalWaypointExecutor)  // global, pos, id, color
                                                .then(CommandManager.argument("style", WaypointStyleArgumentType.identifier())
                                                    .executes(WorldWaypointCommand::globalWaypointExecutor)  // global, pos, id, color, style
                )))));
    }
    @Override
    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("world-waypoint").then(cmd_Create()));
    }
}
