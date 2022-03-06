/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.orm.hibernate5;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.context.spi.CurrentSessionContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.orm.jpa.EntityManagerHolder;
import cn.taketoday.transaction.support.SynchronizationInfo;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

/**
 * Implementation of Hibernate 3.1's {@link CurrentSessionContext} interface
 * that delegates to Framework's {@link SessionFactoryUtils} for providing a
 * Framework-managed current {@link Session}.
 *
 * <p>This CurrentSessionContext implementation can also be specified in custom
 * SessionFactory setup through the "hibernate.current_session_context_class"
 * property, with the fully qualified name of this class as value.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
public class FrameworkSessionContext implements CurrentSessionContext {

  private final SessionFactoryImplementor sessionFactory;

  @Nullable
  private TransactionManager transactionManager;

  @Nullable
  private CurrentSessionContext jtaSessionContext;

  /**
   * Create a new FrameworkSessionContext for the given Hibernate SessionFactory.
   *
   * @param sessionFactory the SessionFactory to provide current Sessions for
   */
  public FrameworkSessionContext(SessionFactoryImplementor sessionFactory) {
    this.sessionFactory = sessionFactory;
    try {
      JtaPlatform jtaPlatform = sessionFactory.getServiceRegistry().getService(JtaPlatform.class);
      this.transactionManager = jtaPlatform.retrieveTransactionManager();
      if (this.transactionManager != null) {
        this.jtaSessionContext = new FrameworkJtaSessionContext(sessionFactory);
      }
    }
    catch (Exception ex) {
      LoggerFactory.getLogger(FrameworkSessionContext.class)
              .warn("Could not introspect Hibernate JtaPlatform for FrameworkJtaSessionContext", ex);
    }
  }

  /**
   * Retrieve the Framework-managed Session for the current thread, if any.
   */
  @Override
  public Session currentSession() throws HibernateException {
    SynchronizationInfo info = TransactionSynchronizationManager.getSynchronizationInfo();
    Object value = info.getResource(sessionFactory);
    if (value instanceof Session session) {
      return session;
    }
    else if (value instanceof SessionHolder sessionHolder) {
      // HibernateTransactionManager
      Session session = sessionHolder.getSession();
      if (!sessionHolder.isSynchronizedWithTransaction()
              && info.isSynchronizationActive()) {
        info.registerSynchronization(
                new FrameworkSessionSynchronization(sessionHolder, sessionFactory, false));
        sessionHolder.setSynchronizedWithTransaction(true);
        // Switch to FlushMode.AUTO, as we have to assume a thread-bound Session
        // with FlushMode.MANUAL, which needs to allow flushing within the transaction.
        FlushMode flushMode = session.getHibernateFlushMode();
        if (flushMode.equals(FlushMode.MANUAL) &&
                !info.isCurrentTransactionReadOnly()) {
          session.setHibernateFlushMode(FlushMode.AUTO);
          sessionHolder.setPreviousFlushMode(flushMode);
        }
      }
      return session;
    }
    else if (value instanceof EntityManagerHolder entityManagerHolder) {
      // JpaTransactionManager
      return entityManagerHolder.getEntityManager().unwrap(Session.class);
    }

    if (transactionManager != null && jtaSessionContext != null) {
      try {
        if (transactionManager.getStatus() == Status.STATUS_ACTIVE) {
          Session session = jtaSessionContext.currentSession();
          if (info.isSynchronizationActive()) {
            info.registerSynchronization(new FrameworkFlushSynchronization(session));
          }
          return session;
        }
      }
      catch (SystemException ex) {
        throw new HibernateException("JTA TransactionManager found but status check failed", ex);
      }
    }

    if (info.isSynchronizationActive()) {
      Session session = sessionFactory.openSession();
      if (info.isCurrentTransactionReadOnly()) {
        session.setHibernateFlushMode(FlushMode.MANUAL);
      }
      SessionHolder sessionHolder = new SessionHolder(session);
      info.registerSynchronization(
              new FrameworkSessionSynchronization(sessionHolder, sessionFactory, true));
      info.bindResource(sessionFactory, sessionHolder);
      sessionHolder.setSynchronizedWithTransaction(true);
      return session;
    }
    else {
      throw new HibernateException("Could not obtain transaction-synchronized Session for current thread");
    }
  }

}
