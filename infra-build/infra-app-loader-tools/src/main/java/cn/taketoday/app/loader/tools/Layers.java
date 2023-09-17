/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.tools;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Interface to provide information about layers to the {@link Repackager}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Layer
 * @since 4.0
 */
public interface Layers extends Iterable<Layer> {

  /**
   * The default layer resolver.
   */
  Layers IMPLICIT = new ImplicitLayerResolver();

  /**
   * Return the jar layers in the order that they should be added (starting with the
   * least frequently changed layer).
   *
   * @return the layers iterator
   */
  @Override
  Iterator<Layer> iterator();

  /**
   * Return a stream of the jar layers in the order that they should be added (starting
   * with the least frequently changed layer).
   *
   * @return the layers stream
   */
  Stream<Layer> stream();

  /**
   * Return the layer that contains the given resource name.
   *
   * @param applicationResource the name of an application resource (for example a
   * {@code .class} file).
   * @return the layer that contains the resource (must never be {@code null})
   */
  Layer getLayer(String applicationResource);

  /**
   * Return the layer that contains the given library.
   *
   * @param library the library to consider
   * @return the layer that contains the resource (must never be {@code null})
   */
  Layer getLayer(Library library);

}
