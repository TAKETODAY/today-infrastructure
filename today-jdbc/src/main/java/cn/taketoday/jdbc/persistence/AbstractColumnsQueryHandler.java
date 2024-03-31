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

import cn.taketoday.jdbc.persistence.sql.Select;

/**
 * Render select Columns from table
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/16 18:10
 */
public abstract class AbstractColumnsQueryHandler implements QueryHandler {

  @Override
  public StatementSequence render(EntityMetadata metadata) {
    Select select = new Select();
    StringBuilder selectClause = new StringBuilder();

    boolean first = true;
    for (EntityProperty property : metadata.entityProperties) {
      if (first) {
        first = false;
        selectClause.append('`');
      }
      else {
        selectClause.append(", `");
      }
      selectClause.append(property.columnName)
              .append('`');
    }

    select.setSelectClause(selectClause);
    select.setFromClause(metadata.tableName);

    renderInternal(metadata, select);
    return select;
  }

  protected abstract void renderInternal(EntityMetadata metadata, Select select);

}
