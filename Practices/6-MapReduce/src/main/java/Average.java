import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import java.net.URI;
import java.io.DataOutput;
import java.io.DataInput;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Average {
    public static class Aggregate implements Writable {
        private int sum;
        private int count;

        public Aggregate() {
        }

        public Aggregate(int sum, int count) {
            this.sum = sum;
            this.count = count;
        }

        public int getSum() {
            return this.sum;
        }

        public int getCount() {
            return this.count;
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            sum = in.readInt();
            count = in.readInt();
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeInt(sum);
            out.writeInt(count);
        }
    }

    public static class AverageMapper extends Mapper<LongWritable, Text, Text, Aggregate> {
        @Override
        public void map(
            LongWritable key, Text value, Context context
        ) throws IOException, InterruptedException {
            var valueStr = value.toString().strip();
            if (valueStr.isEmpty()) {
                return;
            }
            var parts = valueStr.toString().split(" ");
            assert parts.length == 2;
            var curGroup = parts[0];
            var curValue = Integer.parseInt(parts[1]);
            context.write(new Text(curGroup), new Aggregate(curValue, 1));
        }
    }

    public static class AverageCombiner extends Reducer<Text, Aggregate, Text, Aggregate> {
        @Override
        public void reduce(
            Text key, Iterable<Aggregate> values, Context context
        ) throws IOException, InterruptedException {
            int totalSum = 0;
            int totalCount = 0;
            for (var curAggregate : values) {
                totalSum += curAggregate.getSum();
                totalCount += curAggregate.getCount();
            }
            context.write(key, new Aggregate(totalSum, totalCount));
        }
    }

    public static class AverageReducer extends Reducer<Text, Aggregate, Text, DoubleWritable> {
        @Override
        public void reduce(
            Text key, Iterable<Aggregate> values, Context context
        ) throws IOException, InterruptedException {
            double totalSum = 0;
            double totalCount = 0;
            for (var curAggregate : values) {
                totalSum += (double) curAggregate.getSum();
                totalCount += (double) curAggregate.getCount();
            }
            context.write(key, new DoubleWritable(totalSum / totalCount));
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                "Args: hdfs://host:port /path/to/input /path/to/output <reducers>"
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

        var job = Job.getInstance(conf, "average by group");
        job.setNumReduceTasks(reducers);
        job.setJarByClass(Average.class);

        job.setMapperClass(AverageMapper.class);
        job.setCombinerClass(AverageCombiner.class);
        job.setReducerClass(AverageReducer.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Aggregate.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);

        FileInputFormat.addInputPath(job, inputPath);
        FileOutputFormat.setOutputPath(job, outputPath);

        boolean success = job.waitForCompletion(true);
        if (!success) {
            throw new IllegalStateException("Job not completed");
        }
    }
}
