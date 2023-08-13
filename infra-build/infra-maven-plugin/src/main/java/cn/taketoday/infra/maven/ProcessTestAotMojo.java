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

package cn.taketoday.infra.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.repository.RepositorySystem;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Invoke the AOT engine on tests.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Mojo(name = "process-test-aot", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, threadSafe = true,
      requiresDependencyResolution = ResolutionScope.TEST, requiresDependencyCollection = ResolutionScope.TEST)
public class ProcessTestAotMojo extends AbstractAotMojo {

  private static final String JUNIT_PLATFORM_GROUP_ID = "org.junit.platform";

  private static final String JUNIT_PLATFORM_COMMONS_ARTIFACT_ID = "junit-platform-commons";

  private static final String JUNIT_PLATFORM_LAUNCHER_ARTIFACT_ID = "junit-platform-launcher";

  private static final String AOT_PROCESSOR_CLASS_NAME = "cn.taketoday.framework.test.context.InfraTestAotProcessor";

  /**
   * Directory containing the classes and resource files that should be packaged into
   * the archive.
   */
  @Parameter(defaultValue = "${project.build.testOutputDirectory}", required = true)
  private File testClassesDirectory;

  /**
   * Directory containing the classes and resource files that should be used to run the
   * tests.
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
  private File classesDirectory;

  /**
   * Directory containing the generated sources.
   */
  @Parameter(defaultValue = "${project.build.directory}/infra-aot/test/sources", required = true)
  private File generatedSources;

  /**
   * Directory containing the generated test resources.
   */
  @Parameter(defaultValue = "${project.build.directory}/infra-aot/test/resources", required = true)
  private File generatedResources;

  /**
   * Directory containing the generated test classes.
   */
  @Parameter(defaultValue = "${project.build.directory}/infra-aot/test/classes", required = true)
  private File generatedTestClasses;

  /**
   * Directory containing the generated test classes.
   */
  @Parameter(defaultValue = "${project.build.directory}/infra-aot/main/classes", required = true)
  private File generatedClasses;

  /**
   * Local artifact repository used to resolve JUnit platform launcher jars.
   */
  @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
  private ArtifactRepository localRepository;

  /**
   * Remote artifact repositories used to resolve JUnit platform launcher jars.
   */
  @Parameter(defaultValue = "${project.remoteArtifactRepositories}", required = true, readonly = true)
  private List<ArtifactRepository> remoteRepositories;

  @Component
  private RepositorySystem repositorySystem;

  @Component
  private ResolutionErrorHandler resolutionErrorHandler;

  @Override
  protected void executeAot() throws Exception {
    if (this.project.getPackaging().equals("pom")) {
      getLog().debug("process-test-aot goal could not be applied to pom project.");
      return;
    }
    if (Boolean.getBoolean("skipTests") || Boolean.getBoolean("maven.test.skip")) {
      getLog().info("Skipping AOT test processing since tests are skipped");
      return;
    }
    Path testOutputDirectory = Paths.get(this.project.getBuild().getTestOutputDirectory());
    if (Files.notExists(testOutputDirectory)) {
      getLog().info("Skipping AOT test processing since no tests have been detected");
      return;
    }
    generateAotAssets(getClassPath(true), AOT_PROCESSOR_CLASS_NAME, getAotArguments());
    compileSourceFiles(getClassPath(false), this.generatedSources, this.testClassesDirectory);
    copyAll(this.generatedResources.toPath().resolve("META-INF/native-image"),
            this.testClassesDirectory.toPath().resolve("META-INF/native-image"));
    copyAll(this.generatedTestClasses.toPath(), this.testClassesDirectory.toPath());
  }

  private String[] getAotArguments() {
    List<String> aotArguments = new ArrayList<>();
    aotArguments.add(this.testClassesDirectory.toPath().toAbsolutePath().normalize().toString());
    aotArguments.add(this.generatedSources.toString());
    aotArguments.add(this.generatedResources.toString());
    aotArguments.add(this.generatedTestClasses.toString());
    aotArguments.add(this.project.getGroupId());
    aotArguments.add(this.project.getArtifactId());
    return aotArguments.toArray(String[]::new);
  }

  protected URL[] getClassPath(boolean includeJUnitPlatformLauncher) throws Exception {
    File[] directories = new File[] { this.testClassesDirectory, this.generatedTestClasses, this.classesDirectory,
            this.generatedClasses };
    URL[] classPath = getClassPath(directories);
    if (!includeJUnitPlatformLauncher || this.project.getArtifactMap()
            .containsKey(JUNIT_PLATFORM_GROUP_ID + ":" + JUNIT_PLATFORM_LAUNCHER_ARTIFACT_ID)) {
      return classPath;
    }
    return addJUnitPlatformLauncher(classPath);
  }

  private URL[] addJUnitPlatformLauncher(URL[] classPath) throws Exception {
    String version = getJUnitPlatformVersion();
    DefaultArtifactHandler handler = new DefaultArtifactHandler("jar");
    handler.setIncludesDependencies(true);
    ArtifactResolutionResult resolutionResult = resolveArtifact(new DefaultArtifact(JUNIT_PLATFORM_GROUP_ID,
            JUNIT_PLATFORM_LAUNCHER_ARTIFACT_ID, version, null, "jar", null, handler));
    LinkedHashSet<URL> fullClassPath = new LinkedHashSet<>(Arrays.asList(classPath));
    for (Artifact artifact : resolutionResult.getArtifacts()) {
      fullClassPath.add(artifact.getFile().toURI().toURL());
    }
    return fullClassPath.toArray(URL[]::new);
  }

  private String getJUnitPlatformVersion() throws MojoExecutionException {
    String id = JUNIT_PLATFORM_GROUP_ID + ":" + JUNIT_PLATFORM_COMMONS_ARTIFACT_ID;
    Artifact platformCommonsArtifact = this.project.getArtifactMap().get(id);
    String version = (platformCommonsArtifact != null) ? platformCommonsArtifact.getBaseVersion() : null;
    if (version == null) {
      throw new MojoExecutionException(
              "Unable to find '%s' dependnecy. Please ensure JUnit is correctly configured.".formatted(id));
    }
    return version;
  }

  private ArtifactResolutionResult resolveArtifact(Artifact artifact) throws Exception {
    ArtifactResolutionRequest request = new ArtifactResolutionRequest();
    request.setArtifact(artifact);
    request.setLocalRepository(this.localRepository);
    request.setResolveTransitively(true);
    request.setCollectionFilter(new RuntimeArtifactFilter());
    request.setRemoteRepositories(this.remoteRepositories);
    ArtifactResolutionResult result = this.repositorySystem.resolve(request);
    this.resolutionErrorHandler.throwErrors(request, result);
    return result;
  }

}
