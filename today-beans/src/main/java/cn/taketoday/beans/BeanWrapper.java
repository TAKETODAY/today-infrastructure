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

/**
 * The central interface of Framework's low-level JavaBeans infrastructure.
 *
 * <p>Typically not used directly but rather implicitly via a
 * {@link cn.taketoday.beans.factory.BeanFactory} or a
 * {@link cn.taketoday.validation.DataBinder}.
 *
 * <p>Provides operations to analyze and manipulate standard JavaBeans:
 * the ability to get and set property values (individually or in bulk),
 * get property descriptors, and query the readability/writability of properties.
 *
 * <p>This interface supports <b>nested properties</b> enabling the setting
 * of properties on sub-properties to an unlimited depth.
 *
 * <p>A BeanWrapper's default for the "extractOldValueForEditor" setting
 * is "false", to avoid side effects caused by getter method invocations.
 * Turn this to "true" to expose present property values to custom editors.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertyAccessor
 * @see PropertyEditorRegistry
 * @see BeanWrapper#forBeanPropertyAccess
 * @see cn.taketoday.beans.factory.BeanFactory
 * @see cn.taketoday.validation.BeanPropertyBindingResult
 * @see cn.taketoday.validation.DataBinder#initBeanPropertyAccess()
 * @since 4.0 2022/2/17 17:37
 */
public interface BeanWrapper extends ConfigurablePropertyAccessor {

  /**
   * Specify a limit for array and collection auto-growing.
   * <p>Default is unlimited on a plain BeanWrapper.
   */
  void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);

  /**
   * Return the limit for array and collection auto-growing.
   */
  int getAutoGrowCollectionLimit();

  /**
   * Return the bean instance wrapped by this object.
   */
  Object getWrappedInstance();

  /**
   * Return the type of the wrapped bean instance.
   */
  Class<?> getWrappedClass();

  /**
   * Obtain the BeanProperty for the wrapped object
   *
   * @return the BeanProperty for the wrapped object
   */
  List<BeanProperty> getBeanProperties();

  /**
   * Obtain the BeanProperty for a specific property
   * of the wrapped object.
   *
   * @param propertyName the property to obtain the descriptor for
   * (may be a nested path, but no indexed/mapped property)
   * @return the property descriptor for the specified property
   * @throws InvalidPropertyException if there is no such property
   */
  BeanProperty getBeanProperty(String propertyName) throws InvalidPropertyException;

  /**
   * Obtain BeanMetadata
   */
  BeanMetadata getMetadata();

  // Static factory methods

  /**
   * Obtain a BeanWrapper for the given target object type,
   * accessing properties in JavaBeans style.
   *
   * @param objectType the target object type
   * @return the property accessor
   * @see BeanWrapperImpl
   */
  static BeanWrapper forBeanPropertyAccess(Class<?> objectType) {
    return new BeanWrapperImpl(objectType);
  }

  /**
   * Obtain a BeanWrapper for the given target object,
   * accessing properties in JavaBeans style.
   *
   * @param target the target object to wrap
   * @return the property accessor
   * @see BeanWrapperImpl
   */
  static BeanWrapper forBeanPropertyAccess(Object target) {
    return new BeanWrapperImpl(target);
  }

  /**
   * Obtain a PropertyAccessor for the given target object,
   * accessing properties in direct field style.
   *
   * @param target the target object to wrap
   * @return the property accessor
   * @see DirectFieldAccessor
   */
  static DirectFieldAccessor forDirectFieldAccess(Object target) {
    return new DirectFieldAccessor(target);
  }

}
