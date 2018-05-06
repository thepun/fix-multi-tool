package io.github.thepun.fix.multitool.client.fix;

import io.github.thepun.fix.multitool.Params;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Application;
import quickfix.DefaultMessageFactory;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.NoopStoreFactory;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.ThreadedSocketAcceptor;
import quickfix.UnsupportedMessageType;
import quickfix.field.Password;
import quickfix.field.Username;
import quickfix.fix44.Logon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static quickfix.Acceptor.SETTING_SOCKET_ACCEPT_ADDRESS;
import static quickfix.Acceptor.SETTING_SOCKET_ACCEPT_PORT;
import static quickfix.Session.SETTING_ALLOW_UNKNOWN_MSG_FIELDS;
import static quickfix.Session.SETTING_END_DAY;
import static quickfix.Session.SETTING_END_TIME;
import static quickfix.Session.SETTING_HEARTBTINT;
import static quickfix.Session.SETTING_LOGON_TIMEOUT;
import static quickfix.Session.SETTING_LOGOUT_TIMEOUT;
import static quickfix.Session.SETTING_START_DAY;
import static quickfix.Session.SETTING_START_TIME;
import static quickfix.Session.SETTING_TIMEZONE;
import static quickfix.Session.SETTING_VALIDATE_FIELDS_OUT_OF_ORDER;
import static quickfix.Session.SETTING_VALIDATE_INCOMING_MESSAGE;
import static quickfix.Session.SETTING_VALIDATE_UNORDERED_GROUP_FIELDS;
import static quickfix.Session.SETTING_VALIDATE_USER_DEFINED_FIELDS;
import static quickfix.SessionFactory.ACCEPTOR_CONNECTION_TYPE;
import static quickfix.SessionFactory.SETTING_CONNECTION_TYPE;

final class FixAcceptorConnection implements FixConnection, Application {

    private static final Logger LOG = LoggerFactory.getLogger(FixConnection.class);


    private final int timeout;
    private final String user;
    private final String password;

    private final Object lock;
    private final List<Message> messages;
    private final CountDownLatch readyLatch;
    private final FixSessionID currentSession;
    private final ThreadedSocketAcceptor threadedSocketAcceptor;

    private boolean logon;
    private boolean logout;
    private boolean active;

    FixAcceptorConnection(Params params) {
        lock = new Object();

        synchronized (lock) {
            messages = new ArrayList<>();
            readyLatch = new CountDownLatch(1);

            timeout = params.getInt("timeout");
            user = params.getString("username");
            password = params.getString("password");

            String beginString = params.getString("beginString");
            String senderCompId = params.getString("senderCompId");
            String senderSubId = params.getString("senderSubId");
            String targetCompId = params.getString("targetCompId");
            String targetSubId = params.getString("targetSubId");
            currentSession = new FixSessionID(new SessionID(beginString, senderCompId, senderSubId, targetCompId, targetSubId));

            active = true;
        }

        SessionSettings sessionSettings = new SessionSettings();
        sessionSettings.setString(currentSession.getSessionID(), SETTING_CONNECTION_TYPE, ACCEPTOR_CONNECTION_TYPE);
        sessionSettings.setString(currentSession.getSessionID(), SETTING_START_DAY, params.getString("startDay"));
        sessionSettings.setString(currentSession.getSessionID(), SETTING_START_TIME, params.getString("startTime"));
        sessionSettings.setString(currentSession.getSessionID(), SETTING_END_DAY, params.getString("endDay"));
        sessionSettings.setString(currentSession.getSessionID(), SETTING_END_TIME, params.getString("endTime"));
        sessionSettings.setString(currentSession.getSessionID(), SETTING_TIMEZONE, params.getString("timeZone"));
        sessionSettings.setString(currentSession.getSessionID(), SETTING_SOCKET_ACCEPT_ADDRESS, params.getString("host"));
        sessionSettings.setLong(currentSession.getSessionID(), SETTING_SOCKET_ACCEPT_PORT, params.getInt("port"));
        sessionSettings.setLong(currentSession.getSessionID(), SETTING_LOGON_TIMEOUT, timeout / 1000);
        sessionSettings.setLong(currentSession.getSessionID(), SETTING_LOGOUT_TIMEOUT, timeout / 1000);
        sessionSettings.setBool(currentSession.getSessionID(), SETTING_ALLOW_UNKNOWN_MSG_FIELDS, true);
        sessionSettings.setBool(currentSession.getSessionID(), SETTING_VALIDATE_FIELDS_OUT_OF_ORDER, false);
        sessionSettings.setBool(currentSession.getSessionID(), SETTING_VALIDATE_UNORDERED_GROUP_FIELDS, false);
        sessionSettings.setBool(currentSession.getSessionID(), SETTING_VALIDATE_USER_DEFINED_FIELDS, false);
        sessionSettings.setBool(currentSession.getSessionID(), SETTING_VALIDATE_INCOMING_MESSAGE, false);
        sessionSettings.setLong(currentSession.getSessionID(), SETTING_HEARTBTINT, 30);

        FixLog log = new FixLog(LOG, currentSession);
        NoopStoreFactory storeFactory = new NoopStoreFactory();
        DefaultMessageFactory messageFactory = new DefaultMessageFactory();

        try {
            threadedSocketAcceptor = new ThreadedSocketAcceptor(this, storeFactory, sessionSettings, log, messageFactory, Integer.MAX_VALUE);
            threadedSocketAcceptor.start();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create fix connection", e);
        }
    }

    @Override
    public boolean isReady() {
        synchronized (lock) {
            return active && logon && !logout && currentSession != null;
        }
    }

    @Override
    public boolean isLogon() {
        synchronized (lock) {
            return logon;
        }
    }

    @Override
    public boolean isLogout() {
        synchronized (lock) {
            return logout;
        }
    }

    @Override
    public void skipAllPending() {
        synchronized (lock) {
            messages.clear();
        }
    }

    @Override
    public void write(Message message) {
        synchronized (this) {
            ensureReady();
        }

        try {
            Session.sendToTarget(message, currentSession.getSessionID());
        } catch (SessionNotFound sessionNotFound) {
            throw new IllegalStateException("Unknown session");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Message> T read(Class<T> messageType) {
        long start = System.currentTimeMillis();
        long time = start;
        while (time - start < timeout) {
            synchronized (this) {
                ensureReady();

                Iterator<Message> iterator = messages.iterator();
                while (iterator.hasNext()) {
                    Message message = iterator.next();
                    if (messageType.isInstance(message)) {
                        iterator.remove();
                        return (T) message;
                    }
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }

            time = System.currentTimeMillis();
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Message> List<T> readAll(Class<T> messageType) {
        List<T> messagesToReturn = new ArrayList<>();

        synchronized (this) {
            ensureReady();

            Iterator<Message> iterator = messages.iterator();
            while (iterator.hasNext()) {
                Message message = iterator.next();
                if (messageType.isInstance(message)) {
                    iterator.remove();
                    messagesToReturn.add((T) message);
                }
            }
        }

        return messagesToReturn;
    }

    @Override
    public void close() {
        LOG.info("Stopping {}", currentSession);

        synchronized (lock) {
            active = false;
            threadedSocketAcceptor.stop(true);
        }
    }

    @Override
    public void onCreate(SessionID sessionId) {
        LOG.info("Starting {}", currentSession);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        LOG.info("Logon {}", currentSession);

        synchronized (lock) {
            logon = true;
            readyLatch.countDown();
        }
    }

    @Override
    public void onLogout(SessionID sessionId) {
        LOG.info("Logout {}", currentSession);

        synchronized (lock) {
            logout = true;
            close();
        }
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        if (message instanceof Logon) {
            if (!user.isEmpty()) {
                message.setString(Username.FIELD, user);
            }
            if (!password.isEmpty()) {
                message.setString(Password.FIELD, password);
            }
        }

        LOG.debug("To admin {}: {}", currentSession, message.getClass().getName());
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        LOG.debug("From admin {}: {}", currentSession, message.getClass().getName());

        synchronized (lock) {
            messages.add(message);
        }
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        LOG.debug("To app {}: {}", currentSession, message.getClass().getName());
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        LOG.debug("From app {}: {}", currentSession, message.getClass().getName());

        synchronized (lock) {
            messages.add(message);
        }
    }

    void waitUntilReady() {
        long start = System.currentTimeMillis();
        long time = start;
        while (time - start < timeout) {
            synchronized (lock) {
                if (!active) {
                    return;
                }
            }

            try {
                if (readyLatch.await(100, TimeUnit.MILLISECONDS)) {
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Failed to wait for connection", e);
            }

            time = System.currentTimeMillis();
        }
    }

    private void ensureReady() {
        if (!isReady()) {
            throw new IllegalStateException("Session is not ready");
        }
    }

}

