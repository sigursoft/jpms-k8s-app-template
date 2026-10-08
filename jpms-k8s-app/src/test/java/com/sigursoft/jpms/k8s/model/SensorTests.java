package com.sigursoft.jpms.k8s.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SensorTests {

    @Test
    public void testSensorRecord() {
        Sensor sensor = new Sensor("s-001", "Temperature sensor");
        assertEquals("s-001", sensor.id());
        assertEquals("Temperature sensor", sensor.description());
    }

    @Test
    public void testSensorEquality() {
        Sensor a = new Sensor("id1", "desc");
        Sensor b = new Sensor("id1", "desc");
        assertEquals(a, b);
    }

    @Test
    public void testSensorInequality() {
        Sensor a = new Sensor("id1", "desc");
        Sensor b = new Sensor("id2", "desc");
        assertNotEquals(a, b);
    }

    @Test
    public void testSensorHashCode() {
        Sensor a = new Sensor("id1", "desc");
        Sensor b = new Sensor("id1", "desc");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testSensorToString() {
        Sensor sensor = new Sensor("id1", "desc");
        String str = sensor.toString();
        assertTrue(str.contains("id1"));
        assertTrue(str.contains("desc"));
    }
}
