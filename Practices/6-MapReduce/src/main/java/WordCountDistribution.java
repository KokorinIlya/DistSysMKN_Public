import java.io.IOException;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import java.net.URI;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.jobcontrol.ControlledJob;
import org.apache.hadoop.mapreduce.lib.jobcontrol.JobControl;
 
public class WordCountDistribution {
    private static final int RANGE_SIZE = 10;

    public static class DistributionMapper extends Mapper<Text, IntWritable, IntWritable, IntWritable> {
        private final static IntWritable ONE = new IntWritable(1);

        @Override
        public void map(
            Text key, IntWritable value, Context context
        ) throws IOException, InterruptedException {
            int rangeBegin = value.get() / RANGE_SIZE;
            context.write(new IntWritable(rangeBegin), ONE);
        }
    }

    public static class DistributionReducer extends Reducer<IntWritable, IntWritable, Text, IntWritable> {
        @Override
        public void reduce(
            IntWritable key, Iterable<IntWritable> values, Context context
        ) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            int rangeBegin = key.get() * RANGE_SIZE;
            int rangeEnd = rangeBegin + RANGE_SIZE;
            var range = String.format("[%d; %d)", rangeBegin, rangeEnd);
            context.write(new Text(range), new IntWritable(sum));
        }
    }

    private static Job configureWordCountJob(
        Configuration conf, Path inputPath, Path outputPath, int reducers
    ) throws IOException {
        var job = Job.getInstance(conf, "word count before distribution");
        job.setNumReduceTasks(reducers);
        job.setJarByClass(WordCountDistribution.class);

        job.setMapperClass(WordCount.WordCountMapper.class);
        job.setReducerClass(WordCount.WordCountReducer.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(SequenceFileOutputFormat.class);

        FileInputFormat.addInputPath(job, inputPath);
        FileOutputFormat.setOutputPath(job, outputPath);

        return job;
    }

    private static Job configureDistributionJob(
        Configuration conf, Path inputPath, Path outputPath, int reducers
    ) throws IOException {
        var job = Job.getInstance(conf, "word count distribution");
        job.setNumReduceTasks(reducers);
        job.setJarByClass(WordCountDistribution.class);

        job.setMapperClass(DistributionMapper.class);
        job.setReducerClass(DistributionReducer.class);

        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(IntWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.addInputPath(job, inputPath);
        FileOutputFormat.setOutputPath(job, outputPath);
        
        return job;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            throw new IllegalArgumentException(
                "Args: hdfs://host:port /path/to/input /path/to/wc-output /path/to/output <reducers>"
            );
        }
        var hdfsUri = args[0];
        var inputPath = new Path(hdfsUri + args[1]);
        var wcOutputPath = new Path(hdfsUri + args[2]);
        var outputPath = new Path(hdfsUri + args[3]);
        var reducers = Integer.parseInt(args[4]);

        var conf = new Configuration();
        try (
            var fs = FileSystem.get(URI.create(hdfsUri), conf);
        ) {
            if (fs.exists(wcOutputPath)) {
                fs.delete(wcOutputPath, true);
            }
            if (fs.exists(outputPath)) {
                fs.delete(outputPath, true);
            }
        }

        var wcJob = new ControlledJob(
            configureWordCountJob(conf, inputPath, wcOutputPath, reducers),
            List.of()
        ); 
        var distributionJob = new ControlledJob(
            configureDistributionJob(conf, wcOutputPath, outputPath, reducers),
            List.of(wcJob)
        );
        var jobControl = new JobControl("wcdist");
        jobControl.addJob(wcJob);
        jobControl.addJob(distributionJob);

        var t = new Thread(jobControl);
        t.start();
        while (!jobControl.allFinished()) {
            Thread.sleep(1000);
        }
        jobControl.stop();
        t.join();
    }
}