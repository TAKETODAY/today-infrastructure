/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.core.OrderComparator;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.lang.Nullable;

/**
 * A variant of {@link Supplier} designed specifically for injection points,
 * allowing for programmatic optionality and lenient not-unique handling.
 *
 * <p>this interface extends {@link Iterable} and provides {@link Stream}
 * support. It can be therefore be used in {@code for} loops, provides {@link #forEach}
 * iteration and allows for collection-style {@link #stream} access.
 * <p>
 * Iteration pre-ordered according to the factory's common order comparator.
 * <p>In a standard application context, this will be ordered
 * according to {@link Ordered} conventions,
 * and in case of annotation-based configuration also considering the
 * {@link Order} annotation,
 * analogous to multi-element injection points of list/array type.
 *
 * @param <T> the object type
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanFactory#getBeanProvider
 * @see Autowired
 * @since 3.0 2021/3/6 11:18
 */
public interface ObjectProvider<T> extends Supplier<T>, Iterable<T> {

  /**
   * Return an instance (possibly shared or independent) of the object
   * managed by this factory.
   * <p>Allows for specifying explicit construction arguments, along the
   * lines of {@link BeanFactory#getBean(String)}.
   *
   * @return an instance of the bean
   * @throws BeansException in case of creation errors
   */
  @Override
  default T get() throws BeansException {
    return get((Object[]) null);
  }

  /**
   * Return an instance (possibly shared or independent) of the object
   * managed by this factory.
   * <p>Allows for specifying explicit construction arguments, along the
   * lines of {@link BeanFactory#getBean(String, Object...)}.
   *
   * @param args arguments to use when creating a corresponding instance
   * @return an instance of the bean
   * @throws BeansException in case of creation errors
   * @see #get()
   * @since 4.0
   */
  T get(Object... args) throws BeansException;

  /**
   * Return an instance (possibly shared or independent) of the object
   * managed by this factory.
   *
   * @return an instance of the bean, or {@code null} if not available
   * @throws BeansException in case of creation errors
   * @see #get()
   */
  @Nullable
  T getIfAvailable() throws BeansException;

  /**
   * Return an instance (possibly shared or independent) of the object
   * managed by this factory.
   *
   * @param defaultSupplier a callback for supplying a default object
   * if none is present in the factory
   * @return an instance of the bean, or the supplied default object
   * if no such bean is available
   * @throws BeansException in case of creation errors
   * @see #getIfAvailable()
   */
  default T getIfAvailable(Supplier<T> defaultSupplier) throws BeansException {
    T dependency = getIfAvailable();
    return dependency != null ? dependency : defaultSupplier.get();
  }

  /**
   * Consume an instance (possibly shared or independent) of the object
   * managed by this factory, if available.
   *
   * @param dependencyConsumer a callback for processing the target object
   * if available (not called otherwise)
   * @return if available status
   * @throws BeansException in case of creation errors
   * @see #getIfAvailable()
   */
  default boolean ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
    T dependency = getIfAvailable();
    if (dependency != null) {
      dependencyConsumer.accept(dependency);
      return true;
    }
    return false;
  }

  /**
   * Return an instance (possibly shared or independent) of the object
   * managed by this factory.
   *
   * @return an instance of the bean, or {@code null} if not available or
   * not unique (i.e. multiple candidates found with none marked as primary)
   * @throws BeansException in case of creation errors
   * @see #get()
   */
  @Nullable
  T getIfUnique() throws BeansException;

  /**
   * Return an instance (possibly shared or independent) of the object
   * managed by this factory.
   *
   * @param defaultSupplier a callback for supplying a default object
   * if no unique candidate is present in the factory
   * @return an instance of the bean, or the supplied default object
   * if no such bean is available or if it is not unique in the factory
   * (i.e. multiple candidates found with none marked as primary)
   * @throws BeansException in case of creation errors
   * @see #getIfUnique()
   * @since 4.0
   */
  default T getIfUnique(Supplier<T> defaultSupplier) throws BeansException {
    T dependency = getIfUnique();
    return dependency != null ? dependency : defaultSupplier.get();
  }

  /**
   * Consume an instance (possibly shared or independent) of the object
   * managed by this factory, if unique.
   *
   * @param dependencyConsumer a callback for processing the target object
   * if unique (not called otherwise)
   * @return if Unique
   * @throws BeansException in case of creation errors
   * @see #getIfAvailable()
   */
  default boolean ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
    T dependency = getIfUnique();
    if (dependency != null) {
      dependencyConsumer.accept(dependency);
      return true;
    }
    return false;
  }

  /**
   * Return a sequential {@link Iterator} over all matching object instances,
   * pre-ordered according to the factory's common order comparator.
   * <p>In a standard application context, this will be ordered
   * according to {@link Ordered} conventions,
   * and in case of annotation-based configuration also considering the
   * {@link Order} annotation,
   * analogous to multi-element injection points of list/array type.
   *
   * @see #stream()
   */
  @Override
  default Iterator<T> iterator() {
    return orderedStream().iterator();
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
   * <p>In a standard application context, this will be ordered
   * according to {@link Ordered} conventions,
   * and in case of annotation-based configuration also considering the
   * {@link Order} annotation,
   * analogous to multi-element injection points of list/array type.
   *
   * @see #stream()
   * @see OrderComparator
   */
  default Stream<T> orderedStream() {
    throw new UnsupportedOperationException("Ordered element access not supported");
  }

  /**
   * Return a {@link List} over all matching object instances,
   * pre-ordered according to the factory's common order comparator.
   * <p>In a standard application context, this will be ordered
   * according to {@link Ordered} conventions,
   * and in case of annotation-based configuration also considering the
   * {@link Order} annotation,
   * analogous to multi-element injection points of list/array type.
   *
   * @see Stream#toList()
   * @see #iterator()
   * @see #orderedStream()
   * @since 4.0
   */
  default ArrayList<T> orderedList() {
    ArrayList<T> ret = new ArrayList<>();
    for (T t : this) {
      ret.add(t);
    }
    return ret;
  }

  /**
   * Return a {@link List} over all matching object instances,
   * without specific ordering guarantees (but typically in registration order).
   *
   * @see Stream#toList()
   * @see #iterator()
   * @since 4.0
   */
  default ArrayList<T> toList() {
    ArrayList<T> ret = new ArrayList<>();
    Iterator<T> iterator = stream().iterator();
    while (iterator.hasNext()) {
      ret.add(iterator.next());
    }
    return ret;
  }

  /**
   * Add over all matching object instances to a {@link List}
   * pre-ordered according to the factory's common order comparator.
   * <p>In a standard application context, this will be ordered
   * according to {@link Ordered} conventions,
   * and in case of annotation-based configuration also considering the
   * {@link Order} annotation,
   * analogous to multi-element injection points of list/array type.
   *
   * @see Stream#toList()
   * @see #iterator()
   * @since 4.0
   */
  default void addOrderedTo(Collection<T> destination) {
    for (T t : this) {
      destination.add(t);
    }
  }

  /**
   * Add a {@link List} over all matching object instances,
   * without specific ordering guarantees (but typically in registration order).
   *
   * @see Stream#toList()
   * @see #iterator()
   * @since 4.0
   */
  default void addTo(Collection<T> destination) {
    Iterator<T> iterator = stream().iterator();
    while (iterator.hasNext()) {
      destination.add(iterator.next());
    }
  }

}
