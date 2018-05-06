package io.github.thepun.fix.multitool.core;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import io.github.thepun.fix.multitool.Params;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public final class ParamConfigurer {

    private final String paramPath;

    private Params params;

    public ParamConfigurer(String paramPath) {
        this.paramPath = paramPath;
    }

    public Params getParams() {
        if (params == null) {
            throw new IllegalStateException("Params not prepared");
        }

        return params;
    }

    public void prepareLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();
            configurator.doConfigure(paramPath + "/logback.xml");
        } catch (JoranException je) {
            // StatusPrinter will handle this
        }
    }

    public void prepareConfig() {
        File file = new File(paramPath + "/multitool.properties");
        if (!file.exists()) {
            throw new IllegalStateException("Failed to find properties file " + file.getAbsolutePath());
        }

        Properties properties = new Properties(System.getProperties());
        try {
            properties.load(new FileInputStream(file));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read properties file " + file.getAbsolutePath(), e);
        }

        ParamsFromProperties paramsFromProperties = new ParamsFromProperties(properties);
        RecursiveParams recursiveParams = new RecursiveParams(paramsFromProperties);
        params = recursiveParams;
    }
}
