package dev.feintha.playerwaypoints.mixin;

import dev.feintha.playerwaypoints.compat.polymer_rp.DefaultRPBuilderInstance;
import eu.pb4.polymer.resourcepack.impl.generation.DefaultRPBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@SuppressWarnings("UnstableApiUsage")
@Pseudo
@Mixin(value = DefaultRPBuilder.class, remap = false)
public class DefaultRPBuilderMixin {
    @Inject(method = "<init>", at=@At("TAIL"))
    void initMixin(Path outputPath, CallbackInfo ci){
        DefaultRPBuilderInstance.LAST = (DefaultRPBuilder) (Object)this;
    }
}
