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

package cn.taketoday.test.context.transaction.ejb.dao;

import cn.taketoday.test.context.transaction.ejb.model.TestEntity;
import jakarta.ejb.TransactionAttribute;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Abstract base class for EJB implementations of {@link TestEntityDao} which
 * declare transaction semantics for {@link #incrementCount(String)} via
 * {@link TransactionAttribute}.
 *
 * @author Sam Brannen
 * @author Xavier Detant
 * @see RequiredEjbTxTestEntityDao
 * @see RequiresNewEjbTxTestEntityDao
 * @since 4.0
 */
public abstract class AbstractEjbTxTestEntityDao implements TestEntityDao {

  @PersistenceContext
  protected EntityManager entityManager;

  protected final TestEntity getTestEntity(String name) {
    TestEntity te = entityManager.find(TestEntity.class, name);
    if (te == null) {
      te = new TestEntity(name, 0);
      entityManager.persist(te);
    }
    return te;
  }

  protected final int getCountInternal(String name) {
    return getTestEntity(name).getCount();
  }

  protected final int incrementCountInternal(String name) {
    TestEntity te = getTestEntity(name);
    int count = te.getCount();
    count++;
    te.setCount(count);
    return count;
  }

}
