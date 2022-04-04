/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.test.compiler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.processing.Processor;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Wrapper to make the {@link JavaCompiler} easier to use in tests.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 4.0
 */
public class TestCompiler {

  /**
   * The default source directory.
   */
  public static final File SOURCE_DIRECTORY = new File("src/test/java");

  private final JavaCompiler compiler;

  private final StandardJavaFileManager fileManager;

  private final File outputLocation;

  public TestCompiler(File outputLocation) throws IOException {
    this(ToolProvider.getSystemJavaCompiler(), outputLocation);
  }

  public TestCompiler(JavaCompiler compiler, File outputLocation) throws IOException {
    this.compiler = compiler;
    this.fileManager = compiler.getStandardFileManager(null, null, null);
    this.outputLocation = outputLocation;
    this.outputLocation.mkdirs();
    Iterable<? extends File> temp = Collections.singletonList(this.outputLocation);
    this.fileManager.setLocation(StandardLocation.CLASS_OUTPUT, temp);
    this.fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, temp);
  }

  public TestCompilationTask getTask(Collection<File> sourceFiles) {
    Iterable<? extends JavaFileObject> javaFileObjects = this.fileManager.getJavaFileObjectsFromFiles(sourceFiles);
    return getTask(javaFileObjects);
  }

  public TestCompilationTask getTask(Class<?>... types) {
    Iterable<? extends JavaFileObject> javaFileObjects = getJavaFileObjects(types);
    return getTask(javaFileObjects);
  }

  private TestCompilationTask getTask(Iterable<? extends JavaFileObject> javaFileObjects) {
    return new TestCompilationTask(
            this.compiler.getTask(null, this.fileManager, null, null, null, javaFileObjects));
  }

  public File getOutputLocation() {
    return this.outputLocation;
  }

  private Iterable<? extends JavaFileObject> getJavaFileObjects(Class<?>... types) {
    File[] files = new File[types.length];
    for (int i = 0; i < types.length; i++) {
      files[i] = getFile(types[i]);
    }
    return this.fileManager.getJavaFileObjects(files);
  }

  protected File getFile(Class<?> type) {
    return new File(getSourceDirectory(), sourcePathFor(type));
  }

  public static String sourcePathFor(Class<?> type) {
    return type.getName().replace('.', '/') + ".java";
  }

  protected File getSourceDirectory() {
    return SOURCE_DIRECTORY;
  }

  /**
   * A compilation task.
   */
  public static class TestCompilationTask {

    private final CompilationTask task;

    public TestCompilationTask(CompilationTask task) {
      this.task = task;
    }

    public void call(Processor... processors) {
      this.task.setProcessors(Arrays.asList(processors));
      if (!this.task.call()) {
        throw new IllegalStateException("Compilation failed");
      }
    }

  }

}
