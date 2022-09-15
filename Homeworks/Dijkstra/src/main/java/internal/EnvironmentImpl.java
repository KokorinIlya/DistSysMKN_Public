package internal;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EnvironmentImpl implements Environment {
    private final SystemState systemState;
    private final int processId;
    private final List<Map<Integer, Long>> graph;
    private final MessageBus messageBus;

    public EnvironmentImpl(
            SystemState systemState, MessageBus messageBus,
            int processId, List<Map<Integer, Long>> graph
    ) {
        assert 0 <= processId && processId < graph.size();
        this.systemState = systemState;
        this.messageBus = messageBus;
        this.processId = processId;
        this.graph = graph;
    }

    @Override
    public int getProcessId() {
        return processId;
    }

    @Override
    public Map<Integer, Long> getNeighbours() {
        return Collections.unmodifiableMap(graph.get(processId));
    }

    @Override
    public void finishExecution() {
        systemState.finishExecution();
    }

    @Override
    public void send(int destinationPid, Object message) {
        assert destinationPid >= 0 && destinationPid != processId;
        if (systemState.isExecutionFinished()) {
            throw new IllegalStateException("Execution already finished");
        }

        var messageLength = 0;
        try (
                var bos = new ByteArrayOutputStream();
                var oos = new ObjectOutputStream(bos)
        ) {
            oos.writeObject(message);
            oos.flush();
            messageLength = bos.toByteArray().length;
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot serialize message", e);
        }
        if (messageLength > systemState.maxMessageSize) {
            System.out.println("Has message " + message + " of size " + messageLength);
        }
        assert messageLength <= systemState.maxMessageSize;
        messageBus.addNewMessage(processId, destinationPid, message);
    }

}
