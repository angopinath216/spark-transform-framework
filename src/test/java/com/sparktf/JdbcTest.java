package com.sparktf;

import com.sparktf.core.TransformationData;
import com.sparktf.vo.actions.sink.JdbcSink;
import com.sparktf.vo.actions.source.JdbcSource;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JdbcTest {

    private static final String H2_URL = "jdbc:h2:mem:sparktftest;DB_CLOSE_DELAY=-1";
    private static final String H2_DRIVER = "org.h2.Driver";
    private static final String H2_USER = "sa";
    private static final String H2_PASS = "";

    private TransformationData data;

    @BeforeEach
    void setUp() throws Exception {
        data = new TransformationData();
        data.setSparkSession(Utils.getSpark());

        Class.forName(H2_DRIVER);
        try (Connection conn = DriverManager.getConnection(H2_URL, H2_USER, H2_PASS);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS employees");
            stmt.execute("CREATE TABLE employees (id INT, name VARCHAR(50), age INT)");
            stmt.execute("INSERT INTO employees VALUES (1, 'alice', 30)");
            stmt.execute("INSERT INTO employees VALUES (2, 'bob', 25)");
            stmt.execute("INSERT INTO employees VALUES (3, 'charlie', 35)");
        }
    }

    @Test
    void jdbcSource_transform_loadsFromH2() throws Exception {
        JdbcSource src = new JdbcSource();
        src.setName("employees");
        src.setOptions(Map.of(
                "url", H2_URL,
                "dbtable", "employees",
                "user", H2_USER,
                "password", H2_PASS,
                "driver", H2_DRIVER
        ));
        src.validate(data);
        src.transform(data);

        Dataset<Row> result = data.getDatasets().get("employees");
        assertNotNull(result);
        assertEquals(3, result.count());
    }

    @Test
    void jdbcSource_transform_withQuery_loadsFilteredData() throws Exception {
        JdbcSource src = new JdbcSource();
        src.setName("seniors");
        src.setOptions(Map.of(
                "url", H2_URL,
                "query", "SELECT * FROM employees WHERE age > 27",
                "user", H2_USER,
                "password", H2_PASS,
                "driver", H2_DRIVER
        ));
        src.validate(data);
        src.transform(data);

        Dataset<Row> result = data.getDatasets().get("seniors");
        assertNotNull(result);
        assertEquals(2, result.count());
    }

    @Test
    void jdbcSink_transform_writesToH2() throws Exception {
        StructType schema = new StructType()
                .add("id", DataTypes.IntegerType)
                .add("name", DataTypes.StringType)
                .add("age", DataTypes.IntegerType);
        List<Row> rows = Arrays.asList(
                RowFactory.create(10, "dave", 28),
                RowFactory.create(11, "eve", 32)
        );
        data.getDatasets().put("new_employees", Utils.getSpark().createDataFrame(rows, schema));

        JdbcSink sink = new JdbcSink();
        sink.setName("out");
        sink.setInput("new_employees");
        sink.setMode(SaveMode.Append.toString());
        sink.setOptions(Map.of(
                "url", H2_URL,
                "dbtable", "employees",
                "user", H2_USER,
                "password", H2_PASS,
                "driver", H2_DRIVER
        ));
        sink.validate(data);
        sink.transform(data);

        // Verify by reading back
        JdbcSource verifySource = new JdbcSource();
        verifySource.setName("verify");
        verifySource.setOptions(Map.of(
                "url", H2_URL,
                "dbtable", "employees",
                "user", H2_USER,
                "password", H2_PASS,
                "driver", H2_DRIVER
        ));
        verifySource.validate(data);
        verifySource.transform(data);
        assertEquals(5, data.getDatasets().get("verify").count());
    }
}
