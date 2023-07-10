/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.tools.layer;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import cn.taketoday.app.loader.tools.Layer;
import cn.taketoday.lang.Assert;

/**
 * {@link ContentSelector} backed by {@code include}/{@code exclude} {@link ContentFilter
 * filters}.
 *
 * @param <T> the content type
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class IncludeExcludeContentSelector<T> implements ContentSelector<T> {

  private final Layer layer;

  private final List<ContentFilter<T>> includes;

  private final List<ContentFilter<T>> excludes;

  public IncludeExcludeContentSelector(Layer layer, List<ContentFilter<T>> includes,
          List<ContentFilter<T>> excludes) {
    this(layer, includes, excludes, Function.identity());
  }

  public <S> IncludeExcludeContentSelector(Layer layer, List<S> includes, List<S> excludes,
          Function<S, ContentFilter<T>> filterFactory) {
    Assert.notNull(layer, "Layer must not be null");
    Assert.notNull(filterFactory, "FilterFactory must not be null");
    this.layer = layer;
    this.includes = (includes != null) ? adapt(includes, filterFactory) : Collections.emptyList();
    this.excludes = (excludes != null) ? adapt(excludes, filterFactory) : Collections.emptyList();
  }

  private <S> List<ContentFilter<T>> adapt(List<S> list, Function<S, ContentFilter<T>> mapper) {
    return list.stream().map(mapper).toList();
  }

  @Override
  public Layer getLayer() {
    return this.layer;
  }

  @Override
  public boolean contains(T item) {
    return isIncluded(item) && !isExcluded(item);
  }

  private boolean isIncluded(T item) {
    if (this.includes.isEmpty()) {
      return true;
    }
    for (ContentFilter<T> include : this.includes) {
      if (include.matches(item)) {
        return true;
      }
    }
    return false;
  }

  private boolean isExcluded(T item) {
    if (this.excludes.isEmpty()) {
      return false;
    }
    for (ContentFilter<T> exclude : this.excludes) {
      if (exclude.matches(item)) {
        return true;
      }
    }
    return false;
  }

}
