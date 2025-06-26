package org.esgi.project.java.streaming;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.WindowStore;
import org.esgi.project.java.streaming.models.Like;
import org.esgi.project.java.streaming.models.MovieStats;
import org.esgi.project.java.streaming.models.View;
import org.esgi.project.java.streaming.serdes.JsonPOJOSerde;

import java.time.Duration;


public class StreamProcessing {

    public static final String VIEWS_TOPIC = "views";
    public static final String LIKES_TOPIC = "likes";

    public static final String MOVIE_VIEWS_5MIN_STORE = "movie-views-5min-store";
    public static final String MOVIE_SCORES_STORE     = "movie-scores-store";
    public static final String MOVIE_VIEWS_ALLTIME_STORE = "movie-views-alltime-store";

    private final StreamsBuilder builder = new StreamsBuilder();

    public StreamProcessing() {

        Serde<View>  viewEventSerde  = JsonPOJOSerde.of(View.class);
        Serde<Like>  likeEventSerde  = JsonPOJOSerde.of(Like.class);
        Serde<MovieStats> movieStatsSerde = JsonPOJOSerde.of(MovieStats.class);


        KStream<byte[], View> viewsRaw  =
                builder.stream(VIEWS_TOPIC, Consumed.with(Serdes.ByteArray(), viewEventSerde));

        KStream<byte[], Like> likesRaw  =
                builder.stream(LIKES_TOPIC, Consumed.with(Serdes.ByteArray(), likeEventSerde));

        KStream<Integer, View>  viewsStream =
                viewsRaw.selectKey((rawKey, view) -> view.id);

        KStream<Integer, Like>  likesStream =
                likesRaw.selectKey((rawKey, like) -> like.id);

        //agrégat ALL-TIME (compteur + distribution)
        viewsStream
                .groupByKey(Grouped.with(Serdes.Integer(), viewEventSerde))
                .aggregate(
                        MovieStats::new,
                        (id, view, agg) -> agg.incrementViews(view),          // total + histo
                        Materialized.<Integer, MovieStats, KeyValueStore<Bytes, byte[]>>
                                        as(MOVIE_VIEWS_ALLTIME_STORE)                   // <-- ici
                                .withKeySerde(Serdes.Integer())
                                .withValueSerde(movieStatsSerde)
                );

        //Agrégation 1 : nombre de vues par film (fenêtre 5 min)

        viewsStream
                .groupByKey(Grouped.with(Serdes.Integer(), viewEventSerde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
                .aggregate(
                        MovieStats::new,
                        (id, view, agg) -> agg.incrementViews(view),
                        Materialized.<Integer, MovieStats, WindowStore<Bytes, byte[]>>
                                        as(MOVIE_VIEWS_5MIN_STORE)
                                .withKeySerde(Serdes.Integer())
                                .withValueSerde(movieStatsSerde)
                );


        //Agrégation 2 : moyenne des scores par film (store distinct)

        likesStream
                .groupByKey(Grouped.with(Serdes.Integer(), likeEventSerde))
                .aggregate(
                        MovieStats::new,
                        (id, like, agg) -> agg.addScore(like.score),
                        Materialized.<Integer, MovieStats, KeyValueStore<Bytes, byte[]>>
                                        as(MOVIE_SCORES_STORE)
                                .withKeySerde(Serdes.Integer())
                                .withValueSerde(movieStatsSerde)
                );
    }

    public Topology buildTopology() {
        return builder.build();
    }
}
