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

import org.jspecify.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import infra.persistence.sql.Restriction;
import infra.persistence.sql.SimpleSelect;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/10 16:54
 */
final class MapQueryStatementFactory implements QueryStatementFactory {

  @Override
  public @Nullable QueryStatement createQuery(Object example) {
    if (example instanceof Map<?, ?> map) {
      return new MapQueryStatement(map);
    }
    return null;
  }

  @Override
  public @Nullable ConditionStatement createCondition(Object example) {
    if (example instanceof Map<?, ?> map) {
      return new MapQueryStatement(map);
    }
    return null;
  }

  static class MapQueryStatement extends SimpleSelectQueryStatement
          implements QueryStatement, ConditionStatement, DebugDescriptive {

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
