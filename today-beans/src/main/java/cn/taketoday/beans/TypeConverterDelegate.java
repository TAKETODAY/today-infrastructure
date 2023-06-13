/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.beans;

import java.beans.PropertyEditor;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.NumberUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Internal helper class for converting property values to target types.
 *
 * <p>Works on a given {@link PropertyEditorRegistrySupport} instance.
 * Used as a delegate by {@link BeanWrapperImpl} and {@link SimpleTypeConverter}.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanWrapperImpl
 * @see SimpleTypeConverter
 * @since 4.0 2022/2/17 17:46
 */
public class TypeConverterDelegate {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final PropertyEditorRegistrySupport propertyEditorRegistry;

  @Nullable
  private final Object targetObject;

  /**
   * Create a new TypeConverterDelegate for the given editor registry.
   *
   * @param propertyEditorRegistry the editor registry to use
   */
  public TypeConverterDelegate(PropertyEditorRegistrySupport propertyEditorRegistry) {
    this(propertyEditorRegistry, null);
  }

  /**
   * Create a new TypeConverterDelegate for the given editor registry and bean instance.
   *
   * @param propertyEditorRegistry the editor registry to use
   * @param targetObject the target object to work on (as context that can be passed to editors)
   */
  public TypeConverterDelegate(PropertyEditorRegistrySupport propertyEditorRegistry, @Nullable Object targetObject) {
    this.propertyEditorRegistry = propertyEditorRegistry;
    this.targetObject = targetObject;
  }

  /**
   * Convert the value to the required type for the specified property.
   *
   * @param propertyName name of the property
   * @param oldValue the previous value, if available (may be {@code null})
   * @param newValue the proposed new value
   * @param requiredType the type we must convert to
   * (or {@code null} if not known, for example in case of a collection element)
   * @return the new value, possibly the result of type conversion
   * @throws IllegalArgumentException if type conversion failed
   */
  @Nullable
  public <T> T convertIfNecessary(@Nullable String propertyName, @Nullable Object oldValue,
          Object newValue, @Nullable Class<T> requiredType) throws IllegalArgumentException {

    return convertIfNecessary(propertyName, oldValue, newValue, requiredType, TypeDescriptor.valueOf(requiredType));
  }

  /**
   * Convert the value to the required type (if necessary from a String),
   * for the specified property.
   *
   * @param propertyName name of the property
   * @param oldValue the previous value, if available (may be {@code null})
   * @param newValue the proposed new value
   * @param requiredType the type we must convert to
   * (or {@code null} if not known, for example in case of a collection element)
   * @param typeDescriptor the descriptor for the target property or field
   * @return the new value, possibly the result of type conversion
   * @throws IllegalArgumentException if type conversion failed
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public <T> T convertIfNecessary(@Nullable String propertyName, @Nullable Object oldValue, @Nullable Object newValue,
          @Nullable Class<T> requiredType, @Nullable TypeDescriptor typeDescriptor) throws IllegalArgumentException {
    ConversionFailedException conversionAttemptEx = null;
    // Custom editor for this type?
    PropertyEditor editor = propertyEditorRegistry.findCustomEditor(requiredType, propertyName);
    // No custom editor but custom ConversionService specified?
    ConversionService conversionService = propertyEditorRegistry.getConversionService();
    if (editor == null && conversionService != null && newValue != null && typeDescriptor != null) {
      TypeDescriptor sourceTypeDesc = TypeDescriptor.fromObject(newValue);
      if (conversionService.canConvert(sourceTypeDesc, typeDescriptor)) {
        try {
          return (T) conversionService.convert(newValue, sourceTypeDesc, typeDescriptor);
        }
        catch (ConversionFailedException ex) {
          // fallback to default conversion logic below
          conversionAttemptEx = ex;
        }
      }
    }

    Object convertedValue = newValue;

    // Value not of required type?
    if (editor != null || (requiredType != null && !ClassUtils.isAssignableValue(requiredType, convertedValue))) {
      if (typeDescriptor != null
              && requiredType != null
              && convertedValue instanceof String
              && Collection.class.isAssignableFrom(requiredType)) {
        TypeDescriptor elementTypeDesc = typeDescriptor.getElementDescriptor();
        if (elementTypeDesc != null) {
          Class<?> elementType = elementTypeDesc.getType();
          if (Class.class == elementType || Enum.class.isAssignableFrom(elementType)) {
            convertedValue = StringUtils.commaDelimitedListToStringArray((String) convertedValue);
          }
        }
      }
      if (editor == null) {
        editor = findDefaultEditor(requiredType);
      }
      convertedValue = doConvertValue(oldValue, convertedValue, requiredType, editor);
    }

    boolean standardConversion = false;

    if (requiredType != null) {
      // Try to apply some standard type conversion rules if appropriate.

      if (convertedValue != null) {
        if (Object.class == requiredType) {
          return (T) convertedValue;
        }
        else if (requiredType.isArray()) {
          // Array required -> apply appropriate conversion of elements.
          if (convertedValue instanceof String && Enum.class.isAssignableFrom(requiredType.getComponentType())) {
            convertedValue = StringUtils.commaDelimitedListToStringArray((String) convertedValue);
          }
          return (T) convertToTypedArray(convertedValue, propertyName, requiredType.getComponentType());
        }
        else if (convertedValue instanceof Collection) {
          // Convert elements to target type, if determined.
          convertedValue = convertToTypedCollection(
                  (Collection<?>) convertedValue, propertyName, requiredType, typeDescriptor);
          standardConversion = true;
        }
        else if (convertedValue instanceof Map) {
          // Convert keys and values to respective target type, if determined.
          convertedValue = convertToTypedMap(
                  (Map<?, ?>) convertedValue, propertyName, requiredType, typeDescriptor);
          standardConversion = true;
        }
        if (convertedValue.getClass().isArray() && Array.getLength(convertedValue) == 1) {
          convertedValue = Array.get(convertedValue, 0);
          standardConversion = true;
        }
        if (String.class == requiredType && ClassUtils.isPrimitiveOrWrapper(convertedValue.getClass())) {
          // We can stringify any primitive value...
          return (T) convertedValue.toString();
        }
        else if (convertedValue instanceof String && !requiredType.isInstance(convertedValue)) {
          if (conversionAttemptEx == null && !requiredType.isInterface() && !requiredType.isEnum()) {
            try {
              Constructor<T> strCtor = requiredType.getConstructor(String.class);
              return BeanUtils.newInstance(strCtor, convertedValue);
            }
            catch (NoSuchMethodException ex) {
              // proceed with field lookup
              if (logger.isDebugEnabled()) {
                logger.trace("No String constructor found on type [{}]", requiredType.getName(), ex);
              }
            }
            catch (Exception ex) {
              if (logger.isDebugEnabled()) {
                logger.debug("Construction via String failed for type [{}]", requiredType.getName(), ex);
              }
            }
          }
          String trimmedValue = ((String) convertedValue).trim();
          if (requiredType.isEnum() && trimmedValue.isEmpty()) {
            // It's an empty enum identifier: reset the enum value to null.
            return null;
          }
          convertedValue = attemptToConvertStringToEnum(requiredType, trimmedValue, convertedValue);
          standardConversion = true;
        }
        else if (convertedValue instanceof Number && Number.class.isAssignableFrom(requiredType)) {
          convertedValue = NumberUtils.convertNumberToTargetClass(
                  (Number) convertedValue, (Class<Number>) requiredType);
          standardConversion = true;
        }
      }
      else {
        // convertedValue == null
        if (requiredType == Optional.class) {
          convertedValue = Optional.empty();
        }
      }

      if (!ClassUtils.isAssignableValue(requiredType, convertedValue)) {
        if (conversionAttemptEx != null) {
          // Original exception from former ConversionService call above...
          throw conversionAttemptEx;
        }
        else if (conversionService != null && typeDescriptor != null) {
          // ConversionService not tried before, probably custom editor found
          // but editor couldn't produce the required type...
          TypeDescriptor sourceTypeDesc = TypeDescriptor.fromObject(newValue);
          if (conversionService.canConvert(sourceTypeDesc, typeDescriptor)) {
            return (T) conversionService.convert(newValue, sourceTypeDesc, typeDescriptor);
          }
        }

        // Definitely doesn't match: throw IllegalArgumentException/IllegalStateException
        StringBuilder msg = new StringBuilder();
        msg.append("Cannot convert value of type '").append(ClassUtils.getDescriptiveType(newValue));
        msg.append("' to required type '").append(ClassUtils.getQualifiedName(requiredType)).append('\'');
        if (propertyName != null) {
          msg.append(" for property '").append(propertyName).append('\'');
        }
        if (editor != null) {
          msg.append(": PropertyEditor [")
                  .append(editor.getClass().getName())
                  .append("] returned inappropriate value of type '")
                  .append(ClassUtils.getDescriptiveType(convertedValue))
                  .append('\'');
          throw new IllegalArgumentException(msg.toString());
        }
        else {
          msg.append(": no matching editors or conversion strategy found");
          throw new IllegalStateException(msg.toString());
        }
      }
    }

    if (conversionAttemptEx != null) {
      if (editor == null && !standardConversion && requiredType != null && Object.class != requiredType) {
        throw conversionAttemptEx;
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Original ConversionService attempt failed - ignored since " +
                "PropertyEditor based conversion eventually succeeded", conversionAttemptEx);
      }
    }

    return (T) convertedValue;
  }

  private Object attemptToConvertStringToEnum(Class<?> requiredType, String trimmedValue, Object currentConvertedValue) {
    Object convertedValue = currentConvertedValue;

    if (Enum.class == requiredType && this.targetObject != null) {
      // target type is declared as raw enum, treat the trimmed value as <enum.fqn>.FIELD_NAME
      int index = trimmedValue.lastIndexOf('.');
      if (index > -1) {
        String enumType = trimmedValue.substring(0, index);
        String fieldName = trimmedValue.substring(index + 1);
        ClassLoader cl = this.targetObject.getClass().getClassLoader();
        try {
          Class<?> enumValueType = ClassUtils.forName(enumType, cl);
          Field enumField = enumValueType.getField(fieldName);
          convertedValue = enumField.get(null);
        }
        catch (ClassNotFoundException ex) {
          if (logger.isDebugEnabled()) {
            logger.trace("Enum class [{}] cannot be loaded", enumType, ex);
          }
        }
        catch (Throwable ex) {
          if (logger.isDebugEnabled()) {
            logger.trace("Field [{}] isn't an enum value for type [{}]", fieldName, enumType, ex);
          }
        }
      }
    }

    if (convertedValue == currentConvertedValue) {
      // Try field lookup as fallback: for JDK 1.5 enum or custom enum
      // with values defined as static fields. Resulting value still needs
      // to be checked, hence we don't return it right away.
      try {
        Field enumField = requiredType.getField(trimmedValue);
        ReflectionUtils.makeAccessible(enumField);
        convertedValue = enumField.get(null);
      }
      catch (Throwable ex) {
        if (logger.isDebugEnabled()) {
          logger.trace("Field [{}] isn't an enum value", convertedValue, ex);
        }
      }
    }

    return convertedValue;
  }

  /**
   * Find a default editor for the given type.
   *
   * @param requiredType the type to find an editor for
   * @return the corresponding editor, or {@code null} if none
   */
  @Nullable
  private PropertyEditor findDefaultEditor(@Nullable Class<?> requiredType) {
    PropertyEditor editor = null;
    if (requiredType != null) {
      // No custom editor -> check BeanWrapperImpl's default editors.
      editor = this.propertyEditorRegistry.getDefaultEditor(requiredType);
      if (editor == null && String.class != requiredType) {
        // No BeanWrapper default editor -> check standard JavaBean editor.
        editor = BeanUtils.findEditorByConvention(requiredType);
      }
    }
    return editor;
  }

  /**
   * Convert the value to the required type (if necessary from a String),
   * using the given property editor.
   *
   * @param oldValue the previous value, if available (may be {@code null})
   * @param newValue the proposed new value
   * @param requiredType the type we must convert to
   * (or {@code null} if not known, for example in case of a collection element)
   * @param editor the PropertyEditor to use
   * @return the new value, possibly the result of type conversion
   * @throws IllegalArgumentException if type conversion failed
   */
  @Nullable
  private Object doConvertValue(@Nullable Object oldValue, @Nullable Object newValue,
          @Nullable Class<?> requiredType, @Nullable PropertyEditor editor) {

    Object convertedValue = newValue;

    if (editor != null && !(convertedValue instanceof String)) {
      // Not a String -> use PropertyEditor's setValue.
      // With standard PropertyEditors, this will return the very same object;
      // we just want to allow special PropertyEditors to override setValue
      // for type conversion from non-String values to the required type.
      try {
        editor.setValue(convertedValue);
        Object newConvertedValue = editor.getValue();
        if (newConvertedValue != convertedValue) {
          convertedValue = newConvertedValue;
          // Reset PropertyEditor: It already did a proper conversion.
          // Don't use it again for a setAsText call.
          editor = null;
        }
      }
      catch (Exception ex) {
        if (logger.isDebugEnabled()) {
          logger.debug("PropertyEditor [{}] does not support setValue call", editor.getClass().getName(), ex);
        }
        // Swallow and proceed.
      }
    }

    Object returnValue = convertedValue;

    if (requiredType != null && !requiredType.isArray() && convertedValue instanceof String[]) {
      // Convert String array to a comma-separated String.
      // Only applies if no PropertyEditor converted the String array before.
      // The CSV String will be passed into a PropertyEditor's setAsText method, if any.
      if (logger.isDebugEnabled()) {
        logger.trace("Converting String array to comma-delimited String [{}]", convertedValue);
      }
      convertedValue = StringUtils.arrayToCommaDelimitedString((String[]) convertedValue);
    }

    if (convertedValue instanceof String) {
      if (editor != null) {
        // Use PropertyEditor's setAsText in case of a String value.
        if (logger.isDebugEnabled()) {
          logger.trace("Converting String to [{}] using property editor [{}]", requiredType, editor);
        }
        String newTextValue = (String) convertedValue;
        return doConvertTextValue(oldValue, newTextValue, editor);
      }
      else if (String.class == requiredType) {
        returnValue = convertedValue;
      }
    }

    return returnValue;
  }

  /**
   * Convert the given text value using the given property editor.
   *
   * @param oldValue the previous value, if available (may be {@code null})
   * @param newTextValue the proposed text value
   * @param editor the PropertyEditor to use
   * @return the converted value
   */
  private Object doConvertTextValue(@Nullable Object oldValue, String newTextValue, PropertyEditor editor) {
    try {
      editor.setValue(oldValue);
    }
    catch (Exception ex) {
      if (logger.isDebugEnabled()) {
        logger.debug("PropertyEditor [{}] does not support setValue call", editor.getClass().getName(), ex);
      }
      // Swallow and proceed.
    }
    editor.setAsText(newTextValue);
    return editor.getValue();
  }

  private Object convertToTypedArray(Object input, @Nullable String propertyName, Class<?> componentType) {
    if (input instanceof Collection<?> coll) {
      // Convert Collection elements to array elements.
      Object result = Array.newInstance(componentType, coll.size());
      int i = 0;
      for (Iterator<?> it = coll.iterator(); it.hasNext(); i++) {
        Object value = convertIfNecessary(
                buildIndexedPropertyName(propertyName, i), null, it.next(), componentType);
        Array.set(result, i, value);
      }
      return result;
    }
    else if (input.getClass().isArray()) {
      // Convert array elements, if necessary.
      if (componentType.equals(input.getClass().getComponentType())
              && !this.propertyEditorRegistry.hasCustomEditorForElement(componentType, propertyName)) {
        return input;
      }
      int arrayLength = Array.getLength(input);
      Object result = Array.newInstance(componentType, arrayLength);
      for (int i = 0; i < arrayLength; i++) {
        Object value = convertIfNecessary(
                buildIndexedPropertyName(propertyName, i), null, Array.get(input, i), componentType);
        Array.set(result, i, value);
      }
      return result;
    }
    else {
      // A plain value: convert it to an array with a single component.
      Object result = Array.newInstance(componentType, 1);
      Object value = convertIfNecessary(
              buildIndexedPropertyName(propertyName, 0), null, input, componentType);
      Array.set(result, 0, value);
      return result;
    }
  }

  @SuppressWarnings("unchecked")
  private Collection<?> convertToTypedCollection(
          Collection<?> original, @Nullable String propertyName,
          Class<?> requiredType, @Nullable TypeDescriptor typeDescriptor) {

    if (!Collection.class.isAssignableFrom(requiredType)) {
      return original;
    }

    boolean approximable = CollectionUtils.isApproximableCollectionType(requiredType);
    if (!approximable && cannotCreateCopy(requiredType)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Custom Collection type [{}] does not allow for creating a copy - injecting original Collection as-is",
                classNameOf(original));
      }
      return original;
    }

    boolean originalAllowed = requiredType.isInstance(original);
    TypeDescriptor elementType = typeDescriptor != null ? typeDescriptor.getElementDescriptor() : null;
    if (elementType == null && originalAllowed
            && !this.propertyEditorRegistry.hasCustomEditorForElement(null, propertyName)) {
      return original;
    }

    Iterator<?> it;
    try {
      it = original.iterator();
    }
    catch (Throwable ex) {
      if (logger.isDebugEnabled()) {
        logger.debug("Cannot access Collection of type [{}] - injecting original Collection as-is: {}",
                classNameOf(original), ex.toString());
      }
      return original;
    }

    Collection<Object> convertedCopy;
    try {
      if (approximable) {
        convertedCopy = CollectionUtils.createApproximateCollection(original, original.size());
      }
      else {
        convertedCopy = (Collection<Object>)
                ReflectionUtils.accessibleConstructor(requiredType).newInstance();
      }
    }
    catch (Throwable ex) {
      if (logger.isDebugEnabled()) {
        logger.debug("Cannot create copy of Collection type [{}] - injecting original Collection as-is: {}",
                classNameOf(original), ex.toString());
      }
      return original;
    }

    for (int i = 0; it.hasNext(); i++) {
      Object element = it.next();
      String indexedPropertyName = buildIndexedPropertyName(propertyName, i);
      Object convertedElement = convertIfNecessary(indexedPropertyName, null, element,
              (elementType != null ? elementType.getType() : null), elementType);
      try {
        convertedCopy.add(convertedElement);
      }
      catch (Throwable ex) {
        if (logger.isDebugEnabled()) {
          logger.debug("Collection type [{}] seems to be read-only - injecting original Collection as-is: {}",
                  classNameOf(original), ex.toString());
        }
        return original;
      }
      originalAllowed = originalAllowed && (element == convertedElement);
    }
    return originalAllowed ? original : convertedCopy;
  }

  @SuppressWarnings("unchecked")
  private Map<?, ?> convertToTypedMap(
          Map<?, ?> original, @Nullable String propertyName,
          Class<?> requiredType, @Nullable TypeDescriptor typeDescriptor) {

    if (!Map.class.isAssignableFrom(requiredType)) {
      return original;
    }

    boolean approximable = CollectionUtils.isApproximableMapType(requiredType);
    if (!approximable && cannotCreateCopy(requiredType)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Custom Map type [{}] does not allow for creating a copy - injecting original Map as-is", classNameOf(original));
      }
      return original;
    }

    boolean originalAllowed = requiredType.isInstance(original);
    TypeDescriptor keyType = typeDescriptor != null ? typeDescriptor.getMapKeyDescriptor() : null;
    TypeDescriptor valueType = typeDescriptor != null ? typeDescriptor.getMapValueDescriptor() : null;
    if (keyType == null && valueType == null
            && originalAllowed && !this.propertyEditorRegistry.hasCustomEditorForElement(null, propertyName)) {
      return original;
    }

    Iterator<?> it;
    try {
      it = original.entrySet().iterator();
    }
    catch (Throwable ex) {
      if (logger.isDebugEnabled()) {
        logger.debug("Cannot access Map of type [{}] - injecting original Map as-is: {}", classNameOf(original), ex.toString());
      }
      return original;
    }

    Map<Object, Object> convertedCopy;
    try {
      if (approximable) {
        convertedCopy = CollectionUtils.createApproximateMap(original, original.size());
      }
      else {
        convertedCopy = (Map<Object, Object>) ReflectionUtils.accessibleConstructor(requiredType).newInstance();
      }
    }
    catch (Throwable ex) {
      if (logger.isDebugEnabled()) {
        logger.debug("Cannot create copy of Map type [{}] - injecting original Map as-is: {}", classNameOf(original), ex.toString());
      }
      return original;
    }

    while (it.hasNext()) {
      Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
      Object key = entry.getKey();
      Object value = entry.getValue();
      String keyedPropertyName = buildKeyedPropertyName(propertyName, key);
      Object convertedKey = convertIfNecessary(
              keyedPropertyName, null, key, (keyType != null ? keyType.getType() : null), keyType);
      Object convertedValue = convertIfNecessary(
              keyedPropertyName, null, value, (valueType != null ? valueType.getType() : null), valueType);
      try {
        convertedCopy.put(convertedKey, convertedValue);
      }
      catch (Throwable ex) {
        if (logger.isDebugEnabled()) {
          logger.debug("Map type [{}] seems to be read-only - injecting original Map as-is: {}", classNameOf(original), ex.toString());
        }
        return original;
      }
      originalAllowed = originalAllowed && (key == convertedKey) && (value == convertedValue);
    }
    return (originalAllowed ? original : convertedCopy);
  }

  private static String classNameOf(Object original) {
    return original.getClass().getName();
  }

  @Nullable
  private String buildIndexedPropertyName(@Nullable String propertyName, int index) {
    return propertyName != null
           ? propertyName + PropertyAccessor.PROPERTY_KEY_PREFIX + index + PropertyAccessor.PROPERTY_KEY_SUFFIX
           : null;
  }

  @Nullable
  private String buildKeyedPropertyName(@Nullable String propertyName, Object key) {
    return propertyName != null
           ? propertyName + PropertyAccessor.PROPERTY_KEY_PREFIX + key + PropertyAccessor.PROPERTY_KEY_SUFFIX
           : null;
  }

  private boolean cannotCreateCopy(Class<?> requiredType) {
    return requiredType.isInterface()
            || Modifier.isAbstract(requiredType.getModifiers())
            || !Modifier.isPublic(requiredType.getModifiers())
            || !ReflectionUtils.hasConstructor(requiredType);
  }

}
