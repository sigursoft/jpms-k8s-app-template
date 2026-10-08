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
        assertTrue(repository.add(sensor));
        assertEquals(Optional.of(sensor), repository.find("a"));
        assertEquals(Optional.empty(), repository.find("b"));
    }

    @Test
    public void testAddRejectsDuplicateId() {
        var repository = new SensorRepository();
        assertTrue(repository.add(new Sensor("a", "x")));
        assertFalse(repository.add(new Sensor("a", "y")));
        assertEquals("x", repository.find("a").orElseThrow().description());
    }

    @Test
    public void testFindAllIsOrderedById() {
        var repository = new SensorRepository();
        repository.add(new Sensor("b", "y"));
        repository.add(new Sensor("a", "x"));
        assertEquals(List.of(new Sensor("a", "x"), new Sensor("b", "y")), repository.findAll());
    }
}
