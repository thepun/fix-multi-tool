package io.github.thepun.fix.multitool.core;

import io.github.thepun.fix.multitool.Params;

import java.util.Properties;

final class ParamsFromProperties implements Params {

    private final Properties properties;

    ParamsFromProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public boolean hasParam(String name) {
        return properties.containsKey(name);
    }

    @Override
    public int getInt(String name) {
        String property = notNullProperty(name);

        int value;
        try {
            value = Integer.parseInt(property);
        } catch (Exception e) {
            throw new RuntimeException("Property " + name + " with value " + property + " is not an int");
        }

        return value;
    }

    @Override
    public long getLong(String name) {
        String property = notNullProperty(name);

        long value;
        try {
            value = Long.parseLong(property);
        } catch (Exception e) {
            throw new RuntimeException("Property " + name + " with value " + property + " is not an long");
        }

        return value;
    }

    @Override
    public boolean getBoolean(String name) {
        String property = notNullProperty(name);

        boolean value;
        try {
            value = Boolean.parseBoolean(property);
        } catch (Exception e) {
            throw new RuntimeException("Property " + name + " with value " + property + " is not a boolean");
        }

        return value;
    }

    @Override
    public String getString(String name) {
        return notNullProperty(name);
    }

    @Override
    public Params getSubgroup(String name) {
        return new SuffixedParams(name, this);
    }

    private String notNullProperty(String name) {
        String property = properties.getProperty(name);
        if (property == null) {
            throw new RuntimeException("Property " + name + " not found");
        }
        return property;
    }
}
