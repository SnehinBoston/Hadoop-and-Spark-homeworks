package wc;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Arrays;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
//import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

// This is the main class.
public class FollowerCount extends Configured implements Tool {
	private static final Logger logger = LogManager.getLogger(FollowerCount.class);

	public static class TokenizerMapper extends Mapper<Object, Text, Text, Text> {
		//private final static IntWritable one = new IntWritable(1);
		//rivate final String DELIMIT = ",";
		private final static int MAX_BUFFER = 100000;
		@Override
		public void map(final Object key, final Text value, final Context context) throws IOException, InterruptedException {
			//System.out.println(value);
			//final StringTokenizer itr = new StringTokenizer(value.toString(),DELIMIT);
			String[] followers_Users = null;
			followers_Users = value.toString().split(",");
			if(Integer.parseInt(followers_Users[0])<= MAX_BUFFER && Integer.parseInt(followers_Users[1]) <= MAX_BUFFER){
				context.write(new Text(followers_Users[0]),new Text("follower:"+ followers_Users[1]));
				context.write(new Text(followers_Users[1]),new Text("user:"+ followers_Users[0]));
			}
		}
	}
	// Job2
	public static class TokenizerMapper_ZX extends Mapper<Object,Text,Text,Text>{

		public void map(final Object key, final Text value, final Context context) throws IOException, InterruptedException{
			String row_output = value.toString();
			String[] row = row_output.split(",");
			context.write(new Text(row[1]), new Text("Reducer:" + row[0]));
		}
	}
	public static class TokenizerMapper_input extends Mapper<Object, Text, Text, Text>{
		public void map(final Object key, final Text value, final Context context) throws IOException, InterruptedException{
			String in_row = value.toString();
			String[] row_kv = in_row.split(",");
			context.write(new Text(row_kv[0]),new Text("Original:" + row_kv[1]));
		}
	}
	// This is the reducer class(inherited from 'Reducer' class).
	public static class PairReducer extends Reducer<Text, Text, Text, Text> {
		//@Override
		//private List<String> follower = new ArrayList<>();
		//private List<String> user = new ArrayList<>();
		public void reduce(final Text key, final Iterable<Text> values, final Context context) throws IOException, InterruptedException {
			List<String> follower = new ArrayList<>();
			List<String> user = new ArrayList<>();
			//String val_str = new String();
			String foll = new String();
			String use = new String();
			for (final Text val : values) {
				if (val.toString().contains("follower")) {
					follower.add(val.toString().split(":")[1]);
				} else if (val.toString().contains("user")) {
					user.add(val.toString().split(":")[1]);
				}
				//context.write(key,val);
			}
			for (int i = 0; i < follower.size(); i++) {
				for (int j = 0; j < user.size(); j++) {
					foll = follower.get(i);
					use = user.get(j);
					context.write(new Text(use), new Text(foll));
				}
			}
		}
	}
	public static class TriangleReducer extends Reducer<Text, Text, Text, Text> {
		private static int sum = 0;
		private enum EnumCounter{triangle_counter};
		//private List<Integer> original = new ArrayList<Integer>();
		//private List<Integer> reduce = new ArrayList<Integer>();
		public void reduce(final Text key, final Iterable<Text> values, final Context context) throws IOException, InterruptedException{
			List<Integer> original = new ArrayList<Integer>();
			List<Integer> reduce = new ArrayList<Integer>();
			for (final Text val : values) {
				String[] parts = val.toString().split(":");
				if(parts[0].equals("Original")) {
					original.add(Integer.parseInt(parts[1]));
				}
				else if(parts[0].equals("Reducer")) {
					reduce.add(Integer.parseInt(parts[1]));
				}
			}
			//context.write(new Text(Integer.toString(original.size())),new Text(Integer.toString(sum)));
			if(original.size()>0 && reduce.size()>0){
				for (int orig : original) {
					for (int red : reduce) {
						if (orig == red) {
							context.getCounter(EnumCounter.triangle_counter).increment(1);
							break;
						}
					}
				}
			}
			long sum = context.getCounter(EnumCounter.triangle_counter).getValue()/3;
			context.write(new Text("Number of triangle pairs: "),new Text(Long.toString(sum)));
			//context.write(new Text(Integer.toString(sum)),new Text(Integer.toString(sum)));
		}
	}

	@Override
	public int run(final String[] args) throws Exception {
		final Configuration conf = getConf();
		final Job job = Job.getInstance(conf, "Pair Count");
		job.setJarByClass(FollowerCount.class);
		final Configuration jobConf = job.getConfiguration();
		jobConf.set("mapreduce.output.textoutputformat.separator", ",");
		job.setMapperClass(TokenizerMapper.class);
		job.setReducerClass(PairReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1],"out1"));

		boolean job1_complete = job.waitForCompletion(true);
		// New job
		if(job1_complete) {

			final Job job2 = Job.getInstance(conf, "Mapper-Reducer 2nd phase");
			job2.setJarByClass(FollowerCount.class);
            final Configuration jobConf2 = job2.getConfiguration();
            jobConf2.set("mapreduce.output.textoutputformat.separator", "");
			//String input_path = "/Users/snehgurdasani/Documents/CS4240_Large_Scale_Data_Processing/Homeworks/course.ccs.neu.edu/cs6240/parent/hw2/input/";
			//final String output_path = "/Users/snehgurdasani/Documents/CS4240_Large_Scale_Data_Processing/Homeworks/course.ccs.neu.edu/cs6240/parent/hw2/final_output/";
			MultipleInputs.addInputPath(job2, new Path(args[0]),TextInputFormat.class,TokenizerMapper_input.class);
			MultipleInputs.addInputPath(job2, new Path(args[1],"out1"),TextInputFormat.class, TokenizerMapper_ZX.class);
			job2.setReducerClass(TriangleReducer.class);
			job2.setOutputKeyClass(Text.class);
			job2.setOutputValueClass(Text.class);

			//FileInputFormat.setInputPaths(job2, new Path(args[1]));
			//FileOutputFormat.setOutputPath(conf, new Path(args[1]));
			FileOutputFormat.setOutputPath(job2, new Path(args[1],"out2"));
			return job2.waitForCompletion(true) ? 0 : 1;
		}
		return 0;
	}

	public static void main(final String[] args) {
		if (args.length != 2) {
			throw new Error("Two arguments required:\n<input-dir> <output-dir>");
		}

		try {
			ToolRunner.run(new FollowerCount(), args);
		} catch (final Exception e) {
			logger.error("", e);
		}
	}

}