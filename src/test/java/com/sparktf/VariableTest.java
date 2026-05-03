package com.sparktf;

import com.sparktf.core.TransformationData;
import com.sparktf.vo.variables.ExpressionVariable;
import com.sparktf.vo.variables.StringVariable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VariableTest {

    private TransformationData data;

    @BeforeEach
    void setUp() {
        data = new TransformationData();
        data.setSparkSession(Utils.getSpark());
    }

    // ── StringVariable ──

    @Test
    void stringVariable_validate_addsToVariables() throws Exception {
        StringVariable v = new StringVariable();
        v.setName("env");
        v.setValue("prod");
        v.validate(data);
        assertEquals("prod", data.getVariables().get("env"));
    }

    @Test
    void stringVariable_transform_isNoOp() {
        StringVariable v = new StringVariable();
        v.setName("x");
        v.setValue("y");
        int sizeBefore = data.getDatasets().size();
        v.transform(data);
        assertEquals(sizeBefore, data.getDatasets().size());
    }

    @Test
    void stringVariable_equalsAndHashCode() {
        StringVariable a = new StringVariable();
        a.setName("k");
        a.setValue("v");
        StringVariable b = new StringVariable();
        b.setName("k");
        b.setValue("v");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ── ExpressionVariable ──

    @Test
    void expressionVariable_validate_executesSparkSql() throws Exception {
        ExpressionVariable v = new ExpressionVariable();
        v.setName("computed");
        v.setExpression("1 + 1");
        v.validate(data);
        assertEquals("2", data.getVariables().get("computed"));
    }

    @Test
    void expressionVariable_validate_stringExpression() throws Exception {
        ExpressionVariable v = new ExpressionVariable();
        v.setName("greeting");
        v.setExpression("'hello'");
        v.validate(data);
        assertEquals("hello", data.getVariables().get("greeting"));
    }

    @Test
    void expressionVariable_validate_nullResult_doesNotAddVariable() throws Exception {
        ExpressionVariable v = new ExpressionVariable();
        v.setName("nullable");
        v.setExpression("cast(null as string)");
        v.validate(data);
        assertFalse(data.getVariables().containsKey("nullable"));
    }

    @Test
    void expressionVariable_defaultValue_isEmptyStringByDefault() {
        ExpressionVariable v = new ExpressionVariable();
        assertEquals("", v.getDefaultValue());
    }

    @Test
    void expressionVariable_transform_isNoOp() {
        ExpressionVariable v = new ExpressionVariable();
        v.setName("e");
        v.setExpression("1");
        v.transform(data);
        assertTrue(data.getDatasets().isEmpty());
    }
}
