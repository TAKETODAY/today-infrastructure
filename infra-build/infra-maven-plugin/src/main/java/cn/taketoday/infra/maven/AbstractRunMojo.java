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

package cn.taketoday.infra.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.app.loader.tools.FileUtils;

/**
 * Base class to run a Infra application.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author David Liu
 * @author Daniel Young
 * @author Dmytro Nosan
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RunMojo
 * @see StartMojo
 * @since 4.0
 */
public abstract class AbstractRunMojo extends AbstractDependencyFilterMojo {

  /**
   * The Maven project.
   */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  /**
   * The current Maven session. This is used for toolchain manager API calls.
   */
  @Parameter(defaultValue = "${session}", readonly = true)
  private MavenSession session;

  /**
   * The toolchain manager to use to locate a custom JDK.
   */
  @Component
  private ToolchainManager toolchainManager;

  /**
   * Add maven resources to the classpath directly, this allows live in-place editing of
   * resources. Duplicate resources are removed from {@code target/classes} to prevent
   * them from appearing twice if {@code ClassLoader.getResources()} is called. Please
   * consider adding {@code today-infra-devtools} to your project instead as it provides
   * this feature and many more.
   */
  @Parameter(property = "infra.run.addResources", defaultValue = "false")
  private boolean addResources = false;

  /**
   * Path to agent jars.
   */
  @Parameter(property = "infra.run.agents")
  private File[] agents;

  /**
   * Flag to say that the agent requires -noverify.
   */
  @Parameter(property = "infra.run.noverify")
  private boolean noverify = false;

  /**
   * Current working directory to use for the application. If not specified, basedir
   * will be used.
   */
  @Parameter(property = "infra.run.workingDirectory")
  private File workingDirectory;

  /**
   * JVM arguments that should be associated with the forked process used to run the
   * application. On command line, make sure to wrap multiple values between quotes.
   */
  @Parameter(property = "infra.run.jvmArguments")
  private String jvmArguments;

  /**
   * List of JVM system properties to pass to the process.
   */
  @Parameter
  private Map<String, String> systemPropertyVariables;

  /**
   * List of Environment variables that should be associated with the forked process
   * used to run the application.
   */
  @Parameter
  private Map<String, String> environmentVariables;

  /**
   * Arguments that should be passed to the application.
   */
  @Parameter
  private String[] arguments;

  /**
   * Arguments from the command line that should be passed to the application. Use
   * spaces to separate multiple arguments and make sure to wrap multiple values between
   * quotes. When specified, takes precedence over {@link #arguments}.
   */
  @Parameter(property = "infra.run.arguments")
  private String commandlineArguments;

  /**
   * The spring profiles to activate. Convenience shortcut of specifying the
   * 'infra.profiles.active' argument. On command line use commas to separate multiple
   * profiles.
   */
  @Parameter(property = "infra.run.profiles")
  private String[] profiles;

  /**
   * The name of the main class. If not specified the first compiled class found that
   * contains a 'main' method will be used.
   */
  @Parameter(property = "infra.run.main-class")
  private String mainClass;

  /**
   * Additional directories besides the classes directory that should be added to the
   * classpath.
   */
  @Parameter(property = "infra.run.directories")
  private String[] directories;

  /**
   * Directory containing the classes and resource files that should be used to run the
   * application.
   */
  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
  private File classesDirectory;

  /**
   * Skip the execution.
   */
  @Parameter(property = "infra.run.skip", defaultValue = "false")
  private boolean skip;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (this.skip) {
      getLog().debug("skipping run as per configuration.");
      return;
    }
    run(determineMainClass());
  }

  private String determineMainClass() throws MojoExecutionException {
    if (this.mainClass != null) {
      return this.mainClass;
    }
    return InfraApplicationClassFinder.findSingleClass(getClassesDirectories());
  }

  /**
   * Returns the directories that contain the application's classes and resources. When
   * the application's main class has not been configured, each directory is searched in
   * turn for an appropriate main class.
   *
   * @return the directories that contain the application's classes and resources
   */
  protected List<File> getClassesDirectories() {
    return List.of(this.classesDirectory);
  }

  protected abstract boolean isUseTestClasspath();

  private void run(String startClassName) throws MojoExecutionException, MojoFailureException {
    List<String> args = new ArrayList<>();
    addAgents(args);
    addJvmArgs(args);
    addClasspath(args);
    args.add(startClassName);
    addArgs(args);
    JavaProcessExecutor processExecutor = new JavaProcessExecutor(this.session, this.toolchainManager);
    File workingDirectoryToUse = (this.workingDirectory != null) ? this.workingDirectory
                                                                 : this.project.getBasedir();
    run(processExecutor, workingDirectoryToUse, args, determineEnvironmentVariables());
  }

  /**
   * Run the application.
   *
   * @param processExecutor the {@link JavaProcessExecutor} to use
   * @param workingDirectory the working directory of the forked JVM
   * @param args the arguments (JVM arguments and application arguments)
   * @param environmentVariables the environment variables
   * @throws MojoExecutionException in case of MOJO execution errors
   * @throws MojoFailureException in case of MOJO failures
   */
  protected abstract void run(JavaProcessExecutor processExecutor, File workingDirectory, List<String> args,
          Map<String, String> environmentVariables) throws MojoExecutionException, MojoFailureException;

  /**
   * Resolve the application arguments to use.
   *
   * @return a {@link RunArguments} defining the application arguments
   */
  protected RunArguments resolveApplicationArguments() {
    RunArguments runArguments =
            arguments != null ? new RunArguments(this.arguments)
                              : new RunArguments(this.commandlineArguments);
    addActiveProfileArgument(runArguments);
    return runArguments;
  }

  /**
   * Resolve the environment variables to use.
   *
   * @return an {@link EnvVariables} defining the environment variables
   */
  protected EnvVariables resolveEnvVariables() {
    return new EnvVariables(this.environmentVariables);
  }

  private void addArgs(List<String> args) {
    RunArguments applicationArguments = resolveApplicationArguments();
    Collections.addAll(args, applicationArguments.asArray());
    logArguments("Application argument(s): ", applicationArguments.asArray());
  }

  private Map<String, String> determineEnvironmentVariables() {
    EnvVariables envVariables = resolveEnvVariables();
    logArguments("Environment variable(s): ", envVariables.asArray());
    return envVariables.asMap();
  }

  /**
   * Resolve the JVM arguments to use.
   *
   * @return a {@link RunArguments} defining the JVM arguments
   */
  protected RunArguments resolveJvmArguments() {
    StringBuilder stringBuilder = new StringBuilder();
    if (this.systemPropertyVariables != null) {
      stringBuilder.append(this.systemPropertyVariables.entrySet()
              .stream()
              .map((e) -> SystemPropertyFormatter.format(e.getKey(), e.getValue()))
              .collect(Collectors.joining(" ")));
    }
    if (this.jvmArguments != null) {
      stringBuilder.append(" ").append(this.jvmArguments);
    }
    return new RunArguments(stringBuilder.toString());
  }

  private void addJvmArgs(List<String> args) {
    RunArguments jvmArguments = resolveJvmArguments();
    Collections.addAll(args, jvmArguments.asArray());
    logArguments("JVM argument(s): ", jvmArguments.asArray());
  }

  private void addAgents(List<String> args) {
    if (this.agents != null) {
      if (getLog().isInfoEnabled()) {
        getLog().info("Attaching agents: " + Arrays.asList(this.agents));
      }
      for (File agent : this.agents) {
        args.add("-javaagent:" + agent);
      }
    }
    if (this.noverify) {
      args.add("-noverify");
    }
  }

  private void addActiveProfileArgument(RunArguments arguments) {
    if (this.profiles.length > 0) {
      StringBuilder arg = new StringBuilder("--infra.profiles.active=");
      for (int i = 0; i < this.profiles.length; i++) {
        arg.append(this.profiles[i]);
        if (i < this.profiles.length - 1) {
          arg.append(",");
        }
      }
      arguments.getArgs().addFirst(arg.toString());
      logArguments("Active profile(s): ", this.profiles);
    }
  }

  private void addClasspath(List<String> args) throws MojoExecutionException {
    try {
      StringBuilder classpath = new StringBuilder();
      for (URL ele : getClassPathUrls()) {
        if (classpath.length() > 0) {
          classpath.append(File.pathSeparator);
        }
        classpath.append(new File(ele.toURI()));
      }
      if (getLog().isDebugEnabled()) {
        getLog().debug("Classpath for forked process: " + classpath);
      }
      args.add("-cp");
      args.add(classpath.toString());
    }
    catch (Exception ex) {
      throw new MojoExecutionException("Could not build classpath", ex);
    }
  }

  protected URL[] getClassPathUrls() throws MojoExecutionException {
    try {
      List<URL> urls = new ArrayList<>();
      addUserDefinedDirectories(urls);
      addResources(urls);
      addProjectClasses(urls);
      addDependencies(urls);
      return urls.toArray(new URL[0]);
    }
    catch (IOException ex) {
      throw new MojoExecutionException("Unable to build classpath", ex);
    }
  }

  private void addUserDefinedDirectories(List<URL> urls) throws MalformedURLException {
    if (this.directories != null) {
      for (String directory : this.directories) {
        urls.add(new File(directory).toURI().toURL());
      }
    }
  }

  private void addResources(List<URL> urls) throws IOException {
    if (this.addResources) {
      for (Resource resource : this.project.getResources()) {
        File directory = new File(resource.getDirectory());
        urls.add(directory.toURI().toURL());
        for (File classesDirectory : getClassesDirectories()) {
          FileUtils.removeDuplicatesFromOutputDirectory(classesDirectory, directory);
        }
      }
    }
  }

  private void addProjectClasses(List<URL> urls) throws MalformedURLException {
    for (File classesDirectory : getClassesDirectories()) {
      urls.add(classesDirectory.toURI().toURL());
    }
  }

  private void addDependencies(List<URL> urls) throws MalformedURLException, MojoExecutionException {
    Set<Artifact> artifacts = (isUseTestClasspath())
                              ? filterDependencies(this.project.getArtifacts())
                              : filterDependencies(this.project.getArtifacts(), new ExcludeTestScopeArtifactFilter());
    for (Artifact artifact : artifacts) {
      if (artifact.getFile() != null) {
        urls.add(artifact.getFile().toURI().toURL());
      }
    }
  }

  private void logArguments(String message, String[] args) {
    if (getLog().isDebugEnabled()) {
      getLog().debug(Arrays.stream(args).collect(Collectors.joining(" ", message, "")));
    }
  }

  /**
   * Format System properties.
   */
  static class SystemPropertyFormatter {

    static String format(String key, String value) {
      if (key == null) {
        return "";
      }
      if (value == null || value.isEmpty()) {
        return String.format("-D%s", key);
      }
      return String.format("-D%s=\"%s\"", key, value);
    }

  }

}
