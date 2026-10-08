package com.sigursoft.jpms.k8s.app;

import com.sigursoft.jpms.k8s.model.Sensor;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.exc.ValueInstantiationException;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.LogicalType;

import java.util.Collection;
import java.util.Map;

/**
 * Converts between JSON and the application model using Jackson databind.
 */
final class JsonCodec {

    private static final JsonMapper MAPPER = JsonMapper.builder(JsonFactory.builder()
                    .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                    .build())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            // Reject numbers and booleans where a string is expected instead of silently converting them
            .withCoercionConfig(LogicalType.Textual, config -> config
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
            .build();

    private JsonCodec() {
    }

    /**
     * Parses a sensor from a JSON object such as {@code {"id": "s-001", "description": "Temperature sensor"}}.
     * Unknown properties are ignored.
     *
     * @throws IllegalArgumentException if the input is not valid JSON or does not describe a valid sensor
     */
    static Sensor readSensor(byte[] json) {
        Sensor sensor;
        try {
            sensor = MAPPER.readValue(json, Sensor.class);
        } catch (StreamReadException e) {
            throw new IllegalArgumentException("Malformed JSON: " + e.getOriginalMessage(), e);
        } catch (ValueInstantiationException e) {
            // The Sensor constructor rejected the values; its message is safe to report to the client.
            throw new IllegalArgumentException(
                    e.getCause() instanceof IllegalArgumentException cause ? cause.getMessage() : EXPECTED_OBJECT, e);
        } catch (DatabindException e) {
            // Report the offending property without exposing internal type names to the client.
            String property = e.getPath().isEmpty() ? null : e.getPath().getFirst().getPropertyName();
            throw new IllegalArgumentException(property == null
                    ? EXPECTED_OBJECT
                    : "Property '" + property + "' must be a string", e);
        }
        if (sensor == null) {
            throw new IllegalArgumentException(EXPECTED_OBJECT);
        }
        return sensor;
    }

    static byte[] writeSensor(Sensor sensor) {
        return write(sensor);
    }

    static byte[] writeSensors(Collection<Sensor> sensors) {
        return write(sensors);
    }

    static byte[] writeMessage(String message) {
        return write(Map.of("message", message));
    }

    static byte[] writeError(String error) {
        return write(Map.of("error", error));
    }

    private static byte[] write(Object value) {
        return MAPPER.writeValueAsBytes(value);
    }

    private static final String EXPECTED_OBJECT = "Request body must be a single JSON object";
}
