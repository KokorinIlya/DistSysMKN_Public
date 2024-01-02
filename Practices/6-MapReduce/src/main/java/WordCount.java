import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
 
public class WordCount { 
    public static class WordCountMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        private final static IntWritable ONE = new IntWritable(1);
        private final Set<String> stopWords = new HashSet<>();

        @Override
        public void setup(Context context) throws IOException {
            var cacheFiles = context.getCacheFiles();
            if (cacheFiles == null) {
                return;
            }
            assert cacheFiles.length == 1;
            var path = Paths.get(cacheFiles[0].toString()).getFileName();
            try (var stream = Files.newBufferedReader(path)) {
                stream.lines().forEach(line -> {
                    line = line.strip();
                    if (!line.isEmpty()) {
                        stopWords.add(line);
                    }
                });
            }
        }

        @Override
        public void map(
            LongWritable key, Text value, Context context
        ) throws IOException, InterruptedException {
            var tokenizer = new StringTokenizer(value.toString());
            while (tokenizer.hasMoreTokens()) {
                var curToken = tokenizer.nextToken();
                if (stopWords.contains(curToken)) {
                    continue;
                }
                context.write(new Text(curToken), ONE);
            }
        }
    }

    public static class WordCountReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        @Override
        public void reduce(
            Text key, Iterable<IntWritable> values, Context context
        ) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            context.write(key, new IntWritable(sum));
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4 && args.length != 5) {
            throw new IllegalArgumentException(
                "Args: hdfs://host:port /path/to/input /path/to/output <reducers> [/path/to/stop]"
            );
        }
        var hdfsUri = args[0];
        var inputPath = new Path(hdfsUri + args[1]);
        var outputPath = new Path(hdfsUri + args[2]);
        var reducers = Integer.parseInt(args[3]);
        var conf = new Configuration();
        try (
            var fs = FileSystem.get(URI.create(hdfsUri), conf);
        ) {
            if (fs.exists(outputPath)) {
                fs.delete(outputPath, true);
            }
        }

        var job = Job.getInstance(conf, "word count");
        if (args.length == 5) {
            job.addCacheFile(URI.create(args[4]));
        }
        job.setNumReduceTasks(reducers);
        job.setJarByClass(WordCount.class);

        job.setMapperClass(WordCountMapper.class);
        job.setReducerClass(WordCountReducer.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job, inputPath);
        FileOutputFormat.setOutputPath(job, outputPath);

        boolean success = job.waitForCompletion(true);
        if (!success) {
            throw new IllegalStateException("Job not completed");
        }
    }
}