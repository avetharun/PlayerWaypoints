package dev.feintha.playerwaypoints.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class AbstractCommandRegistrar {
    static final List<AbstractCommandRegistrar> commandRegistrars = new ArrayList<>();
    public AbstractCommandRegistrar() {
        commandRegistrars.add(this);
    }
    public static void registerAll(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        commandRegistrars.forEach(r -> r.registerCommands(dispatcher, registryAccess, environment));
    }
    public abstract void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment);
    public static void getImplsForPackage(String packageName) throws Exception{

        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends AbstractCommandRegistrar>> classes = reflections.getSubTypesOf(AbstractCommandRegistrar.class);
        for (Class<? extends AbstractCommandRegistrar> aClass : classes) {
            var a = aClass.getConstructor().newInstance();
        }
    }
    static {  }
}

