/*
 * Copyright 2012-present the original author or authors.
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

package infra.context.properties.source;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

import infra.core.env.ConfigurableEnvironment;
import infra.core.env.PropertySource;
import infra.core.env.PropertySource.StubPropertySource;
import infra.core.env.PropertySources;
import infra.lang.Assert;
import infra.origin.OriginLookup;
import infra.util.ConcurrentReferenceHashMap;
import infra.util.ConcurrentReferenceHashMap.ReferenceType;

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
