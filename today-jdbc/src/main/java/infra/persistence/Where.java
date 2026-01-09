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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.lang.Constant;

/**
 * An annotation used to define conditional logic for the annotated element,
 * such as filtering query results or applying runtime constraints. It is
 * typically applied to classes, methods, or fields to specify a where-clause
 * predicate.
 *
 * <p>The {@link #value()} attribute defines the primary condition as a string.
 * If no value is explicitly provided, the default is set to
 * {@link Constant#DEFAULT_NONE}, which represents the absence of a meaningful
 * default value in annotations. This ensures compatibility with annotation
 * constraints that do not allow {@code null} values.
 *
 * <p><b>Usage Examples:</b>
 *
 * <pre>{@code
 *   // Example 1: Applying a simple where-clause to a field
 *   @Where(value = "status > ?")
 *   private int status;
 *
 * }</pre>
 *
 * @author Emmanuel Bernard
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Where {

  /**
   * Returns the value of the annotation's primary condition as a string.
   * This attribute defines the main predicate or logic to be applied,
   * typically used in filtering query results or applying runtime constraints.
   *
   * <p>If no value is explicitly provided, the default is set to
   * {@link Constant#DEFAULT_NONE}, which represents the absence of a meaningful
   * default value. This ensures compatibility with annotation constraints that
   * do not allow {@code null} values.
   *
   * <p><b>Usage Examples:</b>
   *
   * <pre>{@code
   *   // Example 1: Specifying a custom condition
   *   @Where(value = "status > ?")
   *   private int status;
   *
   *   // Example 2: Using the default value
   *   @Where
   *   private String description;
   * }</pre>
   *
   * @return the string representation of the condition or
   * {@link Constant#DEFAULT_NONE} if no value is specified
   */
  String value() default Constant.DEFAULT_NONE;

  /**
   * Returns the operator associated with the annotation's condition.
   * This attribute defines the logical operator to be applied, typically
   * used in conjunction with the {@link #value()} attribute to construct
   * complex query conditions or filtering logic.
   *
   * <p>If no operator is explicitly provided, the default is set to
   * {@link Constant#DEFAULT_NONE}, which represents the absence of a meaningful
   * default value. This ensures compatibility with annotation constraints that
   * do not allow {@code null} values.
   *
   * <p><b>Usage Examples:</b>
   *
   * <pre>{@code
   *   // Example 1: Specifying a custom operator
   *   @Where(operator = "=")
   *   private int status;
   *
   *   // Example 2: Using the default operator
   *   @Where(value = "name LIKE ?")
   *   private String name;
   * }</pre>
   *
   * @return the string representation of the operator or
   * {@link Constant#DEFAULT_NONE} if no operator is specified
   */
  String operator() default Constant.DEFAULT_NONE;

}