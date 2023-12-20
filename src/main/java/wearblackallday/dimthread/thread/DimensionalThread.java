package wearblackallday.dimthread.thread;

import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.SystemDetails;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.world.GameRules;

import java.util.concurrent.Semaphore;
import java.util.function.BooleanSupplier;

/**
 * A thread that dedicated to a dimension
 */
public class DimensionalThread extends Thread {

	private final Thread mainThread = Thread.currentThread();
	private final ServerWorld world;
	private final Semaphore lockTick = new Semaphore(0), lockRetrieve = new Semaphore(0);
	private volatile CrashException error = null;
	private BooleanSupplier shouldKeepTicking = () -> true;

	/**
	 * Creates a new dimensional thread
	 * @param world the world this thread is dedicated to
	 */
	public DimensionalThread(ServerWorld world) {
		this.world = world;
		setName("Dimensional Thread (" + world.getRegistryKey().getValue() + ")");
	}

	@Override
	public void run() {
		long tick = 0;
		while (!isInterrupted()) {
			try {
				lockTick.acquire();
			} catch (InterruptedException e) {
				return; // Sever is shutting down, don't care about errors
			}
			try {
				tick++;
				if (tick % 20 == 0) {
					world.getServer().getPlayerManager().sendToDimension(
							new WorldTimeUpdateS2CPacket(
									world.getTime(),
									world.getTimeOfDay(),
									world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)),
							world.getRegistryKey());
				}
				((IMutableMainThread) world).dimthread_setCurrentThread(this);
				((IMutableMainThread) world.getChunkManager()).dimthread_setCurrentThread(this);
				world.tick(shouldKeepTicking);
				((IMutableMainThread) world).dimthread_setCurrentThread(mainThread);
				((IMutableMainThread) world.getChunkManager()).dimthread_setCurrentThread(mainThread);
			} catch (Throwable t) {
				CrashReport cr = t instanceof CrashException ce ? ce.getReport() : CrashReport.create(t, "Exception ticking world");
				SystemDetails details = cr.getSystemDetailsSection();
				details.addSection("Dimension", world.getRegistryKey().getValue().toString());
				details.addSection("Dimensional Thread", getName());
				world.addDetailsToCrashReport(cr);
				error = t instanceof CrashException ce ? ce : new CrashException(cr);
				return;
			} finally {
				lockRetrieve.release();
			}
		}
	}

	/**
	 * Ticks the dimension on the worker thread
	 */
	public void tick(BooleanSupplier shouldKeepTicking) {
		this.shouldKeepTicking = shouldKeepTicking;
		lockTick.release();
	}

	/**
	 * Waits until the dimension has been ticked and retrieves any errors that occurred
	 */
	public void retrieve() {
		try {
			lockRetrieve.acquire();
		} catch (InterruptedException e) {
			return; // Sever is shutting down, don't care about errors
		}
		if (error != null) {
			CrashReport cr = error.getReport();
			cr.addElement("Caller Thread", 1); // Add the main thread stack trace
			throw error;
		}
	}
}
