package org.esgi.project.java.streaming.serdes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;


public class JsonPOJOSerializer<T> implements Serializer<T> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        /* rien à configurer */
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) return null;
        try {
            return MAPPER.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException(
                    "Erreur de sérialisation du message pour le topic " + topic, e);
        }
    }

    @Override
    public void close() {
        /* rien à fermer */
    }
}
