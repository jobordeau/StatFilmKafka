package org.esgi.project.java.api;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.server.AllDirectives;
import org.esgi.project.java.api.controllers.MovieController;

public class ApiServer extends AllDirectives {

    public void start() {
        ActorSystem system = ActorSystem.create("routes");
        final Http http = Http.get(system);

        MovieController movieController = new MovieController();

        Route routes = movieController.createRoute();
        http.newServerAt("localhost", 8080).bind(routes);
    }
}
