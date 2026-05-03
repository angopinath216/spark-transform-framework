package com.sparktf.vo.actions;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sparktf.core.TransformationStep;
import com.sparktf.vo.actions.sink.Sink;
import com.sparktf.vo.actions.source.Source;
import com.sparktf.vo.actions.transformer.Transformer;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "kind"
)
@JsonSubTypes(value = {
        @JsonSubTypes.Type(value = Sink.class, name = ActionTypes.SINK),
        @JsonSubTypes.Type(value = Source.class, name = ActionTypes.SOURCE),
        @JsonSubTypes.Type(value = Transformer.class, name = ActionTypes.TRANSFORMER)
})
public abstract class Action implements TransformationStep {
    @NotNull
    private String name;
}
