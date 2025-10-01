/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.aot.generate;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

import infra.core.io.InputStreamSource;
import infra.javapoet.JavaFile;
import infra.lang.Assert;
import infra.util.ClassUtils;
import infra.util.StringUtils;
import infra.util.function.ThrowingConsumer;

/**
 * Interface that can be used to add {@link Kind#SOURCE source},
 * {@link Kind#RESOURCE resource}, or {@link Kind#CLASS class} files generated
 * during ahead-of-time processing. Source and resource files are written using
 * UTF-8 encoding.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see InMemoryGeneratedFiles
 * @see FileSystemGeneratedFiles
 * @since 4.0
 */
public interface GeneratedFiles {

  /**
   * Add a generated {@link Kind#SOURCE source file} with content from the
   * given {@link JavaFile}.
   *
   * @param javaFile the java file to add
   */
  default void addSourceFile(JavaFile javaFile) {
    validatePackage(javaFile.packageName, javaFile.typeSpec.name);
    String className = javaFile.packageName + "." + javaFile.typeSpec.name;
    addSourceFile(className, javaFile::writeTo);
  }

  /**
   * Add a generated {@link Kind#SOURCE source file} with content from the
   * given {@link CharSequence}.
   *
   * @param className the class name that should be used to determine the path
   * of the file
   * @param content the contents of the file
   */
  default void addSourceFile(String className, CharSequence content) {
    addSourceFile(className, appendable -> appendable.append(content));
  }

  /**
   * Add a generated {@link Kind#SOURCE source file} with content written to
   * an {@link Appendable} passed to the given {@link ThrowingConsumer}.
   *
   * @param className the class name that should be used to determine the path
   * of the file
   * @param content a {@link ThrowingConsumer} that accepts an
   * {@link Appendable} which will receive the file contents
   */
  default void addSourceFile(String className, ThrowingConsumer<Appendable> content) {
    addFile(Kind.SOURCE, getClassNamePath(className), content);
  }

  /**
   * Add a generated {@link Kind#SOURCE source file} with content from the
   * given {@link InputStreamSource}.
   *
   * @param className the class name that should be used to determine the path
   * of the file
   * @param content an {@link InputStreamSource} that will provide an input
   * stream containing the file contents
   */
  default void addSourceFile(String className, InputStreamSource content) {
    addFile(Kind.SOURCE, getClassNamePath(className), content);
  }

  /**
   * Add a generated {@link Kind#RESOURCE resource file} with content from the
   * given {@link CharSequence}.
   *
   * @param path the relative path of the file
   * @param content the contents of the file
   */
  default void addResourceFile(String path, CharSequence content) {
    addResourceFile(path, appendable -> appendable.append(content));
  }

  /**
   * Add a generated {@link Kind#RESOURCE resource file} with content written
   * to an {@link Appendable} passed to the given {@link ThrowingConsumer}.
   *
   * @param path the relative path of the file
   * @param content a {@link ThrowingConsumer} that accepts an
   * {@link Appendable} which will receive the file contents
   */
  default void addResourceFile(String path, ThrowingConsumer<Appendable> content) {
    addFile(Kind.RESOURCE, path, content);
  }

  /**
   * Add a generated {@link Kind#RESOURCE resource file} with content from the
   * given {@link InputStreamSource}.
   *
   * @param path the relative path of the file
   * @param content an {@link InputStreamSource} that will provide an input
   * stream containing the file contents
   */
  default void addResourceFile(String path, InputStreamSource content) {
    addFile(Kind.RESOURCE, path, content);
  }

  /**
   * Add a generated {@link Kind#CLASS class file} with content from the given
   * {@link InputStreamSource}.
   *
   * @param path the relative path of the file
   * @param content an {@link InputStreamSource} that will provide an input
   * stream containing the file contents
   */
  default void addClassFile(String path, InputStreamSource content) {
    addFile(Kind.CLASS, path, content);
  }

  /**
   * Add a generated file of the specified {@link Kind} with content from the
   * given {@link CharSequence}.
   *
   * @param kind the kind of file being written
   * @param path the relative path of the file
   * @param content the contents of the file
   */
  default void addFile(Kind kind, String path, CharSequence content) {
    addFile(kind, path, appendable -> appendable.append(content));
  }

  /**
   * Add a generated file of the specified {@link Kind} with content written
   * to an {@link Appendable} passed to the given {@link ThrowingConsumer}.
   *
   * @param kind the kind of file being written
   * @param path the relative path of the file
   * @param content a {@link ThrowingConsumer} that accepts an
   * {@link Appendable} which will receive the file contents
   */
  default void addFile(Kind kind, String path, ThrowingConsumer<Appendable> content) {
    Assert.notNull(content, "'content' is required");
    addFile(kind, path, new AppendableConsumerInputStreamSource(content));
  }

  /**
   * Add a generated file of the specified {@link Kind} with content from the
   * given {@link InputStreamSource}.
   *
   * @param kind the kind of file being written
   * @param path the relative path of the file
   * @param content an {@link InputStreamSource} that will provide an input
   * stream containing the file contents
   */
  default void addFile(Kind kind, String path, InputStreamSource content) {
    Assert.notNull(kind, "'kind' is required");
    Assert.hasLength(path, "'path' must not be empty");
    Assert.notNull(content, "'content' is required");
    handleFile(kind, path, handler -> handler.create(content));
  }

  /**
   * Handle a generated file of the specified {@link Kind} with the given
   * {@linkplain FileHandler handler}. The file handler lets you consume
   * the content of the already generated file, if any and provide a way
   * to override its content if necessary.
   *
   * @param kind the kind of file
   * @param path the relative path of the file
   * @param handler a consumer of a {@link FileHandler} for the file
   */
  void handleFile(Kind kind, String path, ThrowingConsumer<FileHandler> handler);

  private static String getClassNamePath(String className) {
    Assert.hasLength(className, "'className' must not be empty");
    validatePackage(ClassUtils.getPackageName(className), className);
    Assert.isTrue(isJavaIdentifier(className),
            "'className' must be a valid identifier, got '" + className + "'");
    return ClassUtils.convertClassNameToResourcePath(className) + ".java";
  }

  private static void validatePackage(String packageName, String className) {
    if (StringUtils.isEmpty(packageName)) {
      throw new IllegalArgumentException("Could not add '" + className + "', "
              + "processing classes in the default package is not supported. "
              + "Did you forget to add a package statement?");
    }
  }

  private static boolean isJavaIdentifier(String className) {
    char[] chars = className.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      if (i == 0 && !Character.isJavaIdentifierStart(chars[i])) {
        return false;
      }
      if (i > 0 && chars[i] != '.' && !Character.isJavaIdentifierPart(chars[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * The various kinds of generated files that are supported.
   */
  enum Kind {

    /**
     * A source file containing Java code that should be compiled.
     */
    SOURCE,

    /**
     * A resource file that should be directly added to the final application.
     * For example, a {@code .properties} file.
     */
    RESOURCE,

    /**
     * A class file containing bytecode. For example, the result of a proxy
     * generated using CGLIB.
     */
    CLASS

  }

  /**
   * Provide access to a particular file and offer convenient methods to retrieve,
   * save, or override its content.
   */
  abstract class FileHandler {

    private final boolean exists;

    private final Supplier<InputStreamSource> existingContent;

    protected FileHandler(boolean exists, Supplier<InputStreamSource> existingContent) {
      this.exists = exists;
      this.existingContent = existingContent;
    }

    /**
     * Specify whether the file already exists.
     *
     * @return {@code true} if the file already exists
     */
    public boolean exists() {
      return this.exists;
    }

    /**
     * Return an {@link InputStreamSource} for the content of the file or
     * {@code null} if the file does not exist.
     */
    @Nullable
    public InputStreamSource getContent() {
      return (exists() ? this.existingContent.get() : null);
    }

    /**
     * Create a file with the given {@linkplain InputStreamSource content}.
     *
     * @throws IllegalStateException if the file already exists
     */
    public void create(InputStreamSource content) {
      Assert.notNull(content, "'content' is required");
      if (exists()) {
        throw new IllegalStateException("%s already exists".formatted(this));
      }
      copy(content, false);
    }

    /**
     * Override the content of the file handled by this instance using the
     * given {@linkplain InputStreamSource content}. If the file does not
     * exist, it is created.
     */
    public void override(InputStreamSource content) {
      Assert.notNull(content, "'content' is required");
      copy(content, true);
    }

    protected abstract void copy(InputStreamSource content, boolean override);

  }

}
