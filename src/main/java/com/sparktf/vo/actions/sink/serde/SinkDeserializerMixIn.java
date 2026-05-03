package com.sparktf.vo.actions.sink.serde;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = SinkDeserializer.class)
public abstract class SinkDeserializerMixIn {
}
