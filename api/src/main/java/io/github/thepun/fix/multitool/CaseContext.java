package io.github.thepun.fix.multitool;

public interface CaseContext {

    boolean isRunning();

    void runOnFinish(Runnable runnable);
    void closeOnFinish(AutoCloseable closeable);

    static CaseContext get() {
       return CaseContextHolder.getCaseContext();
    }

}
