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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Compiled}.
 *
 * @author Phillip Webb
 */
class CompiledTests {

  private static final String HELLO_WORLD = """
          package com.example;

          public class HelloWorld implements java.util.function.Supplier<String> {

          	public String get() {
          		return "Hello World!";
          	}

          }
          """;

  private static final String HELLO_SPRING = """
          package com.example;

          public class HelloSpring implements java.util.function.Supplier<String> {

          	public String get() {
          		return "Hello Spring!"; // !!
          	}

          }
          """;

  @Test
  void getSourceFileWhenSingleReturnsSourceFile() {
    SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
    TestCompiler.forSystem().compile(sourceFile,
            compiled -> assertThat(compiled.getSourceFile()).isSameAs(sourceFile));
  }

  @Test
  void getSourceFileWhenMultipleThrowsException() {
    SourceFiles sourceFiles = SourceFiles.of(SourceFile.of(HELLO_WORLD),
            SourceFile.of(HELLO_SPRING));
    TestCompiler.forSystem().compile(sourceFiles,
            compiled -> assertThatIllegalStateException().isThrownBy(
                    compiled::getSourceFile));
  }

  @Test
  void getSourceFileWhenNoneThrowsException() {
    TestCompiler.forSystem().compile(
            compiled -> assertThatIllegalStateException().isThrownBy(
                    compiled::getSourceFile));
  }

  @Test
  void getSourceFilesReturnsSourceFiles() {
    SourceFiles sourceFiles = SourceFiles.of(SourceFile.of(HELLO_WORLD),
            SourceFile.of(HELLO_SPRING));
    TestCompiler.forSystem().compile(sourceFiles,
            compiled -> assertThat(compiled.getSourceFiles()).isEqualTo(sourceFiles));
  }

  @Test
  void getResourceFileWhenSingleReturnsSourceFile() {
    ResourceFile resourceFile = ResourceFile.of("META-INF/myfile", "test");
    TestCompiler.forSystem().withResources(resourceFile).compile(
            compiled -> assertThat(compiled.getResourceFile()).isSameAs(
                    resourceFile));
  }

  @Test
  void getResourceFileWhenMultipleThrowsException() {
    ResourceFiles resourceFiles = ResourceFiles.of(
            ResourceFile.of("META-INF/myfile1", "test1"),
            ResourceFile.of("META-INF/myfile2", "test2"));
    TestCompiler.forSystem().withResources(resourceFiles).compile(
            compiled -> assertThatIllegalStateException().isThrownBy(compiled::getResourceFile));
  }

  @Test
  void getResourceFileWhenNoneThrowsException() {
    TestCompiler.forSystem().compile(
            compiled -> assertThatIllegalStateException().isThrownBy(compiled::getResourceFile));
  }

  @Test
  void getResourceFilesReturnsResourceFiles() {
    ResourceFiles resourceFiles = ResourceFiles.of(
            ResourceFile.of("META-INF/myfile1", "test1"),
            ResourceFile.of("META-INF/myfile2", "test2"));
    TestCompiler.forSystem().withResources(resourceFiles).compile(
            compiled -> assertThat(compiled.getResourceFiles()).isEqualTo(
                    resourceFiles));
  }

  @Test
  void getInstanceWhenNoneMatchesThrowsException() {
    TestCompiler.forSystem().compile(SourceFile.of(HELLO_WORLD),
            compiled -> assertThatIllegalStateException().isThrownBy(
                    () -> compiled.getInstance(Callable.class)));
  }

  @Test
  void getInstanceWhenMultipleMatchesThrowsException() {
    SourceFiles sourceFiles = SourceFiles.of(SourceFile.of(HELLO_WORLD),
            SourceFile.of(HELLO_SPRING));
    TestCompiler.forSystem().compile(sourceFiles,
            compiled -> assertThatIllegalStateException().isThrownBy(
                    () -> compiled.getInstance(Supplier.class)));
  }

  @Test
  void getInstanceWhenNoDefaultConstructorThrowsException() {
    SourceFile sourceFile = SourceFile.of("""
            package com.example;

            public class HelloWorld implements java.util.function.Supplier<String> {

            	public HelloWorld(String name) {
            	}

            	public String get() {
            		return "Hello World!";
            	}

            }
            """);
    TestCompiler.forSystem().compile(sourceFile,
            compiled -> assertThatIllegalStateException().isThrownBy(
                    () -> compiled.getInstance(Supplier.class)));
  }

  @Test
  void getInstanceReturnsInstance() {
    TestCompiler.forSystem().compile(SourceFile.of(HELLO_WORLD),
            compiled -> assertThat(compiled.getInstance(Supplier.class)).isNotNull());
  }

  @Test
  void getInstanceByNameReturnsInstance() {
    SourceFiles sourceFiles = SourceFiles.of(SourceFile.of(HELLO_WORLD),
            SourceFile.of(HELLO_SPRING));
    TestCompiler.forSystem().compile(sourceFiles,
            compiled -> assertThat(compiled.getInstance(Supplier.class,
                    "com.example.HelloWorld")).isNotNull());
  }

  @Test
  void getAllCompiledClassesReturnsCompiledClasses() {
    SourceFiles sourceFiles = SourceFiles.of(SourceFile.of(HELLO_WORLD),
            SourceFile.of(HELLO_SPRING));
    TestCompiler.forSystem().compile(sourceFiles, compiled -> {
      List<Class<?>> classes = compiled.getAllCompiledClasses();
      assertThat(classes.stream().map(Class::getName)).containsExactlyInAnyOrder(
              "com.example.HelloWorld", "com.example.HelloSpring");
    });
  }

}
