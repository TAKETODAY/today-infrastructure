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

package infra.test.classpath;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ModifiedClassPathExtension} overriding entries on the class path.
 *
 * @author Christoph Dreis
 */
@ClassPathOverrides("org.assertj:assertj-core:3.25.0")
class ModifiedClassPathExtensionOverridesTests {

  @Test
  void classesAreLoadedFromOverride() {
    assertThat(Assertions.class.getProtectionDomain().getCodeSource().getLocation().toString())
             .endsWith("assertj-core-3.25.0.jar");
  }

}
