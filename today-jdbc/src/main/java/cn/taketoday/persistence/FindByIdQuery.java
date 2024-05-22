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

import cn.taketoday.logging.LogMessage;
import cn.taketoday.persistence.sql.Select;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/19 19:31
 */
class FindByIdQuery extends ColumnsQueryStatement implements QueryStatement {
  private final Object id;

  FindByIdQuery(Object id) {
    this.id = id;
  }

  @Override
  protected void renderInternal(EntityMetadata metadata, Select select) {
    select.setWhereClause('`' + metadata.idColumnName + "`=? LIMIT 1");
  }

  @Override
  public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
    metadata.idProperty().setParameter(statement, 1, id);
  }

  @Override
  public String getDescription() {
    return "Fetch entity By ID";
  }

  @Override
  public Object getDebugLogMessage() {
    return LogMessage.format("Query entity using ID: '{}'", id);
  }
}
