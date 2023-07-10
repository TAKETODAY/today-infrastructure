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

import cn.taketoday.app.loader.tools.Layer;

/**
 * Strategy used by {@link CustomLayers} to select the layer of an item.
 *
 * @param <T> the content type
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see IncludeExcludeContentSelector
 * @since 4.0
 */
public interface ContentSelector<T> {

  /**
   * Return the {@link Layer} that the selector represents.
   *
   * @return the named layer
   */
  Layer getLayer();

  /**
   * Returns {@code true} if the specified item is contained in this selection.
   *
   * @param item the item to test
   * @return if the item is contained
   */
  boolean contains(T item);

}
