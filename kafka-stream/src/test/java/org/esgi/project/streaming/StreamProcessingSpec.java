package org.esgi.project.streaming;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.state.*;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Windowed;
import org.esgi.project.java.streaming.StreamProcessing;
import org.esgi.project.java.streaming.models.*;
import org.esgi.project.java.streaming.serdes.JsonPOJOSerde;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class StreamProcessingTopologyTest {

    private TopologyTestDriver testDriver;
    private TestInputTopic<byte[], View> viewsTopic;
    private TestInputTopic<byte[], Like> likesTopic;

    private ReadOnlyKeyValueStore<Integer, MovieStats> allTimeStore;
    private ReadOnlyWindowStore<Integer, MovieStats> window5Store;
    private ReadOnlyKeyValueStore<Integer, MovieStats> scoreStore;

    @BeforeEach
    void setUp() {
        StreamProcessing streamProc = new StreamProcessing();
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "unit-test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        testDriver = new TopologyTestDriver(streamProc.buildTopology(), props);

        Serde<View> viewSerde = JsonPOJOSerde.of(View.class);
        Serde<Like> likeSerde = JsonPOJOSerde.of(Like.class);

        viewsTopic = testDriver.createInputTopic(
                StreamProcessing.VIEWS_TOPIC,
                Serdes.ByteArray().serializer(),
                viewSerde.serializer());

        likesTopic = testDriver.createInputTopic(
                StreamProcessing.LIKES_TOPIC,
                Serdes.ByteArray().serializer(),
                likeSerde.serializer());

        allTimeStore = testDriver.getKeyValueStore(StreamProcessing.MOVIE_VIEWS_ALLTIME_STORE);
        window5Store = testDriver.getWindowStore(StreamProcessing.MOVIE_VIEWS_5MIN_STORE);
        scoreStore   = testDriver.getKeyValueStore(StreamProcessing.MOVIE_SCORES_STORE);
    }

    @AfterEach
    void tearDown() {
        testDriver.close();
    }

    @Test
    void shouldAggregateViewsAllTime() {
        viewsTopic.pipeKeyValueList(List.of(
                new KeyValue<>(null, new View(1, "Dune", "start_only")),
                new KeyValue<>(null, new View(1, "Dune", "full"))));

        MovieStats stats = allTimeStore.get(1);
        assertNotNull(stats);
        assertEquals(2, stats.totalViews);
        assertEquals(1, stats.startOnly);
        assertEquals(1, stats.full);
    }

    @Test
    void shouldAggregateViewsInFiveMinuteWindow() {
        long t0 = Instant.parse("2025-06-26T12:00:00Z").toEpochMilli();
        long t1 = t0 + 30_000;
        long t2 = t0 + 6 * 60_000;

        viewsTopic.pipeInput(null, new View(2, "Alien", "full"), t0);
        viewsTopic.pipeInput(null, new View(2, "Alien", "full"), t1);
        viewsTopic.pipeInput(null, new View(2, "Alien", "half"), t2);

        try (WindowStoreIterator<MovieStats> it =
                     window5Store.fetch(2, Instant.ofEpochMilli(t0), Instant.ofEpochMilli(t1))) {
            int total = 0;
            while (it.hasNext()) total += it.next().value.totalViews;
            assertEquals(2, total);
        }
    }

    @Test
    void shouldComputeAverageScore() {
        likesTopic.pipeKeyValueList(List.of(
                kvLike(3, 8.0),
                kvLike(3, 6.0),
                kvLike(3,10.0)
        ));

        MovieStats scoreStats = scoreStore.get(3);
        assertNotNull(scoreStats);
        assertEquals(3, scoreStats.scoresCount);
        assertEquals(24.0 / 3, scoreStats.avgScore(), 0.0001);
    }

    private static KeyValue<byte[], Like> kvLike(int id, double score){
        Like l = new Like();
        l.id    = id;
        l.score = score;
        return new KeyValue<>(null, l);
    }
}
