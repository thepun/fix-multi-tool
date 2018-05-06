package io.github.thepun.fix.multitool.core;

import io.github.thepun.fix.multitool.CaseContext;

import java.util.ArrayList;
import java.util.List;

public final class ManagedCaseContext implements CaseContext {

    private boolean started;
    private boolean finished;
    private final List<Runnable> runnables;

    public ManagedCaseContext() {
        runnables = new ArrayList<>();
    }

    @Override
    public synchronized boolean isRunning() {
        return started && !finished;
    }

    @Override
    public synchronized void runOnFinish(Runnable runnable) {
        if (!started) {
            throw new IllegalStateException("Case context is not started yet");
        }

        runnables.add(runnable);
    }

    @Override
    public synchronized void closeOnFinish(AutoCloseable closeable) {
        if (!started) {
            throw new IllegalStateException("Case context is not started yet");
        }

        runnables.add(() -> {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        });
    }

    synchronized void start() {
        if (started) {
            throw new IllegalStateException("Case context is already started");
        }

        started = true;
    }

    synchronized void finish() {
        if (!started) {
            throw new IllegalStateException("Case context is not started");
        }

        if (finished) {
            return;
        }

        finished = true;

        for (Runnable runnable : runnables) {
            try {
                runnable.run();
            } catch (Exception ignored) {
            }
        }
    }
}
