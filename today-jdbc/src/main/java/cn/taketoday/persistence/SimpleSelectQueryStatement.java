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

import java.util.ArrayList;
import java.util.Arrays;

import cn.taketoday.persistence.sql.SimpleSelect;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/28 22:19
 */
public abstract class SimpleSelectQueryStatement implements QueryStatement {

  @Override
  public StatementSequence render(EntityMetadata metadata) {
    SimpleSelect select = new SimpleSelect(Arrays.asList(metadata.columnNames), new ArrayList<>());
    select.setTableName(metadata.tableName);

    renderInternal(metadata, select);
    return select;
  }

  protected abstract void renderInternal(EntityMetadata metadata, SimpleSelect select);

}
