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

package infra.jdbc;

import org.jspecify.annotations.Nullable;

import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;
import infra.beans.PropertyAccessorUtils;

/**
 * Represents a path to a nested property within a Java object, allowing traversal
 * and manipulation of nested properties in a structured manner.
 *
 * <p>This class is immutable and designed to handle property paths that may span
 * multiple levels of nested objects. It provides methods for retrieving, setting,
 * and navigating through the properties defined by the path.
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
 * // Create a PropertyPath instance
 * PropertyPath path = new PropertyPath(Person.class, "address.city");
 *
 * // Retrieve the nested property value
 * Person person = new Person();
 * Address address = new Address();
 * person.setAddress(address);
 * address.setCity("New York");
 * Object city = path.getNestedObject(person); // Returns "New York"
 *
 * // Set a new value for the nested property
 * path.set(person, "San Francisco");
 * System.out.println(address.getCity()); // Outputs "San Francisco"
 * }</pre>
 *
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li>Supports nested property traversal using dot notation (e.g., "address.city").</li>
 *   <li>Handles null intermediate objects by instantiating them when necessary.</li>
 *   <li>Provides methods to retrieve, set, and navigate through nested properties.</li>
 * </ul>
 *
 * <p><strong>Notes:</strong>
 * <ul>
 *   <li>If a property in the path does not exist, it is represented by the placeholder
 *       {@link #emptyPlaceholder} in the string representation.</li>
 *   <li>The class uses {@link BeanMetadata} and {@link BeanProperty} internally to
 *       resolve and manipulate properties.</li>
 * </ul>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanMetadata
 * @see BeanProperty
 * @since 4.0 2022/7/30 20:31
 */
final class PropertyPath {

  static final String emptyPlaceholder = "<not-found>";

  @Nullable
  public final PropertyPath next;

  // @Nullable check first
  @Nullable
  public final BeanProperty beanProperty;

  public PropertyPath(Class<?> objectType, String propertyPath) {
    BeanMetadata metadata = BeanMetadata.forClass(objectType);
    int pos = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(propertyPath);
    String name = propertyPath.substring(0, pos);
    this.beanProperty = metadata.obtainBeanProperty(name);

    BeanMetadata nextMetadata = BeanMetadata.forClass(beanProperty.getType());
    this.next = new PropertyPath(propertyPath.substring(pos + 1), nextMetadata);
  }

  public PropertyPath(String propertyPath, BeanMetadata metadata) {
    int pos = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(propertyPath);
    if (pos > -1) {
      // compute next PropertyPath
      String propertyName = propertyPath.substring(0, pos);
      this.beanProperty = metadata.getBeanProperty(propertyName);
      if (beanProperty != null) {
        BeanMetadata nextMetadata = BeanMetadata.forClass(beanProperty.getType());
        this.next = new PropertyPath(propertyPath.substring(pos + 1), nextMetadata);
      }
      else {
        this.next = null;
      }
    }
    else {
      // terminated (last PropertyPath)
      this.next = null;
      this.beanProperty = metadata.getBeanProperty(propertyPath); // maybe null
    }
  }

  @Nullable
  public BeanProperty getNestedBeanProperty() {
    if (next != null) {
      return next.getNestedBeanProperty();
    }
    return beanProperty;
  }

  /**
   * Retrieves the nested object from the given parent object by traversing
   * the property path defined in the current {@code PropertyPath} instance.
   * If the {@code next} property is not null, the method recursively retrieves
   * the next nested object until the end of the path is reached.
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
   * PropertyPath propertyPath = new PropertyPath(Person.class, "address.city");
   *
   * // Retrieve the nested 'city' object
   * Object cityObject = propertyPath.getNestedObject(person);
   *
   * // If 'address' was null, it would be instantiated automatically
   * System.out.println(cityObject); // Output: null (default value for 'city')
   * }</pre>
   *
   * @param parent the root object from which to start retrieving the nested object;
   * must not be {@code null}
   * @return the nested object at the end of the property path, or the parent
   * object if the path is empty or fully traversed
   */
  public Object getNestedObject(Object parent) {
    if (next != null) {
      Object nextParent = getProperty(parent);
      return next.getNestedObject(nextParent);
    }
    return parent;
  }

  /**
   * Sets the value of a property in a nested object structure defined by the current {@code PropertyPath}.
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
   * PropertyPath propertyPath = new PropertyPath(Person.class, "address.city");
   *
   * // Set the value of 'city' in the nested structure
   * propertyPath.set(person, "New York");
   *
   * // The 'address' object is automatically instantiated if it was null
   * System.out.println(person.getAddress().getCity()); // Output: New York
   * }</pre>
   *
   * @param obj the root object from which the property path starts; must not be {@code null}
   * @param result the value to set on the final property in the path; can be {@code null}
   * @see #getProperty(Object)
   * @see BeanProperty#setValue(Object, Object)
   */
  public void set(Object obj, @Nullable Object result) {
    PropertyPath current = this;
    while (current.next != null) {
      obj = getProperty(obj);
      current = current.next;
    }

    // set current object's property
    current.beanProperty.setValue(obj, result);
  }

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
