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

package cn.taketoday.jdbc.core;

import cn.taketoday.lang.Nullable;

/**
 * Interface to be implemented by objects that can provide SQL strings.
 *
 * <p>Typically implemented by PreparedStatementCreators, CallableStatementCreators
 * and StatementCallbacks that want to expose the SQL they use to create their
 * statements, to allow for better contextual information in case of exceptions.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PreparedStatementCreator
 * @see CallableStatementCreator
 * @see StatementCallback
 * @since 4.0
 */
public interface SqlProvider {

  /**
   * Return the SQL string for this object, i.e.
   * typically the SQL used for creating statements.
   *
   * @return the SQL string, or {@code null} if not available
   */
  @Nullable
  String getSql();

}
