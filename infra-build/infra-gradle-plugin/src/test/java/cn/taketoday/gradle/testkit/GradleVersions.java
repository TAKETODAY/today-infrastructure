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

package cn.taketoday.gradle.testkit;

import org.gradle.util.GradleVersion;

import java.util.Arrays;
import java.util.List;

/**
 * Versions of Gradle used for testing.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class GradleVersions {

  private GradleVersions() { }

  public static List<String> allCompatible() {
    return Arrays.asList(GradleVersion.current().getVersion(), "8.5", "8.4");
  }

  public static String minimumCompatible() {
    return allCompatible().get(0);
  }

}
