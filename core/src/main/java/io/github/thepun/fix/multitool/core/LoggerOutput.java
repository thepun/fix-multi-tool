package io.github.thepun.fix.multitool.core;

import io.github.thepun.fix.multitool.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LoggerOutput implements Output {

    private final Logger logger;
    private final Logger loggerToFile;

    LoggerOutput() {
        logger = LoggerFactory.getLogger("OUT");
        loggerToFile = LoggerFactory.getLogger("OUT_FILE");
    }

    @Override
    public void print(String text) {
        logger.info(text);
    }

    @Override
    public void print(String text, Object... args) {
        logger.info(text, args);
    }

    @Override
    public void printToFile(String text) {
        loggerToFile.info(text);
    }

    @Override
    public void printToFile(String text, Object... args) {
        loggerToFile.info(text, args);
    }
}
