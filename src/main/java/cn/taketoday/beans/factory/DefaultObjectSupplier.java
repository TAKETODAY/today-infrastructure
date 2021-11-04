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

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;

/**
 * @author TODAY 2021/3/6 14:10
 * @since 3.0
 */
@SuppressWarnings("unchecked")
public class DefaultObjectSupplier<T> implements ObjectSupplier<T> {

  final Class<T> requiredType;
  final BeanFactory beanFactory;

  public DefaultObjectSupplier(Class<?> requiredType, BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    this.requiredType = (Class<T>) requiredType;
  }

  @Override
  public T getIfAvailable() throws BeansException {
    return beanFactory.getBean(requiredType);
  }

  @Override
  public Class<?> getRequiredType() {
    return requiredType;
  }

  @Override
  public Stream<T> stream() {
    return beanFactory.getBeans(requiredType).stream();
  }

  @Override
  public Iterator<T> iterator() {
    return beanFactory.getBeans(requiredType).iterator();
  }

  @Override
  public Stream<T> orderedStream() {
    List<T> beans = beanFactory.getBeans(requiredType);
    AnnotationAwareOrderComparator.sort(beans);
    return beans.stream();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof DefaultObjectSupplier))
      return false;
    final DefaultObjectSupplier<?> that = (DefaultObjectSupplier<?>) o;
    return Objects.equals(requiredType, that.requiredType)
            && Objects.equals(beanFactory, that.beanFactory);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requiredType, beanFactory);
  }

  @Override
  public String toString() {
    return "DefaultObjectSupplier{" +
            "requiredType=" + requiredType +
            ", beanFactory=" + beanFactory +
            '}';
  }
}
