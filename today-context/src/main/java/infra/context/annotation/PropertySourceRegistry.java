/*
 * Copyright 2002-present the original author or authors.
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

package infra.context.annotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import infra.context.BootstrapContext;
import infra.core.annotation.MergedAnnotation;
import infra.core.env.ConfigurableEnvironment;
import infra.core.io.PropertySourceDescriptor;
import infra.core.io.PropertySourceFactory;
import infra.core.io.PropertySourceProcessor;
import infra.lang.Assert;
import infra.util.StringUtils;

/**
 * Registry of {@link PropertySource} processed on configuration classes.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertySourceDescriptor
 * @since 4.0
 */
class PropertySourceRegistry {

  private final PropertySourceProcessor propertySourceProcessor;

  private final List<PropertySourceDescriptor> descriptors;

  public PropertySourceRegistry(ConfigurableEnvironment ce, BootstrapContext context) {
    this.propertySourceProcessor = new BootstrapPropertySourceProcessor(ce, context);
    this.descriptors = new ArrayList<>();
  }

  public List<PropertySourceDescriptor> getDescriptors() {
    return Collections.unmodifiableList(this.descriptors);
  }

  /**
   * Process the given <code>@PropertySource</code> annotation metadata.
   *
   * @param propertySource metadata for the <code>@PropertySource</code> annotation found
   * @throws IOException if loading a property source failed
   */
  public void processPropertySource(MergedAnnotation<PropertySource> propertySource) throws IOException {
    String name = propertySource.getString("name");
    if (StringUtils.isEmpty(name)) {
      name = null;
    }
    String encoding = propertySource.getString("encoding");
    if (StringUtils.isEmpty(encoding)) {
      encoding = null;
    }
    String[] locations = propertySource.getStringArray("value");
    Assert.isTrue(locations.length > 0, "At least one @PropertySource(value) location is required");
    boolean ignoreResourceNotFound = propertySource.getBoolean("ignoreResourceNotFound");

    Class<? extends PropertySourceFactory> factoryClass = propertySource.getClass("factory");
    Class<? extends PropertySourceFactory> factorClassToUse =
            (factoryClass != PropertySourceFactory.class ? factoryClass : null);
    var descriptor = new PropertySourceDescriptor(Arrays.asList(locations),
            ignoreResourceNotFound, name, factorClassToUse, encoding);
    this.propertySourceProcessor.processPropertySource(descriptor);
    this.descriptors.add(descriptor);
  }

  private static final class BootstrapPropertySourceProcessor extends PropertySourceProcessor {

    private final BootstrapContext context;

    public BootstrapPropertySourceProcessor(ConfigurableEnvironment environment, BootstrapContext context) {
      super(environment, context.getResourceLoader());
      this.context = context;
    }

    @Override
    protected PropertySourceFactory getDefaultPropertySourceFactory() {
      return context.getPropertySourceFactory();
    }

    @Override
    protected PropertySourceFactory instantiateClass(Class<? extends PropertySourceFactory> type) {
      return context.instantiate(type);
    }

  }

}
