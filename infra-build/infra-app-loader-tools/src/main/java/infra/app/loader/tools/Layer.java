/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.loader.tools;

import java.util.Locale;
import java.util.regex.Pattern;

import infra.lang.Assert;

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
    Assert.isTrue(!name.equalsIgnoreCase("ext")
            && !name.toLowerCase(Locale.ROOT).startsWith("infraapp"), () -> "Layer name '" + name + "' is reserved");
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
