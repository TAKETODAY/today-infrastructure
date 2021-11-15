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

import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.core.style.ToStringBuilder;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * <p>
 * Subclasses override {@link #resolveValue(ConfigurableBeanFactory)}
 * to resolve different Property
 * </p>
 *
 * @author TODAY 2021/3/6 14:50
 * @since 3.0
 */
public abstract class AbstractDependencySetter implements DependencySetter {
  /**
   * It shows that the value is not set
   */
  public static final Object DO_NOT_SET = new Object();

  /** field info */
  protected final BeanProperty property;

  public AbstractDependencySetter(Field field) {
    this(BeanProperty.valueOf(field));
  }

  public AbstractDependencySetter(BeanProperty property) {
    this.property = property;
  }

  /**
   * set value to property
   * <p>
   * If property value is {@link #DO_NOT_SET} will not set value
   * </p>
   *
   * @param bean property's bean
   * @param beanFactory current AbstractBeanFactory
   */
  @Override
  public void applyTo(Object bean, ConfigurableBeanFactory beanFactory) {
    final Object property = resolveValue(beanFactory);
    if (property != DO_NOT_SET) {
      doSetValue(bean, property);
    }
  }

  public void doSetValue(Object bean, Object value) {
    property.setValue(bean, value);
  }

  /**
   * resolve property value
   *
   * @param beanFactory AbstractBeanFactory
   * @return property value
   * @see #DO_NOT_SET
   */
  protected abstract Object resolveValue(ConfigurableBeanFactory beanFactory);

  public String getName() {
    return property.getName();
  }

  public Field getField() {
    return property.getField();
  }

  public BeanProperty getProperty() {
    return property;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o instanceof final AbstractDependencySetter that)
      return Objects.equals(property, that.property);
    return false;
  }

  @Override
  public int hashCode() {
    return property.hashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("property", property.getName())
            .append("propertyClass", property.getType())
            .append("beanClass", property.getDeclaringClass())
            .toString();

  }
}
