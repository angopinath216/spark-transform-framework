package com.sparktf;

import com.sparktf.core.Formatter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormatterTest {

    @Test
    void formatString_replacesVariables() {
        Map<String, String> vars = new HashMap<>();
        vars.put("table", "my_table");
        vars.put("date", "2024-01-01");
        assertEquals("SELECT * FROM my_table WHERE dt = '2024-01-01'",
                Formatter.formatString("SELECT * FROM {{table}} WHERE dt = '{{date}}'", vars));
    }

    @Test
    void formatString_noMatchingVariables_returnsOriginal() {
        Map<String, String> vars = new HashMap<>();
        vars.put("other", "value");
        String input = "SELECT * FROM my_table";
        assertEquals(input, Formatter.formatString(input, vars));
    }

    @Test
    void formatString_emptyVariables_returnsOriginal() {
        String input = "SELECT {{col}} FROM t";
        assertEquals(input, Formatter.formatString(input, new HashMap<>()));
    }

    @Test
    void formatList_replacesInEachEntry() {
        Map<String, String> vars = Map.of("col", "age", "alias", "user_age");
        List<String> list = Arrays.asList("{{col}} as {{alias}}", "name");
        List<String> result = Formatter.formatList(list, vars);
        assertEquals("age as user_age", result.get(0));
        assertEquals("name", result.get(1));
    }

    @Test
    void formatList_emptyList_returnsEmpty() {
        assertTrue(Formatter.formatList(List.of(), new HashMap<>()).isEmpty());
    }

    @Test
    void formatMap_replacesValues() {
        Map<String, String> vars = Map.of("schema", "public");
        Map<String, String> options = new HashMap<>();
        options.put("dbtable", "{{schema}}.orders");
        options.put("user", "admin");
        Map<String, String> result = Formatter.formatMap(options, vars);
        assertEquals("public.orders", result.get("dbtable"));
        assertEquals("admin", result.get("user"));
    }

    @Test
    void formatMap_emptyOptions_returnsEmpty() {
        assertTrue(Formatter.formatMap(new HashMap<>(), new HashMap<>()).isEmpty());
    }
}
