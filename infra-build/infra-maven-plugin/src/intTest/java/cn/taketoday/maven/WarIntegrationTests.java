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

package cn.taketoday.maven;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;

import cn.taketoday.app.loader.tools.FileUtils;
import cn.taketoday.app.loader.tools.JarModeLibrary;
import cn.taketoday.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Maven plugin's war support.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
@ExtendWith(MavenBuildExtension.class)
class WarIntegrationTests extends AbstractArchiveIntegrationTests {

  @Override
  protected String getLayersIndexLocation() {
    return "WEB-INF/layers.idx";
  }

  @TestTemplate
  void warRepackaging(MavenBuild mavenBuild) {
    mavenBuild.project("war")
            .execute((project) -> assertThat(jar(new File(project, "target/war-0.0.1.BUILD-SNAPSHOT.war")))
                    .hasEntryWithNameStartingWith("WEB-INF/lib/today-context")
                    .hasEntryWithNameStartingWith("WEB-INF/lib/today-core")
                    .hasEntryWithNameStartingWith("WEB-INF/lib-provided/jakarta.servlet-api-6")
                    .hasEntryWithName("cn/taketoday/app/loader/WarLauncher.class")
                    .hasEntryWithName("WEB-INF/classes/org/test/SampleApplication.class")
                    .hasEntryWithName("index.html")
                    .manifest((manifest) -> manifest.hasMainClass("cn.taketoday.app.loader.WarLauncher")
                            .hasStartClass("org.test.SampleApplication")
                            .hasAttribute("Not-Used", "Foo")));
  }

  @TestTemplate
  void jarDependencyWithCustomFinalNameBuiltInSameReactorIsPackagedUsingArtifactIdAndVersion(MavenBuild mavenBuild) {
    mavenBuild.project("war-reactor")
            .execute(((project) -> assertThat(jar(new File(project, "war/target/war-0.0.1.BUILD-SNAPSHOT.war")))
                    .hasEntryWithName("WEB-INF/lib/jar-0.0.1.BUILD-SNAPSHOT.jar")
                    .doesNotHaveEntryWithName("WEB-INF/lib/jar.jar")));
  }

  @TestTemplate
  void whenRequiresUnpackConfigurationIsProvidedItIsReflectedInTheRepackagedWar(MavenBuild mavenBuild) {
    mavenBuild.project("war-with-unpack")
            .execute((project) -> assertThat(jar(new File(project, "target/war-with-unpack-0.0.1.BUILD-SNAPSHOT.war")))
                    .hasUnpackEntryWithNameStartingWith("WEB-INF/lib/today-core-")
                    .hasEntryWithNameStartingWith("WEB-INF/lib/today-context-"));
  }

  @TestTemplate
  void whenWarIsRepackagedWithOutputTimestampConfiguredThenWarIsReproducible(MavenBuild mavenBuild)
          throws InterruptedException {
    String firstHash = buildWarWithOutputTimestamp(mavenBuild);
    Thread.sleep(1500);
    String secondHash = buildWarWithOutputTimestamp(mavenBuild);
    assertThat(firstHash).isEqualTo(secondHash);
  }

  private String buildWarWithOutputTimestamp(MavenBuild mavenBuild) {
    AtomicReference<String> warHash = new AtomicReference<>();
    mavenBuild.project("war-output-timestamp").execute((project) -> {
      File repackaged = new File(project, "target/war-output-timestamp-0.0.1.BUILD-SNAPSHOT.war");
      assertThat(repackaged).isFile();
      long expectedModified = 1584352800000L;
      assertThat(repackaged.lastModified()).isEqualTo(expectedModified);
      long offsetExpectedModified = expectedModified - TimeZone.getDefault().getOffset(expectedModified);
      try (JarFile jar = new JarFile(repackaged)) {
        List<String> unreproducibleEntries = jar.stream()
                .filter((entry) -> entry.getLastModifiedTime().toMillis() != offsetExpectedModified)
                .map((entry) -> entry.getName() + ": " + entry.getLastModifiedTime())
                .toList();
        assertThat(unreproducibleEntries).isEmpty();
        warHash.set(FileUtils.sha1Hash(repackaged));
        FileSystemUtils.deleteRecursively(project);
      }
      catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    });
    return warHash.get();
  }

  @TestTemplate
  void whenWarIsRepackagedWithOutputTimestampConfiguredThenLibrariesAreSorted(MavenBuild mavenBuild) {
    mavenBuild.project("war-output-timestamp").execute((project) -> {
      File repackaged = new File(project, "target/war-output-timestamp-0.0.1.BUILD-SNAPSHOT.war");
      List<String> sortedLibs = Arrays.asList(
              // these libraries are copied from the original war, sorted when
              // packaged by Maven
              "WEB-INF/lib/today-aop", "WEB-INF/lib/today-beans",
              "WEB-INF/lib/today-context", "WEB-INF/lib/today-core",
              // these libraries are contributed by Infra repackaging, and
              // sorted separately
              "WEB-INF/lib/infra-jarmode-layertools");
      assertThat(jar(repackaged)).entryNamesInPath("WEB-INF/lib/")
              .zipSatisfy(sortedLibs,
                      (String jarLib, String expectedLib) -> assertThat(jarLib).startsWith(expectedLib));
    });
  }

  @TestTemplate
  void whenADependencyHasSystemScopeAndInclusionOfSystemScopeDependenciesIsEnabledItIsIncludedInTheRepackagedJar(
          MavenBuild mavenBuild) {
    mavenBuild.project("war-system-scope").execute((project) -> {
      File main = new File(project, "target/war-system-scope-0.0.1.BUILD-SNAPSHOT.war");
      assertThat(jar(main)).hasEntryWithName("WEB-INF/lib-provided/sample-1.0.0.jar");
    });
  }

  @TestTemplate
  void repackagedWarContainsTheLayersIndexByDefault(MavenBuild mavenBuild) {
    mavenBuild.project("war-layered").execute((project) -> {
      File repackaged = new File(project, "war/target/war-layered-0.0.1.BUILD-SNAPSHOT.war");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("WEB-INF/classes/")
              .hasEntryWithNameStartingWith("WEB-INF/lib/jar-release")
              .hasEntryWithNameStartingWith("WEB-INF/lib/jar-snapshot")
              .hasEntryWithNameStartingWith(
                      "WEB-INF/lib/" + JarModeLibrary.LAYER_TOOLS.getCoordinates().getArtifactId());
      try (JarFile jarFile = new JarFile(repackaged)) {
        Map<String, List<String>> layerIndex = readLayerIndex(jarFile);
        assertThat(layerIndex.keySet()).containsExactly("dependencies", "infra-loader",
                "snapshot-dependencies", "application");
        List<String> dependenciesAndSnapshotDependencies = new ArrayList<>();
        dependenciesAndSnapshotDependencies.addAll(layerIndex.get("dependencies"));
        dependenciesAndSnapshotDependencies.addAll(layerIndex.get("snapshot-dependencies"));
        assertThat(layerIndex.get("application")).contains("WEB-INF/lib/jar-release-0.0.1.RELEASE.jar",
                "WEB-INF/lib/jar-snapshot-0.0.1.BUILD-SNAPSHOT.jar");
        assertThat(dependenciesAndSnapshotDependencies)
                .anyMatch((dependency) -> dependency.startsWith("WEB-INF/lib/today-context"));
        assertThat(layerIndex.get("dependencies"))
                .anyMatch((dependency) -> dependency.startsWith("WEB-INF/lib-provided/"));
      }
      catch (IOException ex) {
      }
    });
  }

  @TestTemplate
  void whenWarIsRepackagedWithTheLayersDisabledDoesNotContainLayersIndex(MavenBuild mavenBuild) {
    mavenBuild.project("war-layered-disabled").execute((project) -> {
      File repackaged = new File(project, "war/target/war-layered-0.0.1.BUILD-SNAPSHOT.war");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("WEB-INF/classes/")
              .hasEntryWithNameStartingWith("WEB-INF/lib/jar-release")
              .hasEntryWithNameStartingWith("WEB-INF/lib/jar-snapshot")
              .doesNotHaveEntryWithName("WEB-INF/layers.idx")
              .doesNotHaveEntryWithNameStartingWith("WEB-INF/lib/" + JarModeLibrary.LAYER_TOOLS.getName());
    });
  }

  @TestTemplate
  void whenWarIsRepackagedWithTheLayersEnabledAndLayerToolsExcluded(MavenBuild mavenBuild) {
    mavenBuild.project("war-layered-no-layer-tools").execute((project) -> {
      File repackaged = new File(project, "war/target/war-layered-0.0.1.BUILD-SNAPSHOT.war");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("WEB-INF/classes/")
              .hasEntryWithNameStartingWith("WEB-INF/lib/jar-release")
              .hasEntryWithNameStartingWith("WEB-INF/lib/jar-snapshot")
              .hasEntryWithNameStartingWith("WEB-INF/layers.idx")
              .doesNotHaveEntryWithNameStartingWith("WEB-INF/lib/" + JarModeLibrary.LAYER_TOOLS.getName());
    });
  }

  @TestTemplate
  void whenWarIsRepackagedWithTheCustomLayers(MavenBuild mavenBuild) {
    mavenBuild.project("war-layered-custom").execute((project) -> {
      File repackaged = new File(project, "war/target/war-layered-0.0.1.BUILD-SNAPSHOT.war");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("WEB-INF/classes/")
              .hasEntryWithNameStartingWith("WEB-INF/lib/jar-release")
              .hasEntryWithNameStartingWith("WEB-INF/lib/jar-snapshot");
      try (JarFile jarFile = new JarFile(repackaged)) {
        Map<String, List<String>> layerIndex = readLayerIndex(jarFile);
        assertThat(layerIndex.keySet()).containsExactly("my-dependencies-name", "snapshot-dependencies",
                "configuration", "application");
        assertThat(layerIndex.get("application"))
                .contains("WEB-INF/lib/jar-release-0.0.1.RELEASE.jar",
                        "WEB-INF/lib/jar-snapshot-0.0.1.BUILD-SNAPSHOT.jar",
                        "WEB-INF/lib/jar-classifier-0.0.1-bravo.jar")
                .doesNotContain("WEB-INF/lib/jar-classifier-0.0.1-alpha.jar");
      }
    });
  }

  @TestTemplate
  void repackagedWarContainsClasspathIndex(MavenBuild mavenBuild) {
    mavenBuild.project("war").execute((project) -> {
      File repackaged = new File(project, "target/war-0.0.1.BUILD-SNAPSHOT.war");
      assertThat(jar(repackaged))
              .manifest((manifest) -> manifest.hasAttribute("Infra-App-Classpath-Index", "WEB-INF/classpath.idx"));
      assertThat(jar(repackaged)).hasEntryWithName("WEB-INF/classpath.idx");
      try (JarFile jarFile = new JarFile(repackaged)) {
        List<String> index = readClasspathIndex(jarFile, "WEB-INF/classpath.idx");
        assertThat(index)
                .allMatch((entry) -> entry.startsWith("WEB-INF/lib/") || entry.startsWith("WEB-INF/lib-provided/"));
      }
    });
  }

  @TestTemplate
  void whenEntryIsExcludedItShouldNotBePresentInTheRepackagedWar(MavenBuild mavenBuild) {
    mavenBuild.project("war-exclude-entry").execute((project) -> {
      File war = new File(project, "target/war-exclude-entry-0.0.1.BUILD-SNAPSHOT.war");
      assertThat(jar(war)).hasEntryWithNameStartingWith("WEB-INF/lib/today-context")
              .doesNotHaveEntryWithNameStartingWith("WEB-INF/lib/today-core");
    });
  }

}
