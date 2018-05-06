package io.github.thepun.fix.multitool.core;

import io.github.thepun.fix.multitool.MultiToolCase;
import io.github.thepun.fix.multitool.Params;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CaseExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(CaseExecutor.class);


    private final Params params;
    private final MultiToolCase multiToolCase;

    public CaseExecutor(MultiToolCase multiToolCase, Params params) {
        this.multiToolCase = multiToolCase;
        this.params = params;
    }

    public void process(ManagedCaseContext managedCaseContext) {
        long timeout = params.getLong("timeout");

        CaseTask task = new CaseTask(multiToolCase);

        Thread thread = new Thread(task);
        thread.setName("runner");
        thread.setDaemon(true);

        managedCaseContext.start();
        thread.start();
        try {
            thread.join(timeout);
        } catch (InterruptedException e) {
            LOG.error("Interrupted during case", e);
            managedCaseContext.finish();
            Thread.currentThread().interrupt();
            System.exit(0);
            return;
        }

        managedCaseContext.finish();

        if (thread.isAlive()) {
            LOG.warn("Finished with timeout");
            System.exit(0);
        } else {
            if (task.exception != null) {
                LOG.warn("Finished with error: {}", task.exception.getMessage());
            } else {
                LOG.info("Finished");
            }
        }
    }


    private final class CaseTask implements Runnable {

        private final MultiToolCase multiToolCase;

        private Throwable exception;

        private CaseTask(MultiToolCase multiToolCase) {
            this.multiToolCase = multiToolCase;
        }

        @Override
        public void run() {
            try {
                multiToolCase.execute(params, new LoggerOutput());
            } catch (Throwable e) {
                LOG.error("Error during case run", e);
                exception = e;
            }
        }
    }
}
