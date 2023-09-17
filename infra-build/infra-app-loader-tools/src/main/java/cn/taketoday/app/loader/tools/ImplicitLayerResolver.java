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

/**
 * Implementation of {@link Layers} that uses implicit rules.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ImplicitLayerResolver extends StandardLayers {

  private static final String INFRA_APP_LOADER_PREFIX = "cn/taketoday/app/loader/";

  @Override
  public Layer getLayer(String name) {
    if (name.startsWith(INFRA_APP_LOADER_PREFIX)) {
      return INFRA_APP_LOADER;
    }
    return APPLICATION;
  }

  @Override
  public Layer getLayer(Library library) {
    if (library.isLocal()) {
      return APPLICATION;
    }
    if (library.getName().contains("SNAPSHOT.")) {
      return SNAPSHOT_DEPENDENCIES;
    }
    return DEPENDENCIES;
  }

}
