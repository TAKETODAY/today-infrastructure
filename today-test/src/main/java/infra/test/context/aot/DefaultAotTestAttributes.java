/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.context.aot;

import java.util.Map;

import infra.aot.AotDetector;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * Default implementation of {@link AotTestAttributes} backed by a {@link Map}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultAotTestAttributes implements AotTestAttributes {

  private final Map<String, String> attributes;

  DefaultAotTestAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  @Override
  public void setAttribute(String name, String value) {
    assertNotInAotRuntime();
    Assert.notNull(value, "'value' is required");
    Assert.isTrue(!this.attributes.containsKey(name),
            () -> "AOT attributes cannot be overridden. Name '%s' is already in use.".formatted(name));
    this.attributes.put(name, value);
  }

  @Override
  public void removeAttribute(String name) {
    assertNotInAotRuntime();
    this.attributes.remove(name);
  }

  @Override
  @Nullable
  public String getString(String name) {
    return this.attributes.get(name);
  }

  private static void assertNotInAotRuntime() {
    if (AotDetector.useGeneratedArtifacts()) {
      throw new UnsupportedOperationException(
              "AOT attributes cannot be modified during AOT run-time execution");
    }
  }

}
