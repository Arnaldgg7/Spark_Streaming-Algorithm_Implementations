package exercise_1;

import org.apache.spark.streaming.State;
import org.apache.spark.streaming.StateSpec;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.api.java.function.*;
import org.apache.spark.streaming.api.java.JavaMapWithStateDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import scala.Tuple2;
import twitter4j.Status;
import org.apache.spark.api.java.Optional;
import java.util.Arrays;

@SuppressWarnings("all")
public class Exercise_1 {

    public static void hashtagAccumulator(JavaDStream<Status> statuses) {
        JavaDStream<String> words = statuses
                .flatMap(t -> Arrays.asList(t.getText().split("\\s+")).iterator());

        JavaDStream<String> hashTags = words
                .filter(t -> t.startsWith("#"));

        JavaPairDStream<String, Integer> tuples = hashTags.
                mapToPair(t -> new Tuple2<>(t, 1))
                .reduceByKey((t1, t2) -> t1+t2);

        Function3<String, Optional<Integer>, State<Integer>, Tuple2<String, Integer>> mappingFunction =
                (word, count, state) -> {
                    int new_count = (state.exists() ? state.get() : 0) + count.orElse(0);
                    Tuple2<String, Integer> output = new Tuple2<>(word, new_count);
                    state.update(new_count);
                    return output;
                };

        JavaMapWithStateDStream<String, Integer, Integer, Tuple2<String, Integer>> counts =
                tuples.mapWithState(StateSpec.function(mappingFunction));

        // Creating the snapshot of the current state to be used afterwards in the output:
        JavaPairDStream<Integer, String> current_state = counts.stateSnapshots()
                .mapToPair(Tuple2::swap)
                .transformToPair(t -> t.sortByKey(false));

        current_state.foreachRDD(t -> {
            // We are getting the top 10 hashtags from the whole stream (not just for the current RDD)
            // because of the call to 'stateSnapshots()', which we have already updated with the current
            // RDD in the 'mapWithState()' function:
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
    }
}