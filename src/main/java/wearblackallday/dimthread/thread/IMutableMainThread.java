package wearblackallday.dimthread.thread;

public interface IMutableMainThread {

	void dimthread_setCurrentThread(Thread thread);

	Thread dimthread_getCurrentThread();
}
