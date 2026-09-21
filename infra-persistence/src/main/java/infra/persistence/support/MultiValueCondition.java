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

package infra.persistence.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import infra.persistence.Condition;
import infra.persistence.EntityProperty;
import infra.persistence.platform.Platform;
import infra.persistence.sql.Restriction;
import infra.util.Assert;

/**
 * A {@link Condition} that binds several values to the placeholders of one
 * restriction, for example {@code BETWEEN ? AND ?} or {@code IN (?, ?, ...)}.
 *
 * <p>The values are bound in order through the {@link EntityProperty} type
 * handler; the number of values must match the number of placeholders rendered
 * by the restriction.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Condition
 * @since 5.0
 */
public final class MultiValueCondition implements Condition {

  private final EntityProperty property;

  private final Restriction restriction;

  private final List<Object> values;

  /**
   * Create a condition binding {@code values} to the restriction's placeholders.
   *
   * @param property the mapped property used to bind the values
   * @param restriction the SQL restriction to render
   * @param values the values to bind, in placeholder order; must not be empty
   * @throws IllegalArgumentException if {@code values} is empty
   */
  public MultiValueCondition(EntityProperty property, Restriction restriction, List<Object> values) {
    Assert.notNull(property, "EntityProperty is required");
    Assert.notNull(restriction, "Restriction is required");
    Assert.notEmpty(values, "Values are required");
    this.property = property;
    this.restriction = restriction;
    this.values = List.copyOf(values);
  }

  @Override
  public void render(Platform platform, StringBuilder sqlBuffer) {
    restriction.render(platform, sqlBuffer);
  }

  @Override
  public int setParameter(PreparedStatement ps, int parameterIndex) throws SQLException {
    for (Object value : values) {
      property.setParameter(ps, parameterIndex++, value);
    }
    return parameterIndex;
  }

}