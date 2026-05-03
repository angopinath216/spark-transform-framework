package com.sparktf.vo.actions.source;

import com.sparktf.core.Formatter;
import com.sparktf.core.TransformationData;
import com.sparktf.exception.ValidationException;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

@EqualsAndHashCode(callSuper = true)
@Data
public class JdbcSource extends Source {

    Logger LOG = LoggerFactory.getLogger(this.getClass());

    Map<String, String> options;
    String table;
    String queryFile;

    @Setter(AccessLevel.NONE)
    private String query;

    @Override
    public void validate(TransformationData transformationData) throws ValidationException {
        if(Objects.nonNull(queryFile)) {
            try {
                String queryString = String.join(" \n ", Files.readAllLines(Paths.get(queryFile)));
                queryString = Formatter.formatString(queryString, transformationData.getVariables());
                LOG.debug("query String of {}: {}", getName(), queryString);
                query = queryString;

            } catch (IOException e) {
                throw new ValidationException(String.format("Error while getting query file %s", getQueryFile()), e);
            }
        }

    }

    @Override
    public void transform(TransformationData transformationData) {
        DataFrameReader reader = transformationData.getSparkSession()
                .read()
                .format(SourceTypes.JDBC)
                .options(options);
        if(Objects.nonNull(getQuery())){
            reader = reader.option("query", getQuery());
        }
        Dataset<Row> data =  reader.load();
        transformationData.getDatasets().put(getName(), data);
    }
}
