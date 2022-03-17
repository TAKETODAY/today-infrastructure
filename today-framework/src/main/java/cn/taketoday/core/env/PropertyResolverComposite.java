/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.env;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CompositeIterator;

/**
 * Composite PropertyResolver implementation
 *
 * @author TODAY 2021/10/5 00:02
 * @since 4.0
 */
public class PropertyResolverComposite implements PropertyResolver, IterablePropertyResolver {
  private final List<PropertyResolver> resolverList;

  public PropertyResolverComposite(PropertyResolver... resolvers) {
    this.resolverList = Arrays.asList(resolvers);
  }

  public PropertyResolverComposite(List<PropertyResolver> resolverList) {
    this.resolverList = resolverList;
  }

  @Override
  public boolean containsProperty(String key) {
    for (PropertyResolver propertyResolver : resolverList) {
      if (propertyResolver.containsProperty(key)) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  @Override
  public String getProperty(String key) {
    for (PropertyResolver propertyResolver : resolverList) {
      String property = propertyResolver.getProperty(key);
      if (property != null) {
        return property;
      }
    }
    return null;
  }

  @Override
  public String getProperty(String key, String defaultValue) {
    for (PropertyResolver propertyResolver : resolverList) {
      String property = propertyResolver.getProperty(key, (String) null);
      if (property != null) {
        return property;
      }
    }
    return defaultValue;
  }

  @Nullable
  @Override
  public <T> T getProperty(String key, Class<T> targetType) {
    for (PropertyResolver propertyResolver : resolverList) {
      T property = propertyResolver.getProperty(key, targetType);
      if (property != null) {
        return property;
      }
    }
    return null;
  }

  @Override
  public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
    for (PropertyResolver propertyResolver : resolverList) {
      T property = propertyResolver.getProperty(key, targetType);
      if (property != null) {
        return property;
      }
    }
    return defaultValue;
  }

  @Override
  public String getRequiredProperty(String key) throws IllegalStateException {
    for (PropertyResolver propertyResolver : resolverList) {
      String property = propertyResolver.getProperty(key);
      if (property != null) {
        return property;
      }
    }
    throw new IllegalStateException("Required key '" + key + "' not found");
  }

  @Override
  public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
    for (PropertyResolver propertyResolver : resolverList) {
      T property = propertyResolver.getProperty(key, targetType);
      if (property != null) {
        return property;
      }
    }
    throw new IllegalStateException("Required key '" + key + "' not found");
  }

  @Override
  public String resolvePlaceholders(String text) {
    for (PropertyResolver propertyResolver : resolverList) {
      text = propertyResolver.resolvePlaceholders(text);
    }
    return text;
  }

  @Override
  public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
    for (PropertyResolver propertyResolver : resolverList) {
      text = propertyResolver.resolveRequiredPlaceholders(text);
    }
    return text;
  }

  // IterablePropertyResolver

  @Override
  public Iterator<String> iterator() {
    CompositeIterator<String> iterator = new CompositeIterator<>();

    for (PropertyResolver propertyResolver : resolverList) {
      if (propertyResolver instanceof IterablePropertyResolver) {
        iterator.add(((IterablePropertyResolver) propertyResolver).iterator());
      }
    }

    return iterator;
  }
}
