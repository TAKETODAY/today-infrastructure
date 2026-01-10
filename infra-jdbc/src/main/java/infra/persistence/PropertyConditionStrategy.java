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

import org.jspecify.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;

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
   * Resolves a condition based on the provided logical operator, entity property, and value.
   *
   * <p>This method evaluates the given parameters and generates a {@link Condition} object
   * if applicable. The logical operator determines how the condition is structured, while
   * the entity property and value define the specific criteria for the condition.</p>
   *
   * <p><strong>Example Usage:</strong></p>
   *
   * <pre>{@code
   * EntityProperty property = new EntityProperty("age");
   * Object value = 30;
   *
   * // Resolve a condition with logical AND
   * Condition condition = strategy.resolve(true, property, value);
   * if (condition != null) {
   *   System.out.println("Condition resolved: " + condition);
   * }
   * }</pre>
   *
   * @param logicalAnd Indicates whether the condition should use a logical AND operator.
   * If {@code true}, the condition will be resolved with AND logic;
   * otherwise, it may use a different logical operator.
   * @param entityProperty The property of the entity to be evaluated in the condition.
   * This defines the field or attribute being queried.
   * @param value The value to compare against the entity property.
   * This can be {@code null}, depending on the use case.
   * @return A {@link Condition} object representing the resolved condition,
   * or {@code null} if no condition can be resolved based on the inputs.
   */
  @Nullable
  Condition resolve(boolean logicalAnd, EntityProperty entityProperty, Object value);

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

    /**
     * Constructs a new {@code Condition} instance with the specified value, restriction,
     * and entity property. The logical AND operation is enabled by default.
     *
     * <p>This constructor is commonly used to define conditions for SQL queries, where
     * the {@code value} represents the condition's value, the {@code restriction} defines
     * the type of restriction (e.g., equality, inequality), and the {@code entityProperty}
     * specifies the property of the entity being restricted.
     *
     * <p><strong>Example Usage:</strong>
     * <pre>{@code
     *   // Create a condition for an entity property "name" with the value "John"
     *   EntityProperty nameProperty = ...;
     *   Restriction equalsRestriction = Restriction.equal("name");
     *
     *   Condition condition = new Condition("John", equalsRestriction, nameProperty);
     *
     *   // This condition can now be used in query construction
     * }</pre>
     *
     * @param value the value to be used in the condition
     * @param restriction the restriction type defining the condition logic
     * @param entityProperty the entity property to which the condition applies
     */
    public Condition(Object value, Restriction restriction, EntityProperty entityProperty) {
      this(value, restriction, entityProperty, true);
    }

    /**
     * Constructs a new {@code Condition} instance with the specified value, restriction,
     * entity property, and logical operator. This constructor allows explicit control
     * over whether the condition should be combined using a logical AND or OR operation.
     *
     * <p>This constructor is commonly used to define conditions for SQL queries, where
     * the {@code value} represents the condition's value, the {@code restriction} defines
     * the type of restriction (e.g., equality, inequality), and the {@code entityProperty}
     * specifies the property of the entity being restricted. The {@code logicalAnd} parameter
     * determines how this condition will be logically combined with other conditions.
     *
     * <p><strong>Example Usage:</strong>
     * <pre>{@code
     *   // Create a condition for an entity property "age" with the value 30
     *   EntityProperty ageProperty = ...;
     *   Restriction greaterThanRestriction = Restriction.graterThan("age");
     *
     *   // Logical AND is enabled
     *   Condition condition = new Condition(30, greaterThanRestriction, ageProperty, true);
     *
     *   // This condition can now be used in query construction
     * }</pre>
     *
     * @param value the value to be used in the condition; can be null
     * @param restriction the restriction type defining the condition logic; must not be null
     * @param entityProperty the entity property to which the condition applies; must not be null
     * @param logicalAnd a boolean flag indicating whether this condition should be combined
     * using a logical AND operation (true) or a logical OR operation (false)
     * @since 5.0
     */
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
     * @param propertyValue the new value to be used in the condition
     * @return a new {@code Condition} instance with the updated property value
     * @since 5.0
     */
    public Condition withValue(Object propertyValue) {
      return new Condition(propertyValue, restriction, entityProperty, logicalAnd);
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
