package org.esgi.project.java.streaming.serdes;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

/** Désérialise un JSON (byte[]) vers l’objet Java cible. */
public class JsonPOJODeserializer<T> implements Deserializer<T> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Class<T> targetClass;

    public JsonPOJODeserializer(Class<T> targetClass) {
        this.targetClass = targetClass;
    }

    @Override public void configure(Map<String, ?> configs, boolean isKey) { }

    @Override public T deserialize(String topic, byte[] bytes) {
        if (bytes == null) return null;
        try {
            return MAPPER.readValue(bytes, targetClass);
        } catch (Exception e) {
            throw new SerializationException(
                    "Erreur de désérialisation du message pour le topic " + topic, e);
        }
    }

    @Override public void close() { }
}
