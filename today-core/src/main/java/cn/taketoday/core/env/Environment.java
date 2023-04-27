/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import cn.taketoday.lang.Constant;

/**
 * Interface representing the environment in which the current application is running.
 * Models two key aspects of the application environment: <em>profiles</em> and
 * <em>properties</em>. Methods related to property access are exposed via the
 * {@link PropertyResolver} superinterface.
 *
 * <p>A <em>profile</em> is a named, logical group of bean definitions to be registered
 * with the container only if the given profile is <em>active</em>. Beans may be assigned
 * to a profile whether defined in XML or via annotations; see the or the
 * {@link cn.taketoday.context.annotation.Profile @Profile} annotation for
 * syntax details. The role of the {@code Environment} object with relation to profiles is
 * in determining which profiles (if any) are currently {@linkplain #getActiveProfiles
 * active}, and which profiles (if any) should be {@linkplain #getDefaultProfiles active
 * by default}.
 *
 * <p><em>Properties</em> play an important role in almost all applications, and may
 * originate from a variety of sources: properties files, JVM system properties, system
 * environment variables, JNDI, servlet context parameters, ad-hoc Properties objects,
 * Maps, and so on. The role of the environment object with relation to properties is to
 * provide the user with a convenient service interface for configuring property sources
 * and resolving properties from them.
 *
 * <p>Beans managed within an {@code ApplicationContext} may register to be {@link
 * cn.taketoday.context.EnvironmentAware EnvironmentAware} or {@code @Inject} the
 * {@code Environment} in order to query profile state or resolve properties directly.
 *
 *
 * <p>Configuration of the environment object must be done through the
 * {@code ConfigurableEnvironment} interface, returned from all
 * {@code AbstractApplicationContext} subclass {@code getEnvironment()} methods. See
 * {@link ConfigurableEnvironment} Javadoc for usage examples demonstrating manipulation
 * of property sources prior to application context {@code refresh()}.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertyResolver
 * @see EnvironmentCapable
 * @see ConfigurableEnvironment
 * @see AbstractEnvironment
 * @see StandardEnvironment
 * @see cn.taketoday.context.EnvironmentAware
 * @see cn.taketoday.context.ConfigurableApplicationContext#getEnvironment
 * @see cn.taketoday.context.ConfigurableApplicationContext#setEnvironment
 * @see cn.taketoday.context.support.AbstractApplicationContext#createEnvironment
 * @since 4.0
 */
public interface Environment extends PropertyResolver {

  /**
   * Name of the {@link Environment} bean in the factory.
   *
   * @since 4.0
   */
  String ENVIRONMENT_BEAN_NAME = "environment";

  /**
   * Name of the System properties bean in the factory.
   *
   * @see java.lang.System#getProperties()
   * @since 4.0
   */
  String SYSTEM_PROPERTIES_BEAN_NAME = "systemProperties";

  /**
   * Name of the System environment bean in the factory.
   *
   * @see java.lang.System#getenv()
   * @since 4.0
   */
  String SYSTEM_ENVIRONMENT_BEAN_NAME = "systemEnvironment";

  /**
   * System property that instructs to ignore system environment variables,
   * i.e. to never attempt to retrieve such a variable via {@link System#getenv()}.
   * <p>The default is "false", falling back to system environment variable checks if an
   * environment property (e.g. a placeholder in a configuration String) isn't
   * resolvable otherwise. Consider switching this flag to "true" if you experience
   * log warnings from {@code getenv} calls coming from TODAY.
   *
   * @see AbstractEnvironment#suppressGetenvAccess()
   */
  String KEY_IGNORE_GETENV = "infra.getenv.ignore";

  /**
   * Name of property to set to specify active profiles: {@value}. Value may be comma
   * delimited.
   * <p>Note that certain shell environments such as Bash disallow the use of the period
   * character in variable names. Assuming that {@link SystemEnvironmentPropertySource}
   * is in use, this property may be specified as an environment variable as
   * {@code CONTEXT_PROFILES_ACTIVE}.
   *
   * @see ConfigurableEnvironment#setActiveProfiles
   */
  String KEY_ACTIVE_PROFILES = "infra.profiles.active";

  /**
   * Name of property to set to specify profiles active by default: {@value}. Value may
   * be comma delimited.
   * <p>Note that certain shell environments such as Bash disallow the use of the period
   * character in variable names. Assuming that {@link SystemEnvironmentPropertySource}
   * is in use, this property may be specified as an environment variable as
   * {@code CONTEXT_PROFILES_DEFAULT}.
   *
   * @see ConfigurableEnvironment#setDefaultProfiles
   */
  String KEY_DEFAULT_PROFILES = "infra.profiles.default";

  /**
   * Name of reserved default profile name: {@value}. If no default profile names are
   * explicitly and no active profile names are explicitly set, this profile will
   * automatically be activated by default.
   *
   * @see ConfigurableEnvironment#setDefaultProfiles
   * @see ConfigurableEnvironment#setActiveProfiles
   * @see AbstractEnvironment#KEY_DEFAULT_PROFILES
   * @see AbstractEnvironment#KEY_ACTIVE_PROFILES
   */
  String DEFAULT_PROFILE = Constant.DEFAULT;

  /**
   * Return the set of profiles explicitly made active for this environment. Profiles
   * are used for creating logical groupings of bean definitions to be registered
   * conditionally, for example based on deployment environment. Profiles can be
   * activated by setting {@linkplain AbstractEnvironment#KEY_ACTIVE_PROFILES
   * "infra.profiles.active"} as a system property or by calling
   * {@link ConfigurableEnvironment#setActiveProfiles(String...)}.
   * <p>If no profiles have explicitly been specified as active, then any
   * {@linkplain #getDefaultProfiles() default profiles} will automatically be activated.
   *
   * @see #getDefaultProfiles
   * @see ConfigurableEnvironment#setActiveProfiles
   * @see AbstractEnvironment#KEY_ACTIVE_PROFILES
   */
  String[] getActiveProfiles();

  /**
   * Return the set of profiles to be active by default when no active profiles have
   * been set explicitly.
   *
   * @see #getActiveProfiles
   * @see ConfigurableEnvironment#setDefaultProfiles
   * @see AbstractEnvironment#KEY_DEFAULT_PROFILES
   */
  String[] getDefaultProfiles();

  /**
   * Determine whether one of the given profile expressions matches the
   * {@linkplain #getActiveProfiles() active profiles} &mdash; or in the case
   * of no explicit active profiles, whether one of the given profile expressions
   * matches the {@linkplain #getDefaultProfiles() default profiles}.
   * <p>Profile expressions allow for complex, boolean profile logic to be
   * expressed &mdash; for example {@code "p1 & p2"}, {@code "(p1 & p2) | p3"},
   * etc. See {@link Profiles#of(String...)} for details on the supported
   * expression syntax.
   * <p>This method is a convenient shortcut for
   * {@code env.acceptsProfiles(Profiles.of(profileExpressions))}.
   *
   * @see Profiles#of(String...)
   * @see #acceptsProfiles(Profiles)
   */
  default boolean matchesProfiles(String... profileExpressions) {
    return acceptsProfiles(Profiles.of(profileExpressions));
  }

  /**
   * Determine whether the given {@link Profiles} predicate matches the
   * {@linkplain #getActiveProfiles() active profiles} &mdash; or in the case
   * of no explicit active profiles, whether the given {@code Profiles} predicate
   * matches the {@linkplain #getDefaultProfiles() default profiles}.
   * <p>If you wish provide profile expressions directly as strings, use
   * {@link #matchesProfiles(String...)} instead.
   *
   * @see #matchesProfiles(String...)
   * @see Profiles#of(String...)
   */
  boolean acceptsProfiles(Profiles profiles);

}
