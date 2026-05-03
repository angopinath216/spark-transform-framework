package com.sparktf.vo.actions.sink.serde;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.sparktf.vo.actions.sink.Sink;
import com.sparktf.vo.actions.transformer.Transformer;

import java.io.IOException;

public class SinkDeserializer extends JsonDeserializer<Object> {
    @Override
    public Sink deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        ObjectMapper jsonMapper = new ObjectMapper();

        // Omit null values from the JSON.
        jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Treat received empty JSON strings as null Java values.
        // Note: doesn't seem to work - using custom deserializer through module
        // below instead.
        jsonMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

        jsonMapper.addMixIn(Sink.class, SinkMixIn.class);

        return jsonMapper.readValue(node.toString(), Sink.class);

    }
}
