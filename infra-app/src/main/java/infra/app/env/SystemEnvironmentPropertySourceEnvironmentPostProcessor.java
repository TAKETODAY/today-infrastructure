/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.env;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.app.Application;
import infra.core.Ordered;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.PropertySource;
import infra.core.env.StandardEnvironment;
import infra.core.env.SystemEnvironmentPropertySource;
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
  protected static class OriginAwareSystemEnvironmentPropertySource extends SystemEnvironmentPropertySource implements OriginLookup<String> {

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
