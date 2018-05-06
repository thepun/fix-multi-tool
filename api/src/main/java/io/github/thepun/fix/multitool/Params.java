package io.github.thepun.fix.multitool;

public interface Params {

    boolean hasParam(String name);
    int getInt(String name);
    long getLong(String name);
    boolean getBoolean(String name);
    String getString(String name);
    Params getSubgroup(String name);

}
