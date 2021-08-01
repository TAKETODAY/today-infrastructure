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

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import cn.taketoday.context.utils.GenericTypeResolver;

/**
 * @author TODAY 2021/3/6 11:18
 * @since 3.0
 */
@FunctionalInterface
public interface ObjectSupplier<T> extends Supplier<T>, Iterable<T> {

  /**
   * Return an instance (possibly shared or independent) of the object
   * managed by this factory.
   * <p>Allows for specifying explicit construction arguments, along the
   * lines of {@link BeanFactory#getBean(String)}.
   *
   * @return an instance of the bean
   *
   * @throws BeansException
   *         in case of creation errors
   */
  @Override
  default T get() throws BeansException {
    final T ret = getIfAvailable();
    if (ret == null) {
      throw new NoSuchBeanDefinitionException(getRequiredType());
    }
    return ret;
  }

  /**
   * Return an instance (possibly shared or independent) of the object
   * managed by this factory.
   *
   * @return an instance of the bean, or {@code null} if not available
   *
   * @throws BeansException
   *         in case of creation errors
   * @see #get()
   */
  T getIfAvailable() throws BeansException;

  /**
   * Return an instance (possibly shared or independent) of the object
   * managed by this factory.
   *
   * @param defaultSupplier
   *         a callback for supplying a default object
   *         if none is present in the factory
   *
   * @return an instance of the bean, or the supplied default object
   * if no such bean is available
   *
   * @throws BeansException
   *         in case of creation errors
   * @see #getIfAvailable()
   */
  default T getIfAvailable(Supplier<T> defaultSupplier) throws BeansException {
    T dependency = getIfAvailable();
    return (dependency != null ? dependency : defaultSupplier.get());
  }

  /**
   * Consume an instance (possibly shared or independent) of the object
   * managed by this factory, if available.
   *
   * @param dependencyConsumer
   *         a callback for processing the target object
   *         if available (not called otherwise)
   *
   * @throws BeansException
   *         in case of creation errors
   * @see #getIfAvailable()
   */
  default void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
    T dependency = getIfAvailable();
    if (dependency != null) {
      dependencyConsumer.accept(dependency);
    }
  }

  default Class<?> getRequiredType() {
    return GenericTypeResolver.resolveTypeArgument(getClass(), ObjectSupplier.class);
  }

  //

  /**
   * Return an {@link Iterator} over all matching object instances,
   * without specific ordering guarantees (but typically in registration order).
   *
   * @see #stream()
   */
  @Override
  default Iterator<T> iterator() {
    return stream().iterator();
  }

  /**
   * Return a sequential {@link Stream} over all matching object instances,
   * without specific ordering guarantees (but typically in registration order).
   *
   * @see #iterator()
   * @see #orderedStream()
   */
  default Stream<T> stream() {
    throw new UnsupportedOperationException("Multi element access not supported");
  }

  /**
   * Return a sequential {@link Stream} over all matching object instances,
   * pre-ordered according to the factory's common order comparator.
   * <p>In a standard Spring application context, this will be ordered
   * according to {@link cn.taketoday.context.Ordered} conventions,
   * and in case of annotation-based configuration also considering the
   * {@link cn.taketoday.context.annotation.Order} annotation,
   * analogous to multi-element injection points of list/array type.
   *
   * @see #stream()
   * @see cn.taketoday.context.utils.OrderUtils
   */
  default Stream<T> orderedStream() {
    throw new UnsupportedOperationException("Ordered element access not supported");
  }

}
