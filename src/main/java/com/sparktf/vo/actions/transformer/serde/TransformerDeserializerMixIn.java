package com.sparktf.vo.actions.transformer.serde;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = TransformerDeserializer.class)
public abstract class TransformerDeserializerMixIn {
}
