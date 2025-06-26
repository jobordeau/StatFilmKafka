package org.esgi.project.java;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.esgi.project.java.api.ApiServer;
import org.esgi.project.java.streaming.StreamProcessing;

import java.util.Properties;

public class Main {
    private static final String applicationName = "stream-processing";

    public static void main(String[] args) {
        StreamProcessing streamProcessing = new StreamProcessing();
        KafkaStreams streams = new KafkaStreams(streamProcessing.buildTopology(), buildProperties());

        streams.start();

        ApiServer apiServer = new ApiServer();
        apiServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }


    public static Properties buildProperties() {
        Properties properties = new Properties();
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(StreamsConfig.CLIENT_ID_CONFIG, applicationName);
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationName);
        properties.put(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, "0");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        properties.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, "-1");
        properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1");
        return properties;
    }
}
