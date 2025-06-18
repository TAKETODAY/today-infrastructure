/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import infra.persistence.sql.Restriction;
import infra.persistence.sql.Select;
import infra.util.CollectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/6/17 15:42
 */
public class QueryBuilder extends ColumnsQueryStatement implements ConditionStatement {

  private final List<Restriction> restrictions = new ArrayList<>();

  private final List<Object> conditions = new ArrayList<>();

  public QueryBuilder add(Restriction restriction) {
    restrictions.add(restriction);
    return this;
  }

  public QueryBuilder add(Restriction restriction, Object value) {
    restrictions.add(restriction);
    conditions.add(value);
    return this;
  }

  public QueryBuilder add(Restriction restriction, Object... value) {
    restrictions.add(restriction);
    CollectionUtils.addAll(this.conditions, value);
    return this;
  }

  @Override
  protected void renderInternal(EntityMetadata metadata, Select select) {
    select.setWhereClause(Restriction.renderWhereClause(restrictions));
  }

  @Override
  public void renderWhereClause(EntityMetadata metadata, List<Restriction> restrictions) {
    restrictions.addAll(this.restrictions);
  }

  @Override
  public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
    int index = 1;
    for (Object condition : conditions) {
      statement.setObject(index++, condition);
    }
  }

  public static QueryBuilder of() {
    return new QueryBuilder();
  }

  public static QueryBuilder of(Restriction restriction) {
    return new QueryBuilder().add(restriction);
  }

  public static QueryBuilder of(Restriction restriction, Object... value) {
    return new QueryBuilder().add(restriction, value);
  }

}
