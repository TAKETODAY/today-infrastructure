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

import org.jspecify.annotations.Nullable;

import infra.core.annotation.MergedAnnotation;
import infra.lang.Constant;
import infra.persistence.EntityProperty;
import infra.persistence.PropertyConditionStrategy;
import infra.persistence.ValueNormalizer;
import infra.persistence.annotation.Where;
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
   * This method processes the {@code extracted} value and uses the {@code @Where}
   * annotation to determine the appropriate condition to return.
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
   *   <li>If the {@code @Where} annotation is present, its value or operator is used to construct the condition.</li>
   *   <li>If no valid condition can be resolved, the method returns {@code null}.</li>
   * </ul>
   *
   * @param logicalAnd Indicates whether the condition should be combined using a logical AND operation.
   * @param entityProperty The entity property associated with the condition. Must not be {@code null}.
   * @param value The extracted value to be used in the condition.
   * @return A {@link Condition} object if a valid condition is resolved, or {@code null} if no condition can be determined.
   */
  @Override
  public @Nullable Condition resolve(boolean logicalAnd, EntityProperty entityProperty, Object value,
          ValueNormalizer valueNormalizer) {
    // render where clause
    MergedAnnotation<Where> annotation = entityProperty.getAnnotation(Where.class);
    if (annotation.isPresent()) {
      value = valueNormalizer.normalize(entityProperty, value);
      String restriction = annotation.getStringValue();
      if (!Constant.DEFAULT_NONE.equals(restriction)) {
        return new Condition(value, Restriction.plain(restriction), entityProperty, logicalAnd);
      }
      else {
        String operator = annotation.getString("operator");
        if (Constant.DEFAULT_NONE.equals(operator)) {
          // default to equality operator
          return new Condition(value, Restriction.equal(entityProperty.getColumnName()), entityProperty, logicalAnd);
        }
        else {
          return new Condition(value, Restriction.forOperator(
                  entityProperty.getColumnName(), operator, "?"), entityProperty, logicalAnd);
        }
      }
    }
    return null;
  }

}
