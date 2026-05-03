package com.sparktf.vo.actions.transformer;

import com.sparktf.core.TransformationData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;

import java.util.Arrays;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class GroupTransformer extends Transformer{
    private String input;
    private List<String> groupBy;
    private List<String> aggregations;

    @Override
    public void validate(TransformationData transformationData) {

    }

    @Override
    public void transform(TransformationData transformationData) {
        Column[] aggColumns = aggregations.stream().map(functions::expr).toArray(Column[]::new);
        Dataset<Row> data = transformationData.getDatasets().get(getInput())
                .groupBy(groupBy.stream().map(Column::new).toArray(Column[]::new))
                .agg(aggColumns[0], Arrays.copyOfRange(aggColumns, 1, aggColumns.length));
        transformationData.getDatasets().put(getName(), data);
    }
}
