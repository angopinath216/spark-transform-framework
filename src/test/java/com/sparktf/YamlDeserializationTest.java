package com.sparktf;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sparktf.vo.Root;
import com.sparktf.vo.actions.sink.*;
import com.sparktf.vo.actions.source.*;
import com.sparktf.vo.actions.transformer.*;
import com.sparktf.vo.actions.sink.serde.SinkDeserializerMixIn;
import com.sparktf.vo.actions.source.serde.SourceDeserializerMixIn;
import com.sparktf.vo.actions.transformer.serde.TransformerDeserializerMixIn;
import com.sparktf.vo.variables.ExpressionVariable;
import com.sparktf.vo.variables.StringVariable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YamlDeserializationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.addMixIn(Source.class, SourceDeserializerMixIn.class);
        mapper.addMixIn(Transformer.class, TransformerDeserializerMixIn.class);
        mapper.addMixIn(Sink.class, SinkDeserializerMixIn.class);
    }

    @Test
    void parseRoot_withNameAndEmptyActions() throws Exception {
        Root root = mapper.readValue("---\nname: my-pipeline\nactions: []\n", Root.class);
        assertEquals("my-pipeline", root.getName());
        assertTrue(root.getActions().isEmpty());
    }

    @Test
    void parseRoot_withStringVariable() throws Exception {
        String yaml = "---\nname: var-test-p\nvariables:\n  - name: env\n    type: string\n    value: prod\nactions: []\n";
        Root root = mapper.readValue(yaml, Root.class);
        assertEquals(1, root.getVariables().size());
        assertInstanceOf(StringVariable.class, root.getVariables().get(0));
        assertEquals("prod", ((StringVariable) root.getVariables().get(0)).getValue());
    }

    @Test
    void parseRoot_withExpressionVariable() throws Exception {
        String yaml = "---\nname: expr-test-p\nvariables:\n  - name: dt\n    type: expression\n    expression: current_date()\nactions: []\n";
        Root root = mapper.readValue(yaml, Root.class);
        assertInstanceOf(ExpressionVariable.class, root.getVariables().get(0));
    }

    @Test
    void parseJdbcSource() throws Exception {
        String yaml = "---\nname: jdbc-src-pl\nactions:\n  - name: src\n    kind: source\n    type: jdbc\n    options:\n      url: jdbc:postgresql://localhost/db\n      driver: org.postgresql.Driver\n";
        Root root = mapper.readValue(yaml, Root.class);
        assertInstanceOf(JdbcSource.class, root.getActions().get(0));
        JdbcSource src = (JdbcSource) root.getActions().get(0);
        assertEquals("src", src.getName());
    }

    @Test
    void parseGenericSource() throws Exception {
        String yaml = "---\nname: gen-src-pipe\nactions:\n  - name: src\n    kind: source\n    type: generic\n    format: csv\n    options:\n      header: 'true'\n";
        Root root = mapper.readValue(yaml, Root.class);
        assertInstanceOf(GenericSource.class, root.getActions().get(0));
        assertEquals("csv", ((GenericSource) root.getActions().get(0)).getFormat());
    }

    @Test
    void parseGeneralTransformer() throws Exception {
        String yaml = "---\nname: gen-tf-pipe\nactions:\n  - name: tf\n    kind: transformer\n    type: general\n    input: src\n    filter: \"id > 0\"\n";
        Root root = mapper.readValue(yaml, Root.class);
        assertInstanceOf(GeneralTransformer.class, root.getActions().get(0));
        assertEquals("id > 0", ((GeneralTransformer) root.getActions().get(0)).getFilter());
    }

    @Test
    void parseDropTransformer() throws Exception {
        String yaml = "---\nname: drop-tf-pipe\nactions:\n  - name: tf\n    kind: transformer\n    type: drop\n    input: src\n    columns:\n      - col_a\n      - col_b\n";
        Root root = mapper.readValue(yaml, Root.class);
        assertInstanceOf(DropTransformer.class, root.getActions().get(0));
        assertEquals(2, ((DropTransformer) root.getActions().get(0)).getColumns().size());
    }

    @Test
    void parseGroupTransformer() throws Exception {
        String yaml = "---\nname: grp-tf-pipe\nactions:\n  - name: tf\n    kind: transformer\n    type: group\n    input: src\n    groupBy:\n      - city\n    aggregations:\n      - count(*) as cnt\n";
        Root root = mapper.readValue(yaml, Root.class);
        assertInstanceOf(GroupTransformer.class, root.getActions().get(0));
    }

    @Test
    void parseJoinTransformer() throws Exception {
        String yaml = "---\nname: join-tf-pipe\nactions:\n" +
                "  - name: tf\n    kind: transformer\n    type: join\n" +
                "    left:\n      input: a\n      alias: la\n" +
                "    right:\n      input: b\n      alias: rb\n" +
                "    join: left\n    on: la.id = rb.id\n    select:\n      - la.id\n";
        Root root = mapper.readValue(yaml, Root.class);
        assertInstanceOf(JoinTransformer.class, root.getActions().get(0));
        JoinTransformer t = (JoinTransformer) root.getActions().get(0);
        assertEquals("left", t.getJoin());
    }

    @Test
    void parsePivotTransformer() throws Exception {
        String yaml = "---\nname: pvt-tf-pipe\nactions:\n  - name: tf\n    kind: transformer\n    type: pivot\n    input: src\n    groupBy:\n      - id\n    pivotColumn: metric\n    aggregations:\n      - sum(value)\n";
        Root root = mapper.readValue(yaml, Root.class);
        assertInstanceOf(PivotTransformer.class, root.getActions().get(0));
    }

    @Test
    void parseUnpivotTransformer() throws Exception {
        String yaml = "---\nname: upvt-tf-pipe\nactions:\n  - name: tf\n    kind: transformer\n    type: unpivot\n    input: src\n    columns:\n      - id\n    nameColumn: metric\n    valueColumn: value\n";
        Root root = mapper.readValue(yaml, Root.class);
        assertInstanceOf(UnPivotTransformer.class, root.getActions().get(0));
    }

    @Test
    void parsePersistTransformer() throws Exception {
        String yaml = "---\nname: per-tf-pipe\nactions:\n  - name: tf\n    kind: transformer\n    type: persist\n    input: src\n";
        Root root = mapper.readValue(yaml, Root.class);
        assertInstanceOf(PersistTransformer.class, root.getActions().get(0));
    }

    @Test
    void parseShowSink() throws Exception {
        String yaml = "---\nname: show-sk-pipe\nactions:\n  - name: out\n    kind: sink\n    type: show\n    input: src\n";
        Root root = mapper.readValue(yaml, Root.class);
        assertInstanceOf(ShowSink.class, root.getActions().get(0));
    }

    @Test
    void parseCsvSink() throws Exception {
        String yaml = "---\nname: csv-sk-pipe\nactions:\n  - name: out\n    kind: sink\n    type: csv\n    input: src\n    path: /tmp/out\n    options:\n      header: 'true'\n";
        Root root = mapper.readValue(yaml, Root.class);
        assertInstanceOf(CsvSink.class, root.getActions().get(0));
        assertEquals("/tmp/out", ((CsvSink) root.getActions().get(0)).getPath());
    }

    @Test
    void parseJdbcSink() throws Exception {
        String yaml = "---\nname: jdbc-sk-pipe\nactions:\n  - name: out\n    kind: sink\n    type: jdbc\n    input: src\n    options:\n      url: jdbc:postgresql://localhost/db\n";
        Root root = mapper.readValue(yaml, Root.class);
        assertInstanceOf(JdbcSink.class, root.getActions().get(0));
    }

    @Test
    void parseFullPipelineYaml_allFeaturesInOneYaml() throws Exception {
        String yaml =
            "---\n" +
            "name: full-feature-pipe\n" +
            "variables:\n" +
            "  - name: envName\n" +
            "    type: string\n" +
            "    value: prod\n" +
            "  - name: runDate\n" +
            "    type: expression\n" +
            "    expression: current_date()\n" +
            "actions:\n" +
            "  - name: src-jdbc\n" +
            "    kind: source\n" +
            "    type: jdbc\n" +
            "    options:\n" +
            "      url: jdbc:postgresql://localhost/db\n" +
            "      driver: org.postgresql.Driver\n" +
            "      dbtable: orders\n" +
            "  - name: src-csv\n" +
            "    kind: source\n" +
            "    type: generic\n" +
            "    format: csv\n" +
            "    options:\n" +
            "      header: 'true'\n" +
            "      path: /data/input\n" +
            "  - name: tf-general\n" +
            "    kind: transformer\n" +
            "    type: general\n" +
            "    input: src-csv\n" +
            "    filter: \"age > 20\"\n" +
            "    select:\n" +
            "      - id\n" +
            "      - name\n" +
            "    order:\n" +
            "      - id\n" +
            "    distinct: true\n" +
            "  - name: tf-drop\n" +
            "    kind: transformer\n" +
            "    type: drop\n" +
            "    input: tf-general\n" +
            "    columns:\n" +
            "      - name\n" +
            "  - name: tf-group\n" +
            "    kind: transformer\n" +
            "    type: group\n" +
            "    input: src-csv\n" +
            "    groupBy:\n" +
            "      - city\n" +
            "    aggregations:\n" +
            "      - count(*) as cnt\n" +
            "      - max(age) as max_age\n" +
            "  - name: tf-join\n" +
            "    kind: transformer\n" +
            "    type: join\n" +
            "    left:\n" +
            "      input: src-csv\n" +
            "      alias: a\n" +
            "    right:\n" +
            "      input: src-jdbc\n" +
            "      alias: b\n" +
            "    join: left\n" +
            "    on: a.id = b.id\n" +
            "    select:\n" +
            "      - \"a.id as id\"\n" +
            "      - \"b.name as bname\"\n" +
            "  - name: tf-pivot\n" +
            "    kind: transformer\n" +
            "    type: pivot\n" +
            "    input: src-csv\n" +
            "    groupBy:\n" +
            "      - id\n" +
            "    pivotColumn: city\n" +
            "    aggregations:\n" +
            "      - sum(age) as total_age\n" +
            "  - name: tf-unpivot\n" +
            "    kind: transformer\n" +
            "    type: unpivot\n" +
            "    input: src-csv\n" +
            "    columns:\n" +
            "      - id\n" +
            "    nameColumn: metric\n" +
            "    valueColumn: value\n" +
            "  - name: tf-persist\n" +
            "    kind: transformer\n" +
            "    type: persist\n" +
            "    input: src-csv\n" +
            "  - name: sink-show\n" +
            "    kind: sink\n" +
            "    type: show\n" +
            "    input: tf-general\n" +
            "    printSchema: true\n" +
            "  - name: sink-csv\n" +
            "    kind: sink\n" +
            "    type: csv\n" +
            "    input: tf-general\n" +
            "    path: /tmp/output\n" +
            "    mode: Overwrite\n" +
            "    options:\n" +
            "      header: 'true'\n" +
            "  - name: sink-jdbc\n" +
            "    kind: sink\n" +
            "    type: jdbc\n" +
            "    input: tf-general\n" +
            "    options:\n" +
            "      url: jdbc:postgresql://localhost/db\n" +
            "      dbtable: output\n" +
            "...";

        Root root = mapper.readValue(yaml, Root.class);

        assertEquals("full-feature-pipe", root.getName());
        assertEquals(2, root.getVariables().size());
        assertInstanceOf(StringVariable.class, root.getVariables().get(0));
        assertInstanceOf(ExpressionVariable.class, root.getVariables().get(1));
        assertEquals(12, root.getActions().size());

        assertInstanceOf(JdbcSource.class,       root.getActions().get(0));
        assertInstanceOf(GenericSource.class,    root.getActions().get(1));
        assertInstanceOf(GeneralTransformer.class, root.getActions().get(2));
        assertInstanceOf(DropTransformer.class,  root.getActions().get(3));
        assertInstanceOf(GroupTransformer.class, root.getActions().get(4));
        assertInstanceOf(JoinTransformer.class,  root.getActions().get(5));
        assertInstanceOf(PivotTransformer.class, root.getActions().get(6));
        assertInstanceOf(UnPivotTransformer.class, root.getActions().get(7));
        assertInstanceOf(PersistTransformer.class, root.getActions().get(8));
        assertInstanceOf(ShowSink.class,         root.getActions().get(9));
        assertInstanceOf(CsvSink.class,          root.getActions().get(10));
        assertInstanceOf(JdbcSink.class,         root.getActions().get(11));

        GeneralTransformer gen = (GeneralTransformer) root.getActions().get(2);
        assertEquals("age > 20", gen.getFilter());
        assertTrue(gen.getDistinct());

        JoinTransformer join = (JoinTransformer) root.getActions().get(5);
        assertEquals("left", join.getJoin());
        assertEquals("a.id = b.id", join.getOn());

        ShowSink show = (ShowSink) root.getActions().get(9);
        assertTrue(show.getPrintSchema());

        CsvSink csv = (CsvSink) root.getActions().get(10);
        assertEquals("/tmp/output", csv.getPath());
        assertEquals("Overwrite", csv.getMode());
    }
}
