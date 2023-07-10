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

import java.util.regex.Pattern;

import cn.taketoday.lang.Assert;

/**
 * A named layer used to separate the jar when creating a Docker image.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Layers
 * @since 4.0
 */
public class Layer {

  private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z0-9-]+$");

  private final String name;

  /**
   * Create a new {@link Layer} instance with the specified name.
   *
   * @param name the name of the layer.
   */
  public Layer(String name) {
    Assert.hasText(name, "Name must not be empty");
    Assert.isTrue(PATTERN.matcher(name).matches(), () -> "Malformed layer name '" + name + "'");
    Assert.isTrue(!name.equalsIgnoreCase("ext") && !name.toLowerCase().startsWith("springboot"),
            () -> "Layer name '" + name + "' is reserved");
    this.name = name;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return this.name.equals(((Layer) obj).name);
  }

  @Override
  public int hashCode() {
    return this.name.hashCode();
  }

  @Override
  public String toString() {
    return this.name;
  }

}
