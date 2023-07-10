/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.gradle.tasks.bundling;

import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;

import java.util.Set;

/**
 * A Infra "fat" archive task.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface InfraArchive extends Task {

  /**
   * Returns the fully-qualified name of the application's main class.
   *
   * @return the fully-qualified name of the application's main class
   */
  @Input
  Property<String> getMainClass();

  /**
   * Adds Ant-style patterns that identify files that must be unpacked from the archive
   * when it is launched.
   *
   * @param patterns the patterns
   */
  void requiresUnpack(String... patterns);

  /**
   * Adds a spec that identifies files that must be unpacked from the archive when it is
   * launched.
   *
   * @param spec the spec
   */
  void requiresUnpack(Spec<FileTreeElement> spec);

  /**
   * Returns the {@link LaunchScriptConfiguration} that will control the script that is
   * prepended to the archive.
   *
   * @return the launch script configuration, or {@code null} if the launch script has
   * not been configured.
   */
  @Nested
  @Optional
  LaunchScriptConfiguration getLaunchScript();

  /**
   * Configures the archive to have a prepended launch script.
   */
  void launchScript();

  /**
   * Configures the archive to have a prepended launch script, customizing its
   * configuration using the given {@code action}.
   *
   * @param action the action to apply
   */
  void launchScript(Action<LaunchScriptConfiguration> action);

  /**
   * Returns the classpath that will be included in the archive.
   *
   * @return the classpath
   */
  @Optional
  @Classpath
  FileCollection getClasspath();

  /**
   * Adds files to the classpath to include in the archive. The given {@code classpath}
   * is evaluated as per {@link Project#files(Object...)}.
   *
   * @param classpath the additions to the classpath
   */
  void classpath(Object... classpath);

  /**
   * Sets the classpath to include in the archive. The given {@code classpath} is
   * evaluated as per {@link Project#files(Object...)}.
   *
   * @param classpath the classpath
   */
  void setClasspath(Object classpath);

  /**
   * Sets the classpath to include in the archive.
   *
   * @param classpath the classpath
   */
  void setClasspath(FileCollection classpath);

  /**
   * Returns the target Java version of the project (e.g. as provided by the
   * {@code targetCompatibility} build property).
   *
   * @return the target Java version
   */
  @Input
  @Optional
  Property<JavaVersion> getTargetJavaVersion();

  /**
   * Registers the given lazily provided {@code resolvedArtifacts}. They are used to map
   * from the files in the {@link #getClasspath classpath} to their dependency
   * coordinates.
   *
   * @param resolvedArtifacts the lazily provided resolved artifacts
   */
  void resolvedArtifacts(Provider<Set<ResolvedArtifactResult>> resolvedArtifacts);

}
