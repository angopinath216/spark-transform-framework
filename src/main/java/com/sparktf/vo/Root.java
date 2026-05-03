package com.sparktf.vo;

import com.sparktf.core.TransformationData;
import com.sparktf.core.TransformationStep;
import com.sparktf.exception.ValidationException;
import com.sparktf.vo.actions.Action;
import com.sparktf.vo.variables.Variable;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.*;

@Data
public class Root  implements TransformationStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(Root.class);

    @NotNull(message = "name should not be null")
    @Size(min = 5, max = 30, message = "name must be 5-30 char")
    private String name;

    private Map<String,String> configuration;

    private List<Variable> variables = new ArrayList<Variable>();

    private List<Action> actions = new ArrayList<>();

    @Override
    public void validate(TransformationData transformationData) {
        variables.forEach(e -> {
            try {
                e.validate(transformationData);
            } catch (ValidationException ex) {
                throw new RuntimeException(ex);
            }
        });

        actions.forEach(e -> {
            try {
                e.validate(transformationData);
            } catch (ValidationException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    @Override
    public void transform(TransformationData transformationData) {
        actions.forEach(e -> e.transform(transformationData));
    }

}
