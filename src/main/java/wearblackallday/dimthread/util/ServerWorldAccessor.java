package wearblackallday.dimthread.util;

import wearblackallday.dimthread.thread.DimensionalThread;

/**
 * Create this calass wo that we can call tickTime from ServerWorldMixin
 */
public interface ServerWorldAccessor {
	void dimthread_tickTime();

	/**
	 * Get the worker thread of this world
	 * @return the worker thread
	 */
	DimensionalThread dimthread_getWorkerThread();
}
