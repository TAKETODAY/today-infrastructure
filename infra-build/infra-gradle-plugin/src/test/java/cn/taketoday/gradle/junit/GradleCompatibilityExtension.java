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

package cn.taketoday.gradle.junit;

import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.util.AnnotationUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import cn.taketoday.gradle.testkit.GradleBuild;
import cn.taketoday.gradle.testkit.GradleBuildExtension;
import cn.taketoday.gradle.testkit.GradleVersions;
import cn.taketoday.util.StringUtils;

/**
 * {@link Extension} that runs {@link TestTemplate templated tests} against multiple
 * versions of Gradle. Test classes using the extension must have a non-private and
 * non-final {@link GradleBuild} field named {@code gradleBuild}.
 *
 * @author Andy Wilkinson
 */
final class GradleCompatibilityExtension implements TestTemplateInvocationContextProvider {

  private static final List<String> GRADLE_VERSIONS = GradleVersions.allCompatible();

  @Override
  public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
    Stream<String> gradleVersions = GRADLE_VERSIONS.stream();
    GradleCompatibility gradleCompatibility = AnnotationUtils
            .findAnnotation(context.getRequiredTestClass(), GradleCompatibility.class)
            .get();
    if (StringUtils.hasText(gradleCompatibility.versionsLessThan())) {
      GradleVersion upperExclusive = GradleVersion.version(gradleCompatibility.versionsLessThan());
      gradleVersions = gradleVersions
              .filter((version) -> GradleVersion.version(version).compareTo(upperExclusive) < 0);
    }
    return gradleVersions.flatMap((version) -> {
      List<TestTemplateInvocationContext> invocationContexts = new ArrayList<>();
      invocationContexts.add(new GradleVersionTestTemplateInvocationContext(version, false));
      boolean configurationCache = gradleCompatibility.configurationCache();
      if (configurationCache) {
        invocationContexts.add(new GradleVersionTestTemplateInvocationContext(version, true));
      }
      return invocationContexts.stream();
    });
  }

  @Override
  public boolean supportsTestTemplate(ExtensionContext context) {
    return true;
  }

  private static final class GradleVersionTestTemplateInvocationContext implements TestTemplateInvocationContext {

    private final String gradleVersion;

    private final boolean configurationCache;

    GradleVersionTestTemplateInvocationContext(String gradleVersion, boolean configurationCache) {
      this.gradleVersion = gradleVersion;
      this.configurationCache = configurationCache;
    }

    @Override
    public String getDisplayName(int invocationIndex) {
      return "Gradle " + this.gradleVersion + ((this.configurationCache) ? " --configuration-cache" : "");
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
      GradleBuild gradleBuild = new GradleBuild().gradleVersion(this.gradleVersion);
      if (this.configurationCache) {
        gradleBuild.configurationCache();
      }
      return Arrays.asList(new GradleBuildFieldSetter(gradleBuild), new GradleBuildExtension());
    }

  }

}
