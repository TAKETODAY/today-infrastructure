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

package cn.taketoday.core.env;

/**
 * {@link Environment} implementation suitable for use in 'standard' (i.e. non-web)
 * applications.
 *
 * <p>In addition to the usual functions of a {@link ConfigurableEnvironment} such as
 * property resolution and profile-related operations, this implementation configures two
 * default property sources, to be searched in the following order:
 * <ul>
 * <li>{@linkplain AbstractEnvironment#getSystemProperties() system properties}
 * <li>{@linkplain AbstractEnvironment#getSystemEnvironment() system environment variables}
 * </ul>
 *
 * That is, if the key "xyz" is present both in the JVM system properties as well as in
 * the set of environment variables for the current process, the value of key "xyz" from
 * system properties will return from a call to {@code environment.getProperty("xyz")}.
 * This ordering is chosen by default because system properties are per-JVM, while
 * environment variables may be the same across many JVMs on a given system.  Giving
 * system properties precedence allows for overriding of environment variables on a
 * per-JVM basis.
 *
 * <p>These default property sources may be removed, reordered, or replaced; and
 * additional property sources may be added using the {@link PropertySources}
 * instance available from {@link #getPropertySources()}. See
 * {@link ConfigurableEnvironment} Javadoc for usage examples.
 *
 * <p>See {@link SystemEnvironmentPropertySource} javadoc for details on special handling
 * of property names in shell environments (e.g. Bash) that disallow period characters in
 * variable names.
 *
 * @author Chris Beams
 * @see ConfigurableEnvironment
 * @see SystemEnvironmentPropertySource
 * @since 4.0
 */
public class StandardEnvironment extends AbstractEnvironment {

  /** System environment property source name: {@value}. */
  public static final String SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME = "systemEnvironment";

  /** JVM system properties property source name: {@value}. */
  public static final String SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME = "systemProperties";

  /**
   * Create a new {@code StandardEnvironment} instance.
   */
  public StandardEnvironment() { }

  /**
   * Create a new {@code StandardEnvironment} instance with a specific
   * {@link PropertySources} instance.
   *
   * @param propertySources property sources to use
   */
  protected StandardEnvironment(PropertySources propertySources) {
    super(propertySources);
  }

  /**
   * Customize the set of property sources with those appropriate for any standard
   * Java environment:
   * <ul>
   * <li>{@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME}
   * <li>{@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}
   * </ul>
   * <p>Properties present in {@value #SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME} will
   * take precedence over those in {@value #SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME}.
   *
   * @see AbstractEnvironment#customizePropertySources(PropertySources)
   * @see #getSystemProperties()
   * @see #getSystemEnvironment()
   */
  @Override
  protected void customizePropertySources(PropertySources propertySources) {
    propertySources.addLast(
            new PropertiesPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, getSystemProperties()));
    propertySources.addLast(
            new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, getSystemEnvironment()));
  }

}
