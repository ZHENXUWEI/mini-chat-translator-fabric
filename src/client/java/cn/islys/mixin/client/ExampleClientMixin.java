package cn.islys.mixin.client;

import net.minecraft.client.MinecraftClient;  // 把 Minecraft 改成 MinecraftClient
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)  // 这里也要改
public class ExampleClientMixin {
    @Inject(at = @At("HEAD"), method = "run")
    private void init(CallbackInfo info) {
        // This code is injected into the start of Minecraft.run()V
    }
}