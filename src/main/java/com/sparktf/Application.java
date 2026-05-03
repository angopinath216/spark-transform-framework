package com.sparktf;

import com.sparktf.core.TransformationFlow;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.cli.*;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private final CmdInput input;
    private final SparkSession session;

    public Application(CmdInput input, SparkSession session) {
        this.input = input;
        this.session = session;
    }

    public static void main(String[] args) {
        CmdInput input = parseCli(getOptions(), args);
        SparkSession session = SparkSession.builder().master("local[*]").getOrCreate();
        new Application(input, session).run();
    }

    public static CmdInput parseCli(Options options, String[] args) {
        CommandLineParser parser = new BasicParser();
        HelpFormatter helper = new HelpFormatter();
        try {
            CommandLine cmd = parser.parse(options, args);
            return new CmdInput(cmd.getOptionValue("f"));
        } catch (ParseException e) {
            helper.printHelp(Application.class.getName(), options);
            throw new RuntimeException("Error while parsing the command line", e);
        }
    }

    public static Options getOptions() {
        Options options = new Options();
        Option fileOption = new Option("f", "file", true, "Transformation YAML file");
        fileOption.setRequired(true);
        options.addOption(fileOption);
        return options;
    }

    public void run() {
        new TransformationFlow(input.getYamlFile(), session).run();
    }

    @Data
    @AllArgsConstructor
    public static class CmdInput {
        private String yamlFile;
    }
}
