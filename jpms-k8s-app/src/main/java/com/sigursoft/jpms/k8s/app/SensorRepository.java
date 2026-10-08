package com.sigursoft.jpms.k8s.app;

import com.sigursoft.jpms.k8s.model.Sensor;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe in-memory store of at most {@link #maxSize()} sensors, ordered by id.
 */
final class SensorRepository {

    static final int DEFAULT_MAX_SIZE = 10_000;

    enum AddResult { ADDED, DUPLICATE, FULL }

    private final ConcurrentMap<String, Sensor> sensors = new ConcurrentSkipListMap<>();
    private final AtomicInteger size = new AtomicInteger();
    private final int maxSize;

    SensorRepository() {
        this(DEFAULT_MAX_SIZE);
    }

    SensorRepository(int maxSize) {
        if (maxSize < 1) {
            throw new IllegalArgumentException("maxSize must be positive: " + maxSize);
        }
        this.maxSize = maxSize;
    }

    int maxSize() {
        return maxSize;
    }

    AddResult add(Sensor sensor) {
        if (sensors.containsKey(sensor.id())) {
            return AddResult.DUPLICATE;
        }
        // Reserve a slot first so concurrent adds can never exceed the limit.
        if (size.incrementAndGet() > maxSize) {
            size.decrementAndGet();
            return AddResult.FULL;
        }
        if (sensors.putIfAbsent(sensor.id(), sensor) != null) {
            size.decrementAndGet();
            return AddResult.DUPLICATE;
        }
        return AddResult.ADDED;
    }

    Optional<Sensor> find(String id) {
        return Optional.ofNullable(sensors.get(id));
    }

    List<Sensor> findAll() {
        return List.copyOf(sensors.values());
    }
}
