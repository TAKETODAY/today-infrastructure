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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties.source;

import java.util.Collections;
import java.util.stream.Stream;

import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.ConfigurablePropertyResolver;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySource.StubPropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.PropertySourcesPropertyResolver;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Provides access to {@link ConfigurationPropertySource ConfigurationPropertySources}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class ConfigurationPropertySources {

  /**
   * The name of the {@link PropertySource} {@link #attach(Environment) adapter}.
   */
  private static final String ATTACHED_PROPERTY_SOURCE_NAME = "configurationProperties";

  private ConfigurationPropertySources() { }

  /**
   * Create a new {@link PropertyResolver} that resolves property values against an
   * underlying set of {@link PropertySources}. Provides an
   * {@link ConfigurationPropertySource} aware and optimized alternative to
   * {@link PropertySourcesPropertyResolver}.
   *
   * @param propertySources the set of {@link PropertySource} objects to use
   * @return a {@link ConfigurablePropertyResolver} implementation
   */
  public static ConfigurablePropertyResolver createPropertyResolver(PropertySources propertySources) {
    return new ConfigurationPropertySourcesPropertyResolver(propertySources);
  }

  /**
   * Determines if the specific {@link PropertySource} is the
   * {@link ConfigurationPropertySource} that was {@link #attach(Environment) attached}
   * to the {@link Environment}.
   *
   * @param propertySource the property source to test
   * @return {@code true} if this is the attached {@link ConfigurationPropertySource}
   */
  public static boolean isAttachedConfigurationPropertySource(PropertySource<?> propertySource) {
    return ATTACHED_PROPERTY_SOURCE_NAME.equals(propertySource.getName());
  }

  /**
   * Attach a {@link ConfigurationPropertySource} support to the specified
   * {@link Environment}. Adapts each {@link PropertySource} managed by the environment
   * to a {@link ConfigurationPropertySource} and allows classic
   * {@link PropertySourcesPropertyResolver} calls to resolve using
   * {@link ConfigurationPropertyName configuration property names}.
   * <p>
   * The attached resolver will dynamically track any additions or removals from the
   * underlying {@link Environment} property sources.
   *
   * @param environment the source environment (must be an instance of
   * {@link ConfigurableEnvironment})
   * @see #get(Environment)
   */
  public static void attach(Environment environment) {
    Assert.isInstanceOf(ConfigurableEnvironment.class, environment);
    PropertySources sources = ((ConfigurableEnvironment) environment).getPropertySources();
    PropertySource<?> attached = getAttached(sources);
    if (attached == null || !isUsingSources(attached, sources)) {
      attached = new ConfigurationPropertySourcesPropertySource(ATTACHED_PROPERTY_SOURCE_NAME,
              new FrameworkConfigurationPropertySources(sources));
    }
    sources.remove(ATTACHED_PROPERTY_SOURCE_NAME);
    sources.addFirst(attached);
  }

  private static boolean isUsingSources(PropertySource<?> attached, PropertySources sources) {
    return attached instanceof ConfigurationPropertySourcesPropertySource
            && ((FrameworkConfigurationPropertySources) attached.getSource()).isUsingSources(sources);
  }

  @Nullable
  static PropertySource<?> getAttached(@Nullable PropertySources sources) {
    return sources != null ? sources.get(ATTACHED_PROPERTY_SOURCE_NAME) : null;
  }

  /**
   * Return a set of {@link ConfigurationPropertySource} instances that have previously
   * been {@link #attach(Environment) attached} to the {@link Environment}.
   *
   * @param environment the source environment (must be an instance of
   * {@link ConfigurableEnvironment})
   * @return an iterable set of configuration property sources
   * @throws IllegalStateException if not configuration property sources have been
   * attached
   */
  public static Iterable<ConfigurationPropertySource> get(Environment environment) {
    Assert.isInstanceOf(ConfigurableEnvironment.class, environment);
    PropertySources sources = ((ConfigurableEnvironment) environment).getPropertySources();
    ConfigurationPropertySourcesPropertySource attached =
            (ConfigurationPropertySourcesPropertySource) sources.get(ATTACHED_PROPERTY_SOURCE_NAME);
    if (attached == null) {
      return from(sources);
    }
    return attached.getSource();
  }

  /**
   * Return {@link Iterable} containing a single new {@link ConfigurationPropertySource}
   * adapted from the given Spring {@link PropertySource}.
   *
   * @param source the Spring property source to adapt
   * @return an {@link Iterable} containing a single newly adapted
   * {@link FrameworkConfigurationPropertySource}
   */
  public static Iterable<ConfigurationPropertySource> from(PropertySource<?> source) {
    return Collections.singleton(ConfigurationPropertySource.from(source));
  }

  /**
   * Return {@link Iterable} containing new {@link ConfigurationPropertySource}
   * instances adapted from the given Spring {@link PropertySource PropertySources}.
   * <p>
   * This method will flatten any nested property sources and will filter all
   * {@link StubPropertySource stub property sources}. Updates to the underlying source,
   * identified by changes in the sources returned by its iterator, will be
   * automatically tracked. The underlying source should be thread safe, for example a
   * {@link PropertySources}
   *
   * @param sources the Spring property sources to adapt
   * @return an {@link Iterable} containing newly adapted
   * {@link FrameworkConfigurationPropertySource} instances
   */
  public static Iterable<ConfigurationPropertySource> from(Iterable<PropertySource<?>> sources) {
    return new FrameworkConfigurationPropertySources(sources);
  }

  private static Stream<PropertySource<?>> streamPropertySources(PropertySources sources) {
    return sources.stream().flatMap(ConfigurationPropertySources::flatten)
            .filter(ConfigurationPropertySources::isIncluded);
  }

  private static Stream<PropertySource<?>> flatten(PropertySource<?> source) {
    if (source.getSource() instanceof ConfigurableEnvironment) {
      return streamPropertySources(((ConfigurableEnvironment) source.getSource()).getPropertySources());
    }
    return Stream.of(source);
  }

  private static boolean isIncluded(PropertySource<?> source) {
    return !(source instanceof StubPropertySource)
            && !(source instanceof ConfigurationPropertySourcesPropertySource);
  }

}
