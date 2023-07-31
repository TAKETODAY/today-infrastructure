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

package cn.taketoday.gradle.testkit;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.Versioned;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.sun.jna.Platform;

import org.antlr.v4.runtime.Lexer;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.util.GradleVersion;
import org.tomlj.Toml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.JarFile;

import cn.taketoday.app.loader.tools.LaunchScript;
import cn.taketoday.buildpack.platform.build.BuildRequest;
import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.framework.ApplicationAotProcessor;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.FileSystemUtils;
import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * A {@code GradleBuild} is used to run a Gradle build using {@link GradleRunner}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class GradleBuild {

  private final Dsl dsl;

  private File projectDir;

  private String script;

  private String settings;

  private String gradleVersion;

  private String infraVersion = "TEST-SNAPSHOT";

  private GradleVersion expectDeprecationWarnings;

  private final List<String> expectedDeprecationMessages = new ArrayList<>();

  private boolean configurationCache = false;

  private final Map<String, String> scriptProperties = new HashMap<>();

  public GradleBuild() {
    this(Dsl.GROOVY);
  }

  public GradleBuild(Dsl dsl) {
    this.dsl = dsl;
  }

  public Dsl getDsl() {
    return this.dsl;
  }

  void before() throws IOException {
    this.projectDir = ApplicationTemp.createDirectory("gradle-");
  }

  void after() {
    this.script = null;
    FileSystemUtils.deleteRecursively(this.projectDir);
  }

  private List<File> pluginClasspath() {
    return Arrays.asList(new File("bin/main"),
            new File("build/classes/java/main"),
            new File("build/resources/main"),
            new File(pathOfJarContaining(ClassUtils.class)),
            new File(pathOfJarContaining(ApplicationAotProcessor.class)),
            new File(pathOfJarContaining(LaunchScript.class)),
            new File(pathOfJarContaining(DependencyManagementPlugin.class)),
            new File(pathOfJarContaining(ArchiveEntry.class)),
            new File(pathOfJarContaining(BuildRequest.class)),
            new File(pathOfJarContaining(HttpClientConnectionManager.class)),
            new File(pathOfJarContaining(HttpRequest.class)),
            new File(pathOfJarContaining(HttpVersionPolicy.class)),
            new File(pathOfJarContaining(Module.class)),
            new File(pathOfJarContaining(Versioned.class)),
            new File(pathOfJarContaining(ParameterNamesModule.class)),
            new File(pathOfJarContaining(JsonView.class)),
            new File(pathOfJarContaining(Platform.class)),
            new File(pathOfJarContaining(Toml.class)),
            new File(pathOfJarContaining(Lexer.class)),
            new File(pathOfJarContaining("org.graalvm.buildtools.gradle.NativeImagePlugin")),
            new File(pathOfJarContaining("org.graalvm.reachability.GraalVMReachabilityMetadataRepository")),
            new File(pathOfJarContaining("org.graalvm.buildtools.utils.SharedConstants")));
  }

  private String pathOfJarContaining(String className) {
    try {
      return pathOfJarContaining(Class.forName(className));
    }
    catch (ClassNotFoundException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  private String pathOfJarContaining(Class<?> type) {
    return type.getProtectionDomain().getCodeSource().getLocation().getPath();
  }

  public GradleBuild script(String script) {
    this.script = script.endsWith(this.dsl.getExtension()) ? script : script + this.dsl.getExtension();
    return this;
  }

  public void settings(String settings) {
    this.settings = settings;
  }

  public GradleBuild expectDeprecationWarningsWithAtLeastVersion(String gradleVersion) {
    this.expectDeprecationWarnings = GradleVersion.version(gradleVersion);
    return this;
  }

  public GradleBuild expectDeprecationMessages(String... messages) {
    this.expectedDeprecationMessages.addAll(Arrays.asList(messages));
    return this;
  }

  public GradleBuild configurationCache() {
    this.configurationCache = true;
    return this;
  }

  public boolean isConfigurationCache() {
    return this.configurationCache;
  }

  public GradleBuild scriptProperty(String key, String value) {
    this.scriptProperties.put(key, value);
    return this;
  }

  public GradleBuild scriptPropertyFrom(File propertiesFile, String key) {
    this.scriptProperties.put(key, getProperty(propertiesFile, key));
    return this;
  }

  public boolean gradleVersionIsAtLeast(String version) {
    return GradleVersion.version(this.gradleVersion).compareTo(GradleVersion.version(version)) >= 0;
  }

  public BuildResult build(String... arguments) {
    try {
      BuildResult result = prepareRunner(arguments).build();
      if (this.expectDeprecationWarnings == null || (this.gradleVersion != null
              && this.expectDeprecationWarnings.compareTo(GradleVersion.version(this.gradleVersion)) > 0)) {
        String buildOutput = result.getOutput();
        for (String message : this.expectedDeprecationMessages) {
          buildOutput = buildOutput.replaceAll(message, "");
        }
        assertThat(buildOutput).doesNotContainIgnoringCase("deprecated");
      }
      return result;
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public BuildResult buildAndFail(String... arguments) {
    try {
      return prepareRunner(arguments).buildAndFail();
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public GradleRunner prepareRunner(String... arguments) throws IOException {
    this.scriptProperties.put("infraVersion", getInfraVersion());
    this.scriptProperties.put("dependencyManagementPluginVersion", getDependencyManagementPluginVersion());
    copyTransformedScript(this.script, new File(this.projectDir, "build" + this.dsl.getExtension()));
    if (this.settings != null) {
      copyTransformedScript(this.settings, new File(this.projectDir, "settings.gradle"));
    }
    File repository = new File("src/test/resources/repository");
    if (repository.exists()) {
      FileSystemUtils.copyRecursively(repository, new File(this.projectDir, "repository"));
    }
    List<File> classpath = pluginClasspath();
    GradleRunner gradleRunner = GradleRunner.create()
            .withProjectDir(this.projectDir)
            .withPluginClasspath(classpath);
    if (!this.configurationCache) {
      // see https://github.com/gradle/gradle/issues/6862
      gradleRunner.withDebug(true);
    }
    if (this.gradleVersion != null) {
      gradleRunner.withGradleVersion(this.gradleVersion);
    }
    gradleRunner.withTestKitDir(getTestKitDir());
    List<String> allArguments = new ArrayList<>();
    allArguments.add("-PinfraVersion=" + getInfraVersion());
    allArguments.add("--stacktrace");
    allArguments.addAll(Arrays.asList(arguments));
    allArguments.add("--warning-mode");
    allArguments.add("all");
    if (this.configurationCache) {
      allArguments.add("--configuration-cache");
    }
    return gradleRunner.withArguments(allArguments);
  }

  private void copyTransformedScript(String script, File destination) throws IOException {
    String scriptContent = FileCopyUtils.copyToString(new FileReader(script));
    for (Entry<String, String> property : this.scriptProperties.entrySet()) {
      scriptContent = scriptContent.replace("{" + property.getKey() + "}", property.getValue());
    }
    FileCopyUtils.copy(scriptContent, new FileWriter(destination));
  }

  private File getTestKitDir() {
    File temp = new File(System.getProperty("java.io.tmpdir"));
    String username = System.getProperty("user.name");
    String gradleVersion = (this.gradleVersion != null) ? this.gradleVersion : "default";
    return new File(temp, ".gradle-test-kit-" + username + "-" + getInfraVersion() + "-" + gradleVersion);
  }

  public File getProjectDir() {
    return this.projectDir;
  }

  public void setProjectDir(File projectDir) {
    this.projectDir = projectDir;
  }

  public GradleBuild gradleVersion(String version) {
    this.gradleVersion = version;
    return this;
  }

  public String getGradleVersion() {
    return this.gradleVersion;
  }

  public GradleBuild infraVersion(String version) {
    this.infraVersion = version;
    return this;
  }

  private String getInfraVersion() {
    return this.infraVersion;
  }

  private static String getDependencyManagementPluginVersion() {
    try {
      URL location = DependencyManagementExtension.class.getProtectionDomain().getCodeSource().getLocation();
      try (JarFile jar = new JarFile(new File(location.toURI()))) {
        return jar.getManifest().getMainAttributes().getValue("Implementation-Version");
      }
    }
    catch (Exception ex) {
      throw new IllegalStateException("Failed to find dependency management plugin version", ex);
    }
  }

  private String getProperty(File propertiesFile, String key) {
    try {
      assertThat(propertiesFile)
              .withFailMessage("Expecting properties file to exist at path '%s'", propertiesFile.getCanonicalFile())
              .exists();
      Properties properties = new Properties();
      try (FileInputStream input = new FileInputStream(propertiesFile)) {
        properties.load(input);
        String value = properties.getProperty(key);
        assertThat(value)
                .withFailMessage("Expecting properties file '%s' to contain the key '%s'",
                        propertiesFile.getCanonicalFile(), key)
                .isNotEmpty();
        return value;
      }
    }
    catch (IOException ex) {
      fail("Error reading properties file '" + propertiesFile + "'");
      return null;
    }
  }

}
