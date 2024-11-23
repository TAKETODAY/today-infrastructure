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

package cn.taketoday.core.test.tools;

import com.example.PublicInterface;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link TestCompiler}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class TestCompilerTests {

  private static final String HELLO_WORLD = """
          package com.example;

          import java.util.function.Supplier;

          @Deprecated
          public class Hello implements Supplier<String> {

          	public String get() {
          		return "Hello World!";
          	}

          }
          """;

  private static final String HELLO_SPRING = """
          package com.example;

          import java.util.function.Supplier;

          public class Hello implements Supplier<String> {

          	public String get() {
          		return "Hello Spring!"; // !!
          	}

          }
          """;

  private static final String HELLO_BAD = """
          package com.example;

          public class Hello implements Supplier<String> {

          	public String get() {
          		return "Missing Import!";
          	}

          }
          """;

  private static final String HELLO_DEPRECATED = """
          package com.example;

          import java.util.function.Supplier;

          public class Hello implements Supplier<String> {

          	@Deprecated
          	public String get() {
          		return "Hello Deprecated";
          	}

          }
          """;

  @Test
  @SuppressWarnings("unchecked")
  void compileWhenHasDifferentClassesWithSameClassNameCompilesBoth() {
    TestCompiler.forSystem().withSources(SourceFile.of(HELLO_WORLD)).compile(
            compiled -> {
              Supplier<String> supplier = compiled.getInstance(Supplier.class,
                      "com.example.Hello");
              assertThat(supplier.get()).isEqualTo("Hello World!");
            });
    TestCompiler.forSystem().withSources(SourceFile.of(HELLO_SPRING)).compile(
            compiled -> {
              Supplier<String> supplier = compiled.getInstance(Supplier.class,
                      "com.example.Hello");
              assertThat(supplier.get()).isEqualTo("Hello Spring!");
            });
  }

  @Test
  void compileAndGetSourceFile() {
    TestCompiler.forSystem().withSources(SourceFile.of(HELLO_SPRING)).compile(
            compiled -> assertThat(compiled.getSourceFile()).contains("// !!"));
  }

  @Test
  void compileWhenSourceHasCompileErrors() {
    assertThatExceptionOfType(CompilationException.class).isThrownBy(
            () -> TestCompiler.forSystem().withSources(
                    SourceFile.of(HELLO_BAD)).compile(compiled -> {
            }));
  }

  @Test
  @SuppressWarnings("unchecked")
  void compileWhenSourceUseDeprecateCodeAndNoOptionSet() {
    SourceFile main = SourceFile.of("""
            package com.example;

            public class Main {

            	public static void main(String[] args) {
            		new Hello().get();
            	}

            }
            """);
    TestCompiler.forSystem().withSources(
            SourceFile.of(HELLO_DEPRECATED), main).compile(compiled -> {
      Supplier<String> supplier = compiled.getInstance(Supplier.class,
              "com.example.Hello");
      assertThat(supplier.get()).isEqualTo("Hello Deprecated");
    });
  }

  @Test
  void compileWhenSourceUseDeprecateCodeAndFailOnWarningIsSet() {
    SourceFile main = SourceFile.of("""
            package com.example;

            public class Main {

            	public static void main(String[] args) {
            		new Hello().get();
            	}

            }
            """);
    assertThatExceptionOfType(CompilationException.class).isThrownBy(
            () -> TestCompiler.forSystem().failOnWarning().withSources(
                    SourceFile.of(HELLO_DEPRECATED), main).compile(compiled -> {
            }));
  }

  @Test
  @SuppressWarnings("unchecked")
  void compileWhenSourceUseDeprecateCodeAndFailOnWarningWithSuppressWarnings() {
    SourceFile main = SourceFile.of("""
            package com.example;

            public class Main {

            	@SuppressWarnings("deprecation")
            	public static void main(String[] args) {
            		new Hello().get();
            	}

            }
            """);
    TestCompiler.forSystem().failOnWarning().withSources(
            SourceFile.of(HELLO_DEPRECATED), main).compile(compiled -> {
      Supplier<String> supplier = compiled.getInstance(Supplier.class,
              "com.example.Hello");
      assertThat(supplier.get()).isEqualTo("Hello Deprecated");
    });
  }

  @Test
  void withSourcesArrayAddsSource() {
    SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
    TestCompiler.forSystem().withSources(sourceFile).compile(
            this::assertSuppliesHelloWorld);
  }

  @Test
  void withSourcesAddsSource() {
    SourceFiles sourceFiles = SourceFiles.of(SourceFile.of(HELLO_WORLD));
    TestCompiler.forSystem().withSources(sourceFiles).compile(
            this::assertSuppliesHelloWorld);
  }

  @Test
  void withResourcesArrayAddsResource() {
    ResourceFile resourceFile = ResourceFile.of("META-INF/myfile", "test");
    TestCompiler.forSystem().withResources(resourceFile).compile(
            this::assertHasResource);
  }

  @Test
  void withResourcesAddsResource() {
    ResourceFiles resourceFiles = ResourceFiles.of(
            ResourceFile.of("META-INF/myfile", "test"));
    TestCompiler.forSystem().withResources(resourceFiles).compile(
            this::assertHasResource);
  }

  @Test
  void withProcessorsArrayAddsProcessors() {
    SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
    TestProcessor processor = new TestProcessor();
    TestCompiler.forSystem().withSources(sourceFile).withProcessors(processor).compile((compiled -> {
      assertThat(processor.getProcessedAnnotations()).isNotEmpty();
      assertThat(processor.getProcessedAnnotations()).satisfiesExactly(element ->
              assertThat(element.getQualifiedName().toString()).isEqualTo("java.lang.Deprecated"));
    }));
  }

  @Test
  void withProcessorsAddsProcessors() {
    SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
    TestProcessor processor = new TestProcessor();
    List<Processor> processors = List.of(processor);
    TestCompiler.forSystem().withSources(sourceFile).withProcessors(processors).compile((compiled -> {
      assertThat(processor.getProcessedAnnotations()).isNotEmpty();
      assertThat(processor.getProcessedAnnotations()).satisfiesExactly(element ->
              assertThat(element.getQualifiedName().toString()).isEqualTo("java.lang.Deprecated"));
    }));
  }

  @Test
  void compileWithWritableContent() {
    WritableContent content = appendable -> appendable.append(HELLO_WORLD);
    TestCompiler.forSystem().compile(content, this::assertSuppliesHelloWorld);
  }

  @Test
  void compileWithSourceFile() {
    SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
    TestCompiler.forSystem().compile(sourceFile, this::assertSuppliesHelloWorld);
  }

  @Test
  void compileWithSourceFiles() {
    SourceFiles sourceFiles = SourceFiles.of(SourceFile.of(HELLO_WORLD));
    TestCompiler.forSystem().compile(sourceFiles, this::assertSuppliesHelloWorld);
  }

  @Test
  void compileWithSourceFilesAndResourceFiles() {
    SourceFiles sourceFiles = SourceFiles.of(SourceFile.of(HELLO_WORLD));
    ResourceFiles resourceFiles = ResourceFiles.of(
            ResourceFile.of("META-INF/myfile", "test"));
    TestCompiler.forSystem().compile(sourceFiles, resourceFiles, compiled -> {
      assertSuppliesHelloWorld(compiled);
      assertHasResource(compiled);
    });
  }

  @Test
  @CompileWithForkedClassLoader
  void compiledCodeCanAccessExistingPackagePrivateClassIfAnnotated() throws LinkageError {
    SourceFiles sourceFiles = SourceFiles.of(SourceFile.of("""
            package com.example;

            public class Test implements PublicInterface {

            	public String perform() {
            		return new PackagePrivate().perform();
            	}

            }
            """));
    TestCompiler.forSystem().compile(sourceFiles, compiled -> assertThat(
            compiled.getInstance(PublicInterface.class, "com.example.Test").perform())
            .isEqualTo("Hello from PackagePrivate"));
  }

  @Test
  void compiledCodeCannotAccessExistingPackagePrivateClassIfNotAnnotated() {
    SourceFiles sourceFiles = SourceFiles.of(SourceFile.of("""
            package com.example;

            public class Test implements PublicInterface {

            	public String perform() {
            		return new PackagePrivate().perform();
            	}

            }
            """));
    assertThatExceptionOfType(IllegalAccessError.class)
            .isThrownBy(() -> TestCompiler.forSystem().compile(sourceFiles,
                    compiled -> compiled.getInstance(PublicInterface.class, "com.example.Test").perform()))
            .withMessageContaining(ClassUtils.getShortName(CompileWithForkedClassLoader.class));
  }

  @Test
  void compiledCodeCanReferenceAdditionalClassInSamePackage() {
    SourceFiles sourceFiles = SourceFiles.of(SourceFile.of("""
            package com.example;

            public class Test implements PublicInterface {

            	public String perform() {
            		return Messages.HELLO;
            	}

            }
            """));
    ClassFile messagesClass = ClassFile.of("com.example.Messages",
            new ClassPathResource("com.example.Messages"));
    TestCompiler.forSystem().withClasses(List.of(messagesClass)).compile(sourceFiles, compiled ->
            assertThat(compiled.getInstance(PublicInterface.class, "com.example.Test").perform())
                    .isEqualTo("Hello"));
  }

  @Test
  void compiledCodeCanReferenceAdditionalClassInDifferentPackage() {
    SourceFiles sourceFiles = SourceFiles.of(SourceFile.of("""
            package com.example;

            import com.example.subpackage.Messages;

            public class Test implements PublicInterface {

            	public String perform() {
            		return Messages.HELLO;
            	}

            }
            """));
    ClassFile messagesClass = ClassFile.of("com.example.subpackage.Messages",
            new ClassPathResource("com.example.subpackage.Messages"));
    TestCompiler.forSystem().withClasses(List.of(messagesClass)).compile(sourceFiles, compiled -> assertThat(
            compiled.getInstance(PublicInterface.class, "com.example.Test").perform()).isEqualTo("Hello from subpackage"));
  }

  @Test
  void getResourceForCompiledBytecode() {
    SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
    TestCompiler.forSystem().compile(sourceFile, compiled -> {
      InputStream stream = compiled.getClassLoader().getResourceAsStream("com/example/Hello.class");
      assertThat(stream).isNotNull();
    });
  }

  @Test
  void getUpdatedResourceAsStream() {
    SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
    TestCompiler.forSystem().withResources(ResourceFile.of("com/example/resource", new byte[] { 'a' }))
            .withProcessors(new ResourceModifyingProcessor()).compile(sourceFile, compiled -> assertThat(
                    compiled.getClassLoader().getResourceAsStream("com/example/resource")).hasContent("b"));
  }

  private void assertSuppliesHelloWorld(Compiled compiled) {
    assertThat(compiled.getInstance(Supplier.class).get()).isEqualTo("Hello World!");
  }

  private void assertHasResource(Compiled compiled) {
    assertThat(compiled.getClassLoader().getResourceAsStream(
            "META-INF/myfile")).hasContent("test");
  }

  @SupportedAnnotationTypes("java.lang.Deprecated")
  static class TestProcessor extends AbstractProcessor {

    private final List<TypeElement> processedAnnotations = new ArrayList<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      this.processedAnnotations.addAll(annotations);
      return true;
    }

    public List<TypeElement> getProcessedAnnotations() {
      return this.processedAnnotations;
    }
  }

  @SupportedAnnotationTypes("java.lang.Deprecated")
  static class ResourceModifyingProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      if (roundEnv.processingOver()) {
        try {
          FileObject resource = this.processingEnv.getFiler()
                  .createResource(StandardLocation.CLASS_OUTPUT, "", "com/example/resource");
          try (OutputStream output = resource.openOutputStream()) {
            output.write('b');
          }
        }
        catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
      }
      return true;
    }

  }

}
