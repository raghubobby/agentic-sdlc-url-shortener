package com.agentic.sdlc.controlplane.tool;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class ProcessExecutor {
    private static final int MAX_CAPTURED_CHARS = 120_000;

    public ToolResult run(Path workingDirectory, List<String> command, Duration timeout) {
        Path outputFile = null;
        try {
            outputFile = Files.createTempFile("agentic-tool-", ".log");
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(workingDirectory.toFile());
            builder.redirectErrorStream(true);
            builder.redirectOutput(outputFile.toFile());
            Process process = builder.start();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                String output = readOutput(outputFile);
                return ToolResult.failure(124, "Command timed out after " + timeout.toSeconds() + "s", output);
            }
            String output = readOutput(outputFile);
            int exit = process.exitValue();
            return exit == 0
                    ? ToolResult.success("Command completed successfully", output)
                    : ToolResult.failure(exit, "Command failed with exit code " + exit, output);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Tool process interrupted", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not start tool process: " + command.getFirst(), ex);
        } finally {
            if (outputFile != null) {
                try { Files.deleteIfExists(outputFile); } catch (IOException ignored) { }
            }
        }
    }

    private String readOutput(Path outputFile) throws IOException {
        String output = Files.readString(outputFile, StandardCharsets.UTF_8);
        if (output.length() > MAX_CAPTURED_CHARS) {
            int half = MAX_CAPTURED_CHARS / 2;

            output =
                    output.substring(0, half)
                            + "\n\n...[middle of output truncated]...\n\n"
                            + output.substring(output.length() - half);
        }
        return output;
    }
}
