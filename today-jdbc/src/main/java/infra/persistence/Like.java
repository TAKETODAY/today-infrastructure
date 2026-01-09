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

import infra.core.annotation.AliasFor;
import infra.lang.Constant;

/**
 * Indicates that a field or parameter should be treated as a "like" query condition.
 * This annotation can be applied to classes, methods, or fields and is retained at runtime.
 * It is typically used in conjunction with query builders or ORM frameworks to generate
 * SQL-like conditions dynamically.
 *
 * <p>The {@code Like} annotation allows specifying the column name and whether the value
 * should be trimmed before processing. By default, the value is trimmed to remove leading
 * and trailing whitespace.</p>
 *
 * <p><b>Attributes:</b></p>
 * <ul>
 *   <li>{@code value}: The column name or where-clause predicate. Defaults to {@code Constant.DEFAULT_NONE}.</li>
 *   <li>{@code column}: An alias for {@code value}. Defaults to {@code Constant.DEFAULT_NONE}.</li>
 *   <li>{@code trim}: Whether to trim the value before processing. Defaults to {@code true}.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 *
 * Example 1: Basic usage on a field
 * <pre>{@code
 * @Like(value = "name")
 * private String searchName;
 * }</pre>
 *
 * Example 2: Disabling trimming
 * <pre>{@code
 * @Like(value = "description", trim = false)
 * private String descriptionQuery;
 * }</pre>
 *
 * Example 3: Using alias attributes
 * <pre>{@code
 * @Like(column = "email")
 * private String emailFilter;
 * }</pre>
 *
 * <p>This annotation is particularly useful in scenarios where dynamic query conditions
 * are required, such as filtering data based on user input.</p>
 */
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Like {

  /**
   * Gets the column name or where-clause predicate associated with this annotation.
   * This attribute is an alias for {@code column} and defaults to {@code Constant.DEFAULT_NONE}.
   *
   * <p>This value is typically used to specify the target column in a "like" query condition.
   * It can be explicitly set or left as the default value to indicate no specific column.</p>
   *
   * <p><b>Usage Examples:</b></p>
   *
   * Example 1: Using the default value
   * <pre>{@code
   * @Like
   * private String query;
   * }</pre>
   *
   * Example 2: Specifying a column name
   * <pre>{@code
   * @Like(value = "username")
   * private String usernameFilter;
   * }</pre>
   *
   * Example 3: Using alias attributes interchangeably
   * <pre>{@code
   * @Like(column = "address")
   * private String addressQuery;
   * }</pre>
   *
   * @return the column name or where-clause predicate, or {@code Constant.DEFAULT_NONE} if not specified
   */
  @AliasFor(annotation = Like.class, attribute = "column")
  String value() default Constant.DEFAULT_NONE;

  /**
   * Gets the column name or where-clause predicate associated with this annotation.
   * This attribute is an alias for {@code value} and defaults to {@code Constant.DEFAULT_NONE}.
   *
   * <p>This value is typically used to specify the target column in a "like" query condition.
   * It can be explicitly set or left as the default value to indicate no specific column.</p>
   *
   * <p><b>Usage Examples:</b></p>
   *
   * Example 1: Using the default value
   * <pre>{@code
   * @Like
   * private String query;
   * }</pre>
   *
   * Example 2: Specifying a column name
   * <pre>{@code
   * @Like(column = "username")
   * private String usernameFilter;
   * }</pre>
   *
   * Example 3: Using alias attributes interchangeably
   * <pre>{@code
   * @Like(value = "address")
   * private String addressQuery;
   * }</pre>
   *
   * @return the column name or where-clause predicate, or {@code Constant.DEFAULT_NONE} if not specified
   */
  @AliasFor(annotation = Like.class, attribute = "value")
  String column() default Constant.DEFAULT_NONE;

  /**
   * Indicates whether the value should be trimmed before processing.
   * Trimming removes leading and trailing whitespace from the value.
   * By default, trimming is enabled ({@code true}).
   *
   * <p><b>Usage Examples:</b></p>
   *
   * Example 1: Enabling trimming (default behavior)
   * <pre>{@code
   * @Like(value = "name")
   * private String searchName;
   * }</pre>
   *
   * Example 2: Disabling trimming
   * <pre>{@code
   * @Like(value = "description", trim = false)
   * private String descriptionQuery;
   * }</pre>
   *
   * <p>When trimming is disabled, the value will be processed as-is,
   * including any leading or trailing whitespace.</p>
   *
   * @return {@code true} if trimming is enabled, {@code false} otherwise
   */
  boolean trim() default true;
}
