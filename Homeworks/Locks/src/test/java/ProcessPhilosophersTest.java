import internal.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import solution.ProcessPhilosophers;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProcessPhilosophersTest {
    private TestSystem testSystem;

    private void initProcs(int nProcs, Supplier<MessageBus> messageBusBuilder) {
        testSystem = new TestSystem(nProcs, 100, ProcessPhilosophers::new, messageBusBuilder);
    }

    private void doLock(int pid, int nProcs, Consumer<List<IncomingMessage>> checker) {
        assert 1 <= pid && pid <= nProcs;
        testSystem.getProcs().get(pid - 1).onLockRequest();
        var msgs = testSystem.getEnvs().get(pid - 1).takeIncomingMessages();
        assertTrue(msgs.size() < nProcs);
        if (checker != null) {
            checker.accept(msgs);
        }

        for (var msg : msgs) {
            var dstPid = msg.destinationPid();
            testSystem.getProcs().get(dstPid - 1).onMessage(pid, msg.content());
            var responses = testSystem.getEnvs().get(dstPid - 1).takeIncomingMessages();
            assertEquals(1, responses.size());

            var resp = responses.get(0);
            testSystem.getProcs().get(pid - 1).onMessage(dstPid, resp.content());
            assertEquals(0, testSystem.getEnvs().get(pid - 1).takeIncomingMessages().size());
        }
        assertEquals(pid, testSystem.getSystem().getLockerPid());
    }

    private void doUnlock(int pid) {
        assertEquals(pid, testSystem.getSystem().getLockerPid());
        testSystem.getProcs().get(pid - 1).onUnlockRequest();
        assertNull(testSystem.getSystem().getLockerPid());
        assertEquals(0, testSystem.getEnvs().get(pid - 1).takeIncomingMessages().size());
    }

    @Test
    public void firstLockUnlockManyTimes() {
        for (int nProcs = 1; nProcs <= 10; nProcs++) {
            initProcs(nProcs, () -> null);
            for (int i = 0; i < 100; i++) {
                doLock(1, nProcs, null);
                doUnlock(1);
            }
        }
    }

    @Test
    public void roundRobinUnlock() {
        for (int nProcs = 1; nProcs <= 10; nProcs++) {
            initProcs(nProcs, () -> null);
            for (int i = 0; i < 1000; i++) {
                var pid = i % nProcs + 1;
                doLock(pid, nProcs, null);
                doUnlock(pid);
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
                doLock(pid, nProcs, null);
                doUnlock(pid);
            }
        }
    }

    @Test
    public void secondLockIsFree() {
        for (int nProcs = 1; nProcs <= 100; nProcs++) {
            initProcs(nProcs, () -> null);

            doLock(1, nProcs, null);
            doUnlock(1);

            testSystem.getProcs().get(0).onLockRequest();
            assertEquals(0, testSystem.getEnvs().get(0).takeIncomingMessages().size());
            assertEquals(1, testSystem.getSystem().getLockerPid());
        }
    }

    @Test
    public void onlyLockHoldersConst() {
        int nProcs = 100;
        initProcs(nProcs, () -> null);

        for (int pid = 1; pid <= 3; pid++) {
            doLock(pid, nProcs, null);
            doUnlock(pid);
        }

        for (int i = 0; i < 100; i++) {
            for (int pid = 1; pid <= 3; pid++) {
                var curPid = pid;
                doLock(pid, nProcs, (msgs) -> {
                    assertTrue(msgs.size() <= 2);
                    for (var msg : msgs) {
                        assertTrue(
                                1 <= msg.destinationPid() && msg.destinationPid() <= 3 &&
                                        msg.destinationPid() != curPid
                        );
                    }
                });
                doUnlock(pid);
            }
        }
    }

    @Test
    public void onlyLockHolders() {
        var random = new Random(System.nanoTime());
        for (int i = 0; i < 1000; i++) {
            var nProcs = 10 + random.nextInt(100);
            initProcs(nProcs, () -> null);
            var lockersCount = 1 + random.nextInt(nProcs / 2);
            var lockers = new HashSet<Integer>();

            while (lockers.size() < lockersCount) {
                var pid = random.nextInt(nProcs) + 1;
                //noinspection ConstantConditions
                assert 1 <= pid && pid <= nProcs;
                lockers.add(pid);
            }

            for (var pid : lockers) {
                doLock(pid, nProcs, null);
                doUnlock(pid);
            }

            for (int j = 0; j < 10; j++) {
                var lockersList = new ArrayList<>(lockers);
                Collections.shuffle(lockersList);
                for (var pid : lockersList) {
                    doLock(pid, nProcs, (msgs) -> {
                        assertTrue(msgs.size() <= lockers.size());
                        for (var msg : msgs) {
                            assertTrue(
                                    lockers.contains(msg.destinationPid()) &&
                                            msg.destinationPid() != pid
                            );
                        }
                    });
                    doUnlock(pid);
                }
            }
        }
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
