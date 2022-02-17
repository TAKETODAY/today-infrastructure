/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * Default {@link BeanWrapper} implementation that should be sufficient
 * for all typical use cases. Caches introspection results for efficiency.
 *
 * <p>Note: Auto-registers default property editors from the
 * {@code cn.taketoday.beans.propertyeditors} package, which apply
 * in addition to the JDK's standard PropertyEditors. Applications can call
 * the {@link #registerCustomEditor(Class, java.beans.PropertyEditor)} method
 * to register an editor for a particular instance (i.e. they are not shared
 * across the application). See the base class
 * {@link PropertyEditorRegistrySupport} for details.
 *
 * <p><b>NOTE: As of Spring 2.5, this is - for almost all purposes - an
 * internal class.</b> It is just public in order to allow for access from
 * other framework packages. For standard application access purposes, use the
 * {@link PropertyAccessorFactory#forBeanPropertyAccess} factory method instead.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #registerCustomEditor
 * @see #setPropertyValues
 * @see #setPropertyValue
 * @see #getPropertyValue
 * @see #getPropertyType
 * @see BeanWrapper
 * @see PropertyEditorRegistrySupport
 * @since 15 April 2001
 * @since 4.0 2022/2/17 17:40
 */
public class BeanWrapperImpl extends AbstractNestablePropertyAccessor implements BeanWrapper {

  /**
   * Cached introspections results for this object, to prevent encountering
   * the cost of JavaBeans introspection every time.
   */
  @Nullable
  private CachedIntrospectionResults cachedIntrospectionResults;

  /**
   * Create a new empty BeanWrapperImpl. Wrapped instance needs to be set afterwards.
   * Registers default editors.
   *
   * @see #setWrappedInstance
   */
  public BeanWrapperImpl() {
    this(true);
  }

  /**
   * Create a new empty BeanWrapperImpl. Wrapped instance needs to be set afterwards.
   *
   * @param registerDefaultEditors whether to register default editors
   * (can be suppressed if the BeanWrapper won't need any type conversion)
   * @see #setWrappedInstance
   */
  public BeanWrapperImpl(boolean registerDefaultEditors) {
    super(registerDefaultEditors);
  }

  /**
   * Create a new BeanWrapperImpl for the given object.
   *
   * @param object the object wrapped by this BeanWrapper
   */
  public BeanWrapperImpl(Object object) {
    super(object);
  }

  /**
   * Create a new BeanWrapperImpl, wrapping a new instance of the specified class.
   *
   * @param clazz class to instantiate and wrap
   */
  public BeanWrapperImpl(Class<?> clazz) {
    super(clazz);
  }

  /**
   * Create a new BeanWrapperImpl for the given object,
   * registering a nested path that the object is in.
   *
   * @param object the object wrapped by this BeanWrapper
   * @param nestedPath the nested path of the object
   * @param rootObject the root object at the top of the path
   */
  public BeanWrapperImpl(Object object, String nestedPath, Object rootObject) {
    super(object, nestedPath, rootObject);
  }

  /**
   * Create a new BeanWrapperImpl for the given object,
   * registering a nested path that the object is in.
   *
   * @param object the object wrapped by this BeanWrapper
   * @param nestedPath the nested path of the object
   * @param parent the containing BeanWrapper (must not be {@code null})
   */
  private BeanWrapperImpl(Object object, String nestedPath, BeanWrapperImpl parent) {
    super(object, nestedPath, parent);
  }

  /**
   * Set a bean instance to hold, without any unwrapping of {@link java.util.Optional}.
   *
   * @param object the actual target object
   * @see #setWrappedInstance(Object)
   * @since 4.3
   */
  public void setBeanInstance(Object object) {
    this.wrappedObject = object;
    this.rootObject = object;
    this.typeConverterDelegate = new TypeConverterDelegate(this, this.wrappedObject);
    setIntrospectionClass(object.getClass());
  }

  @Override
  public void setWrappedInstance(Object object, @Nullable String nestedPath, @Nullable Object rootObject) {
    super.setWrappedInstance(object, nestedPath, rootObject);
    setIntrospectionClass(getWrappedClass());
  }

  /**
   * Set the class to introspect.
   * Needs to be called when the target object changes.
   *
   * @param clazz the class to introspect
   */
  protected void setIntrospectionClass(Class<?> clazz) {
    if (this.cachedIntrospectionResults != null && this.cachedIntrospectionResults.getBeanClass() != clazz) {
      this.cachedIntrospectionResults = null;
    }
  }

  /**
   * Obtain a lazily initialized CachedIntrospectionResults instance
   * for the wrapped object.
   */
  private CachedIntrospectionResults getCachedIntrospectionResults() {
    if (this.cachedIntrospectionResults == null) {
      this.cachedIntrospectionResults = CachedIntrospectionResults.forClass(getWrappedClass());
    }
    return this.cachedIntrospectionResults;
  }

  /**
   * Convert the given value for the specified property to the latter's type.
   * <p>This method is only intended for optimizations in a BeanFactory.
   * Use the {@code convertIfNecessary} methods for programmatic conversion.
   *
   * @param value the value to convert
   * @param propertyName the target property
   * (note that nested or indexed properties are not supported here)
   * @return the new value, possibly the result of type conversion
   * @throws TypeMismatchException if type conversion failed
   */
  @Nullable
  public Object convertForProperty(@Nullable Object value, String propertyName) throws TypeMismatchException {
    CachedIntrospectionResults cachedIntrospectionResults = getCachedIntrospectionResults();
    PropertyDescriptor pd = cachedIntrospectionResults.getPropertyDescriptor(propertyName);
    if (pd == null) {
      throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName,
              "No property '" + propertyName + "' found");
    }
    TypeDescriptor td = cachedIntrospectionResults.getTypeDescriptor(pd);
    if (td == null) {
      td = cachedIntrospectionResults.addTypeDescriptor(pd, new TypeDescriptor(property(pd)));
    }
    return convertForProperty(propertyName, null, value, td);
  }

  private BeanProperty property(PropertyDescriptor pd) {
    return BeanProperty.valueOf(pd.getName(), pd.getReadMethod(), pd.getWriteMethod(), pd.getPropertyType());
  }

  @Override
  @Nullable
  protected BeanPropertyHandler getLocalPropertyHandler(String propertyName) {
    PropertyDescriptor pd = getCachedIntrospectionResults().getPropertyDescriptor(propertyName);
    return (pd != null ? new BeanPropertyHandler(pd) : null);
  }

  @Override
  protected BeanWrapperImpl newNestedPropertyAccessor(Object object, String nestedPath) {
    return new BeanWrapperImpl(object, nestedPath, this);
  }

  @Override
  protected NotWritablePropertyException createNotWritablePropertyException(String propertyName) {
    PropertyMatches matches = PropertyMatches.forProperty(propertyName, getRootClass());
    throw new NotWritablePropertyException(getRootClass(), getNestedPath() + propertyName,
            matches.buildErrorMessage(), matches.getPossibleMatches());
  }

  @Override
  public PropertyDescriptor[] getPropertyDescriptors() {
    return getCachedIntrospectionResults().getPropertyDescriptors();
  }

  @Override
  public PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException {
    BeanWrapperImpl nestedBw = (BeanWrapperImpl) getPropertyAccessorForPropertyPath(propertyName);
    String finalPath = getFinalPath(nestedBw, propertyName);
    PropertyDescriptor pd = nestedBw.getCachedIntrospectionResults().getPropertyDescriptor(finalPath);
    if (pd == null) {
      throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName,
              "No property '" + propertyName + "' found");
    }
    return pd;
  }

  private class BeanPropertyHandler extends PropertyHandler {

    private final PropertyDescriptor pd;

    public BeanPropertyHandler(PropertyDescriptor pd) {
      super(pd.getPropertyType(), pd.getReadMethod() != null, pd.getWriteMethod() != null);
      this.pd = pd;
    }

    @Override
    public ResolvableType getResolvableType() {
      return ResolvableType.forReturnType(this.pd.getReadMethod());
    }

    @Override
    public TypeDescriptor toTypeDescriptor() {
      return new TypeDescriptor(property(this.pd));
    }

    @Override
    @Nullable
    public TypeDescriptor nested(int level) {
      return TypeDescriptor.nested(new TypeDescriptor(property(this.pd)), level);
    }

    @Override
    @Nullable
    public Object getValue() throws Exception {
      Method readMethod = this.pd.getReadMethod();
      ReflectionUtils.makeAccessible(readMethod);
      return readMethod.invoke(getWrappedInstance(), (Object[]) null);
    }

    @Override
    public void setValue(@Nullable Object value) throws Exception {
      Method writeMethod = this.pd.getWriteMethod();
      ReflectionUtils.makeAccessible(writeMethod);
      writeMethod.invoke(getWrappedInstance(), value);
    }
  }

}
