package com.sparktf;

import org.apache.commons.cli.Options;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationTest {

    @Test
    void getOptions_hasFOption() {
        Options options = Application.getOptions();
        assertTrue(options.hasOption("f"));
        assertTrue(options.hasOption("file"));
    }

    @Test
    void parseCli_withFileOption_returnsCmdInput() {
        Options options = Application.getOptions();
        Application.CmdInput input = Application.parseCli(options, new String[]{"-f", "my-pipeline.yaml"});
        assertEquals("my-pipeline.yaml", input.getYamlFile());
    }

    @Test
    void parseCli_missingRequiredFile_throwsRuntimeException() {
        Options options = Application.getOptions();
        assertThrows(RuntimeException.class, () -> Application.parseCli(options, new String[]{}));
    }

    @Test
    void cmdInput_gettersWork() {
        Application.CmdInput input = new Application.CmdInput("path/to/file.yaml");
        assertEquals("path/to/file.yaml", input.getYamlFile());
    }

    @Test
    void run_executesPipelineFromYaml(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("input.csv");
        Files.writeString(csvFile, "id,name\n1,alice\n2,bob\n");

        String yaml = String.format(
                "---\nname: app-test-pipeline\nactions:\n" +
                "  - name: load\n    kind: source\n    type: generic\n" +
                "    format: csv\n    options:\n      header: \"true\"\n      path: \"%s\"\n" +
                "  - name: out\n    kind: sink\n    type: show\n    input: load\n",
                csvFile.toAbsolutePath().toString().replace("\\", "/"));

        Path yamlFile = tempDir.resolve("pipeline.yaml");
        Files.writeString(yamlFile, yaml);

        Application app = new Application(
                new Application.CmdInput(yamlFile.toString()),
                Utils.getSpark());
        assertDoesNotThrow(app::run);
    }
}
