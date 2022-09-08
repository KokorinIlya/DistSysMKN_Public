package solution;

public interface MutexProcess {
    void onMessage(int sourcePid, Object message);
    void onLockRequest();
    void onUnlockRequest();
}
