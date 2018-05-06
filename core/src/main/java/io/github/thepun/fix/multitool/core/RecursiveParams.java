package io.github.thepun.fix.multitool.core;

import io.github.thepun.fix.multitool.Params;

import java.util.function.Function;

final class RecursiveParams implements Params {

    private final Params source;

    RecursiveParams(Params source) {
        this.source = source;
    }

    @Override
    public boolean hasParam(String name) {
        return recusrion(name, n -> true, propertyNotFound(name));
    }

    @Override
    public int getInt(String name) {
        return recusrion(name, source::getInt, propertyNotFound(name));
    }

    @Override
    public long getLong(String name) {
        return recusrion(name, source::getLong, propertyNotFound(name));
    }

    @Override
    public boolean getBoolean(String name) {
        return recusrion(name, source::getBoolean, propertyNotFound(name));
    }

    @Override
    public String getString(String name) {
        return recusrion(name, source::getString, propertyNotFound(name));
    }

    @Override
    public Params getSubgroup(String name) {
        return new SuffixedParams(name, this);
    }

    private <T> T recusrion(String name, Function<String, T> function, Function<Void, T> fallback) {
        if (source.hasParam(name)) {
            return function.apply(name);
        } else if (name.contains(".")) {
            name = name.substring(name.indexOf('.') + 1);
            return recusrion(name, function, fallback);
        } else {
            return fallback.apply(null);
        }
    }

    private <T> Function<Void, T> propertyNotFound(String name) {
        return (v) -> {
            throw new RuntimeException("Property " + name + " not found");
        };
    }
}
