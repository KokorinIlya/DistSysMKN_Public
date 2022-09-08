package internal;

import java.util.Objects;

public class SystemState {
    private int lockerPid;
    public final int maxMessageSize;

    public SystemState(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
        this.lockerPid = 0;
    }

    public void tryLock(int pid) {
        assert pid > 0;
        if (lockerPid == 0) {
            lockerPid = pid;
        } else {
            throw new IllegalStateException("Another process holds the lock");
        }
    }

    public void tryUnlock(int pid) {
        assert pid > 0;
        if (lockerPid == pid) {
            lockerPid = 0;
        } else {
            throw new IllegalStateException("Process does not hold the lock");
        }
    }

    public Integer getLockerPid() {
        if (lockerPid == 0) {
            return null;
        }
        return lockerPid;
    }
}
