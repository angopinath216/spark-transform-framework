package com.sparktf.vo.actions.transformer;

import com.sparktf.core.TransformationData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import static org.apache.spark.sql.functions.*;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class JoinTransformer extends Transformer{

    private Table left;
    private Table right;
    private String join;
    private String on;
    private List<String> select;

    @Override
    public void validate(TransformationData transformationData) {

    }

    @Override
    public void transform(TransformationData transformationData) {

        Dataset<Row> leftDs = transformationData.getDatasets().get(left.input).as(left.alias);
        Dataset<Row> rightDs = transformationData.getDatasets().get(right.input).as(right.alias);

        Dataset<Row> data = leftDs
                .join(rightDs, expr(on), join)
                .selectExpr(select.toArray(new String[0]));

        transformationData.getDatasets().put(getName(), data);
    }

    @Data
    public static class Table {
        private String input;
        private String alias;
    }


}
