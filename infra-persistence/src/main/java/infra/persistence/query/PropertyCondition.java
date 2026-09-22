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

package infra.persistence.query;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import infra.persistence.EntityProperty;
import infra.persistence.platform.Platform;
import infra.persistence.sql.Restriction;

/**
 * The standard {@link Condition} for a single mapped entity property: its SQL
 * restriction, the bindable value, and the mapped {@link EntityProperty} used
 * for binding.
 *
 * <p>Rendering delegates to the underlying restriction. Parameter binding uses
 * the {@link EntityProperty} type handler, so conditions must be bound in the
 * same order in which they were rendered.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Condition
 * @see PropertyConditionStrategy
 * @see EntityProperty
 * @since 5.0
 */
public class PropertyCondition implements Condition {

  public final Object value;

  public final Restriction restriction;

  public final EntityProperty entityProperty;

  /**
   * Create a condition for the given mapped property and value.
   *
   * @param value the value to bind
   * @param restriction the SQL restriction to render
   * @param entityProperty the mapped property used to bind the value
   */
  public PropertyCondition(Object value, Restriction restriction, EntityProperty entityProperty) {
    this.value = value;
    this.restriction = restriction;
    this.entityProperty = entityProperty;
  }

  /**
   * Delegate rendering to the underlying restriction.
   *
   * @param platform the database platform whose rendering rules apply
   * @param sqlBuffer the buffer to append to
   */
  @Override
  public void render(Platform platform, StringBuilder sqlBuffer) {
    restriction.render(platform, sqlBuffer);
  }

  /**
   * Return a copy with a different bindable value.
   *
   * @param propertyValue the replacement value
   * @return a condition retaining the restriction and mapped property
   */
  public PropertyCondition withValue(Object propertyValue) {
    return new PropertyCondition(propertyValue, restriction, entityProperty);
  }

  /**
   * Bind this condition's value at the given JDBC parameter index.
   *
   * @param ps the prepared statement to bind
   * @param parameterIndex the one-based parameter index
   * @return the next parameter index
   * @throws SQLException if the value cannot be bound
   */
  @Override
  public int setParameter(PreparedStatement ps, int parameterIndex) throws SQLException {
    entityProperty.setParameter(ps, parameterIndex++, value);
    return parameterIndex;
  }

}