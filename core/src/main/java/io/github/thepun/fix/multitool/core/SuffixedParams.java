package io.github.thepun.fix.multitool.core;

import io.github.thepun.fix.multitool.Params;

final class SuffixedParams implements Params {

    private final String suffix;
    private final Params parent;

    SuffixedParams(String suffix, Params parent) {
        this.suffix = suffix;
        this.parent = parent;
    }

    @Override
    public boolean hasParam(String name) {
        return parent.hasParam(suffix + "." + name);
    }

    @Override
    public int getInt(String name) {
        return parent.getInt(suffix + "." + name);
    }

    @Override
    public long getLong(String name) {
        return parent.getLong(suffix + "." + name);
    }

    @Override
    public boolean getBoolean(String name) {
        return parent.getBoolean(suffix + "." + name);
    }

    @Override
    public String getString(String name) {
        return parent.getString(suffix + "." + name);
    }

    @Override
    public Params getSubgroup(String name) {
        return new SuffixedParams(name, this);
    }
}
