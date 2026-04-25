package com.vitasync.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatientPriorityTest {

    @Test
    void lowerScoreComesFirst() {
        PatientPriority critical = new PatientPriority("P001", 0.3);
        PatientPriority stable   = new PatientPriority("P002", 0.9);
        assertTrue(critical.compareTo(stable) < 0);
    }

    @Test
    void higherScoreComesLast() {
        PatientPriority critical = new PatientPriority("P001", 0.3);
        PatientPriority stable   = new PatientPriority("P002", 0.9);
        assertTrue(stable.compareTo(critical) > 0);
    }

    @Test
    void sortingOrderIsCorrect() {
        List<PatientPriority> list = new ArrayList<>();
        list.add(new PatientPriority("P003", 0.8));
        list.add(new PatientPriority("P001", 0.2));
        list.add(new PatientPriority("P002", 0.5));
        Collections.sort(list);
        assertEquals("P001", list.get(0).getPatientId());
        assertEquals("P002", list.get(1).getPatientId());
        assertEquals("P003", list.get(2).getPatientId());
    }

    @Test
    void equalityBasedOnPatientId() {
        PatientPriority a = new PatientPriority("P001", 0.3);
        PatientPriority b = new PatientPriority("P001", 0.9);
        assertEquals(a, b);
    }
}
