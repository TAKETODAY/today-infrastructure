/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.gradle.plugin;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.TestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import infra.gradle.junit.GradleCompatibility;
import infra.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ApplicationPluginAction}.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility
class ApplicationPluginActionIntegrationTests {

  GradleBuild gradleBuild;

  @TestTemplate
  void noInfraDistributionWithoutApplicationPluginApplied() {
    assertThat(this.gradleBuild.build("distributionExists", "-PdistributionName=infra").getOutput())
            .contains("infra exists = false");
  }

  @TestTemplate
  void applyingApplicationPluginCreatesInfraDistribution() {
    assertThat(this.gradleBuild.build("distributionExists", "-PdistributionName=infra", "-PapplyApplicationPlugin")
            .getOutput()).contains("infra exists = true");
  }

  @TestTemplate
  void noInfraStartScriptsTaskWithoutApplicationPluginApplied() {
    assertThat(this.gradleBuild.build("taskExists", "-PtaskName=infraStartScripts").getOutput())
            .contains("infraStartScripts exists = false");
  }

  @TestTemplate
  void applyingApplicationPluginCreatesInfraStartScriptsTask() {
    assertThat(this.gradleBuild.build("taskExists", "-PtaskName=infraStartScripts", "-PapplyApplicationPlugin")
            .getOutput()).contains("infraStartScripts exists = true");
  }

  @TestTemplate
  void createsInfraStartScriptsTaskUsesApplicationPluginsDefaultJvmOpts() {
    assertThat(this.gradleBuild.build("startScriptsDefaultJvmOpts", "-PapplyApplicationPlugin").getOutput())
            .contains("infraStartScripts defaultJvmOpts = [-Dcom.example.a=alpha, -Dcom.example.b=bravo]");
  }

  @TestTemplate
  void zipDistributionForJarCanBeBuilt() throws IOException {
    assertThat(this.gradleBuild.build("infraDistZip").task(":infraDistZip").getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);
    String name = this.gradleBuild.getProjectDir().getName();
    File distribution = new File(this.gradleBuild.getProjectDir(), "build/distributions/" + name + "-infra-app.zip");
    assertThat(distribution).isFile();
    assertThat(zipEntryNames(distribution)).containsExactlyInAnyOrder(name + "-infra-app/", name + "-infra-app/lib/",
            name + "-infra-app/lib/" + name + ".jar", name + "-infra-app/bin/", name + "-infra-app/bin/" + name,
            name + "-infra-app/bin/" + name + ".bat");
  }

  @TestTemplate
  void tarDistributionForJarCanBeBuilt() throws IOException {
    assertThat(this.gradleBuild.build("infraDistTar").task(":infraDistTar").getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);
    String name = this.gradleBuild.getProjectDir().getName();
    File distribution = new File(this.gradleBuild.getProjectDir(), "build/distributions/" + name + "-infra-app.tar");
    assertThat(distribution).isFile();
    assertThat(tarEntryNames(distribution)).containsExactlyInAnyOrder(name + "-infra-app/", name + "-infra-app/lib/",
            name + "-infra-app/lib/" + name + ".jar", name + "-infra-app/bin/", name + "-infra-app/bin/" + name,
            name + "-infra-app/bin/" + name + ".bat");
  }

  @TestTemplate
  void zipDistributionForWarCanBeBuilt() throws IOException {
    assertThat(this.gradleBuild.build("infraDistZip").task(":infraDistZip").getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);
    String name = this.gradleBuild.getProjectDir().getName();
    File distribution = new File(this.gradleBuild.getProjectDir(), "build/distributions/" + name + "-infra-app.zip");
    assertThat(distribution).isFile();
    assertThat(zipEntryNames(distribution)).containsExactlyInAnyOrder(name + "-infra-app/", name + "-infra-app/lib/",
            name + "-infra-app/lib/" + name + ".war", name + "-infra-app/bin/", name + "-infra-app/bin/" + name,
            name + "-infra-app/bin/" + name + ".bat");
  }

  @TestTemplate
  void tarDistributionForWarCanBeBuilt() throws IOException {
    assertThat(this.gradleBuild.build("infraDistTar").task(":infraDistTar").getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);
    String name = this.gradleBuild.getProjectDir().getName();
    File distribution = new File(this.gradleBuild.getProjectDir(), "build/distributions/" + name + "-infra-app.tar");
    assertThat(distribution).isFile();
    assertThat(tarEntryNames(distribution)).containsExactlyInAnyOrder(name + "-infra-app/", name + "-infra-app/lib/",
            name + "-infra-app/lib/" + name + ".war", name + "-infra-app/bin/", name + "-infra-app/bin/" + name,
            name + "-infra-app/bin/" + name + ".bat");
  }

  @TestTemplate
  void applicationNameCanBeUsedToCustomizeDistributionName() throws IOException {
    assertThat(this.gradleBuild.build("infraDistTar").task(":infraDistTar").getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);
    File distribution = new File(this.gradleBuild.getProjectDir(), "build/distributions/custom-infra-app.tar");
    assertThat(distribution).isFile();
    String name = this.gradleBuild.getProjectDir().getName();
    assertThat(tarEntryNames(distribution)).containsExactlyInAnyOrder("custom-infra-app/", "custom-infra-app/lib/",
            "custom-infra-app/lib/" + name + ".jar", "custom-infra-app/bin/", "custom-infra-app/bin/custom",
            "custom-infra-app/bin/custom.bat");
  }

  @TestTemplate
  void scriptsHaveCorrectPermissions() throws IOException {
    assertThat(this.gradleBuild.build("infraDistTar").task(":infraDistTar").getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);
    String name = this.gradleBuild.getProjectDir().getName();
    File distribution = new File(this.gradleBuild.getProjectDir(), "build/distributions/" + name + "-infra-app.tar");
    assertThat(distribution).isFile();
    tarEntries(distribution, (entry) -> {
      int filePermissions = entry.getMode() & 0777;
      if (entry.isFile() && !entry.getName().startsWith(name + "-infra-app/bin/")) {
        assertThat(filePermissions).isEqualTo(0644);
      }
      else {
        assertThat(filePermissions).isEqualTo(0755);
      }
    });
  }

  @TestTemplate
  void taskConfigurationIsAvoided() throws IOException {
    BuildResult result = this.gradleBuild.build("help");
    String output = result.getOutput();
    BufferedReader reader = new BufferedReader(new StringReader(output));
    String line;
    Set<String> configured = new HashSet<>();
    while ((line = reader.readLine()) != null) {
      if (line.startsWith("Configuring :")) {
        configured.add(line.substring("Configuring :".length()));
      }
    }
    if (GradleVersion.version(this.gradleBuild.getGradleVersion()).compareTo(GradleVersion.version("7.3.3")) < 0) {
      assertThat(configured).containsExactly("help");
    }
    else {
      assertThat(configured).containsExactlyInAnyOrder("help", "clean");
    }
  }

  private List<String> zipEntryNames(File distribution) throws IOException {
    List<String> entryNames = new ArrayList<>();
    try (ZipFile zipFile = new ZipFile(distribution)) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        entryNames.add(entries.nextElement().getName());
      }
    }
    return entryNames;
  }

  private List<String> tarEntryNames(File distribution) throws IOException {
    List<String> entryNames = new ArrayList<>();
    try (TarArchiveInputStream input = new TarArchiveInputStream(new FileInputStream(distribution))) {
      TarArchiveEntry entry;
      while ((entry = input.getNextEntry()) != null) {
        entryNames.add(entry.getName());
      }
    }
    return entryNames;
  }

  private void tarEntries(File distribution, Consumer<TarArchiveEntry> consumer) throws IOException {
    try (TarArchiveInputStream input = new TarArchiveInputStream(new FileInputStream(distribution))) {
      TarArchiveEntry entry;
      while ((entry = input.getNextEntry()) != null) {
        consumer.accept(entry);
      }
    }
  }

}
