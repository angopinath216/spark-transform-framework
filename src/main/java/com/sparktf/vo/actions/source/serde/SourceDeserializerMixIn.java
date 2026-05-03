package com.sparktf.vo.actions.source.serde;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = SourceDeserializer.class)
public abstract class SourceDeserializerMixIn {
}
