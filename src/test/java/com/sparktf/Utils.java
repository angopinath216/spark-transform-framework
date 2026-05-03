package com.sparktf;

import org.apache.spark.sql.SparkSession;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

public class Utils {

    public static String getAbsolutePath(String location) {
        URL url = Utils.class.getClassLoader().getResource(location);
        try {
            return Paths.get(url.toURI()).toAbsolutePath().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static SparkSession getSpark(){
        return SparkSession.builder().master("local").getOrCreate();
    }
}
