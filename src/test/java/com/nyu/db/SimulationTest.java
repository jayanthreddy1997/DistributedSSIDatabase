//package com.nyu.db;
//
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.MethodSource;
//import org.springframework.boot.test.system.CapturedOutput;
//import org.springframework.boot.test.system.OutputCaptureExtension;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.stream.IntStream;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//@ExtendWith(OutputCaptureExtension.class)
//public class SimulationTest {
//
//    private Simulation sim;
//    @BeforeEach
//    void init() {
//        this.sim = new Simulation();
//    }
//
//    @ParameterizedTest
//    @MethodSource("getTestNumbers")
//    public void runEachTest(int testNum, CapturedOutput output) throws IOException {
//        String inputFile = String.format("src/test/resources/inputs/test%d.txt", testNum);
//        sim.run(inputFile);
//        String expectedOut = Files.readString(Path.of(String.format("src/test/resources/outputs/test%d.txt", testNum)));
//        assertEquals(expectedOut, output.getOut());
//    }
//
//    public static IntStream getTestNumbers() {
//        File folder = new File("src/test/resources/inputs");
//        File[] listOfFiles = folder.listFiles();
//        int numTests = listOfFiles.length;
//
//        return IntStream.rangeClosed(1, numTests);
//    }
//}