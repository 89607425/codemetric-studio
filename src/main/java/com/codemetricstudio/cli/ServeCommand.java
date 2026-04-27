package com.codemetricstudio.cli;

import com.codemetricstudio.web.MetricWebServer;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "serve", mixinStandardHelpOptions = true, description = "Run all-in-one web app for upload + analysis + visualization")
public class ServeCommand implements Callable<Integer> {

    @CommandLine.Option(names = "--host", defaultValue = "127.0.0.1", description = "Bind host")
    private String host;

    @CommandLine.Option(names = "--port", defaultValue = "8080", description = "Bind port")
    private int port;

    @CommandLine.Option(names = "--web-root", defaultValue = "./web", description = "Web static files directory")
    private Path webRoot;

    @Override
    public Integer call() {
        try {
            MetricWebServer server = new MetricWebServer(host, port, webRoot);
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));

            System.out.println("CodeMetric Studio Web started");
            System.out.println("Open: http://" + host + ":" + port + "/");
            System.out.println("Press Ctrl+C to stop");

            Thread.currentThread().join();
            return 0;
        } catch (Exception ex) {
            System.err.println("Failed to start web server: " + ex.getMessage());
            return 1;
        }
    }
}
