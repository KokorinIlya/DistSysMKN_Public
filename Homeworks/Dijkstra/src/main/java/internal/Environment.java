package internal;

import java.util.Map;

public interface Environment {
    int getProcessId();

    Map<Integer, Long> getNeighbours();

    void finishExecution();

    void send(int destinationPid, Object message);
}
