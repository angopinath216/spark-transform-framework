package com.sparktf.core;

import lombok.Data;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.HashMap;
import java.util.Map;

@Data
public class TransformationData {

    private SparkSession sparkSession;

    private Map<String, Dataset<Row>> datasets =  new HashMap<>();

    private Map<String, String> variables = new HashMap<>();

    public void addVariable(String key, String value) {
        variables.put(key, value);
    }
}
