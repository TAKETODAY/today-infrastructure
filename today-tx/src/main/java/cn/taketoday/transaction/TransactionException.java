/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.transaction;

import java.io.Serial;

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.lang.Nullable;

/**
 * Superclass for all transaction exceptions.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Rod Johnson
 * @since 2018-11-06 22:46
 */
public abstract class TransactionException extends NestedRuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  public TransactionException(@Nullable String message) {
    super(message);
  }

  public TransactionException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }

}
