
package wc;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.util.*;
import java.io.BufferedReader;

import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


public class Twitterpair extends Configured implements Tool {
	private static final Logger logger = LogManager.getLogger(Twitterpair.class);

	// Declare the max value
	private final static int MAX_BUFFER = 200000;

	private enum EnumCounter {counter};

	public static class SetupMapper extends Mapper<Object, Text, Text, Text> {
		// Make the user connection or create  a hashmap of the input files.
		private Map<String, Set<String>> userconn = new HashMap<>();

		public void setup(final Context context)
				throws IOException, InterruptedException {

			BufferedReader bfr = null;

			try {

				URI[] cache = context.getCacheFiles();
				logger.info("Cache Files: " + Arrays.toString(cache));

				if (cache == null || cache.length == 0) {
					throw new RuntimeException("No file in cache");
				}

				for (URI file : cache) {

					String filename;
					int last = file.toString().lastIndexOf('/');
					if (last != -1) {
						filename = file.toString().substring(last + 1);
					} else {
						filename = file.toString();
					}

					bfr = new BufferedReader(new FileReader(filename));

					String user_arr;

					while ((user_arr = bfr.readLine()) != null) {
						String[] split = user_arr.split(",");
						if (split.length != 2) {
							return;
						}
						// Splitting the user  id and follower id
						String user_id = split[1];
						String foll_id = split[0];

						if (Integer.parseInt(foll_id) < MAX_BUFFER && Integer.parseInt(user_id) < MAX_BUFFER) {
							userconn.computeIfAbsent(user_id, u -> new HashSet<>()).add(foll_id);
						}
					}
				}
			}

			catch (Exception e) {
				logger.info("Error cache not created " + e.getMessage());
			}

			finally {
				if (bfr != null) {
					bfr.close();
				}
			}
		}


		public void map(final Object key, final Text value, final Context context)
				throws IOException, InterruptedException {

			String[] split = value.toString().split(",");


			String follower_id = split[0];
			String user_id = split[1];

			if (Integer.parseInt(follower_id) < MAX_BUFFER && Integer.parseInt(user_id) < MAX_BUFFER) {

				Set<String> conn = userconn.get(follower_id);

				if (CollectionUtils.isNotEmpty(conn)) {

					for (String con : conn) {
						Set<String> connectionsOfC = userconn.get(con);
						if (CollectionUtils.isNotEmpty(connectionsOfC) && connectionsOfC.contains(user_id)) {
							context.getCounter(EnumCounter.counter).increment(1);

						}
					}
				}

			}
		}
	}


	@Override
	public int run(String[] args) throws Exception {

		final Configuration conf = getConf();
		final Job job = Job.getInstance(conf, "Replicated Join");

		job.setJarByClass(Twitterpair.class);

		final Configuration jobConf = job.getConfiguration();
		jobConf.set("mapreduce.output.textoutputformat.separator", ",");

		job.setMapperClass(SetupMapper.class);
		job.setNumReduceTasks(0);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		job.addCacheFile(new URI(args[0] + "/edges.csv"));

		boolean isJobComplete = job.waitForCompletion(true);

		Counters counter = job.getCounters();
		Counter final_count = counter.findCounter(EnumCounter.counter);
		long count = final_count.getValue() / 3;
		logger.info("\n Number of Triangles:  "+ count);

		return isJobComplete ? 0 : 1;
	}


	public static void main(String[] args) {
		if (args.length != 2) {
			throw new IllegalArgumentException("Two arguments required:\n<input-dir> <output-dir>");
		}
		try {
			ToolRunner.run(new Twitterpair(), args);
		} catch (Exception e) {
			logger.error("", e);
		}
	}
}
