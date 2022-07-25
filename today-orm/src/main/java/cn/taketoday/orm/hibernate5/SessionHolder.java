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
import org.hibernate.Transaction;

import cn.taketoday.lang.Nullable;
import cn.taketoday.orm.jpa.EntityManagerHolder;

/**
 * Resource holder wrapping a Hibernate {@link Session} (plus an optional {@link Transaction}).
 * {@link HibernateTransactionManager} binds instances of this class to the thread,
 * for a given {@link org.hibernate.SessionFactory}. Extends {@link EntityManagerHolder}
 * as of 5.1, automatically exposing an {@code EntityManager} handle on Hibernate 5.2+.
 *
 * <p>Note: This is an SPI class, not intended to be used by applications.
 *
 * @author Juergen Hoeller
 * @see HibernateTransactionManager
 * @see SessionFactoryUtils
 * @since 4.0
 */
public class SessionHolder extends EntityManagerHolder {

  @Nullable
  private Transaction transaction;

  @Nullable
  private FlushMode previousFlushMode;

  public SessionHolder(Session session) {
    super(session);
  }

  public Session getSession() {
    return (Session) getEntityManager();
  }

  public void setTransaction(@Nullable Transaction transaction) {
    this.transaction = transaction;
    setTransactionActive(transaction != null);
  }

  @Nullable
  public Transaction getTransaction() {
    return this.transaction;
  }

  public void setPreviousFlushMode(@Nullable FlushMode previousFlushMode) {
    this.previousFlushMode = previousFlushMode;
  }

  @Nullable
  public FlushMode getPreviousFlushMode() {
    return this.previousFlushMode;
  }

  @Override
  public void clear() {
    super.clear();
    this.transaction = null;
    this.previousFlushMode = null;
  }

}
