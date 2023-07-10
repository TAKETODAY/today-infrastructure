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

package cn.taketoday.infra.maven;

import java.io.File;

/**
 * Layer configuration options.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Layers {

  private boolean enabled = true;

  private boolean includeLayerTools = true;

  private File configuration;

  /**
   * Whether a {@code layers.idx} file should be added to the jar.
   *
   * @return true if a {@code layers.idx} file should be added.
   */
  public boolean isEnabled() {
    return this.enabled;
  }

  /**
   * Whether to include the layer tools jar.
   *
   * @return true if layer tools should be included
   */
  public boolean isIncludeLayerTools() {
    return this.includeLayerTools;
  }

  /**
   * The location of the layers configuration file. If no file is provided, a default
   * configuration is used with four layers: {@code application}, {@code resources},
   * {@code snapshot-dependencies} and {@code dependencies}.
   *
   * @return the layers configuration file
   */
  public File getConfiguration() {
    return this.configuration;
  }

  public void setConfiguration(File configuration) {
    this.configuration = configuration;
  }

}
