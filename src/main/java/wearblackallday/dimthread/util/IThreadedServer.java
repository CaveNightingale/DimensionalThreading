package wearblackallday.dimthread.util;

/**
 * In order to fix the serious performance problem, we migrated to this implement
 */

public interface IThreadedServer {
	boolean isDimThreadActive();

	void setDimThreadActive(boolean active);
}
