import internal.FIFOMessageBus;
import internal.FairMessageBus;
import internal.MessageBus;
import internal.RandomMessageBus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import solution.ProcessToken;

import java.util.function.Supplier;

public class ProcessTokenTest {
    private TestSystem testSystem;

    private void initProcs(int nProcs, Supplier<MessageBus> messageBusBuilder) {
        testSystem = new TestSystem(nProcs, 100, ProcessToken::new, messageBusBuilder);
    }

    @SuppressWarnings("SameParameterValue")
    private void testRandomLocks(float lockProb, float unlockProb, Supplier<MessageBus> messageBusBuilder) {
        for (int nProcs = 2; nProcs <= 100; nProcs++) {
            for (int i = 0; i < 100; i++) {
                initProcs(nProcs, messageBusBuilder);
                testSystem.runSimulation(nProcs * 100, lockProb, unlockProb, true);
            }
        }
    }

    @Test
    public void noLocks() {
        for (int nProcs = 2; nProcs < 100; nProcs++) {
            initProcs(nProcs, () -> null);
            int curPid = 1;
            for (int step = 0; step < nProcs * 5 + 7; step++) {
                var msgs = testSystem.getEnvs().get(curPid - 1).takeIncomingMessages();
                assertEquals(1, msgs.size());
                int nextPid = curPid + 1;
                if (curPid == nProcs) {
                    nextPid = 1;
                }
                assertEquals(nextPid, msgs.get(0).destinationPid());
                testSystem.getProcs().get(nextPid - 1).onMessage(curPid, msgs.get(0).content());
                curPid = nextPid;
            }
        }
    }

    @Test
    public void locks() {
        initProcs(4, () -> null);
        var procA = testSystem.getProcs().get(0);
        var procB = testSystem.getProcs().get(1);
        var procC = testSystem.getProcs().get(2);
        var procD = testSystem.getProcs().get(3);

        var envA = testSystem.getEnvs().get(0);
        var envB = testSystem.getEnvs().get(1);
        var envC = testSystem.getEnvs().get(2);
        var envD = testSystem.getEnvs().get(3);

        procB.onLockRequest();
        assertNull(testSystem.getSystem().getLockerPid());

        var msgs = envA.takeIncomingMessages();
        assertEquals(1, msgs.size());
        assertEquals(2, msgs.get(0).destinationPid());

        procB.onMessage(1, msgs.get(0).content());
        assertEquals(0, envB.takeIncomingMessages().size());
        assertEquals(2, testSystem.getSystem().getLockerPid());

        procB.onUnlockRequest();
        assertNull(testSystem.getSystem().getLockerPid());
        msgs = envB.takeIncomingMessages();
        assertEquals(1, msgs.size());
        assertEquals(3, msgs.get(0).destinationPid());

        procD.onLockRequest();
        assertEquals(0, envD.takeIncomingMessages().size());
        assertNull(testSystem.getSystem().getLockerPid());

        procA.onLockRequest();
        assertEquals(0, envA.takeIncomingMessages().size());
        assertNull(testSystem.getSystem().getLockerPid());

        procC.onMessage(2, msgs.get(0).content());
        msgs = envC.takeIncomingMessages();
        assertEquals(1, msgs.size());
        assertEquals(4, msgs.get(0).destinationPid());
        assertNull(testSystem.getSystem().getLockerPid());

        procD.onMessage(1, msgs.get(0).content());
        assertEquals(0, envB.takeIncomingMessages().size());
        assertEquals(4, testSystem.getSystem().getLockerPid());

        procD.onUnlockRequest();
        assertNull(testSystem.getSystem().getLockerPid());
        msgs = envD.takeIncomingMessages();
        assertEquals(1, msgs.size());
        assertEquals(1, msgs.get(0).destinationPid());

        procA.onMessage(4, msgs.get(0).content());
        assertEquals(0, envA.takeIncomingMessages().size());
        assertEquals(1, testSystem.getSystem().getLockerPid());

        procA.onUnlockRequest();
        assertNull(testSystem.getSystem().getLockerPid());
        msgs = envA.takeIncomingMessages();
        assertEquals(1, msgs.size());
        assertEquals(2, msgs.get(0).destinationPid());
    }

    @Test
    public void randomLocksFair() {
        testRandomLocks(0.3f, 0.3f, FairMessageBus::new);
    }

    @Test
    public void randomLocksFIFO() {
        testRandomLocks(0.3f, 0.3f, FIFOMessageBus::new);
    }

    @Test
    public void randomLocksRandom() {
        testRandomLocks(0.3f, 0.3f, RandomMessageBus::new);
    }
}
