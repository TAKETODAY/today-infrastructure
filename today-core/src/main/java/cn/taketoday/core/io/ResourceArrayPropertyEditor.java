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
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Editor for {@link cn.taketoday.core.io.Resource} arrays, to
 * automatically convert {@code String} location patterns
 * (e.g. {@code "file:C:/my*.txt"} or {@code "classpath*:myfile.txt"})
 * to {@code Resource} array properties. Can also translate a collection
 * or array of location patterns into a merged Resource array.
 *
 * <p>A path may contain {@code ${...}} placeholders, to be
 * resolved as {@link cn.taketoday.core.env.Environment} properties:
 * e.g. {@code ${user.dir}}. Unresolvable placeholders are ignored by default.
 *
 * <p>Delegates to a {@link PatternResourceLoader},
 * by default using a {@link PathMatchingPatternResourceLoader}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.core.io.Resource
 * @see PatternResourceLoader
 * @see PathMatchingPatternResourceLoader
 * @since 4.0 2022/2/17 17:50
 */
public class ResourceArrayPropertyEditor extends PropertyEditorSupport {

  private static final Logger logger = LoggerFactory.getLogger(ResourceArrayPropertyEditor.class);

  private final PatternResourceLoader resourcePatternResolver;

  @Nullable
  private PropertyResolver propertyResolver;

  private final boolean ignoreUnresolvablePlaceholders;

  /**
   * Create a new ResourceArrayPropertyEditor with a default
   * {@link PathMatchingPatternResourceLoader} and {@link StandardEnvironment}.
   *
   * @see PathMatchingPatternResourceLoader
   * @see Environment
   */
  public ResourceArrayPropertyEditor() {
    this(new PathMatchingPatternResourceLoader(), null, true);
  }

  /**
   * Create a new ResourceArrayPropertyEditor with the given {@link PatternResourceLoader}
   * and {@link PropertyResolver} (typically an {@link Environment}).
   *
   * @param resourcePatternResolver the ResourcePatternResolver to use
   * @param propertyResolver the PropertyResolver to use
   */
  public ResourceArrayPropertyEditor(
          PatternResourceLoader resourcePatternResolver, @Nullable PropertyResolver propertyResolver) {

    this(resourcePatternResolver, propertyResolver, true);
  }

  /**
   * Create a new ResourceArrayPropertyEditor with the given {@link PatternResourceLoader}
   * and {@link PropertyResolver} (typically an {@link Environment}).
   *
   * @param resourcePatternResolver the ResourcePatternResolver to use
   * @param propertyResolver the PropertyResolver to use
   * @param ignoreUnresolvablePlaceholders whether to ignore unresolvable placeholders
   * if no corresponding system property could be found
   */
  public ResourceArrayPropertyEditor(PatternResourceLoader resourcePatternResolver,
          @Nullable PropertyResolver propertyResolver, boolean ignoreUnresolvablePlaceholders) {

    Assert.notNull(resourcePatternResolver, "ResourcePatternResolver is required");
    this.resourcePatternResolver = resourcePatternResolver;
    this.propertyResolver = propertyResolver;
    this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
  }

  /**
   * Treat the given text as a location pattern and convert it to a Resource array.
   */
  @Override
  public void setAsText(String text) {
    String pattern = resolvePath(text).trim();
    try {
      setValue(this.resourcePatternResolver.getResources(pattern));
    }
    catch (IOException ex) {
      throw new IllegalArgumentException(
              "Could not resolve resource location pattern [" + pattern + "]: " + ex.getMessage());
    }
  }

  /**
   * Treat the given value as a collection or array and convert it to a Resource array.
   * Considers String elements as location patterns and takes Resource elements as-is.
   */
  @Override
  public void setValue(Object value) throws IllegalArgumentException {
    if (value instanceof Collection || (value instanceof Object[] && !(value instanceof Resource[]))) {
      Collection<?> input = (value instanceof Collection ? (Collection<?>) value : Arrays.asList((Object[]) value));
      LinkedHashSet<Resource> merged = new LinkedHashSet<>();
      for (Object element : input) {
        if (element instanceof String path) {
          // A location pattern: resolve it into a Resource array.
          // Might point to a single resource or to multiple resources.
          String pattern = resolvePath(path).trim();
          try {
            Set<Resource> resources = this.resourcePatternResolver.getResources(pattern);
            merged.addAll(resources);
          }
          catch (IOException ex) {
            // ignore - might be an unresolved placeholder or non-existing base directory
            if (logger.isDebugEnabled()) {
              logger.debug("Could not retrieve resources for pattern '{}'", pattern, ex);
            }
          }
        }
        else if (element instanceof Resource resource) {
          // A Resource object: add it to the result.
          merged.add(resource);
        }
        else {
          throw new IllegalArgumentException("Cannot convert element [" + element + "] to [" +
                  Resource.class.getName() + "]: only location String and Resource object supported");
        }
      }
      super.setValue(merged.toArray(Resource.EMPTY_ARRAY));
    }

    else {
      // An arbitrary value: probably a String or a Resource array.
      // setAsText will be called for a String; a Resource array will be used as-is.
      super.setValue(value);
    }
  }

  /**
   * Resolve the given path, replacing placeholders with
   * corresponding system property values if necessary.
   *
   * @param path the original file path
   * @return the resolved file path
   * @see PropertyResolver#resolvePlaceholders
   * @see PropertyResolver#resolveRequiredPlaceholders(String)
   */
  protected String resolvePath(String path) {
    if (this.propertyResolver == null) {
      this.propertyResolver = new StandardEnvironment();
    }
    return (this.ignoreUnresolvablePlaceholders ? this.propertyResolver.resolvePlaceholders(path) :
            this.propertyResolver.resolveRequiredPlaceholders(path));
  }

}
