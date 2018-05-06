package io.github.thepun.fix.multitool.client.fix;

import quickfix.SessionID;

import java.util.concurrent.atomic.AtomicInteger;

final class FixSessionID {

    private static final AtomicInteger COUNTER = new AtomicInteger();


    private final String name;
    private final SessionID sessionID;

    FixSessionID(SessionID sessionID) {
        this.sessionID = sessionID;

        name = "fix-session-" + COUNTER.incrementAndGet();
    }

    public String getName() {
        return name;
    }

    public SessionID getSessionID() {
        return sessionID;
    }

    @Override
    public String toString() {
        return name;
    }
}
