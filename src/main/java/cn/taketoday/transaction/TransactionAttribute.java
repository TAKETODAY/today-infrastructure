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

import cn.taketoday.lang.Nullable;

/**
 * This interface adds a {@code rollbackOn} specification to {@link TransactionDefinition}.
 * As custom {@code rollbackOn} is only possible with AOP, it resides in the AOP-related
 * transaction subpackage.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Paluch
 * @author TODAY 2021/11/5 15:02
 * @since 4.0
 */
public interface TransactionAttribute extends TransactionDefinition {

  /**
   * Return a qualifier value associated with this transaction attribute.
   * <p>This may be used for choosing a corresponding transaction manager
   * to process this specific transaction.
   */
  @Nullable
  String getQualifier();

  /**
   * Should we roll back on the given exception?
   *
   * @param ex the exception to evaluate
   * @return whether to perform a rollback or not
   */
  boolean rollbackOn(Throwable ex);

}
