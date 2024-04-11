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

import cn.taketoday.persistence.sql.OrderByClause;
import cn.taketoday.persistence.sql.Select;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/16 22:39
 */
class NoConditionsOrderByQuery extends ColumnsQueryStatement implements QueryStatement {

  private final OrderByClause clause;

  NoConditionsOrderByQuery(OrderByClause clause) {
    this.clause = clause;
  }

  @Override
  protected void renderInternal(EntityMetadata metadata, Select select) {
    if (!clause.isEmpty()) {
      select.setOrderByClause(clause.toClause());
    }
  }

  @Override
  public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException { }

}
