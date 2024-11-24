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
