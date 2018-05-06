package io.github.thepun.fix.multitool;

import io.github.thepun.fix.multitool.core.CaseExecutor;
import io.github.thepun.fix.multitool.core.CaseInitializer;
import io.github.thepun.fix.multitool.core.ParamConfigurer;
import io.github.thepun.fix.multitool.core.ManagedCaseContext;

public class MultiToolRunner {

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Wrong arguments. Expected <config path> <case name>");
        }

        // prepare config
        String configPath = args[0];
        ParamConfigurer paramConfigurer = new ParamConfigurer(configPath);
        paramConfigurer.prepareLogging();
        paramConfigurer.prepareConfig();

        // prepare case
        String name = args[1];
        CaseInitializer caseInitializer = new CaseInitializer(name, paramConfigurer.getParams());
        CaseExecutor caseExecutor = new CaseExecutor(caseInitializer.initialize(), paramConfigurer.getParams());

        // run
        ManagedCaseContext caseContext = new ManagedCaseContext();
        CaseContextHolder.setCaseContext(caseContext);
        caseExecutor.process(caseContext);
    }

}
