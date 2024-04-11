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
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import cn.taketoday.persistence.sql.OrderByClause;
import cn.taketoday.persistence.sql.Restriction;
import cn.taketoday.persistence.sql.SimpleSelect;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/10 16:54
 */
final class MapQueryHandlerFactory implements QueryHandlerFactory {

  @Override
  public QueryStatement createQuery(Object example) {
    if (example instanceof Map<?, ?> map) {
      return new MapQueryStatement(map);
    }
    return null;
  }

  @Override
  public ConditionStatement createCondition(Object example) {
    if (example instanceof Map<?, ?> map) {
      return new MapQueryStatement(map);
    }
    return null;
  }

  static class MapQueryStatement extends SimpleSelectQueryStatement implements QueryStatement, ConditionStatement {

    private final Map<?, ?> map;

    public MapQueryStatement(Map<?, ?> map) {
      this.map = map;
    }

    @Override
    protected void renderInternal(EntityMetadata metadata, SimpleSelect select) {
      renderWhereClause(metadata, select.restrictions);
    }

    @Override
    public void renderWhereClause(EntityMetadata metadata, List<Restriction> restrictions) {
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        restrictions.add(Restriction.equal(entry.getKey().toString()));
      }
    }

    @Nullable
    @Override
    public OrderByClause getOrderByClause(EntityMetadata metadata) {
      return null;
    }

    @Override
    public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
      int idx = 1;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        statement.setObject(idx++, entry.getValue());
      }
    }

    @Override
    public String getDescription() {
      return "Query with Map of params: " + map;
    }

  }

}
