package org.esgi.project.java.api;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.server.Route;
import akka.http.javadsl.server.AllDirectives;
import org.apache.kafka.streams.KafkaStreams;
import org.esgi.project.java.api.controllers.MovieController;
import org.esgi.project.java.api.services.MovieService;

public class ApiServer {
    private final KafkaStreams streams;
    public ApiServer(KafkaStreams streams){ this.streams = streams; }

    public void start(){
        ActorSystem system = ActorSystem.create("routes");
        Http http = Http.get(system);

        MovieService service = new MovieService(streams);
        MovieController ctrl = new MovieController(service);

        http.newServerAt("0.0.0.0", 8080).bind(ctrl.createRoute());
    }
}

