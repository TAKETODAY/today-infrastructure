/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.jdbc.metadata;

import javax.sql.DataSource;

/**
 * A base {@link DataSourcePoolMetadata} implementation.
 *
 * @param <T> the data source type
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractDataSourcePoolMetadata<T extends DataSource> implements DataSourcePoolMetadata {

  private final T dataSource;

  /**
   * Create an instance with the data source to use.
   *
   * @param dataSource the data source
   */
  protected AbstractDataSourcePoolMetadata(T dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public Float getUsage() {
    Integer maxSize = getMax();
    Integer currentSize = getActive();
    if (maxSize == null || currentSize == null) {
      return null;
    }
    if (maxSize < 0) {
      return -1f;
    }
    if (currentSize == 0) {
      return 0f;
    }
    return (float) currentSize / (float) maxSize;
  }

  protected final T getDataSource() {
    return this.dataSource;
  }

}
