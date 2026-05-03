package com.sparktf;

import com.sparktf.core.TransformationData;
import com.sparktf.core.TransformationFlow;
import com.sparktf.exception.ValidationException;
import com.sparktf.vo.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TransformationFlowTest {

    @Test
    void getRoot_parsesValidYaml(@TempDir Path tempDir) throws IOException {
        Path yaml = writePipelineYaml(tempDir, "show-pipeline", "");
        Root root = new TransformationFlow(yaml.toString(), Utils.getSpark()).getRoot(yaml.toString());
        assertEquals("show-pipeline", root.getName());
    }

    @Test
    void getRoot_invalidPath_throwsRuntimeException() {
        TransformationFlow flow = new TransformationFlow("nonexistent.yaml", Utils.getSpark());
        assertThrows(RuntimeException.class, () -> flow.getRoot("nonexistent.yaml"));
    }

    @Test
    void validate_validRoot_doesNotThrow(@TempDir Path tempDir) throws Exception {
        Path yaml = writePipelineYaml(tempDir, "valid-pipeline", "");
        TransformationFlow flow = new TransformationFlow(yaml.toString(), Utils.getSpark());
        Root root = flow.getRoot(yaml.toString());
        TransformationData data = new TransformationData();
        data.setSparkSession(Utils.getSpark());
        assertDoesNotThrow(() -> flow.validate(root, data));
    }

    @Test
    void validate_nullName_throwsValidationException(@TempDir Path tempDir) throws IOException {
        String rawYaml = "---\nname: ~\nactions: []\n";
        Path yaml = tempDir.resolve("invalid.yaml");
        Files.writeString(yaml, rawYaml);
        TransformationFlow flow = new TransformationFlow(yaml.toString(), Utils.getSpark());
        Root root = flow.getRoot(yaml.toString());
        TransformationData data = new TransformationData();
        data.setSparkSession(Utils.getSpark());
        assertThrows(ValidationException.class, () -> flow.validate(root, data));
    }

    @Test
    void validate_nameTooShort_throwsValidationException(@TempDir Path tempDir) throws IOException {
        String rawYaml = "---\nname: \"ab\"\nactions: []\n";
        Path yaml = tempDir.resolve("short.yaml");
        Files.writeString(yaml, rawYaml);
        TransformationFlow flow = new TransformationFlow(yaml.toString(), Utils.getSpark());
        Root root = flow.getRoot(yaml.toString());
        TransformationData data = new TransformationData();
        data.setSparkSession(Utils.getSpark());
        assertThrows(ValidationException.class, () -> flow.validate(root, data));
    }

    @Test
    void validate_nameTooLong_throwsValidationException(@TempDir Path tempDir) throws IOException {
        String longName = "a".repeat(31);
        String rawYaml = "---\nname: \"" + longName + "\"\nactions: []\n";
        Path yaml = tempDir.resolve("long.yaml");
        Files.writeString(yaml, rawYaml);
        TransformationFlow flow = new TransformationFlow(yaml.toString(), Utils.getSpark());
        Root root = flow.getRoot(yaml.toString());
        TransformationData data = new TransformationData();
        data.setSparkSession(Utils.getSpark());
        assertThrows(ValidationException.class, () -> flow.validate(root, data));
    }

    @Test
    void run_fullPipeline_completesSuccessfully(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("data.csv");
        Files.writeString(csvFile, "id,value\n1,100\n2,200\n");

        String csvPath = csvFile.toAbsolutePath().toString().replace("\\", "/");
        Path yaml = writePipelineYaml(tempDir, "run-test-pipe", String.format(
                "  - name: src\n    kind: source\n    type: generic\n" +
                "    format: csv\n    options:\n      header: \"true\"\n      path: \"%s\"\n" +
                "  - name: out\n    kind: sink\n    type: show\n    input: src\n", csvPath));

        TransformationFlow flow = new TransformationFlow(yaml.toString(), Utils.getSpark());
        assertDoesNotThrow(flow::run);
    }

    @Test
    void transform_callsRootTransform(@TempDir Path tempDir) throws Exception {
        Path yaml = writePipelineYaml(tempDir, "transform-test", "");
        TransformationFlow flow = new TransformationFlow(yaml.toString(), Utils.getSpark());
        Root root = flow.getRoot(yaml.toString());
        TransformationData data = new TransformationData();
        data.setSparkSession(Utils.getSpark());
        assertDoesNotThrow(() -> flow.transform(root, data));
    }

    private Path writePipelineYaml(Path dir, String name, String actionsBlock) throws IOException {
        String actions = actionsBlock.isEmpty() ? "  []\n" : actionsBlock;
        String content = "---\nname: " + name + "\nactions:\n" + actions;
        Path f = dir.resolve("pipeline.yaml");
        Files.writeString(f, content);
        return f;
    }
}
