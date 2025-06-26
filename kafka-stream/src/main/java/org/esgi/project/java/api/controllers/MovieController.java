package org.esgi.project.java.api.controllers;

import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;

import static akka.http.javadsl.server.PathMatchers.integerSegment;

public class MovieController extends AllDirectives {

    public Route createRoute() {
        return pathPrefix("movies", () ->
                path(integerSegment(), movieId ->
                        get(() -> complete("Details du film avec ID : " + movieId))
                )
        );
    }
}
