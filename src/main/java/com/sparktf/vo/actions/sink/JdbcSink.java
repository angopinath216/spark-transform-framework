package com.sparktf.vo.actions.sink;

import com.sparktf.core.TransformationData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.spark.sql.SaveMode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class JdbcSink extends Sink {

    private String mode = SaveMode.Append.toString();
    private Map<String, String> options;

    @Override
    public void validate(TransformationData transformationData) {

    }

    @Override
    public void transform(TransformationData transformationData) {
        transformationData.getDatasets().get(getInput())
                .write()
                .format(SinkTypes.JDBC)
                .mode(mode)
                .options(options)
                .save();
    }
}
