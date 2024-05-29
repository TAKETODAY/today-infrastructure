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

package cn.taketoday.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import cn.taketoday.lang.Descriptive;

/**
 * Query condition builder
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DebugDescriptive
 * @see Descriptive
 * @since 4.0 2024/2/16 14:45
 */
public interface QueryStatement {

  /**
   * prepare select statement
   *
   * @param metadata entity info
   */
  StatementSequence render(EntityMetadata metadata);

  /**
   * apply statement parameters
   *
   * @param metadata entity info
   * @param statement JDBC statement
   */
  void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException;

}
