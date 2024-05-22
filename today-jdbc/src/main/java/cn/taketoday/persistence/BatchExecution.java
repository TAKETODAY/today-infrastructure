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

/**
 * Batch execution metadata
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/20 23:25
 */
public class BatchExecution {

  public final String sql;

  public final boolean autoGenerateId;

  public final EntityMetadata entityMetadata;

  public final PropertyUpdateStrategy strategy;

  public final ArrayList<Object> entities = new ArrayList<>();

  BatchExecution(String sql, PropertyUpdateStrategy strategy,
          EntityMetadata entityMetadata, boolean autoGenerateId) {
    this.sql = sql;
    this.strategy = strategy;
    this.entityMetadata = entityMetadata;
    this.autoGenerateId = autoGenerateId;
  }

}
