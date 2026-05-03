package com.sparktf;

import com.sparktf.core.TransformationData;
import com.sparktf.vo.actions.sink.CsvSink;
import com.sparktf.vo.actions.sink.JdbcSink;
import com.sparktf.vo.actions.sink.ShowSink;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SinkTest {

    private TransformationData data;
    private Dataset<Row> testDf;

    @BeforeEach
    void setUp() {
        data = new TransformationData();
        data.setSparkSession(Utils.getSpark());

        StructType schema = new StructType()
                .add("id", DataTypes.IntegerType)
                .add("name", DataTypes.StringType);
        List<Row> rows = Arrays.asList(
                RowFactory.create(1, "alice"),
                RowFactory.create(2, "bob")
        );
        testDf = Utils.getSpark().createDataFrame(rows, schema);
        data.getDatasets().put("src", testDf);
    }

    // ── ShowSink ──

    @Test
    void showSink_transform_doesNotThrow() {
        ShowSink sink = new ShowSink();
        sink.setName("out");
        sink.setInput("src");
        assertDoesNotThrow(() -> sink.transform(data));
    }

    @Test
    void showSink_withPrintSchema_doesNotThrow() {
        ShowSink sink = new ShowSink();
        sink.setName("out");
        sink.setInput("src");
        sink.setPrintSchema(true);
        assertDoesNotThrow(() -> sink.transform(data));
    }

    @Test
    void showSink_defaultPrintSchema_isFalse() {
        ShowSink sink = new ShowSink();
        assertFalse(sink.getPrintSchema());
    }

    @Test
    void showSink_validate_isNoOp() {
        ShowSink sink = new ShowSink();
        sink.setInput("src");
        assertDoesNotThrow(() -> sink.validate(data));
    }

    // ── CsvSink ──

    @Test
    void csvSink_writesToTempDir(@TempDir Path tempDir) {
        String outPath = tempDir.resolve("output").toAbsolutePath().toString().replace("\\", "/");
        CsvSink sink = new CsvSink();
        sink.setName("out");
        sink.setInput("src");
        sink.setPath(outPath);
        sink.setMode(SaveMode.Overwrite.toString());
        sink.setOptions(Map.of("header", "true"));
        assertDoesNotThrow(() -> sink.transform(data));
        assertTrue(tempDir.resolve("output").toFile().exists());
    }

    @Test
    void csvSink_pathWithVariable_substituted(@TempDir Path tempDir) {
        data.getVariables().put("date", "2024-01-01");
        String outPath = tempDir.resolve("output-{{date}}").toAbsolutePath().toString().replace("\\", "/");
        CsvSink sink = new CsvSink();
        sink.setName("out");
        sink.setInput("src");
        sink.setPath(outPath);
        sink.setMode(SaveMode.Overwrite.toString());
        sink.setOptions(new HashMap<>());
        assertDoesNotThrow(() -> sink.transform(data));
    }

    @Test
    void csvSink_defaultMode_isIgnore() {
        CsvSink sink = new CsvSink();
        assertEquals(SaveMode.Ignore.toString(), sink.getMode());
    }

    @Test
    void csvSink_validate_isNoOp() {
        CsvSink sink = new CsvSink();
        sink.setInput("src");
        assertDoesNotThrow(() -> sink.validate(data));
    }

    // ── JdbcSink ──

    @Test
    void jdbcSink_defaultMode_isAppend() {
        JdbcSink sink = new JdbcSink();
        assertEquals(SaveMode.Append.toString(), sink.getMode());
    }

    @Test
    void jdbcSink_validate_isNoOp() {
        JdbcSink sink = new JdbcSink();
        sink.setInput("src");
        assertDoesNotThrow(() -> sink.validate(data));
    }
}
