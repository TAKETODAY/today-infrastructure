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

import java.util.List;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.lang.Nullable;

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
 * <p><b>NOTE: this is - for almost all purposes - an
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
 * @since 4.0 2022/2/17 17:40
 */
public class BeanWrapperImpl extends AbstractNestablePropertyAccessor implements BeanWrapper {

  @Nullable
  private BeanMetadata beanMetadata;

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
   * Create a new BeanWrapperImpl for the given object.
   *
   * @param object the object wrapped by this BeanWrapper
   * @param beanMetadata the object beanMetadata
   */
  public BeanWrapperImpl(Object object, @Nullable BeanMetadata beanMetadata) {
    super(object);
    registerDefaultEditors();
    this.beanMetadata = beanMetadata;
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
   */
  public void setBeanInstance(Object object) {
    this.rootObject = object;
    this.wrappedObject = object;
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
    BeanMetadata beanMetadata = this.beanMetadata;
    if (beanMetadata != null && beanMetadata.getType() != clazz) {
      this.beanMetadata = null;
    }
  }

  /**
   * Obtain a lazily initialized BeanMetadata instance
   * for the wrapped object.
   */
  @Override
  public BeanMetadata getMetadata() {
    BeanMetadata beanMetadata = this.beanMetadata;
    if (beanMetadata == null) {
      beanMetadata = BeanMetadata.from(getWrappedClass());
      this.beanMetadata = beanMetadata;
    }
    return beanMetadata;
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
    BeanProperty beanProperty = getMetadata().getBeanProperty(propertyName);
    if (beanProperty == null) {
      throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName,
              "No property '" + propertyName + "' found");
    }
    TypeDescriptor typeDescriptor = beanProperty.getTypeDescriptor();
    return convertForProperty(propertyName, null, value, typeDescriptor);
  }

  @Override
  @Nullable
  protected BeanPropertyHandler getLocalPropertyHandler(String propertyName) {
    BeanProperty beanProperty = getMetadata().getBeanProperty(propertyName);
    return beanProperty != null ? new BeanPropertyHandler(beanProperty) : null;
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
  public List<BeanProperty> getBeanProperties() {
    return getMetadata().beanProperties();
  }

  @Override
  public BeanProperty getBeanProperty(String propertyName) throws InvalidPropertyException {
    BeanWrapperImpl nestedBw = (BeanWrapperImpl) getPropertyAccessorForPropertyPath(propertyName);
    String finalPath = getFinalPath(nestedBw, propertyName);
    BeanProperty property = nestedBw.getMetadata().getBeanProperty(finalPath);
    if (property == null) {
      throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName,
              "No property '" + propertyName + "' found");
    }
    return property;
  }

  private class BeanPropertyHandler extends PropertyHandler {

    private final BeanProperty property;

    public BeanPropertyHandler(BeanProperty property) {
      super(property.getType(), property.isReadable(), property.isWriteable());
      this.property = property;
    }

    @Override
    public ResolvableType getResolvableType() {
      return property.getResolvableType();
    }

    @Override
    public TypeDescriptor toTypeDescriptor() {
      return property.getTypeDescriptor();
    }

    @Override
    @Nullable
    public TypeDescriptor nested(int level) {
      return TypeDescriptor.nested(property.getTypeDescriptor(), level);
    }

    @Override
    @Nullable
    public Object getValue() throws Exception {
      return property.getValue(getWrappedInstance());
    }

    @Override
    public void setValue(@Nullable Object value) throws Exception {
      property.setDirectly(getWrappedInstance(), value);
    }
  }

}
