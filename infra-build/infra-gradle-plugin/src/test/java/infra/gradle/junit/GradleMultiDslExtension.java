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

package infra.gradle.junit;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import infra.gradle.testkit.Dsl;
import infra.gradle.testkit.GradleBuild;
import infra.gradle.testkit.GradleBuildExtension;
import infra.gradle.testkit.GradleVersions;

/**
 * {@link Extension} that runs {@link TestTemplate templated tests} against the Groovy and
 * Kotlin DSLs. Test classes using the extension most have a non-private non-final
 * {@link GradleBuild} field named {@code gradleBuild}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class GradleMultiDslExtension implements TestTemplateInvocationContextProvider {

  @Override
  public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
    return Stream.of(Dsl.values()).map(DslTestTemplateInvocationContext::new);
  }

  @Override
  public boolean supportsTestTemplate(ExtensionContext context) {
    return true;
  }

  private static final class DslTestTemplateInvocationContext implements TestTemplateInvocationContext {

    private final Dsl dsl;

    DslTestTemplateInvocationContext(Dsl dsl) {
      this.dsl = dsl;
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
      GradleBuild gradleBuild = new GradleBuild(this.dsl);
      gradleBuild.gradleVersion(GradleVersions.minimumCompatible());
      return Arrays.asList(new GradleBuildFieldSetter(gradleBuild), new GradleBuildExtension());
    }

    @Override
    public String getDisplayName(int invocationIndex) {
      return this.dsl.getName();
    }

  }

}
