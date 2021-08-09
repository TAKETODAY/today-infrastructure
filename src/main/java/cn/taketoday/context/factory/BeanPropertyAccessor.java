/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.factory;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.context.conversion.ConversionService;
import cn.taketoday.context.conversion.TypeConverter;
import cn.taketoday.context.conversion.support.DefaultConversionService;
import cn.taketoday.context.loader.NoSuchPropertyException;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.GenericDescriptor;

/**
 * @author TODAY 2021/1/27 22:35
 * @since 3.0
 */
public class BeanPropertyAccessor {

  protected Object rootObject;
  protected BeanMetadata metadata;

  /**
   * ignore unknown properties when {@code setProperty}
   */
  private boolean ignoreUnknownProperty = true;
  /**
   * throws a PropertyReadOnlyException when set a read-only property
   *
   * @see #setProperty(String, Object)
   * @since 3.0.2
   */
  private boolean throwsWhenReadOnly = true;

  private ConversionService conversionService;

  public BeanPropertyAccessor() {
    conversionService = DefaultConversionService.getSharedInstance();
  }

  public BeanPropertyAccessor(Class<?> beanClass) {
    this(beanClass, DefaultConversionService.getSharedInstance());
  }

  public BeanPropertyAccessor(Class<?> beanClass, ConversionService conversionService) {
    final BeanMetadata metadata = BeanMetadata.ofClass(beanClass);
    this.rootObject = metadata.newInstance();
    this.metadata = metadata;
    this.conversionService = conversionService;
  }

  public BeanPropertyAccessor(Object rootObject) {
    this(BeanMetadata.ofObject(rootObject), rootObject);
  }

  public BeanPropertyAccessor(BeanMetadata metadata, Object rootObject) {
    this();
    this.metadata = metadata;
    this.rootObject = rootObject;
  }

  // get

  /**
   * Get property object with given requiredType
   *
   * @param propertyPath
   *         Property path
   * @param requiredType
   *         Required type
   *
   * @return property object with given requiredType
   *
   * @throws ArrayIndexOutOfBoundsException
   *         Thrown to indicate that an array has been accessed with an
   *         illegal index. The index is either negative or greater than or
   *         equal to the size of the array.
   * @throws IllegalArgumentException
   *         Property path is Illegal
   * @throws NoSuchPropertyException
   *         If there is not a property
   * @throws IndexOutOfBoundsException
   *         if the index is out of list range
   *         (<tt>index &lt; 0 || index &gt;= size()</tt>)
   * @throws InvalidPropertyValueException
   *         conversion failed
   * @see #getProperty(String)
   */
  @SuppressWarnings("unchecked")
  public <T> T getProperty(final String propertyPath, final Class<T> requiredType) {
    return (T) convertIfNecessary(getProperty(propertyPath), requiredType);
  }

  /**
   * Get property object
   *
   * @param propertyPath
   *         Property path
   *
   * @return property object
   *
   * @throws ArrayIndexOutOfBoundsException
   *         Thrown to indicate that an array has been accessed with an
   *         illegal index. The index is either negative or greater than or
   *         equal to the size of the array.
   * @throws IllegalArgumentException
   *         Property path is Illegal
   * @throws NoSuchPropertyException
   *         If there is not a property
   * @throws IndexOutOfBoundsException
   *         if the index is out of list range (<tt>index &lt; 0 || index &gt;= size()</tt>)
   */
  public Object getProperty(final String propertyPath) {
    return getProperty(getRootObject(), obtainMetadata(), propertyPath);
  }

  /**
   * Get property object
   *
   * @param propertyPath
   *         Property path
   *
   * @return property object
   *
   * @throws ArrayIndexOutOfBoundsException
   *         Thrown to indicate that an array has been accessed with an
   *         illegal index. The index is either negative or greater than or
   *         equal to the size of the array.
   * @throws IllegalArgumentException
   *         Property path is Illegal
   * @throws NoSuchPropertyException
   *         If there is not a property
   * @throws IndexOutOfBoundsException
   *         if the index is out of list range (<tt>index &lt; 0 || index &gt;= size()</tt>)
   */
  public static Object getProperty(final Object root, final String propertyPath) {
    return getProperty(root, BeanMetadata.ofObject(root), propertyPath);
  }

  /**
   * Get property object
   *
   * @param propertyPath
   *         Property path
   *
   * @return property object
   *
   * @throws ArrayIndexOutOfBoundsException
   *         Thrown to indicate that an array has been accessed with an
   *         illegal index. The index is either negative or greater than or
   *         equal to the size of the array.
   * @throws IllegalArgumentException
   *         Property path is Illegal
   * @throws NoSuchPropertyException
   *         If there is not a property
   * @throws IndexOutOfBoundsException
   *         if the index is out of list range (<tt>index &lt; 0 || index &gt;= size()</tt>)
   */
  public static Object getProperty(
          final Object root, final BeanMetadata metadata, final String propertyPath) {
    final int signIndex = getNestedPropertySeparatorIndex(propertyPath);

    if (signIndex != -1) {
      final String property = propertyPath.substring(0, signIndex);
      // get property value and get value in the next call // root[1].name
      final Object propertyValue = getPropertyValue(root, metadata, property);
      if (propertyValue == null) {
        return null; // 上一级为空,下一级自然为空
      }

      final BeanMetadata subMetadata = getSubBeanMetadata(metadata, property, propertyValue);
      final String newPath = propertyPath.substring(signIndex + 1);
      return getProperty(propertyValue, subMetadata, newPath);
    }
    return getPropertyValue(root, metadata, propertyPath);
  }

  private static BeanMetadata getSubBeanMetadata(BeanMetadata root, String property, Object propertyValue) {
    if (property.indexOf('[') != -1) {
      return BeanMetadata.ofObject(propertyValue);
    }
    return BeanMetadata.ofClass(root.getBeanProperty(property).getType());
  }

  static Object getPropertyValue(Object root, BeanMetadata metadata, String propertyPath) {
    final int signIndex = propertyPath.indexOf('['); // array,list: [0]; map: [key]
    if (signIndex < 0) {
      return metadata.getProperty(root, propertyPath);
    }
    return getKeyedPropertyValue(root, metadata, signIndex, propertyPath);
  }

  static Object getKeyedPropertyValue(Object root, BeanMetadata metadata, int signIndex, String propertyPath) {
    // check
    final int endIndex = propertyPath.indexOf(']');
    if (endIndex == -1 || signIndex + 1 == endIndex) {
      // key is illegal
      throw new IllegalArgumentException("Unsupported Operator: " + propertyPath);
    }
    Object propValue = root;
    // array,list: [0]; map: [key]
    if (signIndex != 0) {
      final String property = propertyPath.substring(0, signIndex);
      propValue = metadata.getProperty(root, property);
      if (propValue == null) {
        return null;
      }
    }

    try {
      final String key = propertyPath.substring(signIndex + 1, endIndex);
      propValue = getKeyedPropertyValue(propValue, key);
      if (endIndex != propertyPath.length() - 1
              && propertyPath.charAt(endIndex + 1) == '[') {
        // Multidimensional Arrays
        return getKeyedPropertyValue(propValue, metadata, 0, propertyPath.substring(endIndex + 1));
      }
      return propValue;
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException(
              "Unsupported Operator: " + propertyPath + ", value: " + root, e);
    }
  }

  /**
   * Get propertyValue[key]
   *
   * @return key-value
   *
   * @throws ArrayIndexOutOfBoundsException
   *         Thrown to indicate that an array has been accessed with an
   *         illegal index. The index is either negative or greater than or
   *         equal to the size of the array.
   * @throws IllegalArgumentException
   *         Property path is Illegal
   * @throws NoSuchPropertyException
   *         If there is not a property
   * @throws IndexOutOfBoundsException
   *         if the index is out of list range (<tt>index &lt; 0 || index &gt;= size()</tt>)
   */
  @SuppressWarnings({ "rawtypes" })
  static Object getKeyedPropertyValue(Object propertyValue, String key) {
    if (propertyValue instanceof Map) {
      final Map map = (Map) propertyValue;
      return map.get(key);
    }
    else if (propertyValue instanceof List) {
      final List list = (List) propertyValue;
      return list.get(Integer.parseInt(key));
    }
    else if (propertyValue instanceof Set) {
      // Apply index to Iterator in case of a Set.
      final Set set = (Set) propertyValue;
      int index = Integer.parseInt(key);
      if (index < 0 || index >= set.size()) {
        throw new IndexOutOfBoundsException(
                "Cannot get element with index " + index + " from Set of size " + set.size());
      }
      final Iterator it = set.iterator();
      for (int j = 0; it.hasNext(); j++) {
        final Object elem = it.next();
        if (j == index) {
          return elem;
        }
      }
    }
    else if (propertyValue.getClass().isArray()) {
      final int arrayIndex = Integer.parseInt(key);
      final int length = Array.getLength(propertyValue);
      if (arrayIndex >= length) {
        throw new ArrayIndexOutOfBoundsException(length);
      }
      return Array.get(propertyValue, arrayIndex);
    }
    throw new IllegalArgumentException(
            "Unsupported data structure: " + propertyValue.getClass() + ", value: " + propertyValue);
  }

  // set

  /**
   * Set value to object's property
   *
   * @param propertyPath
   *         Property path to set
   * @param value
   *         Property value
   *
   * @throws NoSuchPropertyException
   *         If no such property
   * @throws InvalidPropertyValueException
   *         Invalid property value
   */
  public void setProperty(final String propertyPath, final Object value) {
    setProperty(getRootObject(), obtainMetadata(), propertyPath, value);
  }

  /**
   * Set value to object's property
   *
   * @param root
   *         Root object that apply to
   * @param propertyPath
   *         Property path to set
   * @param value
   *         Property value
   *
   * @throws NoSuchPropertyException
   *         If no such property
   * @throws InvalidPropertyValueException
   *         Invalid property value
   */
  public void setProperty(final Object root, final String propertyPath, final Object value) {
    setProperty(root, BeanMetadata.ofObject(root), propertyPath, value);
  }

  /**
   * Set value to object's property
   *
   * @param root
   *         Root object that apply to
   * @param metadata
   *         {@link BeanMetadata}
   * @param propertyPath
   *         Property path to set
   * @param value
   *         Property value
   *
   * @throws PropertyReadOnlyException
   *         property is read-only
   * @throws NoSuchPropertyException
   *         If no such property
   * @throws InvalidPropertyValueException
   *         Invalid property value
   * @see #ignoreUnknownProperty
   * @see #throwsWhenReadOnly
   */
  public void setProperty(
          final Object root, final BeanMetadata metadata, final String propertyPath, final Object value) {
    final int index = getNestedPropertySeparatorIndex(propertyPath);

    if (index != -1) {
      Object subValue;
      Class<?> propertyType;
      if (propertyPath.charAt(index - 1) == ']') { // xxx[0].list[0]
        final int signIndex = propertyPath.indexOf('['); // array,list: [0]; map: [key]
        final BeanProperty beanProperty = getBeanProperty(metadata, propertyPath, signIndex);
        if (beanProperty == null) { // @since 3.0.2
          return;
        }
        final Class<?> componentType = beanProperty.getComponentClass();

        propertyType = componentType != null ? componentType : root.getClass();
        try { // xxx[0].list[0]
          subValue = getProperty(root, metadata, propertyPath.substring(0, index));
        }
        catch (IndexOutOfBoundsException ignored) {
          // 值不够，设置新值
          subValue = getSubValue(root, beanProperty);
          if (componentType != null) {
            subValue = getComponentValue(root, propertyPath, subValue, signIndex, beanProperty);
          }
        }
        // 不存在,设置新值
        if (subValue == null) {
          // set new value
          subValue = setNewValue(root, beanProperty);
          if (componentType != null) {
            subValue = getComponentValue(root, propertyPath, subValue, signIndex, beanProperty);
          }
        }
      }
      else {
        final BeanProperty beanProperty = getBeanProperty(metadata, propertyPath, index);
        if (beanProperty == null) {
          return;
        }
        propertyType = beanProperty.getType();
        subValue = getSubValue(root, beanProperty);
      }
      // next
      BeanMetadata subMetadata = BeanMetadata.ofClass(propertyType);
      String newPath = propertyPath.substring(index + 1);
      setProperty(subValue, subMetadata, newPath, value);
    }
    else {
      // do set property operation
      final int signIndex = propertyPath.indexOf('['); // array,list: [0]; map: [key]
      if (signIndex < 0) {
        final BeanProperty beanProperty = getBeanProperty(metadata, propertyPath);
        if (beanProperty != null) {
          setValue(root, beanProperty, value);
        }
      }
      else {
        final BeanProperty beanProperty = getBeanProperty(metadata, propertyPath, signIndex);
        if (beanProperty != null) {
          final Object subValue = getSubValue(root, beanProperty);
          final String key = getKey(propertyPath, signIndex);
          setKeyedProperty(root, beanProperty, subValue, key, value, propertyPath);
        }
      }
    }
  }

  /**
   * @since 3.0.2
   */
  private void setValue(Object root, BeanProperty beanProperty, Object value) {
    if (beanProperty.isReadOnly()) {
      if (throwsWhenReadOnly) {
        throw new PropertyReadOnlyException(
                root + " has a property: '" + beanProperty.getName() + "' that is read-only");
      }
    }
    else {
      beanProperty.setDirectly(root, convertIfNecessary(value, beanProperty));
    }
  }

  private BeanProperty getBeanProperty(BeanMetadata metadata, String propertyPath, int index) {
    final String property = propertyPath.substring(0, index);
    return getBeanProperty(metadata, property);
  }

  /**
   * @since 3.0.2
   */
  private BeanProperty getBeanProperty(final BeanMetadata metadata, final String propertyPath) {
    final BeanProperty beanProperty = metadata.getBeanProperty(propertyPath);
    if (beanProperty == null && !ignoreUnknownProperty) {
      throw new NoSuchPropertyException(metadata.getType(), propertyPath);
    }
    return beanProperty;
  }

  protected Object getComponentValue(
          Object root, String propertyPath, Object subValue, int signIndex, BeanProperty beanProperty) {
    final Object componentValue = beanProperty.newComponentInstance();
    final String key = getKey(propertyPath, signIndex);
    setKeyedProperty(root, beanProperty, subValue, key, componentValue, propertyPath);
    return componentValue;
  }

  protected static String getKey(String propertyPath, int signIndex) {
    return propertyPath.substring(signIndex + 1, propertyPath.indexOf(']'));
  }

  private Object setNewValue(Object root, BeanProperty beanProperty) {
    final Object subValue = beanProperty.newInstance();
    setValue(root, beanProperty, subValue);
    return subValue;
  }

  private Object getSubValue(final Object object, final BeanProperty beanProperty) {
    // check if it has value
    Object subValue = beanProperty.getValue(object);
    if (subValue == null) {
      // set new value
      subValue = setNewValue(object, beanProperty);
    }
    return subValue;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected void setKeyedProperty(
          final Object root, final BeanProperty beanProperty, Object propValue,
          final String key, final Object value, final String propertyPath
  ) {
    if (propValue instanceof List) {
      Object convertedValue = value;
      final Type valueType = beanProperty.getGeneric(0);
      if (valueType instanceof Class) {
        convertedValue = convertIfNecessary(convertedValue, (Class<?>) valueType);
      }

      final List<Object> list = (List<Object>) propValue;
      final int index = Integer.parseInt(key);
      try {
        CollectionUtils.setValue(list, index, convertedValue);
      }
      catch (NullPointerException ex) {
        throw new InvalidPropertyValueException(
                "Cannot set element with index " + index + " in List of size " + list.size() +
                        ", accessed using property path '" + propertyPath +
                        "': List does not support filling up gaps with null elements");
      }
      catch (IndexOutOfBoundsException ex) {
        throw new InvalidPropertyValueException("Invalid list index in property path '" + propertyPath + "'", ex);
      }
    }
    else if (propValue instanceof Map) {
      Object convertedKey = key;
      Object convertedValue = value;
      final Type keyType = beanProperty.getGeneric(0);
      if (keyType instanceof Class) {
        convertedKey = convertIfNecessary(convertedKey, (Class<?>) keyType);
      }
      final Type valueType = beanProperty.getGeneric(1);
      if (valueType instanceof Class) {
        convertedValue = convertIfNecessary(convertedValue, (Class<?>) valueType);
      }
      ((Map) propValue).put(convertedKey, convertedValue);
    }
    else {
      // array
      final Class<?> propValueClass = propValue.getClass();
      if (propValueClass.isArray()) {
        Class<?> componentType = propValueClass.getComponentType();
        int arrayIndex = Integer.parseInt(key);
        int length = Array.getLength(propValue);

        // grow
        if (arrayIndex >= length && arrayIndex < Integer.MAX_VALUE) { // Integer.MAX_VALUE
          Object newArray = Array.newInstance(componentType, arrayIndex + 1);
          System.arraycopy(propValue, 0, newArray, 0, length);
          propValue = newArray;
          setValue(root, beanProperty, propValue);
        }

        Array.set(propValue, arrayIndex, convertIfNecessary(value, componentType));
      }
      else {
        throw new InvalidPropertyValueException(
                "Property referenced in indexed property path '" + propertyPath +
                        "' is neither an array nor a List nor a Map; returned value was [" + propValue + "]");
      }
    }
  }

  /**
   * @param beanProperty
   *         property metadata
   *
   * @throws InvalidPropertyValueException
   *         conversion failed
   */
  protected Object convertIfNecessary(final Object value, final BeanProperty beanProperty) {
    if (value == null || beanProperty.isInstance(value)) {
      return value;
    }
    return doConvertInternal(value, beanProperty);
  }

  protected Object doConvertInternal(Object value, final BeanProperty beanProperty) {
    return doConvertInternal(value, GenericDescriptor.ofProperty(beanProperty));
  }

  /**
   * @throws InvalidPropertyValueException
   *         conversion failed
   */
  protected Object convertIfNecessary(final Object value, final Class<?> requiredType) {
    if (value == null || requiredType.isInstance(value)) {
      return value;
    }
    return doConvertInternal(value, GenericDescriptor.valueOf(requiredType));
  }

  protected Object doConvertInternal(final Object value, final GenericDescriptor requiredType) {
    final TypeConverter typeConverter = getConversionService().getConverter(value, requiredType);
    if (typeConverter == null) {
      return converterNotFound(value, requiredType);
    }
    return typeConverter.convert(requiredType, value);
  }

  protected Object converterNotFound(final Object value, final GenericDescriptor requiredType) {
    throw new InvalidPropertyValueException(
            "Invalid property value [" + value + "] cannot convert '"
                    + value.getClass() + "' to target class: [" + requiredType + "]");
  }

  /**
   * Determine the first nested property separator in the
   * given property path, ignoring dots in keys (like "map[my.key]").
   *
   * @param propertyPath
   *         the property path to check
   *
   * @return the index of the nested property separator, or -1 if none
   */
  public static int getNestedPropertySeparatorIndex(final String propertyPath) {
    int idx = 0;
    boolean inKey = false;
    for (final char value : propertyPath.toCharArray()) {
      if (value == '[' || value == ']') {
        inKey = !inKey;
      }
      else if (value == '.' && !inKey) {
        return idx;
      }
      idx++;
    }
    return -1;
  }

  //

  public void setRootObject(Object rootObject) {
    this.rootObject = rootObject;
  }

  public void setMetadata(BeanMetadata metadata) {
    this.metadata = metadata;
  }

  public Object getRootObject() {
    if (rootObject == null) {
      setRootObject(obtainMetadata().newInstance());
    }
    return rootObject;
  }

  public BeanMetadata getMetadata() {
    return metadata;
  }

  public BeanMetadata obtainMetadata() {
    final BeanMetadata metadata = getMetadata();
    Assert.state(metadata != null, "No BeanMetadata.");
    return metadata;
  }

  public void setConversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  public ConversionService getConversionService() {
    ConversionService conversionService = this.conversionService;
    if (conversionService == null) {
      this.conversionService = conversionService = DefaultConversionService.getSharedInstance();
    }
    return conversionService;
  }

  /**
   * ignore unknown properties when {@code setProperty} ?
   */
  public void setIgnoreUnknownProperty(boolean ignoreUnknownProperty) {
    this.ignoreUnknownProperty = ignoreUnknownProperty;
  }

  public boolean isIgnoreUnknownProperty() {
    return ignoreUnknownProperty;
  }

  public void setThrowsWhenReadOnly(boolean throwsWhenReadOnly) {
    this.throwsWhenReadOnly = throwsWhenReadOnly;
  }

  public boolean isThrowsWhenReadOnly() {
    return throwsWhenReadOnly;
  }

  // static

  public static BeanPropertyAccessor ofObject(Object object) {
    return new BeanPropertyAccessor(object);
  }

  public static BeanPropertyAccessor ofClass(Class<?> beanClass) {
    return new BeanPropertyAccessor(beanClass);
  }

  public static BeanPropertyAccessor of(BeanMetadata metadata, Object object) {
    return new BeanPropertyAccessor(metadata, object);
  }
}
