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

import infra.logging.LogMessage;
import infra.persistence.sql.Select;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/19 19:31
 */
class FindByIdQuery extends ColumnsQueryStatement implements QueryStatement, DebugDescriptive {
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
