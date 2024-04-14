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

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;

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

  private final PatternResourceLoader patternResourceLoader;

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
   * @param patternResourceLoader the PatternResourceLoader to use
   * @param propertyResolver the PropertyResolver to use
   */
  public ResourceArrayPropertyEditor(
          PatternResourceLoader patternResourceLoader, @Nullable PropertyResolver propertyResolver) {

    this(patternResourceLoader, propertyResolver, true);
  }

  /**
   * Create a new ResourceArrayPropertyEditor with the given {@link PatternResourceLoader}
   * and {@link PropertyResolver} (typically an {@link Environment}).
   *
   * @param patternResourceLoader the PatternResourceLoader to use
   * @param propertyResolver the PropertyResolver to use
   * @param ignoreUnresolvablePlaceholders whether to ignore unresolvable placeholders
   * if no corresponding system property could be found
   */
  public ResourceArrayPropertyEditor(PatternResourceLoader patternResourceLoader,
          @Nullable PropertyResolver propertyResolver, boolean ignoreUnresolvablePlaceholders) {

    Assert.notNull(patternResourceLoader, "PatternResourceLoader is required");
    this.patternResourceLoader = patternResourceLoader;
    this.propertyResolver = propertyResolver;
    this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
  }

  /**
   * Treat the given text as a location pattern or comma delimited location patterns
   * and convert it to a Resource array.
   */
  @Override
  public void setAsText(String text) {
    String pattern = resolvePath(text).trim();
    String[] locationPatterns = StringUtils.commaDelimitedListToStringArray(pattern);

    LinkedHashSet<Resource> resources = new LinkedHashSet<>();
    if (locationPatterns.length == 1) {
      scan(locationPatterns[0], resources);
    }
    else {
      for (String locationPattern : locationPatterns) {
        scan(locationPattern, resources);
      }
    }
    setValue(resources.toArray(Resource.EMPTY_ARRAY));
  }

  private void scan(String locationPattern, LinkedHashSet<Resource> resources) {
    try {
      patternResourceLoader.scan(locationPattern.trim(), resources::add);
    }
    catch (IOException ex) {
      throw new IllegalArgumentException(
              "Could not resolve resource location pattern [" + locationPattern.trim() + "]: " + ex.getMessage());
    }
  }

  /**
   * Treat the given value as a collection or array and convert it to a Resource array.
   * <p>Considers String elements as location patterns and takes Resource elements as-is.
   */
  @Override
  public void setValue(Object value) throws IllegalArgumentException {
    if (value instanceof Collection || (value instanceof Object[] && !(value instanceof Resource[]))) {
      Collection<?> input = (value instanceof Collection<?> collection ? collection : Arrays.asList((Object[]) value));
      LinkedHashSet<Resource> merged = new LinkedHashSet<>();
      for (Object element : input) {
        if (element instanceof String path) {
          // A location pattern: resolve it into a Resource array.
          // Might point to a single resource or to multiple resources.
          String pattern = resolvePath(path.trim());
          try {
            Resource[] resources = this.patternResourceLoader.getResourcesArray(pattern);
            Collections.addAll(merged, resources);
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
