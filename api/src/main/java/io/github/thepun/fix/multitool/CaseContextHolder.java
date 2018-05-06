package io.github.thepun.fix.multitool;

final class CaseContextHolder {

    private static CaseContext caseContext;

    static synchronized CaseContext getCaseContext() {
        if (caseContext == null) {
            throw new IllegalStateException("Case context is not initialized");
        }

        return caseContext;
    }

    static synchronized void setCaseContext(CaseContext newCaseContext) {
        if (caseContext != null) {
            throw new IllegalStateException("Case context is already initialized");
        }

        caseContext = newCaseContext;
    }

}
