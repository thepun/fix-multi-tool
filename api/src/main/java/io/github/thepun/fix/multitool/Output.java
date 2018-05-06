package io.github.thepun.fix.multitool;

public interface Output {

    void print(String text);
    void print(String text, Object ... args);

    void printToFile(String text);
    void printToFile(String text, Object ... args);

}
