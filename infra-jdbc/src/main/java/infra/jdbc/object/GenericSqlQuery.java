/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
  protected RowMapper<T> newRowMapper(@Nullable Object @Nullable [] parameters, @Nullable Map<?, ?> context) {
    if (this.rowMapper != null) {
      return this.rowMapper;
    }
    else {
      Assert.state(this.rowMapperClass != null, "No RowMapper set");
      return BeanUtils.newInstance(this.rowMapperClass);
    }
  }

}
