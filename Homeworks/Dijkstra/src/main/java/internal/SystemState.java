package internal;

public class SystemState {
    private boolean isFinished;
    public final int maxMessageSize;

    public SystemState(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
        this.isFinished = false;
    }

    void finishExecution() {
        if (!isFinished) {
            isFinished = true;
        } else {
            throw new IllegalStateException("Execution already finished");
        }
    }

    boolean isExecutionFinished() {
        return isFinished;
    }
}
