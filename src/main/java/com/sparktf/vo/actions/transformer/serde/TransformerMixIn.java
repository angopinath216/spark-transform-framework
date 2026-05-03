package com.sparktf.vo.actions.transformer.serde;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sparktf.vo.actions.transformer.*;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes(value = {
        @JsonSubTypes.Type(value = JoinTransformer.class, name = TransformerTypes.JOIN),
        @JsonSubTypes.Type(value = GroupTransformer.class, name = TransformerTypes.GROUP),
        @JsonSubTypes.Type(value = PivotTransformer.class, name = TransformerTypes.PIVOT),
        @JsonSubTypes.Type(value = UnPivotTransformer.class, name = TransformerTypes.UNPIVOT),
        @JsonSubTypes.Type(value = GeneralTransformer.class, name = TransformerTypes.GENERAL),
        @JsonSubTypes.Type(value = DropTransformer.class, name = TransformerTypes.DROP),
        @JsonSubTypes.Type(value = PersistTransformer.class, name = TransformerTypes.PERSIST)
})
public abstract class TransformerMixIn {
}
