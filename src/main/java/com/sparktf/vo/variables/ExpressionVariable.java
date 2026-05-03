package com.sparktf.vo.variables;

import com.sparktf.core.TransformationData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.spark.sql.Encoders;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@Data
public class ExpressionVariable extends Variable {
    @NotNull(message = "variable expression is required")
    private String expression;
    private String defaultValue = "";

    @Override
    public void validate(TransformationData transformationData) {
        List<ExpressionVariableVO> data = transformationData.getSparkSession().sql(String.format("select %s as variable", expression)).as(Encoders.bean(ExpressionVariableVO.class)).collectAsList();
        if(Objects.nonNull(data) && !data.isEmpty() && Objects.nonNull(data.get(0)) && Objects.nonNull(data.get(0).getVariable())){
            transformationData.addVariable(getName(), data.get(0).getVariable());
        }
    }

    @Override
    public void transform(TransformationData transformationData) {

    }

    @Data
    public static class ExpressionVariableVO implements Serializable {
        private String variable;
    }
}
