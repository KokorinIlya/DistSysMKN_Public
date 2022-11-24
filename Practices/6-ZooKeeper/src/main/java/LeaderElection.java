import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import java.util.concurrent.CountDownLatch;
import java.io.IOException;
import java.util.List;

public class LeaderElection {
    private static enum State {
        INITIAL,
        FOLLOWER,
        LEADER,
        LEADER_RESIGNED
    }

    private Logger log = LoggerFactory.getLogger(LeaderElection.class);
    private State state = State.INITIAL;
    private BlockingQueue<WatchedEvent> queue = new LinkedBlockingDeque<>();
    private ZooKeeper zoo = null;
    private String myNodePath = null;
    private String prevNodePath = null;

    private boolean installNodeWatch(String nodePath) throws KeeperException, InterruptedException {
        log.info("Setting watch for node {}", nodePath);
        /*
        If previously installed watches are not removed, multiple NODE_DELETED
        events will be processed
        */
        try {
            zoo.removeAllWatches(nodePath, Watcher.WatcherType.Any, true);
            log.info("All previously installed watches for node {} removed", nodePath);
        } catch (KeeperException.NoWatcherException e) {
            log.info("Setting the first watch for node {}", nodePath);
        }
        
        Stat result = zoo.exists(nodePath, (event) -> {
            /*
            If events are not filtered, multiple SYNC_CONNECTED/DISCONNECTED/etc 
            events will be processed: one by main watcher, and one per node watcher
            */
            if (
                nodePath.equals(event.getPath()) &&
                event.getType() == Watcher.Event.EventType.NodeDeleted
            ) {
                log.info("{} node watch: received event {}, queueing it", nodePath, event);
                try {
                    queue.put(event);
                } catch (InterruptedException e) {
                    log.error("Interrupted while putting event {} to the queue", event);
                }
            } else {
                log.info("{} node watch: received event {}, ignoring it", nodePath, event);
            }
        });
        return result != null;
    }

    private boolean tryCheckLeadership() throws KeeperException, InterruptedException {
        List<String> children = zoo.getChildren("/leader", null);
        children.sort(
            (a, b) -> {
                assert a.startsWith("candidate_") && b.startsWith("candidate_") && !a.equals(b);
                int aNumber = Integer.parseInt(a.substring(10));
                int bNunber = Integer.parseInt(b.substring(10));
                if (aNumber < bNunber) {
                    return -1;
                } else {
                    return 1;
                }
            }
        );
        log.info(
            "Children list is {}",
            children.stream().collect(Collectors.joining(", ", "[", "]"))
        );
        if (children.isEmpty()) {
            throw new IllegalStateException("Session expired: my node not found in the child list");
        }
        
        if (myNodePath.equals("/leader/" + children.get(0))) {
            log.info("Acting as the leader");
            prevNodePath = null;
            state = State.LEADER;
            return true;
        }

        if (children.size() == 1) {
            throw new IllegalStateException("Session expired: my node not found in the child list");
        }
        for (int i = 1; i < children.size(); i++) {
            if (myNodePath.equals("/leader/" + children.get(i))) {
                prevNodePath = "/leader/" + children.get(i - 1);

                if (!installNodeWatch(prevNodePath)) {
                    log.info("Previous node {} removed, retrying", prevNodePath);
                    return false;
                }
                log.info(
                    "Acting as follower; watcher for node {} has been set",
                    prevNodePath
                );
                state = State.FOLLOWER;
                return true;
            }
        }
        throw new IllegalStateException("Session expired: my node not found in the child list");
    }

    private void checkLeadership() throws KeeperException, InterruptedException {
        while (true) {
            if (tryCheckLeadership()) {
                return;
            }
        }
    }

    private void doSync() throws InterruptedException {
        /*
        No synchronous sync() mathod :( 
        Have to simulate it via async method and a CountDownLatch
        */
        boolean[] result = {false};
        CountDownLatch latch = new CountDownLatch(1);
        zoo.sync("/", (rc, path, ctx) -> {
            KeeperException.Code code = KeeperException.Code.get(rc);
            log.info("Sync happended, result is {}", code);
            if (code == KeeperException.Code.OK) {
                result[0] = true;
            }
            latch.countDown();
        }, "sync context");
        latch.await();
        if (!result[0]) {
            throw new IllegalStateException("Sync failed");
        }
    }

    private void initialActions() throws KeeperException, InterruptedException {
        try {
            String createResult = zoo.create(
                "/leader", new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT
            );
            log.info("Voting station creation result is {}", createResult);
        } catch (KeeperException e) {
            if (e.code() == KeeperException.Code.NODEEXISTS) {
                log.info("Voting station already exists", e);
            } else {
                throw e;
            }
        }

        myNodePath = zoo.create(
            "/leader/candidate_",
            new byte[0],
            Ids.OPEN_ACL_UNSAFE,
            CreateMode.EPHEMERAL_SEQUENTIAL
        );
        log.info("My node has been created on path {}", myNodePath);
        if (!installNodeWatch(myNodePath)) {
            throw new IllegalStateException("My node has been deleted");
        }
        checkLeadership();
    }

    private void start(String zkConnectString) throws KeeperException, 
                                                        InterruptedException, 
                                                        IOException {
        try (
            ZooKeeper z = new ZooKeeper(zkConnectString, 5000, (event) -> {
                log.info("Received main watch event {}", event);
                try {
                    queue.put(event);
                } catch (InterruptedException e) {
                    log.error("Interrupted while putting event {} to the queue", event);
                }
            })
        ) {
            this.zoo = z;
            while (true) {
                WatchedEvent curEvent = queue.take();
                log.info("Processing event {}", curEvent);
                if (curEvent.getState() == Watcher.Event.KeeperState.Expired) {
                    log.error("Session expired: exiting");
                    throw new IllegalStateException("Session expired");
                } else if (
                    curEvent.getState() == Watcher.Event.KeeperState.Disconnected ||
                    curEvent.getState() == Watcher.Event.KeeperState.ConnectedReadOnly
                ) {
                    if (state == State.LEADER) {
                        state = State.LEADER_RESIGNED;
                        log.warn("Disconnected: cannot act as a leader");
                    }
                } else if (
                    curEvent.getType() == Watcher.Event.EventType.None && 
                    curEvent.getState() == Watcher.Event.KeeperState.SyncConnected
                ) {
                    if (state == State.INITIAL) {
                        initialActions();
                    } else if (state == State.FOLLOWER) {
                        checkLeadership();
                    } else {
                        /*
                        synchronize to read up-to-date state of the system and guarantee,
                        that my node still exist
                        */
                        doSync();
                        if (zoo.exists(myNodePath, null) == null) {
                            log.error("I am not a leader anymore: sesion expired");
                            throw new IllegalStateException("Session expired");
                        }
                        log.info("I am still leader");
                        state = State.LEADER;
                    }
                } else if (
                    curEvent.getType() == Watcher.Event.EventType.NodeDeleted &&
                    curEvent.getPath().equals(prevNodePath)
                ) {
                    checkLeadership();
                } else if (
                    curEvent.getType() == Watcher.Event.EventType.NodeDeleted &&
                    curEvent.getPath().equals(myNodePath)
                ) {
                    throw new IllegalStateException("Session expired: my node has been removed");
                } else {
                    log.warn("Skipping unknown event {}", curEvent);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Zookeeper connect string not specified");
        } 

        new LeaderElection().start(args[0]);
    }
}
