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

import java.lang.annotation.Annotation;
import java.util.List;

import infra.beans.BeanProperty;
import infra.core.annotation.MergedAnnotation;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.reflect.Property;
import infra.util.StringUtils;

/**
 * An interface for discovering column names associated with a given property.
 * Implementations of this interface provide strategies to resolve column names
 * based on various criteria, such as property names, annotations, or custom logic.
 *
 * <p>Example usage:
 * <pre>{@code
 * ColumnNameDiscover discover = ColumnNameDiscover.composite(
 *   ColumnNameDiscover.forColumnAnnotation(),
 *   ColumnNameDiscover.camelCaseToUnderscore()
 * );
 *
 * BeanProperty property = ...; // Obtain a BeanProperty instance
 * String columnName = discover.getColumnName(property);
 * System.out.println("Resolved column name: " + columnName);
 * }</pre>
 *
 * <p>The interface also provides static factory methods to create common implementations:
 * <ul>
 *   <li>{@link #forPropertyName()} - Uses the property name directly as the column name.</li>
 *   <li>{@link #camelCaseToUnderscore()} - Converts camelCase property names to underscore format.</li>
 *   <li>{@link #forColumnAnnotation()} - Resolves column names from the {@link Column} annotation.</li>
 *   <li>{@link #forAnnotation(Class)} - Resolves column names from a custom annotation.</li>
 * </ul>
 *
 * <p>Additionally, the {@link #and(ColumnNameDiscover)} method allows chaining multiple
 * discovery strategies, and the {@link #composite(List)} method enables combining multiple
 * strategies into a single resolver.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanProperty
 * @see StringUtils#camelCaseToUnderscore(String)
 * @see Column
 * @since 4.0 2022/8/16 22:03
 */
public interface ColumnNameDiscover {

  /**
   * Retrieves the column name associated with the given {@link BeanProperty}.
   * If no column name can be determined, the method may return {@code null}.
   *
   * <p>This method is typically used in scenarios where mapping between object properties
   * and database columns is required. For example, it can be used to resolve column names
   * based on property names, annotations, or other conventions.</p>
   *
   * <p><strong>Example Usage:</strong></p>
   *
   * <pre>{@code
   * ColumnNameDiscover resolver = ColumnNameDiscover.forPropertyName();
   * BeanProperty property = BeanProperty.of("firstName");
   *
   * String columnName = resolver.getColumnName(property);
   * if (columnName != null) {
   *   System.out.println("Resolved column name: " + columnName);
   * }
   * else {
   *   System.out.println("Column name could not be resolved.");
   * }
   * }</pre>
   *
   * @param property the {@link BeanProperty} for which the column name is to be retrieved
   * @return the resolved column name as a {@link String}, or {@code null} if no column name
   * can be determined for the given property
   */
  @Nullable
  String getColumnName(BeanProperty property);

  // static

  /**
   * Combines this {@code ColumnNameDiscover} with another one to form a composite resolver.
   * The returned resolver first attempts to retrieve the column name using the current
   * instance. If no column name is resolved (i.e., the result is {@code null}), it delegates
   * to the provided {@code next} resolver.
   *
   * <p>This method is useful for creating a chain of resolvers that apply different strategies
   * to resolve column names in a fallback manner.</p>
   *
   * <p><strong>Example Usage:</strong></p>
   *
   * <pre>{@code
   * ColumnNameDiscover resolver1 = ColumnNameDiscover.forPropertyName();
   * ColumnNameDiscover resolver2 = ColumnNameDiscover.forColumnAnnotation();
   *
   * ColumnNameDiscover compositeResolver = resolver1.and(resolver2);
   * BeanProperty property = BeanProperty.of("firstName");
   *
   * String columnName = compositeResolver.getColumnName(property);
   * if (columnName != null) {
   *   System.out.println("Resolved column name: " + columnName);
   * }
   * else {
   *   System.out.println("Column name could not be resolved.");
   * }
   * }</pre>
   *
   * @param next the next {@code ColumnNameDiscover} to use if this instance cannot resolve
   * a column name
   * @return a new {@code ColumnNameDiscover} that combines this instance with the provided
   * {@code next} resolver
   */
  default ColumnNameDiscover and(ColumnNameDiscover next) {
    return property -> {
      String columnName = getColumnName(property);
      if (columnName == null) {
        return next.getColumnName(property);
      }
      return columnName;
    };
  }

  /**
   * Creates a composite {@code ColumnNameDiscover} by combining multiple resolvers.
   * The resulting resolver attempts to retrieve the column name using the provided
   * resolvers in sequence. If one resolver fails to resolve a column name (i.e., returns
   * {@code null}), the next resolver in the chain is consulted.
   *
   * <p>This method is particularly useful when you want to apply multiple strategies for
   * resolving column names, such as combining property name conventions, annotations, or
   * custom logic.</p>
   *
   * <p><strong>Example Usage:</strong></p>
   *
   * <pre>{@code
   * ColumnNameDiscover resolver1 = ColumnNameDiscover.forPropertyName();
   * ColumnNameDiscover resolver2 = ColumnNameDiscover.forColumnAnnotation();
   * ColumnNameDiscover resolver3 = ColumnNameDiscover.camelCaseToUnderscore();
   *
   * ColumnNameDiscover compositeResolver = ColumnNameDiscover.composite(resolver1, resolver2, resolver3);
   * BeanProperty property = BeanProperty.of("firstName");
   *
   * String columnName = compositeResolver.getColumnName(property);
   * if (columnName != null) {
   *   System.out.println("Resolved column name: " + columnName);
   * }
   * else {
   *   System.out.println("Column name could not be resolved.");
   * }
   * }</pre>
   *
   * @param discovers an array of {@code ColumnNameDiscover} instances to be combined into a composite resolver
   * @return a new {@code ColumnNameDiscover} that resolves column names by sequentially consulting the provided resolvers
   * @throws IllegalArgumentException if the input array is {@code null}
   */
  static ColumnNameDiscover composite(ColumnNameDiscover... discovers) {
    Assert.notNull(discovers, "ColumnNameDiscover is required");
    return composite(List.of(discovers));
  }

  /**
   * Creates a composite {@code ColumnNameDiscover} by combining multiple resolvers provided as a list.
   * The resulting resolver attempts to retrieve the column name using the provided resolvers in sequence.
   * If one resolver fails to resolve a column name (i.e., returns {@code null}), the next resolver in the
   * chain is consulted. This process continues until a non-null column name is found or all resolvers have
   * been exhausted.
   *
   * <p>This method is particularly useful when you want to apply multiple strategies for resolving column
   * names, such as combining property name conventions, annotations, or custom logic.</p>
   *
   * <p><strong>Example Usage:</strong></p>
   *
   * <pre>{@code
   * ColumnNameDiscover resolver1 = ColumnNameDiscover.forPropertyName();
   * ColumnNameDiscover resolver2 = ColumnNameDiscover.forColumnAnnotation();
   * ColumnNameDiscover resolver3 = ColumnNameDiscover.camelCaseToUnderscore();
   *
   * List<ColumnNameDiscover> discovers = Arrays.asList(resolver1, resolver2, resolver3);
   * ColumnNameDiscover compositeResolver = ColumnNameDiscover.composite(discovers);
   *
   * BeanProperty property = BeanProperty.of("firstName");
   *
   * String columnName = compositeResolver.getColumnName(property);
   * if (columnName != null) {
   *   System.out.println("Resolved column name: " + columnName);
   * }
   * else {
   *   System.out.println("Column name could not be resolved.");
   * }
   * }</pre>
   *
   * @param discovers a list of {@code ColumnNameDiscover} instances to be combined into a composite resolver
   * @return a new {@code ColumnNameDiscover} that resolves column names by sequentially consulting the provided resolvers
   * @throws IllegalArgumentException if the input list is {@code null}
   */
  static ColumnNameDiscover composite(List<ColumnNameDiscover> discovers) {
    Assert.notNull(discovers, "ColumnNameDiscover is required");
    return property -> {
      for (ColumnNameDiscover discover : discovers) {
        String columnName = discover.getColumnName(property);
        if (columnName != null) {
          return columnName;
        }
      }
      return null;
    };
  }

  /**
   * Returns a {@code ColumnNameDiscover} that resolves column names based on the property name
   * of a given {@link BeanProperty}. This method is typically used in scenarios where the column
   * name directly corresponds to the property name of a bean.
   *
   * <p><strong>Example Usage:</strong></p>
   *
   * <pre>{@code
   *   ColumnNameDiscover resolver = ColumnNameDiscover.forPropertyName();
   *   BeanProperty property = BeanProperty.of("firstName");
   *
   *   String columnName = resolver.getColumnName(property);
   *   if (columnName != null) {
   *     System.out.println("Resolved column name: " + columnName);
   *   }
   *   else {
   *     System.out.println("Column name could not be resolved.");
   *   }
   * }</pre>
   *
   * @return a {@code ColumnNameDiscover} instance that resolves column names using the property name
   */
  static ColumnNameDiscover forPropertyName() {
    return Property::getName;
  }

  /**
   * Returns a {@code ColumnNameDiscover} that converts camelCase property names to
   * underscore-separated (snake_case) column names. This method is useful when mapping
   * Java bean properties to database column names that follow a snake_case naming convention.
   *
   * <p><strong>Example Usage:</strong></p>
   *
   * <pre>{@code
   *   ColumnNameDiscover resolver = ColumnNameDiscover.camelCaseToUnderscore();
   *   BeanProperty property = BeanProperty.of("firstName");
   *
   *   String columnName = resolver.getColumnName(property);
   *   if (columnName != null) {
   *     System.out.println("Resolved column name: " + columnName);
   *     // Output: Resolved column name: first_name
   *   }
   *   else {
   *     System.out.println("Column name could not be resolved.");
   *   }
   * }</pre>
   *
   * <p>This implementation uses the {@link StringUtils#camelCaseToUnderscore(String)} utility
   * method to perform the conversion.</p>
   *
   * @return a {@code ColumnNameDiscover} instance that resolves column names by converting
   * camelCase property names to underscore-separated (snake_case) format
   */
  static ColumnNameDiscover camelCaseToUnderscore() {
    return property -> {
      String propertyName = property.getName();
      return StringUtils.camelCaseToUnderscore(propertyName);
    };
  }

  /**
   * Returns a {@code ColumnNameDiscover} instance configured to discover
   * column names based on the {@code Column} annotation.
   *
   * <p>This method is a convenience wrapper around {@code forAnnotation(Class<T>)}
   * and is specifically tailored for the {@code Column} annotation.
   *
   * <p><b>Example Usage:</b>
   * <pre>{@code
   *   ColumnNameDiscover discoverer = forColumnAnnotation();
   *
   *   // Use the discoverer to extract column names from annotated fields
   *   String columnName = discoverer.getColumnName(property);
   *   System.out.println("Column Name: " + columnName);
   * }</pre>
   *
   * @return a {@code ColumnNameDiscover} instance for discovering column names
   * using the {@code Column} annotation
   */
  static ColumnNameDiscover forColumnAnnotation() {
    return forAnnotation(Column.class);
  }

  /**
   * Creates a {@code ColumnNameDiscover} instance based on the provided annotation type.
   * This method is a convenience overload that uses the default attribute name
   * {@code MergedAnnotation.VALUE} for discovering column names.
   *
   * <p>Example usage:
   * <pre>{@code
   * ColumnNameDiscover discover = ColumnNameDiscover.forAnnotation(MyColumn.class);
   * String columnName = discover.getColumnName(property);
   * }</pre>
   *
   * @param annotationType the type of the annotation to be used for column name discovery;
   * must not be null
   * @return a {@code ColumnNameDiscover} instance configured to extract column names
   * from the specified annotation type
   */
  static ColumnNameDiscover forAnnotation(Class<? extends Annotation> annotationType) {
    return forAnnotation(annotationType, MergedAnnotation.VALUE);
  }

  /**
   * Creates a {@code ColumnNameDiscover} instance that extracts column names
   * from the specified annotation type and attribute name.
   * <p>
   * This method is useful when you need to dynamically resolve column names
   * based on annotations present on properties. For example, it can be used
   * in frameworks that map object properties to database columns.
   *
   * <pre>{@code
   * // Example usage:
   * ColumnNameDiscover discover = forAnnotation(Column.class, "name");
   * String columnName = discover.getColumnName(property);
   * }</pre>
   *
   * @param annotationType the type of annotation to search for; must not be null
   * @param attributeName the name of the attribute within the annotation
   * that holds the column name; must not be null
   * @return a {@code ColumnNameDiscover} function that retrieves the column
   * name from the specified annotation and attribute, or null if
   * the annotation or attribute is not present or empty
   */
  static ColumnNameDiscover forAnnotation(Class<? extends Annotation> annotationType, String attributeName) {
    Assert.notNull(attributeName, "attributeName is required");
    Assert.notNull(annotationType, "annotationType is required");

    return property -> {
      var annotation = property.mergedAnnotations().get(annotationType);
      if (annotation.isPresent()) {
        String columnName = annotation.getString(attributeName);
        if (StringUtils.hasText(columnName)) {
          return columnName;
        }
      }

      return null;
    };
  }

}
