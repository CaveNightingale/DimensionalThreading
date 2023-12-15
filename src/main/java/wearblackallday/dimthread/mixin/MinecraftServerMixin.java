package wearblackallday.dimthread.mixin;

import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wearblackallday.dimthread.DimThread;
import wearblackallday.dimthread.util.*;

import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements IThreadedServer {

	@Shadow
	private int ticks;
	@Shadow
	private PlayerManager playerManager;
	private boolean dimthread_active;
	private ThreadPool dimthread_threadPool;

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
		return ServerManager.isActive((MinecraftServer) (Object) this) ? Collections.emptyIterator() : oldValue;
	}

	/**
	 * Distributes world ticking over 3 worker threads (one for each dimension) and waits until
	 * they are all complete.
	 */
	@Inject(method = "tickWorlds", at = @At(value = "INVOKE",
			target = "Ljava/lang/Iterable;iterator()Ljava/util/Iterator;"))
	public void tickWorlds(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
		if (!ServerManager.isActive((MinecraftServer) (Object) this)) return;

		AtomicReference<CrashInfo> crash = new AtomicReference<>();
		ThreadPool pool = DimThread.getThreadPool((MinecraftServer) (Object) this);

		pool.execute(this.getWorlds().iterator(), serverWorld -> {
			DimThread.attach(Thread.currentThread(), serverWorld);

			if (this.ticks % 20 == 0) {
				WorldTimeUpdateS2CPacket timeUpdatePacket = new WorldTimeUpdateS2CPacket(
						serverWorld.getTime(), serverWorld.getTimeOfDay(),
						serverWorld.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE));

				this.playerManager.sendToDimension(timeUpdatePacket, serverWorld.getRegistryKey());
			}

			DimThread.swapThreadsAndRun(() -> {
				try {
					serverWorld.tick(shouldKeepTicking);
				} catch (Throwable throwable) {
					crash.set(new CrashInfo(serverWorld, throwable));
				}
			}, serverWorld, serverWorld.getChunkManager());
		});

		pool.awaitCompletion();
		getWorlds().forEach(world -> ((ServerWorldAccessor) world).dimthread_tickTime()); // Time ticking is not thread-safe, fix https://github.com/WearBlackAllDay/DimensionalThreading/issues/72

		if (crash.get() != null) {
			crash.get().crash("Exception ticking world");
		}
	}

	/**
	 * Shutdown all threadpools when the server stop.
	 * Prevent server hang when stopping the server.
	 */
	@Inject(method = "shutdown", at = @At("HEAD"))
	public void shutdownThreadpool(CallbackInfo ci) {
		if (getDimThreadPool() != null) {
			getDimThreadPool().shutdown();
		}
	}

	@Override
	public boolean isDimThreadActive() {
		return dimthread_active;
	}

	@Override
	public void setDimThreadActive(boolean active) {
		this.dimthread_active = active;
	}

	@Override
	public ThreadPool getDimThreadPool() {
		return dimthread_threadPool;
	}

	@Override
	public void setDimThreadPool(ThreadPool pool) {
		this.dimthread_threadPool = pool;
	}
}
