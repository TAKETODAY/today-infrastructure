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

package cn.taketoday.framework;

import java.util.HashSet;
import java.util.Set;

import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.servlet.support.StandardServletEnvironment;

/**
 * Utility class for converting one type of {@link Environment} to another.
 *
 * @author Ethan Rubinson
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/3 14:30
 */
final class EnvironmentConverter {

  private static final String CONFIGURABLE_WEB_ENVIRONMENT_CLASS = "cn.taketoday.web.servlet.ConfigurableWebEnvironment";

  private static final Set<String> SERVLET_ENVIRONMENT_SOURCE_NAMES = Set.of(
          StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME,
          StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME,
          StandardServletEnvironment.JNDI_PROPERTY_SOURCE_NAME
  );

  /**
   * Converts the given {@code environment} to the given {@link StandardEnvironment}
   * type. If the environment is already of the same type, no conversion is performed
   * and it is returned unchanged.
   *
   * @param environment the Environment to convert
   * @param type the type to convert the Environment to
   * @return the converted Environment
   */
  static StandardEnvironment convertIfNecessary(ClassLoader classLoader, ConfigurableEnvironment environment, Class<? extends StandardEnvironment> type) {
    if (type.equals(environment.getClass())) {
      return (StandardEnvironment) environment;
    }
    return convertEnvironment(classLoader, environment, type);
  }

  static StandardEnvironment convertEnvironment(ClassLoader classLoader,
          ConfigurableEnvironment environment, Class<? extends StandardEnvironment> type) {
    StandardEnvironment result = createEnvironment(type);
    result.setActiveProfiles(environment.getActiveProfiles());
    result.setConversionService(environment.getConversionService());
    copyPropertySources(environment, result, classLoader);
    return result;
  }

  static StandardEnvironment createEnvironment(Class<? extends StandardEnvironment> type) {
    try {
      return type.getDeclaredConstructor().newInstance();
    }
    catch (Exception ex) {
      return new StandardEnvironment();
    }
  }

  static void copyPropertySources(ConfigurableEnvironment source, StandardEnvironment target, ClassLoader classLoader) {
    removePropertySources(target.getPropertySources(), isServletEnvironment(target.getClass(), classLoader));
    for (PropertySource<?> propertySource : source.getPropertySources()) {
      if (!SERVLET_ENVIRONMENT_SOURCE_NAMES.contains(propertySource.getName())) {
        target.getPropertySources().addLast(propertySource);
      }
    }
  }

  static boolean isServletEnvironment(Class<?> conversionType, ClassLoader classLoader) {
    try {
      Class<?> webEnvironmentClass = ClassUtils.forName(CONFIGURABLE_WEB_ENVIRONMENT_CLASS, classLoader);
      return webEnvironmentClass.isAssignableFrom(conversionType);
    }
    catch (Throwable ex) {
      return false;
    }
  }

  static void removePropertySources(PropertySources propertySources, boolean isServletEnvironment) {
    HashSet<String> names = new HashSet<>();
    for (PropertySource<?> propertySource : propertySources) {
      names.add(propertySource.getName());
    }
    for (String name : names) {
      if (!isServletEnvironment || !SERVLET_ENVIRONMENT_SOURCE_NAMES.contains(name)) {
        propertySources.remove(name);
      }
    }
  }

}
