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

package infra.context.annotation.config;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import infra.context.ApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.ImportSelector;
import infra.core.OrderComparator;
import infra.core.Ordered;
import infra.lang.Assert;
import infra.util.ClassUtils;

/**
 * A set of {@link Configuration @Configuration} classes that can be registered in
 * {@link ApplicationContext}. Classes can be returned from one or more
 * {@link Configurations} instances by using {@link #getClasses(Configurations[])}. The
 * resulting array follows the ordering rules usually applied by the
 * {@link ApplicationContext} and/or custom {@link ImportSelector} implementations.
 * <p>
 * This class is primarily intended for use with tests that need to specify configuration
 * classes but can't use {@link infra.app.ApplicationRunner}.
 * <p>
 * Implementations of this class should be annotated with {@code @Order} or implement
 * {@link Ordered}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see UserConfigurations
 * @since 4.0 2022/2/1 12:13
 */
public abstract class Configurations {

  private static final Comparator<Object> COMPARATOR = OrderComparator.INSTANCE
          .thenComparing((other) -> other.getClass().getName());

  @Nullable
  private final UnaryOperator<Collection<Class<?>>> sorter;

  private final Set<Class<?>> classes;

  @Nullable
  private final Function<Class<?>, String> beanNameGenerator;

  /**
   * Create a new {@link Configurations} instance.
   *
   * @param classes the configuration classes
   */
  protected Configurations(Collection<Class<?>> classes) {
    Assert.notNull(classes, "Classes is required");
    Collection<Class<?>> sorted = sort(classes);
    this.sorter = null;
    this.classes = Collections.unmodifiableSet(new LinkedHashSet<>(sorted));
    this.beanNameGenerator = null;
  }

  /**
   * Create a new {@link Configurations} instance.
   *
   * @param sorter a {@link UnaryOperator} used to sort the configurations
   * @param classes the configuration classes
   * @param beanNameGenerator an optional function used to generate the bean name
   */
  protected Configurations(@Nullable UnaryOperator<Collection<Class<?>>> sorter, Collection<Class<?>> classes,
          @Nullable Function<Class<?>, String> beanNameGenerator) {
    Assert.notNull(classes, "Classes must not be null");
    this.sorter = (sorter != null) ? sorter : UnaryOperator.identity();
    Collection<Class<?>> sorted = this.sorter.apply(classes);
    this.classes = Collections.unmodifiableSet(new LinkedHashSet<>(sorted));
    this.beanNameGenerator = beanNameGenerator;
  }

  protected final Set<Class<?>> getClasses() {
    return this.classes;
  }

  /**
   * Sort configuration classes into the order that they should be applied.
   *
   * @param classes the classes to sort
   * @return a sorted set of classes
   * {@link #Configurations(UnaryOperator, Collection, Function)}
   */
  protected Collection<Class<?>> sort(Collection<Class<?>> classes) {
    return classes;
  }

  /**
   * Merge configurations from another source of the same type.
   *
   * @param other the other {@link Configurations} (must be of the same type as this
   * instance)
   * @return a new configurations instance (must be of the same type as this instance)
   */
  protected Configurations merge(Configurations other) {
    Set<Class<?>> mergedClasses = new LinkedHashSet<>(getClasses());
    mergedClasses.addAll(other.getClasses());
    if (this.sorter != null) {
      mergedClasses = new LinkedHashSet<>(this.sorter.apply(mergedClasses));
    }
    return merge(mergedClasses);
  }

  /**
   * Merge configurations.
   *
   * @param mergedClasses the merged classes
   * @return a new configurations instance (must be of the same type as this instance)
   */
  protected abstract Configurations merge(Set<Class<?>> mergedClasses);

  /**
   * Return the bean name that should be used for the given configuration class or
   * {@code null} to use the default name.
   *
   * @param beanClass the bean class
   * @return the bean name
   */
  @Nullable
  public String getBeanName(Class<?> beanClass) {
    return (this.beanNameGenerator != null) ? this.beanNameGenerator.apply(beanClass) : null;
  }

  /**
   * Return the classes from all the specified configurations in the order that they
   * would be registered.
   *
   * @param configurations the source configuration
   * @return configuration classes in registration order
   */
  public static Class<?>[] getClasses(Configurations... configurations) {
    return getClasses(Arrays.asList(configurations));
  }

  /**
   * Return the classes from all the specified configurations in the order that they
   * would be registered.
   *
   * @param configurations the source configuration
   * @return configuration classes in registration order
   */
  public static Class<?>[] getClasses(Collection<Configurations> configurations) {
    List<Configurations> collated = collate(configurations);
    LinkedHashSet<Class<?>> classes = collated.stream()
            .flatMap(Configurations::streamClasses)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    return ClassUtils.toClassArray(classes);
  }

  /**
   * Collate the given configuration by sorting and merging them.
   *
   * @param configurations the source configuration
   * @return the collated configurations
   */
  public static List<Configurations> collate(Collection<Configurations> configurations) {
    LinkedList<Configurations> collated = new LinkedList<>();
    for (Configurations configuration : sortConfigurations(configurations)) {
      if (collated.isEmpty() || collated.getLast().getClass() != configuration.getClass()) {
        collated.add(configuration);
      }
      else {
        collated.set(collated.size() - 1, collated.getLast().merge(configuration));
      }
    }
    return collated;
  }

  private static List<Configurations> sortConfigurations(Collection<Configurations> configurations) {
    List<Configurations> sorted = new ArrayList<>(configurations);
    sorted.sort(COMPARATOR);
    return sorted;
  }

  private static Stream<Class<?>> streamClasses(Configurations configurations) {
    return configurations.getClasses().stream();
  }

}
