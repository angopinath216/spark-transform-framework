package com.sparktf.vo.actions.sink.serde;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sparktf.vo.actions.sink.CsvSink;
import com.sparktf.vo.actions.sink.JdbcSink;
import com.sparktf.vo.actions.sink.ShowSink;
import com.sparktf.vo.actions.sink.SinkTypes;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes(value = {
        @JsonSubTypes.Type(value = JdbcSink.class, name = SinkTypes.JDBC),
        @JsonSubTypes.Type(value = CsvSink.class, name = SinkTypes.CSV),
        @JsonSubTypes.Type(value = ShowSink.class, name = SinkTypes.SHOW)
})
public abstract class SinkMixIn {
}
