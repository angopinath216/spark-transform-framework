package com.sparktf.core;

import org.apache.commons.text.StringSubstitutor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Formatter {
    private static final String START_INDEX = "{{";
    private static final String END_INDEX = "}}";


    public static String formatString(String str, Map<String, String> variables){
        return StringSubstitutor.replace(str, variables, START_INDEX, END_INDEX);
    }

    public static List<String> formatList(List<String> list, Map<String, String> variables){
        return list.stream().map(str -> StringSubstitutor.replace(str, variables, START_INDEX, END_INDEX)).collect(Collectors.toList());
    }

    public static Map<String, String> formatMap(Map<String,String> map, Map<String, String> variables){
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> StringSubstitutor.replace(entry.getValue(), variables, START_INDEX, END_INDEX)
                ));
    }
}
