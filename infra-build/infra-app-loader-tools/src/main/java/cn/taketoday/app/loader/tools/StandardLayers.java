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

package cn.taketoday.app.loader.tools;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Base class for the standard set of {@link Layers}. Defines the following layers:
 * <ol>
 * <li>"dependencies" - For non snapshot dependencies</li>
 * <li>"infra-app-loader" - For classes from {@code infra-app-loader} used to launch a
 * fat jar</li>
 * <li>"snapshot-dependencies" - For snapshot dependencies</li>
 * <li>"application" - For application classes and resources</li>
 * </ol>
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class StandardLayers implements Layers {

  /**
   * The dependencies layer.
   */
  public static final Layer DEPENDENCIES = new Layer("dependencies");

  /**
   * The spring boot loader layer.
   */
  public static final Layer INFRA_APP_LOADER = new Layer("infra-app-loader");

  /**
   * The snapshot dependencies layer.
   */
  public static final Layer SNAPSHOT_DEPENDENCIES = new Layer("snapshot-dependencies");

  /**
   * The application layer.
   */
  public static final Layer APPLICATION = new Layer("application");

  private static final List<Layer> LAYERS = List.of(
          DEPENDENCIES, INFRA_APP_LOADER, SNAPSHOT_DEPENDENCIES, APPLICATION);

  @Override
  public Iterator<Layer> iterator() {
    return LAYERS.iterator();
  }

  @Override
  public Stream<Layer> stream() {
    return LAYERS.stream();
  }

}
