import internal.FIFOMessageBus;
import internal.FairMessageBus;
import internal.MessageBus;
import internal.RandomMessageBus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import solution.ProcessCentralized;

import java.util.Random;
import java.util.function.Supplier;

public class ProcessCentralizedTest {
    private TestSystem testSystem;

    private void initProcs(int nProcs, Supplier<MessageBus> messageBusBuilder) {
        testSystem = new TestSystem(nProcs, 100, ProcessCentralized::new, messageBusBuilder);
    }

    @Test
    public void coordinatorLock() {
        for (int nProcs = 1; nProcs <= 10; nProcs++) {
            initProcs(nProcs, () -> null);
            testSystem.getProcs().get(0).onLockRequest();
            var msgs = testSystem.getEnvs().get(0).takeIncomingMessages();
            assertEquals(0, msgs.size());
            assertEquals(1, testSystem.getSystem().getLockerPid());
        }
    }

    @Test
    public void nonCoordinatorLock() {
        for (int nProcs = 2; nProcs <= 10; nProcs++) {
            initProcs(nProcs, () -> null);

            testSystem.getProcs().get(1).onLockRequest();
            var msgs = testSystem.getEnvs().get(1).takeIncomingMessages();
            assertEquals(1, msgs.size());
            assertEquals(1, msgs.get(0).destinationPid());

            testSystem.getProcs().get(0).onMessage(2, msgs.get(0).content());
            msgs = testSystem.getEnvs().get(0).takeIncomingMessages();
            assertEquals(1, msgs.size());
            assertEquals(2, msgs.get(0).destinationPid());

            testSystem.getProcs().get(1).onMessage(1, msgs.get(0).content());
            assertEquals(2, testSystem.getSystem().getLockerPid());
        }
    }

    private void lockUnlockPid(int processIndex) {
        if (processIndex == 0) {
            testSystem.getProcs().get(0).onLockRequest();
            var msgs = testSystem.getEnvs().get(0).takeIncomingMessages();
            assertEquals(0, msgs.size());
            assertEquals(1, testSystem.getSystem().getLockerPid());

            testSystem.getProcs().get(0).onUnlockRequest();
            msgs = testSystem.getEnvs().get(0).takeIncomingMessages();
            assertEquals(0, msgs.size());
            assertNull(testSystem.getSystem().getLockerPid());
        } else {
            testSystem.getProcs().get(processIndex).onLockRequest();
            var msgs = testSystem.getEnvs().get(processIndex).takeIncomingMessages();
            assertEquals(1, msgs.size());
            assertEquals(1, msgs.get(0).destinationPid());

            testSystem.getProcs().get(0).onMessage(processIndex + 1, msgs.get(0).content());
            msgs = testSystem.getEnvs().get(0).takeIncomingMessages();
            assertEquals(1, msgs.size());
            assertEquals(processIndex + 1, msgs.get(0).destinationPid());

            testSystem.getProcs().get(processIndex).onMessage(1, msgs.get(0).content());
            assertEquals(processIndex + 1, testSystem.getSystem().getLockerPid());

            testSystem.getProcs().get(processIndex).onUnlockRequest();
            assertNull(testSystem.getSystem().getLockerPid());
            msgs = testSystem.getEnvs().get(processIndex).takeIncomingMessages();
            assertEquals(1, msgs.size());
            assertEquals(1, msgs.get(0).destinationPid());

            testSystem.getProcs().get(0).onMessage(processIndex + 1, msgs.get(0).content());
            msgs = testSystem.getEnvs().get(0).takeIncomingMessages();
            assertEquals(0, msgs.size());
        }
    }

    @Test
    public void coordinatorLockUnlock() {
        for (int nProcs = 1; nProcs <= 10; nProcs++) {
            initProcs(nProcs, () -> null);
            for (int i = 0; i < 10; i++) {
                lockUnlockPid(0);
            }
        }
    }

    @Test
    public void nonCoordinatorLockUnlock() {
        for (int nProcs = 2; nProcs <= 10; nProcs++) {
            initProcs(nProcs, () -> null);
            for (int i = 0; i < 10; i++) {
                lockUnlockPid(1);
            }
        }
    }

    @Test
    public void lockUnlockRoundRobin() {
        for (int nProcs = 1; nProcs <= 10; nProcs++) {
            initProcs(nProcs, () -> null);
            for (int i = 0; i < 1000; i++) {
                var processIndex = i % nProcs;
                lockUnlockPid(processIndex);
            }
        }
    }

    @Test
    public void lockUnlockRandom() {
        var random = new Random(System.nanoTime());
        for (int nProcs = 1; nProcs <= 10; nProcs++) {
            initProcs(nProcs, () -> null);
            for (int i = 0; i < 1000; i++) {
                var processIndex = random.nextInt(nProcs);
                lockUnlockPid(processIndex);
            }
        }
    }

    @Test
    public void concurrentLocksCoordinator() {
        initProcs(2, () -> null);
        var procA = testSystem.getProcs().get(0);
        var procB = testSystem.getProcs().get(1);
        var envA = testSystem.getEnvs().get(0);
        var envB = testSystem.getEnvs().get(1);

        procB.onLockRequest();
        var msgs = envB.takeIncomingMessages();
        assertEquals(1, msgs.size());
        assertEquals(1, msgs.get(0).destinationPid());
        assertNull(testSystem.getSystem().getLockerPid());

        procA.onLockRequest();
        assertEquals(0, envA.takeIncomingMessages().size());
        assertEquals(1, testSystem.getSystem().getLockerPid());

        procA.onMessage(2, msgs.get(0).content());
        assertEquals(0, envA.takeIncomingMessages().size());

        procA.onUnlockRequest();
        assertNull(testSystem.getSystem().getLockerPid());
        msgs = envA.takeIncomingMessages();
        assertEquals(1, msgs.size());
        assertEquals(2, msgs.get(0).destinationPid());

        procB.onMessage(1, msgs.get(0).content());
        assertEquals(0, envB.takeIncomingMessages().size());
        assertEquals(2, testSystem.getSystem().getLockerPid());

        procB.onUnlockRequest();
        msgs = envB.takeIncomingMessages();
        assertEquals(1, msgs.size());
        assertEquals(1, msgs.get(0).destinationPid());
        assertNull(testSystem.getSystem().getLockerPid());

        procA.onMessage(2, msgs.get(0).content());
        assertEquals(0, envA.takeIncomingMessages().size());
        assertNull(testSystem.getSystem().getLockerPid());
    }

    @Test
    public void concurrentLocksNonCoordinator() {
        initProcs(3, () -> null);

        var procA = testSystem.getProcs().get(0);
        var procB = testSystem.getProcs().get(1);
        var procC = testSystem.getProcs().get(2);

        var envA = testSystem.getEnvs().get(0);
        var envB = testSystem.getEnvs().get(1);
        var envC = testSystem.getEnvs().get(2);

        procB.onLockRequest();
        var msgsB = envB.takeIncomingMessages();
        assertEquals(1, msgsB.size());
        assertEquals(1, msgsB.get(0).destinationPid());
        assertNull(testSystem.getSystem().getLockerPid());

        procC.onLockRequest();
        var msgsC = envC.takeIncomingMessages();
        assertEquals(1, msgsC.size());
        assertEquals(1, msgsC.get(0).destinationPid());
        assertNull(testSystem.getSystem().getLockerPid());

        procA.onMessage(3, msgsC.get(0).content());
        var msgsA = envA.takeIncomingMessages();
        assertEquals(1, msgsA.size());
        assertEquals(3, msgsA.get(0).destinationPid());
        assertNull(testSystem.getSystem().getLockerPid());

        procA.onMessage(2, msgsB.get(0).content());
        assertEquals(0, envA.takeIncomingMessages().size());
        assertNull(testSystem.getSystem().getLockerPid());

        procC.onMessage(1, msgsA.get(0).content());
        assertEquals(0, envC.takeIncomingMessages().size());
        assertEquals(3, testSystem.getSystem().getLockerPid());

        procC.onUnlockRequest();
        msgsC = envC.takeIncomingMessages();
        assertEquals(1, msgsC.size());
        assertEquals(1, msgsC.get(0).destinationPid());
        assertNull(testSystem.getSystem().getLockerPid());

        procA.onMessage(3, msgsC.get(0).content());
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

    @Test
    public void randomLocksRandom() {
        testRandomLocks(0.3f, 0.3f, RandomMessageBus::new);
    }

    @Test
    public void randomLocksSmallUnlockProbRandom() {
        testRandomLocks(0.3f, 0.05f, RandomMessageBus::new);
    }
}
