package com.nyu.db;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SimulationTest {

    private Simulation sim;

    @BeforeEach
    void init() {

    }

    @Test
    public void demoTest() {
        String inputFile = "src/test/resources/inputs/test1.txt";
        sim.run(inputFile);
        String expectedOutputFile = "src/test/resources/outputs/test1.txt";
    }
}