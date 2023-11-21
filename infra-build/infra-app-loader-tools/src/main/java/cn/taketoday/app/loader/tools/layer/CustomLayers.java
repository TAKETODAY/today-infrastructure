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

package cn.taketoday.app.loader.tools.layer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import cn.taketoday.app.loader.tools.Layer;
import cn.taketoday.app.loader.tools.Layers;
import cn.taketoday.app.loader.tools.Library;
import cn.taketoday.lang.Assert;

/**
 * Custom {@link Layers} implementation where layer content is selected by the user.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CustomLayers implements Layers {

  private final List<Layer> layers;

  private final List<ContentSelector<String>> applicationSelectors;

  private final List<ContentSelector<Library>> librarySelectors;

  public CustomLayers(List<Layer> layers, List<ContentSelector<String>> applicationSelectors,
          List<ContentSelector<Library>> librarySelectors) {
    Assert.notNull(layers, "Layers is required");
    Assert.notNull(applicationSelectors, "ApplicationSelectors is required");
    validateSelectorLayers(applicationSelectors, layers);
    Assert.notNull(librarySelectors, "LibrarySelectors is required");
    validateSelectorLayers(librarySelectors, layers);
    this.layers = new ArrayList<>(layers);
    this.applicationSelectors = new ArrayList<>(applicationSelectors);
    this.librarySelectors = new ArrayList<>(librarySelectors);
  }

  private static <T> void validateSelectorLayers(List<ContentSelector<T>> selectors, List<Layer> layers) {
    for (ContentSelector<?> selector : selectors) {
      validateSelectorLayers(selector, layers);
    }
  }

  private static void validateSelectorLayers(ContentSelector<?> selector, List<Layer> layers) {
    Layer layer = selector.getLayer();
    Assert.state(layer != null, "Missing content selector layer");
    Assert.state(layers.contains(layer),
            () -> "Content selector layer '" + selector.getLayer() + "' not found in " + layers);
  }

  @Override
  public Iterator<Layer> iterator() {
    return this.layers.iterator();
  }

  @Override
  public Stream<Layer> stream() {
    return this.layers.stream();
  }

  @Override
  public Layer getLayer(String resourceName) {
    return selectLayer(resourceName, this.applicationSelectors, () -> "Resource '" + resourceName + "'");
  }

  @Override
  public Layer getLayer(Library library) {
    return selectLayer(library, this.librarySelectors, () -> "Library '" + library.getName() + "'");
  }

  private <T> Layer selectLayer(T item, List<ContentSelector<T>> selectors, Supplier<String> name) {
    for (ContentSelector<T> selector : selectors) {
      if (selector.contains(item)) {
        return selector.getLayer();
      }
    }
    throw new IllegalStateException(name.get() + " did not match any layer");
  }

}
