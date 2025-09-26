/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.beans.factory;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import infra.beans.BeansException;
import infra.beans.factory.annotation.Autowired;
import infra.core.OrderComparator;
import infra.core.Ordered;
import infra.core.annotation.Order;

/**
 * A variant of {@link Supplier} designed specifically for injection points,
 * allowing for programmatic optionality and lenient not-unique handling.
 *
 * <p>In a {@link BeanFactory} environment, every {@code ObjectProvider} obtained
 * from the factory will be bound to its {@code BeanFactory} for a specific bean
 * type, matching all provider calls against factory-registered bean definitions.
 * Note that all such calls dynamically operate on the underlying factory state,
 * freshly resolving the requested target object on every call.
 *
 * <p>this interface extends {@link Iterable} and provides {@link Stream}
 * support. It can be therefore be used in {@code for} loops, provides {@link #forEach}
 * iteration and allows for collection-style {@link #stream} access.
 *
 * <p>As of 5.0, this interface declares default implementations for all methods.
 * This makes it easier to implement in a custom fashion, e.g. for unit tests.
 * For typical purposes, implement {@link #stream()} to enable all other methods.
 * Alternatively, you may implement the specific methods that your callers expect,
 * e.g. just {@link #get()} or {@link #getIfAvailable()}.
 *
 * <p>Note that {@link #get()} never returns {@code null} - it will throw a
 * {@link NoSuchBeanDefinitionException} instead -, whereas {@link #getIfAvailable()}
 * will return {@code null} if no matching bean is present at all. However, both
 * methods will throw a {@link NoUniqueBeanDefinitionException} if more than one
 * matching bean is found without a clear unique winner (see below). Last but not
 * least, {@link #getIfUnique()} will return {@code null} both when no matching bean
 * is found and when more than one matching bean is found without a unique winner.
 *
 * <p>Uniqueness is generally up to the container's candidate resolution algorithm
 * but always honors the "primary" flag (with only one of the candidate beans marked
 * as primary) and the "fallback" flag (with only one of the candidate beans not
 * marked as fallback). The default-candidate flag is consistently taken into
 * account as well, even for non-annotation-based injection points, with a single
 * default candidate winning in case of no clear primary/fallback indication.
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
   * A predicate for unfiltered type matches, including non-default candidates
   * but still excluding non-autowire candidates when used on injection points.
   *
   * @see #stream(Predicate)
   * @see #orderedStream(Predicate)
   * @see infra.beans.factory.config.BeanDefinition#isAutowireCandidate()
   * @see infra.beans.factory.support.AbstractBeanDefinition#isDefaultCandidate()
   * @since 5.0
   */
  Predicate<Class<?>> UNFILTERED = (clazz -> true);

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
    Iterator<T> it = iterator();
    if (!it.hasNext()) {
      throw new NoSuchBeanDefinitionException(Object.class);
    }
    T result = it.next();
    if (it.hasNext()) {
      throw new NoUniqueBeanDefinitionException(Object.class, 2, "more than 1 matching bean");
    }
    return result;
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
  default T get(Object... args) throws BeansException {
    throw new UnsupportedOperationException("Retrieval with arguments not supported -" +
            "for custom ObjectProvider classes, implement getObject(Object...) for your purposes");
  }

  /**
   * Return an instance (possibly shared or independent) of the object
   * managed by this factory.
   *
   * @return an instance of the bean, or {@code null} if not available
   * @throws BeansException in case of creation errors
   * @see #get()
   */
  @Nullable
  default T getIfAvailable() throws BeansException {
    try {
      return get();
    }
    catch (NoUniqueBeanDefinitionException ex) {
      throw ex;
    }
    catch (NoSuchBeanDefinitionException ex) {
      return null;
    }
  }

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
  default T getIfUnique() throws BeansException {
    try {
      return get();
    }
    catch (NoSuchBeanDefinitionException ex) {
      return null;
    }
  }

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
   * @see #orderedStream()
   */
  @Override
  default Iterator<T> iterator() {
    return orderedStream().iterator();
  }

  /**
   * Return a sequential {@link Stream} over all matching object instances,
   * without specific ordering guarantees (but typically in registration order).
   * <p>Note: The result may be filtered by default according to qualifiers on the
   * injection point versus target beans and the general autowire candidate status
   * of matching beans. For custom filtering against type-matching candidates, use
   * {@link #stream(Predicate)} instead (potentially with {@link #UNFILTERED}).
   *
   * @see #iterator()
   * @see #orderedStream()
   */
  default Stream<T> stream() {
    throw new UnsupportedOperationException("Element access not supported - " +
            "for custom ObjectProvider classes, implement stream() to enable all other methods");
  }

  /**
   * Return a custom-filtered {@link Stream} over all matching object instances,
   * without specific ordering guarantees (but typically in registration order).
   *
   * @param customFilter a custom type filter for selecting beans among the raw
   * bean type matches (or {@link #UNFILTERED} for all raw type matches without
   * any default filtering)
   * @see #stream()
   * @see #orderedStream(Predicate)
   * @since 5.0
   */
  default Stream<T> stream(Predicate<Class<?>> customFilter) {
    return stream(customFilter, true);
  }

  /**
   * Return a custom-filtered {@link Stream} over all matching object instances,
   * without specific ordering guarantees (but typically in registration order).
   *
   * @param customFilter a custom type filter for selecting beans among the raw
   * bean type matches (or {@link #UNFILTERED} for all raw type matches without
   * any default filtering)
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @see #stream(Predicate)
   * @see #orderedStream(Predicate, boolean)
   * @since 5.0
   */
  default Stream<T> stream(Predicate<Class<?>> customFilter, boolean includeNonSingletons) {
    if (!includeNonSingletons) {
      throw new UnsupportedOperationException("Only supports includeNonSingletons=true by default");
    }
    return stream().filter(obj -> customFilter.test(obj.getClass()));
  }

  /**
   * Return a sequential {@link Stream} over all matching object instances,
   * pre-ordered according to the factory's common order comparator.
   * <p>In a standard application context, this will be ordered
   * according to {@link Ordered} conventions,
   * and in case of annotation-based configuration also considering the
   * {@link Order} annotation,
   * analogous to multi-element injection points of list/array type.
   * <p>Note: The result may be filtered by default according to qualifiers on the
   * injection point versus target beans and the general autowire candidate status
   * of matching beans. For custom filtering against type-matching candidates, use
   * {@link #stream(Predicate)} instead (potentially with {@link #UNFILTERED}).
   *
   * @see #stream()
   * @see OrderComparator
   */
  default Stream<T> orderedStream() {
    return stream().sorted(OrderComparator.INSTANCE);
  }

  /**
   * Return a custom-filtered {@link Stream} over all matching object instances,
   * pre-ordered according to the factory's common order comparator.
   *
   * @param customFilter a custom type filter for selecting beans among the raw
   * bean type matches (or {@link #UNFILTERED} for all raw type matches without
   * any default filtering)
   * @see #orderedStream()
   * @see #stream(Predicate)
   * @since 5.0
   */
  default Stream<T> orderedStream(Predicate<Class<?>> customFilter) {
    return orderedStream(customFilter, true);
  }

  /**
   * Return a custom-filtered {@link Stream} over all matching object instances,
   * pre-ordered according to the factory's common order comparator.
   *
   * @param customFilter a custom type filter for selecting beans among the raw
   * bean type matches (or {@link #UNFILTERED} for all raw type matches without
   * any default filtering)
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @see #orderedStream()
   * @see #stream(Predicate)
   * @since 5.0
   */
  default Stream<T> orderedStream(Predicate<Class<?>> customFilter, boolean includeNonSingletons) {
    if (!includeNonSingletons) {
      throw new UnsupportedOperationException("Only supports includeNonSingletons=true by default");
    }
    return orderedStream().filter(obj -> customFilter.test(obj.getClass()));
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
