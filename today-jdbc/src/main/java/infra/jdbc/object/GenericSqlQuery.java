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

package infra.jdbc.object;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.beans.BeanUtils;
import infra.jdbc.core.RowMapper;
import infra.lang.Assert;

/**
 * A concrete variant of {@link SqlQuery} which can be configured
 * with a {@link RowMapper}.
 *
 * @param <T> the result type
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see #setRowMapper
 * @see #setRowMapperClass
 * @since 4.0
 */
public class GenericSqlQuery<T> extends SqlQuery<T> {

  @Nullable
  private RowMapper<T> rowMapper;

  @SuppressWarnings("rawtypes")
  @Nullable
  private Class<? extends RowMapper> rowMapperClass;

  /**
   * Set a specific {@link RowMapper} instance to use for this query.
   */
  public void setRowMapper(RowMapper<T> rowMapper) {
    this.rowMapper = rowMapper;
  }

  /**
   * Set a {@link RowMapper} class for this query, creating a fresh
   * {@link RowMapper} instance per execution.
   */
  @SuppressWarnings("rawtypes")
  public void setRowMapperClass(Class<? extends RowMapper> rowMapperClass) {
    this.rowMapperClass = rowMapperClass;
  }

  @Override
  public void afterPropertiesSet() {
    super.afterPropertiesSet();
    Assert.isTrue(this.rowMapper != null || this.rowMapperClass != null,
            "'rowMapper' or 'rowMapperClass' is required");
  }

  @Override
  @SuppressWarnings("unchecked")
  protected RowMapper<T> newRowMapper(Object @Nullable [] parameters, @Nullable Map<?, ?> context) {
    if (this.rowMapper != null) {
      return this.rowMapper;
    }
    else {
      Assert.state(this.rowMapperClass != null, "No RowMapper set");
      return BeanUtils.newInstance(this.rowMapperClass);
    }
  }

}
