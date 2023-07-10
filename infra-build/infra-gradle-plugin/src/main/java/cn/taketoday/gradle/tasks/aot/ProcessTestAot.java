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

package cn.taketoday.gradle.tasks.aot;

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
public class ProcessTestAot extends AbstractAot {

  private FileCollection classpathRoots;

  public ProcessTestAot() {
    getMainClass().set("cn.taketoday.framework.test.context.InfraTestAotProcessor");
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
