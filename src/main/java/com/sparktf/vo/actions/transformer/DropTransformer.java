package com.sparktf.vo.actions.transformer;

import com.sparktf.core.TransformationData;
import lombok.Data;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.util.List;

@Data
public class DropTransformer extends  Transformer{

    private String input;
    private List<String> columns;

    @Override
    public void validate(TransformationData transformationData) {

    }

    @Override
    public void transform(TransformationData transformationData) {
        Dataset<Row> inputData = transformationData.getDatasets().get(input);
        Dataset<Row> outputData = inputData.drop(columns.toArray(new String[0]));
        transformationData.getDatasets().put(getName(), outputData);
    }
}
