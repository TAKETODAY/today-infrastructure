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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.toolchain.ToolchainManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import cn.taketoday.infra.maven.CommandLineBuilder.ClasspathBuilder;

/**
 * Abstract base class for AOT processing MOJOs.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractAotMojo extends AbstractDependencyFilterMojo {

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
   * Skip the execution.
   */
  @Parameter(property = "infra.aot.skip", defaultValue = "false")
  private boolean skip;

  /**
   * List of JVM system properties to pass to the AOT process.
   */
  @Parameter
  private Map<String, String> systemPropertyVariables;

  /**
   * JVM arguments that should be associated with the AOT process. On command line, make
   * sure to wrap multiple values between quotes.
   */
  @Parameter(property = "infra.aot.jvmArguments")
  private String jvmArguments;

  /**
   * Arguments that should be provided to the AOT compile process. On command line, make
   * sure to wrap multiple values between quotes.
   */
  @Parameter(property = "infra.aot.compilerArguments")
  private String compilerArguments;

  protected final MavenSession getSession() {
    return this.session;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (this.skip) {
      getLog().debug("Skipping AOT execution as per configuration");
      return;
    }
    try {
      executeAot();
    }
    catch (Exception ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    }
  }

  protected abstract void executeAot() throws Exception;

  protected void generateAotAssets(URL[] classPath, String processorClassName, String... arguments) throws Exception {
    List<String> command = CommandLineBuilder.forMainClass(processorClassName)
            .withSystemProperties(this.systemPropertyVariables)
            .withJvmArguments(new RunArguments(this.jvmArguments).asArray())
            .withClasspath(classPath)
            .withArguments(arguments)
            .build();
    if (getLog().isDebugEnabled()) {
      getLog().debug("Generating AOT assets using command: " + command);
    }
    JavaProcessExecutor processExecutor = new JavaProcessExecutor(this.session, this.toolchainManager);
    processExecutor.run(this.project.getBasedir(), command, Collections.emptyMap());
  }

  protected final void compileSourceFiles(URL[] classPath, File sourcesDirectory, File outputDirectory)
          throws Exception {
    List<Path> sourceFiles;
    try (Stream<Path> pathStream = Files.walk(sourcesDirectory.toPath())) {
      sourceFiles = pathStream.filter(Files::isRegularFile).toList();
    }
    if (sourceFiles.isEmpty()) {
      return;
    }
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      JavaCompilerPluginConfiguration compilerConfiguration = new JavaCompilerPluginConfiguration(this.project);
      List<String> options = new ArrayList<>();
      options.add("-cp");
      options.add(ClasspathBuilder.build(Arrays.asList(classPath)));
      options.add("-d");
      options.add(outputDirectory.toPath().toAbsolutePath().toString());
      String releaseVersion = compilerConfiguration.getReleaseVersion();
      if (releaseVersion != null) {
        options.add("--release");
        options.add(releaseVersion);
      }
      else {
        options.add("--source");
        options.add(compilerConfiguration.getSourceMajorVersion());
        options.add("--target");
        options.add(compilerConfiguration.getTargetMajorVersion());
      }
      options.addAll(new RunArguments(this.compilerArguments).getArgs());
      Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromPaths(sourceFiles);
      Errors errors = new Errors();
      CompilationTask task = compiler.getTask(null, fileManager, errors, options, null, compilationUnits);
      boolean result = task.call();
      if (!result || errors.hasReportedErrors()) {
        throw new IllegalStateException("Unable to compile generated source" + errors);
      }
    }
  }

  protected final URL[] getClassPath(File[] directories, ArtifactsFilter... artifactFilters)
          throws MojoExecutionException {
    List<URL> urls = new ArrayList<>();
    Arrays.stream(directories).map(this::toURL).forEach(urls::add);
    urls.addAll(getDependencyURLs(artifactFilters));
    return urls.toArray(URL[]::new);
  }

  protected final void copyAll(Path from, Path to) throws IOException {
    if (!Files.exists(from)) {
      return;
    }
    List<Path> files;
    try (Stream<Path> pathStream = Files.walk(from)) {
      files = pathStream.filter(Files::isRegularFile).toList();
    }
    for (Path file : files) {
      String relativeFileName = file.subpath(from.getNameCount(), file.getNameCount()).toString();
      getLog().debug("Copying '" + relativeFileName + "' to " + to);
      Path target = to.resolve(relativeFileName);
      Files.createDirectories(target.getParent());
      Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * {@link DiagnosticListener} used to collect errors.
   */
  protected static class Errors implements DiagnosticListener<JavaFileObject> {

    private final StringBuilder message = new StringBuilder();

    @Override
    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
      if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
        this.message.append("\n");
        this.message.append(diagnostic.getMessage(Locale.getDefault()));
        if (diagnostic.getSource() != null) {
          this.message.append(" ");
          this.message.append(diagnostic.getSource().getName());
          this.message.append(" ");
          this.message.append(diagnostic.getLineNumber()).append(":").append(diagnostic.getColumnNumber());
        }
      }
    }

    boolean hasReportedErrors() {
      return this.message.length() > 0;
    }

    @Override
    public String toString() {
      return this.message.toString();
    }

  }

}
