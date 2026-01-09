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

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;

import infra.beans.BeanProperty;
import infra.lang.Assert;

/**
 * An interface to discover and determine whether a given property is an ID property.
 * This interface provides various static factory methods and default methods to
 * create and compose {@link IdPropertyDiscover} instances for different use cases.
 *
 * <p>Example usage:
 * <pre>{@code
 *   // Create a discoverer for a specific property name
 *   IdPropertyDiscover byName = IdPropertyDiscover.forPropertyName("userId");
 *
 *   // Create a discoverer for properties annotated with @Id
 *   IdPropertyDiscover byAnnotation = IdPropertyDiscover.forIdAnnotation();
 *
 *   // Combine multiple discoverers using the composite pattern
 *   IdPropertyDiscover compositeDiscover = IdPropertyDiscover.composite(byName, byAnnotation);
 *
 *   // Use the composite discoverer to check if a property is an ID property
 *   BeanProperty property = ...;
 *   if (compositeDiscover.isIdProperty(property)) {
 *     System.out.println("The property is an ID property.");
 *   }
 *   else {
 *     System.out.println("The property is not an ID property.");
 *   }
 * }</pre>
 *
 * <p>This interface supports flexible composition of discovery logic, allowing
 * developers to define custom rules for identifying ID properties in a bean.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 21:48
 */
public interface IdPropertyDiscover {

  /**
   * The default property name used to identify the ID property of an entity.
   * This constant is typically used as a fallback or default value when determining
   * the ID property of a class in the absence of explicit configuration or annotations.
   *
   * <p>For example, if no specific annotation (e.g., {@link Id}) or custom property name
   * is provided, this constant can be used to assume that the property named "id"
   * represents the primary key of the entity.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   // Using DEFAULT_ID_PROPERTY in a method to check for ID property
   *   public boolean isIdProperty(String propertyName) {
   *     return DEFAULT_ID_PROPERTY.equals(propertyName);
   *   }
   *
   *   // Example of resolving ID property dynamically
   *   String resolveIdProperty(BeanProperty property) {
   *     return Optional.ofNullable(property.getAnnotation(Id.class))
   *                    .map(annotation -> property.getName())
   *                    .orElse(DEFAULT_ID_PROPERTY);
   *   }
   * }</pre>
   *
   * <p>This constant is particularly useful in frameworks or utilities that need to
   * automatically discover or infer the primary key property of a class.
   */
  String DEFAULT_ID_PROPERTY = "id";

  /**
   * Checks if the given {@code BeanProperty} represents an ID property.
   *
   * <p>This method is typically used to determine whether a specific property
   * within a bean is considered an identifier (ID) property. The determination
   * logic depends on the implementation of this method in the containing class.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   * var property = BeanProperty.valueOf("id", readMethod, writeMethod, MyClass.class);
   * IdPropertyDiscover discoverer = IdPropertyDiscover.forPropertyName("id");
   *
   * if (discoverer.isIdProperty(property)) {
   *   System.out.println("The property is an ID property.");
   * }
   * else {
   *   System.out.println("The property is not an ID property.");
   * }
   * }</pre>
   *
   * @param property the {@code BeanProperty} to check
   * @return {@code true} if the property is an ID property, {@code false} otherwise
   */
  boolean isIdProperty(BeanProperty property);

  // static

  /**
   * Combines the current {@code IdPropertyDiscover} with another to create a composite
   * discovery logic. The resulting logic will consider a property as an ID property
   * if either the current instance or the provided {@code next} instance identifies
   * it as such.
   *
   * <p>This method is useful for building flexible and reusable ID property discovery
   * mechanisms by chaining multiple discovery strategies.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   * IdPropertyDiscover discover1 = IdPropertyDiscover.forPropertyName("id");
   * IdPropertyDiscover discover2 = IdPropertyDiscover.forIdAnnotation();
   *
   * IdPropertyDiscover combinedDiscover = discover1.and(discover2);
   *
   * BeanProperty property = BeanProperty.valueOf("id", readMethod, writeMethod, MyClass.class);
   * if (combinedDiscover.isIdProperty(property)) {
   *   System.out.println("The property is identified as an ID property by either discoverer.");
   * }
   * }</pre>
   *
   * @param next the next {@code IdPropertyDiscover} to combine with this instance
   * @return a new {@code IdPropertyDiscover} that applies the combined logic
   */
  default IdPropertyDiscover and(IdPropertyDiscover next) {
    return property -> isIdProperty(property) || next.isIdProperty(property);
  }

  /**
   * Creates a composite {@code IdPropertyDiscover} by combining multiple discovery
   * strategies. The resulting composite logic will identify a property as an ID property
   * if any of the provided {@code IdPropertyDiscover} instances recognize it as such.
   *
   * <p>This method is particularly useful when you need to aggregate multiple ID property
   * discovery mechanisms into a single, unified strategy.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   * IdPropertyDiscover discover1 = IdPropertyDiscover.forPropertyName("id");
   * IdPropertyDiscover discover2 = IdPropertyDiscover.forIdAnnotation();
   *
   * IdPropertyDiscover compositeDiscover = IdPropertyDiscover.composite(discover1, discover2);
   *
   * BeanProperty property = BeanProperty.valueOf("id", readMethod, writeMethod, MyClass.class);
   * if (compositeDiscover.isIdProperty(property)) {
   *   System.out.println("The property is identified as an ID property by at least one discoverer.");
   * }
   * }</pre>
   *
   * @param discovers a variable number of {@code IdPropertyDiscover} instances to combine
   * @return a new {@code IdPropertyDiscover} that applies the combined logic of all provided
   * discovery strategies
   * @throws IllegalArgumentException if the input array is null or empty
   */
  static IdPropertyDiscover composite(IdPropertyDiscover... discovers) {
    Assert.notNull(discovers, "IdPropertyDiscover is required");
    return composite(List.of(discovers));
  }

  /**
   * Creates a composite {@code IdPropertyDiscover} by combining multiple discovery strategies
   * provided as a list. The resulting composite logic identifies a property as an ID property
   * if any of the given {@code IdPropertyDiscover} instances recognize it as such.
   *
   * <p>This method is particularly useful when you need to aggregate multiple ID property
   * discovery mechanisms into a single, unified strategy. For example, you can combine
   * different discovery rules based on property names, annotations, or other criteria.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   * IdPropertyDiscover discover1 = IdPropertyDiscover.forPropertyName("id");
   * IdPropertyDiscover discover2 = IdPropertyDiscover.forIdAnnotation();
   *
   * List<IdPropertyDiscover> discovers = Arrays.asList(discover1, discover2);
   * IdPropertyDiscover compositeDiscover = IdPropertyDiscover.composite(discovers);
   *
   * BeanProperty property = BeanProperty.valueOf("id", readMethod, writeMethod, MyClass.class);
   * if (compositeDiscover.isIdProperty(property)) {
   *   System.out.println("The property is identified as an ID property by at least one discoverer.");
   * }
   * }</pre>
   *
   * @param discovers a list of {@code IdPropertyDiscover} instances to combine; must not be null
   * @return a new {@code IdPropertyDiscover} that applies the combined logic of all provided
   * discovery strategies
   * @throws IllegalArgumentException if the input list is null or empty
   */
  static IdPropertyDiscover composite(List<IdPropertyDiscover> discovers) {
    Assert.notNull(discovers, "IdPropertyDiscover is required");
    return beanProperty -> {

      for (IdPropertyDiscover discover : discovers) {
        if (discover.isIdProperty(beanProperty)) {
          return true;
        }
      }

      return false;
    };
  }

  /**
   * Creates an {@code IdPropertyDiscover} instance that identifies an ID property
   * based on the given property name. The returned instance will recognize a property
   * as an ID property if its name matches the specified name.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   * // Create an IdPropertyDiscover for the property named "id"
   * IdPropertyDiscover discoverer = IdPropertyDiscover.forPropertyName("id");
   *
   * // Assume we have a BeanProperty instance
   * BeanProperty property = BeanProperty.valueOf("id", readMethod, writeMethod, MyClass.class);
   *
   * // Check if the property is identified as an ID property
   * if (discoverer.isIdProperty(property)) {
   *   System.out.println("The property is an ID property.");
   * } else {
   *   System.out.println("The property is not an ID property.");
   * }
   * }</pre>
   *
   * @param name the name of the property to be considered as an ID property; must not be null
   * @return an {@code IdPropertyDiscover} instance that identifies properties with the
   * specified name as ID properties
   * @throws IllegalArgumentException if the provided name is null
   */
  static IdPropertyDiscover forPropertyName(String name) {
    Assert.notNull(name, "property-name is required");
    return property -> Objects.equals(name, property.getName());
  }

  /**
   * Creates an {@code IdPropertyDiscover} instance that identifies an ID property
   * based on the presence of the {@link Id} annotation. The returned instance will
   * recognize a property as an ID property if it is annotated with the {@code Id}
   * annotation, either directly or as a meta-annotation.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   // Create an IdPropertyDiscover for the @Id annotation
   *   IdPropertyDiscover discoverer = IdPropertyDiscover.forIdAnnotation();
   *
   *   // Assume we have a BeanProperty instance
   *   BeanProperty property = BeanProperty.valueOf("id", readMethod, writeMethod, MyClass.class);
   *
   *   // Check if the property is identified as an ID property
   *   if (discoverer.isIdProperty(property)) {
   *     System.out.println("The property is an ID property.");
   *   }
   *   else {
   *     System.out.println("The property is not an ID property.");
   *   }
   * }</pre>
   *
   * <p>This method is particularly useful when you want to dynamically discover ID
   * properties in a class based on the presence of the {@code Id} annotation.
   *
   * @return an {@code IdPropertyDiscover} instance that identifies properties
   * annotated with the {@code Id} annotation as ID properties
   */
  static IdPropertyDiscover forIdAnnotation() {
    return forAnnotation(Id.class);
  }

  /**
   * Creates an {@code IdPropertyDiscover} instance that identifies an ID property
   * based on the presence of the specified annotation type. The returned instance
   * will recognize a property as an ID property if it is annotated with the given
   * annotation, either directly or as a meta-annotation.
   *
   * <p><b>Usage Example:</b>
   * <pre>{@code
   *   // Create an IdPropertyDiscover for a custom annotation @Identifier
   *   IdPropertyDiscover discoverer = IdPropertyDiscover.forAnnotation(Identifier.class);
   *
   *   // Assume we have a BeanProperty instance
   *   BeanProperty property = BeanProperty.valueOf("id", readMethod, writeMethod, MyClass.class);
   *
   *   // Check if the property is identified as an ID property
   *   if (discoverer.isIdProperty(property)) {
   *     System.out.println("The property is an ID property.");
   *   }
   *   else {
   *     System.out.println("The property is not an ID property.");
   *   }
   * }</pre>
   *
   * <p>This method is particularly useful when you want to dynamically discover ID
   * properties in a class based on the presence of a custom annotation.
   *
   * @param annotationType the type of annotation to be considered for identifying
   * ID properties; must not be null
   * @return an {@code IdPropertyDiscover} instance that identifies properties
   * annotated with the specified annotation as ID properties
   * @throws IllegalArgumentException if the provided annotationType is null
   */
  static IdPropertyDiscover forAnnotation(Class<? extends Annotation> annotationType) {
    Assert.notNull(annotationType, "annotationType is required");
    return property -> property.mergedAnnotations().isPresent(annotationType);
  }

}
