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
import java.util.Set;

import infra.beans.BeanProperty;
import infra.lang.Assert;

/**
 * A filter interface used to determine whether a bean property should be excluded
 * from mapping to a database column. This is particularly useful in scenarios where
 * certain properties are transient, annotated with specific annotations, or explicitly
 * excluded by name.
 *
 * <p>This interface provides several static factory methods and a default method to
 * combine filters, allowing flexible and reusable property filtering logic.
 *
 * <h3>Usage Examples</h3>
 *
 * <p>1. Filter properties by their names:
 * <pre>{@code
 * Set<String> filteredNames = Set.of("id", "version");
 * PropertyFilter filter = PropertyFilter.filteredNames(filteredNames);
 *
 * BeanProperty property = ...;
 * if (filter.isFiltered(property)) {
 *   // The property is excluded
 * }
 * }</pre>
 *
 * <p>2. Combine multiple filters using the {@code and} method:
 * <pre>{@code
 * PropertyFilter filter1 = PropertyFilter.filteredNames(Set.of("id"));
 * PropertyFilter filter2 = PropertyFilter.forTransientAnnotation();
 * PropertyFilter combinedFilter = filter1.and(filter2);
 *
 * BeanProperty property = ...;
 * if (combinedFilter.isFiltered(property)) {
 *   // The property is excluded by either filter1 or filter2
 * }
 * }</pre>
 *
 * <p>3. Use annotation-based filtering:
 * <pre>{@code
 * PropertyFilter filter = PropertyFilter.forAnnotation(MyCustomAnnotation.class);
 *
 * BeanProperty property = ...;
 * if (filter.isFiltered(property)) {
 *   // The property is annotated with MyCustomAnnotation
 * }
 * }</pre>
 *
 * <p>4. Accept all properties (no filtering):
 * <pre>{@code
 * PropertyFilter filter = PropertyFilter.acceptAny();
 *
 * BeanProperty property = ...;
 * if (!filter.isFiltered(property)) {
 *   // All properties are accepted
 * }
 * }</pre>
 *
 * <p>5. Filter properties annotated with {@code @Transient}:
 * <pre>{@code
 * PropertyFilter filter = PropertyFilter.forTransientAnnotation();
 *
 * BeanProperty property = ...;
 * if (filter.isFiltered(property)) {
 *   // The property is marked as transient
 * }
 * }</pre>
 *
 * <h3>Key Methods</h3>
 * <ul>
 *   <li>{@link #isFiltered(BeanProperty)}: Determines if a property should be filtered out.</li>
 *   <li>{@link #and(PropertyFilter)}: Combines this filter with another filter.</li>
 *   <li>{@link #filteredNames(Set)}: Creates a filter based on a set of property names.</li>
 *   <li>{@link #acceptAny()}: Creates a filter that accepts all properties.</li>
 *   <li>{@link #forTransientAnnotation()}: Creates a filter for properties annotated with {@code @Transient}.</li>
 *   <li>{@link #forAnnotation(Class)}: Creates a filter for properties annotated with a specific annotation.</li>
 * </ul>
 *
 * <p>This interface is designed to be extensible and composable, making it suitable for
 * various ORM or data mapping frameworks.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 22:29
 */
public interface PropertyFilter {

  /**
   * Checks if the given {@code BeanProperty} is filtered by this {@code PropertyFilter}.
   * <p>
   * This method is typically used to determine whether a property should be excluded
   * from operations such as serialization, deserialization, or database mapping.
   * </p>
   * <p>
   * Example usage:
   * <pre>{@code
   * PropertyFilter filter = PropertyFilter.filteredNames(Set.of("password"));
   * BeanProperty property = BeanProperty.valueOf(User.class, "password");
   *
   * if (filter.isFiltered(property)) {
   *   System.out.println("The property 'password' is filtered.");
   * }
   * else {
   *   System.out.println("The property 'password' is not filtered.");
   * }
   * }</pre>
   * </p>
   *
   * @param property the {@code BeanProperty} to check
   * @return {@code true} if the property is filtered, {@code false} otherwise
   */
  boolean isFiltered(BeanProperty property);

  /**
   * Combines this {@code PropertyFilter} with another {@code PropertyFilter} using a logical OR operation.
   * <p>
   * The resulting {@code PropertyFilter} will filter a property if either this filter or the provided
   * {@code next} filter determines that the property should be filtered. This is useful for chaining multiple
   * filtering conditions together.
   * </p>
   * <p>
   * Example usage:
   * <pre>{@code
   * PropertyFilter filter1 = PropertyFilter.filteredNames(Set.of("password"));
   * PropertyFilter filter2 = PropertyFilter.forAnnotation(Transient.class);
   *
   * PropertyFilter combinedFilter = filter1.and(filter2);
   *
   * BeanProperty passwordProperty = BeanProperty.valueOf(User.class, "password");
   * BeanProperty emailProperty = BeanProperty.valueOf(User.class, "email");
   *
   * if (combinedFilter.isFiltered(passwordProperty)) {
   *   System.out.println("The property 'password' is filtered.");
   * }
   * if (combinedFilter.isFiltered(emailProperty)) {
   *   System.out.println("The property 'email' is filtered.");
   * }
   * }</pre>
   * </p>
   *
   * @param next the next {@code PropertyFilter} to combine with this filter
   * @return a new {@code PropertyFilter} that applies the logical OR of this filter and the {@code next} filter
   */
  default PropertyFilter and(PropertyFilter next) {
    return beanProperty -> isFiltered(beanProperty) || next.isFiltered(beanProperty);
  }

  /**
   * Creates a {@code PropertyFilter} that filters properties based on their names.
   * <p>
   * The returned filter will exclude any property whose name is contained in the provided
   * {@code filteredNames} set. This is useful for excluding specific properties from operations
   * such as serialization, deserialization, or database mapping.
   * </p>
   * <p>
   * Example usage:
   * </p>
   * <pre>{@code
   * Set<String> filteredNames = Set.of("password", "secretKey");
   * PropertyFilter filter = PropertyFilter.filteredNames(filteredNames);
   *
   * BeanProperty passwordProperty = BeanProperty.valueOf(User.class, "password");
   * BeanProperty emailProperty = BeanProperty.valueOf(User.class, "email");
   *
   * if (filter.isFiltered(passwordProperty)) {
   *   System.out.println("The property 'password' is filtered.");
   * }
   * if (!filter.isFiltered(emailProperty)) {
   *   System.out.println("The property 'email' is not filtered.");
   * }
   * }</pre>
   *
   * @param filteredNames a set of property names to be filtered. Must not be empty.
   * @return a {@code PropertyFilter} that filters properties based on the provided names.
   * @throws IllegalArgumentException if {@code filteredNames} is empty.
   */
  static PropertyFilter filteredNames(Set<String> filteredNames) {
    Assert.notEmpty(filteredNames, "filteredNames is empty");
    return property -> filteredNames.contains(property.getName());
  }

  /**
   * Returns a {@code PropertyFilter} that accepts any property, effectively
   * filtering out all properties by always returning {@code false}.
   * <p>
   * This method is useful when you want to create a filter that excludes
   * all properties from operations such as serialization, deserialization,
   * or database mapping.
   * </p>
   * <p>
   * Example usage:
   * </p>
   * <pre>{@code
   * PropertyFilter filter = PropertyFilter.acceptAny();
   *
   * BeanProperty property = BeanProperty.valueOf(User.class, "username");
   * if (filter.isFiltered(property)) {
   *   System.out.println("The property 'username' is filtered.");
   * }
   * else {
   *   System.out.println("The property 'username' is not filtered.");
   * }
   * }</pre>
   *
   * @return a {@code PropertyFilter} that filters all properties
   */
  static PropertyFilter acceptAny() {
    return property -> false;
  }

  /**
   * Creates a {@code PropertyFilter} that filters properties annotated with the
   * {@link Transient} annotation. This is useful for excluding properties marked
   * as non-persistent from operations such as serialization, deserialization,
   * or database mapping.
   * <p>
   * Example usage:
   * </p>
   * <pre>{@code
   *   PropertyFilter filter = PropertyFilter.forTransientAnnotation();
   *
   *   BeanProperty transientProperty = BeanProperty.valueOf(Employee.class, "currentUser");
   *   BeanProperty idProperty = BeanProperty.valueOf(Employee.class, "id");
   *
   *   if (filter.isFiltered(transientProperty)) {
   *     System.out.println("The property 'currentUser' is filtered because it is annotated with @Transient.");
   *   }
   *   if (!filter.isFiltered(idProperty)) {
   *     System.out.println("The property 'id' is not filtered because it is not annotated with @Transient.");
   *   }
   * }</pre>
   *
   * @return a {@code PropertyFilter} that filters properties annotated with {@link Transient}
   */
  static PropertyFilter forTransientAnnotation() {
    return forAnnotation(Transient.class);
  }

  /**
   * Creates a {@code PropertyFilter} that filters properties annotated with the specified
   * annotation type. This is useful for excluding properties marked with a specific annotation
   * from operations such as serialization, deserialization, or database mapping.
   * <p>
   * Example usage:
   * </p>
   * <pre>{@code
   *   PropertyFilter filter = PropertyFilter.forAnnotation(MyCustomAnnotation.class);
   *
   *   BeanProperty annotatedProperty = BeanProperty.valueOf(MyClass.class, "annotatedField");
   *   BeanProperty nonAnnotatedProperty = BeanProperty.valueOf(MyClass.class, "normalField");
   *
   *   if (filter.isFiltered(annotatedProperty)) {
   *     System.out.println("The property 'annotatedField' is filtered because it has @MyCustomAnnotation.");
   *   }
   *   if (!filter.isFiltered(nonAnnotatedProperty)) {
   *     System.out.println("The property 'normalField' is not filtered because it lacks @MyCustomAnnotation.");
   *   }
   * }</pre>
   *
   * @param annotationType the type of annotation to filter by. Must not be null.
   * @return a {@code PropertyFilter} that filters properties annotated with the specified annotation type.
   * @throws IllegalArgumentException if {@code annotationType} is null.
   */
  static PropertyFilter forAnnotation(Class<? extends Annotation> annotationType) {
    Assert.notNull(annotationType, "annotationType is required");
    return property -> property.mergedAnnotations().isPresent(annotationType);
  }

}
