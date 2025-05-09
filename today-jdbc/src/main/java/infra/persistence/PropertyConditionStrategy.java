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

package infra.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import infra.lang.Nullable;
import infra.persistence.sql.Restriction;

/**
 * Strategy interface for resolving SQL WHERE conditions based on entity properties.
 *
 * <p>This interface provides a mechanism to dynamically generate SQL conditions
 * for querying databases. Implementations of this interface define specific
 * strategies for resolving conditions based on metadata and values provided.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/24 23:58
 */
public interface PropertyConditionStrategy {

  /**
   * Resolves an SQL WHERE condition based on the provided entity property and value.
   *
   * <p>This method dynamically generates a {@link Condition} object that represents
   * a restriction in an SQL query. The generated condition can be used to construct
   * query statements or apply filters to database operations.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   * EntityProperty property = ...; // Obtain an EntityProperty instance
   * Object value = "exampleValue";
   *
   * PropertyConditionStrategy strategy = ...; // Obtain a strategy implementation
   * Condition condition = strategy.resolve(property, value);
   *
   * if (condition != null) {
   *   StringBuilder sql = new StringBuilder("SELECT * FROM table WHERE ");
   *   condition.render(sql);
   *   System.out.println(sql.toString());
   * }
   * }</pre>
   *
   * @param entityProperty the metadata of the entity property for which the condition is resolved.
   * Must not be {@code null}.
   * @param value the value to be used in the condition. This can be {@code null}
   * depending on the implementation and use case.
   * @return a {@link Condition} object representing the resolved SQL WHERE condition,
   * or {@code null} if no condition can be resolved for the given inputs.
   */
  @Nullable
  Condition resolve(EntityProperty entityProperty, Object value);

  /**
   * Represents a condition (predicate) that can be applied to an SQL query.
   * A {@code Condition} encapsulates a value, a restriction, and an entity property,
   * providing methods to render the condition into SQL and manipulate its value.
   *
   * <p>This class is typically used in conjunction with query-building mechanisms
   * to dynamically construct SQL WHERE clauses based on entity properties and values.
   *
   * <p><strong>Key Features:</strong>
   * <ul>
   *   <li>Encapsulates a value, a restriction, and an entity property.</li>
   *   <li>Provides a method to render the condition into an SQL fragment.</li>
   *   <li>Supports creating updated conditions with new values via {@link #withValue(Object)}.</li>
   *   <li>Facilitates setting parameter values in a {@code PreparedStatement}.</li>
   * </ul>
   *
   * <p><strong>Example Usage:</strong>
   * <pre>{@code
   *   // Define an entity property and a restriction
   *   EntityProperty property = ...; // Obtain an EntityProperty instance
   *   Restriction restriction = Restriction.equal("columnName");
   *
   *   // Create a condition with a specific value
   *   Condition condition = new Condition("exampleValue", restriction, property);
   *
   *   // Render the condition into an SQL buffer
   *   StringBuilder sqlBuffer = new StringBuilder("SELECT * FROM table WHERE ");
   *   condition.render(sqlBuffer);
   *   System.out.println(sqlBuffer.toString());
   *
   *   // Update the condition with a new value
   *   Condition updatedCondition = condition.withValue("newValue");
   *
   *   // Use the condition to set parameters in a PreparedStatement
   *   try (PreparedStatement ps = connection.prepareStatement(sqlBuffer.toString())) {
   *     int parameterIndex = 1;
   *     parameterIndex = updatedCondition.setParameter(ps, parameterIndex);
   *     ps.executeQuery();
   *   }
   *   catch (SQLException e) {
   *     e.printStackTrace();
   *   }
   * }</pre>
   *
   * @see Restriction
   * @see EntityProperty
   * @since 4.0
   */
  class Condition implements Restriction {

    public final Object value;

    public final Restriction restriction;

    public final EntityProperty entityProperty;

    public final boolean logicalAnd;

    public Condition(Object value, Restriction restriction, EntityProperty entityProperty) {
      this(value, restriction, entityProperty, true);
    }

    public Condition(Object value, Restriction restriction, EntityProperty entityProperty, boolean logicalAnd) {
      this.value = value;
      this.restriction = restriction;
      this.entityProperty = entityProperty;
      this.logicalAnd = logicalAnd;
    }

    /**
     * Renders the SQL representation of this condition into the provided {@code StringBuilder}.
     * This method delegates the rendering logic to the underlying {@code Restriction} instance
     * associated with this condition.
     *
     * <p>This method is typically used internally during the construction of SQL queries
     * to append the condition's SQL fragment to the query buffer.
     *
     * <p><strong>Example Usage:</strong>
     * <pre>{@code
     *   StringBuilder sqlBuffer = new StringBuilder();
     *   Condition condition = new Condition("value", someRestriction, someEntityProperty);
     *
     *   // Render the condition into the SQL buffer
     *   condition.render(sqlBuffer);
     *
     *   // The sqlBuffer now contains the SQL representation of the condition
     *   System.out.println(sqlBuffer.toString());
     * }</pre>
     *
     * @param sqlBuffer the {@code StringBuilder} to which the SQL representation of this condition
     * will be appended; must not be null
     */
    @Override
    public void render(StringBuilder sqlBuffer) {
      restriction.render(sqlBuffer);
    }

    @Override
    public boolean logicalAnd() {
      return logicalAnd;
    }

    /**
     * Creates a new {@code Condition} instance with the specified property value,
     * while retaining the existing restriction and entity property.
     *
     * <p>This method is useful when you need to create a new condition based on
     * an updated or modified property value without altering the underlying
     * restriction logic or entity property mapping.
     *
     * <p><strong>Example Usage:</strong>
     * <pre>{@code
     *   Condition originalCondition = new Condition("oldValue", someRestriction, someEntityProperty);
     *   Condition updatedCondition = originalCondition.withValue("newValue");
     *
     *   // The updatedCondition now uses "newValue" while keeping the same
     *   // restriction and entity property as the originalCondition.
     * }</pre>
     *
     * @param propertyValue the new value to be used in the condition; can be null
     * @return a new {@code Condition} instance with the updated property value
     * @since 5.0
     */
    public Condition withValue(Object propertyValue) {
      return new Condition(propertyValue, restriction, entityProperty);
    }

    /**
     * <p>Sets the value of the designated parameter using the given object.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @return Returns next parameterIndex
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs;
     * this method is called on a closed {@code PreparedStatement}
     * or the type of the given object is ambiguous
     */
    public int setParameter(PreparedStatement ps, int parameterIndex) throws SQLException {
      entityProperty.setParameter(ps, parameterIndex++, value);
      return parameterIndex;
    }

  }
}
