package org.esgi.project.java.api.controllers;

import akka.http.javadsl.server.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.esgi.project.java.api.services.MovieService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static akka.http.javadsl.server.PathMatchers.integerSegment;

public class MovieController extends AllDirectives {

    private final MovieService svc;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public MovieController(MovieService svc){ this.svc = svc; }

    public Route createRoute() {
        return concat(
                pathPrefix("movies", () ->
                        path(integerSegment(), id ->
                                get(() ->
                                        onSuccess(CompletableFuture.supplyAsync(() -> svc.statsForMovie(id)), maybe ->
                                                maybe.map(this::jsonComplete)
                                                        .orElse(reject()))))),

                pathPrefix("stats", () -> concat(
                        path("ten/best/score",  () -> get(() -> jsonComplete(svc.topByScore(10,  true)))),
                        path("ten/best/views",  () -> get(() -> jsonComplete(svc.topByViews(10,  true)))),
                        path("ten/worst/score", () -> get(() -> jsonComplete(svc.topByScore(10, false)))),
                        path("ten/worst/views", () -> get(() -> jsonComplete(svc.topByViews(10, false))))
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
