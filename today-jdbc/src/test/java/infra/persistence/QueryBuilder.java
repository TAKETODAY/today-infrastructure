/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
