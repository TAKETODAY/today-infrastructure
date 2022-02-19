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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties.bind;

import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.util.Assert;
import cn.taketoday.util.PropertyPlaceholderHelper;
import cn.taketoday.util.SystemPropertyUtils;

/**
 * {@link PlaceholdersResolver} to resolve placeholders from {@link PropertySources}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
public class PropertySourcesPlaceholdersResolver implements PlaceholdersResolver {

  private final Iterable<PropertySource<?>> sources;

  private final PropertyPlaceholderHelper helper;

  public PropertySourcesPlaceholdersResolver(Environment environment) {
    this(getSources(environment), null);
  }

  public PropertySourcesPlaceholdersResolver(Iterable<PropertySource<?>> sources) {
    this(sources, null);
  }

  public PropertySourcesPlaceholdersResolver(Iterable<PropertySource<?>> sources, PropertyPlaceholderHelper helper) {
    this.sources = sources;
    this.helper = (helper != null) ? helper : new PropertyPlaceholderHelper(SystemPropertyUtils.PLACEHOLDER_PREFIX,
            SystemPropertyUtils.PLACEHOLDER_SUFFIX, SystemPropertyUtils.VALUE_SEPARATOR, true);
  }

  @Override
  public Object resolvePlaceholders(Object value) {
    if (value instanceof String) {
      return this.helper.replacePlaceholders((String) value, this::resolvePlaceholder);
    }
    return value;
  }

  protected String resolvePlaceholder(String placeholder) {
    if (this.sources != null) {
      for (PropertySource<?> source : this.sources) {
        Object value = source.getProperty(placeholder);
        if (value != null) {
          return String.valueOf(value);
        }
      }
    }
    return null;
  }

  private static PropertySources getSources(Environment environment) {
    Assert.notNull(environment, "Environment must not be null");
    Assert.isInstanceOf(ConfigurableEnvironment.class, environment,
            "Environment must be a ConfigurableEnvironment");
    return ((ConfigurableEnvironment) environment).getPropertySources();
  }

}
