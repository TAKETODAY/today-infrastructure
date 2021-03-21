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

import java.lang.reflect.Field;
import java.util.Objects;

import cn.taketoday.context.reflect.SetterMethod;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ReflectionUtils;

/**
 * <p>
 * Subclasses override {@link #resolveValue(AbstractBeanFactory)}
 * to resolve different Property
 * </p>
 *
 * @author TODAY 2021/3/6 14:50
 * @since 3.0
 */
public abstract class AbstractPropertySetter implements PropertySetter {

  /** field info */
  final Field field;
  /** @since 3.0 */
  final SetterMethod accessor;

  public AbstractPropertySetter(Field field) {
    Assert.notNull(field, "field must not be null");
    this.field = field;
    this.accessor = ReflectionUtils.newSetterMethod(field);
  }

  public void doSetValue(Object bean, Object value) {
    accessor.set(bean, value);
  }

  @Override
  public void applyValue(Object bean, AbstractBeanFactory beanFactory) {
    final Object property = resolveValue(beanFactory);
    if (property != DO_NOT_SET) {
      doSetValue(bean, property);
    }
  }

  /**
   * resolve property value
   *
   * @param beanFactory
   *         AbstractBeanFactory
   *
   * @return property value
   *
   * @see #DO_NOT_SET
   */
  protected abstract Object resolveValue(AbstractBeanFactory beanFactory);

  public String getName() {
    return field.getName();
  }

  public Field getField() {
    return field;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AbstractPropertySetter)) return false;
    final AbstractPropertySetter that = (AbstractPropertySetter) o;
    return Objects.equals(field, that.field);
  }

  @Override
  public int hashCode() {
    return field.hashCode();
  }

  @Override
  public String toString() {
    return new StringBuilder()
            .append("{\"property\":\"").append(field.getName())
            .append("\",\"propertyClass\":\"").append(field.getType())
            .append("\",\"beanClass:\":\"").append(field.getDeclaringClass())
            .append("\"}")
            .toString();
  }
}
