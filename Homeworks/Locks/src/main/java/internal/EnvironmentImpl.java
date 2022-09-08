package internal;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class EnvironmentImpl implements Environment {
    private final int processId;
    private final int numberOfProcesses;
    private final SystemState systemState;

    private List<IncomingMessage> incomingMessages;

    public EnvironmentImpl(int processId, int numberOfProcesses, SystemState systemState) {
        assert processId > 0 && numberOfProcesses > 0 && systemState != null;
        this.processId = processId;
        this.numberOfProcesses = numberOfProcesses;
        this.systemState = systemState;
        incomingMessages = new ArrayList<>();
    }

    @Override
    public int getProcessId() {
        return processId;
    }

    @Override
    public int getNumberOfProcesses() {
        return numberOfProcesses;
    }

    @Override
    public void lock() {
        systemState.tryLock(processId);
    }

    @Override
    public void unlock() {
        systemState.tryUnlock(processId);
    }

    @Override
    public void send(int destinationPid, Object message) {
        assert destinationPid > 0 && destinationPid != processId;

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

        incomingMessages.add(new IncomingMessage(destinationPid, message));
    }

    public List<IncomingMessage> takeIncomingMessages() {
        var result = incomingMessages;
        incomingMessages = new ArrayList<>();
        return result;
    }
}
