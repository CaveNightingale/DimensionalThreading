package wearblackallday.dimthread.mixin;

import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wearblackallday.dimthread.util.ThreadPool;

@Mixin(MinecraftDedicatedServer.class)
public abstract class MinecraftDedicatedServerMixin extends MinecraftServerMixin {
	@Inject(method = "exit", at = @At("HEAD"))
	private void onExit(CallbackInfo ci) {
		ThreadPool pool = getDimThreadPool();
		if (pool != null)
			pool.shutdown();
	}
}
