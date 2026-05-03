package com.sparktf;

import com.sparktf.core.TransformationData;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransformationDataTest {

    @Test
    void newInstance_hasEmptyMaps() {
        TransformationData data = new TransformationData();
        assertNotNull(data.getVariables());
        assertNotNull(data.getDatasets());
        assertTrue(data.getVariables().isEmpty());
        assertTrue(data.getDatasets().isEmpty());
    }

    @Test
    void addVariable_putsKeyValue() {
        TransformationData data = new TransformationData();
        data.addVariable("reportDate", "2024-01-01");
        assertEquals("2024-01-01", data.getVariables().get("reportDate"));
    }

    @Test
    void addVariable_overwritesExistingKey() {
        TransformationData data = new TransformationData();
        data.addVariable("key", "old");
        data.addVariable("key", "new");
        assertEquals("new", data.getVariables().get("key"));
    }

    @Test
    void setVariables_replacesMap() {
        TransformationData data = new TransformationData();
        Map<String, String> vars = new HashMap<>();
        vars.put("a", "1");
        data.setVariables(vars);
        assertEquals("1", data.getVariables().get("a"));
    }

    @Test
    void setDatasets_replacesMap() {
        TransformationData data = new TransformationData();
        Map<String, Dataset<Row>> ds = new HashMap<>();
        data.setDatasets(ds);
        assertSame(ds, data.getDatasets());
    }

    @Test
    void setSparkSession_getsSparkSession() {
        TransformationData data = new TransformationData();
        assertNull(data.getSparkSession());
        data.setSparkSession(Utils.getSpark());
        assertNotNull(data.getSparkSession());
    }
}
