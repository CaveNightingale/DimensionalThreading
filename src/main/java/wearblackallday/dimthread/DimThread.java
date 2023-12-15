package wearblackallday.dimthread;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.apache.logging.log4j.Logger;
import wearblackallday.dimthread.thread.IMutableMainThread;
import wearblackallday.dimthread.util.IThreadedServer;
import wearblackallday.dimthread.util.ServerWorldAccessor;

public class DimThread implements ModInitializer {

	public static final Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("DimThread");

	private static GameRules.Key<GameRules.BooleanRule> ACTIVE;

	/**
	 * Check if the world is being processed on its own worker thread
	 * @param world the world
	 * @return true if the world is being processed on its own worker thread
	 */
	public static boolean onWorkerThread(ServerWorld world) {
		return ((ServerWorldAccessor) world).dimthread_getWorkerThread() == ((IMutableMainThread) world).dimthread_getCurrentThread();
	}

	/**
	 * Is the dimthread active on this server?
	 * @param server the server
	 * @return true if the dimthread is active
	 */
	public static boolean isActive(MinecraftServer server) {
		return ((IThreadedServer) server).isDimThreadActive();
	}

	/**
	 * Set the dimthread active on this server
	 * @param server the server
	 * @param value the value
	 */
	public static void setActive(MinecraftServer server, GameRules.BooleanRule value) {
		((IThreadedServer) server).setDimThreadActive(value.get());
	}

	@Override
	public void onInitialize() {
		var active = GameRuleFactory.createBooleanRule(true, DimThread::setActive);
		ACTIVE = GameRuleRegistry.register("dimthreadActive", GameRules.Category.UPDATES, active);
		ServerLifecycleEvents.SERVER_STARTED.register(this::onServerLoaded);
	}

	public void onServerLoaded(MinecraftServer server) {
		((IThreadedServer) server).setDimThreadActive(server.getGameRules().getBoolean(ACTIVE));
	}
}
