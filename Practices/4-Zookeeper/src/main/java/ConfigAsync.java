import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.nio.charset.StandardCharsets;

public class ConfigAsync {
    private static record KeyValuePair(String key, String value) {}

    /*
    -3     -> create node /configs
    -2     -> create node /configs/config_n
    -1     -> create node /configs/config_n/ready
    i >= 0 -> create node /configs/config_n/pairs[i].key
    */
    private static record RequestContext(int num, String description) {}

    private static record OperationResult(KeeperException.Code code, String path, RequestContext context) {}

    private static final int MAX_ERRORS = 10;

    private Logger log = LoggerFactory.getLogger(ConfigAsync.class);
    private boolean justStarted = true;
    private Set<Integer> kvsCreated = new HashSet<>();
    private String configPath = null;
    private int errors = 0;
    private BlockingQueue<Object> queue = new LinkedBlockingDeque<>();
    private ZooKeeper zoo = null;
    KeyValuePair[] pairs;

    ConfigAsync(KeyValuePair[] pairs) {
        this.pairs = pairs;
    }

    private void insertToQueue(int rc, String path, Object ctx, String name) {
        KeeperException.Code code = KeeperException.Code.get(rc);
        RequestContext context = (RequestContext) ctx;

        String curPath = name;
        if (curPath == null) {
            curPath = path;
        }
        OperationResult res = new OperationResult(code, curPath, context);

        log.info(
            "Async result received: result = {}, path = {}, opnum = {}, description = {}, name = {}",
            code, path, context.num, context.description, name
        );
        try {
            queue.put(res);
        } catch (InterruptedException e) {
            log.warn("Interrupted while queueing event to the queue");
        }
    }

    private void submitOperation(int num) {
        if (num == -3) {
            zoo.create(
                "/configs", new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT,
                this::insertToQueue, new RequestContext(-3, "/configs node creation")
            );
        } else if (num == -2) {
            zoo.create(
                "/configs/config_", new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL,
                this::insertToQueue, new RequestContext(-2, "/configs/config_n node creation")
            );
        } else if (num == -1) {
            zoo.create(
                configPath + "/ready", new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT,
                this::insertToQueue, new RequestContext(-1, "/configs/config_n/ready node creation")
            );
        } else {
            assert num >= 0;
            zoo.create(
                configPath + "/" + pairs[num].key, pairs[num].value.getBytes(StandardCharsets.UTF_8),
                Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT,
                this::insertToQueue, new RequestContext(num, "/configs/config_n/" + pairs[num].key + "node creation")
            );
        }
    }
    
    private void start(String zkConnectString) throws KeeperException, InterruptedException, IOException {
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
                Object curEntry = queue.take();
                log.info("Processing queue entry {}", curEntry);
                
                if (curEntry instanceof WatchedEvent) {
                    WatchedEvent curEvent = (WatchedEvent) curEntry;

                    if (curEvent.getState() == Watcher.Event.KeeperState.Expired) {
                        log.error("Session expired: exiting");
                        throw new IllegalStateException("Session expired");
                    } else if (curEvent.getState() == Watcher.Event.KeeperState.Disconnected) {
                        log.warn("Disconnected");
                        errors++;
                    } else if (
                        curEvent.getType() == Watcher.Event.EventType.None && 
                        curEvent.getState() == Watcher.Event.KeeperState.SyncConnected
                    ) {
                        if (justStarted) {
                            submitOperation(-3);
                           justStarted = false;
                        }
                    } else {
                        log.warn("Ignoring event {} of unknown type", curEvent);
                    }
                } else if (curEntry instanceof OperationResult) {
                    OperationResult curResult = (OperationResult) curEntry;
                    if (
                        curResult.code == KeeperException.Code.OK || 
                        curResult.code == KeeperException.Code.NODEEXISTS
                    ) {
                        if (curResult.code == KeeperException.Code.NODEEXISTS) {
                            log.info(
                                "Operation {}: node on path {} already exists, skipping that phase", 
                                curResult.context, curResult.path
                            );
                            assert curResult.context.num != -2;
                        }
                        if (curResult.context.num == -3) {
                            submitOperation(-2);
                        } else if (curResult.context.num == -2) {
                            configPath = curResult.path;
                            for (int i = 0; i < pairs.length; i++) {
                                submitOperation(i);
                            }
                        } else if (curResult.context.num == -1) {
                            log.info("Config creation finished");
                            return;
                        } else {
                            kvsCreated.add(curResult.context.num);
                            if (kvsCreated.size() == pairs.length) {
                                submitOperation(-1);
                            }
                        }
                    } else {
                        errors++;
                        log.warn(
                            "Operation {} on path {} finished unsuccessfully with code {}", 
                            curResult.context, curResult.path, curResult.code
                        );
                        if (errors >= MAX_ERRORS) {
                            log.error("Maximal number of errors reached");
                            throw new IllegalStateException("Maximal number of errors reached");
                        }
                        submitOperation(curResult.context.num);
                    }
                } else {
                    log.warn("Ignoring entry {} of unknown type", curEntry);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Zookeeper connect string must be specified");
        }
        String zkConnectString = args[0];
        KeyValuePair[] pairs = new KeyValuePair[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            String curPair = args[i];
            String[] parts = curPair.split("=");
            assert parts.length == 2;
            pairs[i - 1] = new KeyValuePair(parts[0], parts[1]);
        }
        new ConfigAsync(pairs).start(zkConnectString);
    }
}
