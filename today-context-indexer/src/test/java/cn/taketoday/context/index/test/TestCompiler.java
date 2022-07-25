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

package cn.taketoday.context.index.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import javax.annotation.processing.Processor;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Wrapper to make the {@link JavaCompiler} easier to use in tests.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
public class TestCompiler {

  public static final File ORIGINAL_SOURCE_FOLDER = new File("src/test/java");

  private final JavaCompiler compiler;

  private final StandardJavaFileManager fileManager;

  private final File outputLocation;

  public TestCompiler(Path tempDir) throws IOException {
    this(ToolProvider.getSystemJavaCompiler(), tempDir);
  }

  public TestCompiler(JavaCompiler compiler, Path tempDir) throws IOException {
    this.compiler = compiler;
    this.fileManager = compiler.getStandardFileManager(null, null, null);
    this.outputLocation = tempDir.toFile();
    Iterable<? extends File> temp = Collections.singletonList(this.outputLocation);
    this.fileManager.setLocation(StandardLocation.CLASS_OUTPUT, temp);
    this.fileManager.setLocation(StandardLocation.SOURCE_OUTPUT, temp);
  }

  public TestCompilationTask getTask(Class<?>... types) {
    return getTask(Arrays.stream(types).map(Class::getName).toArray(String[]::new));
  }

  public TestCompilationTask getTask(String... types) {
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

  private Iterable<? extends JavaFileObject> getJavaFileObjects(String... types) {
    File[] files = new File[types.length];
    for (int i = 0; i < types.length; i++) {
      files[i] = getFile(types[i]);
    }
    return this.fileManager.getJavaFileObjects(files);
  }

  private File getFile(String type) {
    return new File(getSourceFolder(), sourcePathFor(type));
  }

  private static String sourcePathFor(String type) {
    return type.replace(".", "/") + ".java";
  }

  private File getSourceFolder() {
    return ORIGINAL_SOURCE_FOLDER;
  }

  /**
   * A compilation task.
   */
  public static class TestCompilationTask {

    private final JavaCompiler.CompilationTask task;

    public TestCompilationTask(JavaCompiler.CompilationTask task) {
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
