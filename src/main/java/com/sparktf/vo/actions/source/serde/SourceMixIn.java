package com.sparktf.vo.actions.source.serde;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sparktf.vo.actions.source.GenericSource;
import com.sparktf.vo.actions.source.JdbcSource;
import com.sparktf.vo.actions.source.SourceTypes;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes(value = {
        @JsonSubTypes.Type(value = JdbcSource.class, name = SourceTypes.JDBC),
        @JsonSubTypes.Type(value = GenericSource.class, name = SourceTypes.GENERIC)
})
public abstract class SourceMixIn {
}
