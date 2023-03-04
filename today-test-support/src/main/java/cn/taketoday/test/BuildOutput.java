/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.test;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Provides access to build output locations in a build system and IDE agnostic manner.
 *
 * @author Andy Wilkinson
 * @since 4.0
 */
public class BuildOutput {

  private final Class<?> testClass;

  public BuildOutput(Class<?> testClass) {
    this.testClass = testClass;
  }

  /**
   * Returns the location into which test classes have been built.
   *
   * @return test classes location
   */
  public File getTestClassesLocation() {
    try {
      File location = new File(this.testClass.getProtectionDomain().getCodeSource().getLocation().toURI());
      String path = location.getPath();
      if (path.endsWith(path("target", "test-classes"))
              || path.endsWith(path("bin", "test"))
              || path.endsWith(path("bin", "intTest"))
              || path.endsWith(path("build", "classes", "java", "test"))
              || path.endsWith(path("build", "classes", "java", "intTest"))) {
        return location;
      }
      throw new IllegalStateException("Unexpected test classes location '" + location + "'");
    }
    catch (URISyntaxException ex) {
      throw new IllegalStateException("Invalid test class code source location", ex);
    }
  }

  /**
   * Returns the location into which test resources have been built.
   *
   * @return test resources location
   */
  public File getTestResourcesLocation() {
    File testClassesLocation = getTestClassesLocation();
    String path = testClassesLocation.getPath();
    if (path.endsWith(path("target", "test-classes"))
            || path.endsWith(path("bin", "test"))
            || path.endsWith(path("bin", "intTest"))) {
      return testClassesLocation;
    }
    if (path.endsWith(path("build", "classes", "java", "test"))) {
      return new File(testClassesLocation.getParentFile().getParentFile().getParentFile(), "resources/test");
    }
    if (path.endsWith(path("build", "classes", "java", "intTest"))) {
      return new File(testClassesLocation.getParentFile().getParentFile().getParentFile(), "resources/intTest");
    }
    throw new IllegalStateException(
            "Cannot determine test resources location from classes location '" + testClassesLocation + "'");
  }

  /**
   * Returns the root location into which build output is written.
   *
   * @return root location
   */
  public File getRootLocation() {
    return new File("build");
  }

  private String path(String... components) {
    return File.separator + String.join(File.separator, components);
  }

}
