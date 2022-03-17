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

package cn.taketoday.jdbc.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Implement this interface when parameters need to be customized based
 * on the connection. We might need to do this to make use of proprietary
 * features, available only with a specific Connection type.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @see CallableStatementCreatorFactory#newCallableStatementCreator(ParameterMapper)
 * @see cn.taketoday.jdbc.object.StoredProcedure#execute(ParameterMapper)
 */
@FunctionalInterface
public interface ParameterMapper {

  /**
   * Create a Map of input parameters, keyed by name.
   *
   * @param con a JDBC connection. This is useful (and the purpose of this interface)
   * if we need to do something RDBMS-specific with a proprietary Connection
   * implementation class. This class conceals such proprietary details. However,
   * it is best to avoid using such proprietary RDBMS features if possible.
   * @return a Map of input parameters, keyed by name (never {@code null})
   * @throws SQLException if an SQLException is encountered setting
   * parameter values (that is, there's no need to catch SQLException)
   */
  Map<String, ?> createMap(Connection con) throws SQLException;

}
