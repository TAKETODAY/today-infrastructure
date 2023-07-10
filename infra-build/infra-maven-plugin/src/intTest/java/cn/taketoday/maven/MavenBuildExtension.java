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

package cn.taketoday.maven;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * An {@link Extension} for templated tests that use {@link MavenBuild}. Each templated
 * test is run against multiple versions of Maven.
 *
 * @author Andy Wilkinson
 */
class MavenBuildExtension implements TestTemplateInvocationContextProvider {

  @Override
  public boolean supportsTestTemplate(ExtensionContext context) {
    return true;
  }

  @Override
  public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
    try {
      // Returning a stream which must be closed here is fine, as JUnit will take
      // care of closing it
      return Files.list(Paths.get("build/maven-binaries")).map(MavenVersionTestTemplateInvocationContext::new);
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static final class MavenVersionTestTemplateInvocationContext implements TestTemplateInvocationContext {

    private final Path mavenHome;

    private MavenVersionTestTemplateInvocationContext(Path mavenHome) {
      this.mavenHome = mavenHome;
    }

    @Override
    public String getDisplayName(int invocationIndex) {
      return this.mavenHome.getFileName().toString();
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
      return List.of(new MavenBuildParameterResolver(this.mavenHome));
    }

  }

  private static final class MavenBuildParameterResolver implements ParameterResolver {

    private final Path mavenHome;

    private MavenBuildParameterResolver(Path mavenHome) {
      this.mavenHome = mavenHome;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
      return parameterContext.getParameter().getType().equals(MavenBuild.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
      return new MavenBuild(this.mavenHome.toFile());
    }

  }

}
