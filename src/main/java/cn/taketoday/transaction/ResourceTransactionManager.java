/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.transaction;

/**
 * @author TODAY <br>
 * 2018-11-13 17:03
 */
public interface ResourceTransactionManager extends TransactionManager {

  /**
   * Return the resource factory that this transaction manager operates on, e.g. a
   * JDBC DataSource or a JMS ConnectionFactory.
   * <p>
   * This target resource factory is usually used as resource key for
   * {@link SynchronizationManager}'s resource bindings per thread.
   *
   * @return the target resource factory (never {@code null})
   *
   * @see SynchronizationManager#bindResource
   * @see SynchronizationManager#getResource
   */
  Object getResourceFactory();
}
