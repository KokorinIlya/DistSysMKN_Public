import internal.IncomingMessage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TestUtils {
    public static void checkAllPidsPresent(List<IncomingMessage> msgs, int pid, int nProcs) {
        Set<Integer> pids = new HashSet<>();
        for (var msg : msgs) {
            assert 1 <= msg.destinationPid() && msg.destinationPid() <= nProcs && msg.destinationPid() != pid;
            var addRes = pids.add(msg.destinationPid());
            assertTrue(addRes);
        }
        for (int i = 1; i <= nProcs; i++) {
            if (i != pid) {
                assertTrue(pids.contains(i));
            } else {
                assertFalse(pids.contains(i));
            }
        }
        assertEquals(nProcs - 1, pids.size());
    }
}
