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

package cn.taketoday.core.io;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * {@link java.beans.PropertyEditor Editor} for {@link Resource}
 * descriptors, to automatically convert {@code String} locations
 * e.g. {@code file:C:/myfile.txt} or {@code classpath:myfile.txt} to
 * {@code Resource} properties instead of using a {@code String} location property.
 *
 * <p>The path may contain {@code ${...}} placeholders, to be
 * resolved as {@link cn.taketoday.core.env.Environment} properties:
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

