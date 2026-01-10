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

package infra.core.io;

import org.jspecify.annotations.Nullable;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

import infra.core.env.Environment;
import infra.core.env.PropertyResolver;
import infra.core.env.StandardEnvironment;
import infra.lang.Assert;
import infra.util.StringUtils;

/**
 * {@link java.beans.PropertyEditor Editor} for {@link Resource}
 * descriptors, to automatically convert {@code String} locations
 * e.g. {@code file:C:/myfile.txt} or {@code classpath:myfile.txt} to
 * {@code Resource} properties instead of using a {@code String} location property.
 *
 * <p>The path may contain {@code ${...}} placeholders, to be
 * resolved as {@link Environment} properties:
 * e.g. {@code ${user.dir}}. Unresolvable placeholders are ignored by default.
 *
 * <p>Delegates to a {@link ResourceLoader} to do the heavy lifting,
 * by default using a {@link DefaultResourceLoader}.
 *
 * @author Juergen Hoeller
 * @author Dave Syer
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Resource
 * @see ResourceLoader
 * @see DefaultResourceLoader
 * @see PropertyResolver#resolvePlaceholders
 * @since 4.0 2022/2/17 17:49
 */
public class ResourceEditor extends PropertyEditorSupport {

  private final ResourceLoader resourceLoader;

  @Nullable
  private PropertyResolver propertyResolver;

  private final boolean ignoreUnresolvablePlaceholders;

  /**
   * Create a new instance of the {@link ResourceEditor} class
   * using a {@link DefaultResourceLoader} and {@link StandardEnvironment}.
   */
  public ResourceEditor() {
    this(new DefaultResourceLoader(), null);
  }

  /**
   * Create a new instance of the {@link ResourceEditor} class
   * using the given {@link ResourceLoader} and {@link PropertyResolver}.
   *
   * @param resourceLoader the {@code ResourceLoader} to use
   * @param propertyResolver the {@code PropertyResolver} to use
   */
  public ResourceEditor(ResourceLoader resourceLoader, @Nullable PropertyResolver propertyResolver) {
    this(resourceLoader, propertyResolver, true);
  }

  /**
   * Create a new instance of the {@link ResourceEditor} class
   * using the given {@link ResourceLoader}.
   *
   * @param resourceLoader the {@code ResourceLoader} to use
   * @param propertyResolver the {@code PropertyResolver} to use
   * @param ignoreUnresolvablePlaceholders whether to ignore unresolvable placeholders
   * if no corresponding property could be found in the given {@code propertyResolver}
   */
  public ResourceEditor(ResourceLoader resourceLoader, @Nullable PropertyResolver propertyResolver,
          boolean ignoreUnresolvablePlaceholders) {
    Assert.notNull(resourceLoader, "ResourceLoader is required");
    this.resourceLoader = resourceLoader;
    this.propertyResolver = propertyResolver;
    this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
  }

  @Override
  public void setAsText(String text) {
    if (StringUtils.hasText(text)) {
      String locationToUse = resolvePath(text).trim();
      setValue(resourceLoader.getResource(locationToUse));
    }
    else {
      setValue(null);
    }
  }

  /**
   * Resolve the given path, replacing placeholders with corresponding
   * property values from the {@code environment} if necessary.
   *
   * @param path the original file path
   * @return the resolved file path
   * @see PropertyResolver#resolvePlaceholders
   * @see PropertyResolver#resolveRequiredPlaceholders
   */
  protected String resolvePath(String path) {
    if (propertyResolver == null) {
      this.propertyResolver = new StandardEnvironment();
    }
    return ignoreUnresolvablePlaceholders
            ? propertyResolver.resolvePlaceholders(path)
            : propertyResolver.resolveRequiredPlaceholders(path);
  }

  @Override
  @Nullable
  public String getAsText() {
    Resource value = (Resource) getValue();
    try {
      // Try to determine URL for resource.
      return (value != null ? value.getURL().toExternalForm() : "");
    }
    catch (IOException ex) {
      // Couldn't determine resource URL - return null to indicate
      // that there is no appropriate text representation.
      return null;
    }
  }

}

