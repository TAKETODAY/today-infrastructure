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

package infra.core.testfixture.io;

import infra.core.io.ClassPathResource;

/**
 * Convenience utilities for common operations with test resources.
 *
 * @author Chris Beams
 */
public abstract class ResourceTestUtils {

  /**
   * Load a {@link ClassPathResource} qualified by the simple name of clazz,
   * and relative to the package for clazz.
   * <p>Example: given a clazz 'com.foo.BarTests' and a resourceSuffix of 'context.xml',
   * this method will return a ClassPathResource representing com/foo/BarTests-context.xml
   * <p>Intended for use loading context configuration XML files within JUnit tests.
   */
  public static ClassPathResource qualifiedResource(Class<?> clazz, String resourceSuffix) {
    return new ClassPathResource(String.format("%s-%s", clazz.getSimpleName(), resourceSuffix), clazz);
  }

}
