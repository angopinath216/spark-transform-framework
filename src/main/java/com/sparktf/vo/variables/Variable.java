package com.sparktf.vo.variables;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sparktf.core.TransformationStep;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes(value = {
        @JsonSubTypes.Type(value = StringVariable.class, name = VariableTypes.STRING_TYPE),
        @JsonSubTypes.Type(value = ExpressionVariable.class, name = VariableTypes.EXPRESSION_TYPE),
})
public abstract class Variable implements TransformationStep {

    @NotNull(message = "variable name is required")
    private String name;

}
