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

package cn.taketoday.jarmode.layertools;

import java.util.Iterator;
import java.util.zip.ZipEntry;

/**
 * Provides information about the jar layers.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ExtractCommand
 * @see ListCommand
 * @since 4.0
 */
interface Layers extends Iterable<String> {

  /**
   * Return the jar layers in the order that they should be added (starting with the
   * least frequently changed layer).
   */
  @Override
  Iterator<String> iterator();

  /**
   * Return the layer that a given entry is in.
   *
   * @param entry the entry to check
   * @return the layer that the entry is in
   */
  String getLayer(ZipEntry entry);

  /**
   * Return a {@link Layers} instance for the currently running application.
   *
   * @param context the command context
   * @return a new layers instance
   */
  static Layers get(Context context) {
    IndexedLayers indexedLayers = IndexedLayers.get(context);
    if (indexedLayers == null) {
      throw new IllegalStateException("Failed to load layers.idx which is required by layertools");
    }
    return indexedLayers;
  }

}
