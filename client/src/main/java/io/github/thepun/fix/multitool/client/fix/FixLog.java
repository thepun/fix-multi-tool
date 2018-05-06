package io.github.thepun.fix.multitool.client.fix;

import org.slf4j.Logger;
import quickfix.Log;
import quickfix.LogFactory;
import quickfix.SessionID;

final class FixLog implements Log, LogFactory {

    private final Logger logger;
    private final FixSessionID sessionID;

    FixLog(Logger logger, FixSessionID sessionID) {
        this.logger = logger;
        this.sessionID = sessionID;
    }

    @Override
    public void onIncoming(String message) {
        logger.debug("Incoming message in {}: {}", sessionID, message.replace('\u0001', '|'));
    }

    @Override
    public void onOutgoing(String message) {
        logger.debug("Outgoing message in {}: {}", sessionID, message.replace('\u0001', '|'));
    }

    @Override
    public void onEvent(String text) {
        logger.debug("Event in {}: {}", sessionID, text);
    }

    @Override
    public void onErrorEvent(String text) {
        logger.debug("Error event in {}: {}", sessionID, text);
    }

    @Override
    public void clear() {
    }

    @Override
    public Log create(SessionID sessionID) {
        return this;
    }
}
