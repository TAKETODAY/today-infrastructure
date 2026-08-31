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

package infra.beans;

import org.jspecify.annotations.Nullable;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

import infra.beans.support.BeanInstantiator;
import infra.lang.Unmodifiable;
import infra.util.ClassUtils;
import infra.util.ConcurrentReferenceHashMap;
import infra.util.InfraStrategies;
import infra.util.MapCache;
import infra.util.ReflectionUtils;
import infra.util.StringUtils;

/**
 * Provides metadata information for a Java bean class.
 *
 * <p>This class encapsulates details about a bean's structure, including its properties,
 * instantiation strategies, and property accessors (getters/setters). It serves as a central
 * registry for introspecting bean classes efficiently, often utilizing caching mechanisms
 * to avoid repeated reflection costs.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 3.0 2021/1/27 22:26
 */
public final class BeanMetadata implements Iterable<BeanProperty> {

  /**
   * Strategy property for a comma-delimited list of field name prefixes.
   *
   * @since 5.0
   */
  public static final String FIELD_PREFIXES_PROPERTY_NAME = "infra.beans.fields.prefixes";

  private static final MapCache<Class<?>, BeanMetadata, ?> metadataMappings = new MapCache<>(
          new ConcurrentReferenceHashMap<>(), BeanMetadata::new);

  private static final boolean shouldIgnoreFields = InfraStrategies.getFlag("infra.beans.fields.ignore", false);

  private static String[] fieldPrefixes() {
    return StringUtils.tokenizeToStringArray(
            InfraStrategies.getProperty(FIELD_PREFIXES_PROPERTY_NAME), ",");
  }

  private final Class<?> beanClass;

  private @Nullable BeanInstantiator instantiator;

  /**
   * @since 4.0
   */
  private @Nullable BeanPropertiesHolder propertyHolder;

  /**
   * Constructs a new {@code BeanMetadata} instance for the specified bean class.
   *
   * @param beanClass the class of the bean to introspect
   */
  public BeanMetadata(Class<?> beanClass) {
    this.beanClass = beanClass;
  }

  /**
   * Returns the type of the bean class associated with this metadata.
   *
   * @return the bean class
   */
  public Class<?> getType() {
    return this.beanClass;
  }

  /**
   * Returns the {@link BeanInstantiator} for creating instances of this bean class.
   * The instantiator is created lazily and cached for subsequent calls.
   *
   * @return the bean instantiator
   */
  public BeanInstantiator getInstantiator() {
    BeanInstantiator instantiator = this.instantiator;
    if (instantiator == null) {
      instantiator = BeanInstantiator.forClass(beanClass);
      this.instantiator = instantiator;
    }
    return instantiator;
  }

  /**
   * Create this bean a new instance with no arguments
   *
   * @return a new instance object
   */
  public Object newInstance() {
    return newInstance(null);
  }

  /**
   * Create this bean a new instance with given arguments
   *
   * @return a new instance object
   */
  public Object newInstance(@Nullable Object @Nullable [] args) {
    return getInstantiator().instantiate(args);
  }

  /**
   * Retrieves the {@link BeanProperty} associated with the specified property name.
   *
   * @param propertyName the name of the property to retrieve
   * @return the {@link BeanProperty} instance, or {@code null} if no such property exists
   */
  public @Nullable BeanProperty getProperty(String propertyName) {
    return getBeanPropertyMap().get(propertyName);
  }

  /**
   * Obtains the {@link BeanProperty} associated with the specified property name.
   *
   * <p>If the property does not exist, a {@link NoSuchPropertyException} is thrown.
   *
   * @param propertyName the name of the property to obtain
   * @return the {@link BeanProperty} instance
   * @throws NoSuchPropertyException if no property with the given name exists
   */
  public BeanProperty obtainBeanProperty(String propertyName) {
    BeanProperty beanProperty = getProperty(propertyName);
    if (beanProperty == null) {
      throw new NoSuchPropertyException(beanClass, propertyName);
    }
    return beanProperty;
  }

  /**
   * Sets the value of the specified property on the given root object.
   *
   * @param root the target object on which to set the property value
   * @param propertyName the name of the property to set
   * @param value the new value to assign to the property
   * @throws NotWritablePropertyException if the property is read-only and cannot be written to
   * @throws NoSuchPropertyException if no property with the given name exists
   * @see #obtainBeanProperty(String)
   */
  public void setPropertyValue(Object root, String propertyName, Object value) {
    obtainBeanProperty(propertyName).setValue(root, value);
  }

  /**
   * Retrieves the value of the specified property from the given root object.
   *
   * @param root the target object from which to retrieve the property value
   * @param propertyName the name of the property to retrieve
   * @return the value of the property, or {@code null} if the property value is null
   * @throws NoSuchPropertyException if no property with the given name exists
   * @see #obtainBeanProperty(String)
   */
  public @Nullable Object getPropertyValue(Object root, String propertyName) {
    return obtainBeanProperty(propertyName).getValue(root);
  }

  /**
   * Retrieves the type of the specified property.
   *
   * @param propertyName the name of the property whose type is to be retrieved
   * @return the class representing the type of the property
   * @throws NoSuchPropertyException if no property with the given name exists
   * @see #obtainBeanProperty(String)
   */
  public Class<?> getPropertyType(String propertyName) {
    return obtainBeanProperty(propertyName).getType();
  }

  /**
   * Returns an unmodifiable map of all bean properties, keyed by property name.
   *
   * @return an unmodifiable map containing all {@link BeanProperty} instances
   */
  @Unmodifiable
  public Map<String, BeanProperty> getBeanPropertyMap() {
    return propertyHolder().mapping;
  }

  /**
   * Returns an unmodifiable list of all bean properties.
   *
   * @return an unmodifiable list of {@link BeanProperty} instances
   */
  @Unmodifiable
  public List<BeanProperty> getBeanProperties() {
    return propertyHolder().beanProperties;
  }

  /**
   * Returns the number of properties defined in this bean.
   *
   * @return the count of bean properties
   * @since 4.0
   */
  public int getPropertyCount() {
    return propertyHolder().beanProperties.size();
  }

  /**
   * Checks whether a property with the specified name exists in this bean.
   *
   * @param name the name of the property to check
   * @return {@code true} if the property exists, {@code false} otherwise
   * @since 4.0
   */
  public boolean containsProperty(String name) {
    return propertyHolder().mapping.containsKey(name);
  }

  /**
   * Returns an unmodifiable Set of all property names of the bean, including
   * inherited properties. Static properties are excluded.
   *
   * @return an unmodifiable Set of property names
   * @since 5.0
   */
  @Unmodifiable
  public Set<String> propertyNames() {
    return propertyHolder().mapping.keySet();
  }

  /**
   * @since 4.0
   */
  private BeanPropertiesHolder propertyHolder() {
    BeanPropertiesHolder propertyHolder = this.propertyHolder;
    if (propertyHolder == null) {
      propertyHolder = BeanPropertiesMapCache.computeProperties(this);
      this.propertyHolder = propertyHolder;
    }
    return propertyHolder;
  }

  public HashMap<String, BeanProperty> createBeanProperties() {
    HashMap<String, BeanProperty> beanPropertyMap = new LinkedHashMap<>();
    CachedIntrospectionResults results = new CachedIntrospectionResults(beanClass);

    PropertyDescriptor[] propertyDescriptors = results.getPropertyDescriptors();
    for (PropertyDescriptor descriptor : propertyDescriptors) {
      if (descriptor.getReadMethod() != null || descriptor.getWriteMethod() != null) {
        BeanProperty property = new BeanProperty(descriptor, beanClass);
        beanPropertyMap.put(descriptor.getName(), property);
      }
    }

    if (!shouldIgnoreFields) {
      String[] prefixes = fieldPrefixes();
      ReflectionUtils.doWithFields(beanClass, field -> {
        if (!Modifier.isStatic(field.getModifiers())) {
          String propertyName = getPropertyName(field, beanPropertyMap, prefixes);
          BeanProperty property = beanPropertyMap.get(propertyName);
          if (property == null) {
            property = new BeanProperty(field, null, null);
            beanPropertyMap.put(propertyName, property);
          }
          else {
            property.setField(field);
          }
        }
      });
    }
    return beanPropertyMap;
  }

  private String getPropertyName(Field field, Map<String, BeanProperty> beanProperties, String[] prefixes) {
    String fieldName = field.getName();
    if (beanProperties.containsKey(fieldName)) {
      return fieldName;
    }

    // Only derive the property name from accessors for boolean "is" prefixed fields,
    // so that a field like "isEnabled" does not surface a second, conflicting
    // property next to the accessor-derived "enabled". Other fields keep their
    // raw name (e.g. a field named "Title" stays distinct from the "title" getter).
    if (fieldName.startsWith("is")) {
      Method readMethod = ReflectionUtils.getReadMethod(field);
      Method writeMethod = ReflectionUtils.getWriteMethod(field);
      String propertyName = ReflectionUtils.getPropertyName(readMethod, writeMethod);
      if (propertyName != null && beanProperties.containsKey(propertyName)) {
        return propertyName;
      }
    }

    for (String prefix : prefixes) {
      if (fieldName.startsWith(prefix) && fieldName.length() > prefix.length()) {
        String candidate = StringUtils.uncapitalize(fieldName.substring(prefix.length()));
        if (beanProperties.containsKey(candidate)) {
          return candidate;
        }
      }
    }
    return fieldName;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o)
      return true;
    if (!(o instanceof BeanMetadata that))
      return false;
    return beanClass.equals(that.beanClass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(beanClass);
  }

  //---------------------------------------------------------------------
  // Implementation of Iterable interface
  //---------------------------------------------------------------------

  @Override
  public Iterator<BeanProperty> iterator() {
    return propertyHolder().beanProperties.iterator();
  }

  @Override
  public void forEach(Consumer<? super BeanProperty> action) {
    propertyHolder().beanProperties.forEach(action);
  }

  @Override
  public Spliterator<BeanProperty> spliterator() {
    return propertyHolder().beanProperties.spliterator();
  }

  //---------------------------------------------------------------------
  // Static factory methods
  //---------------------------------------------------------------------

  /**
   * Creates a {@link BeanMetadata} instance for the specified bean class.
   *
   * <p>The metadata is cached to avoid repeated introspection costs. Subsequent calls with the same
   * class will return the cached instance.
   *
   * @param beanClass the target bean class; should not be a simple type (e.g., primitive wrappers, String)
   * @return the {@link BeanMetadata} for the given class
   * @see ClassUtils#isSimpleType(Class)
   */
  public static BeanMetadata forClass(Class<?> beanClass) {
    return metadataMappings.get(beanClass);
  }

  /**
   * Creates a {@link BeanMetadata} instance for the specified bean object.
   *
   * <p>This is a convenience method that delegates to {@link #forClass(Class)} using the runtime
   * class of the provided object. The metadata is cached based on the class type.
   *
   * @param object the target bean object; should not be a simple type instance
   * @return the {@link BeanMetadata} for the object's class
   * @see #forClass(Class)
   * @see ClassUtils#isSimpleType(Class)
   */
  public static BeanMetadata forInstance(Object object) {
    return forClass(object.getClass());
  }

  /**
   * @since 4.0
   */
  static final class BeanPropertiesHolder {
    public final Map<String, BeanProperty> mapping;
    public final List<BeanProperty> beanProperties;

    BeanPropertiesHolder(HashMap<String, BeanProperty> mapping) {
      this.mapping = Map.copyOf(mapping);
      this.beanProperties = List.copyOf(mapping.values());
    }
  }

  /**
   * Mapping cache
   */
  static class BeanPropertiesMapCache extends MapCache<BeanMetadata, BeanPropertiesHolder, BeanMetadata> {
    private static final BeanPropertiesMapCache beanPropertiesMappings = new BeanPropertiesMapCache();

    BeanPropertiesMapCache() {
      super(new ConcurrentReferenceHashMap<>());
    }

    static BeanPropertiesHolder computeProperties(BeanMetadata metadata) {
      return beanPropertiesMappings.get(metadata);
    }

    @Override
    protected BeanPropertiesHolder createValue(BeanMetadata key, BeanMetadata param) {
      HashMap<String, BeanProperty> propertyMap = key.createBeanProperties();
      return new BeanPropertiesHolder(propertyMap);
    }

  }

}
