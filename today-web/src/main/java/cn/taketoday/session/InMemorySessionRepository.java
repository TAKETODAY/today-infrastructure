/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.session;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.io.WriteAbortedException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.util.StringUtils;

/**
 * Memory based {@link SessionRepository}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-09-28 10:31
 */
public class InMemorySessionRepository implements SessionRepository {
  private static final Logger log = LoggerFactory.getLogger(InMemorySessionRepository.class);

  private int maxSessions = 10000;

  private Clock clock = Clock.systemUTC();

  /**
   * When an attribute that is already present in the session is added again
   * under the same name and the attribute implements {@link
   * AttributeBindingListener}, should
   * {@link AttributeBindingListener#valueUnbound(WebSession, String)} )}
   * be called followed by
   * {@link AttributeBindingListener#valueBound(WebSession, String)}
   * <p>
   * The default value is {@code false}.
   * <p>
   * {@code true} if the listener will be notified, {@code false} if
   * it will not
   */
  private boolean notifyBindingListenerOnUnchangedValue;

  /**
   * @see #setNotifyAttributeListenerOnUnchangedValue(boolean)
   */
  private boolean notifyAttributeListenerOnUnchangedValue = true;

  private Duration maxIdleTime = Duration.ofMinutes(30);

  private final SessionIdGenerator idGenerator;

  private final SessionEventDispatcher eventDispatcher;

  private final ExpiredSessionChecker expiredSessionChecker = new ExpiredSessionChecker();

  private final ConcurrentHashMap<String, InMemoryWebSession> sessions = new ConcurrentHashMap<>();

  public InMemorySessionRepository(SessionEventDispatcher eventDispatcher, SessionIdGenerator idGenerator) {
    Assert.notNull(idGenerator, "SessionIdGenerator is required");
    Assert.notNull(eventDispatcher, "SessionEventDispatcher is required");
    this.idGenerator = idGenerator;
    this.eventDispatcher = eventDispatcher;
  }

  /**
   * Set the maximum number of sessions that can be stored. Once the limit is
   * reached, any attempt to store an additional session will result in an
   * {@link IllegalStateException}.
   * <p>By default set to 10000.
   *
   * @param maxSessions the maximum number of sessions
   * @since 4.0
   */
  public void setMaxSessions(int maxSessions) {
    this.maxSessions = maxSessions;
  }

  /**
   * Return the maximum number of sessions that can be stored.
   *
   * @since 4.0
   */
  public int getMaxSessions() {
    return this.maxSessions;
  }

  /**
   * Set the duration of session idle timeout
   *
   * @param timeout the duration of session idle timeout
   * @since 4.0
   */
  public void setSessionMaxIdleTime(@Nullable Duration timeout) {
    this.maxIdleTime = timeout == null ? Duration.ofMinutes(30) : timeout;
  }

  /**
   * When an attribute that is already present in the session is added again
   * under the same name and the attribute implements {@link
   * AttributeBindingListener}, should
   * {@link AttributeBindingListener#valueUnbound(WebSession, String)}
   * be called followed by
   * {@link AttributeBindingListener#valueBound(WebSession, String)}
   * <p>
   * The default value is {@code false}.
   * <p>
   *
   * @param notifyBindingListenerOnUnchangedValue {@code true} if the listener will be notified,
   * {@code false} if it will not
   * @since 4.0
   */
  public void setNotifyBindingListenerOnUnchangedValue(boolean notifyBindingListenerOnUnchangedValue) {
    this.notifyBindingListenerOnUnchangedValue = notifyBindingListenerOnUnchangedValue;
  }

  /**
   * When an attribute that is already present in the session is added again
   * under the same name and a {@link
   * WebSessionAttributeListener} is configured for the
   * session should
   * {@link WebSessionAttributeListener#attributeReplaced(WebSession, String, Object, Object)}
   * be called?
   * <p>
   * The default value is {@code true}.
   *
   * @param notifyAttributeListenerOnUnchangedValue {@code true} if the listener will be
   * notified, {@code false} if it will not
   * @since 4.0
   */
  public void setNotifyAttributeListenerOnUnchangedValue(boolean notifyAttributeListenerOnUnchangedValue) {
    this.notifyAttributeListenerOnUnchangedValue = notifyAttributeListenerOnUnchangedValue;
  }

  /**
   * Configure the {@link Clock} to use to set lastAccessTime on every created
   * session and to calculate if it is expired.
   * <p>This may be useful to align to different timezone or to set the clock
   * back in a test, e.g. {@code Clock.offset(clock, Duration.ofMinutes(-31))}
   * in order to simulate session expiration.
   * <p>By default this is {@code Clock.system(ZoneId.of("GMT"))}.
   *
   * @param clock the clock to use
   * @since 4.0
   */
  public void setClock(Clock clock) {
    Assert.notNull(clock, "Clock is required");
    this.clock = clock;
    removeExpiredSessions();
  }

  /**
   * Return the configured clock for session lastAccessTime calculations.
   *
   * @since 4.0
   */
  public Clock getClock() {
    return this.clock;
  }

  @Override
  public int getSessionCount() {
    return sessions.size();
  }

  @Override
  public String[] getIdentifiers() {
    return StringUtils.toStringArray(sessions.keySet());
  }

  /**
   * Return the map of sessions with an {@link Collections#unmodifiableMap
   * unmodifiable} wrapper. This could be used for management purposes, to
   * list active sessions, invalidate expired ones, etc.
   */
  public Map<String, WebSession> getSessions() {
    return Collections.unmodifiableMap(this.sessions);
  }

  @Override
  public WebSession createSession() {
    return createSession(idGenerator.generateId());
  }

  @Override
  public WebSession createSession(String id) {
    // Opportunity to clean expired sessions
    Instant now = clock.instant();
    expiredSessionChecker.checkIfNecessary(now);
    return new InMemoryWebSession(id, now, maxIdleTime);
  }

  @Override
  public WebSession retrieveSession(String id) {
    Instant now = clock.instant();
    expiredSessionChecker.checkIfNecessary(now);

    InMemoryWebSession session = sessions.get(id);
    if (session == null) {
      return null;
    }
    else if (session.isExpired(now)) {
      sessions.remove(id);
      return null;
    }
    else {
      session.lastAccessTime = now;
      return session;
    }
  }

  @Override
  public WebSession removeSession(String id) {
    return sessions.remove(id);
  }

  @Override
  public void updateLastAccessTime(WebSession session) {
    session.setLastAccessTime(clock.instant());
  }

  @Override
  public boolean contains(String id) {
    return sessions.containsKey(id);
  }

  /**
   * Check for expired sessions and remove them. Typically such checks are
   * kicked off lazily during calls to {@link #createSession() create} or
   * {@link #retrieveSession retrieve}, no less than 60 seconds apart.
   * This method can be called to force a check at a specific time.
   *
   * @since 4.0
   */
  public void removeExpiredSessions() {
    expiredSessionChecker.removeExpiredSessions(clock.instant());
  }

  final class InMemoryWebSession extends AbstractWebSession implements WebSession, Serializable, SerializableSession {

    @Serial
    private static final long serialVersionUID = 1L;

    private Instant creationTime;

    private volatile Duration maxIdleTime;

    private volatile Instant lastAccessTime;

    private final AtomicReference<String> id;

    private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

    InMemoryWebSession(String id, Instant creationTime, Duration maxIdleTime) {
      super(InMemorySessionRepository.this.eventDispatcher);
      this.id = new AtomicReference<>(id);
      this.maxIdleTime = maxIdleTime;
      this.creationTime = creationTime;
      this.lastAccessTime = this.creationTime;
    }

    @Override
    public String getId() {
      return id.get();
    }

    @Override
    public Instant getCreationTime() {
      return creationTime;
    }

    @Override
    public Instant getLastAccessTime() {
      return lastAccessTime;
    }

    @Override
    public void setLastAccessTime(Instant lastAccessTime) {
      this.lastAccessTime = lastAccessTime;
    }

    @Override
    public void changeSessionId() {
      sessions.remove(getId());
      String newId = idGenerator.generateId();
      id.set(newId);
      sessions.put(newId, this);
    }

    @Override
    protected void doInvalidate() {
      state.set(State.EXPIRED);
      sessions.remove(getId());
    }

    @Override
    public void save() {
      checkMaxSessionsLimit();

      // Implicitly started session..
      if (hasAttributes()) {
        state.compareAndSet(State.NEW, State.STARTED);
      }
      if (isStarted()) {
        // Save
        sessions.put(getId(), this);

        // Unless it was invalidated
        if (state.get().equals(State.EXPIRED)) {
          sessions.remove(getId());
          throw new IllegalStateException("Session was invalidated");
        }
      }
    }

    @Override
    public void setMaxIdleTime(Duration maxIdleTime) {
      this.maxIdleTime = maxIdleTime;
    }

    @Override
    public Duration getMaxIdleTime() {
      return this.maxIdleTime;
    }

    /**
     * Force the creation of a session causing the session id to be sent when
     * {@link #save()} is called.
     */
    @Override
    public void start() {
      state.compareAndSet(State.NEW, State.STARTED);
      eventDispatcher.onSessionCreated(this);
    }

    /**
     * Whether a session with the client has been started explicitly via
     * {@link #start()} or implicitly by adding session attributes.
     * If "false" then the session id is not sent to the client and the
     * {@link #save()} method is essentially a no-op.
     */
    @Override
    public boolean isStarted() {
      return state.get().equals(State.STARTED) || attributes != null;
    }

    @Override
    protected boolean attributeBinding(Object value, @Nullable Object oldValue) {
      return oldValue != value || notifyBindingListenerOnUnchangedValue;
    }

    @Override
    protected boolean allowAttributeReplaced(Object value, @Nullable Object oldValue) {
      return value != oldValue || notifyAttributeListenerOnUnchangedValue;
    }

    private void checkMaxSessionsLimit() {
      if (sessions.size() >= maxSessions) {
        expiredSessionChecker.removeExpiredSessions(clock.instant());
        if (sessions.size() >= maxSessions) {
          throw new TooManyActiveSessionsException(
                  "Max sessions limit reached: " + sessions.size(), maxSessions);
        }
      }
    }

    @Override
    public boolean isExpired() {
      return isExpired(clock.instant());
    }

    private boolean isExpired(Instant now) {
      if (state.get().equals(State.EXPIRED)) {
        return true;
      }
      if (checkExpired(now)) {
        state.set(State.EXPIRED);
        return true;
      }
      return false;
    }

    private boolean checkExpired(Instant currentTime) {
      return isStarted()
              && !maxIdleTime.isNegative()
              && currentTime.minus(maxIdleTime).isAfter(lastAccessTime);
    }

    // Serializable

    @Override
    public void writeObjectData(ObjectOutputStream stream) throws IOException {
      stream.writeObject(creationTime);
      stream.writeObject(lastAccessTime);
      stream.writeObject(maxIdleTime);
      stream.writeObject(state.get());

      if (attributes != null) {
        if (log.isDebugEnabled()) {
          log.debug("writeObject() [{}]", attributes);
        }

        // Accumulate the names of serializable and non-serializable attributes
        ArrayList<String> saveNames = new ArrayList<>();
        ArrayList<Object> saveValues = new ArrayList<>();

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
          String key = entry.getKey();
          Object value = entry.getValue();
          if (value instanceof Serializable) {
            saveNames.add(key);
            saveValues.add(value);
          }
        }

        // Serialize the attribute count and the Serializable attributes
        int n = saveNames.size();
        stream.writeInt(n);
        for (int i = 0; i < n; i++) {
          String name = saveNames.get(i);
          stream.writeObject(name);
          try {
            Object object = saveValues.get(i);
            stream.writeObject(object);
            LogFormatUtils.traceDebug(log, traceOn -> LogFormatUtils.formatValue("  storing attribute '%s' with value '%s'"
                    .formatted(name, object), !traceOn));
          }
          catch (NotSerializableException e) {
            log.warn("Cannot serialize session attribute [{}] for session [{}]",
                    name, id, e);
          }
        }
      }
      else {
        // size
        stream.writeInt(0);
      }
    }

    @Override
    public void readObjectData(ObjectInputStream stream) throws ClassNotFoundException, IOException {
      creationTime = (Instant) stream.readObject();
      lastAccessTime = (Instant) stream.readObject();
      maxIdleTime = (Duration) stream.readObject();
      state.set((State) stream.readObject());

      if (log.isDebugEnabled()) {
        log.debug("readObject() loading session {}", id);
      }

      int size = stream.readInt();
      if (size > 0) {
        // Deserialize the attribute count and attribute values
        if (attributes == null) {
          attributes = new HashMap<>();
        }
        for (int i = 0; i < size; i++) {
          String name = (String) stream.readObject();
          final Object value;
          try {
            value = stream.readObject();
          }
          catch (WriteAbortedException wae) {
            if (wae.getCause() instanceof NotSerializableException) {
              if (log.isDebugEnabled()) {
                log.debug("Cannot deserialize session attribute [{}] for session [{}]", name, id, wae);
              }
              else {
                log.warn("Cannot deserialize session attribute [{}] for session [{}]", name, id);
              }
              // Skip non serializable attributes
              continue;
            }
            throw wae;
          }
          attributes.put(name, value);
          LogFormatUtils.traceDebug(log, traceOn -> LogFormatUtils.formatValue("  loading attribute '%s' with value '%s'"
                  .formatted(name, value), !traceOn));
        }
      }

      // Save
      sessions.put(getId(), this);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      InMemoryWebSession that = (InMemoryWebSession) o;
      return Objects.equals(id.get(), that.id.get())
              && Objects.equals(state.get(), that.state.get())
              && Objects.equals(creationTime, that.creationTime)
              && Objects.equals(maxIdleTime, that.maxIdleTime)
              && Objects.equals(lastAccessTime, that.lastAccessTime);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), id, creationTime, lastAccessTime, maxIdleTime, state);
    }
  }

  private final class ExpiredSessionChecker {

    /** Max time between expiration checks. */
    private static final int CHECK_PERIOD = 60 * 1000;

    private final ReentrantLock lock = new ReentrantLock();

    private Instant checkTime = clock.instant().plus(CHECK_PERIOD, ChronoUnit.MILLIS);

    public void checkIfNecessary(Instant now) {
      if (checkTime.isBefore(now)) {
        removeExpiredSessions(now);
      }
    }

    public void removeExpiredSessions(Instant now) {
      if (!sessions.isEmpty()) {
        if (lock.tryLock()) {
          try {
            Iterator<InMemoryWebSession> iterator = sessions.values().iterator();
            while (iterator.hasNext()) {
              InMemoryWebSession session = iterator.next();
              if (session.isExpired(now)) {
                iterator.remove();
                session.invalidate();
              }
            }
          }
          finally {
            this.checkTime = now.plus(CHECK_PERIOD, ChronoUnit.MILLIS);
            lock.unlock();
          }
        }
      }
    }
  }

  private enum State {
    NEW, STARTED, EXPIRED
  }

}
