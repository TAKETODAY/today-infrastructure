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
import java.util.Map;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/16 22:39
 */
class NoConditionsOrderByQuery extends AbstractColumnsQueryHandler implements QueryHandler {

  private final Map<String, Order> sortKeys;

  NoConditionsOrderByQuery(Map<String, Order> sortKeys) {
    this.sortKeys = sortKeys;
  }

  @Override
  protected void renderInternal(EntityMetadata metadata, Select select) {
    if (!sortKeys.isEmpty()) {
      StringBuilder orderByClause = new StringBuilder();
      boolean first = true;
      for (var entry : sortKeys.entrySet()) {
        if (first) {
          first = false;
          orderByClause.append('`');
          orderByClause.append(entry.getKey())
                  .append("` ")
                  .append(entry.getValue().name());
        }
        else {
          orderByClause.append(", `")
                  .append(entry.getKey())
                  .append("` ")
                  .append(entry.getValue().name());
        }
      }

      select.setOrderByClause(orderByClause);
    }
  }

  @Override
  public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException { }

}
