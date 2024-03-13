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

package cn.taketoday.core.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.core.env.CompositePropertySource;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.PlaceholderResolutionException;
import cn.taketoday.util.ReflectionUtils;

/**
 * Contribute {@link PropertySource property sources} to the {@link Environment}.
 *
 * <p>This class is stateful and merges descriptors with the same name in a
 * single {@link PropertySource} rather than creating dedicated ones.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertySourceDescriptor
 * @since 4.0
 */
public class PropertySourceProcessor {

  private static final Logger logger = LoggerFactory.getLogger(PropertySourceProcessor.class);

  private final ConfigurableEnvironment environment;

  private final PatternResourceLoader resourceLoader;

  private final ArrayList<String> propertySourceNames = new ArrayList<>();

  public PropertySourceProcessor(ConfigurableEnvironment environment, ResourceLoader resourceLoader) {
    this.environment = environment;
    this.resourceLoader = PatternResourceLoader.fromResourceLoader(resourceLoader);
  }

  /**
   * Process the specified {@link PropertySourceDescriptor} against the
   * environment managed by this instance.
   *
   * @param descriptor the descriptor to process
   * @throws IOException if loading the properties failed
   */
  public void processPropertySource(PropertySourceDescriptor descriptor) throws IOException {
    String name = descriptor.name();
    String encoding = descriptor.encoding();
    List<String> locations = descriptor.locations();
    Assert.isTrue(!locations.isEmpty(), "At least one @PropertySource(value) location is required");
    boolean ignoreResourceNotFound = descriptor.ignoreResourceNotFound();
    PropertySourceFactory factory = descriptor.propertySourceFactory() != null
            ? instantiateClass(descriptor.propertySourceFactory()) : new DefaultPropertySourceFactory();

    for (String location : locations) {
      try {
        String resolvedLocation = environment.resolveRequiredPlaceholders(location);
        for (Resource resource : resourceLoader.getResources(resolvedLocation)) {
          addPropertySource(factory.createPropertySource(name, new EncodedResource(resource, encoding)));
        }
      }
      catch (RuntimeException | IOException ex) {
        // Placeholders not resolvable or resource not found when trying to open it
        if (ignoreResourceNotFound && (ex instanceof PlaceholderResolutionException || isIgnorableException(ex) ||
                isIgnorableException(ex.getCause()))) {
          if (logger.isInfoEnabled()) {
            logger.info("Properties location [{}] not resolvable: {}", location, ex.getMessage());
          }
        }
        else {
          throw ex;
        }
      }
    }
  }

  private void addPropertySource(cn.taketoday.core.env.PropertySource<?> propertySource) {
    String name = propertySource.getName();
    PropertySources propertySources = environment.getPropertySources();

    if (propertySourceNames.contains(name)) {
      // We've already added a version, we need to extend it
      cn.taketoday.core.env.PropertySource<?> existing = propertySources.get(name);
      if (existing != null) {
        PropertySource<?> newSource = (propertySource instanceof ResourcePropertySource rps ?
                rps.withResourceName() : propertySource);
        if (existing instanceof CompositePropertySource cps) {
          cps.addFirstPropertySource(newSource);
        }
        else {
          if (existing instanceof ResourcePropertySource rps) {
            existing = rps.withResourceName();
          }
          CompositePropertySource composite = new CompositePropertySource(name);
          composite.addPropertySource(newSource);
          composite.addPropertySource(existing);
          propertySources.replace(name, composite);
        }
        return;
      }
    }

    if (propertySourceNames.isEmpty()) {
      propertySources.addLast(propertySource);
    }
    else {
      String firstProcessed = propertySourceNames.get(propertySourceNames.size() - 1);
      propertySources.addBefore(firstProcessed, propertySource);
    }
    propertySourceNames.add(name);
  }

  private static PropertySourceFactory instantiateClass(Class<? extends PropertySourceFactory> type) {
    try {
      Constructor<? extends PropertySourceFactory> constructor = type.getDeclaredConstructor();
      ReflectionUtils.makeAccessible(constructor);
      return constructor.newInstance();
    }
    catch (Exception ex) {
      throw new IllegalStateException("Failed to instantiate " + type, ex);
    }
  }

  /**
   * Determine if the supplied exception can be ignored according to
   * {@code ignoreResourceNotFound} semantics.
   */
  private static boolean isIgnorableException(@Nullable Throwable ex) {
    return ex instanceof FileNotFoundException
            || ex instanceof UnknownHostException
            || ex instanceof SocketException;
  }

}
