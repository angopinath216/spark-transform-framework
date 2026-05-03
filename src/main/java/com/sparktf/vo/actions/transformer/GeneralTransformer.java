package com.sparktf.vo.actions.transformer;

import com.sparktf.core.Formatter;
import com.sparktf.core.TransformationData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class GeneralTransformer extends  Transformer{
    private String input;
    private String filter = null;
    private List<String> select = new ArrayList<>();
    private List<String> order = new ArrayList<>();
    private Boolean distinct = Boolean.FALSE;

    @Override
    public void validate(TransformationData transformationData) {

    }

    @Override
    public void transform(TransformationData transformationData) {
        Dataset<Row> inputData = transformationData.getDatasets().get(input);
        Dataset<Row> outputData = inputData.selectExpr("*");
        if(filter != null && !filter.isEmpty()){
            outputData = outputData.filter(filter);
        }
        if(!select.isEmpty()){
            outputData = outputData.selectExpr(Formatter.formatList(select, transformationData.getVariables()).toArray(new String[0]));
        }
        if(!order.isEmpty()){
            outputData = outputData.orderBy(order.stream().map(Column::new).toArray(Column[]::new));
        }

        if(distinct){
            outputData = outputData.distinct();
        }

        transformationData.getDatasets().put(getName(), outputData);

    }
}
