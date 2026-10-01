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

package infra.jdbc;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;
import infra.beans.PropertyPath;

/**
 * A pre-resolved chain of bean properties used for nested JDBC result mapping.
 *
 * <p>The accessor resolves property metadata at construction time and can be reused
 * for multiple root objects. Traversal creates and assigns null intermediate
 * objects through {@link BeanProperty#instantiate()} when necessary.
 * The accessor structure is immutable, but traversal and assignment mutate the
 * supplied object graph.
 *
 * <p>Paths are parsed by {@link PropertyPath}, but this accessor only navigates
 * bean properties separated by dots. It does not implement indexed access to
 * arrays, collections, or maps.
 *
 * <p><strong>Example Usage:</strong>
 *
 * <pre>{@code
 * // Define a class hierarchy
 * public class Address {
 *   private String city;
 *   // getters and setters
 * }
 *
 * public class Person {
 *   private String name;
 *   private Address address;
 *   // getters and setters
 * }
 *
 * // Resolve the nested property chain
 * var accessor = new NestedPropertyAccessor(Person.class, "address.city");
 *
 * // Retrieve the object that owns the leaf property
 * Person person = new Person();
 * Address address = new Address();
 * person.setAddress(address);
 * address.setCity("New York");
 * Address owner = (Address) accessor.getOrCreateLeafOwner(person); // Returns address
 * Object city = accessor.getLeafProperty().getValue(owner); // Returns "New York"
 *
 * // Set a new value for the nested property
 * accessor.set(person, "San Francisco");
 * System.out.println(address.getCity()); // Outputs "San Francisco"
 * }</pre>
 *
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li>Supports nested property traversal using dot notation (e.g., "address.city").</li>
 *   <li>Handles null intermediate objects by instantiating them when necessary.</li>
 *   <li>Provides access to the leaf property and its owner, and assigns leaf values.</li>
 * </ul>
 *
 * <p><strong>Notes:</strong>
 * <ul>
 *   <li>If a property in the path does not exist, it is represented by the placeholder
 *       {@link #emptyPlaceholder} in the string representation. Resolution stops
 *       at that property; such a chain must not be used for traversal or assignment.</li>
 *   <li>The class uses {@link BeanMetadata} and {@link BeanProperty} internally to
 *       resolve and manipulate properties.</li>
 * </ul>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanMetadata
 * @see BeanProperty
 * @since 4.0 2022/7/30 20:31
 */
final class NestedPropertyAccessor {

  static final String emptyPlaceholder = "<not-found>";

  public final @Nullable BeanProperty beanProperty;

  public final @Nullable NestedPropertyAccessor next;

  /**
   * Resolve an accessor for the given root type, requiring the first property to exist.
   * <p>Missing subsequent properties are represented by an unresolved terminal node.
   *
   * @param objectType the root bean type
   * @param propertyPath the dot-separated property path
   * @throws infra.beans.InvalidPropertyPathException if the path is malformed
   * @throws infra.beans.NoSuchPropertyException if the first property does not exist
   */
  public NestedPropertyAccessor(Class<?> objectType, String propertyPath) {
    this(PropertyPath.parse(propertyPath).segments(), 0, BeanMetadata.forClass(objectType), true);
  }

  /**
   * Resolve an accessor using the given root metadata.
   * <p>A missing property is represented by an unresolved terminal node rather
   * than requiring every property to exist.
   *
   * @param propertyPath the dot-separated property path
   * @param metadata the root bean metadata
   * @throws infra.beans.InvalidPropertyPathException if the path is malformed
   */
  public NestedPropertyAccessor(String propertyPath, BeanMetadata metadata) {
    this(PropertyPath.parse(propertyPath).segments(), 0, metadata, false);
  }

  private NestedPropertyAccessor(List<PropertyPath.Segment> segments, int index, BeanMetadata metadata, boolean required) {
    String name = segments.isEmpty() ? "" : segments.get(index).toCanonicalName();
    this.beanProperty = required ? metadata.getRequiredProperty(name) : metadata.getProperty(name);
    this.next = beanProperty != null && index + 1 < segments.size()
            ? new NestedPropertyAccessor(segments, index + 1, BeanMetadata.forClass(beanProperty.getType()), false) : null;
  }

  /**
   * Return the property at the end of the resolved chain.
   *
   * @return the leaf property, or {@code null} if resolution encountered a missing property
   */
  public @Nullable BeanProperty getLeafProperty() {
    if (next != null) {
      return next.getLeafProperty();
    }
    return beanProperty;
  }

  /**
   * Return the object that owns the leaf property, creating null intermediate
   * objects as necessary. This method does not read the leaf property's value.
   *
   * <p>This method relies on the {@link #getProperty(Object)} method to fetch
   * or instantiate intermediate objects if they are null during traversal.
   *
   * <p>Example usage:
   * <pre>{@code
   * // Assume a class hierarchy:
   * // class Address { String city; }
   * // class Person { Address address; }
   *
   * Person person = new Person();
   * var accessor = new NestedPropertyAccessor(Person.class, "address.city");
   *
   * // Obtain the Address object that owns the city property
   * Address owner = (Address) accessor.getOrCreateLeafOwner(person);
   *
   * // If 'address' was null, it would be instantiated automatically
   * System.out.println(owner == person.address); // Output: true
   * }</pre>
   *
   * <p>The chain must be fully resolved. Intermediate properties must be readable
   * and, when null, writable with an instantiable property type.
   *
   * @param parent the root object from which to start traversal;
   * must not be {@code null}
   * @return the owner of the leaf property, or {@code parent} for a single-property chain
   */
  public Object getOrCreateLeafOwner(Object parent) {
    if (next != null) {
      Object nextParent = getProperty(parent);
      return next.getOrCreateLeafOwner(nextParent);
    }
    return parent;
  }

  /**
   * Set the leaf property's value in the object graph rooted at {@code obj}.
   * This method traverses the chain of nested properties starting from the given object and sets the
   * specified result value on the final property in the path.
   *
   * <p>If any intermediate property in the path is {@code null}, it will be instantiated using the
   * {@link #getProperty(Object)} method before proceeding to the next level.
   *
   * <p>Example usage:
   * <pre>{@code
   * // Assume a class hierarchy:
   * // class Address { String city; }
   * // class Person { Address address; }
   *
   * Person person = new Person();
   * var accessor = new NestedPropertyAccessor(Person.class, "address.city");
   *
   * // Set the value of 'city' in the nested structure
   * accessor.set(person, "New York");
   *
   * // The 'address' object is automatically instantiated if it was null
   * System.out.println(person.address.city); // Output: New York
   * }</pre>
   *
   * <p>The chain must be fully resolved and the leaf property must be writable.
   * Intermediate properties must be readable and, when null, writable with an
   * instantiable property type.
   *
   * @param obj the root object from which the property path starts; must not be {@code null}
   * @param value the value to set on the final property in the path; can be {@code null}
   * @see #getProperty(Object)
   * @see BeanProperty#setValue(Object, Object)
   */
  @SuppressWarnings("NullAway")
  public void set(Object obj, @Nullable Object value) {
    NestedPropertyAccessor current = this;
    while (current.next != null) {
      obj = current.getProperty(obj);
      current = current.next;
    }

    // set current object's property
    current.beanProperty.setValue(obj, value);
  }

  @SuppressWarnings("NullAway")
  private Object getProperty(Object obj) {
    Object property = beanProperty.getValue(obj);
    if (property == null) {
      // nested object maybe null
      property = beanProperty.instantiate();
      beanProperty.setValue(obj, property);
    }
    return property;
  }

  @Override
  public String toString() {
    if (next != null) {
      StringBuilder sb = new StringBuilder();
      if (beanProperty == null) {
        sb.append(emptyPlaceholder);
      }
      else {
        sb.append(beanProperty.getName());
      }
      return sb.append('.').append(next).toString();
    }
    if (beanProperty == null) {
      return emptyPlaceholder;
    }
    return beanProperty.getName();
  }
}
