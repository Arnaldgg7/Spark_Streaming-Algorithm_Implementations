package exercise_2;

import org.apache.spark.api.java.Optional;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import scala.Tuple2;
import twitter4j.Status;

import java.util.Arrays;
import java.util.List;

public class Exercise_2 {

    static final float C = (float) 1E-7;
    static final float F = (float) 0.5;

    public static void decayingWindow(JavaDStream<Status> statuses) {
        JavaDStream<String> words = statuses
                .flatMap(t -> Arrays.asList(t.getText().split("\\s+")).iterator());

        JavaDStream<String> hashTags = words
                .filter(t -> t.startsWith("#"));

        // There is no need to apply the 'reduceByKey()' after the 'mapToPair()', as in the previous exercise,
        // since the 'updateStateByKey()' already gets a list of the values per key in the micro-batch, so
        // the reduce function is implicit when calling this function.
        JavaPairDStream<String, Float> tuples = hashTags.
                mapToPair(t -> new Tuple2<>(t, 1F));

        Function2<List<Float>, Optional<Float>, Optional<Float>> updateFunction =
                (values, state) -> {
                    float newSum = state.or(0F);
                    newSum *= (1. - C);
                    // We add up as many '1' as values we have in the list, instead of looping through
                    // the list, which is less efficient, since we know that it will be always made of
                    // '1' values:
                    newSum += 1.*values.size();
                    if(newSum<F){
                        // Spark removes the states with 'null' values, which is what is
                        //yielded by 'Optional.absent()':
                        return Optional.absent();
                    }
                    else {
                        return Optional.of(newSum);
                    }
                };

        JavaPairDStream<Float, String> counts = tuples.updateStateByKey(updateFunction)
                .mapToPair(Tuple2::swap)
                .transformToPair(t -> t.sortByKey(false));

        counts.foreachRDD(t -> {
            // We are getting the top 10 hashtags from the whole stream (not just for the current RDD),
            // because the 'updateStateByKey()' returns the whole state we have in Memory, once it has
            // been updated with the current call to the 'updateStateByKey()'. This is what 't' represents:
            System.out.println("Top 10 hashtags:");
            t.take(10).forEach(System.out::println);
            System.out.println();
            System.out.println("Median hashtag:");
            int median_position = Math.round(t.count()/2F);
            // We only take half of the existing tweets, since we only need the last one of such a half to get
            // the Median, and we only do that when we already have any State of a hashtag in Memory:
            if (median_position > 0) {
                String median_hashtag = t.take(median_position).get(median_position-1)._2;
                System.out.println(median_hashtag);
            }
        });
        /*
        COMMENT ABOUT THE MEDIAN:
        There is a difference between the Median for decaying windows and the non-decaying windows methodology.

        First, decaying windows erase those hashtags with values that are below a threshold, so the whole data
        stream is not maintained. Therefore, what we are getting is not the 'actual median' of the whole stream
        of data we have seen so far, but the median for those values above such a defined threshold.

        On the other hand, non-decaying methods keep all stream data seen so far in memory. Hence, the median
        we obtain is, indeed, the actual median of all hashtags from treated tweets seen so far.

        Consequently, it doesn't have much sense to compute the median when we are erasing some hashtags
        because of some conditional proviso (such as the resulting value, once the penalty term is applied
        in each micro-batch or 't' time, being lower or higher than the defined threshold). At most, it ought
        to be called 'median of hashtags above the threshold', but not the overall median.
        */
    }
}