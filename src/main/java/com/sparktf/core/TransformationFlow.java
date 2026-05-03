package com.sparktf.core;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sparktf.exception.ValidationException;
import com.sparktf.vo.Root;
import com.sparktf.vo.actions.sink.Sink;
import com.sparktf.vo.actions.sink.serde.SinkDeserializerMixIn;
import com.sparktf.vo.actions.source.Source;
import com.sparktf.vo.actions.source.serde.SourceDeserializerMixIn;
import com.sparktf.vo.actions.transformer.Transformer;
import com.sparktf.vo.actions.transformer.serde.TransformerDeserializerMixIn;
import org.apache.spark.sql.SparkSession;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class TransformationFlow {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformationFlow.class);

    private final Validator validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    private final String yamlFile;
    private final SparkSession sparkSession;

    public TransformationFlow(String yamlFile, SparkSession sparkSession) {
        this.yamlFile = yamlFile;
        this.sparkSession = sparkSession;
    }

    public void run() {
        try {
            LOGGER.info("Transformation started: {}", this.yamlFile);
            TransformationData data = new TransformationData();
            data.setSparkSession(sparkSession.cloneSession());

            Root root = getRoot(yamlFile);
            validate(root, data);

            LOGGER.debug("root object: {}", root);
            transform(root, data);
            LOGGER.info("Transformation completed: {}", this.yamlFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void transform(Root root, TransformationData data) {
        root.transform(data);
    }

    public void validate(Root root, TransformationData data) throws ValidationException {
        List<String> violations = validator.validate(root)
                .stream()
                .map(ConstraintViolation::getMessage)
                .peek(e -> LOGGER.info("validation message: {}", e))
                .collect(Collectors.toList());
        if (!violations.isEmpty()) {
            throw new ValidationException(violations.toString());
        }
        try {
            root.validate(data);
        } catch (Exception e) {
            throw new ValidationException("Error in validation", e);
        }
    }

    public Root getRoot(String inputFile) {
        try {
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            objectMapper.addMixIn(Source.class, SourceDeserializerMixIn.class);
            objectMapper.addMixIn(Transformer.class, TransformerDeserializerMixIn.class);
            objectMapper.addMixIn(Sink.class, SinkDeserializerMixIn.class);
            return objectMapper.readValue(new File(inputFile), Root.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
