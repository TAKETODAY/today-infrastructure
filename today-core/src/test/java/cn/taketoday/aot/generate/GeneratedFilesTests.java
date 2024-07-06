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

package cn.taketoday.aot.generate;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.lang.model.element.Modifier;

import cn.taketoday.aot.generate.GeneratedFiles.FileHandler;
import cn.taketoday.aot.generate.GeneratedFiles.Kind;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.InputStreamSource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.javapoet.JavaFile;
import cn.taketoday.javapoet.MethodSpec;
import cn.taketoday.javapoet.TypeSpec;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link GeneratedFiles}.
 *
 * @author Phillip Webb
 */
class GeneratedFilesTests {

  private final TestGeneratedFiles generatedFiles = new TestGeneratedFiles();

  @Test
  void addSourceFileWithJavaFileAddsFile() throws Exception {
    MethodSpec main = MethodSpec.methodBuilder("main")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC).returns(void.class)
            .addParameter(String[].class, "args")
            .addStatement("$T.out.println($S)", System.class, "Hello, World!")
            .build();
    TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL).addMethod(main).build();
    JavaFile javaFile = JavaFile.builder("com.example", helloWorld).build();
    this.generatedFiles.addSourceFile(javaFile);
    assertThatFileAdded(Kind.SOURCE, "com/example/HelloWorld.java")
            .contains("Hello, World!");
  }

  @Test
  void addSourceFileWithJavaFileInTheDefaultPackageThrowsException() {
    TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld").build();
    JavaFile javaFile = JavaFile.builder("", helloWorld).build();
    assertThatIllegalArgumentException().isThrownBy(() -> this.generatedFiles.addSourceFile(javaFile))
            .withMessage("Could not add 'HelloWorld', processing classes in the "
                    + "default package is not supported. Did you forget to add a package statement?");
  }

  @Test
  void addSourceFileWithCharSequenceAddsFile() throws Exception {
    this.generatedFiles.addSourceFile("com.example.HelloWorld", "{}");
    assertThatFileAdded(Kind.SOURCE, "com/example/HelloWorld.java").isEqualTo("{}");
  }

  @Test
  void addSourceFileWithCharSequenceWhenClassNameIsEmptyThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.generatedFiles.addSourceFile("", "{}"))
            .withMessage("'className' must not be empty");
  }

  @Test
  void addSourceFileWithCharSequenceWhenClassNameIsInTheDefaultPackageThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.generatedFiles.addSourceFile("HelloWorld", "{}"))
            .withMessage("Could not add 'HelloWorld', processing classes in the "
                    + "default package is not supported. Did you forget to add a package statement?");
  }

  @Test
  void addSourceFileWithCharSequenceWhenClassNameIsInvalidThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.generatedFiles
                    .addSourceFile("com/example/HelloWorld.java", "{}"))
            .withMessage("'className' must be a valid identifier, got 'com/example/HelloWorld.java'");
  }

  @Test
  void addSourceFileWithConsumedAppendableAddsFile() throws Exception {
    this.generatedFiles.addSourceFile("com.example.HelloWorld",
            appendable -> appendable.append("{}"));
    assertThatFileAdded(Kind.SOURCE, "com/example/HelloWorld.java").isEqualTo("{}");
  }

  @Test
  void addSourceFileWithInputStreamSourceAddsFile() throws Exception {
    Resource resource = new ByteArrayResource("{}".getBytes(StandardCharsets.UTF_8));
    this.generatedFiles.addSourceFile("com.example.HelloWorld", resource);
    assertThatFileAdded(Kind.SOURCE, "com/example/HelloWorld.java").isEqualTo("{}");
  }

  @Test
  void addResourceFileWithCharSequenceAddsFile() throws Exception {
    this.generatedFiles.addResourceFile("META-INF/file", "test");
    assertThatFileAdded(Kind.RESOURCE, "META-INF/file").isEqualTo("test");
  }

  @Test
  void addResourceFileWithConsumedAppendableAddsFile() throws Exception {
    this.generatedFiles.addResourceFile("META-INF/file",
            appendable -> appendable.append("test"));
    assertThatFileAdded(Kind.RESOURCE, "META-INF/file").isEqualTo("test");
  }

  @Test
  void addResourceFileWithInputStreamSourceAddsFile() throws IOException {
    Resource resource = new ByteArrayResource(
            "test".getBytes(StandardCharsets.UTF_8));
    this.generatedFiles.addResourceFile("META-INF/file", resource);
    assertThatFileAdded(Kind.RESOURCE, "META-INF/file").isEqualTo("test");
  }

  @Test
  void addClassFileWithInputStreamSourceAddsFile() throws IOException {
    Resource resource = new ByteArrayResource(
            "test".getBytes(StandardCharsets.UTF_8));
    this.generatedFiles.addClassFile("com/example/HelloWorld.class", resource);
    assertThatFileAdded(Kind.CLASS, "com/example/HelloWorld.class").isEqualTo("test");
  }

  @Test
  void addFileWithCharSequenceAddsFile() throws Exception {
    this.generatedFiles.addFile(Kind.RESOURCE, "META-INF/file", "test");
    assertThatFileAdded(Kind.RESOURCE, "META-INF/file").isEqualTo("test");
  }

  @Test
  void addFileWithConsumedAppendableAddsFile() throws IOException {
    this.generatedFiles.addFile(Kind.SOURCE, "com/example/HelloWorld.java",
            appendable -> appendable.append("{}"));
    assertThatFileAdded(Kind.SOURCE, "com/example/HelloWorld.java").isEqualTo("{}");
  }

  @Test
  void handleFileWhenFileDoesNotExist() throws IOException {
    this.generatedFiles.setFileHandler(new TestFileHandler());
    AtomicBoolean called = new AtomicBoolean(false);
    this.generatedFiles.handleFile(Kind.RESOURCE, "META-INF/test", handler -> {
      called.set(true);
      handler.create(createSource("content"));
    });
    assertThat(called).isTrue();
    assertThatFileAdded(Kind.RESOURCE, "META-INF/test").isEqualTo("content").hasOverride(false);
  }

  @Test
  void handleFileWhenFileExistsCanOverride() throws IOException {
    this.generatedFiles.setFileHandler(new TestFileHandler(createSource("existing")));
    AtomicBoolean called = new AtomicBoolean(false);
    this.generatedFiles.handleFile(Kind.RESOURCE, "META-INF/test", handler -> {
      called.set(true);
      handler.override(createSource("overridden"));
    });
    assertThat(called).isTrue();
    assertThatFileAdded(Kind.RESOURCE, "META-INF/test").isEqualTo("overridden").hasOverride(true);
  }

  @Test
  void handleFileWhenFileExistsCanOverrideUsingExistingContent() throws IOException {
    this.generatedFiles.setFileHandler(new TestFileHandler(createSource("existing")));
    AtomicBoolean called = new AtomicBoolean(false);
    this.generatedFiles.handleFile(Kind.RESOURCE, "META-INF/test", handler -> {
      called.set(true);
      String existing = readSource(handler.getContent());
      handler.override(createSource(existing + "-override"));
    });
    assertThat(called).isTrue();
    assertThatFileAdded(Kind.RESOURCE, "META-INF/test").isEqualTo("existing-override").hasOverride(true);
  }

  @Test
  void handleFileWhenFileExistsFailedToCreate() {
    TestFileHandler fileHandler = new TestFileHandler(createSource("existing"));
    this.generatedFiles.setFileHandler(fileHandler);
    assertThatIllegalStateException()
            .isThrownBy(() -> this.generatedFiles.handleFile(Kind.RESOURCE, "META-INF/test", handler ->
                    handler.create(createSource("should fail"))))
            .withMessage("%s already exists".formatted(fileHandler));
  }

  private static InputStreamSource createSource(String content) {
    return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
  }

  private static String readSource(InputStreamSource content) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    content.getInputStream().transferTo(out);
    return out.toString(StandardCharsets.UTF_8);
  }

  private GeneratedFileAssert assertThatFileAdded(Kind kind, String path)
          throws IOException {
    return this.generatedFiles.assertThatFileAdded(kind, path);
  }

  static class TestGeneratedFiles extends GeneratedFiles {

    private Kind kind;

    private String path;

    private TestFileHandler fileHandler = new TestFileHandler();

    void setFileHandler(TestFileHandler fileHandler) {
      this.fileHandler = fileHandler;
    }

    @Override
    public void handleFile(Kind kind, String path, ThrowingConsumer<FileHandler> handler) {
      this.kind = kind;
      this.path = path;
      handler.accept(this.fileHandler);
    }

    GeneratedFileAssert assertThatFileAdded(Kind kind, String path)
            throws IOException {
      assertThat(this.kind).as("kind").isEqualTo(kind);
      assertThat(this.path).as("path").isEqualTo(path);
      assertThat(this.fileHandler.content).as("content").isNotNull();
      return new GeneratedFileAssert(this.fileHandler);
    }
  }

  private static class GeneratedFileAssert extends AbstractStringAssert<GeneratedFileAssert> {

    private final TestFileHandler fileHandler;

    GeneratedFileAssert(TestFileHandler fileHandler) throws IOException {
      super(readSource(fileHandler.content), GeneratedFileAssert.class);
      this.fileHandler = fileHandler;
    }

    public GeneratedFileAssert hasOverride(boolean expected) {
      assertThat(this.fileHandler.override).isEqualTo(expected);
      return this.myself;
    }
  }

  private static class TestFileHandler extends FileHandler {

    @Nullable
    private InputStreamSource content;

    @Nullable
    private Boolean override;

    TestFileHandler(@Nullable InputStreamSource content) {
      super(content != null, () -> content);
      this.content = content;
    }

    TestFileHandler() {
      this(null);
    }

    @Override
    protected void copy(InputStreamSource content, boolean override) {
      this.content = content;
      this.override = override;
    }
  }

}
