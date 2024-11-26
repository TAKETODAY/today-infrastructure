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

package infra.jdbc.core;

import infra.jdbc.core.support.SqlLobValue;

/**
 * Interface to be implemented by objects that can close resources
 * allocated by parameters like {@code SqlLobValue} objects.
 *
 * <p>Typically implemented by {@code PreparedStatementCreators} and
 * {@code PreparedStatementSetters} that support {@link DisposableSqlTypeValue}
 * objects (e.g. {@code SqlLobValue}) as parameters.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see PreparedStatementCreator
 * @see PreparedStatementSetter
 * @see DisposableSqlTypeValue
 * @see SqlLobValue
 * @since 4.0
 */
public interface ParameterDisposer {

  /**
   * Close the resources allocated by parameters that the implementing
   * object holds, for example in case of a DisposableSqlTypeValue
   * (like an SqlLobValue).
   *
   * @see DisposableSqlTypeValue#cleanup()
   * @see SqlLobValue#cleanup()
   */
  void cleanupParameters();

}
