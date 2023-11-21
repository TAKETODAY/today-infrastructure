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

package cn.taketoday.gradle.junit;

import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.internal.nativeintegration.services.NativeServices;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testfixtures.internal.ProjectBuilderImpl;

import java.io.File;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * Helper class to build Gradle {@link Project Projects} for test fixtures. Wraps
 * functionality of Gradle's own {@link ProjectBuilder} in order to work around an issue
 * on JDK 17 and 18.
 *
 * @author Christoph Dreis
 * @see <a href="https://github.com/gradle/gradle/issues/16857">Gradle Support JDK 17</a>
 */
public final class GradleProjectBuilder {

  private File projectDir;

  private String name;

  private GradleProjectBuilder() {
  }

  public static GradleProjectBuilder builder() {
    return new GradleProjectBuilder();
  }

  public GradleProjectBuilder withProjectDir(File dir) {
    this.projectDir = dir;
    return this;
  }

  public GradleProjectBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public Project build() {
    Assert.notNull(this.projectDir, "ProjectDir is required");
    ProjectBuilder builder = ProjectBuilder.builder();
    builder.withProjectDir(this.projectDir);
    File userHome = new File(this.projectDir, "userHome");
    builder.withGradleUserHomeDir(userHome);
    if (StringUtils.hasText(this.name)) {
      builder.withName(this.name);
    }
    if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
      NativeServices.initializeOnClient(userHome);
      try {
        ProjectBuilderImpl.getGlobalServices();
      }
      catch (Throwable ignore) {
      }
    }
    return builder.build();
  }

}
