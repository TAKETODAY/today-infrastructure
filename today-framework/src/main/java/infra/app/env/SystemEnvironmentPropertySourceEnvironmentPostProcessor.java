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

package infra.app.env;

import java.util.Map;

import infra.app.Application;
import infra.core.Ordered;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.PropertySource;
import infra.core.env.StandardEnvironment;
import infra.core.env.SystemEnvironmentPropertySource;
import infra.lang.Nullable;
import infra.origin.Origin;
import infra.origin.OriginLookup;
import infra.origin.SystemEnvironmentOrigin;
import infra.util.StringUtils;

/**
 * An {@link EnvironmentPostProcessor} that replaces the systemEnvironment
 * {@link SystemEnvironmentPropertySource} with an
 * {@link OriginAwareSystemEnvironmentPropertySource} that can track the
 * {@link SystemEnvironmentOrigin} for every system environment property.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class SystemEnvironmentPropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

  /**
   * The default order for the processor.
   */
  public static final int DEFAULT_ORDER = ApplicationJsonEnvironmentPostProcessor.DEFAULT_ORDER - 1;

  private int order = DEFAULT_ORDER;

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, Application application) {
    String sourceName = StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
    PropertySource<?> propertySource = environment.getPropertySources().get(sourceName);
    if (propertySource != null) {
      replacePropertySource(environment, sourceName, propertySource, application.getEnvironmentPrefix());
    }
  }

  @SuppressWarnings("unchecked")
  private void replacePropertySource(ConfigurableEnvironment environment, String sourceName,
          PropertySource<?> propertySource, @Nullable String environmentPrefix) {
    Map<String, Object> originalSource = (Map<String, Object>) propertySource.getSource();
    SystemEnvironmentPropertySource source = new OriginAwareSystemEnvironmentPropertySource(sourceName,
            originalSource, environmentPrefix);
    environment.getPropertySources().replace(sourceName, source);
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  /**
   * {@link SystemEnvironmentPropertySource} that also tracks {@link Origin}.
   */
  protected static class OriginAwareSystemEnvironmentPropertySource
          extends SystemEnvironmentPropertySource implements OriginLookup<String> {

    @Nullable
    private final String prefix;

    OriginAwareSystemEnvironmentPropertySource(String name, Map<String, Object> source, @Nullable String environmentPrefix) {
      super(name, source);
      this.prefix = determinePrefix(environmentPrefix);
    }

    @Nullable
    private String determinePrefix(@Nullable String environmentPrefix) {
      if (StringUtils.isBlank(environmentPrefix)) {
        return null;
      }
      if (environmentPrefix.endsWith(".") || environmentPrefix.endsWith("_") || environmentPrefix.endsWith("-")) {
        return environmentPrefix.substring(0, environmentPrefix.length() - 1);
      }
      return environmentPrefix;
    }

    @Override
    public boolean containsProperty(String name) {
      return super.containsProperty(name);
    }

    @Nullable
    @Override
    public Object getProperty(String name) {
      return super.getProperty(name);
    }

    @Nullable
    @Override
    public Origin getOrigin(String key) {
      String property = resolvePropertyName(key);
      if (super.containsProperty(property)) {
        return new SystemEnvironmentOrigin(property);
      }
      return null;
    }

    @Nullable
    @Override
    public String getPrefix() {
      return this.prefix;
    }

    @Override
    public boolean isImmutable() {
      return (Object) getSource() == System.getenv();
    }

  }

}
