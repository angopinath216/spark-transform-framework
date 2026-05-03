package com.sparktf.core;

import com.sparktf.exception.ValidationException;

public interface TransformationStep {
    void validate(TransformationData transformationData) throws ValidationException;
    void transform(TransformationData transformationData);
}
