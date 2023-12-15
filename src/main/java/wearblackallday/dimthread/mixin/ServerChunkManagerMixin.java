package wearblackallday.dimthread.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wearblackallday.dimthread.DimThread;
import wearblackallday.dimthread.thread.IMutableMainThread;

@Mixin(value = ServerChunkManager.class, priority = 1001)
public abstract class ServerChunkManagerMixin extends ChunkManager implements IMutableMainThread {

	@Shadow
	@Final
	public ThreadedAnvilChunkStorage threadedAnvilChunkStorage;
	@Shadow
	@Final
	@Mutable
	Thread serverThread;
	@Shadow
	@Final
	ServerWorld world;

	@Override
	public void dimthread_setCurrentThread(Thread thread) {
		this.serverThread = thread;
	}

	@Override
	public Thread dimthread_getCurrentThread() {
		return this.serverThread;
	}

	@Inject(method = "getTotalChunksLoadedCount", at = @At("HEAD"), cancellable = true)
	private void getTotalChunksLoadedCount(CallbackInfoReturnable<Integer> ci) {
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			int count = this.threadedAnvilChunkStorage.getTotalChunksLoadedCount();
			if (count < 441) ci.setReturnValue(441);
		}
	}

	@Redirect(method = "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;currentThread()Ljava/lang/Thread;"))
	public Thread currentThread(int x, int z, ChunkStatus leastStatus, boolean create) {
		Thread thread = Thread.currentThread();

		if (DimThread.isActive(this.world.getServer()) && DimThread.onWorkerThread(world)) {
			return this.serverThread;
		}

		return thread;
	}

}
