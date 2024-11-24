/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.app.test.web;

import java.util.Objects;

import infra.app.Application;
import infra.app.env.EnvironmentPostProcessor;
import infra.core.conversion.ConversionFailedException;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;
import infra.test.context.support.TestPropertySourceUtils;
import infra.util.ClassUtils;

/**
 * {@link EnvironmentPostProcessor} implementation to start the management context on a
 * random port if the main server's port is 0 and the management context is expected on a
 * different port.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class InfraTestRandomPortEnvironmentPostProcessor implements EnvironmentPostProcessor {

  private static final String MANAGEMENT_PORT_PROPERTY = "management.server.port";

  private static final String SERVER_PORT_PROPERTY = "server.port";

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, Application application) {
    MapPropertySource source = (MapPropertySource) environment.getPropertySources()
            .get(TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
    if (source == null || isTestServerPortFixed(source, environment) || isTestManagementPortConfigured(source)) {
      return;
    }
    Integer managementPort = getPropertyAsInteger(environment, MANAGEMENT_PORT_PROPERTY, null);
    if (managementPort == null || managementPort.equals(-1) || managementPort.equals(0)) {
      return;
    }
    Integer serverPort = getPropertyAsInteger(environment, SERVER_PORT_PROPERTY, 8080);
    if (managementPort.equals(serverPort)) {
      source.getSource().put(MANAGEMENT_PORT_PROPERTY, "");
    }
    else {
      source.getSource().put(MANAGEMENT_PORT_PROPERTY, "0");
    }
  }

  private boolean isTestServerPortFixed(MapPropertySource source, ConfigurableEnvironment environment) {
    return !Integer.valueOf(0).equals(getPropertyAsInteger(source, SERVER_PORT_PROPERTY, environment));
  }

  private boolean isTestManagementPortConfigured(PropertySource<?> source) {
    return source.getProperty(MANAGEMENT_PORT_PROPERTY) != null;
  }

  private Integer getPropertyAsInteger(ConfigurableEnvironment environment, String property, Integer defaultValue) {
    return environment.getPropertySources()
            .stream()
            .filter(source -> !source.getName().equals(TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME))
            .map(source -> getPropertyAsInteger(source, property, environment))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(defaultValue);
  }

  private Integer getPropertyAsInteger(PropertySource<?> source,
          String property, ConfigurableEnvironment environment) {
    Object value = source.getProperty(property);
    if (value == null) {
      return null;
    }
    if (ClassUtils.isAssignableValue(Integer.class, value)) {
      return (Integer) value;
    }
    try {
      return environment.getConversionService().convert(value, Integer.class);
    }
    catch (ConversionFailedException ex) {
      if (value instanceof String) {
        return getResolvedValueIfPossible(environment, (String) value);
      }
      throw ex;
    }
  }

  private Integer getResolvedValueIfPossible(ConfigurableEnvironment environment, String value) {
    String resolvedValue = environment.resolveRequiredPlaceholders(value);
    return environment.getConversionService().convert(resolvedValue, Integer.class);
  }

}
