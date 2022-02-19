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
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.PropertyPlaceholderHandler;

/**
 * {@link PlaceholdersResolver} to resolve placeholders from {@link PropertySources}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class PropertySourcesPlaceholdersResolver implements PlaceholdersResolver {

  @Nullable
  private final Iterable<PropertySource<?>> sources;

  private final PropertyPlaceholderHandler helper;

  public PropertySourcesPlaceholdersResolver(Environment environment) {
    this(getSources(environment), null);
  }

  public PropertySourcesPlaceholdersResolver(Iterable<PropertySource<?>> sources) {
    this(sources, null);
  }

  public PropertySourcesPlaceholdersResolver(
          @Nullable Iterable<PropertySource<?>> sources, @Nullable PropertyPlaceholderHandler helper) {
    this.sources = sources;
    this.helper = (helper != null) ? helper : new PropertyPlaceholderHandler(
            PropertyPlaceholderHandler.PLACEHOLDER_PREFIX,
            PropertyPlaceholderHandler.PLACEHOLDER_SUFFIX,
            PropertyPlaceholderHandler.VALUE_SEPARATOR, true
    );
  }

  @Override
  public Object resolvePlaceholders(Object value) {
    if (value instanceof String) {
      return this.helper.replacePlaceholders((String) value, this::resolvePlaceholder);
    }
    return value;
  }

  @Nullable
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
