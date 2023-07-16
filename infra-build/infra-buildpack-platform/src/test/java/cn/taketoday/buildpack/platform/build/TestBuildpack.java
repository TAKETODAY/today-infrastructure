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

import cn.taketoday.buildpack.platform.docker.type.Layer;
import cn.taketoday.buildpack.platform.io.Content;
import cn.taketoday.buildpack.platform.io.IOConsumer;
import cn.taketoday.buildpack.platform.io.Layout;
import cn.taketoday.buildpack.platform.io.Owner;

/**
 * A test {@link Buildpack}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
class TestBuildpack implements Buildpack {

  private final BuildpackCoordinates coordinates;

  TestBuildpack(String id, String version) {
    this.coordinates = BuildpackCoordinates.of(id, version);
  }

  @Override
  public BuildpackCoordinates getCoordinates() {
    return this.coordinates;
  }

  @Override
  public void apply(IOConsumer<Layer> layers) throws IOException {
    layers.accept(Layer.of(this::getContent));
  }

  private void getContent(Layout layout) throws IOException {
    String id = this.coordinates.getSanitizedId();
    String dir = "/cnb/buildpacks/" + id + "/" + this.coordinates.getVersion();
    layout.file(dir + "/buildpack.toml", Owner.ROOT, Content.of("[test]"));
  }

}
