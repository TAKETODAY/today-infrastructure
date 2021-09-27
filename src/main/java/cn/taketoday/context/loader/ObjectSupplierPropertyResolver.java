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

package cn.taketoday.context.loader;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Supplier;

import cn.taketoday.beans.PropertyValueException;
import cn.taketoday.beans.factory.AbstractBeanFactory;
import cn.taketoday.beans.factory.AbstractPropertySetter;
import cn.taketoday.beans.factory.ObjectSupplier;
import cn.taketoday.beans.factory.PropertySetter;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.util.ResolvableType;

/**
 * for {@link ObjectSupplier} PropertyValueResolver
 *
 * @author TODAY 2021/3/6 12:10
 * @since 3.0
 */
public class ObjectSupplierPropertyResolver
        extends OrderedSupport implements PropertyValueResolver {

  public ObjectSupplierPropertyResolver() {
    this(Ordered.HIGHEST_PRECEDENCE);
  }

  public ObjectSupplierPropertyResolver(int order) {
    super(order);
  }

  @Override
  public boolean supportsProperty(Field field) {
    return (field.getType() == Supplier.class
            || field.getType() == ObjectSupplier.class)
            && AutowiredPropertyResolver.isInjectable(field);
  }

  @Override
  public PropertySetter resolveProperty(final Field field) {
    final ResolvableType resolvableType = ResolvableType.fromField(field);
    if (resolvableType.hasGenerics()) {
      final ResolvableType generic = resolvableType.getGeneric(0);
      final Class<?> aClass = generic.toClass();
      return new ObjectSupplierPropertySetter(field, aClass);
    }
    // Usage error
    throw new PropertyValueException("Unsupported '" + field + "' In -> " + field.getDeclaringClass());
  }

  /**
   * {@link ObjectSupplier} property value
   *
   * @since 3.0
   */
  static class ObjectSupplierPropertySetter
          extends AbstractPropertySetter implements PropertySetter {

    final Class<?> target;

    public ObjectSupplierPropertySetter(Field field, Class<?> target) {
      super(field);
      this.target = target;
    }

    @Override
    protected Object resolveValue(AbstractBeanFactory beanFactory) {
      return beanFactory.getBeanSupplier(target);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof ObjectSupplierPropertySetter))
        return false;
      if (!super.equals(o))
        return false;
      final ObjectSupplierPropertySetter that = (ObjectSupplierPropertySetter) o;
      return Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), target);
    }

    @Override
    public String toString() {
      return "ObjectSupplierPropertyValue{" +
              "target=" + target +
              '}';
    }
  }

}
