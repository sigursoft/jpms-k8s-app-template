package com.sigursoft.jpms.k8s.app;

import static org.junit.jupiter.api.Assertions.*;

import com.sigursoft.jpms.k8s.model.Sensor;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class SensorRepositoryTests {

    @Test
    public void testAddAndFind() {
        var repository = new SensorRepository();
        Sensor sensor = new Sensor("a", "x");
        assertEquals(SensorRepository.AddResult.ADDED, repository.add(sensor));
        assertEquals(Optional.of(sensor), repository.find("a"));
        assertEquals(Optional.empty(), repository.find("b"));
    }

    @Test
    public void testAddRejectsDuplicateId() {
        var repository = new SensorRepository();
        assertEquals(SensorRepository.AddResult.ADDED, repository.add(new Sensor("a", "x")));
        assertEquals(SensorRepository.AddResult.DUPLICATE, repository.add(new Sensor("a", "y")));
        assertEquals("x", repository.find("a").orElseThrow().description());
    }

    @Test
    public void testFindAllIsOrderedById() {
        var repository = new SensorRepository();
        repository.add(new Sensor("b", "y"));
        repository.add(new Sensor("a", "x"));
        assertEquals(List.of(new Sensor("a", "x"), new Sensor("b", "y")), repository.findAll());
    }

    @Test
    public void testAddIsRejectedWhenFull() {
        var repository = new SensorRepository(2);
        assertEquals(SensorRepository.AddResult.ADDED, repository.add(new Sensor("a", "x")));
        assertEquals(SensorRepository.AddResult.ADDED, repository.add(new Sensor("b", "x")));
        assertEquals(SensorRepository.AddResult.FULL, repository.add(new Sensor("c", "x")));
        // A duplicate is still reported as a duplicate, and does not consume capacity.
        assertEquals(SensorRepository.AddResult.DUPLICATE, repository.add(new Sensor("a", "y")));
        assertEquals(2, repository.findAll().size());
    }

    @Test
    public void testRejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new SensorRepository(0));
    }

    @Test
    public void testConcurrentAddsNeverExceedCapacity() throws Exception {
        var repository = new SensorRepository(50);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 500; i++) {
                int id = i;
                executor.submit(() -> repository.add(new Sensor("s" + id, "x")));
            }
        }
        assertEquals(50, repository.findAll().size());
    }
}
