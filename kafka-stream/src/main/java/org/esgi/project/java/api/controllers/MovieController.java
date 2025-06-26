package org.esgi.project.java.api.controllers;

import akka.http.javadsl.server.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.esgi.project.java.api.services.MovieService;
import static akka.http.javadsl.server.PathMatchers.segment;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static akka.http.javadsl.server.PathMatchers.integerSegment;

public class MovieController extends AllDirectives {

    private final MovieService svc;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final PathMatcher0 TEN_BEST_SCORE =
            segment("ten").slash(segment("best")).slash(segment("score"));
    private static final PathMatcher0 TEN_BEST_VIEWS =
            segment("ten").slash(segment("best")).slash(segment("views"));
    private static final PathMatcher0 TEN_WORST_SCORE =
            segment("ten").slash(segment("worst")).slash(segment("score"));
    private static final PathMatcher0 TEN_WORST_VIEWS =
            segment("ten").slash(segment("worst")).slash(segment("views"));

    public MovieController(MovieService svc){ this.svc = svc; }

    public Route createRoute() {
        return concat(
                /* /movies/:id --------------------------------------------------- */
                pathPrefix("movies", () ->
                        path(integerSegment(), id ->
                                get(() ->
                                        onSuccess(
                                                CompletableFuture.supplyAsync(() -> svc.statsForMovie(id)),
                                                m -> m.map(this::jsonComplete).orElse(reject())

                                        )
                                )
                        )
                ),

                /* /stats/… ------------------------------------------------------ */
                pathPrefix("stats", () -> concat(
                        path(TEN_BEST_SCORE,  () -> get(() -> jsonComplete(svc.topByScore(10,  true)))),
                        path(TEN_BEST_VIEWS,  () -> get(() -> jsonComplete(svc.topByViews(10,  true)))),
                        path(TEN_WORST_SCORE, () -> get(() -> jsonComplete(svc.topByScore(10, false)))),
                        path(TEN_WORST_VIEWS, () -> get(() -> jsonComplete(svc.topByViews(10, false))))
                ))
        );
    }

    private Route jsonComplete(Object o){
        try {
            return complete(MAPPER.writeValueAsString(o));
        } catch (Exception e){
            return failWith(e);
        }
    }
}
