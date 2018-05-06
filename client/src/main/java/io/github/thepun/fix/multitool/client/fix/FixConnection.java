package io.github.thepun.fix.multitool.client.fix;

import io.github.thepun.fix.multitool.CaseContext;
import io.github.thepun.fix.multitool.Params;
import io.github.thepun.fix.multitool.client.Connection;
import quickfix.Message;

import java.util.List;

public interface FixConnection extends Connection {

    boolean isReady();
    boolean isLogon();
    boolean isLogout();

    void skipAllPending();
    void write(Message message);
    <T extends Message> T read(Class<T> messageType);
    <T extends Message> List<T> readAll(Class<T> messageType);

    static FixConnection accept(Params params) {
        FixAcceptorConnection session = new FixAcceptorConnection(params);
        CaseContext.get().runOnFinish(session::close);
        session.waitUntilReady();
        return session;
    }

    static FixConnection connect(Params params) {
        FixInitiatorConnection session = new FixInitiatorConnection(params);
        CaseContext.get().runOnFinish(session::close);
        session.waitUntilReady();
        return session;
    }
}
