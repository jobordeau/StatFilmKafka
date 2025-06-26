package org.esgi.project.java.streaming.serdes;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;


public class JsonPOJOSerde<T> extends Serdes.WrapperSerde<T> {

    public JsonPOJOSerde(Class<T> targetClass) {
        super(new JsonPOJOSerializer<>(), new JsonPOJODeserializer<>(targetClass));
    }

    public static <T> Serde<T> of(Class<T> clazz) {
        return new JsonPOJOSerde<>(clazz);
    }
}
