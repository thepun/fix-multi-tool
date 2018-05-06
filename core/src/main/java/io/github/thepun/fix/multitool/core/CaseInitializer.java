package io.github.thepun.fix.multitool.core;

import io.github.thepun.fix.multitool.MultiToolCase;
import io.github.thepun.fix.multitool.Params;
import io.github.thepun.fix.multitool.Property;

import java.lang.reflect.Field;

public final class CaseInitializer {

    private final Params params;
    private final String multiToolCaseName;

    public CaseInitializer(String multiToolCaseName, Params params) {
        this.multiToolCaseName = multiToolCaseName;
        this.params = params;
    }

    public MultiToolCase initialize() {
        Class<? extends MultiToolCase> result;
        String fullClassName;
        if (multiToolCaseName.contains(".")) {
            fullClassName = multiToolCaseName;
        } else {
            fullClassName = "io.github.thepun.fix.multitool.cases." + multiToolCaseName;
        }

        try {
            result = (Class<? extends MultiToolCase>) Class.forName(fullClassName);
        } catch (ClassNotFoundException e1) {
            throw new RuntimeException("Failed to load case class", e1);
        }
        Class<? extends MultiToolCase> multiToolCaseClass = result;

        MultiToolCase multiToolCase;
        try {
            multiToolCase = multiToolCaseClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize case", e);
        }

        for (Field field : multiToolCaseClass.getDeclaredFields()) {
            Property annotation = field.getAnnotation(Property.class);
            if (annotation != null) {
                String name = annotation.value();
                if (name.isEmpty()) {
                    name = field.getName();
                }

                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }

                try {
                    if (field.getType().isPrimitive()) {
                        if (!params.hasParam(name)) {
                            throw new RuntimeException("Property " + name + " not found");
                        }

                        if (field.getType() == int.class) {
                            field.setInt(multiToolCase, params.getInt(name));
                        } else if (field.getType() == long.class) {
                            field.setLong(multiToolCase, params.getLong(name));
                        } else if (field.getType() == boolean.class) {
                            field.setBoolean(multiToolCase, params.getBoolean(name));
                        } else {
                            throw new RuntimeException("Unsupported property type: " + field.getType());
                        }
                    } else {
                        if (!params.hasParam(name)) {
                            continue;
                        }

                        if (field.getType() == Integer.class) {
                            field.setInt(multiToolCase, params.getInt(name));
                        } else if (field.getType() == Long.class) {
                            field.setLong(multiToolCase, params.getLong(name));
                        } else if (field.getType() == Boolean.class) {
                            field.setBoolean(multiToolCase, params.getBoolean(name));
                        } else if (field.getType() == String.class) {
                            field.set(multiToolCase, params.getString(name));
                        } else {
                            throw new RuntimeException("Unsupported property type: " + field.getType());
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to set field " + field.getName(), e);
                }
            }
        }

        return multiToolCase;
    }
}
