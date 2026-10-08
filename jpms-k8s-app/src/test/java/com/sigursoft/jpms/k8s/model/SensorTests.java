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

    @Test
    public void testSensorRejectsInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> new Sensor(null, "desc"));
        assertThrows(IllegalArgumentException.class, () -> new Sensor("", "desc"));
        assertThrows(IllegalArgumentException.class, () -> new Sensor("a/b", "desc"));
        assertThrows(IllegalArgumentException.class, () -> new Sensor("a".repeat(65), "desc"));
    }

    @Test
    public void testSensorAcceptsBoundaryIds() {
        assertEquals("a", new Sensor("a", "desc").id());
        assertEquals(64, new Sensor("a".repeat(64), "desc").id().length());
        assertEquals("A.b_c-1", new Sensor("A.b_c-1", "desc").id());
    }

    @Test
    public void testSensorRejectsMissingDescription() {
        assertThrows(IllegalArgumentException.class, () -> new Sensor("a", null));
        assertEquals("", new Sensor("a", "").description());
    }
}
