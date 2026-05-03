package com.sparktf.vo.actions.sink;

import com.sparktf.core.TransformationData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

@EqualsAndHashCode(callSuper = true)
@Data
public class ShowSink extends Sink {

    private Boolean printSchema = Boolean.FALSE;

    @Override
    public void validate(TransformationData transformationData) {

    }

    @Override
    public void transform(TransformationData transformationData) {
        Dataset<Row> input = transformationData.getDatasets().get(getInput());
        if(printSchema){
            input.printSchema();
        }
        input.show(false);
    }
}
