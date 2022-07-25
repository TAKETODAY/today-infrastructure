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

import org.hibernate.Session;

import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.support.TransactionSynchronization;

/**
 * Simple synchronization adapter that propagates a {@code flush()} call
 * to the underlying Hibernate Session. Used in combination with JTA.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class HibernateFlushSynchronization implements TransactionSynchronization {

  private final Session session;

  public HibernateFlushSynchronization(Session session) {
    this.session = session;
  }

  @Override
  public void flush() {
    SessionFactoryUtils.flush(this.session, false);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof HibernateFlushSynchronization
            && this.session == ((HibernateFlushSynchronization) other).session));
  }

  @Override
  public int hashCode() {
    return this.session.hashCode();
  }

}
