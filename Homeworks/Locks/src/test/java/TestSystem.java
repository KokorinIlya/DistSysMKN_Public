import internal.*;
import internal.SystemState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import solution.MutexProcess;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class TestSystem {
    private final SystemState systemState;
    private final List<EnvironmentImpl> envs;
    private final List<MutexProcess> procs;
    private final MessageBus messageBus;
    private final Random random;
    private final HashSet<Integer> waitingPids;

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(TestSystem.class);

    public SystemState getSystem() {
        return systemState;
    }

    public List<EnvironmentImpl> getEnvs() {
        return envs;
    }

    public List<MutexProcess> getProcs() {
        return procs;
    }

    public TestSystem(
            int nProcs, int maxMessageSize,
            Function<Environment, MutexProcess> processBuilder,
            Supplier<MessageBus> messageBusBuilder
    ) {
        assert nProcs > 0;
        systemState = new SystemState(maxMessageSize);
        envs = new ArrayList<>();
        procs = new ArrayList<>();
        messageBus = messageBusBuilder.get();
        random = new Random(System.nanoTime());
        waitingPids = new HashSet<>();

        for (int i = 1; i <= nProcs; i++) {
            var env = new EnvironmentImpl(i, nProcs, systemState);
            envs.add(env);
            procs.add(processBuilder.apply(env));
        }
    }

    private void doUnlock() {
        var lockerPid = systemState.getLockerPid();
        assert lockerPid != null && lockerPid > 0 && !waitingPids.contains(lockerPid);
        procs.get(lockerPid - 1).onUnlockRequest();
        assert systemState.getLockerPid() == null : "Must be unlocked";
        var msgs = envs.get(lockerPid - 1).takeIncomingMessages();
        for (var msg : msgs) {
            messageBus.addNewMessage(lockerPid, msg.destinationPid(), msg.content());
        }
    }

    private boolean canLock() {
        assert envs.size() == procs.size();
        if (systemState.getLockerPid() != null) {
            return waitingPids.size() + 1 < procs.size();
        } else {
            return waitingPids.size() < procs.size();
        }
    }

    private void doRequestLock() {
        while (true) {
            var newPid = 1 + random.nextInt(procs.size());
            if (!waitingPids.contains(newPid) && !Objects.equals(newPid, systemState.getLockerPid())) {
                procs.get(newPid - 1).onLockRequest();
                var msgs = envs.get(newPid - 1).takeIncomingMessages();
                for (var msg : msgs) {
                    messageBus.addNewMessage(newPid, msg.destinationPid(), msg.content());
                }
                if (!Objects.equals(newPid, systemState.getLockerPid())) {
                    var addResult = waitingPids.add(newPid);
                    assert addResult;
                }
                break;
            }
        }
    }

    private void processMessage(Message newMsg) {
        var receiverPid = newMsg.destinationPid();
        var senderPid = newMsg.sourcePid();

        assert receiverPid > 0 && senderPid > 0 && receiverPid != senderPid;

        var prevLockerPid = systemState.getLockerPid();
        procs.get(receiverPid - 1).onMessage(senderPid, newMsg.content());
        var msgs = envs.get(receiverPid - 1).takeIncomingMessages();
        for (var msg : msgs) {
            messageBus.addNewMessage(receiverPid, msg.destinationPid(), msg.content());
        }
        var newLockerPid = systemState.getLockerPid();

        assert Objects.equals(prevLockerPid, newLockerPid) ||
                Objects.equals(receiverPid, prevLockerPid) && newLockerPid == null ||
                prevLockerPid == null && Objects.equals(newLockerPid, receiverPid);
        if (prevLockerPid == null && Objects.equals(newLockerPid, receiverPid)) {
            assert waitingPids.contains(receiverPid);
            var removeResult = waitingPids.remove(receiverPid);
            assert removeResult;
        }
    }

    public void runSimulation(int steps, float lockProb, float unlockProb, boolean isToken) {
        if (isToken) {
            var startMsgs = envs.get(0).takeIncomingMessages();
            assert startMsgs.size() == 1;
            var msg = startMsgs.get(0);
            processMessage(new Message(1, msg.destinationPid(), msg.content()));
        }

        for (int step = 0; step < steps; step++) {
            if (systemState.getLockerPid() != null && random.nextFloat() < unlockProb) {
                doUnlock();
            } else if (canLock() && random.nextFloat() < lockProb) {
                doRequestLock();
            } else {
                var newMsg = messageBus.getNextMessage();
                if (newMsg != null) {
                    processMessage(newMsg);
                } else if (systemState.getLockerPid() != null) {
                    doUnlock();
                } else {
                    assert waitingPids.size() == 0 : "Algorithm lacks progress guarantees";
                    doRequestLock();
                }
            }
        }
    }
}
