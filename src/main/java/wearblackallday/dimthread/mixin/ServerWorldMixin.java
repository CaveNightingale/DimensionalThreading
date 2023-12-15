package wearblackallday.dimthread.mixin;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wearblackallday.dimthread.DimThread;
import wearblackallday.dimthread.thread.DimensionalThread;
import wearblackallday.dimthread.util.ServerWorldAccessor;

import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements ServerWorldAccessor {
	@Unique
	private boolean dimthread_timeTickedOnWorldThread = false;

	@Unique
	private DimensionalThread dimthread_workerThread;

	@Override
	public DimensionalThread dimthread_getWorkerThread() {
		return dimthread_workerThread;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void init(CallbackInfo ci) {
		dimthread_workerThread = new DimensionalThread((ServerWorld) (Object) this);
		dimthread_workerThread.start();
	}

	@Inject(method = "close", at = @At("HEAD"))
	private void close(CallbackInfo ci) {
		dimthread_workerThread.interrupt();
	}


	protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> registryEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
		super(properties, registryRef, registryEntry, profiler, isClient, debugWorld, seed);
	}

	@Shadow
	protected abstract void tickTime();

	/**
	 * Time ticking is not thread-safe. We cancel time ticking from the world thread.
	 * However, DimThread will tick time on the main thread
	 */
	@Inject(method = "tickTime", at = @At("HEAD"), cancellable = true)
	private void preventTimeTicking(CallbackInfo ci) {
		if (DimThread.isActive(getServer()) && DimThread.onWorkerThread((ServerWorld) (Object) this)) {
			dimthread_timeTickedOnWorldThread = true;
			ci.cancel();
		}
	}

	@Override
	public void dimthread_tickTime() {
		if (dimthread_timeTickedOnWorldThread) {
			tickTime();
			dimthread_timeTickedOnWorldThread = false;
		}
	}
}
