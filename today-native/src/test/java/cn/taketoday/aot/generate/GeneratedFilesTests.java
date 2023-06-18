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

package cn.taketoday.aot.generate;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.lang.model.element.Modifier;

import cn.taketoday.aot.generate.GeneratedFiles.Kind;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.InputStreamSource;
import cn.taketoday.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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
  void addSourceFileWithCharSequenceWhenClassNameIsInvalidThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.generatedFiles
                    .addSourceFile("com/example/HelloWorld.java", "{}"))
            .withMessage("'className' must be a valid identifier");
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

  private AbstractStringAssert<?> assertThatFileAdded(Kind kind, String path)
          throws IOException {
    return this.generatedFiles.assertThatFileAdded(kind, path);
  }

  static class TestGeneratedFiles implements GeneratedFiles {

    private Kind kind;

    private String path;

    private InputStreamSource content;

    @Override
    public void addFile(Kind kind, String path, InputStreamSource content) {
      this.kind = kind;
      this.path = path;
      this.content = content;
    }

    AbstractStringAssert<?> assertThatFileAdded(Kind kind, String path)
            throws IOException {
      assertThat(this.kind).as("kind").isEqualTo(kind);
      assertThat(this.path).as("path").isEqualTo(path);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      this.content.getInputStream().transferTo(out);
      return assertThat(out.toString(StandardCharsets.UTF_8));
    }

  }

}
