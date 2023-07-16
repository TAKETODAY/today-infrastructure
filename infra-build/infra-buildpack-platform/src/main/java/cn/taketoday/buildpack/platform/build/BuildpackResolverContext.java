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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import cn.taketoday.buildpack.platform.io.IOBiConsumer;

/**
 * Context passed to a {@link BuildpackResolver}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
interface BuildpackResolverContext {

  List<BuildpackMetadata> getBuildpackMetadata();

  BuildpackLayersMetadata getBuildpackLayersMetadata();

  /**
   * Retrieve an image.
   *
   * @param reference the image reference
   * @param type the type of image
   * @return the retrieved image
   * @throws IOException on IO error
   */
  Image fetchImage(ImageReference reference, ImageType type) throws IOException;

  /**
   * Export the layers of an image.
   *
   * @param reference the reference to export
   * @param exports a consumer to receive the layers (contents can only be accessed
   * during the callback)
   * @throws IOException on IO error
   */
  void exportImageLayers(ImageReference reference, IOBiConsumer<String, Path> exports) throws IOException;

}
