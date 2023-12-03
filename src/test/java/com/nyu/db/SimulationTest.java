package com.nyu.db;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(OutputCaptureExtension.class)
public class SimulationTest {

    private Simulation sim;
    @BeforeEach
    void init() {
        sim = new Simulation();
    }

    @Test
    public void demoTest(CapturedOutput output) throws IOException {
        String inputFile = "src/test/resources/inputs/test1.txt";
        sim.run(inputFile);
        String expectedOut = Files.readString(Path.of("src/test/resources/outputs/test1.txt"));
        assertEquals(expectedOut, output.getOut());
    }
}