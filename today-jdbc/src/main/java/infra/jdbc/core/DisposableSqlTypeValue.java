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
import infra.jdbc.support.SqlValue;

/**
 * Subinterface of {@link SqlTypeValue} that adds a cleanup callback,
 * to be invoked after the value has been set and the corresponding
 * statement has been executed.
 *
 * @author Juergen Hoeller
 * @see SqlLobValue
 * @since 4.0
 */
public interface DisposableSqlTypeValue extends SqlTypeValue {

  /**
   * Clean up resources held by this type value,
   * for example the LobCreator in case of an SqlLobValue.
   *
   * @see SqlLobValue#cleanup()
   * @see SqlValue#cleanup()
   */
  void cleanup();

}
