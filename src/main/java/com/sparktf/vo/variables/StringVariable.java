package com.sparktf.vo.variables;

import com.sparktf.core.TransformationData;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
public class StringVariable extends Variable{
    @NotNull(message = "value is required")
    private String value;

    @Override
    public void validate(TransformationData transformationData) {
        transformationData.addVariable(getName(), getValue());
    }

    @Override
    public void transform(TransformationData transformationData) {

    }
}
