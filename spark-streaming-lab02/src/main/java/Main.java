import exercise_1.Exercise_1;
import exercise_2.Exercise_2;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.twitter.TwitterUtils;


import com.google.common.io.Files;

import twitter4j.Status;


public class Main {

	static String TWITTER_CONFIG_PATH = "C:\\Users\\Arnald\\Desktop\\ARNALD\\KNOWLEDGE\\PROJECTE MASTER\\MASTER\\Hands-On Experience\\Spark Streaming\\spark-streaming-lab02\\src\\main\\resources\\twitter_configuration.txt";
	static String HADOOP_COMMON_PATH = "C:\\Users\\Arnald\\Desktop\\ARNALD\\KNOWLEDGE\\PROJECTE MASTER\\MASTER\\Hands-On Experience\\Spark Streaming\\spark-streaming-lab02\\src\\main\\resources\\winutils";


	public static void main(String[] args) throws Exception {
	    if (args.length != 1) {
            throw new Exception("Exercise number required");
        }

		System.setProperty("hadoop.home.dir", HADOOP_COMMON_PATH);

		SparkConf conf = new SparkConf().setAppName("Main").setMaster("local[*]");
		JavaSparkContext ctx = new JavaSparkContext(conf);
		JavaStreamingContext jsc = new JavaStreamingContext(ctx, new Duration(5000));
		LogManager.getRootLogger().setLevel(Level.ERROR);
		
		jsc.checkpoint(Files.createTempDir().getAbsolutePath());
		
		Utils.setupTwitter(TWITTER_CONFIG_PATH);
		
		JavaDStream<Status> tweets = TwitterUtils.createStream(jsc);

		if (args[0].equals("exercise1")) {
            Exercise_1.hashtagAccumulator(tweets);
        }
		else if (args[0].equals("exercise2")) {
            Exercise_2.decayingWindow(tweets);
        } else {
		    throw new Exception("Wrong exercise number (exercise1, exercise2)");
        }

		jsc.start();
		jsc.awaitTermination();
	}

}
