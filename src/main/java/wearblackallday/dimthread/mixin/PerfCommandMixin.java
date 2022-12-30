package wearblackallday.dimthread.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.PerfCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PerfCommand.class)
public abstract class PerfCommandMixin {
	/**
	 * Remove /perf because it's not thread-safe
	 */
	@Inject(method = "register", at = @At("HEAD"), cancellable = true)
	private static void removePerfCommand(CommandDispatcher<ServerCommandSource> dispatcher, CallbackInfo ci) {
		ci.cancel();
	}
}
