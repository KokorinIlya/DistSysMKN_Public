import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.ArrayList;
import java.util.Base64;
import java.security.MessageDigest;

public class Indexer {
    private static Logger log = LoggerFactory.getLogger(Tree.class);

    private static record PathAndHash(String path, String hashBase64) {}

    private static void collectFiles(
        FileSystem fs, Path path, List<PathAndHash> index
    ) throws Exception {
        if (fs.getFileStatus(path).isDirectory()) {
            for (var child : fs.listStatus(path)) {
                collectFiles(fs, child.getPath(), index);
            }
        } else {
            var digest = MessageDigest.getInstance("SHA-256");
            var buffer = new byte[8192];
            try (var is = fs.open(path)) {
                while (true) {
                    int count = is.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    digest.update(buffer, 0, count);
                }
            }

            var curPath = path.toUri().getRawPath();
            var hashBase64 = Base64.getEncoder().encodeToString(digest.digest());
            log.info("File {} has hash {}", curPath, hashBase64);
            index.add(new PathAndHash(curPath, hashBase64));
        }
    }

    private static void writeIndex(
        FileSystem fs, List<PathAndHash> index, Path indexDir
    ) throws Exception {
        fs.mkdirs(indexDir);
        var indexFile = indexDir.suffix("/index.idx");
        var successFile = indexDir.suffix("/SUCCESS");

        try (
            var out = fs.create(indexFile);
            var writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            var bufWriter = new BufferedWriter(writer);
        ) {
            for (var entry : index) {
                var curLine = String.format("%s %s", entry.hashBase64, entry.path);
                bufWriter.write(curLine);
                bufWriter.newLine();
            }
        }
    
        try (
            var out = fs.create(successFile);
            var writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        ) {
            writer.write("OK\n");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                "Arguments: hdfs://host:port /path/to/root /path/to/index"
            );
        }
        var hdfsUri = args[0];
        var root = new Path(args[1]);
        var indexPath = new Path(args[2]);

        Configuration conf = new Configuration();
        try (
            var fs = FileSystem.get(URI.create(hdfsUri), conf);
        ) {
            var index = new ArrayList<PathAndHash>();
            collectFiles(fs, root, index);
            index.sort((a, b) -> a.hashBase64.compareTo(b.hashBase64));
            writeIndex(fs, index, indexPath);
        }
    }
}
