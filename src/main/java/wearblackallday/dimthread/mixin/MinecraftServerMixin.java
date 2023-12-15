package wearblackallday.dimthread.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wearblackallday.dimthread.DimThread;
import wearblackallday.dimthread.util.IThreadedServer;
import wearblackallday.dimthread.util.ServerWorldAccessor;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements IThreadedServer {
	@Unique
	private boolean dimthread_active;

	@Shadow
	public abstract Iterable<ServerWorld> getWorlds();

	/**
	 * Returns an empty iterator to stop {@code MinecraftServer#tickWorlds} from ticking
	 * dimensions. This behaviour is overwritten below.
	 *
	 * @see MinecraftServerMixin#tickWorlds(BooleanSupplier, CallbackInfo)
	 */
	@ModifyVariable(method = "tickWorlds", at = @At(value = "INVOKE_ASSIGN",
			target = "Ljava/lang/Iterable;iterator()Ljava/util/Iterator;", ordinal = 0))
	public Iterator<?> tickWorlds(Iterator<?> oldValue) {
		return DimThread.isActive((MinecraftServer) (Object) this) ? Collections.emptyIterator() : oldValue;
	}

	/**
	 * Distributes world ticking over 3 worker threads (one for each dimension) and waits until
	 * they are all complete.
	 */
	@Inject(method = "tickWorlds", at = @At(value = "INVOKE",
			target = "Ljava/lang/Iterable;iterator()Ljava/util/Iterator;"))
	public void tickWorlds(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		if (!DimThread.isActive((MinecraftServer) (Object) this)) return;

		long begin = System.nanoTime();
		for (ServerWorld world : this.getWorlds()) {
			((ServerWorldAccessor) world).dimthread_getWorkerThread().tick(shouldKeepTicking);
		}
		for (ServerWorld world : this.getWorlds()) {
			((ServerWorldAccessor) world).dimthread_getWorkerThread().retrieve();// Check for errors
		}
		long end = System.nanoTime();
		DimThread.LOGGER.debug("World ticking took {} ms", (end - begin) / 1000000.0);

		getWorlds().forEach(world -> ((ServerWorldAccessor) world).dimthread_tickTime()); // Time ticking is not thread-safe, fix https://github.com/WearBlackAllDay/DimensionalThreading/issues/72
	}

	@Override
	public boolean isDimThreadActive() {
		return dimthread_active;
	}

	@Override
	public void setDimThreadActive(boolean active) {
		this.dimthread_active = active;
	}
}
