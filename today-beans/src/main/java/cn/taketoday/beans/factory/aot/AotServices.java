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

package cn.taketoday.beans.factory.aot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.util.ObjectUtils;

/**
 * A collection of AOT services that can be {@link Loader loaded} from
 * a {@link cn.taketoday.lang.TodayStrategies} or obtained from a {@link BeanFactory}.
 *
 * @param <T> the service type
 * @author Phillip Webb
 * @since 4.0
 */
public final class AotServices<T> implements Iterable<T> {

  /**
   * The location to look for AOT factories.
   */
  public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/config/aot.factories";

  private final List<T> services;

  private final Map<String, T> beans;

  private final Map<T, Source> sources;

  private AotServices(List<T> loaded, Map<String, T> beans) {
    this.services = collectServices(loaded, beans);
    this.sources = collectSources(loaded, beans.values());
    this.beans = beans;
  }

  private List<T> collectServices(List<T> loaded, Map<String, T> beans) {
    List<T> services = new ArrayList<>();
    services.addAll(beans.values());
    services.addAll(loaded);
    AnnotationAwareOrderComparator.sort(services);
    return Collections.unmodifiableList(services);
  }

  private Map<T, Source> collectSources(Collection<T> loaded, Collection<T> beans) {
    Map<T, Source> sources = new IdentityHashMap<>();
    loaded.forEach(service -> sources.put(service, Source.INFRA_SPI));
    beans.forEach(service -> sources.put(service, Source.BEAN_FACTORY));
    return Collections.unmodifiableMap(sources);
  }

  /**
   * Create a new {@link Loader} that will obtain AOT services from
   * {@value #FACTORIES_RESOURCE_LOCATION}.
   *
   * @return a new {@link Loader} instance
   */
  public static Loader factories() {
    return factories((ClassLoader) null);
  }

  /**
   * Create a new {@link Loader} that will obtain AOT services from
   * {@value #FACTORIES_RESOURCE_LOCATION}.
   *
   * @param classLoader the class loader used to load the factories resource
   * @return a new {@link Loader} instance
   */
  public static Loader factories(@Nullable ClassLoader classLoader) {
    return factories(getTodayStrategies(classLoader));
  }

  /**
   * Create a new {@link Loader} that will obtain AOT services from the given
   * {@link TodayStrategies}.
   *
   * @param strategies the spring factories loader
   * @return a new {@link Loader} instance
   */
  public static Loader factories(TodayStrategies strategies) {
    Assert.notNull(strategies, "'strategies' is required");
    return new Loader(strategies, null);
  }

  /**
   * Create a new {@link Loader} that will obtain AOT services from
   * {@value #FACTORIES_RESOURCE_LOCATION} as well as the given
   * {@link BeanFactory}.
   *
   * @param beanFactory the bean factory
   * @return a new {@link Loader} instance
   */
  public static Loader factoriesAndBeans(BeanFactory beanFactory) {
    ClassLoader classLoader = (beanFactory instanceof ConfigurableBeanFactory configurableBeanFactory ?
                               configurableBeanFactory.getBeanClassLoader() : null);
    return factoriesAndBeans(getTodayStrategies(classLoader), beanFactory);
  }

  /**
   * Create a new {@link Loader} that will obtain AOT services from the given
   * {@link TodayStrategies} and {@link BeanFactory}.
   *
   * @param strategies the spring factories loader
   * @param beanFactory the bean factory
   * @return a new {@link Loader} instance
   */
  public static Loader factoriesAndBeans(TodayStrategies strategies, BeanFactory beanFactory) {
    Assert.notNull(beanFactory, "'beanFactory' is required");
    Assert.notNull(strategies, "'strategies' is required");
    return new Loader(strategies, beanFactory);
  }

  private static TodayStrategies getTodayStrategies(
          @Nullable ClassLoader classLoader) {
    return TodayStrategies.forLocation(FACTORIES_RESOURCE_LOCATION, classLoader);
  }

  @Override
  public Iterator<T> iterator() {
    return this.services.iterator();
  }

  /**
   * Return a {@link Stream} of the AOT services.
   *
   * @return a stream of the services
   */
  public Stream<T> stream() {
    return this.services.stream();
  }

  /**
   * Return the AOT services as a {@link List}.
   *
   * @return a list of the services
   */
  public List<T> asList() {
    return this.services;
  }

  /**
   * Find the AOT service that was loaded for the given bean name.
   *
   * @param beanName the bean name
   * @return the AOT service or {@code null}
   */
  @Nullable
  public T findByBeanName(String beanName) {
    return this.beans.get(beanName);
  }

  /**
   * Get the source of the given service.
   *
   * @param service the service instance
   * @return the source of the service
   */
  public Source getSource(T service) {
    Source source = this.sources.get(service);
    Assert.state(source != null,
            () -> "Unable to find service " + ObjectUtils.identityToString(source));
    return source;
  }

  /**
   * Loader class used to actually load the services.
   */
  public static class Loader {

    private final TodayStrategies strategies;

    @Nullable
    private final BeanFactory beanFactory;

    Loader(TodayStrategies strategies, @Nullable BeanFactory beanFactory) {
      this.strategies = strategies;
      this.beanFactory = beanFactory;
    }

    /**
     * Load all AOT services of the given type.
     *
     * @param <T> the service type
     * @param type the service type
     * @return a new {@link AotServices} instance
     */
    public <T> AotServices<T> load(Class<T> type) {
      return new AotServices<>(this.strategies.load(type), loadBeans(type));
    }

    private <T> Map<String, T> loadBeans(Class<T> type) {
      return (this.beanFactory != null) ? BeanFactoryUtils
              .beansOfTypeIncludingAncestors(this.beanFactory, type, true, false)
                                        : Collections.emptyMap();
    }

  }

  /**
   * Sources from which services were obtained.
   */
  public enum Source {

    /**
     * An AOT service loaded from {@link TodayStrategies}.
     */
    INFRA_SPI,

    /**
     * An AOT service loaded from a {@link BeanFactory}.
     */
    BEAN_FACTORY

  }

}
