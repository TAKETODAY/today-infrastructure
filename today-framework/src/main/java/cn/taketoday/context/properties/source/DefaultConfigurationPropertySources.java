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

package cn.taketoday.context.properties.source;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySource.StubPropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.origin.OriginLookup;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ConcurrentReferenceHashMap.ReferenceType;

/**
 * Adapter to convert Framework's {@link PropertySources} to
 * {@link ConfigurationPropertySource ConfigurationPropertySources}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultConfigurationPropertySources implements Iterable<ConfigurationPropertySource> {

  private final Iterable<PropertySource<?>> sources;

  private final ConcurrentReferenceHashMap<PropertySource<?>, ConfigurationPropertySource> cache
          = new ConcurrentReferenceHashMap<>(16, ReferenceType.SOFT);

  DefaultConfigurationPropertySources(Iterable<PropertySource<?>> sources) {
    Assert.notNull(sources, "Sources is required");
    this.sources = sources;
  }

  boolean isUsingSources(Iterable<PropertySource<?>> sources) {
    return this.sources == sources;
  }

  @Override
  public Iterator<ConfigurationPropertySource> iterator() {
    return new SourcesIterator(this.sources.iterator(), this::adapt);
  }

  private ConfigurationPropertySource adapt(PropertySource<?> source) {
    ConfigurationPropertySource result = this.cache.get(source);
    // Most PropertySources test equality only using the source name, so we need to
    // check the actual source hasn't also changed.
    if (result != null && result.getUnderlyingSource() == source) {
      return result;
    }
    result = DefaultConfigurationPropertySource.from(source);
    if (source instanceof OriginLookup) {
      result = result.withPrefix(((OriginLookup<?>) source).getPrefix());
    }
    this.cache.put(source, result);
    return result;
  }

  private static class SourcesIterator implements Iterator<ConfigurationPropertySource> {
    private final ArrayDeque<Iterator<PropertySource<?>>> iterators;
    private final Function<PropertySource<?>, ConfigurationPropertySource> adapter;

    @Nullable
    private ConfigurationPropertySource next;

    SourcesIterator(Iterator<PropertySource<?>> iterator, Function<PropertySource<?>, ConfigurationPropertySource> adapter) {
      this.iterators = new ArrayDeque<>(4);
      this.iterators.push(iterator);
      this.adapter = adapter;
    }

    @Override
    public boolean hasNext() {
      return fetchNext() != null;
    }

    @Override
    public ConfigurationPropertySource next() {
      ConfigurationPropertySource next = fetchNext();
      if (next == null) {
        throw new NoSuchElementException();
      }
      this.next = null;
      return next;
    }

    @Nullable
    private ConfigurationPropertySource fetchNext() {
      if (this.next == null) {
        if (this.iterators.isEmpty()) {
          return null;
        }
        if (!this.iterators.peek().hasNext()) {
          this.iterators.pop();
          return fetchNext();
        }
        PropertySource<?> candidate = this.iterators.peek().next();
        if (candidate.getSource() instanceof ConfigurableEnvironment) {
          push((ConfigurableEnvironment) candidate.getSource());
          return fetchNext();
        }
        if (isIgnored(candidate)) {
          return fetchNext();
        }
        this.next = this.adapter.apply(candidate);
      }
      return this.next;
    }

    private void push(ConfigurableEnvironment environment) {
      this.iterators.push(environment.getPropertySources().iterator());
    }

    private boolean isIgnored(PropertySource<?> candidate) {
      return candidate instanceof StubPropertySource
              || candidate instanceof ConfigurationPropertySourcesPropertySource;
    }

  }

}
