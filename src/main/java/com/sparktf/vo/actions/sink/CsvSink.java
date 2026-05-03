package com.sparktf.vo.actions.sink;

import com.sparktf.core.Formatter;
import com.sparktf.core.TransformationData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.spark.sql.SaveMode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class CsvSink extends Sink {
    private String mode = SaveMode.Ignore.toString();
    private Map<String, String> options;
    private String path;

    @Override
    public void validate(TransformationData transformationData) {

    }

    @Override
    public void transform(TransformationData transformationData) {
        transformationData.getDatasets().get(getInput())
                .write()
                .mode(mode)
                .options(options)
                .csv(Formatter.formatString(path, transformationData.getVariables()));
    }
}
