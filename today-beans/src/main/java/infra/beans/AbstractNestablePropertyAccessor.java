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

package infra.beans;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import infra.core.ResolvableType;
import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionException;
import infra.core.conversion.ConverterNotFoundException;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.CollectionUtils;
import infra.util.ObjectUtils;
import infra.util.StringUtils;

/**
 * A basic {@link ConfigurablePropertyAccessor} that provides the necessary
 * infrastructure for all typical use cases.
 *
 * <p>This accessor will convert collection and array values to the corresponding
 * target collections or arrays, if necessary. Custom property editors that deal
 * with collections or arrays can either be written via PropertyEditor's
 * {@code setValue}, or against a comma-delimited String via {@code setAsText},
 * as String arrays are converted in such a format if the array itself is not
 * assignable.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Rod Johnson
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #registerCustomEditor
 * @see #setPropertyValues
 * @see #setPropertyValue
 * @see #getPropertyValue
 * @see #getPropertyType
 * @see BeanWrapper
 * @see PropertyEditorRegistrySupport
 * @since 4.0 2022/2/17 17:42
 */
public abstract class AbstractNestablePropertyAccessor extends AbstractPropertyAccessor {
  /**
   * We'll create a lot of these objects, so we don't want a new logger every time.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractNestablePropertyAccessor.class);

  private int autoGrowCollectionLimit = Integer.MAX_VALUE;

  @Nullable
  protected Object wrappedObject;

  private String nestedPath = "";

  @Nullable
  protected Object rootObject;

  /** Map with cached nested Accessors: nested path -> Accessor instance. */
  @Nullable
  private HashMap<String, AbstractNestablePropertyAccessor> nestedPropertyAccessors;

  /**
   * Create a new empty accessor. Wrapped instance needs to be set afterwards.
   * Registers default editors.
   *
   * @see #setWrappedInstance
   */
  protected AbstractNestablePropertyAccessor() {
    this(true);
  }

  /**
   * Create a new empty accessor. Wrapped instance needs to be set afterwards.
   *
   * @param registerDefaultEditors whether to register default editors
   * (can be suppressed if the accessor won't need any type conversion)
   * @see #setWrappedInstance
   */
  protected AbstractNestablePropertyAccessor(boolean registerDefaultEditors) {
    if (registerDefaultEditors) {
      registerDefaultEditors();
    }
    this.typeConverterDelegate = new TypeConverterDelegate(this);
  }

  /**
   * Create a new accessor for the given object.
   *
   * @param object the object wrapped by this accessor
   */
  protected AbstractNestablePropertyAccessor(Object object) {
    registerDefaultEditors();
    setWrappedInstance(object);
  }

  /**
   * Create a new accessor, wrapping a new instance of the specified class.
   *
   * @param clazz class to instantiate and wrap
   */
  protected AbstractNestablePropertyAccessor(Class<?> clazz) {
    registerDefaultEditors();
    setWrappedInstance(BeanUtils.newInstance(clazz));
  }

  /**
   * Create a new accessor for the given object,
   * registering a nested path that the object is in.
   *
   * @param object the object wrapped by this accessor
   * @param nestedPath the nested path of the object
   * @param rootObject the root object at the top of the path
   */
  protected AbstractNestablePropertyAccessor(Object object, String nestedPath, Object rootObject) {
    registerDefaultEditors();
    setWrappedInstance(object, nestedPath, rootObject);
  }

  /**
   * Create a new accessor for the given object,
   * registering a nested path that the object is in.
   *
   * @param object the object wrapped by this accessor
   * @param nestedPath the nested path of the object
   * @param parent the containing accessor (must not be {@code null})
   */
  protected AbstractNestablePropertyAccessor(Object object, String nestedPath, AbstractNestablePropertyAccessor parent) {
    setWrappedInstance(object, nestedPath, parent.getWrappedInstance());
    setExtractOldValueForEditor(parent.isExtractOldValueForEditor());
    setAutoGrowNestedPaths(parent.isAutoGrowNestedPaths());
    setAutoGrowCollectionLimit(parent.getAutoGrowCollectionLimit());
    setConversionService(parent.getConversionService());
  }

  /**
   * Specify a limit for array and collection auto-growing.
   * <p>Default is unlimited on a plain accessor.
   */
  public void setAutoGrowCollectionLimit(int autoGrowCollectionLimit) {
    this.autoGrowCollectionLimit = autoGrowCollectionLimit;
  }

  /**
   * Return the limit for array and collection auto-growing.
   */
  public int getAutoGrowCollectionLimit() {
    return this.autoGrowCollectionLimit;
  }

  /**
   * Switch the target object, replacing the cached introspection results only
   * if the class of the new object is different to that of the replaced object.
   *
   * @param object the new target object
   */
  public void setWrappedInstance(Object object) {
    setWrappedInstance(object, "", null);
  }

  /**
   * Switch the target object, replacing the cached introspection results only
   * if the class of the new object is different to that of the replaced object.
   *
   * @param object the new target object
   * @param nestedPath the nested path of the object
   * @param rootObject the root object at the top of the path
   */
  public void setWrappedInstance(Object object, @Nullable String nestedPath, @Nullable Object rootObject) {
    Object wrappedObject = ObjectUtils.unwrapOptional(object);
    Assert.notNull(wrappedObject, "Target object is required");
    if (nestedPath == null) {
      nestedPath = "";
    }
    this.nestedPath = nestedPath;
    this.wrappedObject = wrappedObject;
    this.rootObject = !nestedPath.isEmpty() ? rootObject : wrappedObject;
    this.nestedPropertyAccessors = null;
    this.typeConverterDelegate = new TypeConverterDelegate(this, wrappedObject);
  }

  public final Object getWrappedInstance() {
    Assert.state(wrappedObject != null, "No wrapped object");
    return wrappedObject;
  }

  public final Class<?> getWrappedClass() {
    return getWrappedInstance().getClass();
  }

  /**
   * Return the nested path of the object wrapped by this accessor.
   */
  public final String getNestedPath() {
    return this.nestedPath;
  }

  /**
   * Return the root object at the top of the path of this accessor.
   *
   * @see #getNestedPath
   */
  public final Object getRootInstance() {
    Assert.state(this.rootObject != null, "No root object");
    return this.rootObject;
  }

  /**
   * Return the class of the root object at the top of the path of this accessor.
   *
   * @see #getNestedPath
   */
  public final Class<?> getRootClass() {
    return getRootInstance().getClass();
  }

  @Override
  public void setPropertyValue(String propertyName, @Nullable Object value) throws BeansException {
    AbstractNestablePropertyAccessor nestedPa = getNestablePropertyAccessor(propertyName);
    PropertyTokenHolder tokens = getPropertyNameTokens(getFinalPath(nestedPa, propertyName));
    nestedPa.setPropertyValue(tokens, new PropertyValue(propertyName, value));
  }

  @Override
  public void setPropertyValue(PropertyValue pv) throws BeansException {
    if (pv.resolvedTokens instanceof PropertyTokenHolder tokens) {
      setPropertyValue(tokens, pv);
    }
    else {
      String propertyName = pv.getName();
      AbstractNestablePropertyAccessor nestedPa = getNestablePropertyAccessor(propertyName);
      PropertyTokenHolder tokens = getPropertyNameTokens(getFinalPath(nestedPa, propertyName));
      if (nestedPa == this) {
        pv.getOriginalPropertyValue().resolvedTokens = tokens;
      }
      nestedPa.setPropertyValue(tokens, pv);
    }
  }

  private AbstractNestablePropertyAccessor getNestablePropertyAccessor(String propertyName) {
    try {
      return getPropertyAccessorForPropertyPath(propertyName);
    }
    catch (NotReadablePropertyException ex) {
      throw new NotWritablePropertyException(getRootClass(), this.nestedPath + propertyName,
              "Nested property in path '%s' does not exist".formatted(propertyName), ex);
    }
  }

  protected void setPropertyValue(PropertyTokenHolder tokens, PropertyValue pv) throws BeansException {
    if (tokens.keys != null) {
      processKeyedProperty(tokens, pv);
    }
    else {
      processLocalProperty(tokens, pv);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void processKeyedProperty(PropertyTokenHolder tokens, PropertyValue pv) {
    Object propValue = getPropertyHoldingValue(tokens);
    PropertyHandler ph = getLocalPropertyHandler(tokens.actualName);
    if (ph == null) {
      throw new InvalidPropertyException(
              getRootClass(), this.nestedPath + tokens.actualName, "No property handler found");
    }
    String[] keys = tokens.keys;
    Assert.state(keys != null, "No token keys");
    String lastKey = keys[keys.length - 1];

    if (propValue.getClass().isArray()) {
      Class<?> componentType = propValue.getClass().getComponentType();
      int arrayIndex = Integer.parseInt(lastKey);
      Object oldValue = null;
      try {
        if (isExtractOldValueForEditor() && arrayIndex < Array.getLength(propValue)) {
          oldValue = Array.get(propValue, arrayIndex);
        }
        Object convertedValue = convertIfNecessary(
                tokens.canonicalName, oldValue, pv.getValue(), componentType, ph.nested(keys.length));
        int length = Array.getLength(propValue);
        if (arrayIndex >= length && arrayIndex < this.autoGrowCollectionLimit) {
          Object newArray = Array.newInstance(componentType, arrayIndex + 1);
          System.arraycopy(propValue, 0, newArray, 0, length);
          int lastKeyIndex = tokens.canonicalName.lastIndexOf('[');
          String propName = tokens.canonicalName.substring(0, lastKeyIndex);
          setPropertyValue(propName, newArray);
          propValue = getPropertyValue(propName);
        }
        Array.set(propValue, arrayIndex, convertedValue);
      }
      catch (IndexOutOfBoundsException ex) {
        throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
                "Invalid array index in property path '%s'".formatted(tokens.canonicalName), ex);
      }
    }
    else if (propValue instanceof List list) {
      TypeDescriptor requiredType = ph.getCollectionType(tokens.keys.length);
      int index = Integer.parseInt(lastKey);
      Object oldValue = null;
      if (isExtractOldValueForEditor() && index < list.size()) {
        oldValue = list.get(index);
      }
      Object convertedValue = convertIfNecessary(tokens.canonicalName, oldValue, pv.getValue(),
              requiredType.getResolvableType().resolve(), requiredType);
      int size = list.size();
      if (index >= size && index < this.autoGrowCollectionLimit) {
        for (int i = size; i < index; i++) {
          try {
            list.add(null);
          }
          catch (NullPointerException ex) {
            throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName, """
                    Cannot set element with index %s in List of size %s, accessed using property path '%s': \
                    List does not support filling up gaps with null elements""".formatted(index, size, tokens.canonicalName));
          }
        }
        list.add(convertedValue);
      }
      else {
        try {
          list.set(index, convertedValue);
        }
        catch (IndexOutOfBoundsException ex) {
          throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
                  "Invalid list index in property path '%s'".formatted(tokens.canonicalName), ex);
        }
      }
    }
    else if (propValue instanceof Map map) {
      TypeDescriptor mapKeyType = ph.getMapKeyType(tokens.keys.length);
      TypeDescriptor mapValueType = ph.getMapValueType(tokens.keys.length);
      // IMPORTANT: Do not pass full property name in here - property editors
      // must not kick in for map keys but rather only for map values.
      Object convertedMapKey = convertIfNecessary(null, null, lastKey,
              mapKeyType.getResolvableType().resolve(), mapKeyType);
      Object oldValue = null;
      if (isExtractOldValueForEditor()) {
        oldValue = map.get(convertedMapKey);
      }
      // Pass full property name and old value in here, since we want full
      // conversion ability for map values.
      Object convertedMapValue = convertIfNecessary(tokens.canonicalName, oldValue, pv.getValue(),
              mapValueType.getResolvableType().resolve(), mapValueType);
      map.put(convertedMapKey, convertedMapValue);
    }
    else {
      throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
              "Property referenced in indexed property path '%s' is neither an array nor a List nor a Map; returned value was [%s]"
                      .formatted(tokens.canonicalName, propValue));
    }
  }

  private Object getPropertyHoldingValue(PropertyTokenHolder tokens) {
    // Apply indexes and map keys: fetch value for all keys but the last one.
    String[] keys = tokens.keys;
    Assert.state(keys != null, "No token keys");
    PropertyTokenHolder getterTokens = new PropertyTokenHolder(tokens.actualName);
    getterTokens.canonicalName = tokens.canonicalName;
    getterTokens.keys = new String[keys.length - 1];
    System.arraycopy(keys, 0, getterTokens.keys, 0, keys.length - 1);

    Object propValue;
    try {
      propValue = getPropertyValue(getterTokens);
    }
    catch (NotReadablePropertyException ex) {
      throw new NotWritablePropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
              "Cannot access indexed value in property referenced in indexed property path '%s'".formatted(tokens.canonicalName), ex);
    }

    if (propValue == null) {
      // null map value case
      if (isAutoGrowNestedPaths()) {
        int lastKeyIndex = tokens.canonicalName.lastIndexOf('[');
        getterTokens.canonicalName = tokens.canonicalName.substring(0, lastKeyIndex);
        propValue = setDefaultValue(getterTokens);
      }
      else {
        throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + tokens.canonicalName,
                "Cannot access indexed value in property referenced in indexed property path '%s': returned null"
                        .formatted(tokens.canonicalName));
      }
    }
    return propValue;
  }

  private void processLocalProperty(PropertyTokenHolder tokens, PropertyValue pv) {
    PropertyHandler ph = getLocalPropertyHandler(tokens.actualName);
    if (ph == null || !ph.writable) {
      if (pv.isOptional()) {
        if (log.isDebugEnabled()) {
          log.debug("Ignoring optional value for property '{}' - property not found on bean class [{}]",
                  tokens.actualName, getRootClass().getName());
        }
        return;
      }
      if (this.suppressNotWritablePropertyException) {
        // Optimization for common ignoreUnknown=true scenario since the
        // exception would be caught and swallowed higher up anyway...
        return;
      }
      throw createNotWritablePropertyException(tokens.canonicalName);
    }

    Object oldValue = null;
    try {
      Object originalValue = pv.getValue();
      Object valueToApply = originalValue;
      if (!Boolean.FALSE.equals(pv.conversionNecessary)) {
        if (pv.isConverted()) {
          valueToApply = pv.getConvertedValue();
        }
        else {
          if (isExtractOldValueForEditor() && ph.readable) {
            try {
              oldValue = ph.getValue();
            }
            catch (Exception ex) {
              if (log.isDebugEnabled()) {
                if (ex instanceof PrivilegedActionException) {
                  ex = ((PrivilegedActionException) ex).getException();
                }
                log.debug("Could not read previous value of property '{}{}'", nestedPath, tokens.canonicalName, ex);
              }
            }
          }
          valueToApply = convertForProperty(
                  tokens.canonicalName, oldValue, originalValue, ph.toTypeDescriptor());
        }
        pv.getOriginalPropertyValue().conversionNecessary = valueToApply != originalValue;
      }
      ph.setValue(valueToApply);
    }
    catch (TypeMismatchException ex) {
      throw ex;
    }
    catch (InvocationTargetException ex) {
      PropertyChangeEvent event = new PropertyChangeEvent(
              getRootInstance(), this.nestedPath + tokens.canonicalName, oldValue, pv.getValue());
      if (ex.getTargetException() instanceof ClassCastException) {
        throw new TypeMismatchException(event, ph.propertyType, ex.getTargetException());
      }
      else {
        Throwable cause = ex.getTargetException();
        if (cause instanceof UndeclaredThrowableException) {
          // May happen e.g. with Groovy-generated methods
          cause = cause.getCause();
        }
        throw new MethodInvocationException(event, cause);
      }
    }
    catch (Exception ex) {
      var pce = new PropertyChangeEvent(getRootInstance(), this.nestedPath + tokens.canonicalName, oldValue, pv.getValue());
      throw new MethodInvocationException(pce, ex);
    }
  }

  @Override
  @Nullable
  public Class<?> getPropertyType(String propertyName) throws BeansException {
    try {
      PropertyHandler ph = getPropertyHandler(propertyName);
      if (ph != null) {
        return ph.propertyType;
      }
      else {
        // Maybe an indexed/mapped property...
        Object value = getPropertyValue(propertyName);
        if (value != null) {
          return value.getClass();
        }
        // Check to see if there is a custom editor,
        // which might give an indication on the desired target type.
        Class<?> editorType = guessPropertyTypeFromEditors(propertyName);
        if (editorType != null) {
          return editorType;
        }
      }
    }
    catch (InvalidPropertyException ex) {
      // Consider as not determinable.
    }
    return null;
  }

  @Override
  @Nullable
  public TypeDescriptor getPropertyTypeDescriptor(String propertyName) throws BeansException {
    try {
      AbstractNestablePropertyAccessor nestedPa = getPropertyAccessorForPropertyPath(propertyName);
      String finalPath = getFinalPath(nestedPa, propertyName);
      PropertyTokenHolder tokens = getPropertyNameTokens(finalPath);
      PropertyHandler handler = nestedPa.getLocalPropertyHandler(tokens.actualName);
      if (handler != null) {
        if (tokens.keys != null) {
          if (handler.readable || handler.writable) {
            return handler.nested(tokens.keys.length);
          }
        }
        else {
          if (handler.readable || handler.writable) {
            return handler.toTypeDescriptor();
          }
        }
      }
    }
    catch (InvalidPropertyException ex) {
      // Consider as not determinable.
    }
    return null;
  }

  @Override
  public boolean isReadableProperty(String propertyName) {
    try {
      PropertyHandler ph = getPropertyHandler(propertyName);
      if (ph != null) {
        return ph.readable;
      }
      else {
        // Maybe an indexed/mapped property...
        getPropertyValue(propertyName);
        return true;
      }
    }
    catch (InvalidPropertyException ex) {
      // Cannot be evaluated, so can't be readable.
    }
    return false;
  }

  @Override
  public boolean isWritableProperty(String propertyName) {
    try {
      PropertyHandler ph = getPropertyHandler(propertyName);
      if (ph != null) {
        return ph.writable;
      }
      else {
        // Maybe an indexed/mapped property...
        getPropertyValue(propertyName);
        return true;
      }
    }
    catch (InvalidPropertyException ex) {
      // Cannot be evaluated, so can't be writable.
    }
    return false;
  }

  @Nullable
  private Object convertIfNecessary(@Nullable String propertyName, @Nullable Object oldValue,
          @Nullable Object newValue, @Nullable Class<?> requiredType, @Nullable TypeDescriptor td) throws TypeMismatchException {

    try {
      return typeConverterDelegate.convertIfNecessary(propertyName, oldValue, newValue, requiredType, td);
    }
    catch (ConverterNotFoundException | IllegalStateException ex) {
      var pce = new PropertyChangeEvent(getRootInstance(),
              this.nestedPath + propertyName, oldValue, newValue);
      throw new ConversionNotSupportedException(pce, requiredType, ex);
    }
    catch (ConversionException | IllegalArgumentException ex) {
      var pce = new PropertyChangeEvent(getRootInstance(),
              this.nestedPath + propertyName, oldValue, newValue);
      throw new TypeMismatchException(pce, requiredType, ex);
    }
  }

  @Nullable
  protected Object convertForProperty(String propertyName, @Nullable Object oldValue,
          @Nullable Object newValue, TypeDescriptor td) throws TypeMismatchException {

    return convertIfNecessary(propertyName, oldValue, newValue, td.getType(), td);
  }

  @Override
  @Nullable
  public Object getPropertyValue(String propertyName) throws BeansException {
    AbstractNestablePropertyAccessor nestedPa = getPropertyAccessorForPropertyPath(propertyName);
    PropertyTokenHolder tokens = getPropertyNameTokens(getFinalPath(nestedPa, propertyName));
    return nestedPa.getPropertyValue(tokens);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Nullable
  protected Object getPropertyValue(PropertyTokenHolder tokens) throws BeansException {
    String actualName = tokens.actualName;
    String propertyName = tokens.canonicalName;
    PropertyHandler handler = getLocalPropertyHandler(actualName);
    if (handler == null || !handler.readable) {
      throw new NotReadablePropertyException(getRootClass(), this.nestedPath + propertyName);
    }
    try {
      Object value = handler.getValue();
      String[] keys = tokens.keys;
      if (keys != null) {
        if (value == null) {
          if (isAutoGrowNestedPaths()) {
            value = setDefaultValue(new PropertyTokenHolder(tokens.actualName));
          }
          else {
            throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + propertyName,
                    "Cannot access indexed value of property referenced in indexed property path '%s': returned null".formatted(propertyName));
          }
        }
        StringBuilder indexedPropertyName = new StringBuilder(tokens.actualName);
        // apply indexes and map keys
        for (int i = 0; i < keys.length; i++) {
          String key = keys[i];
          if (value == null) {
            throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + propertyName,
                    "Cannot access indexed value of property referenced in indexed property path '%s': returned null".formatted(propertyName));
          }
          else if (value.getClass().isArray()) {
            int index = Integer.parseInt(key);
            value = growArrayIfNecessary(value, index, indexedPropertyName.toString());
            value = Array.get(value, index);
          }
          else if (value instanceof List list) {
            int index = Integer.parseInt(key);
            growCollectionIfNecessary(list, index, indexedPropertyName.toString(), handler, i + 1);
            value = list.get(index);
          }
          else if (value instanceof Map<?, ?> map) {
            Class<?> mapKeyType = handler.getResolvableType().getNested(i + 1).asMap().resolveGeneric(0);
            // IMPORTANT: Do not pass full property name in here - property editors
            // must not kick in for map keys but rather only for map values.
            TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(mapKeyType);
            Object convertedMapKey = convertIfNecessary(null, null, key, mapKeyType, typeDescriptor);
            value = map.get(convertedMapKey);
          }
          else if (value instanceof Iterable iterable) {
            // Apply index to Iterator in case of a Set/Collection/Iterable.
            int index = Integer.parseInt(key);
            if (value instanceof Collection<?> coll) {
              if (index < 0 || index >= coll.size()) {
                throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
                        "Cannot get element with index %s from Collection of size %s, accessed using property path '%s'"
                                .formatted(index, coll.size(), propertyName));
              }
            }
            Iterator<Object> it = iterable.iterator();
            boolean found = false;
            int currIndex = 0;
            for (; it.hasNext(); currIndex++) {
              Object elem = it.next();
              if (currIndex == index) {
                value = elem;
                found = true;
                break;
              }
            }
            if (!found) {
              throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
                      "Cannot get element with index %s from Iterable of size %s, accessed using property path '%s'"
                              .formatted(index, currIndex, propertyName));
            }
          }
          else {
            throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
                    "Property referenced in indexed property path '%s' is neither an array nor a List nor a Set nor a Map; returned value was [%s]"
                            .formatted(propertyName, value));
          }
          indexedPropertyName.append(PROPERTY_KEY_PREFIX);
          indexedPropertyName.append(key);
          indexedPropertyName.append(PROPERTY_KEY_SUFFIX);
        }
      }
      return value;
    }
    catch (InvalidPropertyException ex) {
      throw ex;
    }
    catch (IndexOutOfBoundsException ex) {
      throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
              "Index of out of bounds in property path '%s'".formatted(propertyName), ex);
    }
    catch (NumberFormatException | TypeMismatchException ex) {
      throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
              "Invalid index in property path '%s'".formatted(propertyName), ex);
    }
    catch (InvocationTargetException ex) {
      throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
              "Getter for property '%s' threw exception".formatted(actualName), ex);
    }
    catch (Exception ex) {
      throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
              "Illegal attempt to get property '%s' threw exception".formatted(actualName), ex);
    }
  }

  /**
   * Return the {@link PropertyHandler} for the specified {@code propertyName}, navigating
   * if necessary. Return {@code null} if not found rather than throwing an exception.
   *
   * @param propertyName the property to obtain the descriptor for
   * @return the property descriptor for the specified property,
   * or {@code null} if not found
   * @throws BeansException in case of introspection failure
   */
  @Nullable
  protected PropertyHandler getPropertyHandler(String propertyName) throws BeansException {
    Assert.notNull(propertyName, "Property name is required");
    AbstractNestablePropertyAccessor nestedPa = getPropertyAccessorForPropertyPath(propertyName);
    return nestedPa.getLocalPropertyHandler(getFinalPath(nestedPa, propertyName));
  }

  /**
   * Return a {@link PropertyHandler} for the specified local {@code propertyName}.
   * Only used to reach a property available in the current context.
   *
   * @param propertyName the name of a local property
   * @return the handler for that property, or {@code null} if it has not been found
   */
  @Nullable
  protected abstract PropertyHandler getLocalPropertyHandler(String propertyName);

  /**
   * Create a new nested property accessor instance.
   * Can be overridden in subclasses to create a PropertyAccessor subclass.
   *
   * @param object the object wrapped by this PropertyAccessor
   * @param nestedPath the nested path of the object
   * @return the nested PropertyAccessor instance
   */
  protected abstract AbstractNestablePropertyAccessor newNestedPropertyAccessor(Object object, String nestedPath);

  /**
   * Create a {@link NotWritablePropertyException} for the specified property.
   */
  protected abstract NotWritablePropertyException createNotWritablePropertyException(String propertyName);

  private Object growArrayIfNecessary(Object array, int index, String name) {
    if (isAutoGrowNestedPaths()) {
      int length = Array.getLength(array);
      if (index >= length && index < autoGrowCollectionLimit) {
        Class<?> componentType = array.getClass().getComponentType();
        Object newArray = Array.newInstance(componentType, index + 1);
        System.arraycopy(array, 0, newArray, 0, length);
        for (int i = length; i < Array.getLength(newArray); i++) {
          Array.set(newArray, i, newValue(componentType, null, name));
        }
        setPropertyValue(name, newArray);
        Object defaultValue = getPropertyValue(name);
        Assert.state(defaultValue != null, "Default value is required");
        return defaultValue;
      }
    }
    return array;
  }

  private void growCollectionIfNecessary(Collection<Object> collection,
          int index, String name, PropertyHandler ph, int nestingLevel) {
    if (isAutoGrowNestedPaths()) {
      int size = collection.size();
      if (index >= size && index < this.autoGrowCollectionLimit) {
        Class<?> elementType = ph.getResolvableType()
                .getNested(nestingLevel)
                .asCollection()
                .resolveGeneric();
        if (elementType != null) {
          for (int i = collection.size(); i < index + 1; i++) {
            collection.add(newValue(elementType, null, name));
          }
        }
      }
    }
  }

  /**
   * Get the last component of the path. Also works if not nested.
   *
   * @param pa property accessor to work on
   * @param nestedPath property path we know is nested
   * @return last component of the path (the property on the target bean)
   */
  protected String getFinalPath(AbstractNestablePropertyAccessor pa, String nestedPath) {
    if (pa == this) {
      return nestedPath;
    }
    return nestedPath.substring(
            PropertyAccessorUtils.getLastNestedPropertySeparatorIndex(nestedPath) + 1);
  }

  /**
   * Recursively navigate to return a property accessor for the nested property path.
   *
   * @param propertyPath property path, which may be nested
   * @return a property accessor for the target bean
   */
  protected AbstractNestablePropertyAccessor getPropertyAccessorForPropertyPath(String propertyPath) {
    int pos = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(propertyPath);
    // Handle nested properties recursively.
    if (pos > -1) {
      String nestedProperty = propertyPath.substring(0, pos);
      String nestedPath = propertyPath.substring(pos + 1);
      AbstractNestablePropertyAccessor nestedPa = getNestedPropertyAccessor(nestedProperty);
      return nestedPa.getPropertyAccessorForPropertyPath(nestedPath);
    }
    else {
      return this;
    }
  }

  /**
   * Retrieve a Property accessor for the given nested property.
   * Create a new one if not found in the cache.
   * <p>Note: Caching nested PropertyAccessors is necessary now,
   * to keep registered custom editors for nested properties.
   *
   * @param nestedProperty property to create the PropertyAccessor for
   * @return the PropertyAccessor instance, either cached or newly created
   */
  private AbstractNestablePropertyAccessor getNestedPropertyAccessor(String nestedProperty) {
    var nestedPropertyAccessors = this.nestedPropertyAccessors;
    if (nestedPropertyAccessors == null) {
      nestedPropertyAccessors = new HashMap<>();
      this.nestedPropertyAccessors = nestedPropertyAccessors;
    }
    // Get value of bean property.
    PropertyTokenHolder tokens = getPropertyNameTokens(nestedProperty);
    String canonicalName = tokens.canonicalName;
    Object value = getPropertyValue(tokens);
    if (value == null || (value instanceof Optional<?> optional && optional.isEmpty())) {
      if (isAutoGrowNestedPaths()) {
        value = setDefaultValue(tokens);
      }
      else {
        throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + canonicalName);
      }
    }

    // Lookup cached sub-PropertyAccessor, create new one if not found.
    var nestedPa = nestedPropertyAccessors.get(canonicalName);
    if (nestedPa == null || nestedPa.getWrappedInstance() != ObjectUtils.unwrapOptional(value)) {
      if (log.isDebugEnabled()) {
        log.trace("Creating new nested {} for property '{}'", getClass().getSimpleName(), canonicalName);
      }
      nestedPa = newNestedPropertyAccessor(value, this.nestedPath + canonicalName + NESTED_PROPERTY_SEPARATOR);
      // Inherit all type-specific PropertyEditors.
      copyDefaultEditorsTo(nestedPa);
      copyCustomEditorsTo(nestedPa, canonicalName);
      nestedPropertyAccessors.put(canonicalName, nestedPa);
    }
    else {
      if (log.isDebugEnabled()) {
        log.trace("Using cached nested property accessor for property '{}'", canonicalName);
      }
    }
    return nestedPa;
  }

  private Object setDefaultValue(PropertyTokenHolder tokens) {
    PropertyValue pv = createDefaultPropertyValue(tokens);
    setPropertyValue(tokens, pv);
    Object defaultValue = getPropertyValue(tokens);
    Assert.state(defaultValue != null, "Default value is required");
    return defaultValue;
  }

  private PropertyValue createDefaultPropertyValue(PropertyTokenHolder tokens) {
    TypeDescriptor desc = getPropertyTypeDescriptor(tokens.canonicalName);
    if (desc == null) {
      throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + tokens.canonicalName,
              "Could not determine property type for auto-growing a default value");
    }
    Object defaultValue = newValue(desc.getType(), desc, tokens.canonicalName);
    return new PropertyValue(tokens.canonicalName, defaultValue);
  }

  private Object newValue(Class<?> type, @Nullable TypeDescriptor desc, String name) {
    try {
      if (type.isArray()) {
        return createArray(type);
      }
      else if (Collection.class.isAssignableFrom(type)) {
        TypeDescriptor elementDesc = desc != null ? desc.getElementDescriptor() : null;
        return CollectionUtils.createCollection(type, (elementDesc != null ? elementDesc.getType() : null), 16);
      }
      else if (Map.class.isAssignableFrom(type)) {
        TypeDescriptor keyDesc = desc != null ? desc.getMapKeyDescriptor() : null;
        return CollectionUtils.createMap(type, (keyDesc != null ? keyDesc.getType() : null), 16);
      }
      else {
        Constructor<?> ctor = type.getDeclaredConstructor();
        if (Modifier.isPrivate(ctor.getModifiers())) {
          throw new IllegalAccessException("Auto-growing not allowed with private constructor: " + ctor);
        }
        return BeanUtils.newInstance(ctor);
      }
    }
    catch (Throwable ex) {
      throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + name,
              "Could not instantiate property type [%s] to auto-grow nested property path".formatted(type.getName()), ex);
    }
  }

  /**
   * Create the array for the given array type.
   *
   * @param arrayType the desired type of the target array
   * @return a new array instance
   */
  private static Object createArray(Class<?> arrayType) {
    Class<?> componentType = arrayType.componentType();
    if (componentType.isArray()) {
      Object array = Array.newInstance(componentType, 1);
      Array.set(array, 0, createArray(componentType));
      return array;
    }
    else {
      return Array.newInstance(componentType, 0);
    }
  }

  /**
   * Parse the given property name into the corresponding property name tokens.
   *
   * @param propertyName the property name to parse
   * @return representation of the parsed property tokens
   */
  private PropertyTokenHolder getPropertyNameTokens(String propertyName) {
    String actualName = null;
    ArrayList<String> keys = new ArrayList<>(2);
    int searchIndex = 0;
    while (searchIndex != -1) {
      int keyStart = propertyName.indexOf(PROPERTY_KEY_PREFIX, searchIndex);
      searchIndex = -1;
      if (keyStart != -1) {
        int keyEnd = getPropertyNameKeyEnd(propertyName, keyStart + PROPERTY_KEY_PREFIX.length());
        if (keyEnd != -1) {
          if (actualName == null) {
            actualName = propertyName.substring(0, keyStart);
          }
          String key = propertyName.substring(keyStart + PROPERTY_KEY_PREFIX.length(), keyEnd);
          if (key.length() > 1 && (key.startsWith("'") && key.endsWith("'")) ||
                  (key.startsWith("\"") && key.endsWith("\""))) {
            key = key.substring(1, key.length() - 1);
          }
          keys.add(key);
          searchIndex = keyEnd + PROPERTY_KEY_SUFFIX.length();
        }
      }
    }
    PropertyTokenHolder tokens = new PropertyTokenHolder(actualName != null ? actualName : propertyName);
    if (!keys.isEmpty()) {
      StringBuilder canonicalName = new StringBuilder(tokens.canonicalName);
      canonicalName.append(PROPERTY_KEY_PREFIX_CHAR);
      canonicalName.append(StringUtils.collectionToDelimitedString(keys, PROPERTY_KEY_SUFFIX + PROPERTY_KEY_PREFIX));
      canonicalName.append(PROPERTY_KEY_SUFFIX_CHAR);
      tokens.canonicalName = canonicalName.toString();
      tokens.keys = StringUtils.toStringArray(keys);
    }
    return tokens;
  }

  private int getPropertyNameKeyEnd(String propertyName, int startIndex) {
    int unclosedPrefixes = 0;
    int length = propertyName.length();
    for (int i = startIndex; i < length; i++) {
      switch (propertyName.charAt(i)) {
        // The property name contains opening prefix(es)...
        case PROPERTY_KEY_PREFIX_CHAR -> unclosedPrefixes++;
        case PROPERTY_KEY_SUFFIX_CHAR -> {
          if (unclosedPrefixes == 0) {
            // No unclosed prefix(es) in the property name (left) ->
            // this is the suffix we are looking for.
            return i;
          }
          else {
            // This suffix does not close the initial prefix but rather
            // just one that occurred within the property name.
            unclosedPrefixes--;
          }
        }
      }
    }
    return -1;
  }

  @Override
  public String toString() {
    String className = getClass().getName();
    if (this.wrappedObject == null) {
      return className + ": no wrapped object set";
    }
    return "%s: wrapping object [%s]".formatted(className, ObjectUtils.identityToString(this.wrappedObject));
  }

  /**
   * A handler for a specific property.
   */
  protected abstract static class PropertyHandler {

    public final Class<?> propertyType;

    public final boolean readable;

    public final boolean writable;

    public PropertyHandler(Class<?> propertyType, boolean readable, boolean writable) {
      this.propertyType = propertyType;
      this.readable = readable;
      this.writable = writable;
    }

    public abstract TypeDescriptor toTypeDescriptor();

    public abstract ResolvableType getResolvableType();

    public TypeDescriptor getMapKeyType(int nestingLevel) {
      return TypeDescriptor.valueOf(getResolvableType().getNested(nestingLevel).asMap().resolveGeneric(0));
    }

    public TypeDescriptor getMapValueType(int nestingLevel) {
      return TypeDescriptor.valueOf(getResolvableType().getNested(nestingLevel).asMap().resolveGeneric(1));
    }

    public TypeDescriptor getCollectionType(int nestingLevel) {
      return TypeDescriptor.valueOf(getResolvableType().getNested(nestingLevel).asCollection().resolveGeneric());
    }

    @Nullable
    public abstract TypeDescriptor nested(int level);

    @Nullable
    public abstract Object getValue() throws Exception;

    public abstract void setValue(@Nullable Object value) throws Exception;
  }

  /**
   * Holder class used to store property tokens.
   */
  protected static class PropertyTokenHolder {

    public PropertyTokenHolder(String name) {
      this.actualName = name;
      this.canonicalName = name;
    }

    public String actualName;

    public String canonicalName;

    @Nullable
    public String[] keys;
  }

}
