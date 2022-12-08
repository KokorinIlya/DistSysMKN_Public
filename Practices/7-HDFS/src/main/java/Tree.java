import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import java.net.URI;
import java.io.BufferedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tree {
    private static int HASH_MULTIPLIER = 0x01000193;
    private static int INITIAL_HASH = 0x811C9dC5;
    private static Logger log = LoggerFactory.getLogger(Tree.class);

    private static void printDirectory(Path path, int level) {
        if (level == 0) {
            System.out.println(path.toUri().getRawPath());
        } else {
            var result = String.format("%s %s", "-".repeat(level), path.getName());
            System.out.println(result);
        }
    }

    private static void printFile(Path path, int level, int hash) {
        if (level == 0) {
            var result = String.format("%s (0x%08X)", path.toUri().getRawPath(), hash);
            System.out.println(result);
        } else {
            var result = String.format(
                "%s %s (0x%08X)", "-".repeat(level), path.getName(), hash
            );
            System.out.println(result);
        }
    }

    private static void processEntry(FileSystem fs, Path path, int level) throws Exception {
        if (fs.getFileStatus(path).isDirectory()) {
            printDirectory(path, level);
            for (var child : fs.listStatus(path)) {
                processEntry(fs, child.getPath(), level + 1);
            }
        } else {
            var fnv = INITIAL_HASH;
            try (
                var is = fs.open(path);
                var bufIs = new BufferedInputStream(is);
            ) {
                while (true) {
                    var x = bufIs.read();
                    if (x == -1) {
                        break;
                    }
                    var b = (byte) x;
                    fnv = (fnv * HASH_MULTIPLIER) ^ (b & 0xFF);
                }
            }
            printFile(path, level, fnv);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Arguments: hdfs://host:port /path/to/root");
        }
        var hdfsUri = args[0];
        var root = new Path(args[1]);

        Configuration conf = new Configuration();
        try (
            var fs = FileSystem.get(URI.create(hdfsUri), conf);
        ) {
            log.info("Starting FS traverse");
            processEntry(fs, root, 0);
        }
    }
}
