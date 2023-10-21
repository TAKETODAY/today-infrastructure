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

package cn.taketoday.test.context.aot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import cn.taketoday.test.context.aot.samples.basic.BasicInfraJupiterImportedConfigTests;
import cn.taketoday.test.context.aot.samples.basic.BasicInfraJupiterSharedConfigTests;
import cn.taketoday.test.context.aot.samples.basic.BasicInfraJupiterTests;
import cn.taketoday.test.context.aot.samples.basic.BasicInfraVintageTests;
import cn.taketoday.test.context.aot.samples.basic.DisabledInAotProcessingTests;
import cn.taketoday.test.context.aot.samples.basic.DisabledInAotRuntimeClassLevelTests;
import cn.taketoday.test.context.aot.samples.basic.DisabledInAotRuntimeMethodLevelTests;
import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestAotProcessor}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class TestAotProcessorTests extends AbstractAotTests {

  @Test
  void process(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path tempDir) throws Exception {
    // Limit the scope of this test by creating a new classpath root on the fly.
    Path classpathRoot = Files.createDirectories(tempDir.resolve("build/classes"));
    Stream.of(
            BasicInfraJupiterImportedConfigTests.class,
            BasicInfraJupiterSharedConfigTests.class,
            BasicInfraJupiterTests.class,
            BasicInfraJupiterTests.NestedTests.class,
            BasicInfraVintageTests.class,
            DisabledInAotProcessingTests.class,
            DisabledInAotRuntimeClassLevelTests.class,
            DisabledInAotRuntimeMethodLevelTests.class
    ).forEach(testClass -> copy(testClass, classpathRoot));

    Set<Path> classpathRoots = Set.of(classpathRoot);
    Path sourceOutput = tempDir.resolve("generated/sources");
    Path resourceOutput = tempDir.resolve("generated/resources");
    Path classOutput = tempDir.resolve("generated/classes");
    String groupId = "org.example";
    String artifactId = "app-tests";

    TestAotProcessor processor =
            new DemoTestAotProcessor(classpathRoots, sourceOutput, resourceOutput, classOutput, groupId, artifactId);
    processor.process();

    assertThat(findFiles(sourceOutput)).containsExactlyInAnyOrderElementsOf(expectedSourceFiles());

    assertThat(findFiles(resourceOutput.resolve("META-INF/native-image"))).contains(
            Path.of(groupId, artifactId, "reflect-config.json"),
            Path.of(groupId, artifactId, "resource-config.json"));
  }

  private void copy(Class<?> testClass, Path destination) {
    String classFilename = ClassUtils.convertClassNameToResourcePath(testClass.getName()) + ".class";
    Path source = classpathRoot(testClass).resolve(classFilename);
    Path target = destination.resolve(classFilename);
    try {
      Files.createDirectories(target.getParent());
      Files.copy(source, target);
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static Stream<Path> findFiles(Path directory) throws IOException {
    return Files.walk(directory).filter(Files::isRegularFile)
            .map(path -> path.subpath(directory.getNameCount(), path.getNameCount()));
  }

  private static List<Path> expectedSourceFiles() {
    return Arrays.stream(expectedSourceFilesForBasicInfraTests).map(Path::of).toList();
  }

  private static class DemoTestAotProcessor extends TestAotProcessor {

    DemoTestAotProcessor(Set<Path> classpathRoots, Path sourceOutput, Path resourceOutput, Path classOutput,
            String groupId, String artifactId) {
      super(classpathRoots, createSettings(sourceOutput, resourceOutput, classOutput, groupId, artifactId));
    }

    private static Settings createSettings(Path sourceOutput, Path resourceOutput, Path classOutput, String groupId,
            String artifactId) {
      return Settings.builder()
              .sourceOutput(sourceOutput)
              .resourceOutput(resourceOutput)
              .classOutput(classOutput)
              .artifactId(artifactId)
              .groupId(groupId)
              .build();
    }
  }

}

