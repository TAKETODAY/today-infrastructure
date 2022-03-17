/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.transaction.interceptor;

import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionStatus;

/**
 * Used for testing only (for example, when we must replace the
 * behavior of a PlatformTransactionManager bean we don't have access to).
 *
 * <p>Allows behavior of an entire class to change with static delegate change.
 * Not multi-threaded.
 *
 * @author Rod Johnson
 */
public class PlatformTransactionManagerFacade implements PlatformTransactionManager {

  /**
   * This member can be changed to change behavior class-wide.
   */
  public static PlatformTransactionManager delegate;

  @Override
  public TransactionStatus getTransaction(@Nullable TransactionDefinition definition) {
    return delegate.getTransaction(definition);
  }

  @Override
  public void commit(TransactionStatus status) {
    delegate.commit(status);
  }

  @Override
  public void rollback(TransactionStatus status) {
    delegate.rollback(status);
  }

}
