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

package infra.ui.template;

import java.util.List;

import infra.context.properties.bind.Binder;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.io.ResourceLoader;
import infra.util.ClassUtils;

/**
 * Abstract base class for {@link TemplateAvailabilityProvider} implementations that find
 * templates from paths.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class PathBasedTemplateAvailabilityProvider implements TemplateAvailabilityProvider {

  private final String className;

  private final String propertyPrefix;

  private final Class<TemplateAvailabilityProperties> propertiesClass;

  @SuppressWarnings("unchecked")
  public PathBasedTemplateAvailabilityProvider(String className,
          Class<? extends TemplateAvailabilityProperties> propertiesClass, String propertyPrefix) {
    this.className = className;
    this.propertiesClass = (Class<TemplateAvailabilityProperties>) propertiesClass;
    this.propertyPrefix = propertyPrefix;
  }

  @Override
  public boolean isTemplateAvailable(String template, Environment environment,
          ClassLoader classLoader, ResourceLoader resourceLoader) {
    if (ClassUtils.isPresent(this.className, classLoader)) {
      Binder binder = Binder.get((ConfigurableEnvironment) environment);
      var properties = binder.bindOrCreate(this.propertyPrefix, this.propertiesClass);
      return isTemplateAvailable(template, resourceLoader, properties);
    }
    return false;
  }

  private boolean isTemplateAvailable(String view, ResourceLoader resourceLoader, TemplateAvailabilityProperties properties) {
    String location = properties.getPrefix() + view + properties.getSuffix();
    for (String path : properties.getLoaderPath()) {
      if (resourceLoader.getResource(path + location).exists()) {
        return true;
      }
    }
    return false;
  }

  protected abstract static class TemplateAvailabilityProperties {

    private String prefix;

    private String suffix;

    protected TemplateAvailabilityProperties(String prefix, String suffix) {
      this.prefix = prefix;
      this.suffix = suffix;
    }

    protected abstract List<String> getLoaderPath();

    public String getPrefix() {
      return this.prefix;
    }

    public void setPrefix(String prefix) {
      this.prefix = prefix;
    }

    public String getSuffix() {
      return this.suffix;
    }

    public void setSuffix(String suffix) {
      this.suffix = suffix;
    }

  }

}
