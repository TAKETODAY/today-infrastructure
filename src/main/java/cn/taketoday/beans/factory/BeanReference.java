/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.beans.factory;

import java.util.Objects;

import cn.taketoday.beans.support.PropertyValuesBinder;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/27 21:02</a>
 * @since 4.0
 */
public class BeanReference implements PropertyValueRetriever {

  @Nullable
  private String beanName;

  @Nullable
  private Class<?> beanType;

  private boolean required = true;

  /**
   * use propertyPath when beanName is not available
   */
  private boolean usePropertyName = true;

  /**
   * {@inheritDoc}
   *
   * <p>
   * retrieve a bean reference like  ref='bean'
   * </p>
   */
  @Override
  public Object retrieve(
          String propertyPath, PropertyValuesBinder binder, AutowireCapableBeanFactory beanFactory) {
    if (beanName != null && beanType != null) {
      return getObject(beanFactory, beanName, beanType);
    }
    if (beanName != null) {
      Class<?> propertyClass = binder.obtainMetadata().getPropertyClass(propertyPath);
      return getObject(beanFactory, beanName, propertyClass);
    }
    if (beanType != null) {
      if (usePropertyName) {
        return getObject(beanFactory, propertyPath, beanType);
      }
      return beanFactory.getBean(beanType);
    }

    if (usePropertyName) {
      Class<?> propertyClass = binder.obtainMetadata().getPropertyClass(propertyPath);
      return getObject(beanFactory, propertyPath, propertyClass);
    }

    throw new IllegalStateException("beanName and beanType cannot be null at same time");
  }

  @NonNull
  private Object getObject(BeanFactory beanFactory, String beanName, Class<?> beanType) {
    Object bean = beanFactory.getBean(beanName, beanType);
    if (bean == null) {
      if (required) {
        throw new NoSuchBeanDefinitionException(beanName, beanType);
      }
      // do not set; use default value
      return DO_NOT_SET;
    }
    return bean;
  }

  /**
   * use propertyPath when beanName is not available
   */
  public void setUsePropertyName(boolean usePropertyName) {
    this.usePropertyName = usePropertyName;
  }

  /**
   * use propertyPath when beanName is not available
   */
  public boolean isUsePropertyName() {
    return usePropertyName;
  }

  public void setBeanType(@Nullable Class<?> beanType) {
    this.beanType = beanType;
  }

  @Nullable
  public Class<?> getBeanType() {
    return beanType;
  }

  public void setBeanName(@Nullable String beanName) {
    this.beanName = beanName;
  }

  @Nullable
  public String getBeanName() {
    return beanName;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public boolean isRequired() {
    return required;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof BeanReference that))
      return false;
    return Objects.equals(beanName, that.beanName)
            && Objects.equals(beanType, that.beanType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(beanName, beanType);
  }

  // static

  public static BeanReference required(String beanName) {
    return required(beanName, null);
  }

  public static BeanReference required(Class<?> beanType) {
    return required(null, beanType, false);
  }

  public static BeanReference required(Class<?> beanType, boolean usePropertyName) {
    return required(null, beanType, usePropertyName);
  }

  public static BeanReference required(String beanName, Class<?> beanType) {
    return required(beanName, beanType, false);
  }

  public static BeanReference required(String beanName, Class<?> beanType, boolean usePropertyName) {
    BeanReference beanReference = new BeanReference();
    beanReference.setRequired(true);
    beanReference.setBeanName(beanName);
    beanReference.setBeanType(beanType);
    beanReference.setUsePropertyName(usePropertyName);
    return beanReference;
  }

  /**
   * by name
   */
  public static BeanReference required() {
    return required(null, null, true);
  }

  public static BeanReference from(String beanName) {
    return from(beanName, null);
  }

  public static BeanReference from(String beanName, @Nullable Class<?> beanType) {
    BeanReference beanReference = new BeanReference();
    beanReference.setBeanName(beanName);
    beanReference.setBeanType(beanType);
    beanReference.setRequired(false);
    return beanReference;
  }

  public static BeanReference from(Class<?> beanType) {
    return from(beanType, true);
  }

  public static BeanReference from(Class<?> beanType, boolean usePropertyName) {
    BeanReference beanReference = new BeanReference();
    beanReference.setRequired(false);
    beanReference.setBeanType(beanType);
    beanReference.setUsePropertyName(usePropertyName);
    return beanReference;
  }

}
