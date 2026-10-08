package com.sigursoft.jpms.k8s.app;

import com.sigursoft.jpms.k8s.model.Sensor;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Thread-safe in-memory store of sensors, ordered by id.
 */
final class SensorRepository {

    private final ConcurrentMap<String, Sensor> sensors = new ConcurrentSkipListMap<>();

    /**
     * @return {@code true} if the sensor was added, {@code false} if a sensor with the same id already exists
     */
    boolean add(Sensor sensor) {
        return sensors.putIfAbsent(sensor.id(), sensor) == null;
    }

    Optional<Sensor> find(String id) {
        return Optional.ofNullable(sensors.get(id));
    }

    List<Sensor> findAll() {
        return List.copyOf(sensors.values());
    }
}
