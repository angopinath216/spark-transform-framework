package com.sparktf;

import com.sparktf.core.TransformationData;
import com.sparktf.exception.ValidationException;
import com.sparktf.vo.actions.source.GenericSource;
import com.sparktf.vo.actions.source.JdbcSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SourceTest {

    private TransformationData data;

    @BeforeEach
    void setUp() {
        data = new TransformationData();
        data.setSparkSession(Utils.getSpark());
    }

    // ── GenericSource ──

    @Test
    void genericSource_loadsCSV() throws Exception {
        URL url = getClass().getClassLoader().getResource("data/sample.csv");
        assertNotNull(url, "sample.csv test resource not found");
        String csvPath = Paths.get(url.toURI()).toAbsolutePath().toString().replace("\\", "/");

        GenericSource src = new GenericSource();
        src.setName("loaded");
        src.setFormat("csv");
        src.setOptions(Map.of("header", "true", "inferSchema", "true"));
        src.setOptions(new HashMap<>(Map.of("header", "true", "inferSchema", "true", "path", csvPath)));
        src.transform(data);

        assertNotNull(data.getDatasets().get("loaded"));
        assertEquals(5, data.getDatasets().get("loaded").count());
    }

    @Test
    void genericSource_validate_isNoOp() {
        GenericSource src = new GenericSource();
        assertDoesNotThrow(() -> src.validate(data));
    }

    // ── JdbcSource ──

    @Test
    void jdbcSource_validate_nullQueryFile_noQuerySet() throws ValidationException {
        JdbcSource src = new JdbcSource();
        src.setName("src");
        src.setQueryFile(null);
        src.setOptions(new HashMap<>());
        src.validate(data);
        assertNull(src.getQuery());
    }

    @Test
    void jdbcSource_validate_withQueryFile_setsQuery(@TempDir Path tempDir) throws Exception {
        Path sqlFile = tempDir.resolve("query.sql");
        Files.writeString(sqlFile, "SELECT * FROM orders WHERE date = '{{reportDate}}'");
        data.addVariable("reportDate", "2024-01-01");

        JdbcSource src = new JdbcSource();
        src.setName("src");
        src.setQueryFile(sqlFile.toString());
        src.setOptions(new HashMap<>());
        src.validate(data);

        assertNotNull(src.getQuery());
        assertTrue(src.getQuery().contains("2024-01-01"));
    }

    @Test
    void jdbcSource_validate_multilineQueryFile(@TempDir Path tempDir) throws Exception {
        Path sqlFile = tempDir.resolve("query.sql");
        Files.writeString(sqlFile, "SELECT id,\n  name\nFROM users");

        JdbcSource src = new JdbcSource();
        src.setName("src");
        src.setQueryFile(sqlFile.toString());
        src.setOptions(new HashMap<>());
        src.validate(data);

        assertNotNull(src.getQuery());
        assertTrue(src.getQuery().contains("SELECT"));
    }

    @Test
    void jdbcSource_validate_nonExistentQueryFile_throwsValidationException() {
        JdbcSource src = new JdbcSource();
        src.setName("src");
        src.setQueryFile("/nonexistent/path/query.sql");
        src.setOptions(new HashMap<>());
        assertThrows(ValidationException.class, () -> src.validate(data));
    }

}
