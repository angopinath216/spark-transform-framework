package com.sparktf.vo.actions.transformer;

import com.sparktf.core.TransformationData;
import lombok.Data;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.storage.StorageLevel;

@Data
public class PersistTransformer extends  Transformer{

    private String input;
    private String level;

    @Override
    public void validate(TransformationData transformationData) {

    }

    @Override
    public void transform(TransformationData transformationData) {
        Dataset<Row> inputData = transformationData.getDatasets().get(input);
        transformationData.getDatasets().put(getName(), inputData.persist(StorageLevel.MEMORY_ONLY()));
    }
}
