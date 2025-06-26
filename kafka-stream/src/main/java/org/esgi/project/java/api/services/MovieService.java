package org.esgi.project.java.api.services;

import org.apache.kafka.streams.*;
import org.apache.kafka.streams.state.*;
import org.esgi.project.java.streaming.StreamProcessing;
import org.esgi.project.java.streaming.models.MovieStats;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MovieService {

    private final KafkaStreams streams;

    public MovieService(KafkaStreams streams) {
        this.streams = streams;
    }

    /* ──────────────────────────────────────────────────────────── */
    /*  /movies/:id                                                */
    /* ──────────────────────────────────────────────────────────── */

    public Optional<Map<String, Object>> statsForMovie(int id) {

        ReadOnlyKeyValueStore<Integer, MovieStats> allTime =
                streams.store(StoreQueryParameters.fromNameAndType(
                        StreamProcessing.MOVIE_VIEWS_ALLTIME_STORE,
                        QueryableStoreTypes.keyValueStore()));

        MovieStats past = allTime.get(id);
        if (past == null) return Optional.empty();

        // fenêtre glissante 5 minutes
        ReadOnlyWindowStore<Integer, MovieStats> last5Store =
                streams.store(StoreQueryParameters.fromNameAndType(
                        StreamProcessing.MOVIE_VIEWS_5MIN_STORE,
                        QueryableStoreTypes.windowStore()));

        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofMinutes(5));

        MovieStats last5 = new MovieStats();
        try (WindowStoreIterator<MovieStats> it = last5Store.fetch(id, from, now)) {
            while (it.hasNext()) last5.merge(it.next().value);
        }

        return Optional.of(Map.of(
                "id", id,
                "title", past.title,
                "total_view_count", past.totalViews,
                "stats", Map.of(
                        "past", Map.of(
                                "start_only", past.startOnly,
                                "half", past.half,
                                "full", past.full),
                        "last_five_minutes", Map.of(
                                "start_only", last5.startOnly,
                                "half", last5.half,
                                "full", last5.full)
                )));
    }

    /* ──────────────────────────────────────────────────────────── */
    /*  /stats/ten/best|worst/score|views                          */
    /* ──────────────────────────────────────────────────────────── */

    public List<Map<String, Object>> topByScore(int limit, boolean best) {
        ReadOnlyKeyValueStore<Integer, MovieStats> scores =
                streams.store(StoreQueryParameters.fromNameAndType(
                        StreamProcessing.MOVIE_SCORES_STORE,
                        QueryableStoreTypes.keyValueStore()));

        Comparator<MovieStats> cmp = Comparator.comparingDouble(MovieStats::avgScore);
        if (best) cmp = cmp.reversed();

        return toList(scores.all(), cmp, limit,
                (id, s) -> Map.of(
                        "id", id,
                        "title", titleOrUnknown(id, s),
                        "score", round(s.avgScore())));
    }

    public List<Map<String, Object>> topByViews(int limit, boolean best) {
        ReadOnlyKeyValueStore<Integer, MovieStats> views =
                streams.store(StoreQueryParameters.fromNameAndType(
                        StreamProcessing.MOVIE_VIEWS_ALLTIME_STORE,
                        QueryableStoreTypes.keyValueStore()));

        Comparator<MovieStats> cmp = Comparator.comparingInt(ms -> ms.totalViews);
        if (best) cmp = cmp.reversed();

        return toList(views.all(), cmp, limit,
                (id, s) -> Map.of(
                        "id", id,
                        "title", titleOrUnknown(id, s),
                        "views", s.totalViews));
    }

    /* ──────────────────────────────────────────────────────────── */
    /*  Helpers                                                    */
    /* ──────────────────────────────────────────────────────────── */

    /** Renvoie un titre non-nul. */
    private String titleOrUnknown(int id, MovieStats stats) {
        if (stats.title != null) return stats.title;

        // Essaie de le récupérer dans le store des vues
        ReadOnlyKeyValueStore<Integer, MovieStats> views =
                streams.store(StoreQueryParameters.fromNameAndType(
                        StreamProcessing.MOVIE_VIEWS_ALLTIME_STORE,
                        QueryableStoreTypes.keyValueStore()));

        MovieStats v = views.get(id);
        return (v != null && v.title != null) ? v.title : "unknown";
    }

    /** Transforme l’iterator Kafka en liste triée puis mappée. */
    private <V> List<Map<String, Object>> toList(KeyValueIterator<Integer, V> it,
                                                 Comparator<V> cmp,
                                                 int limit,
                                                 BiFunction<Integer, V, Map<String, Object>> mapper) {
        Iterable<KeyValue<Integer, V>> iterable = () -> it;

        List<Map<String, Object>> res = StreamSupport.stream(iterable.spliterator(), false)
                .sorted((kv1, kv2) -> cmp.compare(kv1.value, kv2.value))
                .limit(limit)
                .map(kv -> mapper.apply(kv.key, kv.value))
                .collect(Collectors.toList());      // Compatible Java 11

        it.close();
        return res;
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
