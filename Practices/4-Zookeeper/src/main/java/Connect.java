import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Connect {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Zookeeper connect string not specified");
        } 

        Logger log = LoggerFactory.getLogger(Connect.class);
        BlockingQueue<WatchedEvent> queue = new LinkedBlockingDeque<>();
        try (
                ZooKeeper zoo = new ZooKeeper(args[0], 5000, (event) -> {
                    log.info("Received event {}", event);
                    try {
                        queue.put(event);
                    } catch (InterruptedException e) {
                        log.error("Interrupted while putting event {} to the queue", event);
                    }
                })
        ) {
            while (true) {
                WatchedEvent curEvent = queue.take();
                log.info("Processing event {}", curEvent);
                if (curEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    log.info("Connected to Zookeeper");
                } else if (curEvent.getState() == Watcher.Event.KeeperState.Disconnected) {
                    log.info("Disconnected from Zookeeper");
                }
            }
        }
    }
}
