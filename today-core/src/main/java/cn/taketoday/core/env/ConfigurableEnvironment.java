/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core.env;

import java.util.Map;

/**
 * Configuration interface to be implemented by most if not all {@link Environment} types.
 * Provides facilities for setting active and default profiles and manipulating underlying
 * property sources. Allows clients to set and validate required properties, customize the
 * conversion service and more through the {@link ConfigurablePropertyResolver}
 * superinterface.
 *
 * <h2>Manipulating property sources</h2>
 * <p>Property sources may be removed, reordered, or replaced; and additional
 * property sources may be added using the {@link PropertySources}
 * instance returned from {@link #getPropertySources()}. The following examples
 * are against the {@link StandardEnvironment} implementation of
 * {@code ConfigurableEnvironment}, but are generally applicable to any implementation,
 * though particular default property sources may differ.
 *
 * <h4>Example: adding a new property source with highest search priority</h4>
 * <pre>{@code
 * ConfigurableEnvironment environment = new StandardEnvironment();
 * PropertySources propertySources = environment.getPropertySources();
 * Map<String, String> myMap = new HashMap<>();
 * myMap.put("xyz", "myValue");
 * propertySources.addFirst(new MapPropertySource("MY_MAP", myMap));
 * }</pre>
 *
 * <h4>Example: removing the default system properties property source</h4>
 * <pre>{@code
 * PropertySources propertySources = environment.getPropertySources();
 * propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
 * }</pre>
 *
 * <h4>Example: mocking the system environment for testing purposes</h4>
 * <pre>{@code
 * PropertySources propertySources = environment.getPropertySources();
 * MockPropertySource mockEnvVars = new MockPropertySource().withProperty("xyz", "myValue");
 * propertySources.replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, mockEnvVars);
 * }</pre>
 *
 * When an {@link Environment} is being used by an {@code ApplicationContext}, it is
 * important that any such {@code PropertySource} manipulations be performed
 * <em>before</em> the context's {@link
 * cn.taketoday.context.support.AbstractApplicationContext#refresh() refresh()}
 * method is called. This ensures that all property sources are available during the
 * container bootstrap process.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see StandardEnvironment
 * @see cn.taketoday.context.ConfigurableApplicationContext#getEnvironment
 * @since 4.0
 */
public interface ConfigurableEnvironment extends Environment, ConfigurablePropertyResolver {

  /**
   * Specify the set of profiles active for this {@code Environment}. Profiles are
   * evaluated during container bootstrap to determine whether bean definitions
   * should be registered with the container.
   * <p>Any existing active profiles will be replaced with the given arguments; call
   * with zero arguments to clear the current set of active profiles. Use
   * {@link #addActiveProfile} to add a profile while preserving the existing set.
   *
   * @throws IllegalArgumentException if any profile is null, empty or whitespace-only
   * @see #addActiveProfile
   * @see #setDefaultProfiles
   * @see cn.taketoday.context.annotation.Profile
   * @see AbstractEnvironment#KEY_ACTIVE_PROFILES
   */
  void setActiveProfiles(String... profiles);

  /**
   * Add a profile to the current set of active profiles.
   *
   * @throws IllegalArgumentException if the profile is null, empty or whitespace-only
   * @see #setActiveProfiles
   */
  void addActiveProfile(String profile);

  /**
   * Specify the set of profiles to be made active by default if no other profiles
   * are explicitly made active through {@link #setActiveProfiles}.
   *
   * @throws IllegalArgumentException if any profile is null, empty or whitespace-only
   * @see AbstractEnvironment#KEY_DEFAULT_PROFILES
   */
  void setDefaultProfiles(String... profiles);

  /**
   * Return the {@link PropertySources} for this {@code Environment} in mutable form,
   * allowing for manipulation of the set of {@link PropertySource} objects that should
   * be searched when resolving properties against this {@code Environment} object.
   * The various {@link PropertySources} methods such as
   * {@link PropertySources#addFirst addFirst},
   * {@link PropertySources#addLast addLast},
   * {@link PropertySources#addBefore addBefore} and
   * {@link PropertySources#addAfter addAfter} allow for fine-grained control
   * over property source ordering. This is useful, for example, in ensuring that
   * certain user-defined property sources have search precedence over default property
   * sources such as the set of system properties or the set of system environment
   * variables.
   *
   * @see AbstractEnvironment#customizePropertySources
   */
  PropertySources getPropertySources();

  /**
   * Return the value of {@link System#getProperties()}.
   * <p>Note that most {@code Environment} implementations will include this system
   * properties map as a default {@link PropertySource} to be searched. Therefore, it is
   * recommended that this method not be used directly unless bypassing other property
   * sources is expressly intended.
   */
  Map<String, Object> getSystemProperties();

  /**
   * Return the value of {@link System#getenv()}.
   * <p>Note that most {@link Environment} implementations will include this system
   * environment map as a default {@link PropertySource} to be searched. Therefore, it
   * is recommended that this method not be used directly unless bypassing other
   * property sources is expressly intended.
   */
  Map<String, Object> getSystemEnvironment();

  /**
   * Append the given parent environment's active profiles, default profiles and
   * property sources to this (child) environment's respective collections of each.
   * <p>For any identically-named {@code PropertySource} instance existing in both
   * parent and child, the child instance is to be preserved and the parent instance
   * discarded. This has the effect of allowing overriding of property sources by the
   * child as well as avoiding redundant searches through common property source types,
   * e.g. system environment and system properties.
   * <p>Active and default profile names are also filtered for duplicates, to avoid
   * confusion and redundant storage.
   * <p>The parent environment remains unmodified in any case. Note that any changes to
   * the parent environment occurring after the call to {@code merge} will not be
   * reflected in the child. Therefore, care should be taken to configure parent
   * property sources and profile information prior to calling {@code merge}.
   *
   * @param parent the environment to merge with
   * @see cn.taketoday.context.support.AbstractApplicationContext#setParent
   */
  void merge(ConfigurableEnvironment parent);

}
