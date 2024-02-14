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

package cn.taketoday.util;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Stream Iterable
 * <p>
 * just use once
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/14 18:46
 */
public class StreamIterable<T> implements Iterable<T> {
  private final Stream<T> stream;

  public StreamIterable(Stream<T> stream) {
    this.stream = stream;
  }

  @Override
  public Iterator<T> iterator() {
    return stream.iterator();
  }

  @Override
  public void forEach(Consumer<? super T> action) {
    stream.forEach(action);
  }

  @Override
  public Spliterator<T> spliterator() {
    return stream.spliterator();
  }

}
