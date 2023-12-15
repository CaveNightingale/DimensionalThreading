package wearblackallday.dimthread.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;

public final class ServerManager {
	public static boolean isActive(MinecraftServer server) {
		return ((IThreadedServer) server).isDimThreadActive();
	}

	public static void setActive(MinecraftServer server, GameRules.BooleanRule value) {
		((IThreadedServer) server).setDimThreadActive(value.get());
	}

	public static ThreadPool getThreadPool(MinecraftServer server) {
		return ((IThreadedServer) server).getDimThreadPool();
	}

	public static void setThreadCount(MinecraftServer server, GameRules.IntRule value) {
		IThreadedServer server1 = (IThreadedServer) server;
		ThreadPool current = server1.getDimThreadPool();
		server1.setDimThreadPool(new ThreadPool(value.get()));
		current.shutdown();
	}
}
