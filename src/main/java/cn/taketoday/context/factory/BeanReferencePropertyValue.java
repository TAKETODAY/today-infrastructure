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

import cn.taketoday.context.exception.NoSuchBeanDefinitionException;

/**
 * @author TODAY 2021/3/6 15:18
 */
public class BeanReferencePropertyValue extends AbstractPropertyValue {

  final BeanReference reference;

  public BeanReferencePropertyValue(BeanReference value, Field field) {
    super(field);
    this.reference = value;
  }

  protected Object resolveValue(AbstractBeanFactory beanFactory) {
    // reference bean
    final BeanReference reference = this.reference;
    // fix: same name of bean
    final Object value = resolveBeanReference(beanFactory, reference);
    if (value == null) {
      if (reference.isRequired()) {
        throw new NoSuchBeanDefinitionException(reference.getName(), reference.getReferenceClass());
      }
      return DO_NOT_SET; // if reference bean is null and it is not required ,do nothing,default value
    }
    return value;
  }

  /**
   * Resolve reference {@link PropertyValue}
   *
   * @param ref
   *         {@link BeanReference} record a reference of bean
   *
   * @return A {@link PropertyValue} bean or a proxy
   */
  protected Object resolveBeanReference(AbstractBeanFactory beanFactory, BeanReference ref) {
    final String name = ref.getName();
    final Class<?> type = ref.getReferenceClass();

    if (beanFactory.isFullPrototype() && ref.isPrototype() && beanFactory.containsBeanDefinition(name)) {
      return Prototypes.newProxyInstance(type, beanFactory.getBeanDefinition(name), beanFactory);
    }
    final BeanDefinition reference = ref.getReference();
    if (reference != null) {
      return beanFactory.getBean(reference);
    }
    final Object bean = beanFactory.getBean(name, type);
    return bean != null ? bean : beanFactory.doGetBeanForType(type);
  }

  public BeanReference getReference() {
    return reference;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BeanReferencePropertyValue)) return false;
    if (!super.equals(o)) return false;
    final BeanReferencePropertyValue that = (BeanReferencePropertyValue) o;
    return Objects.equals(reference, that.reference);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), reference);
  }
}
