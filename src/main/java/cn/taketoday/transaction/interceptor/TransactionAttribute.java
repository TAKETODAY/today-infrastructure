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

import java.util.Collection;

import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.TransactionDefinition;

/**
 * This interface adds a {@code rollbackOn} specification to {@link TransactionDefinition}.
 * As custom {@code rollbackOn} is only possible with AOP, it resides in the AOP-related
 * transaction subpackage.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Paluch
 * @see DefaultTransactionAttribute
 * @see RuleBasedTransactionAttribute
 * @since 4.0
 */
public interface TransactionAttribute extends TransactionDefinition {

  /**
   * Return a qualifier value associated with this transaction attribute.
   * <p>This may be used for choosing a corresponding transaction manager
   * to process this specific transaction.
   *
   * @since 3.0
   */
  @Nullable
  String getQualifier();

  /**
   * Return labels associated with this transaction attribute.
   * <p>This may be used for applying specific transactional behavior
   * or follow a purely descriptive nature.
   *
   * @since 4.0
   */
  Collection<String> getLabels();

  /**
   * Should we roll back on the given exception?
   *
   * @param ex the exception to evaluate
   * @return whether to perform a rollback or not
   */
  boolean rollbackOn(Throwable ex);

}
