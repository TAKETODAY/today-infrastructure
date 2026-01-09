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

package infra.gradle.tasks.aot;

import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom {@link JavaExec} task for ahead-of-time processing of a Infra
 * application's tests.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@CacheableTask
public abstract class ProcessTestAot extends AbstractAot {

  private FileCollection classpathRoots;

  public ProcessTestAot() {
    getMainClass().set("infra.test.context.InfraTestAotProcessor");
  }

  /**
   * Returns the classpath roots that should be scanned for test classes to process.
   *
   * @return the classpath roots
   */
  @InputFiles
  @PathSensitive(PathSensitivity.RELATIVE)
  public final FileCollection getClasspathRoots() {
    return this.classpathRoots;
  }

  /**
   * Sets the classpath roots that should be scanned for test classes to process.
   *
   * @param classpathRoots the classpath roots
   */
  public void setClasspathRoots(FileCollection classpathRoots) {
    this.classpathRoots = classpathRoots;
  }

  @InputFiles
  @SkipWhenEmpty
  @IgnoreEmptyDirectories
  @PathSensitive(PathSensitivity.RELATIVE)
  final FileTree getInputClasses() {
    return this.classpathRoots.getAsFileTree();
  }

  @Override
  @TaskAction
  public void exec() {
    List<String> args = new ArrayList<>();
    args.add(getClasspathRoots().getFiles()
            .stream()
            .filter(File::exists)
            .map(File::getAbsolutePath)
            .collect(Collectors.joining(File.pathSeparator)));
    args.addAll(processorArgs());
    setArgs(args);
    super.exec();
  }

}
