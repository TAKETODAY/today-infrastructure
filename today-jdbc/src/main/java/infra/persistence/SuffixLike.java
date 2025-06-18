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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.core.annotation.AliasFor;
import infra.lang.Constant;

/**
 * Indicates that a field or parameter should be treated as a "suffix-like" query condition.
 * This annotation can be applied to classes, methods, or fields and is retained at runtime.
 * It is typically used in conjunction with query builders or ORM frameworks to generate
 * SQL-like conditions dynamically, specifically for suffix-based matching (e.g., "LIKE '%value'").
 *
 * <p>The {@code SuffixLike} annotation allows specifying the column name and whether the value
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
 * @SuffixLike(value = "name")
 * private String searchName;
 * }</pre>
 *
 * Example 2: Disabling trimming
 * <pre>{@code
 * @SuffixLike(value = "description", trim = false)
 * private String descriptionQuery;
 * }</pre>
 *
 * Example 3: Using alias attributes
 * <pre>{@code
 * @SuffixLike(column = "email")
 * private String emailFilter;
 * }</pre>
 *
 * <p>This annotation is particularly useful in scenarios where dynamic query conditions
 * are required, such as filtering data based on user input with suffix-based matching.</p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/24 23:53
 */
@Like
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SuffixLike {

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
