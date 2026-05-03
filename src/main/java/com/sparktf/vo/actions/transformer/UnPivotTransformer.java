package com.sparktf.vo.actions.transformer;

import com.sparktf.core.TransformationData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.StructField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
public class UnPivotTransformer extends Transformer{

    private String input;
    private List<String> columns;
    private String nameColumn;
    private String valueColumn;

    @Override
    public void validate(TransformationData transformationData) {

    }

    @Override
    public void transform(TransformationData transformationData) {
        Dataset<Row> inputData = transformationData.getDatasets().get(getInput());
        List<String> inputDataColumns = Arrays.stream(inputData.schema().fields()).map(StructField::name).collect(Collectors.toList());

        List<String> unpivotColumns = inputDataColumns.stream().filter(e -> !columns.contains(e)).collect(Collectors.toList());

        String stack = String.format("stack(%d,%s) as (%s,%s)", unpivotColumns.size(), unpivotColumns.stream().map(e -> String.format("'%s',%s", e, e)).collect(Collectors.joining(",")), nameColumn, valueColumn);

        List<String> new1 = new ArrayList<>(columns);
        new1.add(stack);
        Dataset<Row> data = inputData.selectExpr(new1.toArray(new String[0]));
        transformationData.getDatasets().put(getName(), data);
    }
}
