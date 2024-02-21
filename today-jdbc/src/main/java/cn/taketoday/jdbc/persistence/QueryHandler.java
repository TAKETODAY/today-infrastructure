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

package cn.taketoday.jdbc.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import cn.taketoday.jdbc.persistence.sql.Select;
import cn.taketoday.lang.Descriptive;
import cn.taketoday.logging.LogMessage;

/**
 * Query condition builder
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/16 14:45
 */
public interface QueryHandler extends Descriptive {

  /**
   * prepare select statement
   *
   * @param metadata entity info
   * @param select select
   */
  void render(EntityMetadata metadata, Select select);

  /**
   * apply statement parameters
   *
   * @param metadata entity info
   * @param statement JDBC statement
   */
  void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException;

  @Override
  default String getDescription() {
    return "Query entities";
  }

  default Object getDebugLogMessage() {
    return LogMessage.format(getDescription());
  }

}
