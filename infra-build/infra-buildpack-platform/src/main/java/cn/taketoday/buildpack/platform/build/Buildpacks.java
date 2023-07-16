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
import java.util.Collections;
import java.util.List;

import cn.taketoday.buildpack.platform.docker.type.Layer;
import cn.taketoday.buildpack.platform.io.Content;
import cn.taketoday.buildpack.platform.io.IOConsumer;
import cn.taketoday.buildpack.platform.io.Layout;
import cn.taketoday.buildpack.platform.io.Owner;

import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * A collection of {@link Buildpack} instances that can be used to apply buildpack layers.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class Buildpacks {

  static final Buildpacks EMPTY = new Buildpacks(Collections.emptyList());

  private final List<Buildpack> buildpacks;

  private Buildpacks(List<Buildpack> buildpacks) {
    this.buildpacks = buildpacks;
  }

  List<Buildpack> getBuildpacks() {
    return this.buildpacks;
  }

  void apply(IOConsumer<Layer> layers) throws IOException {
    if (!this.buildpacks.isEmpty()) {
      for (Buildpack buildpack : this.buildpacks) {
        buildpack.apply(layers);
      }
      layers.accept(Layer.of(this::addOrderLayerContent));
    }
  }

  void addOrderLayerContent(Layout layout) throws IOException {
    layout.file("/cnb/order.toml", Owner.ROOT, Content.of(getOrderToml()));
  }

  private String getOrderToml() {
    StringBuilder builder = new StringBuilder();
    builder.append("[[order]]\n\n");
    for (Buildpack buildpack : this.buildpacks) {
      appendToOrderToml(builder, buildpack.getCoordinates());
    }
    return builder.toString();
  }

  private void appendToOrderToml(StringBuilder builder, BuildpackCoordinates coordinates) {
    builder.append("  [[order.group]]\n");
    builder.append("    id = \"" + coordinates.getId() + "\"\n");
    if (StringUtils.hasText(coordinates.getVersion())) {
      builder.append("    version = \"" + coordinates.getVersion() + "\"\n");
    }
    builder.append("\n");
  }

  static Buildpacks of(List<Buildpack> buildpacks) {
    return CollectionUtils.isEmpty(buildpacks) ? EMPTY : new Buildpacks(buildpacks);
  }

}
