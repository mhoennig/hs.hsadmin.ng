package net.hostsharing.hsadminng.system;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class SystemProcess {
    private final ProcessBuilder processBuilder;

    @Getter
    private String stdOut;
    @Getter
    private String stdErr;

    public SystemProcess(final String... command) {
        this.processBuilder = new ProcessBuilder(command);
    }


    public String getCommand() {
        return processBuilder.command().toString();
    }

    public int execute() throws IOException, InterruptedException {
        final var process = processBuilder.start();
        stdOut = fetchOutput(process.getInputStream()); // yeah, twisted ProcessBuilder API
        stdErr = fetchOutput(process.getErrorStream());
        return process.waitFor();
    }

    public int execute(final String input) throws IOException, InterruptedException {
        final var process = processBuilder.start();
        feedInput(input, process);
        stdOut = fetchOutput(process.getInputStream()); // yeah, twisted ProcessBuilder API
        stdErr = fetchOutput(process.getErrorStream());
        return process.waitFor();
    }

    private static void feedInput(final String input, final Process process) throws IOException {
        try (
                final OutputStreamWriter stdIn = new OutputStreamWriter(process.getOutputStream()); // yeah, twisted ProcessBuilder API
                final BufferedWriter writer = new BufferedWriter(stdIn)) {
            writer.write(input);
            writer.flush();
        }
    }

    private static String fetchOutput(final InputStream inputStream) throws IOException {
        final var output = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            for (String line; (line = reader.readLine()) != null; ) {
                output.append(line).append(System.lineSeparator());
            }
        }
        return output.toString();
    }
}
