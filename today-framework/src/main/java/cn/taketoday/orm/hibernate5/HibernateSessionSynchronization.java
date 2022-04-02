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
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import cn.taketoday.core.Ordered;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.transaction.support.TransactionSynchronization;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;

/**
 * Callback for resource cleanup at the end of a Framework-managed transaction
 * for a pre-bound Hibernate Session.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class HibernateSessionSynchronization implements TransactionSynchronization, Ordered {

  private final SessionHolder sessionHolder;

  private final SessionFactory sessionFactory;

  private final boolean newSession;

  private boolean holderActive = true;

  public HibernateSessionSynchronization(SessionHolder sessionHolder, SessionFactory sessionFactory) {
    this(sessionHolder, sessionFactory, false);
  }

  public HibernateSessionSynchronization(SessionHolder sessionHolder, SessionFactory sessionFactory, boolean newSession) {
    this.sessionHolder = sessionHolder;
    this.sessionFactory = sessionFactory;
    this.newSession = newSession;
  }

  private Session getCurrentSession() {
    return sessionHolder.getSession();
  }

  @Override
  public int getOrder() {
    return SessionFactoryUtils.SESSION_SYNCHRONIZATION_ORDER;
  }

  @Override
  public void suspend() {
    if (holderActive) {
      TransactionSynchronizationManager.unbindResource(sessionFactory);
      // Eagerly disconnect the Session here, to make release mode "on_close" work on JBoss.
      getCurrentSession().disconnect();
    }
  }

  @Override
  public void resume() {
    if (holderActive) {
      TransactionSynchronizationManager.bindResource(sessionFactory, sessionHolder);
    }
  }

  @Override
  public void flush() {
    SessionFactoryUtils.flush(getCurrentSession(), false);
  }

  @Override
  public void beforeCommit(boolean readOnly) throws DataAccessException {
    if (!readOnly) {
      Session session = getCurrentSession();
      // Read-write transaction -> flush the Hibernate Session.
      // Further check: only flush when not FlushMode.MANUAL.
      if (!FlushMode.MANUAL.equals(session.getHibernateFlushMode())) {
        SessionFactoryUtils.flush(getCurrentSession(), true);
      }
    }
  }

  @Override
  public void beforeCompletion() {
    try {
      Session session = sessionHolder.getSession();
      if (sessionHolder.getPreviousFlushMode() != null) {
        // In case of pre-bound Session, restore previous flush mode.
        session.setHibernateFlushMode(sessionHolder.getPreviousFlushMode());
      }
      // Eagerly disconnect the Session here, to make release mode "on_close" work nicely.
      session.disconnect();
    }
    finally {
      // Unbind at this point if it's a new Session...
      if (newSession) {
        TransactionSynchronizationManager.unbindResource(sessionFactory);
        this.holderActive = false;
      }
    }
  }

  @Override
  public void afterCommit() {
  }

  @Override
  public void afterCompletion(int status) {
    try {
      if (status != STATUS_COMMITTED) {
        // Clear all pending inserts/updates/deletes in the Session.
        // Necessary for pre-bound Sessions, to avoid inconsistent state.
        sessionHolder.getSession().clear();
      }
    }
    finally {
      sessionHolder.setSynchronizedWithTransaction(false);
      // Call close() at this point if it's a new Session...
      if (newSession) {
        SessionFactoryUtils.closeSession(sessionHolder.getSession());
      }
    }
  }

}
