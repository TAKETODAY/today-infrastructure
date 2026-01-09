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
 * Indicates that a field, method, or class should be treated as a "prefix-like" query condition.
 * This annotation is a specialized form of the {@link Like} annotation and is retained at runtime.
 * It is typically used in conjunction with query builders or ORM frameworks to generate
 * SQL-like conditions dynamically, specifically for prefix-based searches.
 *
 * <p>The {@code PrefixLike} annotation allows specifying the column name or where-clause predicate,
 * and whether the value should be trimmed before processing. By default, the value is trimmed to
 * remove leading and trailing whitespace.</p>
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
 * Example 1: Basic usage on a field for prefix search
 * <pre>{@code
 * @PrefixLike(value = "name")
 * private String searchName;
 * }</pre>
 *
 * Example 2: Disabling trimming for a specific field
 * <pre>{@code
 * @PrefixLike(value = "description", trim = false)
 * private String descriptionQuery;
 * }</pre>
 *
 * Example 3: Using alias attributes interchangeably
 * <pre>{@code
 * @PrefixLike(column = "email")
 * private String emailFilter;
 * }</pre>
 *
 * <p>This annotation is particularly useful in scenarios where dynamic query conditions
 * are required, such as filtering data based on user input with a prefix match.</p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/24 23:53
 */
@Like
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface PrefixLike {

  /**
   * The where-clause predicate.
   */
  @AliasFor(annotation = Like.class, attribute = "value")
  String value() default Constant.DEFAULT_NONE;

  @AliasFor(annotation = Like.class, attribute = "column")
  String column() default Constant.DEFAULT_NONE;

  @AliasFor(annotation = Like.class, attribute = "trim")
  boolean trim() default true;

}
