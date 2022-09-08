import internal.*;
import org.junit.jupiter.api.Test;
import solution.ProcessLamport;

import java.util.Random;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessLamportTest {
    private TestSystem testSystem;

    private void initProcs(int nProcs, Supplier<MessageBus> messageBusBuilder) {
        testSystem = new TestSystem(nProcs, 100, ProcessLamport::new, messageBusBuilder);
    }

    private void doLock(int pid, int nProcs) {
        assert 1 <= pid && pid <= nProcs;

        testSystem.getProcs().get(pid - 1).onLockRequest();
        var msgs = testSystem.getEnvs().get(pid - 1).takeIncomingMessages();
        assertEquals(nProcs - 1, msgs.size());
        TestUtils.checkAllPidsPresent(msgs, pid, nProcs);

        for (var msg : msgs) {
            var dstPid = msg.destinationPid();
            testSystem.getProcs().get(dstPid - 1).onMessage(pid, msg.content());
            var returnMsgs = testSystem.getEnvs().get(dstPid - 1).takeIncomingMessages();
            assertEquals(1, returnMsgs.size());
            assertEquals(pid, returnMsgs.get(0).destinationPid());

            testSystem.getProcs().get(pid - 1).onMessage(dstPid, returnMsgs.get(0).content());
            var retRetMsgs = testSystem.getEnvs().get(pid - 1).takeIncomingMessages();
            assertEquals(0, retRetMsgs.size());
        }

        assertEquals(pid, testSystem.getSystem().getLockerPid());
    }

    private void doUnlock(int pid, int nProcs) {
        assert 1 <= pid && pid <= nProcs;

        assertEquals(pid, testSystem.getSystem().getLockerPid());
        testSystem.getProcs().get(pid - 1).onUnlockRequest();
        assertNull(testSystem.getSystem().getLockerPid());

        var msgs = testSystem.getEnvs().get(pid - 1).takeIncomingMessages();
        assertEquals(nProcs - 1, msgs.size());
        TestUtils.checkAllPidsPresent(msgs, pid, nProcs);

        for (var msg : msgs) {
            var dstPid = msg.destinationPid();
            testSystem.getProcs().get(dstPid - 1).onMessage(pid, msg.content());
            var returnMsgs = testSystem.getEnvs().get(dstPid - 1).takeIncomingMessages();
            assertEquals(0, returnMsgs.size());
        }
    }

    @Test
    public void firstLock() {
        for (int nProcs = 1; nProcs <= 10; nProcs++) {
            initProcs(nProcs, () -> null);
            doLock(1, nProcs);
        }
    }

    @Test
    public void firstLockUnlock() {
        for (int nProcs = 1; nProcs <= 10; nProcs++) {
            initProcs(nProcs, () -> null);
            doLock(1, nProcs);
            doUnlock(1, nProcs);
        }
    }

    @Test
    public void firstLockUnlockManyTimes() {
        for (int nProcs = 1; nProcs <= 10; nProcs++) {
            initProcs(nProcs, () -> null);
            for (int i = 0; i < 100; i++) {
                doLock(1, nProcs);
                doUnlock(1, nProcs);
            }
        }
    }

    @Test
    public void roundRobinUnlock() {
        for (int nProcs = 1; nProcs <= 10; nProcs++) {
            initProcs(nProcs, () -> null);
            for (int i = 0; i < 1000; i++) {
                var pid = i % nProcs + 1;
                doLock(pid, nProcs);
                doUnlock(pid, nProcs);
            }
        }
    }

    @Test
    public void randomUnlock() {
        var random = new Random(System.nanoTime());
        for (int nProcs = 1; nProcs <= 10; nProcs++) {
            initProcs(nProcs, () -> null);
            for (int i = 0; i < 1000; i++) {
                var pid = random.nextInt(nProcs) + 1;
                doLock(pid, nProcs);
                doUnlock(pid, nProcs);
            }
        }
    }

    @Test
    public void twoConcurrentLocks() {
        initProcs(2, () -> null);
        var procA = testSystem.getProcs().get(0);
        var procB = testSystem.getProcs().get(1);
        var envA = testSystem.getEnvs().get(0);
        var envB = testSystem.getEnvs().get(1);

        procA.onLockRequest();
        var msgsA = envA.takeIncomingMessages();
        assertEquals(1, msgsA.size());
        assertEquals(2, msgsA.get(0).destinationPid());
        assertNull(testSystem.getSystem().getLockerPid());

        procB.onLockRequest();
        var msgsB = envB.takeIncomingMessages();
        assertEquals(1, msgsB.size());
        assertEquals(1, msgsB.get(0).destinationPid());
        assertNull(testSystem.getSystem().getLockerPid());

        procB.onMessage(1, msgsA.get(0).content());
        var retMsgsB = envB.takeIncomingMessages();
        assertEquals(1, retMsgsB.size());
        assertEquals(1, retMsgsB.get(0).destinationPid());
        assertNull(testSystem.getSystem().getLockerPid());

        procA.onMessage(2, msgsB.get(0).content());
        var retMsgsA = envA.takeIncomingMessages();
        assertEquals(1, retMsgsA.size());
        assertEquals(2, retMsgsA.get(0).destinationPid());
        assertNull(testSystem.getSystem().getLockerPid());

        procB.onMessage(1, retMsgsA.get(0).content());
        assertEquals(0, envB.takeIncomingMessages().size());
        assertNull(testSystem.getSystem().getLockerPid());

        procA.onMessage(2, retMsgsB.get(0).content());
        assertEquals(0, envA.takeIncomingMessages().size());
        assertEquals(1, testSystem.getSystem().getLockerPid());

        procA.onUnlockRequest();
        msgsA = envA.takeIncomingMessages();
        assertEquals(1, msgsA.size());
        assertEquals(2, msgsA.get(0).destinationPid());
        assertNull(testSystem.getSystem().getLockerPid());

        procB.onMessage(1, msgsA.get(0).content());
        assertEquals(0, envB.takeIncomingMessages().size());
        assertEquals(2, testSystem.getSystem().getLockerPid());

        procB.onUnlockRequest();
        msgsB = envB.takeIncomingMessages();
        assertEquals(1, msgsB.size());
        assertEquals(1, msgsB.get(0).destinationPid());
        assertNull(testSystem.getSystem().getLockerPid());

        procA.onMessage(2, msgsB.get(0).content());
        assertEquals(0, envA.takeIncomingMessages().size());
        assertNull(testSystem.getSystem().getLockerPid());
    }

    @SuppressWarnings("SameParameterValue")
    private void testRandomLocks(float lockProb, float unlockProb, Supplier<MessageBus> messageBusBuilder) {
        for (int nProcs = 1; nProcs <= 100; nProcs++) {
            for (int i = 0; i < 100; i++) {
                initProcs(nProcs, messageBusBuilder);
                testSystem.runSimulation(nProcs * 100, lockProb, unlockProb, false);
            }
        }
    }

    @Test
    public void randomLocksFair() {
        testRandomLocks(0.3f, 0.3f, FairMessageBus::new);
    }

    @Test
    public void randomLocksSmallUnlockProbFair() {
        testRandomLocks(0.3f, 0.05f, FairMessageBus::new);
    }

    @Test
    public void randomLocksFIFO() {
        testRandomLocks(0.3f, 0.3f, FIFOMessageBus::new);
    }

    @Test
    public void randomLocksSmallUnlockProbFIFO() {
        testRandomLocks(0.3f, 0.05f, FIFOMessageBus::new);
    }

    @Test()
    public void randomLocksRandom() {
        assertThrows(Throwable.class, () ->
                testRandomLocks(0.3f, 0.3f, RandomMessageBus::new)
        );
    }

    @Test
    public void randomLocksSmallUnlockProbRandom() {
        assertThrows(Throwable.class, () ->
                testRandomLocks(0.3f, 0.05f, RandomMessageBus::new)
        );
    }
}
