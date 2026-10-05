/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans;

import org.jspecify.annotations.Nullable;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.PrivilegedActionException;
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
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.Assert;
import infra.util.CollectionUtils;
import infra.util.ObjectUtils;

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

  protected @Nullable Object wrappedObject;

  protected @Nullable Object rootObject;

  /** Map with cached nested Accessors: path segment -> Accessor instance. */
  private @Nullable HashMap<PropertyPath.Segment, AbstractNestablePropertyAccessor> nestedPropertyAccessors;

  private String nestedPath = "";

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
    setMaxNestedPathDepth(parent.getMaxNestedPathDepth());
    setConversionService(parent.getConversionService());
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
    ResolvedProperty resolved;
    try {
      resolved = resolvePropertyPath(propertyName);
    }
    catch (InvalidPropertyPathException ex) {
      // A malformed path is a syntax error, distinct from a syntactically
      // valid path whose intermediate segment genuinely does not exist
      // (caught below and reported as "not writable" instead).
      throw new InvalidPropertyPathException(getRootInstance(), this.nestedPath + propertyName, value, ex);
    }
    catch (NotReadablePropertyException ex) {
      throw new NotWritablePropertyException(getRootClass(), this.nestedPath + propertyName,
              "Nested property in path '%s' does not exist".formatted(propertyName), ex);
    }
    resolved.accessor.setPropertyValue(resolved.segment, new PropertyValue(propertyName, value));
  }

  @Override
  public void setPropertyValue(PropertyValue pv) throws BeansException {
    if (pv.resolvedTokens instanceof PropertyPath.Segment segment) {
      setPropertyValue(segment, pv);
    }
    else {
      String propertyName = pv.getName();
      ResolvedProperty resolved;
      try {
        resolved = resolvePropertyPath(propertyName);
      }
      catch (InvalidPropertyPathException ex) {
        throw new InvalidPropertyPathException(getRootInstance(), this.nestedPath + propertyName, pv.getValue(), ex);
      }
      catch (NotReadablePropertyException ex) {
        throw new NotWritablePropertyException(getRootClass(), this.nestedPath + propertyName,
                "Nested property in path '%s' does not exist".formatted(propertyName), ex);
      }
      if (resolved.accessor == this) {
        pv.getOriginalPropertyValue().resolvedTokens = resolved.segment;
      }
      resolved.accessor.setPropertyValue(resolved.segment, pv);
    }
  }

  protected void setPropertyValue(PropertyPath.Segment segment, PropertyValue pv) throws BeansException {
    if (!segment.keys().isEmpty()) {
      processKeyedProperty(segment, pv);
    }
    else {
      processLocalProperty(segment, pv);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes", "NullAway" })
  private void processKeyedProperty(PropertyPath.Segment segment, PropertyValue pv) {
    Object propValue = getPropertyHoldingValue(segment);
    PropertyHandler ph = getLocalPropertyHandler(segment.name());
    if (ph == null) {
      throw new InvalidPropertyException(
              getRootClass(), this.nestedPath + segment.name(), "No property handler found");
    }
    List<String> keys = segment.keys();
    String lastKey = keys.get(keys.size() - 1);
    String canonicalName = segment.toCanonicalName();

    if (propValue.getClass().isArray()) {
      Class<?> componentType = propValue.getClass().getComponentType();
      int arrayIndex = Integer.parseInt(lastKey);
      Object oldValue = null;
      try {
        if (isExtractOldValueForEditor() && arrayIndex < Array.getLength(propValue)) {
          oldValue = Array.get(propValue, arrayIndex);
        }
        Object convertedValue = convertIfNecessary(
                canonicalName, oldValue, pv.getValue(), componentType, ph.nested(keys.size()));
        int length = Array.getLength(propValue);
        if (arrayIndex >= length && arrayIndex < autoGrowCollectionLimit) {
          Object newArray = Array.newInstance(componentType, arrayIndex + 1);
          System.arraycopy(propValue, 0, newArray, 0, length);
          String propName = segment.withoutLastKey().toCanonicalName();
          setPropertyValue(propName, newArray);
          propValue = getPropertyValue(propName);
        }
        Array.set(propValue, arrayIndex, convertedValue);
      }
      catch (IndexOutOfBoundsException ex) {
        throw new InvalidPropertyException(getRootClass(), this.nestedPath + canonicalName,
                "Invalid array index in property path '%s'".formatted(canonicalName), ex);
      }
    }
    else if (propValue instanceof List list) {
      TypeDescriptor requiredType = ph.getCollectionType(keys.size());
      int index = Integer.parseInt(lastKey);
      Object oldValue = null;
      if (isExtractOldValueForEditor() && index < list.size()) {
        oldValue = list.get(index);
      }
      Object convertedValue = convertIfNecessary(canonicalName, oldValue, pv.getValue(),
              requiredType.getResolvableType().resolve(), requiredType);
      int size = list.size();
      if (index >= size && index < this.autoGrowCollectionLimit) {
        for (int i = size; i < index; i++) {
          try {
            list.add(null);
          }
          catch (NullPointerException ex) {
            throw new InvalidPropertyException(getRootClass(), this.nestedPath + canonicalName, """
                    Cannot set element with index %s in List of size %s, accessed using property path '%s': \
                    List does not support filling up gaps with null elements""".formatted(index, size, canonicalName));
          }
        }
        list.add(convertedValue);
      }
      else {
        try {
          list.set(index, convertedValue);
        }
        catch (IndexOutOfBoundsException ex) {
          throw new InvalidPropertyException(getRootClass(), this.nestedPath + canonicalName,
                  "Invalid list index in property path '%s'".formatted(canonicalName), ex);
        }
      }
    }
    else if (propValue instanceof Map map) {
      TypeDescriptor mapKeyType = ph.getMapKeyType(keys.size());
      TypeDescriptor mapValueType = ph.getMapValueType(keys.size());
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
      Object convertedMapValue = convertIfNecessary(canonicalName, oldValue, pv.getValue(),
              mapValueType.getResolvableType().resolve(), mapValueType);
      map.put(convertedMapKey, convertedMapValue);
    }
    else {
      throw new InvalidPropertyException(getRootClass(), this.nestedPath + canonicalName,
              "Property referenced in indexed property path '%s' is neither an array nor a List nor a Map; returned value was [%s]"
                      .formatted(canonicalName, propValue));
    }
  }

  private Object getPropertyHoldingValue(PropertyPath.Segment segment) {
    // Apply indexes and map keys: fetch value for all keys but the last one.
    PropertyPath.Segment getterSegment = segment.withoutLastKey();
    String canonicalName = segment.toCanonicalName();

    Object propValue;
    try {
      propValue = getPropertyValue(getterSegment);
    }
    catch (NotReadablePropertyException ex) {
      throw new NotWritablePropertyException(getRootClass(), this.nestedPath + canonicalName,
              "Cannot access indexed value in property referenced in indexed property path '%s'".formatted(canonicalName), ex);
    }

    if (propValue == null) {
      // null map value case
      if (isAutoGrowNestedPaths()) {
        propValue = setDefaultValue(getterSegment);
      }
      else {
        throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + canonicalName,
                "Cannot access indexed value in property referenced in indexed property path '%s': returned null"
                        .formatted(canonicalName));
      }
    }
    return propValue;
  }

  private void processLocalProperty(PropertyPath.Segment segment, PropertyValue pv) {
    // segment.keys() is always empty here (see setPropertyValue(Segment, PropertyValue)
    // above), so the segment's canonical name is always just its raw name.
    String name = segment.name();
    PropertyHandler ph = getLocalPropertyHandler(name);
    if (ph == null || !ph.writable) {
      if (pv.isOptional()) {
        if (log.isDebugEnabled()) {
          log.debug("Ignoring optional value for property '{}' - property not found on bean class [{}]",
                  name, getRootClass().getName());
        }
        return;
      }
      if (this.suppressNotWritablePropertyException) {
        // Optimization for common ignoreUnknown=true scenario since the
        // exception would be caught and swallowed higher up anyway...
        return;
      }
      throw createNotWritablePropertyException(name);
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
                log.debug("Could not read previous value of property '{}{}'", nestedPath, name, ex);
              }
            }
          }
          valueToApply = convertForProperty(name, oldValue, originalValue, ph.toTypeDescriptor());
        }
        pv.getOriginalPropertyValue().conversionNecessary = valueToApply != originalValue;
      }
      ph.setValue(valueToApply);
    }
    catch (TypeMismatchException ex) {
      if (!ph.setValueFallbackIfPossible(pv.getValue())) {
        throw ex;
      }
    }
    catch (InvocationTargetException ex) {
      PropertyChangeEvent event = new PropertyChangeEvent(
              getRootInstance(), this.nestedPath + name, oldValue, pv.getValue());
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
      var pce = new PropertyChangeEvent(getRootInstance(), this.nestedPath + name, oldValue, pv.getValue());
      throw new MethodInvocationException(pce, ex);
    }
  }

  @Override
  public @Nullable Class<?> getPropertyType(String propertyName) throws BeansException {
    if (this.wrappedObject == null) {
      return null;
    }
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
    catch (InvalidPropertyException | InvalidPropertyPathException ex) {
      // Consider as not determinable.
    }
    return null;
  }

  @Override
  public @Nullable TypeDescriptor getPropertyTypeDescriptor(String propertyName) throws BeansException {
    try {
      ResolvedProperty resolved = resolvePropertyPath(propertyName);
      PropertyPath.Segment segment = resolved.segment;
      PropertyHandler handler = resolved.accessor.getLocalPropertyHandler(segment.name());
      if (handler != null) {
        if (!segment.keys().isEmpty()) {
          if (handler.readable || handler.writable) {
            return handler.nested(segment.keys().size());
          }
        }
        else {
          if (handler.readable || handler.writable) {
            return handler.toTypeDescriptor();
          }
        }
      }
    }
    catch (InvalidPropertyException | InvalidPropertyPathException ex) {
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
    catch (InvalidPropertyException | InvalidPropertyPathException ex) {
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
    catch (InvalidPropertyException | InvalidPropertyPathException ex) {
      // Cannot be evaluated, so can't be writable.
    }
    return false;
  }

  private @Nullable Object convertIfNecessary(@Nullable String propertyName, @Nullable Object oldValue,
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

  protected @Nullable Object convertForProperty(String propertyName, @Nullable Object oldValue,
          @Nullable Object newValue, TypeDescriptor td) throws TypeMismatchException {

    return convertIfNecessary(propertyName, oldValue, newValue, td.getType(), td);
  }

  @Override
  public @Nullable Object getPropertyValue(String propertyName) throws BeansException {
    ResolvedProperty resolved = resolvePropertyPath(propertyName);
    return resolved.accessor.getPropertyValue(resolved.segment);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected @Nullable Object getPropertyValue(PropertyPath.Segment segment) throws BeansException {
    String actualName = segment.name();
    String propertyName = segment.toCanonicalName();
    PropertyHandler ph = getLocalPropertyHandler(actualName);
    if (ph == null || !ph.readable) {
      throw new NotReadablePropertyException(getRootClass(), this.nestedPath + propertyName);
    }
    try {
      Object value = ph.getValue();
      List<String> keys = segment.keys();
      if (!keys.isEmpty()) {
        if (value == null) {
          if (isAutoGrowNestedPaths()) {
            value = setDefaultValue(new PropertyPath.Segment(actualName, List.of()));
          }
          else {
            throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + propertyName,
                    "Cannot access indexed value of property referenced in indexed property path '%s': returned null".formatted(propertyName));
          }
        }
        StringBuilder indexedPropertyName = new StringBuilder(actualName);
        // apply indexes and map keys
        for (int i = 0; i < keys.size(); i++) {
          String key = keys.get(i);
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
            growCollectionIfNecessary(list, index, indexedPropertyName.toString(), ph, i + 1);
            if (index < 0 || index >= list.size()) {
              throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
                      "Cannot get element with index %d from List of size %d, accessed using property path '%s'"
                              .formatted(index, list.size(), propertyName));
            }
            value = list.get(index);
          }
          else if (value instanceof Map map) {
            Class<?> mapKeyType = ph.getResolvableType().getNested(i + 1).asMap().resolveGeneric(0);
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
                        "Cannot get element with index %d from Collection of size %d, accessed using property path '%s'"
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
                      "Cannot get element with index %d from Iterable of size %d, accessed using property path '%s'"
                              .formatted(index, currIndex, propertyName));
            }
          }
          else {
            throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
                    "Property referenced in indexed property path '%s' is neither an array nor a List/Set/Collection/Iterable nor a Map; returned value was [%s]"
                            .formatted(propertyName, value));
          }
          indexedPropertyName.append(PROPERTY_KEY_PREFIX).append(key).append(PROPERTY_KEY_SUFFIX);
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
  protected @Nullable PropertyHandler getPropertyHandler(String propertyName) throws BeansException {
    Assert.notNull(propertyName, "Property name is required");
    ResolvedProperty resolved = resolvePropertyPath(propertyName);
    return resolved.accessor.getLocalPropertyHandler(resolved.segment.toCanonicalName());
  }

  /**
   * Return a {@link PropertyHandler} for the specified local {@code propertyName}.
   * Only used to reach a property available in the current context.
   *
   * @param propertyName the name of a local property
   * @return the handler for that property, or {@code null} if it has not been found
   */
  protected abstract @Nullable PropertyHandler getLocalPropertyHandler(String propertyName);

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
   * Resolve a property path to the accessor owning its final segment and
   * that segment itself, parsing the path exactly once.
   *
   * @param propertyPath the property path, which may be nested
   * @return the accessor for the target bean, paired with the final segment
   * @throws InvalidPropertyPathException if the path is malformed or its nesting
   * depth exceeds {@link #getMaxNestedPathDepth()}
   * @since 5.0
   */
  protected ResolvedProperty resolvePropertyPath(String propertyPath) {
    PropertyPath.Options options = PropertyPath.Options.withMaxNestedPathDepth(getMaxNestedPathDepth());
    List<PropertyPath.Segment> segments = PropertyPath.parse(propertyPath, options).segments();
    AbstractNestablePropertyAccessor accessor = getPropertyAccessorForSegments(segments, 0);
    return new ResolvedProperty(accessor, finalSegment(propertyPath, segments));
  }

  /**
   * Return the final segment of an already parsed property path. An empty path
   * has no parsed segments and is treated as a property with an empty name.
   */
  private static PropertyPath.Segment finalSegment(String propertyPath, List<PropertyPath.Segment> segments) {
    return segments.isEmpty() ? new PropertyPath.Segment(propertyPath, List.of()) : segments.get(segments.size() - 1);
  }

  /**
   * Navigate the already parsed intermediate segments to the accessor that
   * owns the final property.
   */
  private AbstractNestablePropertyAccessor getPropertyAccessorForSegments(List<PropertyPath.Segment> segments, int fromIndex) {
    if (segments.isEmpty() || fromIndex >= segments.size() - 1) {
      return this;
    }
    AbstractNestablePropertyAccessor nestedPa = getNestedPropertyAccessor(segments.get(fromIndex));
    return nestedPa.getPropertyAccessorForSegments(segments, fromIndex + 1);
  }

  /**
   * Retrieve a Property accessor for the given nested property.
   * Create a new one if not found in the cache.
   * <p>Note: Caching nested PropertyAccessors is necessary now,
   * to keep registered custom editors for nested properties.
   *
   * @param segment the already parsed segment to create the PropertyAccessor for
   * @return the PropertyAccessor instance, either cached or newly created
   */
  private AbstractNestablePropertyAccessor getNestedPropertyAccessor(PropertyPath.Segment segment) {
    var nestedAccessors = this.nestedPropertyAccessors;
    if (nestedAccessors == null) {
      nestedAccessors = new HashMap<>();
      this.nestedPropertyAccessors = nestedAccessors;
    }
    // Get value of bean property.
    Object value = getPropertyValue(segment);
    if (value == null || (value instanceof Optional<?> optional && optional.isEmpty())) {
      if (isAutoGrowNestedPaths()) {
        value = setDefaultValue(segment);
      }
      else {
        throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + segment.toCanonicalName());
      }
    }

    // Lookup cached sub-PropertyAccessor, create new one if not found.
    var nestedPa = nestedAccessors.get(segment);
    if (nestedPa == null || nestedPa.getWrappedInstance() != ObjectUtils.unwrapOptional(value)) {
      String canonicalName = segment.toCanonicalName();
      if (log.isDebugEnabled()) {
        log.trace("Creating new nested {} for property '{}'", getClass().getSimpleName(), canonicalName);
      }
      nestedPa = newNestedPropertyAccessor(value, this.nestedPath + canonicalName + NESTED_PROPERTY_SEPARATOR);
      // Inherit all type-specific PropertyEditors.
      copyDefaultEditorsTo(nestedPa);
      copyCustomEditorsTo(nestedPa, canonicalName);
      nestedAccessors.put(segment, nestedPa);
    }
    else {
      if (log.isDebugEnabled()) {
        log.trace("Using cached nested property accessor for property '{}'", segment.toCanonicalName());
      }
    }
    return nestedPa;
  }

  private Object setDefaultValue(PropertyPath.Segment segment) {
    PropertyValue pv = createDefaultPropertyValue(segment);
    setPropertyValue(segment, pv);
    Object defaultValue = getPropertyValue(segment);
    Assert.state(defaultValue != null, "Default value is required");
    return defaultValue;
  }

  private PropertyValue createDefaultPropertyValue(PropertyPath.Segment segment) {
    String canonicalName = segment.toCanonicalName();
    TypeDescriptor desc = getPropertyTypeDescriptor(canonicalName);
    if (desc == null) {
      throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + canonicalName,
              "Could not determine property type for auto-growing a default value");
    }
    Object defaultValue = newValue(desc.getType(), desc, canonicalName);
    return new PropertyValue(canonicalName, defaultValue);
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

    public abstract @Nullable TypeDescriptor nested(int level);

    public abstract @Nullable Object getValue() throws Exception;

    public abstract void setValue(@Nullable Object value) throws Exception;

    /**
     * Try an alternative write method after conversion to the primary property's
     * type failed. The default implementation does not provide a fallback.
     *
     * @param value the original value to write
     * @return whether the fallback successfully wrote the value
     * @since 5.0
     */
    public boolean setValueFallbackIfPossible(@Nullable Object value) {
      return false;
    }
  }

  /**
   * The accessor that owns the final property and its parsed segment.
   *
   * @since 5.0
   */
  protected static class ResolvedProperty {

    public final AbstractNestablePropertyAccessor accessor;

    public final PropertyPath.Segment segment;

    /**
     * @param accessor the accessor for the target bean
     * @param segment the final segment of the resolved path
     */
    protected ResolvedProperty(AbstractNestablePropertyAccessor accessor, PropertyPath.Segment segment) {
      this.accessor = accessor;
      this.segment = segment;
    }

  }

}
