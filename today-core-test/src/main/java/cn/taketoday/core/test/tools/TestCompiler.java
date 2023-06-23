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

package cn.taketoday.core.test.tools;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import cn.taketoday.lang.Nullable;

/**
 * Utility that can be used to dynamically compile and test Java source code.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @see #forSystem()
 * @since 4.0
 */
public final class TestCompiler {

  @Nullable
  private final ClassLoader classLoader;

  private final JavaCompiler compiler;

  private final SourceFiles sourceFiles;

  private final ResourceFiles resourceFiles;

  private final ClassFiles classFiles;

  private final List<Processor> processors;

  private TestCompiler(@Nullable ClassLoader classLoader, JavaCompiler compiler,
          SourceFiles sourceFiles, ResourceFiles resourceFiles, ClassFiles classFiles,
          List<Processor> processors) {

    this.classLoader = classLoader;
    this.compiler = compiler;
    this.sourceFiles = sourceFiles;
    this.resourceFiles = resourceFiles;
    this.classFiles = classFiles;
    this.processors = processors;
  }

  /**
   * Create a new {@code TestCompiler} backed by the system java compiler.
   *
   * @return a new {@code TestCompiler} instance
   */
  public static TestCompiler forSystem() {
    return forCompiler(ToolProvider.getSystemJavaCompiler());
  }

  /**
   * Create a new {@code TestCompiler} backed by the given {@link JavaCompiler}.
   *
   * @param javaCompiler the java compiler to use
   * @return a new {@code TestCompiler} instance
   */
  public static TestCompiler forCompiler(JavaCompiler javaCompiler) {
    return new TestCompiler(null, javaCompiler, SourceFiles.none(),
            ResourceFiles.none(), ClassFiles.none(), Collections.emptyList());
  }

  /**
   * Apply customization to this compiler.
   *
   * @param customizer the customizer to call
   * @return a new {@code TestCompiler} instance with the customizations applied
   */
  public TestCompiler with(UnaryOperator<TestCompiler> customizer) {
    return customizer.apply(this);
  }

  /**
   * Create a new {@code TestCompiler} instance with additional source files.
   *
   * @param sourceFiles the additional source files
   * @return a new {@code TestCompiler} instance
   */
  public TestCompiler withSources(SourceFile... sourceFiles) {
    return new TestCompiler(this.classLoader, this.compiler,
            this.sourceFiles.and(sourceFiles), this.resourceFiles,
            this.classFiles, this.processors);
  }

  /**
   * Create a new {@code TestCompiler} instance with additional source files.
   *
   * @param sourceFiles the additional source files
   * @return a new {@code TestCompiler} instance
   */
  public TestCompiler withSources(Iterable<SourceFile> sourceFiles) {
    return new TestCompiler(this.classLoader, this.compiler,
            this.sourceFiles.and(sourceFiles), this.resourceFiles,
            this.classFiles, this.processors);
  }

  /**
   * Create a new {@code TestCompiler} instance with additional source files.
   *
   * @param sourceFiles the additional source files
   * @return a new {@code TestCompiler} instance
   */
  public TestCompiler withSources(SourceFiles sourceFiles) {
    return new TestCompiler(this.classLoader, this.compiler,
            this.sourceFiles.and(sourceFiles), this.resourceFiles,
            this.classFiles, this.processors);
  }

  /**
   * Create a new {@code TestCompiler} instance with additional resource files.
   *
   * @param resourceFiles the additional resource files
   * @return a new {@code TestCompiler} instance
   */
  public TestCompiler withResources(ResourceFile... resourceFiles) {
    return new TestCompiler(this.classLoader, this.compiler, this.sourceFiles,
            this.resourceFiles.and(resourceFiles), this.classFiles, this.processors);
  }

  /**
   * Create a new {@code TestCompiler} instance with additional source files.
   *
   * @param resourceFiles the additional source files
   * @return a new {@code TestCompiler} instance
   */
  public TestCompiler withResources(Iterable<ResourceFile> resourceFiles) {
    return new TestCompiler(this.classLoader, this.compiler, this.sourceFiles,
            this.resourceFiles.and(resourceFiles), this.classFiles, this.processors);
  }

  /**
   * Create a new {@code TestCompiler} instance with additional resource files.
   *
   * @param resourceFiles the additional resource files
   * @return a new {@code TestCompiler} instance
   */
  public TestCompiler withResources(ResourceFiles resourceFiles) {
    return new TestCompiler(this.classLoader, this.compiler, this.sourceFiles,
            this.resourceFiles.and(resourceFiles), this.classFiles, this.processors);
  }

  /**
   * Create a new {@code TestCompiler} instance with additional classes.
   *
   * @param classFiles the additional classes
   * @return a new {@code TestCompiler} instance
   */
  public TestCompiler withClasses(Iterable<ClassFile> classFiles) {
    return new TestCompiler(this.classLoader, this.compiler, this.sourceFiles,
            this.resourceFiles, this.classFiles.and(classFiles), this.processors);
  }

  /**
   * Create a new {@code TestCompiler} instance with additional annotation processors.
   *
   * @param processors the additional annotation processors
   * @return a new {@code TestCompiler} instance
   */
  public TestCompiler withProcessors(Processor... processors) {
    List<Processor> mergedProcessors = new ArrayList<>(this.processors);
    mergedProcessors.addAll(Arrays.asList(processors));
    return new TestCompiler(this.classLoader, this.compiler, this.sourceFiles,
            this.resourceFiles, this.classFiles, mergedProcessors);
  }

  /**
   * Create a new {@code TestCompiler} instance with additional annotation processors.
   *
   * @param processors the additional annotation processors
   * @return a new {@code TestCompiler} instance
   */
  public TestCompiler withProcessors(Iterable<Processor> processors) {
    List<Processor> mergedProcessors = new ArrayList<>(this.processors);
    processors.forEach(mergedProcessors::add);
    return new TestCompiler(this.classLoader, this.compiler, this.sourceFiles,
            this.resourceFiles, this.classFiles, mergedProcessors);
  }

  /**
   * Compile content from this instance along with the additional provided
   * content.
   *
   * @param content the additional content to compile
   * @param compiled a consumed used to further assert the compiled code
   * @throws CompilationException if source cannot be compiled
   */
  public void compile(WritableContent content, Consumer<Compiled> compiled) {
    compile(SourceFile.of(content), compiled);
  }

  /**
   * Compile content from this instance along with the additional provided
   * source file.
   *
   * @param sourceFile the additional source file to compile
   * @param compiled a consumed used to further assert the compiled code
   * @throws CompilationException if source cannot be compiled
   */
  public void compile(SourceFile sourceFile, Consumer<Compiled> compiled) {
    withSources(sourceFile).compile(compiled);
  }

  /**
   * Compile content from this instance along with the additional provided
   * source files.
   *
   * @param sourceFiles the additional source files to compile
   * @param compiled a consumed used to further assert the compiled code
   * @throws CompilationException if source cannot be compiled
   */
  public void compile(SourceFiles sourceFiles, Consumer<Compiled> compiled) {
    withSources(sourceFiles).compile(compiled);
  }

  /**
   * Compile content from this instance along with the additional provided
   * source and resource files.
   *
   * @param sourceFiles the additional source files to compile
   * @param resourceFiles the additional resource files to include
   * @param compiled a {@code Consumer} used to further assert the compiled code
   * @throws CompilationException if source cannot be compiled
   */
  public void compile(SourceFiles sourceFiles, ResourceFiles resourceFiles,
          Consumer<Compiled> compiled) {
    withSources(sourceFiles).withResources(resourceFiles).compile(compiled);
  }

  /**
   * Compile content from this instance.
   *
   * @param compiled a {@code Consumer} used to further assert the compiled code
   * @throws CompilationException if source cannot be compiled
   */
  public void compile(Consumer<Compiled> compiled) throws CompilationException {
    DynamicClassLoader dynamicClassLoader = compile();
    ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(dynamicClassLoader);
      compiled.accept(new Compiled(dynamicClassLoader, this.sourceFiles, this.resourceFiles));
    }
    catch (IllegalAccessError ex) {
      throw new IllegalAccessError(ex.getMessage() + ". " +
              "For non-public access ensure you annotate your test class or test method with @CompileWithForkedClassLoader");
    }
    finally {
      Thread.currentThread().setContextClassLoader(previousClassLoader);
    }
  }

  private DynamicClassLoader compile() {
    ClassLoader classLoaderToUse = (this.classLoader != null) ? this.classLoader
                                                              : Thread.currentThread().getContextClassLoader();
    List<DynamicJavaFileObject> compilationUnits = this.sourceFiles.stream().map(
            DynamicJavaFileObject::new).toList();
    StandardJavaFileManager standardFileManager = this.compiler.getStandardFileManager(
            null, null, null);
    DynamicJavaFileManager fileManager = new DynamicJavaFileManager(
            standardFileManager, classLoaderToUse, this.classFiles, this.resourceFiles);
    if (!this.sourceFiles.isEmpty()) {
      Errors errors = new Errors();
      CompilationTask task = this.compiler.getTask(null, fileManager, errors, null,
              null, compilationUnits);
      if (!this.processors.isEmpty()) {
        task.setProcessors(this.processors);
      }
      boolean result = task.call();
      if (!result || errors.hasReportedErrors()) {
        throw new CompilationException(errors.toString(), this.sourceFiles, this.resourceFiles);
      }
    }
    return new DynamicClassLoader(classLoaderToUse, this.classFiles, this.resourceFiles,
            fileManager.getDynamicClassFiles(), fileManager.getDynamicResourceFiles());
  }

  /**
   * Print the contents of the source and resource files to the specified
   * {@link PrintStream}.
   *
   * @param printStream the destination print stream
   * @return this instance
   */
  public TestCompiler printFiles(PrintStream printStream) {
    for (SourceFile sourceFile : this.sourceFiles) {
      printStream.append("---- source:   ").append(sourceFile.getPath()).append("\n\n");
      printStream.append(sourceFile.getContent());
      printStream.append("\n\n");
    }
    for (ResourceFile resourceFile : this.resourceFiles) {
      printStream.append("---- resource: ").append(resourceFile.getPath()).append("\n\n");
      printStream.append(resourceFile.getContent());
      printStream.append("\n\n");
    }
    return this;
  }

  /**
   * {@link DiagnosticListener} used to collect errors.
   */
  static class Errors implements DiagnosticListener<JavaFileObject> {

    private final StringBuilder message = new StringBuilder();

    @Override
    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
      if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
        this.message.append('\n');
        this.message.append(diagnostic.getMessage(Locale.getDefault()));
        if (diagnostic.getSource() != null) {
          this.message.append(' ');
          this.message.append(diagnostic.getSource().getName());
          this.message.append(' ');
          this.message.append(diagnostic.getLineNumber()).append(':')
                  .append(diagnostic.getColumnNumber());
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
