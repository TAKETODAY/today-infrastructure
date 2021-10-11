/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.orm.hibernate5;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.spi.AbstractCurrentSessionContext;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.transaction.Synchronization;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * @author TODAY <br>
 * 2018-09-18 13:17
 */
@SuppressWarnings("all")
public final class HibernateSessionContext extends AbstractCurrentSessionContext {
  private static final Logger log = LoggerFactory.getLogger(HibernateSessionContext.class);

  private static final long serialVersionUID = 8140326831060650585L;

  private static final Class<?>[] SESSION_PROXY_INTERFACES
          = new Class[] { Session.class, SessionImplementor.class, EventSource.class, LobCreationContext.class };

  private static final ThreadLocal<Map> CONTEXT_TL = new ThreadLocal<Map>();

  /**
   * Constructs a ThreadLocal
   *
   * @param factory
   *         The factory this context will service
   */
  public HibernateSessionContext(SessionFactoryImplementor factory) {
    super(factory);
  }

  @Override
  public final Session currentSession() throws HibernateException {
    Session current = existingSession(factory());
    if (current == null) {
      current = buildOrObtainSession();
      // register a cleanup sync
      current.getTransaction().registerSynchronization(buildCleanupSynch());
      // wrap the session in the transaction-protection proxy
      if (needsWrapping(current)) {
        current = wrap(current);
      }
      // then bind it
      doBind(current, factory());
//		} else {
//			validateExistingSession(current);
    }
    return current;
  }

  private boolean needsWrapping(Session session) {
    // try to make sure we don't wrap and already wrapped session
    if (session != null) {
      if (Proxy.isProxyClass(session.getClass())) {
        final InvocationHandler invocationHandler = Proxy.getInvocationHandler(session);
        if (invocationHandler != null && TransactionProtectionWrapper.class.isInstance(invocationHandler)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Strictly provided for sub-classing purposes; specifically to allow
   * long-session support.
   * <p/>
   * This implementation always just opens a new session.
   *
   * @return the built or (re)obtained session.
   */
  protected Session buildOrObtainSession() {
    return baseSessionBuilder().autoClose(isAutoCloseEnabled()).connectionReleaseMode(getConnectionReleaseMode())
            .flushBeforeCompletion(isAutoFlushEnabled()).openSession();
  }

  protected CleanupSync buildCleanupSynch() {
    return new CleanupSync(factory());
  }

  /**
   * Mainly for subclass usage. This impl always returns true.
   *
   * @return Whether or not the the session should be closed by transaction
   * completion.
   */
  protected boolean isAutoCloseEnabled() {
    return true;
  }

  /**
   * Mainly for subclass usage. This impl always returns true.
   *
   * @return Whether or not the the session should be flushed prior transaction
   * completion.
   */
  protected boolean isAutoFlushEnabled() {
    return true;
  }

  /**
   * Mainly for subclass usage. This impl always returns after_transaction.
   *
   * @return The connection release mode for any built sessions.
   */
  protected ConnectionReleaseMode getConnectionReleaseMode() {
    return factory().getSettings().getConnectionReleaseMode();
  }

  protected Session wrap(Session session) {
    final TransactionProtectionWrapper wrapper = new TransactionProtectionWrapper(session);
    final Session wrapped = (Session) Proxy.newProxyInstance(
            Session.class.getClassLoader(), SESSION_PROXY_INTERFACES, wrapper);
    // yick! need this for proper serialization/deserialization handling...
    wrapper.setWrapped(wrapped);
    return wrapped;
  }

  /**
   * Associates the given session with the current thread of execution.
   *
   * @param session
   *         The session to bind.
   */
  public static void bind(Session session) {
    final SessionFactory factory = session.getSessionFactory();
    cleanupAnyOrphanedSession(factory);
    doBind(session, factory);
  }

  private static void cleanupAnyOrphanedSession(SessionFactory factory) {
    final Session orphan = doUnbind(factory, false);
    if (orphan != null) {
      try {
        if (orphan.getTransaction() != null
                && orphan.getTransaction().getStatus() == TransactionStatus.ACTIVE) {
          try {
            orphan.getTransaction().rollback();
          }
          catch (Throwable t) {
            log.debug("Unable to rollback transaction for orphaned session", t);
          }
        }
        orphan.close();
      }
      catch (Throwable t) {
        log.debug("Unable to close orphaned session", t);
      }
    }
  }

  /**
   * Disassociates a previously bound session from the current thread of
   * execution.
   *
   * @param factory
   *         The factory for which the session should be unbound.
   *
   * @return The session which was unbound.
   */
  public static Session unbind(SessionFactory factory) {
    return doUnbind(factory, true);
  }

  private static Session existingSession(SessionFactory factory) {
    final Map sessionMap = CONTEXT_TL.get();
    if (sessionMap == null) {
      return null;
    }
    return (Session) sessionMap.get(factory);
  }

  private static void doBind(org.hibernate.Session session, SessionFactory factory) {
    Map sessionMap = CONTEXT_TL.get();
    if (sessionMap == null) {
      sessionMap = new HashMap();
      CONTEXT_TL.set(sessionMap);
    }
    sessionMap.put(factory, session);
  }

  private static Session doUnbind(SessionFactory factory, boolean releaseMapIfEmpty) {
    Session session = null;
    final Map sessionMap = CONTEXT_TL.get();
    if (sessionMap != null) {
      session = (Session) sessionMap.remove(factory);
      if (releaseMapIfEmpty && sessionMap.isEmpty()) {
        CONTEXT_TL.set(null);
      }
    }
    return session;
  }

  /**
   * Transaction sync used for cleanup of the internal session map.
   */
  protected static class CleanupSync implements Synchronization, Serializable {
    private static final long serialVersionUID = 7693735851966300334L;

    protected final SessionFactory factory;

    public CleanupSync(SessionFactory factory) {
      this.factory = factory;
    }

    @Override
    public void beforeCompletion() {

    }

    @Override
    public void afterCompletion(int i) {
      unbind(factory);
    }
  }

  private class TransactionProtectionWrapper implements InvocationHandler, Serializable {
    private static final long serialVersionUID = 6508638665617592893L;

    private final Session realSession;
    private Session wrappedSession;

    public TransactionProtectionWrapper(Session realSession) {
      this.realSession = realSession;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

      final String methodName = method.getName();

      // first check methods calls that we handle completely locally:
      if ("equals".equals(methodName) && method.getParameterCount() == 1) {
        if (args[0] == null || !Proxy.isProxyClass(args[0].getClass())) {
          return false;
        }
        return this.equals(Proxy.getInvocationHandler(args[0]));
      }
      else if ("hashCode".equals(methodName) && method.getParameterCount() == 0) {
        return this.hashCode();
      }
      else if ("toString".equals(methodName) && method.getParameterCount() == 0) {
        return String.format(Locale.ROOT, "ThreadLocalSessionContext.TransactionProtectionWrapper[{}]",
                             realSession);
      }

      // then check method calls that we need to delegate to the real Session
      try {
        // If close() is called, guarantee unbind()
        if ("close".equals(methodName)) {
          unbind(realSession.getSessionFactory());
        }
        else if ("getStatistics".equals(methodName) || "isOpen".equals(methodName)
                || "getListeners".equals(methodName)) {
          // allow these to go through the the real session no matter what
          log.trace("Allowing invocation [{}] to proceed to real session", methodName);
        }
        else if (!realSession.isOpen()) {
          // essentially, if the real session is closed allow any
          // method call to pass through since the real session
          // will complain by throwing an appropriate exception;
          // NOTE that allowing close() above has the same basic effect,
          // but we capture that there simply to doAfterTransactionCompletion the
          // unbind...
          log.trace("Allowing invocation [{}] to proceed to real (closed) session", methodName);
        }
        else if (realSession.getTransaction().getStatus() != TransactionStatus.ACTIVE) {
          // limit the methods available if no transaction is active
          if ("beginTransaction".equals(methodName) || "getTransaction".equals(methodName)
                  || "isTransactionInProgress".equals(methodName) || "setFlushMode".equals(methodName)
                  || "getFactory".equals(methodName) || "getSessionFactory".equals(methodName)
                  || "getTenantIdentifier".equals(methodName)) {
            log.trace("Allowing invocation [{}] to proceed to real (non-transacted) session", methodName);
          }
          else if ("reconnect".equals(methodName) || "disconnect".equals(methodName)) {
            // allow these (deprecated) methods to pass through
            log.trace(
                    "Allowing invocation [{}] to proceed to real (non-transacted) session - deprecated methods",
                    methodName);
          }
          else {
            throw new HibernateException(methodName + " is not valid without active transaction");
          }
        }
        log.trace("Allowing proxy invocation [{}] to proceed to real session", methodName);
        return method.invoke(realSession, args);
      }
      catch (InvocationTargetException e) {
        if (e.getTargetException() instanceof RuntimeException) {
          throw (RuntimeException) e.getTargetException();
        }
        throw e;
      }
    }

    /**
     * Setter for property 'wrapped'.
     *
     * @param wrapped
     *         Value to set for property 'wrapped'.
     */
    public void setWrapped(Session wrapped) {
      this.wrappedSession = wrapped;
    }

    // serialization ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private void writeObject(ObjectOutputStream oos) throws IOException {
      // if a ThreadLocalSessionContext-bound session happens to get
      // serialized, to be completely correct, we need to make sure
      // that unbinding of that session occurs.
      oos.defaultWriteObject();
      if (existingSession(factory()) == wrappedSession) {
        unbind(factory());
      }
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
      // on the inverse, it makes sense that if a ThreadLocalSessionContext-
      // bound session then gets deserialized to go ahead and re-bind it to
      // the ThreadLocalSessionContext session map.
      ois.defaultReadObject();
      realSession.getTransaction().registerSynchronization(buildCleanupSynch());
      doBind(wrappedSession, factory());
    }
  }

}
