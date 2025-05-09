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
   * Resolves a condition based on the provided entity property and extracted value.
   * This method processes the extracted value and entity property annotations to
   * construct a {@code Condition} object that can be used in SQL query building.
   *
   * <p>If the extracted value is a {@code String} and the entity property is annotated
   * with {@code @TrimWhere}, the string is trimmed before further processing.
   *
   * <p>The method checks for the presence of a {@code @Where} annotation on the entity
   * property. If the annotation is present, it uses its attributes to determine the
   * restriction logic for the condition. If the annotation's value is not the default
   * "none", it creates a plain restriction. Otherwise, it evaluates the "operator"
   * attribute to determine the appropriate restriction type.
   *
   * <p><strong>Example Usage:</strong>
   * <pre>{@code
   *   EntityProperty property = ...; // Obtain an EntityProperty instance
   *   Object extractedValue = " example ";
   *
   *   WhereAnnotationConditionStrategy strategy = new WhereAnnotationConditionStrategy();
   *   Condition condition = strategy.resolve(property, extractedValue);
   *
   *   if (condition != null) {
   *     StringBuilder sqlBuffer = new StringBuilder("SELECT * FROM table WHERE ");
   *     condition.render(sqlBuffer);
   *     System.out.println(sqlBuffer.toString());
   *   }
   * }</pre>
   *
   * @param entityProperty the entity property to resolve the condition for; must not be null
   * @param extracted the extracted value to be used in the condition; can be null
   * @return a {@code Condition} object representing the resolved condition, or null
   * if no condition can be resolved based on the provided inputs
   */
  @Nullable
  @Override
  public Condition resolve(EntityProperty entityProperty, Object extracted) {
    if (extracted instanceof String string && entityProperty.isPresent(TrimWhere.class)) {
      extracted = string.trim();
    }

    // render where clause
    MergedAnnotation<Where> annotation = entityProperty.getAnnotation(Where.class);
    if (annotation.isPresent()) {
      String value = annotation.getStringValue();
      if (!Constant.DEFAULT_NONE.equals(value)) {
        return new Condition(extracted, Restriction.plain(value), entityProperty);
      }
      else {
        String operator = annotation.getString("operator");
        if (Constant.DEFAULT_NONE.equals(operator)) {
          // default to equality operator
          return new Condition(extracted, Restriction.equal(entityProperty.columnName), entityProperty);
        }
        else {
          return new Condition(extracted, Restriction.forOperator(
                  entityProperty.columnName, operator, "?"), entityProperty);
        }
      }
    }
    return null;
  }

}
