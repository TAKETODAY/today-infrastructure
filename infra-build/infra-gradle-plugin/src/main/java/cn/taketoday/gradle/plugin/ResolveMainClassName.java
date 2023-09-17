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

package cn.taketoday.gradle.plugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.Transformer;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.stream.Collectors;

import cn.taketoday.app.loader.tools.MainClassFinder;

/**
 * {@link Task} for resolving the name of the application's main class.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableCachingByDefault(because = "Not worth caching")
public class ResolveMainClassName extends DefaultTask {

  private static final String INFRA_APPLICATION_CLASS_NAME = "cn.taketoday.framework.InfraApplication";

  private final RegularFileProperty outputFile;

  private final Property<String> configuredMainClass;

  private FileCollection classpath;

  /**
   * Creates a new instance of the {@code ResolveMainClassName} task.
   */
  public ResolveMainClassName() {
    this.outputFile = getProject().getObjects().fileProperty();
    this.configuredMainClass = getProject().getObjects().property(String.class);
  }

  /**
   * Returns the classpath that the task will examine when resolving the main class
   * name.
   *
   * @return the classpath
   */
  @Classpath
  public FileCollection getClasspath() {
    return this.classpath;
  }

  /**
   * Sets the classpath that the task will examine when resolving the main class name.
   *
   * @param classpath the classpath
   */
  public void setClasspath(FileCollection classpath) {
    setClasspath((Object) classpath);
  }

  /**
   * Sets the classpath that the task will examine when resolving the main class name.
   * The given {@code classpath} is evaluated as per {@link Project#files(Object...)}.
   *
   * @param classpath the classpath
   */
  public void setClasspath(Object classpath) {
    this.classpath = getProject().files(classpath);
  }

  /**
   * Returns the property for the task's output file that will contain the name of the
   * main class.
   *
   * @return the output file
   */
  @OutputFile
  public RegularFileProperty getOutputFile() {
    return this.outputFile;
  }

  /**
   * Returns the property for the explicitly configured main class name that should be
   * used in favor of resolving the main class name from the classpath.
   *
   * @return the configured main class name property
   */
  @Input
  @Optional
  public Property<String> getConfiguredMainClassName() {
    return this.configuredMainClass;
  }

  @TaskAction
  void resolveAndStoreMainClassName() throws IOException {
    File outputFile = this.outputFile.getAsFile().get();
    outputFile.getParentFile().mkdirs();
    String mainClassName = resolveMainClassName();
    Files.writeString(outputFile.toPath(), mainClassName, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);
  }

  private String resolveMainClassName() {
    String configuredMainClass = this.configuredMainClass.getOrNull();
    if (configuredMainClass != null) {
      return configuredMainClass;
    }
    return getClasspath().filter(File::isDirectory)
            .getFiles()
            .stream()
            .map(this::findMainClass)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse("");
  }

  private String findMainClass(File file) {
    try {
      return MainClassFinder.findSingleMainClass(file, INFRA_APPLICATION_CLASS_NAME);
    }
    catch (IOException ex) {
      return null;
    }
  }

  Provider<String> readMainClassName() {
    String classpath = getClasspath().filter(File::isDirectory)
            .getFiles()
            .stream()
            .map(directory -> getProject().getProjectDir().toPath().relativize(directory.toPath()))
            .map(Path::toString)
            .collect(Collectors.joining(","));
    return this.outputFile.map(new ClassNameReader(classpath));
  }

  private static final class ClassNameReader implements Transformer<String, RegularFile> {

    private final String classpath;

    private ClassNameReader(String classpath) {
      this.classpath = classpath;
    }

    @Override
    public String transform(RegularFile file) {
      if (file.getAsFile().length() == 0) {
        throw new InvalidUserDataException(
                "Main class name has not been configured and it could not be resolved from classpath "
                        + this.classpath);
      }
      Path output = file.getAsFile().toPath();
      try {
        return Files.readString(output);
      }
      catch (IOException ex) {
        throw new RuntimeException("Failed to read main class name from '" + output + "'");
      }
    }

  }

}
