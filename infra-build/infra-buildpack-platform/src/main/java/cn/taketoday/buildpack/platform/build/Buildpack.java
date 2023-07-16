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

package cn.taketoday.buildpack.platform.build;

import cn.taketoday.buildpack.platform.docker.type.Layer;
import cn.taketoday.buildpack.platform.io.IOConsumer;

import java.io.IOException;

/**
 * A Buildpack that should be invoked by the builder during image building.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BuildpackResolver
 * @since 4.0
 */
interface Buildpack {

  /**
   * Return the coordinates of the builder.
   *
   * @return the builder coordinates
   */
  BuildpackCoordinates getCoordinates();

  /**
   * Apply the necessary buildpack layers.
   *
   * @param layers a consumer that should accept the layers
   * @throws IOException on IO error
   */
  void apply(IOConsumer<Layer> layers) throws IOException;

}
