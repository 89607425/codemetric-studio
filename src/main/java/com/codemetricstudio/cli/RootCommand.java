package com.codemetricstudio.cli;

import picocli.CommandLine;

@CommandLine.Command(
        name = "codemetric",
        mixinStandardHelpOptions = true,
        subcommands = {AnalyzeCommand.class, ServeCommand.class},
        description = "CodeMetric Studio command line"
)
public class RootCommand {
}
