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

package cn.taketoday.transaction.support;

import cn.taketoday.transaction.PlatformTransactionManager;

/**
 * Extension of the {@link PlatformTransactionManager}
 * interface, indicating a native resource transaction manager, operating on a single
 * target resource. Such transaction managers differ from JTA transaction managers in
 * that they do not use XA transaction enlistment for an open number of resources but
 * rather focus on leveraging the native power and simplicity of a single target resource.
 *
 * <p>This interface is mainly used for abstract introspection of a transaction manager,
 * giving clients a hint on what kind of transaction manager they have been given
 * and on what concrete resource the transaction manager is operating on.
 *
 * @author Juergen Hoeller
 * @see TransactionSynchronizationManager
 * @since 4.0
 */
public interface ResourceTransactionManager extends PlatformTransactionManager {

  /**
   * Return the resource factory that this transaction manager operates on,
   * e.g. a JDBC DataSource or a JMS ConnectionFactory.
   * <p>This target resource factory is usually used as resource key for
   * {@link TransactionSynchronizationManager}'s resource bindings per thread.
   *
   * @return the target resource factory (never {@code null})
   * @see TransactionSynchronizationManager#bindResource
   * @see TransactionSynchronizationManager#getResource
   */
  Object getResourceFactory();

}
