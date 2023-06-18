/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core.test.tools;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import cn.taketoday.lang.Nullable;

/**
 * Internal class used by {@link SourceFiles} and {@link ResourceFiles} to
 * manage {@link DynamicFile} instances.
 *
 * @param <F> the {@link DynamicFile} type
 * @author Phillip Webb
 * @since 4.0
 */
final class DynamicFiles<F extends DynamicFile> implements Iterable<F> {

  private static final DynamicFiles<?> NONE = new DynamicFiles<>(Collections.emptyMap());

  private final Map<String, F> files;

  private DynamicFiles(Map<String, F> files) {
    this.files = files;
  }

  @SuppressWarnings("unchecked")
  static <F extends DynamicFile> DynamicFiles<F> none() {
    return (DynamicFiles<F>) NONE;
  }

  DynamicFiles<F> and(Iterable<F> files) {
    Map<String, F> merged = new LinkedHashMap<>(this.files);
    files.forEach(file -> merged.put(file.getPath(), file));
    return new DynamicFiles<>(Collections.unmodifiableMap(merged));
  }

  DynamicFiles<F> and(F[] files) {
    Map<String, F> merged = new LinkedHashMap<>(this.files);
    Arrays.stream(files).forEach(file -> merged.put(file.getPath(), file));
    return new DynamicFiles<>(Collections.unmodifiableMap(merged));
  }

  DynamicFiles<F> and(DynamicFiles<F> files) {
    Map<String, F> merged = new LinkedHashMap<>(this.files);
    merged.putAll(files.files);
    return new DynamicFiles<>(Collections.unmodifiableMap(merged));
  }

  @Override
  public Iterator<F> iterator() {
    return this.files.values().iterator();
  }

  Stream<F> stream() {
    return this.files.values().stream();
  }

  boolean isEmpty() {
    return this.files.isEmpty();
  }

  @Nullable
  F get(String path) {
    return this.files.get(path);
  }

  F getSingle() {
    return getSingle(candidate -> true);
  }

  F getSingle(Predicate<F> filter) {
    List<F> files = this.files.values().stream().filter(filter).toList();
    if (files.size() != 1) {
      throw new IllegalStateException("No single file available");
    }
    return files.iterator().next();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return this.files.equals(((DynamicFiles<?>) obj).files);
  }

  @Override
  public int hashCode() {
    return this.files.hashCode();
  }

  @Override
  public String toString() {
    return this.files.toString();
  }

}
