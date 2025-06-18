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

package infra.persistence.support;

import infra.core.annotation.MergedAnnotation;
import infra.lang.Constant;
import infra.lang.Nullable;
import infra.persistence.EntityProperty;
import infra.persistence.PropertyConditionStrategy;
import infra.persistence.TrimWhere;
import infra.persistence.Where;
import infra.persistence.sql.Restriction;

/**
 * A strategy implementation for resolving SQL WHERE conditions based on the
 * presence and configuration of the {@code @Where} annotation on an entity property.
 *
 * <p>This class implements the {@link PropertyConditionStrategy} interface and
 * provides a mechanism to dynamically generate SQL conditions by inspecting the
 * {@code @Where} annotation associated with an entity property. It supports
 * customizing the condition using the annotation's attributes, such as the
 * operator or a predefined SQL fragment.
 *
 * <p>If the {@code @Where} annotation is present, this strategy resolves the
 * condition as follows:
 * <ul>
 *   <li>If the annotation specifies a non-default SQL fragment, it uses that fragment.</li>
 *   <li>If no SQL fragment is specified, it falls back to using the operator defined
 *       in the annotation (defaulting to equality if no operator is provided).</li>
 * </ul>
 *
 * <p>Additionally, if the property is annotated with {@code @TrimWhere}, any string
 * value extracted for the condition will be trimmed before processing.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * // Define an entity property with a @Where annotation
 * EntityProperty property = ...; // Obtain an EntityProperty instance
 * Object extractedValue = "  exampleValue  ";
 *
 * // Resolve the condition using WhereAnnotationConditionStrategy
 * WhereAnnotationConditionStrategy strategy = new WhereAnnotationConditionStrategy();
 * Condition condition = strategy.resolve(property, extractedValue);
 *
 * if (condition != null) {
 *   StringBuilder sql = new StringBuilder("SELECT * FROM table WHERE ");
 *   condition.render(sql);
 *   System.out.println(sql.toString());
 * }
 * }</pre>
 *
 * <p>In the above example, if the {@code @Where} annotation specifies an operator
 * like "LIKE", the generated SQL might look like:
 * <pre>{@code
 * SELECT * FROM table WHERE column LIKE ?
 * }</pre>
 *
 * <p>If the annotation specifies a custom SQL fragment, such as "column > ?", the
 * generated SQL would reflect that instead:
 * <pre>{@code
 * SELECT * FROM table WHERE column > ?
 * }</pre>
 *
 * <p>This strategy is particularly useful for scenarios where dynamic query
 * construction is required based on metadata annotations.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/25 00:02
 */
public class WhereAnnotationConditionStrategy implements PropertyConditionStrategy {

  /**
   * Resolves a condition based on the provided parameters and annotations associated with the entity property.
   * This method processes the {@code extracted} value and uses annotations like {@code @Where} and {@code @TrimWhere}
   * to determine the appropriate condition to return.
   *
   * <p>Usage example:
   * <pre>{@code
   *   EntityProperty property = ...; // Obtain an EntityProperty instance
   *   Object extractedValue = "  example  "; // Example extracted value
   *   boolean logicalAnd = true; // Logical AND flag
   *
   *   Condition condition = strategy.resolve(logicalAnd, property, extractedValue);
   *   if (condition != null) {
   *     System.out.println("Resolved condition: " + condition);
   *   }
   * }</pre>
   *
   * <p>This method handles the following scenarios:
   * <ul>
   *   <li>If the {@code extracted} value is a string and the {@code @TrimWhere} annotation is present,
   *       the string is trimmed before further processing.</li>
   *   <li>If the {@code @Where} annotation is present, its value or operator is used to construct the condition.</li>
   *   <li>If no valid condition can be resolved, the method returns {@code null}.</li>
   * </ul>
   *
   * @param logicalAnd Indicates whether the condition should be combined using a logical AND operation.
   * @param entityProperty The entity property associated with the condition. Must not be {@code null}.
   * @param extracted The extracted value to be used in the condition. Can be {@code null}.
   * @return A {@link Condition} object if a valid condition is resolved, or {@code null} if no condition can be determined.
   */
  @Nullable
  @Override
  public Condition resolve(boolean logicalAnd, EntityProperty entityProperty, Object extracted) {
    if (extracted instanceof String string && entityProperty.isPresent(TrimWhere.class)) {
      extracted = string.trim();
    }

    // render where clause
    MergedAnnotation<Where> annotation = entityProperty.getAnnotation(Where.class);
    if (annotation.isPresent()) {
      String value = annotation.getStringValue();
      if (!Constant.DEFAULT_NONE.equals(value)) {
        return new Condition(extracted, Restriction.plain(value), entityProperty, logicalAnd);
      }
      else {
        String operator = annotation.getString("operator");
        if (Constant.DEFAULT_NONE.equals(operator)) {
          // default to equality operator
          return new Condition(extracted, Restriction.equal(entityProperty.columnName), entityProperty, logicalAnd);
        }
        else {
          return new Condition(extracted, Restriction.forOperator(
                  entityProperty.columnName, operator, "?"), entityProperty, logicalAnd);
        }
      }
    }
    return null;
  }

}
