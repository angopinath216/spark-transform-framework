package com.sparktf;

import com.sparktf.core.TransformationData;
import com.sparktf.vo.actions.transformer.*;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransformerTest {

    private TransformationData data;

    @BeforeEach
    void setUp() {
        data = new TransformationData();
        data.setSparkSession(Utils.getSpark());

        StructType schema = new StructType()
                .add("id", DataTypes.IntegerType)
                .add("name", DataTypes.StringType)
                .add("age", DataTypes.IntegerType)
                .add("city", DataTypes.StringType);

        List<Row> rows = Arrays.asList(
                RowFactory.create(1, "alice", 30, "NYC"),
                RowFactory.create(2, "bob", 25, "LA"),
                RowFactory.create(3, "charlie", 20, "NYC"),
                RowFactory.create(4, "diana", 35, "LA"),
                RowFactory.create(4, "diana", 35, "LA")
        );
        data.getDatasets().put("src", Utils.getSpark().createDataFrame(rows, schema));
    }

    // ── GeneralTransformer ──

    @Test
    void general_noOptions_passthrough() {
        GeneralTransformer t = generalTransformer(null, List.of(), List.of(), false);
        t.transform(data);
        assertEquals(5, data.getDatasets().get("out").count());
    }

    @Test
    void general_withFilter_reducesRows() {
        GeneralTransformer t = generalTransformer("age > 25", List.of(), List.of(), false);
        t.transform(data);
        assertEquals(3, data.getDatasets().get("out").count());
    }

    @Test
    void general_emptyFilter_treatedAsNoFilter() {
        GeneralTransformer t = generalTransformer("", List.of(), List.of(), false);
        t.transform(data);
        assertEquals(5, data.getDatasets().get("out").count());
    }

    @Test
    void general_withSelect_projectsColumns() {
        GeneralTransformer t = generalTransformer(null, List.of("id", "name"), List.of(), false);
        t.transform(data);
        Dataset<Row> result = data.getDatasets().get("out");
        assertEquals(2, result.columns().length);
    }

    @Test
    void general_withOrder_sortsData() {
        GeneralTransformer t = generalTransformer(null, List.of(), List.of("age"), false);
        t.transform(data);
        Dataset<Row> result = data.getDatasets().get("out");
        Row first = result.first();
        assertEquals(20, (int) first.getAs("age"));
    }

    @Test
    void general_withDistinct_deduplicates() {
        GeneralTransformer t = generalTransformer(null, List.of(), List.of(), true);
        t.transform(data);
        assertEquals(4, data.getDatasets().get("out").count());
    }

    @Test
    void general_withVariablesInSelect_substituted() {
        data.getVariables().put("col", "name");
        GeneralTransformer t = generalTransformer(null, List.of("{{col}}"), List.of(), false);
        t.transform(data);
        assertEquals(1, data.getDatasets().get("out").columns().length);
    }

    @Test
    void general_validate_isNoOp() {
        GeneralTransformer t = generalTransformer(null, List.of(), List.of(), false);
        assertDoesNotThrow(() -> t.validate(data));
    }

    // ── DropTransformer ──

    @Test
    void drop_removesColumns() {
        DropTransformer t = new DropTransformer();
        t.setName("out");
        t.setInput("src");
        t.setColumns(List.of("city", "age"));
        t.transform(data);
        Dataset<Row> result = data.getDatasets().get("out");
        assertEquals(2, result.columns().length);
        assertArrayEquals(new String[]{"id", "name"}, result.columns());
    }

    @Test
    void drop_validate_isNoOp() {
        DropTransformer t = new DropTransformer();
        t.setName("out");
        t.setInput("src");
        t.setColumns(List.of("city"));
        assertDoesNotThrow(() -> t.validate(data));
    }

    // ── GroupTransformer ──

    @Test
    void group_aggregatesCorrectly() {
        GroupTransformer t = new GroupTransformer();
        t.setName("out");
        t.setInput("src");
        t.setGroupBy(List.of("city"));
        t.setAggregations(List.of("count(*) as cnt", "max(age) as max_age"));
        t.transform(data);
        Dataset<Row> result = data.getDatasets().get("out");
        assertEquals(2, result.count()); // NYC and LA
    }

    @Test
    void group_singleAggregation() {
        GroupTransformer t = new GroupTransformer();
        t.setName("out");
        t.setInput("src");
        t.setGroupBy(List.of("city"));
        t.setAggregations(List.of("count(*) as cnt"));
        t.transform(data);
        assertEquals(2, data.getDatasets().get("out").count());
    }

    @Test
    void group_validate_isNoOp() {
        GroupTransformer t = new GroupTransformer();
        assertDoesNotThrow(() -> t.validate(data));
    }

    // ── JoinTransformer ──

    @Test
    void join_innerJoin_onlyMatchingRows() {
        addDimensionDataset();
        JoinTransformer t = buildJoin("inner", "l.city = r.region");
        t.transform(data);
        Dataset<Row> result = data.getDatasets().get("out");
        assertEquals(5, result.count());
    }

    @Test
    void join_leftJoin_allLeftRows() {
        addDimensionDataset();
        JoinTransformer t = buildJoin("left", "l.city = r.region");
        t.transform(data);
        Dataset<Row> result = data.getDatasets().get("out");
        assertEquals(5, result.count());
    }

    @Test
    void join_validate_isNoOp() {
        JoinTransformer t = new JoinTransformer();
        assertDoesNotThrow(() -> t.validate(data));
    }

    // ── PivotTransformer ──

    @Test
    void pivot_createsWideFormat() {
        addPivotDataset();
        PivotTransformer t = new PivotTransformer();
        t.setName("out");
        t.setInput("metrics");
        t.setGroupBy(List.of("id"));
        t.setPivotColumn("metric");
        t.setAggregations(List.of("sum(value) as total"));
        t.transform(data);
        Dataset<Row> result = data.getDatasets().get("out");
        assertTrue(result.columns().length > 1);
    }

    @Test
    void pivot_validate_isNoOp() {
        PivotTransformer t = new PivotTransformer();
        assertDoesNotThrow(() -> t.validate(data));
    }

    // ── UnPivotTransformer ──

    @Test
    void unpivot_createsLongFormat() {
        addWideDataset();
        UnPivotTransformer t = new UnPivotTransformer();
        t.setName("out");
        t.setInput("wide");
        t.setColumns(List.of("id"));
        t.setNameColumn("metric");
        t.setValueColumn("value");
        t.transform(data);
        Dataset<Row> result = data.getDatasets().get("out");
        assertEquals(3, result.columns().length); // id, metric, value
        assertEquals(4, result.count()); // 2 rows × 2 value cols
    }

    @Test
    void unpivot_validate_isNoOp() {
        UnPivotTransformer t = new UnPivotTransformer();
        assertDoesNotThrow(() -> t.validate(data));
    }

    // ── PersistTransformer ──

    @Test
    void persist_cachesDatasetsAndPreservesData() {
        PersistTransformer t = new PersistTransformer();
        t.setName("out");
        t.setInput("src");
        t.transform(data);
        Dataset<Row> result = data.getDatasets().get("out");
        assertNotNull(result);
        assertEquals(5, result.count());
        assertTrue(result.storageLevel().useMemory());
    }

    @Test
    void persist_validate_isNoOp() {
        PersistTransformer t = new PersistTransformer();
        assertDoesNotThrow(() -> t.validate(data));
    }

    // ── Helpers ──

    private GeneralTransformer generalTransformer(String filter, List<String> select,
                                                   List<String> order, boolean distinct) {
        GeneralTransformer t = new GeneralTransformer();
        t.setName("out");
        t.setInput("src");
        t.setFilter(filter);
        t.setSelect(select);
        t.setOrder(order);
        t.setDistinct(distinct);
        return t;
    }

    private void addDimensionDataset() {
        StructType schema = new StructType()
                .add("region", DataTypes.StringType)
                .add("country", DataTypes.StringType);
        List<Row> rows = Arrays.asList(
                RowFactory.create("NYC", "US"),
                RowFactory.create("LA", "US")
        );
        data.getDatasets().put("dim", Utils.getSpark().createDataFrame(rows, schema));
    }

    private JoinTransformer buildJoin(String joinType, String condition) {
        JoinTransformer t = new JoinTransformer();
        t.setName("out");
        JoinTransformer.Table left = new JoinTransformer.Table();
        left.setInput("src");
        left.setAlias("l");
        JoinTransformer.Table right = new JoinTransformer.Table();
        right.setInput("dim");
        right.setAlias("r");
        t.setLeft(left);
        t.setRight(right);
        t.setJoin(joinType);
        t.setOn(condition);
        t.setSelect(List.of("l.id", "l.name", "r.country"));
        return t;
    }

    private void addPivotDataset() {
        StructType schema = new StructType()
                .add("id", DataTypes.IntegerType)
                .add("metric", DataTypes.StringType)
                .add("value", DataTypes.DoubleType);
        List<Row> rows = Arrays.asList(
                RowFactory.create(1, "speed", 100.0),
                RowFactory.create(1, "latency", 20.0),
                RowFactory.create(2, "speed", 150.0),
                RowFactory.create(2, "latency", 15.0)
        );
        data.getDatasets().put("metrics", Utils.getSpark().createDataFrame(rows, schema));
    }

    private void addWideDataset() {
        StructType schema = new StructType()
                .add("id", DataTypes.IntegerType)
                .add("col_a", DataTypes.IntegerType)
                .add("col_b", DataTypes.IntegerType);
        List<Row> rows = Arrays.asList(
                RowFactory.create(1, 10, 20),
                RowFactory.create(2, 30, 40)
        );
        data.getDatasets().put("wide", Utils.getSpark().createDataFrame(rows, schema));
    }
}
