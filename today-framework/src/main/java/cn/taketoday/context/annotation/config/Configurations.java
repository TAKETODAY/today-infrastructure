/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.annotation.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.loader.ImportSelector;
import cn.taketoday.core.OrderComparator;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;

/**
 * A set of {@link Configuration @Configuration} classes that can be registered in
 * {@link ApplicationContext}. Classes can be returned from one or more
 * {@link Configurations} instances by using {@link #getClasses(Configurations[])}. The
 * resulting array follows the ordering rules usually applied by the
 * {@link ApplicationContext} and/or custom {@link ImportSelector} implementations.
 * <p>
 * This class is primarily intended for use with tests that need to specify configuration
 * classes but can't use {@link TodayRunner}.
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

  private final Set<Class<?>> classes;

  protected Configurations(Collection<Class<?>> classes) {
    Assert.notNull(classes, "Classes must not be null");
    Collection<Class<?>> sorted = sort(classes);
    this.classes = Collections.unmodifiableSet(new LinkedHashSet<>(sorted));
  }

  /**
   * Sort configuration classes into the order that they should be applied.
   *
   * @param classes the classes to sort
   * @return a sorted set of classes
   */
  protected Collection<Class<?>> sort(Collection<Class<?>> classes) {
    return classes;
  }

  protected final Set<Class<?>> getClasses() {
    return this.classes;
  }

  /**
   * Merge configurations from another source of the same type.
   *
   * @param other the other {@link Configurations} (must be of the same type as this
   * instance)
   * @return a new configurations instance (must be of the same type as this instance)
   */
  protected Configurations merge(Configurations other) {
    LinkedHashSet<Class<?>> mergedClasses = new LinkedHashSet<>(getClasses());
    mergedClasses.addAll(other.getClasses());
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
    List<Configurations> ordered = new ArrayList<>(configurations);
    ordered.sort(COMPARATOR);
    List<Configurations> collated = collate(ordered);
    LinkedHashSet<Class<?>> classes = collated.stream().flatMap(Configurations::streamClasses)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    return ClassUtils.toClassArray(classes);
  }

  private static Stream<Class<?>> streamClasses(Configurations configurations) {
    return configurations.getClasses().stream();
  }

  private static List<Configurations> collate(List<Configurations> orderedConfigurations) {
    LinkedList<Configurations> collated = new LinkedList<>();
    for (Configurations item : orderedConfigurations) {
      if (collated.isEmpty() || collated.getLast().getClass() != item.getClass()) {
        collated.add(item);
      }
      else {
        collated.set(collated.size() - 1, collated.getLast().merge(item));
      }
    }
    return collated;
  }

}
