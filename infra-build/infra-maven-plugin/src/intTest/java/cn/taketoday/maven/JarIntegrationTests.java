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
 * Integration tests for the Maven plugin's jar support.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 */
@ExtendWith(MavenBuildExtension.class)
class JarIntegrationTests extends AbstractArchiveIntegrationTests {

  @Override
  protected String getLayersIndexLocation() {
    return "APP-INF/layers.idx";
  }

  @TestTemplate
  void whenJarIsRepackagedInPlaceOnlyRepackagedJarIsInstalled(MavenBuild mavenBuild) {
    mavenBuild.project("jar").goals("install").execute((project) -> {
      File original = new File(project, "target/jar-0.0.1.BUILD-SNAPSHOT.jar.original");
      assertThat(original).isFile();
      File repackaged = new File(project, "target/jar-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(launchScript(repackaged)).isEmpty();
      assertThat(jar(repackaged)).manifest((manifest) -> {
                manifest.hasMainClass("cn.taketoday.app.loader.JarLauncher");
                manifest.hasStartClass("some.random.Main");
                manifest.hasAttribute("Not-Used", "Foo");
              })
              .hasEntryWithNameStartingWith("APP-INF/lib/today-context")
              .hasEntryWithNameStartingWith("APP-INF/lib/today-core")
              .hasEntryWithNameStartingWith("APP-INF/lib/jakarta.servlet-api-6")
              .hasEntryWithName("APP-INF/classes/org/test/SampleApplication.class")
              .hasEntryWithName("cn/taketoday/app/loader/JarLauncher.class");
      assertThat(buildLog(project))
              .contains("Replacing main artifact " + repackaged + " with repackaged archive,")
              .contains("The original artifact has been renamed to " + original)
              .contains("Installing " + repackaged + " to")
              .doesNotContain("Installing " + original + " to");
    });
  }

  @TestTemplate
  void whenAttachIsDisabledOnlyTheOriginalJarIsInstalled(MavenBuild mavenBuild) {
    mavenBuild.project("jar-attach-disabled").goals("install").execute((project) -> {
      File original = new File(project, "target/jar-attach-disabled-0.0.1.BUILD-SNAPSHOT.jar.original");
      assertThat(original).isFile();
      File main = new File(project, "target/jar-attach-disabled-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(main).isFile();
      assertThat(buildLog(project)).contains("Updating main artifact " + main + " to " + original)
              .contains("Installing " + original + " to")
              .doesNotContain("Installing " + main + " to");
    });
  }

  @TestTemplate
  void whenAClassifierIsConfiguredTheRepackagedJarHasAClassifierAndBothItAndTheOriginalAreInstalled(
          MavenBuild mavenBuild) {
    mavenBuild.project("jar-classifier-main").goals("install").execute((project) -> {
      assertThat(new File(project, "target/jar-classifier-main-0.0.1.BUILD-SNAPSHOT.jar.original"))
              .doesNotExist();
      File main = new File(project, "target/jar-classifier-main-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(main).isFile();
      File repackaged = new File(project, "target/jar-classifier-main-0.0.1.BUILD-SNAPSHOT-test.jar");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("APP-INF/classes/");
      assertThat(buildLog(project))
              .contains("Attaching repackaged archive " + repackaged + " with classifier test")
              .doesNotContain("Creating repackaged archive " + repackaged + " with classifier test")
              .contains("Installing " + main + " to")
              .contains("Installing " + repackaged + " to");
    });
  }

  @TestTemplate
  void whenBothJarsHaveTheSameClassifierRepackagingIsDoneInPlaceAndOnlyRepackagedJarIsInstalled(
          MavenBuild mavenBuild) {
    mavenBuild.project("jar-classifier-source").goals("install").execute((project) -> {
      File original = new File(project, "target/jar-classifier-source-0.0.1.BUILD-SNAPSHOT-test.jar.original");
      assertThat(original).isFile();
      File repackaged = new File(project, "target/jar-classifier-source-0.0.1.BUILD-SNAPSHOT-test.jar");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("APP-INF/classes/");
      assertThat(buildLog(project))
              .contains("Replacing artifact with classifier test " + repackaged + " with repackaged archive,")
              .contains("The original artifact has been renamed to " + original)
              .doesNotContain("Installing " + original + " to")
              .contains("Installing " + repackaged + " to");
    });
  }

  @TestTemplate
  void whenBothJarsHaveTheSameClassifierAndAttachIsDisabledOnlyTheOriginalJarIsInstalled(MavenBuild mavenBuild) {
    mavenBuild.project("jar-classifier-source-attach-disabled").goals("install").execute((project) -> {
      File original = new File(project,
              "target/jar-classifier-source-attach-disabled-0.0.1.BUILD-SNAPSHOT-test.jar.original");
      assertThat(original).isFile();
      File repackaged = new File(project,
              "target/jar-classifier-source-attach-disabled-0.0.1.BUILD-SNAPSHOT-test.jar");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("APP-INF/classes/");
      assertThat(buildLog(project))
              .doesNotContain("Attaching repackaged archive " + repackaged + " with classifier test")
              .contains("Updating artifact with classifier test " + repackaged + " to " + original)
              .contains("Installing " + original + " to")
              .doesNotContain("Installing " + repackaged + " to");
    });
  }

  @TestTemplate
  void whenAClassifierAndAnOutputDirectoryAreConfiguredTheRepackagedJarHasAClassifierAndIsWrittenToTheOutputDirectory(
          MavenBuild mavenBuild) {
    mavenBuild.project("jar-create-dir").goals("install").execute((project) -> {
      File repackaged = new File(project, "target/foo/jar-create-dir-0.0.1.BUILD-SNAPSHOT-foo.jar");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("APP-INF/classes/");
      assertThat(buildLog(project)).contains("Installing " + repackaged + " to");
    });
  }

  @TestTemplate
  void whenAnOutputDirectoryIsConfiguredTheRepackagedJarIsWrittenToIt(MavenBuild mavenBuild) {
    mavenBuild.project("jar-custom-dir").goals("install").execute((project) -> {
      File repackaged = new File(project, "target/foo/jar-custom-dir-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("APP-INF/classes/");
      assertThat(buildLog(project)).contains("Installing " + repackaged + " to");
    });
  }

  @TestTemplate
  void whenACustomLaunchScriptIsConfiguredItAppearsInTheRepackagedJar(MavenBuild mavenBuild) {
    mavenBuild.project("jar-custom-launcher").goals("install").execute((project) -> {
      File repackaged = new File(project, "target/jar-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("APP-INF/classes/");
      assertThat(launchScript(repackaged)).contains("Hello world");
    });
  }

  @TestTemplate
  void whenAnEntryIsExcludedItDoesNotAppearInTheRepackagedJar(MavenBuild mavenBuild) {
    mavenBuild.project("jar-exclude-entry").goals("install").execute((project) -> {
      File repackaged = new File(project, "target/jar-exclude-entry-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("APP-INF/classes/")
              .hasEntryWithNameStartingWith("APP-INF/lib/today-context")
              .hasEntryWithNameStartingWith("APP-INF/lib/today-core")
              .doesNotHaveEntryWithName("APP-INF/lib/servlet-api-2.5.jar");
    });
  }

  @TestTemplate
  void whenAGroupIsExcludedNoEntriesInThatGroupAppearInTheRepackagedJar(MavenBuild mavenBuild) {
    mavenBuild.project("jar-exclude-group").goals("install").execute((project) -> {
      File repackaged = new File(project, "target/jar-exclude-group-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("APP-INF/classes/")
              .hasEntryWithNameStartingWith("APP-INF/lib/today-context")
              .hasEntryWithNameStartingWith("APP-INF/lib/today-core")
              .doesNotHaveEntryWithName("APP-INF/lib/log4j-api-2.4.1.jar");
    });
  }

  @TestTemplate
  void whenAJarIsExecutableItBeginsWithTheDefaultLaunchScript(MavenBuild mavenBuild) {
    mavenBuild.project("jar-executable").execute((project) -> {
      File repackaged = new File(project, "target/jar-executable-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("APP-INF/classes/");
      assertThat(launchScript(repackaged)).contains("Infra Application Startup Script")
              .contains("MyFullyExecutableJarName")
              .contains("MyFullyExecutableJarDesc");
    });
  }

  @TestTemplate
  void whenAJarIsBuiltWithLibrariesWithConflictingNamesTheyAreMadeUniqueUsingTheirGroupIds(MavenBuild mavenBuild) {
    mavenBuild.project("jar-lib-name-conflict").execute((project) -> {
      File repackaged = new File(project, "test-project/target/test-project-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("APP-INF/classes/")
              .hasEntryWithName("APP-INF/lib/cn.taketoday.maven.it-acme-lib-0.0.1.BUILD-SNAPSHOT.jar")
              .hasEntryWithName(
                      "APP-INF/lib/cn.taketoday.maven.it.another-acme-lib-0.0.1.BUILD-SNAPSHOT.jar");
    });
  }

  @TestTemplate
  void whenAProjectUsesPomPackagingRepackagingIsSkipped(MavenBuild mavenBuild) {
    mavenBuild.project("jar-pom").execute((project) -> {
      File target = new File(project, "target");
      assertThat(target.listFiles()).containsExactly(new File(target, "build.log"));
    });
  }

  @TestTemplate
  void whenRepackagingIsSkippedTheJarIsNotRepackaged(MavenBuild mavenBuild) {
    mavenBuild.project("jar-skip").execute((project) -> {
      File main = new File(project, "target/jar-skip-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(jar(main)).doesNotHaveEntryWithNameStartingWith("cn/taketoday");
      assertThat(new File(project, "target/jar-skip-0.0.1.BUILD-SNAPSHOT.jar.original")).doesNotExist();

    });
  }

  @TestTemplate
  void whenADependencyHasSystemScopeAndInclusionOfSystemScopeDependenciesIsEnabledItIsIncludedInTheRepackagedJar(
          MavenBuild mavenBuild) {
    mavenBuild.project("jar-system-scope").execute((project) -> {
      File main = new File(project, "target/jar-system-scope-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(jar(main)).hasEntryWithName("APP-INF/lib/sample-1.0.0.jar");

    });
  }

  @TestTemplate
  void whenADependencyHasSystemScopeItIsNotIncludedInTheRepackagedJar(MavenBuild mavenBuild) {
    mavenBuild.project("jar-system-scope-default").execute((project) -> {
      File main = new File(project, "target/jar-system-scope-default-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(jar(main)).doesNotHaveEntryWithName("APP-INF/lib/sample-1.0.0.jar");

    });
  }

  @TestTemplate
  void whenADependencyHasTestScopeItIsNotIncludedInTheRepackagedJar(MavenBuild mavenBuild) {
    mavenBuild.project("jar-test-scope").execute((project) -> {
      File main = new File(project, "target/jar-test-scope-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(jar(main)).doesNotHaveEntryWithNameStartingWith("APP-INF/lib/log4j")
              .hasEntryWithNameStartingWith("APP-INF/lib/today-");
    });
  }

  @TestTemplate
  void whenAProjectIsBuiltWithALayoutPropertyTheSpecifiedLayoutIsUsed(MavenBuild mavenBuild) {
    mavenBuild.project("jar-with-layout-property")
            .goals("package", "-Dinfra.repackage.layout=ZIP")
            .execute((project) -> {
              File main = new File(project, "target/jar-with-layout-property-0.0.1.BUILD-SNAPSHOT.jar");
              assertThat(jar(main))
                      .manifest((manifest) -> manifest.hasMainClass("cn.taketoday.app.loader.PropertiesLauncher")
                              .hasStartClass("org.test.SampleApplication"));
              assertThat(buildLog(project)).contains("Layout: ZIP");
            });
  }

  @TestTemplate
  void whenALayoutIsConfiguredTheSpecifiedLayoutIsUsed(MavenBuild mavenBuild) {
    mavenBuild.project("jar-with-zip-layout").execute((project) -> {
      File main = new File(project, "target/jar-with-zip-layout-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(jar(main))
              .manifest((manifest) -> manifest.hasMainClass("cn.taketoday.app.loader.PropertiesLauncher")
                      .hasStartClass("org.test.SampleApplication"));
      assertThat(buildLog(project)).contains("Layout: ZIP");
    });
  }

  @TestTemplate
  void whenRequiresUnpackConfigurationIsProvidedItIsReflectedInTheRepackagedJar(MavenBuild mavenBuild) {
    mavenBuild.project("jar-with-unpack").execute((project) -> {
      File main = new File(project, "target/jar-with-unpack-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(jar(main)).hasUnpackEntryWithNameStartingWith("APP-INF/lib/today-core-")
              .hasEntryWithNameStartingWith("APP-INF/lib/today-context-");
    });
  }

  @TestTemplate
  void whenJarIsRepackagedWithACustomLayoutTheJarUsesTheLayout(MavenBuild mavenBuild) {
    mavenBuild.project("jar-custom-layout").execute((project) -> {
      assertThat(jar(new File(project, "custom/target/custom-0.0.1.BUILD-SNAPSHOT.jar")))
              .hasEntryWithName("custom");
      assertThat(jar(new File(project, "default/target/default-0.0.1.BUILD-SNAPSHOT.jar")))
              .hasEntryWithName("sample");
    });
  }

  @TestTemplate
  void repackagedJarContainsTheLayersIndexByDefault(MavenBuild mavenBuild) {
    mavenBuild.project("jar-layered").execute((project) -> {
      File repackaged = new File(project, "jar/target/jar-layered-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("APP-INF/classes/")
              .hasEntryWithNameStartingWith("APP-INF/lib/jar-release")
              .hasEntryWithNameStartingWith("APP-INF/lib/jar-snapshot")
              .hasEntryWithNameStartingWith(
                      "APP-INF/lib/" + JarModeLibrary.LAYER_TOOLS.getCoordinates().getArtifactId());
      try (JarFile jarFile = new JarFile(repackaged)) {
        Map<String, List<String>> layerIndex = readLayerIndex(jarFile);
        assertThat(layerIndex.keySet()).containsExactly("dependencies", "infra-loader",
                "snapshot-dependencies", "application");
        assertThat(layerIndex.get("application")).contains("APP-INF/lib/jar-release-0.0.1.RELEASE.jar",
                "APP-INF/lib/jar-snapshot-0.0.1.BUILD-SNAPSHOT.jar");
        assertThat(layerIndex.get("dependencies"))
                .anyMatch((dependency) -> dependency.startsWith("APP-INF/lib/log4j-api-2"));
      }
      catch (IOException ex) {
      }
    });
  }

  @TestTemplate
  void whenJarIsRepackagedWithTheLayersDisabledDoesNotContainLayersIndex(MavenBuild mavenBuild) {
    mavenBuild.project("jar-layered-disabled").execute((project) -> {
      File repackaged = new File(project, "jar/target/jar-layered-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("APP-INF/classes/")
              .hasEntryWithNameStartingWith("APP-INF/lib/jar-release")
              .hasEntryWithNameStartingWith("APP-INF/lib/jar-snapshot")
              .doesNotHaveEntryWithName("APP-INF/layers.idx")
              .doesNotHaveEntryWithNameStartingWith("APP-INF/lib/" + JarModeLibrary.LAYER_TOOLS.getName());
    });
  }

  @TestTemplate
  void whenJarIsRepackagedWithTheLayersEnabledAndLayerToolsExcluded(MavenBuild mavenBuild) {
    mavenBuild.project("jar-layered-no-layer-tools").execute((project) -> {
      File repackaged = new File(project, "jar/target/jar-layered-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("APP-INF/classes/")
              .hasEntryWithNameStartingWith("APP-INF/lib/jar-release")
              .hasEntryWithNameStartingWith("APP-INF/lib/jar-snapshot")
              .hasEntryWithNameStartingWith("APP-INF/layers.idx")
              .doesNotHaveEntryWithNameStartingWith("APP-INF/lib/" + JarModeLibrary.LAYER_TOOLS.getName());
    });
  }

  @TestTemplate
  void whenJarIsRepackagedWithTheCustomLayers(MavenBuild mavenBuild) {
    mavenBuild.project("jar-layered-custom").execute((project) -> {
      File repackaged = new File(project, "jar/target/jar-layered-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(jar(repackaged)).hasEntryWithNameStartingWith("APP-INF/classes/")
              .hasEntryWithNameStartingWith("APP-INF/lib/jar-release")
              .hasEntryWithNameStartingWith("APP-INF/lib/jar-snapshot");
      try (JarFile jarFile = new JarFile(repackaged)) {
        Map<String, List<String>> layerIndex = readLayerIndex(jarFile);
        assertThat(layerIndex.keySet()).containsExactly("my-dependencies-name", "snapshot-dependencies",
                "configuration", "application");
        assertThat(layerIndex.get("application"))
                .contains("APP-INF/lib/jar-release-0.0.1.RELEASE.jar",
                        "APP-INF/lib/jar-snapshot-0.0.1.BUILD-SNAPSHOT.jar",
                        "APP-INF/lib/jar-classifier-0.0.1-bravo.jar")
                .doesNotContain("APP-INF/lib/jar-classifier-0.0.1-alpha.jar");
      }
    });
  }

  @TestTemplate
  void repackagedJarContainsClasspathIndex(MavenBuild mavenBuild) {
    mavenBuild.project("jar").execute((project) -> {
      File repackaged = new File(project, "target/jar-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(jar(repackaged))
              .manifest((manifest) -> manifest.hasAttribute("Infra-App-Classpath-Index", "APP-INF/classpath.idx"));
      assertThat(jar(repackaged)).hasEntryWithName("APP-INF/classpath.idx");
      try (JarFile jarFile = new JarFile(repackaged)) {
        List<String> index = readClasspathIndex(jarFile, "APP-INF/classpath.idx");
        assertThat(index).allMatch((entry) -> entry.startsWith("APP-INF/lib/"));
      }
    });
  }

  @TestTemplate
  void whenJarIsRepackagedWithOutputTimestampConfiguredThenJarIsReproducible(MavenBuild mavenBuild)
          throws InterruptedException {
    String firstHash = buildJarWithOutputTimestamp(mavenBuild);
    Thread.sleep(1500);
    String secondHash = buildJarWithOutputTimestamp(mavenBuild);
    assertThat(firstHash).isEqualTo(secondHash);
  }

  private String buildJarWithOutputTimestamp(MavenBuild mavenBuild) {
    AtomicReference<String> jarHash = new AtomicReference<>();
    mavenBuild.project("jar-output-timestamp").execute((project) -> {
      File repackaged = new File(project, "target/jar-output-timestamp-0.0.1.BUILD-SNAPSHOT.jar");
      assertThat(repackaged).isFile();
      long expectedModified = 1584352800000L;
      long offsetExpectedModified = expectedModified - TimeZone.getDefault().getOffset(expectedModified);
      assertThat(repackaged.lastModified()).isEqualTo(expectedModified);
      try (JarFile jar = new JarFile(repackaged)) {
        List<String> unreproducibleEntries = jar.stream()
                .filter((entry) -> entry.getLastModifiedTime().toMillis() != offsetExpectedModified)
                .map((entry) -> entry.getName() + ": " + entry.getLastModifiedTime())
                .toList();
        assertThat(unreproducibleEntries).isEmpty();
        jarHash.set(FileUtils.sha1Hash(repackaged));
        FileSystemUtils.deleteRecursively(project);
      }
      catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    });
    return jarHash.get();
  }

  @TestTemplate
  void whenJarIsRepackagedWithOutputTimestampConfiguredThenLibrariesAreSorted(MavenBuild mavenBuild) {
    mavenBuild.project("jar-output-timestamp").execute((project) -> {
      File repackaged = new File(project, "target/jar-output-timestamp-0.0.1.BUILD-SNAPSHOT.jar");
      List<String> sortedLibs = Arrays.asList("APP-INF/lib/jakarta.servlet-api", "APP-INF/lib/today-aop",
              "APP-INF/lib/today-beans", "APP-INF/lib/infra-jarmode-layertools",
              "APP-INF/lib/today-context", "APP-INF/lib/today-core");
      assertThat(jar(repackaged)).entryNamesInPath("APP-INF/lib/")
              .zipSatisfy(sortedLibs,
                      (String jarLib, String expectedLib) -> assertThat(jarLib).startsWith(expectedLib));
    });
  }

}
