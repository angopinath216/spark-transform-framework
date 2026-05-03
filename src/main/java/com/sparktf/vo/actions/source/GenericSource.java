package com.sparktf.vo.actions.source;

import com.sparktf.core.TransformationData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class GenericSource extends Source {

    Map<String, String> options;
    String format;

    @Override
    public void validate(TransformationData transformationData) {

    }

    @Override
    public void transform(TransformationData transformationData) {
        Dataset<Row> data = transformationData.getSparkSession()
                .read()
                .format(format)
                .options(options)
                .load();
        transformationData.getDatasets().put(getName(), data);
    }
}
